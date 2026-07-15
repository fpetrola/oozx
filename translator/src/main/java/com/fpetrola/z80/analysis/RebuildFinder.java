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

package com.fpetrola.z80.analysis;

import java.util.*;

/**
 * The "rebuilds" command: finds the SELECTOR variables of the game ("current room",
 * "current level") and the rebuild cluster each one triggers, from evidence alone:
 * <ol>
 *   <li><b>selector</b>: a single dynamic cell whose value feeds — through ADDR edges —
 *       the source address of a bulk copy, such that {@code source = base + value*stride}
 *       fits the observed ranges. The copy pulls one record of a table indexed by the
 *       cell: the cell selects WHICH content gets built.</li>
 *   <li><b>cluster</b>: every other bulk copy that runs with the same cadence (an
 *       integer multiple of the pinned copy's count — k records expanded per rebuild
 *       show up as exactly k× the executions). Together they are "what gets rebuilt
 *       when the selector changes".</li>
 *   <li><b>trigger nuance</b>: executions of the cluster vs. actual value CHANGES of
 *       the cell (from {@code frame_cells}) — a cluster that runs more often than the
 *       value changes also re-initialises on the same value (death/restart).</li>
 *   <li><b>hand-off</b>: who READS the rebuilt regions afterwards — the routines that
 *       consume the freshly built content (renderers, entity movers, text printers),
 *       i.e. where to look next to recover each region's structure.</li>
 * </ol>
 * Nothing here is game-specific: no address, stride or count is assumed.
 */
public class RebuildFinder {
  private final AnalysisDB db;
  private final String dbPath;

  public RebuildFinder(AnalysisDB db, String dbPath) {
    this.db = db;
    this.dbPath = dbPath;
  }

  /** a bulk copy whose source address is computed from a selector cell. */
  private record Pinned(int cell, AnalysisDB.Bulk bulk, int readSite, int stride, int base) {
  }

  /** all selector-driven rebuilds as data, ready for the JSON export. */
  public List<Map<String, Object>> analyze() {
    List<Pinned> pinned = new ArrayList<>();
    for (AnalysisDB.Bulk b : db.bulks.values())
      pinned.addAll(pinSelector(b));

    // group by selector cell; one finding per cell
    Map<Integer, List<Pinned>> byCell = new TreeMap<>();
    for (Pinned p : pinned)
      byCell.computeIfAbsent(p.cell(), k -> new ArrayList<>()).add(p);

    List<Map<String, Object>> out = new ArrayList<>();
    for (Map.Entry<Integer, List<Pinned>> e : byCell.entrySet())
      out.add(describe(e.getKey(), e.getValue()));
    return out;
  }

  /**
   * selector candidates for one bulk copy: walk the copy's ADDR dependencies back to
   * single-cell dynamic reads and keep those whose value RANGE explains the copy's
   * source range as {@code base + value*stride}.
   */
  private List<Pinned> pinSelector(AnalysisDB.Bulk b) {
    List<Pinned> out = new ArrayList<>();
    int srcSpan = b.srcMax() - b.srcMin();
    if (srcSpan <= 0)
      return out; // fixed source: nothing selects it
    Set<Integer> seen = new HashSet<>();
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(b.pc(), List.of()))
      if (e.src() != 0 && e.role() != null && e.role().contains("ADDR") && seen.add(e.src()))
        queue.add(e.src());
    for (int depth = 0; depth < 4 && !queue.isEmpty(); depth++) {
      int n = queue.size();
      for (int i = 0; i < n; i++) {
        int pc = queue.poll();
        AnalysisDB.Stat r = db.reads.get(pc);
        if (r != null && r.addrMin() == r.addrMax() && isMutableCell(r.addrMin())) {
          int span = r.valMax() - r.valMin();
          if (span > 0) {
            int stride = Math.round((float) srcSpan / span);
            boolean fits = stride >= 2 && Math.abs(srcSpan - stride * span) <= stride / 4;
            double cadence = (double) b.count() / r.count();
            if (fits && cadence >= 0.2 && cadence <= 5.0)
              out.add(new Pinned(r.addrMin(), b, pc, stride, b.srcMin() - r.valMin() * stride));
          }
        }
        for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of()))
          if (e.src() != 0 && seen.add(e.src()))
            queue.add(e.src());
      }
    }
    return out;
  }

  /** a cell some small-range write site rewrites (wide clears do not count as writers). */
  private boolean isMutableCell(int addr) {
    for (AnalysisDB.Stat w : db.writes.values())
      if (w.addrMin() <= addr && addr <= w.addrMax() && w.addrMax() - w.addrMin() <= 4)
        return true;
    return false;
  }

  private Map<String, Object> describe(int cell, List<Pinned> copies) {
    copies.sort(Comparator.comparingLong(p -> -p.bulk().count()));
    Pinned main = copies.get(0);
    long mainCount = main.bulk().count();

    Map<String, Object> f = new LinkedHashMap<>();
    AnalysisDB.Stat sel = db.reads.get(main.readSite());
    Map<String, Object> selector = new LinkedHashMap<>();
    selector.put("celda", cell);
    selector.put("valores", List.of(sel.valMin(), sel.valMax()));
    long[] cambios = cellChanges(cell);
    if (cambios[0] > 0) {
      selector.put("cambios_de_valor", cambios[0]);
      selector.put("valores_distintos", cambios[1]);
    }
    selector.put("escrito_por", writersOf(cell));
    f.put("selector", selector);

    List<Object> principales = new ArrayList<>();
    for (Pinned p : copies) {
      AnalysisDB.Bulk b = p.bulk();
      int registros = (b.srcMax() - b.srcMin()) / p.stride() + 1;
      Map<String, Object> c = new LinkedHashMap<>();
      c.put("site", b.pc());
      c.put("rutina", db.method.getOrDefault(b.pc(), "?"));
      c.put("veces", b.count());
      c.put("formula", "origen = " + p.base() + " + selector*" + p.stride());
      c.put("tabla_indexada", Map.of(
          "base", p.base(), "registro_bytes", p.stride(), "registros_usados", registros,
          "rango", List.of(b.srcMin(), b.srcMax() + Math.max(0, b.lenMax() - 1))));
      c.put("destino", List.of(b.dstMin(), b.dstMax() + Math.max(0, b.lenMax() - 1)));
      principales.add(c);
    }
    f.put("copias_indexadas_por_el_selector", principales);

    if (cambios[0] > 0 && mainCount > cambios[0] * 3 / 2)
      f.put("nota_disparo", "la reconstruccion corrio x" + mainCount + " pero el selector cambio de valor "
          + cambios[0] + " veces: tambien se re-ejecuta sin cambiar el valor (muerte/reinicio)");

    // the rest of the cluster: bulk copies running an integer multiple of the main cadence
    Set<Integer> pinnedPcs = new HashSet<>();
    for (Pinned p : copies)
      pinnedPcs.add(p.bulk().pc());
    List<Object> cluster = new ArrayList<>();
    List<int[]> regiones = new ArrayList<>();
    for (Pinned p : copies)
      regiones.add(new int[]{p.bulk().dstMin(), p.bulk().dstMax() + Math.max(0, p.bulk().lenMax() - 1)});
    for (AnalysisDB.Bulk b : db.bulks.values()) {
      if (pinnedPcs.contains(b.pc()))
        continue;
      long k = Math.round((double) b.count() / mainCount);
      if (k < 1 || k > 16 || Math.abs(b.count() - k * mainCount) > k * mainCount / 10)
        continue;
      Map<String, Object> c = new LinkedHashMap<>();
      c.put("site", b.pc());
      c.put("rutina", db.method.getOrDefault(b.pc(), "?"));
      c.put("veces", b.count());
      c.put("por_reconstruccion", k);
      c.put("origen", List.of(b.srcMin(), b.srcMax() + Math.max(0, b.lenMax() - 1)));
      c.put("destino", List.of(b.dstMin(), b.dstMax() + Math.max(0, b.lenMax() - 1)));
      // the LDIR fill idiom: source = destination - 1 propagates one byte over the block
      if (b.srcMin() + 1 == b.dstMin() && b.srcMax() < b.dstMax() + b.lenMax())
        c.put("nota", "relleno con un valor (origen = destino-1: propaga un byte)");
      cluster.add(c);
      regiones.add(new int[]{b.dstMin(), b.dstMax() + Math.max(0, b.lenMax() - 1)});
    }
    if (!cluster.isEmpty())
      f.put("corren_con_la_misma_cadencia", cluster);

    List<int[]> merged = GameMapper.mergeRanges(regiones, 16);
    f.put("regiones_reconstruidas", merged.stream()
        .map(r -> List.of(r[0], r[1])).toList());

    // hand-off: who consumes each rebuilt region afterwards (where structure recovery goes next)
    Map<String, Object> lectores = new LinkedHashMap<>();
    for (int[] r : merged)
      lectores.put("[" + r[0] + ".." + r[1] + "]", readersOf(r[0], r[1], pinnedPcs));
    f.put("destino_leido_por", lectores);
    return f;
  }

  /** small-range write sites covering the cell: who changes the selector, with what values. */
  private List<Object> writersOf(int cell) {
    List<Object> out = new ArrayList<>();
    db.writes.values().stream()
        .filter(w -> w.addrMin() <= cell && cell <= w.addrMax() && w.addrMax() - w.addrMin() <= 4)
        .sorted(Comparator.comparingLong(w -> -w.count()))
        .limit(8)
        .forEach(w -> out.add(Map.of(
            "site", w.pc(), "rutina", db.method.getOrDefault(w.pc(), "?"),
            "veces", w.count(), "valores", List.of(w.valMin(), w.valMax()))));
    return out;
  }

  /** routines reading [lo..hi], heaviest first. */
  private List<Object> readersOf(int lo, int hi, Set<Integer> exclude) {
    Map<String, long[]> porRutina = new TreeMap<>(); // rutina -> {lecturas, sites}
    for (AnalysisDB.Stat r : db.reads.values()) {
      if (r.addrMax() < lo || r.addrMin() > hi || exclude.contains(r.pc()))
        continue;
      long[] acc = porRutina.computeIfAbsent(db.method.getOrDefault(r.pc(), "?"), k -> new long[2]);
      acc[0] += r.count();
      acc[1]++;
    }
    return porRutina.entrySet().stream()
        .sorted((a, b2) -> Long.compare(b2.getValue()[0], a.getValue()[0]))
        .limit(6)
        .map(e -> (Object) Map.of("rutina", e.getKey(),
            "lecturas", e.getValue()[0], "sites", e.getValue()[1]))
        .toList();
  }

  /** value changes of the cell over the whole replay, from the per-frame track log. */
  private long[] cellChanges(int cell) {
    try (java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbPath);
         java.sql.PreparedStatement ps = c.prepareStatement(
             "SELECT COUNT(*), COUNT(DISTINCT val) FROM frame_cells WHERE addr = ?")) {
      ps.setInt(1, cell);
      try (java.sql.ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return new long[]{rs.getLong(1), rs.getLong(2)};
      }
    } catch (java.sql.SQLException ignored) {
    }
    return new long[]{0, 0};
  }

  // ---------- text rendering ----------
  @SuppressWarnings("unchecked")
  public void report() {
    List<Map<String, Object>> all = analyze();
    if (all.isEmpty()) {
      System.out.println("sin selectores detectados (ninguna copia en bloque depende de una celda dinamica)");
      return;
    }
    for (Map<String, Object> f : all) {
      Map<String, Object> sel = (Map<String, Object>) f.get("selector");
      List<Integer> vals = (List<Integer>) sel.get("valores");
      System.out.printf("%n===== SELECTOR mem[%d]  val[%d..%d]%s =====%n",
          (int) sel.get("celda"), vals.get(0), vals.get(1),
          sel.containsKey("cambios_de_valor")
              ? "  (" + sel.get("cambios_de_valor") + " cambios, "
              + sel.get("valores_distintos") + " valores distintos)" : "");
      for (Object wo : (List<Object>) sel.get("escrito_por")) {
        Map<String, Object> w = (Map<String, Object>) wo;
        List<Integer> wv = (List<Integer>) w.get("valores");
        System.out.printf("  escrito por %s @%s x%s val[%d..%d]%n",
            w.get("rutina"), w.get("site"), w.get("veces"), wv.get(0), wv.get(1));
      }
      for (Object co : (List<Object>) f.get("copias_indexadas_por_el_selector")) {
        Map<String, Object> c = (Map<String, Object>) co;
        Map<String, Object> t = (Map<String, Object>) c.get("tabla_indexada");
        List<Integer> dst = (List<Integer>) c.get("destino");
        System.out.printf("  COPIA @%s (%s) x%s: %s  — tabla de %s registros de %s bytes -> destino [%d..%d]%n",
            c.get("site"), c.get("rutina"), c.get("veces"), c.get("formula"),
            t.get("registros_usados"), t.get("registro_bytes"), dst.get(0), dst.get(1));
      }
      if (f.containsKey("nota_disparo"))
        System.out.println("  NOTA: " + f.get("nota_disparo"));
      if (f.containsKey("corren_con_la_misma_cadencia")) {
        System.out.println("  corren con la misma cadencia (parte de la reconstruccion):");
        for (Object co : (List<Object>) f.get("corren_con_la_misma_cadencia")) {
          Map<String, Object> c = (Map<String, Object>) co;
          List<Integer> src = (List<Integer>) c.get("origen"), dst = (List<Integer>) c.get("destino");
          System.out.printf("    @%s (%s) x%s (%sx por reconstruccion): [%d..%d] -> [%d..%d]%s%n",
              c.get("site"), c.get("rutina"), c.get("veces"), c.get("por_reconstruccion"),
              src.get(0), src.get(1), dst.get(0), dst.get(1),
              c.containsKey("nota") ? "  <- " + c.get("nota") : "");
        }
      }
      System.out.println("  regiones reconstruidas: " + f.get("regiones_reconstruidas"));
      for (Map.Entry<String, Object> le : ((Map<String, Object>) f.get("destino_leido_por")).entrySet()) {
        System.out.println("  " + le.getKey() + " leido despues por:");
        for (Object ro : (List<Object>) le.getValue()) {
          Map<String, Object> r = (Map<String, Object>) ro;
          System.out.printf("    %s (x%s lecturas, %s sites)%n",
              r.get("rutina"), r.get("lecturas"), r.get("sites"));
        }
      }
    }
  }
}
