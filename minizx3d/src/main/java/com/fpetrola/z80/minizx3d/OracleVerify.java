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

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * El arnés de verificación de la base semántica (doc/BASE-SEMANTICA.md §8): contrasta lo que
 * el replay observa contra {@code jsw-oracle.json}, los hechos DOCUMENTADOS del desensamblado
 * comunitario de JSW — no inferidos del propio replay, que es la regla de doc/CLAUDE.md.
 *
 * <p>El estilo es el de {@link CallTreeProbe}: el éxito de cada plano es un número impreso,
 * no una impresión. Este main es la etapa 0 del plan: deja el medidor listo ANTES de que
 * exista el plano de dirección, e imprime el baseline que ese plano tiene que superar (dos
 * guardianes idénticos = UNA entrada de owner, porque owner clasifica por base de catálogo).
 *
 * <p>Qué mide hoy, por sección: (1) qué rutinas del oráculo aparecen ejecutadas y cuáles
 * escriben pantalla — la sanidad de que el RZX corre el juego documentado y de que las
 * direcciones no están corridas; (2) la tabla de entidades leída EN VIVO del emulador en el
 * mismo frame (§8: el contenido lo da la memoria, el oráculo sólo dice dónde mirar), con la
 * cobertura del RZX: salas visitadas, salas con soga, salas con guardianes idénticos, y los
 * rangos de frames en que Willy cuelga de la soga; (3) el baseline de atribución por owner.
 */
public final class OracleVerify {

  /** los hechos del oráculo, tal cual el JSON; sólo lo que esta etapa consume. */
  static final class Oracle {
    final JsonValue root;
    final Map<String, Integer> rutinas = new LinkedHashMap<>();
    final Set<String> dibujan = new HashSet<>();
    final Set<String> porJP = new HashSet<>();
    final int[] bufferCandidatos;
    final int entBase, entStride, entSlots, entTerm, tipoOffset, tipoMask;
    final Map<Integer, String> tipoNombre = new HashMap<>();
    final int willyY, willySoga, vidas, salaActual;
    final int salasBase, salaBytes, specsOffset, specsMax;

    Oracle(JsonValue root) {
      this.root = root;
      for (JsonValue r = root.get("rutinas").child; r != null; r = r.next)
        rutinas.put(r.name, r.asInt());
      for (JsonValue d = root.get("rutinasQueDibujanPantalla").child; d != null; d = d.next)
        dibujan.add(d.asString());
      for (JsonValue d = root.get("rutinasQueEntranPorJP").child; d != null; d = d.next)
        porJP.add(d.asString());
      bufferCandidatos = root.get("pantalla").get("bufferCandidatos").asIntArray();
      JsonValue e = root.get("entidades");
      entBase = e.getInt("bufferBase");
      entStride = e.getInt("stride");
      entSlots = e.getInt("slots");
      entTerm = e.getInt("terminador");
      tipoOffset = e.get("tipo").getInt("offset");
      tipoMask = e.get("tipo").getInt("mask");
      for (JsonValue v = e.get("tipo").get("valores").child; v != null; v = v.next)
        tipoNombre.put(Integer.parseInt(v.name), v.asString());
      willyY = root.get("willy").getInt("y");
      willySoga = root.get("willy").getInt("soga");
      vidas = root.getInt("vidas");
      salaActual = root.getInt("salaActual");
      JsonValue s = root.get("salas");
      salasBase = s.getInt("base");
      salaBytes = s.getInt("bytesPorSala");
      specsOffset = s.get("specs").getInt("offset");
      specsMax = s.get("specs").getInt("max");
    }

    static Oracle load() throws Exception {
      String path = System.getProperty("oracle", "");
      try (java.io.InputStream in = path.isEmpty()
          ? OracleVerify.class.getResourceAsStream("/jsw-oracle.json")
          : new java.io.FileInputStream(path)) {
        return new Oracle(new JsonReader().parse(in));
      }
    }
  }

  /** lo observado en una sala a lo largo de la corrida. */
  private static final class Room {
    int frameFirst = Integer.MAX_VALUE, frameLast, samples;
    String name = "";
    long activeSum;
    int specCount = -1;
    boolean rope, identicalPair;
    long ownerBoxesSum, ownerActiveSamples;
    final Map<Integer, Integer> typeHisto = new TreeMap<>();
    /** movimiento entre muestras por slot: |dx|+|dy| acumulado y máximo. */
    long moveSum;
    int moveMax, moveSamples;
    /** plano dir: grupos (slots+willy) observados por muestra, y el smoke §3.1 — en
     *  cuántas muestras el gráfico compartido de un par idéntico se partió en ≥2 slots. */
    long dirGroupsSum;
    int dirSamples, dirPairFrames, dirPairSplit;
  }

  public static void main(String[] args) throws Exception {
    GameProfile profile = GameProfile.resolve(args);
    Oracle o = Oracle.load();
    int from = Integer.getInteger("oracle.from", 500);
    int frames = Integer.getInteger("oracle.frames", 20000);
    int sample = Integer.getInteger("oracle.sample", 10);
    // -Doracle.db: el perfil prefiere el catalogo legible (doc/catalogo-<g>.md, curado y
    // CHICO); para medir atribucion sobre TODOS los guardianes hace falta el catalogo
    // ancho del tracker/discovery. La metrica vale lo que cubra el catalogo elegido.
    String dbPath = System.getProperty("oracle.db", profile.db);
    SpriteCatalog catalog = new SpriteCatalog(dbPath, 128);
    System.out.println("arnes contra jsw-oracle.json: " + profile.title + " · frames "
        + from + ".." + (from + frames) + " (muestra cada " + sample + ")");

    Set<Integer> oracleAddrs = new HashSet<>(o.rutinas.values());
    Set<Integer> seenRoutines = new HashSet<>();
    Set<Integer> screenRoutines = new HashSet<>();
    Map<Integer, Long> foreignScreen = new HashMap<>(); // rutinas NO-oraculo que escriben pantalla
    Map<Integer, Room> rooms = new TreeMap<>();
    List<int[]> lifeEvents = new ArrayList<>(); // {frame, vidasAntes, vidasDespues}
    Set<Integer> loseLifeFrames = new HashSet<>();
    List<int[]> ropeRanges = new ArrayList<>(); // rangos de frames con Willy colgado
    int[] willyRopePrev = {0, -1}; // {estado previo, frame de inicio del rango}
    int[] livesPrev = {-1};
    int[] roomPrev = {-1};
    int[] sampled = {0};
    double[] bufferMatch = new double[o.bufferCandidatos.length];
    int[] bufferSamples = {0};
    // --- plano dir (solo con -Ddir.plane=true): atribución contra la tabla de entidades ---
    Map<Integer, int[]> dirMemo = new HashMap<>();
    long[] dirOwned = {0}, dirAttributed = {0}, dirMulti = {0}, dirSaturated = {0};
    Map<String, Long> leafZones = new TreeMap<>();
    Map<Integer, Long> topLeaves = new HashMap<>();
    // por slot: posicion previa muestreada, para la suavidad de movimiento
    int[][] prevPos = new int[o.entSlots][]; // null = no activo en la muestra anterior
    int[] prevPosRoom = {-1};

    TaintReplay[] holder = new TaintReplay[1];
    TaintReplay replay = new TaintReplay(profile.rzx, catalog, snap -> {
      TaintReplay r = holder[0];
      int f = snap.frame();
      if (f < from)
        return;
      // --- cada frame: rutinas del arbol, vidas, sala, soga de Willy (baratos) ---
      for (int i = 0; i < r.nodeAddr.size; i++) {
        int a = r.nodeAddr.get(i);
        if (oracleAddrs.contains(a)) {
          seenRoutines.add(a);
          if (a == o.rutinas.get("loseLife"))
            loseLifeFrames.add(f);
        }
      }
      int lives = r.memByte(o.vidas);
      if (livesPrev[0] >= 0 && lives != livesPrev[0])
        lifeEvents.add(new int[]{f, livesPrev[0], lives});
      livesPrev[0] = lives;
      int roomNo = r.memByte(o.salaActual);
      Room room = rooms.computeIfAbsent(roomNo, k -> new Room());
      room.frameFirst = Math.min(room.frameFirst, f);
      room.frameLast = f;
      if (roomNo != roomPrev[0]) {
        roomPrev[0] = roomNo;
        if (room.name.isEmpty()) {
          StringBuilder nm = new StringBuilder();
          for (int i = 0; i < 32; i++) {
            int c = r.memByte(o.salasBase + roomNo * o.salaBytes + 128 + i);
            nm.append(c >= 32 && c < 127 ? (char) c : ' ');
          }
          room.name = nm.toString().trim();
          int n = 0;
          for (int i = 0; i < o.specsMax; i++) {
            int b0 = r.memByte(o.salasBase + roomNo * o.salaBytes + o.specsOffset + i * 2);
            if (b0 == 255)
              break;
            if (b0 != 0)
              n++;
          }
          room.specCount = n;
        }
      }
      int rope = r.memByte(o.willySoga);
      boolean hanging = rope >= 3 && rope <= 32;
      if (hanging && willyRopePrev[1] < 0)
        willyRopePrev[1] = f;
      else if (!hanging && willyRopePrev[1] >= 0) {
        ropeRanges.add(new int[]{willyRopePrev[1], f - 1});
        willyRopePrev[1] = -1;
      }

      // --- frames muestreados: tabla de entidades + baseline de atribucion ---
      if ((f - from) % sample != 0)
        return;
      sampled[0]++;
      room.samples++;
      int active = 0;
      Map<Long, Integer> gfxCount = new HashMap<>(); // (pagina<<8|idx) -> cuantos
      if (prevPosRoom[0] != roomNo) { // cambio de sala: las posiciones previas no comparan
        prevPosRoom[0] = roomNo;
        for (int s = 0; s < o.entSlots; s++)
          prevPos[s] = null;
      }
      for (int s = 0; s < o.entSlots; s++) {
        int base = o.entBase + s * o.entStride;
        int b0 = r.memByte(base + o.tipoOffset);
        if (b0 == o.entTerm)
          break;
        int type = b0 & o.tipoMask;
        room.typeHisto.merge(type, 1, Integer::sum);
        if (type == 0) {
          prevPos[s] = null;
          continue;
        }
        active++;
        if (type == 3)
          room.rope = true;
        if (type == 1 || type == 2) {
          int x = r.memByte(base + 2) & 31, y = r.memByte(base + 3);
          long gfx = ((long) r.memByte(base + 5) << 8) | (r.memByte(base + 2) >> 5);
          gfxCount.merge(gfx, 1, Integer::sum);
          if (prevPos[s] != null) {
            int d = Math.abs(x - prevPos[s][0]) + Math.abs(y - prevPos[s][1]);
            room.moveSum += d;
            room.moveMax = Math.max(room.moveMax, d);
            room.moveSamples++;
          }
          prevPos[s] = new int[]{x, y};
        } else
          prevPos[s] = null;
      }
      if (gfxCount.values().stream().anyMatch(c -> c >= 2))
        room.identicalPair = true;
      room.activeSum += active;

      // baseline: entradas de owner (clasificacion actual por base de catalogo) en el
      // playfield (y<128; la fila de vidas dibuja Willys abajo y no es una entidad)
      Set<Integer> owners = new HashSet<>();
      Set<Integer> routines = new HashSet<>();
      for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
        int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7);
        if (y >= 128)
          continue;
        if (r.lastWrite[i] == f - 1 && (snap.pixels()[i] & 0xff) != 0) {
          int addr = r.writeRoutine[i];
          routines.add(addr);
          if (!oracleAddrs.contains(addr))
            foreignScreen.merge(addr, 1L, Long::sum);
        }
        if (snap.owner()[i] != 0)
          owners.add(snap.owner()[i]);
      }
      for (int a : routines)
        if (oracleAddrs.contains(a))
          screenRoutines.add(a);
      room.ownerBoxesSum += owners.size();
      room.ownerActiveSamples++;

      // --- plano dir: ¿las hojas dir de cada byte con dueño caen DENTRO del slot de su
      // entidad? (la métrica §3.2 de BASE-SEMANTICA; sólo corre con -Ddir.plane=true) ---
      if (r.dir != null) {
        if (dirMemo.size() > 400_000)
          dirMemo.clear();
        room.dirSamples++;
        Set<Integer> groups = new HashSet<>(); // slots vistos; -1 = willy
        Map<Integer, Set<Integer>> slotsPerBase = new HashMap<>();
        for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
          int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7);
          if (y >= 128 || snap.owner()[i] == 0)
            continue;
          int[] lv = r.dir.leavesSorted(r.dir.mem[TaintReplay.SCREEN + i],
              TaintReplay.DIR_LEAFCAP, dirMemo);
          dirOwned[0]++;
          if (lv == null) {
            dirSaturated[0]++;
            continue;
          }
          Set<Integer> slots = new HashSet<>();
          boolean willy = false;
          for (int leaf : lv) {
            String zone;
            if (leaf >= o.entBase && leaf < o.entBase + o.entSlots * o.entStride) {
              slots.add((leaf - o.entBase) / o.entStride);
              zone = "buffer-entidades";
            } else if (leaf >= 34240 && leaf <= 34303) {
              willy = true;
              zone = "vars-willy";
            } else if (leaf >= o.salasBase + roomNo * o.salaBytes + o.specsOffset
                && leaf < o.salasBase + roomNo * o.salaBytes + o.specsOffset + o.specsMax * 2) {
              zone = "specs-de-sala";
            } else if (leaf >= 33280 && leaf < 33792) {
              zone = "tablas-82xx-83xx"; // SBL y afines: medido, $83xx aparece en salas SIN soga
            } else if (leaf < 16384) {
              zone = "rom";
            } else if (leaf >= o.salasBase) {
              zone = "data-de-salas"; // historia de transiciones: procedencia real, no identidad
            } else {
              zone = "otra";
              topLeaves.merge(leaf, 1L, Long::sum);
            }
            leafZones.merge(zone, 1L, Long::sum);
          }
          if (!slots.isEmpty() || willy)
            dirAttributed[0]++;
          if (slots.size() >= 2)
            dirMulti[0]++;
          groups.addAll(slots);
          if (willy)
            groups.add(-1);
          if (!slots.isEmpty())
            slotsPerBase.computeIfAbsent(snap.owner()[i], k -> new HashSet<>()).addAll(slots);
        }
        room.dirGroupsSum += groups.size();
        // smoke §3.1: en una sala con par identico, el grafico COMPARTIDO tiene que
        // resolverse en >=2 slots — dos guardianes identicos, dos identidades
        if (room.identicalPair) {
          room.dirPairFrames++;
          if (slotsPerBase.values().stream().anyMatch(s -> s.size() >= 2))
            room.dirPairSplit++;
        }
      }

      // ¿que region de memoria ESPEJA el display file? El que la copia deja identico es el
      // buffer de composicion: ahi es donde el plano dir tiene que ver escribir a drawSprite
      for (int c = 0; c < o.bufferCandidatos.length; c++) {
        int eq = 0;
        for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++)
          if (r.memByte(o.bufferCandidatos[c] + i) == (snap.pixels()[i] & 0xff))
            eq++;
        bufferMatch[c] += eq / (double) TaintReplay.PIXEL_BYTES;
      }
      bufferSamples[0]++;
    });
    holder[0] = replay;
    replay.paced = false;
    replay.maxFrames = from + frames;
    long t = System.currentTimeMillis();
    try {
      replay.run();
    } catch (RuntimeException e) {
      System.out.println("replay terminado: " + e.getMessage());
    }
    if (willyRopePrev[1] >= 0)
      ropeRanges.add(new int[]{willyRopePrev[1], from + frames});
    System.out.println("corrida: " + (System.currentTimeMillis() - t) / 1000 + "s, "
        + sampled[0] + " frames muestreados\n");

    System.out.println("=== rutinas del oraculo ===");
    System.out.println("  rutina          addr   ejecutada(CALL)  escribe-display");
    o.rutinas.forEach((name, addr) -> System.out.printf("  %-15s %5d %10s %12s%n", name, addr,
        seenRoutines.contains(addr) ? "SI" : o.porJP.contains(name) ? "no vista (JP)" : "NO",
        screenRoutines.contains(addr) ? "SI" : "-"));
    System.out.println("  quien escribe el display file (top 5, bytes; $0000 = la raiz, o sea"
        + " la copia buffer->pantalla del main loop):");
    foreignScreen.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(5)
        .forEach(e -> System.out.printf("    $%04x (%d) x%d%n", e.getKey(), e.getKey(),
            e.getValue()));
    System.out.println("  espejo del display file (candidatos a buffer de composicion):");
    for (int c = 0; c < o.bufferCandidatos.length; c++)
      System.out.printf("    $%04x (%d): %.1f%% de bytes identicos%n", o.bufferCandidatos[c],
          o.bufferCandidatos[c], 100.0 * bufferMatch[c] / Math.max(1, bufferSamples[0]));

    System.out.println("\n=== cobertura del RZX: salas visitadas ===");
    System.out.println("  sala nombre                            frames        specs  activos  tipos"
        + "                soga  par-identico  mov avg/max");
    rooms.forEach((no, rm) -> {
      if (rm.samples == 0)
        return;
      StringBuilder types = new StringBuilder();
      rm.typeHisto.forEach((ty, c) -> types.append(o.tipoNombre.getOrDefault(ty, "?" + ty))
          .append(String.format("x%.1f ", c / (double) Math.max(1, rm.samples))));
      System.out.printf("  %4d %-33s %6d..%-6d %5d %8.1f  %-20s %4s %8s %10.1f/%d%n",
          no, rm.name, rm.frameFirst, rm.frameLast, rm.specCount,
          rm.activeSum / (double) Math.max(1, rm.samples), types, rm.rope ? "SI" : "-",
          rm.identicalPair ? "SI" : "-",
          rm.moveSum / (double) Math.max(1, rm.moveSamples), rm.moveMax);
    });

    System.out.println("\n=== la soga ===");
    System.out.println("  salas con soga: " + rooms.entrySet().stream()
        .filter(e -> e.getValue().rope).map(Map.Entry::getKey).toList());
    System.out.println("  Willy colgado (rangos de frames): " + ropeRanges.stream()
        .map(rg -> rg[0] + ".." + rg[1]).toList());

    System.out.println("\n=== vidas ===");
    if (lifeEvents.isEmpty())
      System.out.println("  sin cambios del contador en la corrida");
    for (int[] ev : lifeEvents) {
      boolean near = false;
      for (int df = -25; df <= 25 && !near; df++)
        near = loseLifeFrames.contains(ev[0] + df);
      System.out.printf("  frame %d: %d -> %d %s%n", ev[0], ev[1], ev[2],
          ev[2] == ev[1] - 1 ? (near ? "(decremento, loseLife cerca: OK)"
              : "(decremento; loseLife no vista como CALL cerca — probablemente entra por JP)")
              : "(no es decremento)");
    }

    System.out.println("\n=== baseline de atribucion (sin plano dir) ===");
    System.out.println("  por sala: entradas owner promedio vs entidades activas + Willy."
        + "\n  El numero a batir: en salas con par identico, owner da UNA entrada por grafico"
        + "\n  compartido; el plano dir tiene que dar una por ENTIDAD.");
    rooms.forEach((no, rm) -> {
      if (rm.ownerActiveSamples == 0)
        return;
      System.out.printf("  sala %4d %-30s owner %.1f vs activos+1 %.1f %s%n", no, rm.name,
          rm.ownerBoxesSum / (double) rm.ownerActiveSamples,
          rm.activeSum / (double) Math.max(1, rm.samples) + 1,
          rm.identicalPair ? " <- PAR IDENTICO" : "");
    });
    if (holder[0].dir != null) {
      System.out.println("\n=== plano dir: atribucion contra la tabla de entidades ===");
      System.out.printf("  bytes con dueño analizados: %d · atribuidos a un slot o a Willy:"
          + " %.1f%% · con hojas de DOS slots (impureza): %.1f%% · SATURADOS: %.1f%%%n",
          dirOwned[0], 100.0 * dirAttributed[0] / Math.max(1, dirOwned[0]),
          100.0 * dirMulti[0] / Math.max(1, dirOwned[0]),
          100.0 * dirSaturated[0] / Math.max(1, dirOwned[0]));
      System.out.println("  hojas dir por zona (el histograma que dice QUE bloque discrimina):");
      long zoneTotal = leafZones.values().stream().mapToLong(Long::longValue).sum();
      leafZones.forEach((z, c) -> System.out.printf("    %-16s %10d (%.1f%%)%n", z, c,
          100.0 * c / Math.max(1, zoneTotal)));
      System.out.println("  hojas 'otra' mas frecuentes (top 10):");
      topLeaves.entrySet().stream()
          .sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).limit(10)
          .forEach(e -> System.out.printf("    $%04x (%d) x%d%n", e.getKey(), e.getKey(),
              e.getValue()));
      System.out.println("  por sala: grupos dir (slots+willy) vs activos+1, y el smoke §3.1"
          + " (par identico partido en >=2 slots):");
      rooms.forEach((no, rm) -> {
        if (rm.dirSamples == 0)
          return;
        System.out.printf("  sala %4d %-30s grupos %.1f vs %.1f %s%n", no, rm.name,
            rm.dirGroupsSum / (double) rm.dirSamples,
            rm.activeSum / (double) Math.max(1, rm.samples) + 1,
            rm.dirPairFrames == 0 ? ""
                : String.format("| par partido %d/%d muestras %s", rm.dirPairSplit,
                    rm.dirPairFrames, rm.dirPairSplit * 2 >= rm.dirPairFrames ? "OK" : "MAL"));
      });
    }

    System.out.println("\nleer asi: si 'ejecutada' da NO en alguna rutina del oraculo, el RZX"
        + "\nno corre el juego documentado (o la direccion esta mal transcripta) y NADA de lo"
        + "\ndemas vale. 'specs' vs 'activos' valida la tabla de entidades; 'mov avg' chico y"
        + "\nacotado valida los offsets de posicion. Las salas con soga y par identico son"
        + "\ndonde el smoke test del plano dir (§3.1) se juega.");
  }

  private OracleVerify() {
  }
}
