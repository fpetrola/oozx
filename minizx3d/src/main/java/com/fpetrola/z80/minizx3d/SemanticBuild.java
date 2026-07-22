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
 * Etapa 3 de la base semántica: deriva {@code instances} y {@code deps} de la capa 1
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
public final class SemanticBuild {

  private static final int SALAS = 49152, SALA_BYTES = 256, SPECS_OFF = 240, SPECS_MAX = 8;
  private static final int WILLY_LO = 34240, WILLY_HI = 34303;
  private static final int DEFS_LO = 40960, DEFS_HI = 43007;
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

  /** una instancia acumulando sus eventos; slot -1 = Willy. */
  private static final class Inst {
    final TreeSet<Integer> frames = new TreeSet<>();
    final Set<Integer> gfx = new TreeSet<>();
    final Set<Integer> defLeaves = new TreeSet<>();
    int events;
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

    Map<Integer, Inst> insts = new TreeMap<>(); // clave: pairAddr; -1 = Willy
    // candidatos a follower(willy -> par): frame -> par -> ventana $83xx que ese evento
    // de willy arrastra. El vinculo esta VIVO si (a) hay evento de soga-SOLA cerca y las
    // ventanas coinciden (tras soltarse la de willy queda congelada y dejan de coincidir),
    // o (b) NO hay soga-sola cerca y el evento fusionado lleva par + ventana: la soga no
    // se dibuja sola exactamente porque willy va arriba (misma invocacion, §plan).
    Map<Integer, Map<Integer, Set<Integer>>> willyCand = new TreeMap<>();
    Map<Integer, TreeMap<Integer, Set<Integer>>> ropeWin = new TreeMap<>();
    // co-ocurrencia par-par (contaminación o dependencia guardián-guardián): conteo
    Map<Long, Integer> pairPair = new HashMap<>();
    long events = 0, multiPair = 0;

    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT frame_first, frame_last, dir_core_set, gfx_base, bytes FROM draw_events"
            + " WHERE dir_core_set IS NOT NULL AND routine != 0 AND bytes <= 512")) {
      while (rs.next()) {
        int f0 = rs.getInt(1), f1 = rs.getInt(2), gfx = rs.getInt(4);
        int[] lv = sets.get(rs.getInt(3));
        if (lv == null)
          continue;
        events++;
        Set<Integer> pairs = new TreeSet<>();
        Set<Integer> defs = new TreeSet<>();
        Set<Integer> win = new TreeSet<>();
        boolean willy = false;
        for (int leaf : lv) {
          if (leaf >= SALAS) {
            int off = leaf % SALA_BYTES;
            if (off >= SPECS_OFF && off < SPECS_OFF + SPECS_MAX * 2)
              pairs.add(leaf - (off - SPECS_OFF) % 2);
          } else if (leaf >= WILLY_LO && leaf <= WILLY_HI)
            willy = true;
          else if (leaf >= DEFS_LO && leaf <= DEFS_HI)
            defs.add(leaf);
          else if (leaf >= 33536 && leaf < 33792)
            win.add(leaf);
        }
        if (!willy && pairs.size() == 1 && win.size() >= 6)
          ropeWin.computeIfAbsent(pairs.iterator().next(), k -> new TreeMap<>())
              .put(f0, win);
        if (willy && pairs.isEmpty() || !willy && pairs.size() == 1) {
          int key = willy ? -1 : pairs.iterator().next();
          Inst in = insts.computeIfAbsent(key, k -> new Inst());
          in.events++;
          for (int f = f0; f <= f1; f += Math.max(1, (f1 - f0) / Math.max(1, rs.getInt(5))))
            in.frames.add(f);
          in.frames.add(f0);
          in.frames.add(f1);
          if (gfx != 0)
            in.gfx.add(gfx);
          in.defLeaves.addAll(defs);
        } else if (willy) {
          for (int f = f0; f <= f1; f++) {
            Map<Integer, Set<Integer>> byPair =
                willyCand.computeIfAbsent(f, k -> new HashMap<>());
            for (int p : pairs)
              byPair.merge(p, win, (a, b) -> {
                Set<Integer> u = new TreeSet<>(a);
                u.addAll(b);
                return u;
              });
          }
          // el evento de willy también alimenta su instancia
          Inst in = insts.computeIfAbsent(-1, k -> new Inst());
          in.events++;
          in.frames.add(f0);
          in.frames.add(f1);
        } else if (pairs.size() >= 2) {
          multiPair++;
          List<Integer> ps = new ArrayList<>(pairs);
          for (int i = 0; i < ps.size(); i++)
            for (int j = i + 1; j < ps.size(); j++)
              pairPair.merge(((long) ps.get(i) << 32) | ps.get(j), 1, Integer::sum);
        }
      }
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
      for (int[] ep : epochs) {
        insI.setInt(1, pairAddr);
        insI.setInt(2, epoch++);
        insI.setInt(3, pairAddr);
        insI.setInt(4, 2);
        insI.setInt(5, pairAddr < 0 ? -1 : (pairAddr - SALAS) / SALA_BYTES);
        insI.setInt(6, pairAddr < 0 ? -1 : (pairAddr % SALA_BYTES - SPECS_OFF) / 2);
        insI.setInt(7, ep[0]);
        insI.setInt(8, ep[1]);
        insI.setInt(9, in.events);
        insI.setString(10, in.gfx.toString());
        insI.setString(11, pairAddr < 0 ? "willy-vars" : "spec-par; defs=" + in.defLeaves);
        insI.addBatch();
        instRows++;
      }
    }
    insI.executeBatch();

    // --- deps: follower(willy -> par) sostenido ---
    PreparedStatement insD = conn.prepareStatement("INSERT INTO deps VALUES (?,?,?,?,?,?,?)");
    int depRows = 0;
    Map<Integer, List<int[]>> depRanges = new TreeMap<>();
    Map<Integer, int[]> open = new HashMap<>(); // par -> {first, last, count}
    for (Map.Entry<Integer, Map<Integer, Set<Integer>>> e : willyCand.entrySet()) {
      int f = e.getKey();
      for (Map.Entry<Integer, Set<Integer>> pe : e.getValue().entrySet()) {
        int pair = pe.getKey();
        Set<Integer> ww = pe.getValue();
        // vivo ⟺ la soga se dibuja SOLA cerca (si no, willy está en otra sala arrastrando
        // hojas viejas: medido 624 a 1) Y su ventana actual solapa la que willy arrastra
        // ≥25% (colgado 124 casos vs 11 libres; los eventos sin ventana no dicen nada y
        // el DEP_GAP los puentea)
        TreeMap<Integer, Set<Integer>> rw = ropeWin.get(pair);
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
        int[] r = open.get(pair);
        if (r != null && f - r[1] <= DEP_GAP) {
          r[1] = f;
          r[2]++;
        } else {
          if (r != null && r[2] >= DEP_TICKS)
            depRanges.computeIfAbsent(pair, k -> new ArrayList<>()).add(r);
          open.put(pair, new int[]{f, f, 1});
        }
      }
    }
    for (Map.Entry<Integer, int[]> e : open.entrySet())
      if (e.getValue()[2] >= DEP_TICKS)
        depRanges.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).add(e.getValue());
    for (Map.Entry<Integer, List<int[]>> e : depRanges.entrySet())
      for (int[] r : e.getValue()) {
        insD.setInt(1, r[0]);
        insD.setInt(2, r[1]);
        insD.setInt(3, -1);
        insD.setInt(4, e.getKey());
        insD.setString(5, "follower");
        insD.setDouble(6, r[2] / Math.max(1.0, (r[1] - r[0]) / 4.0));
        insD.setString(7, "willy lleva hojas del par $" + Integer.toHexString(e.getKey()));
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
    // instancias indexadas por frame (rangos de época)
    List<int[]> instRanges = new ArrayList<>(); // {first, last, pairIdx, room}
    try (var st = conn.createStatement(); ResultSet rs = st.executeQuery(
        "SELECT frame_first, frame_last, pair_idx, room FROM instances WHERE slot >= 0")) {
      while (rs.next())
        instRanges.add(new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)});
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
      for (int[] r : instRanges)
        if (f >= r[0] && f <= r[1])
          found.add(r[2]);
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
    for (Map.Entry<Integer, List<int[]>> e : depRanges.entrySet())
      for (int[] r : e.getValue())
        for (int f = r[0]; f <= r[1]; f++) {
          depFrames.add(f);
          depFramesByPair.computeIfAbsent(e.getKey(), k -> new TreeSet<>()).add(f);
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

    // 3. cero aristas guardian-guardian sostenidas (co-ocurrencias multi-par)
    System.out.printf("  co-ocurrencias par-par (candidatas a falso positivo): %d pares"
        + " distintos, top:%n", pairPair.size());
    pairPair.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue())).limit(5)
        .forEach(e -> System.out.printf("    $%x-$%x x%d%n", e.getKey() >>> 32,
            e.getKey() & 0xffffffffL, e.getValue()));
    conn.close();
  }

  private SemanticBuild() {
  }
}
