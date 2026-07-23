/*
 *
 *  * Copyright (c) 2023-2026 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.z80.minizx3d;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * EXPERIMENTAL — derivación GENÉRICA de instances/deps, SIN direcciones de juego. Estado:
 * los bloques discriminantes se derivan y el buffer runtime $8100 sale como particionante
 * dominante (94%, corridas angostas, span≤8, ratio≥0.9 filtran las ventanas de POSICIÓN
 * tipo SBL), pero la atribución dueño-por-gfx todavía filtra arrastres espurios
 * (precisión 0.9%) y el assert quedó mapeado a los pares de spec. NO usar en el pipeline
 * hasta cerrar eso; el SemanticBuild verificado sigue siendo el de producción.
 * Pendientes (medidos): el filtro de exclusividad de varying (≥70% propias) se come la
 * señal del follower — las hojas de la soga que Willy arrastra cuentan como suyas porque
 * sus eventos de arrastre son de Willy; el share debe computarse descontando los eventos
 * de arrastre (o por dueños del MISMO tipo de bloque). Y el assert de conteo por frame
 * necesita actividad por eventos cercanos (±2 ticks), no rangos de época: el slot runtime
 * se reusa entre salas y su época cubre casi todo el juego.
 *
 * <p>ESTADO tras la segunda ronda (canon por structs + share por gfx): la canonicalización
 * por la grilla de structs derivados funciona (claves limpias $8108/$8110/$8120 por slot,
 * el test de disjunción se volvió honesto y el ratio real del bloque bueno ronda 0.8);
 * los arrastres correctos aparecen ($85ab→$8109/$8111 = el jugador arrastrando la soga).
 * En MONTY (sin oráculo): los structs de estado se derivan solos ($6300 y $6E00 stride 2,
 * ~390 votos) y los bloques particionan al 100%, pero cada evento trae un run de VARIOS
 * bloques a la vez (JSW tenía uno dominante) y la regla de dueño no elige entre bloques →
 * 0 instancias. LO QUE FALTA: elegir EL bloque-identidad por familia de eventos (p.ej. el
 * bloque cuyas particiones mejor se alinean con los gfx), y recién después dueño/arrastre
 * dentro de ese bloque. El assert de conteo en JSW sigue en 0% por esto mismo.
 *
 * <p>Etapa 3 de la base semántica: deriva {@code instances} y {@code deps} de la capa 1
 * ({@code draw_events} + {@code read_sets}) SIN re-replay, y se verifica contra
 * {@code oracle_truth} imprimiendo números (estilo CallTreeProbe).
 *
 * <p>La identidad de instancia salió de la medición, no de una suposición: el
 * {@code dir_core_set} de cada dibujo de guardián nombra el PAR DE SPEC de la data de sala
 * ($C000 + sala*256 + 240 + 2*slot) — no el buffer runtime $8100, que nunca se lee antes de
 * su primer write y por eso no aparece como hoja. Una instancia es (sala, par de spec); la
 * época es cada racha de eventos separada por un hueco (salir de la sala y volver).
 *
 * <p>{@code deps}: una arista follower(A→B) existe cuando los eventos de A llevan además
 * las hojas discriminantes de B, SOSTENIDO en el tiempo — el caso Willy colgado de la soga.
 * La contaminación por cruce tiñe un tick suelto; una dependencia real dura mientras dura
 * el vínculo. Se verifica contra la variable de soga de Willy ($85D6, guardada como fila
 * slot=-1 de oracle_truth).
 */
public final class SemanticBuildGeneric {

  /** hueco que separa dos bloques candidatos en el universo de hojas core. */
  private static final int BLOCK_GAP = Integer.getInteger("build.blockgap", 16);
  /** hueco que separa dos subclusters (instancias) dentro de un bloque. */
  private static final int SUB_GAP = Integer.getInteger("build.subgap", 3);
  // las direcciones de JSW viven SOLO en la verificación contra oracle_truth (el assert);
  // la derivación de bloques discriminantes es generica
  private static final int SALAS = 49152, SALA_BYTES = 256, SPECS_OFF = 240;
  /** hueco de frames que separa dos épocas de la misma instancia (salir y volver). */
  private static final int EPOCH_GAP = Integer.getInteger("build.epochgap", 250);
  /** ticks sostenidos para que un candidato a follower sea una arista real. Bajo (5)
   *  porque el test de ventana viva ya es evidencia fuerte; el sostén sólo filtra el
   *  roce de un cruce casual. */
  private static final int DEP_TICKS = Integer.getInteger("build.depticks", 5);
  /** hueco máximo entre candidatos de la misma racha (~6 ticks: los eventos de willy sin
   *  ventana en el core no dicen nada y hay que puentearlos; re-agarrar la soga en medio
   *  segundo es visualmente el mismo vínculo). */
  private static final int DEP_GAP = Integer.getInteger("build.depgap", 25);

  /** la clave de una corrida, alineada a la grilla del struct derivado que la contiene:
   *  base + floor((min-base)/stride)*stride. Fuera de todo struct, el mínimo tal cual. */
  private static int canon(TreeMap<Integer, int[]> structGrid, int m) {
    Map.Entry<Integer, int[]> s = structGrid.floorEntry(m);
    if (s == null || m > s.getValue()[0])
      return m;
    int stride = s.getValue()[1];
    return s.getKey() + (m - s.getKey()) / stride * stride;
  }

  /** una instancia acumulando sus eventos; la clave es su subcluster discriminante. */
  private static final class Inst {
    final TreeSet<Integer> frames = new TreeSet<>();
    final Set<Integer> gfx = new TreeSet<>();
    int events;
    /** hojas que van y vienen en sus cores: el estado ACTUAL que un follower comparte. */
    Set<Integer> varying;
  }

  public static void main(String[] args) throws Exception {
    String dbPath = args.length > 0 ? args[0] : "analysis/jsw-semantic.db";
    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    Map<Integer, int[]> sets = new HashMap<>();
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT id, n, addrs FROM read_sets")) {
      while (rs.next()) {
        byte[] blob = rs.getBytes(3);
        int n = rs.getInt(2);
        int[] lv = new int[n];
        for (int i = 0; i < n; i++)
          lv[i] = (blob[i * 2] & 0xff) | ((blob[i * 2 + 1] & 0xff) << 8);
        sets.put(rs.getInt(1), lv);
      }
    }

    // ================== derivación GENÉRICA de bloques discriminantes ==================
    // Sin direcciones de juego: un bloque de hojas es discriminante si (a) PARTICIONA —
    // en un mismo frame, eventos distintos llevan subconjuntos disjuntos de él (los
    // specs por entidad) — o (b) es el discriminante ESTABLE de una familia de gráficos
    // (aparece en ≥80% de los eventos de un mismo gfx: el caso del jugador, sin
    // nombrarlo). En un bloque que particiona, cada subcluster (corrida de hojas del
    // evento con hueco ≤ SUB_GAP) es una instancia; un bloque estable es UNA instancia.
    List<int[]> evs = new ArrayList<>(); // {f0, f1, coreId, gfx}
    TreeSet<Integer> universe = new TreeSet<>();
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT frame_first, frame_last, dir_core_set, gfx_base FROM draw_events"
            + " WHERE dir_core_set IS NOT NULL AND routine != 0 AND bytes <= 512")) {
      while (rs.next()) {
        int[] lv = sets.get(rs.getInt(3));
        if (lv == null)
          continue;
        evs.add(new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)});
        for (int leaf : lv)
          universe.add(leaf);
      }
    }
    long events = evs.size();
    // la grilla de los STRUCTS derivados (etapa §4, ya verificada) canonicaliza las
    // corridas: sin esto la clave de un run es el mínimo casual de las hojas presentes
    // ($8100/$8101/$8108...) y la identidad de un slot se parte en claves inestables —
    // capas alimentando capas, como pide el doc (§4.3)
    TreeMap<Integer, int[]> structGrid = new TreeMap<>(); // base -> {end, stride}
    {
      // solo structs de ALTA confianza (votos), greedy sin solaparse: la tabla trae
      // tambien cientos de candidatos debiles y una grilla envenenada canonicaliza mal
      List<int[]> cand = new ArrayList<>(); // {votos, base, end, stride}
      // primera corrida sobre una DB fresca: structs no existe todavia (esta misma pasada
      // lo crea al final); sin grilla la canonicalizacion es identidad y una SEGUNDA
      // corrida ya la aprovecha — capas alimentando capas, iterando barato sin re-replay
      try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
          "SELECT base, end, stride, slots, evidence FROM structs WHERE stride BETWEEN 2"
              + " AND 64 AND slots BETWEEN 3 AND 64")) {
        while (rs.next()) {
          java.util.regex.Matcher m = java.util.regex.Pattern.compile("votos=(\\d+)")
              .matcher(rs.getString(5) == null ? "" : rs.getString(5));
          int votes = m.find() ? Integer.parseInt(m.group(1)) : 0;
          if (votes >= 500)
            cand.add(new int[]{votes, rs.getInt(1), rs.getInt(2), rs.getInt(3)});
        }
      } catch (java.sql.SQLException noStructsYet) {
        // primera pasada: grilla vacia, canon = identidad
      }
      cand.sort((a, b) -> Integer.compare(b[0], a[0]));
      for (int[] c : cand) {
        Map.Entry<Integer, int[]> below = structGrid.floorEntry(c[2]);
        boolean overlaps = below != null && below.getValue()[0] >= c[1];
        if (!overlaps)
          structGrid.put(c[1], new int[]{c[2], c[3]});
      }
    }
    // bloques: clusters del universo por BLOCK_GAP; blockOf(hoja) = arranque del cluster
    TreeMap<Integer, Integer> blockStart = new TreeMap<>(); // start -> end
    {
      int lo = -1, hi = -1;
      for (int leaf : universe) {
        if (lo < 0 || leaf - hi > BLOCK_GAP) {
          if (lo >= 0)
            blockStart.put(lo, hi);
          lo = leaf;
        }
        hi = leaf;
      }
      if (lo >= 0)
        blockStart.put(lo, hi);
    }
    // subclusters por evento: corridas de hojas (hueco ≤ SUB_GAP) dentro de cada bloque
    // Sólo las corridas ANGOSTAS (span ≤ 8) son candidatas a identidad: medido, un
    // discriminante de instancia son 2-6 bytes (spec/slot), mientras una ventana de
    // POSICIÓN (lookup de direcciones deslizando con la coordenada) trae 15-30 hojas —
    // y también particiona, pero por dónde está, no por quién es.
    int[][] evRuns = new int[evs.size()][];   // claves de corrida angosta (min)
    int[][] evBlocks = new int[evs.size()][]; // bloque de cada corrida
    for (int i = 0; i < evs.size(); i++) {
      int[] lv = sets.get(evs.get(i)[2]);
      List<Integer> runs = new ArrayList<>(), blocks = new ArrayList<>();
      int runMin = -1, prevLeaf = Integer.MIN_VALUE, prevBlock = -1;
      for (int leaf : lv) {
        int block = blockStart.floorKey(leaf);
        if (runMin < 0 || block != prevBlock || leaf - prevLeaf > SUB_GAP) {
          if (runMin >= 0 && prevLeaf - runMin <= 8) {
            runs.add(canon(structGrid, runMin));
            blocks.add(prevBlock);
          }
          runMin = leaf;
        }
        prevLeaf = leaf;
        prevBlock = block;
      }
      if (runMin >= 0 && prevLeaf - runMin <= 8) {
        runs.add(canon(structGrid, runMin));
        blocks.add(prevBlock);
      }
      evRuns[i] = runs.stream().mapToInt(Integer::intValue).toArray();
      evBlocks[i] = blocks.stream().mapToInt(Integer::intValue).toArray();
    }
    // score de partición por bloque: frames con ≥2 eventos con subconjuntos disjuntos
    Map<Integer, List<Integer>> byFrame = new HashMap<>();
    for (int i = 0; i < evs.size(); i++)
      byFrame.computeIfAbsent(evs.get(i)[0], k -> new ArrayList<>()).add(i);
    Map<Integer, int[]> blockScore = new HashMap<>(); // block -> {particiones, multi}
    for (List<Integer> frameEvs : byFrame.values()) {
      if (frameEvs.size() < 2)
        continue;
      Map<Integer, List<Set<Integer>>> perBlock = new HashMap<>();
      for (int i : frameEvs) {
        Map<Integer, Set<Integer>> mine = new HashMap<>();
        for (int k = 0; k < evRuns[i].length; k++)
          mine.computeIfAbsent(evBlocks[i][k], b -> new HashSet<>()).add(evRuns[i][k]);
        for (Map.Entry<Integer, Set<Integer>> e : mine.entrySet())
          perBlock.computeIfAbsent(e.getKey(), b -> new ArrayList<>()).add(e.getValue());
      }
      for (Map.Entry<Integer, List<Set<Integer>>> e : perBlock.entrySet()) {
        if (e.getValue().size() < 2)
          continue;
        int[] sc = blockScore.computeIfAbsent(e.getKey(), b -> new int[2]);
        sc[1]++;
        boolean disjoint = true;
        Set<Integer> seen = new HashSet<>();
        for (Set<Integer> s : e.getValue())
          for (int r : s)
            if (!seen.add(r))
              disjoint = false;
        if (disjoint)
          sc[0]++;
      }
    }
    // ratio ≥ 0.9: un discriminante de identidad particiona SIEMPRE; un bloque de
    // definiciones COMPARTIDAS falla exactamente cuando hay dos guardianes idénticos.
    // 0.75 y no 0.9: con claves canónicas el test detecta las colisiones REALES de
    // contaminación (un evento arrastrando el slot de otro) que antes pasaban como
    // claves distintas — el bloque bueno ronda 0.8-0.94, el de defs queda muy abajo
    Set<Integer> partitionBlocks = new TreeSet<>();
    for (Map.Entry<Integer, int[]> e : blockScore.entrySet())
      if (e.getValue()[0] >= 50 && e.getValue()[0] * 4 >= e.getValue()[1] * 3)
        partitionBlocks.add(e.getKey());
    // bloques estables por gfx: presentes en ≥80% de los eventos de un mismo gráfico
    Map<Integer, Integer> gfxCount = new HashMap<>();
    Map<Long, Integer> gfxBlock = new HashMap<>(); // (gfx, block) -> eventos
    for (int i = 0; i < evs.size(); i++) {
      int gfx = evs.get(i)[3];
      if (gfx == 0)
        continue;
      gfxCount.merge(gfx, 1, Integer::sum);
      Set<Integer> blocksHere = new HashSet<>();
      for (int b : evBlocks[i])
        blocksHere.add(b);
      for (int b : blocksHere)
        gfxBlock.merge(((long) gfx << 32) | b, 1, Integer::sum);
    }
    Set<Integer> stableBlocks = new TreeSet<>();
    for (Map.Entry<Long, Integer> e : gfxBlock.entrySet()) {
      int gfx = (int) (e.getKey() >>> 32), block = e.getKey().intValue();
      int total = gfxCount.get(gfx);
      if (total >= 50 && e.getValue() * 10 >= total * 8 && !partitionBlocks.contains(block))
        stableBlocks.add(block);
    }
    // los bloques estables presentes en CASI TODO evento (globales: numero de sala,
    // historia) no identifican a nadie: fuera
    stableBlocks.removeIf(b -> {
      long with = 0;
      for (int i = 0; i < evs.size(); i++)
        for (int bb : evBlocks[i])
          if (bb == b) {
            with++;
            break;
          }
      return with * 10 >= events * 6;
    });
    if (Boolean.getBoolean("build.debug")) {
      System.out.println("grilla de structs usada para canon:");
      structGrid.forEach((b, v) -> System.out.printf("  $%04x..$%04x stride=%d%n",
          b, v[0], v[1]));
      int shown = 0;
      for (Map.Entry<Integer, List<Integer>> fe : byFrame.entrySet()) {
        if (fe.getValue().size() < 2 || shown >= 3)
          continue;
        StringBuilder sb = new StringBuilder("frame " + fe.getKey() + ":");
        boolean any = false;
        for (int i : fe.getValue()) {
          sb.append(" [");
          for (int k = 0; k < evRuns[i].length; k++)
            if (evRuns[i][k] >= 33024 && evRuns[i][k] < 33152) {
              sb.append(String.format("$%04x ", evRuns[i][k]));
              any = true;
            }
          sb.append("]");
        }
        if (any) {
          System.out.println(sb);
          shown++;
        }
      }
      System.out.println("bloques particionantes (base: particiones/multi):");
      for (int b : partitionBlocks)
        System.out.printf("  $%04x..$%04x: %d/%d%n", b, blockStart.get(b),
            blockScore.get(b)[0], blockScore.get(b)[1]);
      System.out.println("bloques estables por gfx:");
      for (int b : stableBlocks)
        System.out.printf("  $%04x..$%04x%n", b, blockStart.get(b));
    }
    // instancias: subclusters de bloques particionantes (≥5 eventos) + bloques estables
    Map<Integer, Integer> instBlock = new HashMap<>(); // clave de instancia -> su bloque
    Map<Integer, Integer> runCount = new HashMap<>();
    for (int i = 0; i < evs.size(); i++)
      for (int k = 0; k < evRuns[i].length; k++)
        if (partitionBlocks.contains(evBlocks[i][k]))
          runCount.merge(evRuns[i][k], 1, Integer::sum);
    for (Map.Entry<Integer, Integer> e : runCount.entrySet())
      if (e.getValue() >= 5)
        instBlock.put(e.getKey(), blockStart.floorKey(e.getKey()));
    for (int b : stableBlocks)
      instBlock.put(b, b);
    // stride por bloque particionante: moda de deltas entre claves de instancia vecinas
    Map<Integer, Integer> blockStride = new HashMap<>();
    for (int b : partitionBlocks) {
      List<Integer> keys = instBlock.entrySet().stream()
          .filter(e -> e.getValue() == b && e.getKey() != b).map(Map.Entry::getKey)
          .sorted().toList();
      Map<Integer, Integer> ds = new HashMap<>();
      for (int k = 1; k < keys.size(); k++)
        ds.merge(keys.get(k) - keys.get(k - 1), 1, Integer::sum);
      blockStride.put(b, ds.entrySet().stream().max(Map.Entry.comparingByValue())
          .map(Map.Entry::getKey).orElse(2));
    }
    // ====== atribución JERÁRQUICA + candidatos a deps (dueño por asociación de gfx) ======
    // Los runs particionantes son identidad de ENTIDAD; los bloques estables son identidad
    // AMBIENTAL de respaldo (el jugador, la sala). El dueño de un evento es la identidad
    // asociada a su GRÁFICO: un guardián co-ocurre con su run ≥50%; si el gfx apunta a otra
    // identidad, los runs presentes son ARRASTRE (el follower que carga estado ajeno).
    Map<Long, Integer> gfxRun = new HashMap<>(); // (gfx, run) -> co-ocurrencias
    for (int i = 0; i < evs.size(); i++) {
      int gfx = evs.get(i)[3];
      if (gfx == 0)
        continue;
      Set<Integer> runsHere = new HashSet<>();
      for (int k = 0; k < evRuns[i].length; k++)
        if (instBlock.containsKey(evRuns[i][k]))
          runsHere.add(evRuns[i][k]);
      for (int rn : runsHere)
        gfxRun.merge(((long) gfx << 32) | rn, 1, Integer::sum);
    }
    Map<Integer, Integer> bestStable = new HashMap<>(); // gfx -> bloque estable dominante
    for (Map.Entry<Long, Integer> e : gfxBlock.entrySet()) {
      int gfx = (int) (e.getKey() >>> 32), block = e.getKey().intValue();
      if (!stableBlocks.contains(block))
        continue;
      int total = gfxCount.getOrDefault(gfx, 0);
      if (total >= 20 && e.getValue() * 10 >= total * 8) {
        Integer cur = bestStable.get(gfx);
        if (cur == null || gfxBlock.getOrDefault(((long) gfx << 32) | cur, 0) < e.getValue())
          bestStable.put(gfx, block);
      }
    }
    Map<Integer, Inst> insts = new TreeMap<>();
    // hojas variables por instancia (para el vinculo VIVO): frecuencia por hoja
    Map<Integer, Map<Integer, Integer>> leafFreq = new HashMap<>();
    Map<Integer, Integer> ownEvents = new HashMap<>();
    Map<Integer, List<int[]>> carryByFrame = new TreeMap<>(); // frame -> {dueño, arrastrada}
    Map<Integer, Map<Integer, Set<Integer>>> carryWin = new TreeMap<>();
    long multiPair = 0;
    int[] evOwner = new int[evs.size()];
    for (int i = 0; i < evs.size(); i++) {
      int[] ev = evs.get(i);
      int gfx = ev[3];
      Set<Integer> runs = new TreeSet<>();
      for (int k = 0; k < evRuns[i].length; k++)
        if (instBlock.containsKey(evRuns[i][k])
            && partitionBlocks.contains(evBlocks[i][k]))
          runs.add(evRuns[i][k]);
      // dueño: el run particionante con mejor asociación al gráfico, SALVO que la
      // asociación sea de nivel contaminación (<5%: willy cuelga de la soga el 3% de su
      // vida — eso es arrastre, no identidad). Un tipo que recorre salas reparte su gfx
      // entre slots (~25%) y le sigue ganando al bloque de definiciones compartido.
      int owner = -1;
      double best = 0.05;
      for (int rn : runs) {
        int total = gfx == 0 ? 0 : gfxCount.getOrDefault(gfx, 0);
        double share = total == 0 ? 0
            : gfxRun.getOrDefault(((long) gfx << 32) | rn, 0) / (double) total;
        if (share > best) {
          best = share;
          owner = rn;
        }
      }
      if (owner < 0 && gfx != 0 && bestStable.containsKey(gfx))
        owner = bestStable.get(gfx);
      if (owner < 0 && runs.size() == 1)
        owner = runs.iterator().next();
      evOwner[i] = owner;
      if (owner < 0)
        continue;
      Inst in = insts.computeIfAbsent(owner, k -> new Inst());
      in.events++;
      in.frames.add(ev[0]);
      in.frames.add(ev[1]);
      if (gfx != 0)
        in.gfx.add(gfx);
      // todo run presente que no es la identidad del dueño es ARRASTRE
      boolean pure = true;
      for (int carried : runs) {
        if (carried == owner)
          continue;
        pure = false;
        multiPair++;
        for (int f = ev[0]; f <= ev[1]; f++)
          carryByFrame.computeIfAbsent(f, k -> new ArrayList<>())
              .add(new int[]{owner, carried, i});
      }
      // la frecuencia de hojas se computa SOLO sobre eventos puros: un evento de arrastre
      // mete el estado del arrastrado en la cuenta del dueño y el share de exclusividad
      // mataba justo la señal del follower (el bug medido del primer intento)
      if (pure) {
        ownEvents.merge(owner, 1, Integer::sum);
        Map<Integer, Integer> lf = leafFreq.computeIfAbsent(owner, k -> new HashMap<>());
        for (int leaf : sets.get(ev[2]))
          lf.merge(leaf, 1, Integer::sum);
      }
    }
    if (Boolean.getBoolean("build.debug")) {
      System.out.println("instancias por eventos (top 15):");
      insts.entrySet().stream()
          .sorted((a, b) -> Integer.compare(b.getValue().events, a.getValue().events))
          .limit(15).forEach(e -> System.out.printf("  $%04x ev=%d gfx=%s%n", e.getKey(),
              e.getValue().events, e.getValue().gfx.stream().limit(4).toList()));
      Map<Long, Integer> carryPairs = new HashMap<>();
      for (List<int[]> l : carryByFrame.values())
        for (int[] oc : l)
          carryPairs.merge(((long) oc[0] << 32) | oc[1], 1, Integer::sum);
      System.out.println("arrastres (dueño->arrastrada, top 10 por frames):");
      carryPairs.entrySet().stream()
          .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(10)
          .forEach(e -> System.out.printf("  $%04x -> $%04x x%d%n", e.getKey() >>> 32,
              e.getKey() & 0xffffL, e.getValue()));
    }
    // hojas variables por instancia, EXCLUSIVAS: una hoja que aparece en eventos de
    // muchas instancias es maquinaria compartida de POSICIÓN (la tabla de lookup: dos
    // entidades a la misma y llevan las mismas hojas y el test de vínculo vivo pasaría
    // por coincidencia). El estado que identifica un vínculo es variable EN el líder y
    // raro FUERA de él (≥70% de sus apariciones son suyas).
    Map<Integer, Integer> leafTotal = new HashMap<>();
    for (Map<Integer, Integer> lf : leafFreq.values())
      for (Map.Entry<Integer, Integer> l : lf.entrySet())
        leafTotal.merge(l.getKey(), l.getValue(), Integer::sum);
    for (Map.Entry<Integer, Inst> e : insts.entrySet()) {
      Map<Integer, Integer> lf = leafFreq.get(e.getKey());
      if (lf == null)
        continue;
      int own = ownEvents.getOrDefault(e.getKey(), 0);
      Set<Integer> varying = new HashSet<>();
      for (Map.Entry<Integer, Integer> l : lf.entrySet())
        if (l.getValue() * 10 < own * 9 && l.getValue() >= 3
            && l.getValue() * 10 >= leafTotal.get(l.getKey()) * 7)
          varying.add(l.getKey());
      e.getValue().varying = varying;
    }
    Map<Integer, TreeMap<Integer, Set<Integer>>> aloneWin = new TreeMap<>();
    for (int i = 0; i < evs.size(); i++) {
      int owner = evOwner[i];
      Inst in = owner < 0 ? null : insts.get(owner);
      if (in == null || in.varying == null)
        continue;
      Set<Integer> w = new TreeSet<>();
      for (int leaf : sets.get(evs.get(i)[2]))
        if (in.varying.contains(leaf))
          w.add(leaf);
      if (w.size() >= 3)
        aloneWin.computeIfAbsent(owner, k -> new TreeMap<>()).put(evs.get(i)[0], w);
    }
    for (Map.Entry<Integer, List<int[]>> e : carryByFrame.entrySet())
      for (int[] oc : e.getValue()) {
        Inst ci = insts.get(oc[1]);
        if (ci == null || ci.varying == null)
          continue;
        Set<Integer> w = new TreeSet<>();
        for (int leaf : sets.get(evs.get(oc[2])[2]))
          if (ci.varying.contains(leaf))
            w.add(leaf);
        carryWin.computeIfAbsent(e.getKey(), k -> new HashMap<>()).merge(oc[1], w,
            (a, b) -> {
              Set<Integer> u = new TreeSet<>(a);
              u.addAll(b);
              return u;
            });
      }

    // --- instances: épocas por hueco de frames ---
    try (var st = conn.createStatement()) {
      st.execute("DROP TABLE IF EXISTS instances");
      st.execute("DROP TABLE IF EXISTS deps");
      st.execute("CREATE TABLE instances(slot INT, epoch INT, block_base INT, stride INT,"
          + " room INT, pair_idx INT, frame_first INT, frame_last INT, events INT,"
          + " gfx_bases TEXT, evidence TEXT)");
      st.execute("CREATE TABLE deps(frame_first INT, frame_last INT, instance_a INT,"
          + " instance_b INT, kind TEXT, support REAL, evidence TEXT)");
    }
    PreparedStatement insI = conn.prepareStatement(
        "INSERT INTO instances VALUES (?,?,?,?,?,?,?,?,?,?,?)");
    int instRows = 0;
    for (Map.Entry<Integer, Inst> e : insts.entrySet()) {
      int pairAddr = e.getKey();
      Inst in = e.getValue();
      int epoch = 0, first = -1, prev = -1;
      List<int[]> epochs = new ArrayList<>();
      for (int f : in.frames) {
        if (first < 0)
          first = f;
        else if (f - prev > EPOCH_GAP) {
          epochs.add(new int[]{first, prev});
          first = f;
        }
        prev = f;
      }
      if (first >= 0)
        epochs.add(new int[]{first, prev});
      // columnas derivadas, sin layout de juego: bloque, stride del bloque y slot dentro
      // del bloque (para JSW coinciden con sala/par de spec, y eso lo chequea el assert)
      int block = instBlock.getOrDefault(pairAddr, pairAddr);
      int stride = blockStride.getOrDefault(block, 0);
      boolean stable = stableBlocks.contains(pairAddr);
      for (int[] ep : epochs) {
        insI.setInt(1, pairAddr);
        insI.setInt(2, epoch++);
        insI.setInt(3, pairAddr);
        insI.setInt(4, stable ? 0 : stride);
        insI.setInt(5, block);
        insI.setInt(6, stable || stride == 0 ? 0 : (pairAddr - block) / stride);
        insI.setInt(7, ep[0]);
        insI.setInt(8, ep[1]);
        insI.setInt(9, in.events);
        insI.setString(10, in.gfx.toString());
        insI.setString(11, stable ? "bloque-estable (discriminante de familia de gfx)"
            : "subcluster de bloque particionante $" + Integer.toHexString(block));
        insI.addBatch();
        instRows++;
      }
    }
    insI.executeBatch();

    // --- deps: follower(dueño -> arrastrada) sostenido, con el test del vínculo VIVO ---
    PreparedStatement insD = conn.prepareStatement("INSERT INTO deps VALUES (?,?,?,?,?,?,?)");
    int depRows = 0;
    Map<Long, List<int[]>> depRanges = new TreeMap<>();
    Map<Long, int[]> open = new HashMap<>(); // (dueño, arrastrada) -> {first, last, count}
    for (Map.Entry<Integer, List<int[]>> e : carryByFrame.entrySet()) {
      int f = e.getKey();
      for (int[] oc : e.getValue()) {
        int owner = oc[0], carried = oc[1];
        long okey = ((long) owner << 32) | carried;
        Set<Integer> ww = carryWin.getOrDefault(f, Map.of()).getOrDefault(carried, Set.of());
        // vivo ⟺ la arrastrada se dibuja SOLA cerca (si no, el dueño está en otra parte
        // arrastrando hojas viejas: medido 624 a 1 en JSW) Y su estado variable ACTUAL
        // solapa ≥25% lo que el dueño arrastra (congelado tras soltarse, deja de solapar)
        TreeMap<Integer, Set<Integer>> rw = aloneWin.get(carried);
        Map.Entry<Integer, Set<Integer>> near =
            rw == null ? null : rw.floorEntry(f + 8);
        if (near == null || near.getKey() < f - 8 || ww.isEmpty())
          continue;
        int overlap = 0;
        for (int a : ww)
          if (near.getValue().contains(a))
            overlap++;
        if (overlap * 4 < ww.size())
          continue;
        int[] r = open.get(okey);
        if (r != null && f - r[1] <= DEP_GAP) {
          r[1] = f;
          r[2]++;
        } else {
          if (r != null && r[2] >= DEP_TICKS)
            depRanges.computeIfAbsent(okey, k -> new ArrayList<>()).add(r);
          open.put(okey, new int[]{f, f, 1});
        }
      }
    }
    for (Map.Entry<Long, int[]> e : open.entrySet())
      if (e.getValue()[2] >= DEP_TICKS)
        depRanges.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
    for (Map.Entry<Long, List<int[]>> e : depRanges.entrySet())
      for (int[] r : e.getValue()) {
        int owner = (int) (e.getKey() >>> 32), carried = e.getKey().intValue();
        insD.setInt(1, r[0]);
        insD.setInt(2, r[1]);
        insD.setInt(3, owner);
        insD.setInt(4, carried);
        insD.setString(5, "follower");
        insD.setDouble(6, r[2] / Math.max(1.0, (r[1] - r[0]) / 4.0));
        insD.setString(7, "$" + Integer.toHexString(owner) + " lleva el estado variable de $"
            + Integer.toHexString(carried));
        insD.addBatch();
        depRows++;
      }
    insD.executeBatch();

    System.out.printf("instances: %d filas (%d instancias) · deps: %d filas · eventos"
        + " usados %d · multi-par (contaminacion o dep g-g): %d%n",
        instRows, insts.size(), depRows, events, multiPair);

    // =================== VERIFICACIÓN contra oracle_truth ===================
    System.out.println("\n=== verificacion contra oracle_truth ===");
    // 1. conteo por sala: instancias con eventos vs slots activos del truth, por muestra
    Map<Integer, int[]> truthBySlot = new TreeMap<>(); // frame*16+slot compactado no: usar lista
    List<int[]> truth = new ArrayList<>(); // {frame, slot, tipo}
    List<int[]> willyTruth = new ArrayList<>(); // {frame, sogaStatus}
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT frame, slot, tipo FROM oracle_truth")) {
      while (rs.next()) {
        if (rs.getInt(2) < 0)
          willyTruth.add(new int[]{rs.getInt(1), rs.getInt(3)});
        else
          truth.add(new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)});
      }
    }
    // el índice de slot lo calcula EL ASSERT con el layout del oráculo — dos identidades
    // posibles: el buffer runtime (33024 + 8*slot, la que la derivación genérica
    // encuentra) o el par de spec. Y la actividad es POR EVENTOS CERCANOS (±2 ticks), no
    // por rangos de época: el slot runtime se reusa entre salas y su época cubre casi
    // todo el juego (el otro bug medido del primer intento).
    Map<Integer, TreeSet<Integer>> activity = new TreeMap<>(); // slotOracle -> frames
    for (Map.Entry<Integer, Inst> e : insts.entrySet()) {
      int key = e.getKey(), slotOracle = -1;
      if (key >= 33024 && key < 33024 + 8 * 8)
        slotOracle = (key - 33024) / 8;
      else if (key >= SALAS && key % SALA_BYTES >= SPECS_OFF)
        slotOracle = (key % SALA_BYTES - SPECS_OFF) / 2;
      if (slotOracle >= 0)
        activity.computeIfAbsent(slotOracle, k -> new TreeSet<>())
            .addAll(e.getValue().frames);
    }
    Map<Integer, Set<Integer>> truthSlotsByFrame = new TreeMap<>();
    for (int[] t : truth)
      truthSlotsByFrame.computeIfAbsent(t[0], k -> new TreeSet<>()).add(t[1]);
    long okCount = 0, okSlots = 0, samples = 0;
    for (Map.Entry<Integer, Set<Integer>> e : truthSlotsByFrame.entrySet()) {
      int f = e.getKey();
      if (f % 50 != 0)
        continue;
      samples++;
      Set<Integer> found = new TreeSet<>();
      for (Map.Entry<Integer, TreeSet<Integer>> a : activity.entrySet()) {
        Integer near = a.getValue().floor(f + 25);
        if (near != null && near >= f - 25)
          found.add(a.getKey());
      }
      if (found.size() == e.getValue().size())
        okCount++;
      if (found.equals(e.getValue()))
        okSlots++;
    }
    System.out.printf("  conteo instancias == activos del truth: %.1f%% de %d muestras ·"
        + " ademas MISMOS indices de slot: %.1f%%%n",
        100.0 * okCount / Math.max(1, samples), samples,
        100.0 * okSlots / Math.max(1, samples));

    // 2. follower(willy->soga) vs $85D6
    Set<Integer> hangFrames = new TreeSet<>();
    for (int[] w : willyTruth)
      if (w[1] >= 3 && w[1] <= 32)
        hangFrames.add(w[0]);
    Set<Integer> depFrames = new TreeSet<>();
    Map<Integer, Set<Integer>> depFramesByPair = new TreeMap<>();
    for (Map.Entry<Long, List<int[]>> e : depRanges.entrySet())
      for (int[] r : e.getValue())
        for (int f = r[0]; f <= r[1]; f++) {
          depFrames.add(f);
          depFramesByPair.computeIfAbsent(e.getKey().intValue(), k -> new TreeSet<>()).add(f);
        }
    long hit = 0;
    for (int f : depFrames)
      if (hangFrames.contains(f) || hangFrames.contains(f - 1) || hangFrames.contains(f + 1)
          || hangFrames.contains(f - 2) || hangFrames.contains(f + 2))
        hit++;
    long cover = 0;
    for (int f : hangFrames)
      if (depFrames.contains(f) || depFrames.contains(f - 1) || depFrames.contains(f + 1)
          || depFrames.contains(f - 2) || depFrames.contains(f + 2))
        cover++;
    System.out.printf("  follower(willy->X): %d frames en deps; %.1f%% caen en frames"
        + " colgado(truth) [precision] · %.1f%% de los %d frames colgado cubiertos"
        + " [recall]%n", depFrames.size(), 100.0 * hit / Math.max(1, depFrames.size()),
        100.0 * cover / Math.max(1, hangFrames.size()), hangFrames.size());
    // por EPISODIO: cada racha de colgado del truth, ¿tiene su arista?
    List<int[]> hangRanges = new ArrayList<>();
    int hf = -1, hp = -1;
    for (int f : hangFrames) {
      if (hf < 0)
        hf = f;
      else if (f - hp > 25) {
        hangRanges.add(new int[]{hf, hp});
        hf = f;
      }
      hp = f;
    }
    if (hf >= 0)
      hangRanges.add(new int[]{hf, hp});
    long touched = hangRanges.stream().filter(r -> {
      for (int f = r[0]; f <= r[1]; f++)
        if (depFrames.contains(f))
          return true;
      return false;
    }).count();
    System.out.printf("  por episodio: %d/%d rachas de colgado tienen arista%n",
        touched, hangRanges.size());
    for (Map.Entry<Integer, Set<Integer>> e : depFramesByPair.entrySet())
      System.out.printf("    -> par $%x (sala %d, slot %d): %d frames%n", e.getKey(),
          (e.getKey() - SALAS) / SALA_BYTES, (e.getKey() % SALA_BYTES - SPECS_OFF) / 2,
          e.getValue().size());

    // =========== param_routines / structs / fields (§4, derivados de mem_accesses) ===========
    // Dos sabores medidos (§4.2): TRASLADO (misma rutina+patrón, bases distintas — drawSprite
    // sobre la biblioteca de gráficos) y LOOP-INTERNO (el loop vive dentro de la invocación:
    // una sola base y el conjunto de offsets es periódico — moveGuardians sobre el buffer de
    // entidades). El stride no se conoce: se deriva de la moda de deltas o por autocorrelación.
    Map<Integer, int[]> pats = new HashMap<>();
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT id, n, offsets FROM access_patterns")) {
      while (rs.next()) {
        byte[] blob = rs.getBytes(3);
        int n = rs.getInt(2);
        int[] offs = new int[n];
        for (int i = 0; i < n; i++)
          offs[i] = (blob[i * 2] & 0xff) | ((blob[i * 2 + 1] & 0xff) << 8);
        pats.put(rs.getInt(1), offs);
      }
    }
    Map<Integer, Integer> profile = new HashMap<>();
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT addr, writes FROM mem_profile")) {
      while (rs.next())
        profile.put(rs.getInt(1), rs.getInt(2));
    }
    // (rutina, patrón) -> bases vistas (sabor traslado); filas crudas para el sabor loop
    Map<Long, TreeSet<Integer>> basesByRP = new HashMap<>();
    List<long[]> rawRows = new ArrayList<>(); // {routine, base, pattern, count}
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT routine, base, pattern, count FROM mem_accesses WHERE pattern IS NOT NULL")) {
      while (rs.next()) {
        int rout = rs.getInt(1), base = rs.getInt(2), pat = rs.getInt(3);
        basesByRP.computeIfAbsent(((long) rout << 32) | pat, k -> new TreeSet<>()).add(base);
        rawRows.add(new long[]{rout, base, pat, rs.getInt(4)});
      }
    }
    try (var st = conn.createStatement()) {
      st.execute("DROP TABLE IF EXISTS param_routines");
      st.execute("DROP TABLE IF EXISTS structs");
      st.execute("DROP TABLE IF EXISTS fields");
      st.execute("CREATE TABLE param_routines(addr INT, kind TEXT, base INT, stride INT,"
          + " bases INT, span INT, invocations INT, evidence TEXT)");
      st.execute("CREATE TABLE structs(id INTEGER PRIMARY KEY, derived_from INT, base INT,"
          + " end INT, stride INT, slots INT, evidence TEXT)");
      st.execute("CREATE TABLE fields(struct_id INT, offset INT, mutable INT,"
          + " writes_median INT, evidence TEXT)");
    }
    PreparedStatement insP = conn.prepareStatement(
        "INSERT INTO param_routines VALUES (?,?,?,?,?,?,?,?)");
    PreparedStatement insS = conn.prepareStatement(
        "INSERT INTO structs(derived_from, base, end, stride, slots, evidence)"
            + " VALUES (?,?,?,?,?,?)");
    PreparedStatement insF = conn.prepareStatement("INSERT INTO fields VALUES (?,?,?,?,?)");
    int paramRows = 0, structRows = 0;
    // candidatos a struct: {base, end, stride (0 = sin), votos, rutina, offsets plegados}
    List<Object[]> cands = new ArrayList<>();
    // sabor TRASLADO: misma rutina + mismo patrón, bases distintas
    for (Map.Entry<Long, TreeSet<Integer>> e : basesByRP.entrySet()) {
      TreeSet<Integer> bases = e.getValue();
      if (bases.size() < 3)
        continue;
      int rout = (int) (e.getKey() >>> 32), pat = e.getKey().intValue();
      int span = 0;
      for (int o : pats.get(pat))
        span = Math.max(span, o + 1);
      // deltas entre bases consecutivas: la moda; si divide casi todos, es progresión
      Map<Integer, Integer> deltas = new HashMap<>();
      Integer prev = null;
      for (int b : bases) {
        if (prev != null)
          deltas.merge(b - prev, 1, Integer::sum);
        prev = b;
      }
      int mode = deltas.entrySet().stream().max(Map.Entry.comparingByValue())
          .map(Map.Entry::getKey).orElse(0);
      long regular = deltas.entrySet().stream()
          .filter(d -> mode > 0 && d.getKey() % mode == 0).mapToLong(Map.Entry::getValue).sum();
      long total = deltas.values().stream().mapToLong(Integer::intValue).sum();
      // mode 1 = bases empaquetadas byte a byte (leer una tabla desde offsets distintos),
      // no un arreglo de structs: progresión recién desde delta 2
      boolean progression = mode > 1 && regular * 10 >= total * 9;
      insP.setInt(1, rout);
      insP.setString(2, "traslado");
      insP.setInt(3, bases.first());
      if (progression)
        insP.setInt(4, mode);
      else
        insP.setNull(4, java.sql.Types.INTEGER);
      insP.setInt(5, bases.size());
      insP.setInt(6, span);
      insP.setInt(7, bases.size());
      insP.setString(8, "pat=" + pat + " bases=$" + Integer.toHexString(bases.first())
          + "..$" + Integer.toHexString(bases.last()));
      insP.addBatch();
      paramRows++;
      cands.add(new Object[]{bases.first(), bases.last() + span - 1,
          progression ? mode : 0, bases.size(), rout, null});
    }
    // sabor LOOP-INTERNO: autocorrelación POR PATRÓN (la unión histórica mezcla fases y
    // regiones y alucina strides), con ≥3 períodos; después voto por (rutina, base)
    Map<Long, Map<Integer, long[]>> votes = new HashMap<>(); // (rout,base) -> stride -> {votos, span, offsetsFold}
    Map<Long, Map<Integer, TreeSet<Integer>>> foldByRBS = new HashMap<>();
    for (long[] row : rawRows) {
      int[] offs = pats.get((int) row[2]);
      if (offs.length < 9)
        continue;
      int span = offs[offs.length - 1] + 1;
      Set<Integer> set = new HashSet<>();
      for (int o : offs)
        set.add(o);
      int bestS = 0;
      double bestScore = 0;
      for (int s = 2; s <= Math.min(64, span / 3); s++) {
        int match = 0, tot = 0;
        for (int o : offs)
          if (o + s < span) {
            tot++;
            if (set.contains(o + s))
              match++;
          }
        double score = tot < 6 ? 0 : match / (double) tot;
        if (score > bestScore + 0.01) {
          bestScore = score;
          bestS = s;
        }
      }
      if (bestScore < 0.7 || bestS == 0 || span / bestS < 3)
        continue;
      long rb = (row[0] << 32) | row[1];
      votes.computeIfAbsent(rb, k -> new HashMap<>())
          .merge(bestS, new long[]{row[3], span}, (a, b) -> {
            a[0] += b[0];
            a[1] = Math.max(a[1], b[1]);
            return a;
          });
      TreeSet<Integer> fold = foldByRBS.computeIfAbsent(rb, k -> new HashMap<>())
          .computeIfAbsent(bestS, k -> new TreeSet<>());
      for (int o : offs)
        fold.add(o % bestS);
    }
    for (Map.Entry<Long, Map<Integer, long[]>> e : votes.entrySet()) {
      int rout = (int) (e.getKey() >>> 32), base = e.getKey().intValue();
      Map.Entry<Integer, long[]> best = e.getValue().entrySet().stream()
          .max(java.util.Comparator.comparingLong(x -> x.getValue()[0])).orElseThrow();
      int stride = best.getKey();
      long[] v = best.getValue();
      insP.setInt(1, rout);
      insP.setString(2, "loop-interno");
      insP.setInt(3, base);
      insP.setInt(4, stride);
      insP.setInt(5, (int) (v[1] / stride));
      insP.setInt(6, (int) v[1]);
      insP.setInt(7, (int) v[0]);
      insP.setString(8, "votos=" + v[0]);
      insP.addBatch();
      paramRows++;
      cands.add(new Object[]{base, base + (int) v[1] - 1, stride, (int) v[0], rout,
          foldByRBS.get(e.getKey()).get(stride)});
    }
    // consolidación POR RUTINA (§4.3: un struct es lo que fluye por la misma rutina
    // paramétrica; fusionar a través de rutinas encadenaba media RAM en un rango)
    cands.sort((a, b) -> {
      int c = Integer.compare((int) a[2], (int) b[2]);
      if (c == 0)
        c = Integer.compare((int) a[4], (int) b[4]);
      return c != 0 ? c : Integer.compare((int) a[0], (int) b[0]);
    });
    List<Object[]> merged = new ArrayList<>();
    for (Object[] c : cands) {
      Object[] last = merged.isEmpty() ? null : merged.get(merged.size() - 1);
      int stride = (int) c[2];
      if (last != null && (int) last[2] == stride && stride > 0
          && (int) last[4] == (int) c[4]
          && (int) c[0] <= (int) last[1] + stride
          && ((int) c[0] - (int) last[0]) % stride == 0) {
        last[1] = Math.max((int) last[1], (int) c[1]);
        last[3] = (int) last[3] + (int) c[3];
        if (last[5] == null)
          last[5] = c[5];
        else if (c[5] != null)
          ((TreeSet<Integer>) last[5]).addAll((TreeSet<Integer>) c[5]);
        continue;
      }
      merged.add(new Object[]{c[0], c[1], c[2], c[3], c[4], c[5]});
    }
    // subsunción: un candidato contenido en otro con mismo stride y misma fase es el mismo
    // struct visto desde un slot posterior (patrones parciales); se absorbe sumando votos
    for (Object[] a : merged) {
      if (a[0] == null)
        continue;
      for (Object[] b : merged) {
        if (a == b || b[0] == null || (int) a[2] == 0 || !a[2].equals(b[2]))
          continue;
        if ((int) b[0] >= (int) a[0] && (int) b[1] <= (int) a[1]
            && ((int) b[0] - (int) a[0]) % (int) a[2] == 0) {
          a[3] = (int) a[3] + (int) b[3];
          if (a[5] == null)
            a[5] = b[5];
          else if (b[5] != null)
            ((TreeSet<Integer>) a[5]).addAll((TreeSet<Integer>) b[5]);
          b[0] = null;
        }
      }
    }
    merged.removeIf(c -> c[0] == null);
    for (Object[] c : merged) {
      int base = (int) c[0], end = (int) c[1], stride = (int) c[2], vts = (int) c[3];
      int slots = stride > 0 ? (end - base + 1) / stride : vts;
      if (stride > 0 && (slots < 3 || slots > 512) || vts < 5)
        continue;
      insS.setInt(1, (int) c[4]);
      insS.setInt(2, base);
      insS.setInt(3, end);
      if (stride > 0)
        insS.setInt(4, stride);
      else
        insS.setNull(4, java.sql.Types.INTEGER);
      insS.setInt(5, slots);
      insS.setString(6, (stride > 0 ? "loop/progresion" : "traslado") + " votos=" + vts);
      insS.addBatch();
      structRows++;
      if (stride >= 2 && stride <= 64 && c[5] != null) {
        for (int off : (TreeSet<Integer>) c[5]) {
          List<Integer> ws = new ArrayList<>();
          for (int b = base + off; b <= end; b += stride)
            ws.add(profile.getOrDefault(b, 0));
          ws.sort(null);
          int median = ws.isEmpty() ? 0 : ws.get(ws.size() / 2);
          insF.setInt(1, structRows);
          insF.setInt(2, off);
          insF.setInt(3, median > 100 ? 1 : 0);
          insF.setInt(4, median);
          insF.setString(5, "mediana de escrituras sobre " + ws.size() + " slots");
          insF.addBatch();
        }
      }
    }
    insP.executeBatch();
    insS.executeBatch();
    insF.executeBatch();
    System.out.printf("%nparam_routines: %d · structs: %d%n", paramRows, structRows);
    System.out.println("structs derivados (top por slots):");
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT base, end, stride, slots, evidence FROM structs ORDER BY slots DESC LIMIT 8")) {
      while (rs.next())
        System.out.printf("  $%04x..$%04x stride=%s slots=%d (%s)%n", rs.getInt(1),
            rs.getInt(2), rs.getObject(3), rs.getInt(4), rs.getString(5));
    }

    // 3. cero aristas guardian-guardian sostenidas (co-ocurrencias multi-par)
    // el assert de "ningún guardián independiente tiene aristas": deps entre dos
    // instancias de bloques PARTICIONANTES (los guardianes; el jugador es bloque estable)
    long gg = depRanges.keySet().stream().filter(k -> {
      int a = (int) (k >>> 32), b = k.intValue();
      return !stableBlocks.contains(a) && !stableBlocks.contains(b);
    }).count();
    System.out.printf("  aristas entre instancias particionantes (guardian-guardian,"
        + " deberian ser 0): %d · eventos multi-instancia: %d%n", gg, multiPair);
    conn.close();
  }

  private SemanticBuildGeneric() {
  }
}
