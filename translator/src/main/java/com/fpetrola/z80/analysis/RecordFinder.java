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
 * The "records" command: recovers the FIELD LAYOUT of a singleton record — a fixed
 * block that a selector-driven copy rebuilds (the "current room/level" block found by
 * {@link RebuildFinder}). Unlike the arrays that {@link StructFinder} walks with a
 * cursor, a singleton is accessed with absolute addresses, so its fields come from
 * grouping every read/write whose observed range falls inside the block:
 * <ul>
 *   <li><b>packed maps</b>: several read sites over the SAME swept range whose consumers
 *       rotate the byte (rlc/rrc) = k sub-cells of 8/k bits per byte (a tile layout);</li>
 *   <li><b>periodic families</b>: single-cell fields at addresses whose pairwise
 *       distances share a divisor, inside or beside a swept range = an inner table of
 *       stride-N records (tile definitions: attribute + graphic rows);</li>
 *   <li><b>links</b>: a field whose value flows into the WRITE of a selector cell is an
 *       exit — the record names its neighbours, and the records form a graph;</li>
 *   <li><b>pointers/flags/ascii</b>: 2-byte fields feeding ADDR are pointers (with the
 *       region they point into), 0/1 fields are flags, swept ranges whose values stay
 *       in [32..127] are probable text;</li>
 *   <li><b>gaps</b>: sub-ranges no typed site touches, listed with the generic routines
 *       whose aggregated ranges overlap them (e.g. a text printer that reads many
 *       sources through one site) — where to look next.</li>
 * </ul>
 * Everything is generic: no address or field meaning is assumed.
 */
public class RecordFinder {
  private final AnalysisDB db;
  private final String dbPath;

  public RecordFinder(AnalysisDB db, String dbPath) {
    this.db = db;
    this.dbPath = dbPath;
  }

  /** all rebuilt singleton records with their recovered fields. */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> analyze() {
    List<Map<String, Object>> rebuilds = new RebuildFinder(db, dbPath).analyze();
    // every selector cell known, to recognise exit-links from any record
    Set<Integer> selectorCells = new TreeSet<>();
    for (Map<String, Object> f : rebuilds)
      selectorCells.add((Integer) ((Map<String, Object>) f.get("selector")).get("celda"));

    List<Map<String, Object>> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (Map<String, Object> f : rebuilds) {
      int cell = (Integer) ((Map<String, Object>) f.get("selector")).get("celda");
      for (Object co : (List<Object>) f.get("copias_indexadas_por_el_selector")) {
        Map<String, Object> c = (Map<String, Object>) co;
        List<Integer> dst = (List<Integer>) c.get("destino");
        if (!seen.add(dst.get(0) + ".." + dst.get(1)))
          continue;
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("rango", dst);
        rec.put("selector", cell);
        rec.put("plantilla", c.get("tabla_indexada"));
        rec.put("campos", fieldsOf(dst.get(0), dst.get(1), selectorCells));
        rec.put("huecos", gapsOf(dst.get(0), dst.get(1),
            (List<Map<String, Object>>) rec.get("campos")));
        out.add(rec);
      }
    }
    return out;
  }

  /** one field per distinct observed access range inside the block, sorted by address. */
  private List<Map<String, Object>> fieldsOf(int lo, int hi, Set<Integer> selectorCells) {
    // group sites by their exact observed range
    Map<Long, List<AnalysisDB.Stat>> byRange = new TreeMap<>();
    for (Map<Integer, AnalysisDB.Stat> side : List.of(db.reads, db.writes))
      for (AnalysisDB.Stat s : side.values())
        if (s.addrMin() >= lo && s.addrMax() <= hi)
          byRange.computeIfAbsent(((long) s.addrMin() << 17) | ((long) s.addrMax() << 1)
              | (s.op().equals("W") ? 1 : 0), k -> new ArrayList<>()).add(s);
    // fold R and W of the same range into one field
    Map<Long, Map<String, Object>> fields = new TreeMap<>();
    byRange.forEach((key, stats) -> {
      long rangeKey = key >> 1;
      Map<String, Object> f = fields.computeIfAbsent(rangeKey, k -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("rango", List.of(stats.get(0).addrMin(), stats.get(0).addrMax()));
        m.put("bytes", stats.get(0).addrMax() - stats.get(0).addrMin() + 1);
        m.put("lecturas", 0L);
        m.put("escrituras", 0L);
        m.put("valores", new ArrayList<>(List.of(256, -1)));
        return m;
      });
      long reads = 0, writes = 0;
      List<Integer> vr = (List<Integer>) f.get("valores");
      for (AnalysisDB.Stat s : stats) {
        if (s.op().equals("W"))
          writes += s.count();
        else
          reads += s.count();
        vr.set(0, Math.min(vr.get(0), s.valMin()));
        vr.set(1, Math.max(vr.get(1), s.valMax()));
      }
      f.put("lecturas", (long) f.get("lecturas") + reads);
      f.put("escrituras", (long) f.get("escrituras") + writes);
      String who = stats.stream().map(s -> db.method.getOrDefault(s.pc(), "?"))
          .distinct().sorted().reduce((a, b) -> a + " " + b).orElse("");
      String kk = stats.get(0).op().equals("W") ? "escrito_por" : "leido_por";
      f.merge(kk, who, (a, b) -> a + " " + b);
      // remember the read sites for tag derivation
      if (!stats.get(0).op().equals("W")) {
        List<Integer> pcs = (List<Integer>) f.computeIfAbsent("_readPcs", k -> new ArrayList<Integer>());
        for (AnalysisDB.Stat s : stats)
          pcs.add(s.pc());
      }
    });

    List<Map<String, Object>> campos = new ArrayList<>(fields.values());
    for (Map<String, Object> f : campos)
      tagField(f, selectorCells);
    periodicFamilies(campos);
    campos.forEach(f -> f.remove("_readPcs"));
    return campos;
  }

  /** semantic tags of one field, derived from where its reads flow. */
  @SuppressWarnings("unchecked")
  private void tagField(Map<String, Object> f, Set<Integer> selectorCells) {
    List<Integer> rango = (List<Integer>) f.get("rango");
    List<Integer> val = (List<Integer>) f.get("valores");
    int bytes = (int) f.get("bytes");
    List<Integer> readPcs = (List<Integer>) f.getOrDefault("_readPcs", List.of());
    List<String> tags = new ArrayList<>();

    // packed map: k read sites over the same swept range, consumers rotating the byte
    if (bytes > 1 && readPcs.size() > 1) {
      boolean rotado = readPcs.stream().anyMatch(pc ->
          db.edgesOut.getOrDefault(pc, List.of()).stream().anyMatch(e -> {
            String eq = db.equation.get(e.dst());
            return eq != null && (eq.contains("rlc(") || eq.contains("rrc("));
          }) || hasRotationAfter(pc));
      if (rotado) {
        int k = readPcs.size();
        tags.add("mapa empaquetado: " + k + " lecturas por byte con rotaciones = "
            + k + " celdas de " + (8 / k) + " bits por byte ("
            + bytes * k + " celdas en total)");
        f.put("nombre_propuesto", "mapa_empaquetado");
      }
    }
    // where the value lands
    boolean addr = false, cond = false;
    Integer linkSelector = null;
    int[] target = null;
    for (int pc : readPcs) {
      for (AnalysisDB.Edge e : db.edgesOut.getOrDefault(pc, List.of())) {
        String role = e.role() == null ? "" : e.role();
        if (role.contains("ADDR")) {
          addr = true;
          AnalysisDB.Stat t = db.reads.get(e.dst()) != null ? db.reads.get(e.dst()) : db.writes.get(e.dst());
          if (t != null)
            target = new int[]{t.addrMin(), t.addrMax()};
        }
        if (role.contains("COND"))
          cond = true;
        AnalysisDB.Stat w = db.writes.get(e.dst());
        if (w != null && w.addrMin() == w.addrMax() && selectorCells.contains(w.addrMin()))
          linkSelector = w.addrMin();
      }
    }
    if (linkSelector != null) {
      tags.add("su valor se escribe en el selector mem[" + linkSelector
          + "]: es un ENLACE hacia otro registro (los registros forman un grafo)");
      f.putIfAbsent("nombre_propuesto", "enlace_a_registro_vecino");
    }
    if (addr && bytes == 2) {
      tags.add("puntero de 16 bits" + (target != null
          ? " hacia [" + target[0] + ".." + target[1] + "]" : ""));
      f.putIfAbsent("nombre_propuesto", "puntero");
    } else if (addr)
      tags.add("alimenta direcciones (indice/puntero)");
    if (cond)
      tags.add("decide ramas (se compara)");
    if (val.get(0) >= 0 && val.get(1) <= 1 && bytes == 1) {
      tags.add("solo 0/1: flag");
      f.putIfAbsent("nombre_propuesto", "flag");
    }
    if (bytes >= 4 && val.get(0) >= 32 && val.get(1) <= 127) {
      tags.add("valores siempre en [32..127]: texto ASCII probable");
      f.putIfAbsent("nombre_propuesto", "texto");
    }
    if (!tags.isEmpty())
      f.put("etiquetas", tags);
  }

  /** rotation applied right at the read site's own statement (A = mem[HL]; A = rlc(A)). */
  private boolean hasRotationAfter(int pc) {
    for (int d = 1; d <= 2; d++) {
      String eq = db.equation.get(pc + d);
      if (eq != null && (eq.contains("rlc(") || eq.contains("rrc(")))
        return true;
    }
    return false;
  }

  /**
   * single-cell fields whose pairwise distances share a divisor > 1, adjacent to a swept
   * field = an inner table of stride-N records (attribute byte + payload). The classic
   * shape: tile definitions of 9 bytes, the attribute read individually and the 8
   * graphic rows swept by the renderer.
   */
  @SuppressWarnings("unchecked")
  private void periodicFamilies(List<Map<String, Object>> campos) {
    // widest sweep first: overlapping sweeps describe the same family and the widest
    // one sees every record; the singles keep a single "cabecera" tag
    List<Map<String, Object>> porTamanio = new ArrayList<>(campos);
    porTamanio.sort(Comparator.comparingInt(f -> -(int) f.get("bytes")));
    for (Map<String, Object> sweep : porTamanio) {
      int sweepBytes = (int) sweep.get("bytes");
      if (sweepBytes <= 8)
        continue; // the payload is a wide swept range
      List<Integer> sr = (List<Integer>) sweep.get("rango");
      // header candidates: single-cell fields inside the sweep or just before it
      List<Map<String, Object>> singles = campos.stream()
          .filter(f -> (int) f.get("bytes") == 1)
          .filter(f -> {
            int a = ((List<Integer>) f.get("rango")).get(0);
            return a >= sr.get(0) - 8 && a <= sr.get(1);
          })
          .sorted(Comparator.comparingInt(f -> ((List<Integer>) f.get("rango")).get(0)))
          .toList();
      if (singles.size() < 2)
        continue;
      int base = ((List<Integer>) singles.get(0).get("rango")).get(0);
      int g = 0;
      for (Map<String, Object> f : singles)
        g = gcd(g, ((List<Integer>) f.get("rango")).get(0) - base);
      if (g < 2 || g > 64)
        continue;
      int registros = (sr.get(1) - base) / g + 1;
      for (Map<String, Object> f : singles) {
        int a = ((List<Integer>) f.get("rango")).get(0);
        List<String> tags = (List<String>) f.computeIfAbsent("etiquetas", k -> new ArrayList<String>());
        if (tags.stream().anyMatch(s -> s.startsWith("cabecera del registro")))
          continue; // already described by a wider sweep of the same family
        tags.add("cabecera del registro " + ((a - base) / g) + " de la familia periodica: "
            + registros + " registros de " + g + " bytes desde " + base);
        f.putIfAbsent("nombre_propuesto", "cabecera_registro_" + ((a - base) / g));
      }
      ((List<String>) sweep.computeIfAbsent("etiquetas", k -> new ArrayList<String>()))
          .add("carga util de la familia periodica: " + (g - 1)
              + " bytes por registro tras la cabecera (paso " + g + ", " + registros + " registros)");
      sweep.putIfAbsent("nombre_propuesto", "datos_de_registros_de_" + g + "_bytes");
    }
  }

  private static int gcd(int a, int b) {
    return b == 0 ? a : gcd(b, a % b);
  }

  /** untyped gaps of the block, with the generic wide-range readers that overlap them. */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> gapsOf(int lo, int hi, List<Map<String, Object>> campos) {
    boolean[] cubierto = new boolean[hi - lo + 1];
    for (Map<String, Object> f : campos) {
      List<Integer> r = (List<Integer>) f.get("rango");
      for (int a = r.get(0); a <= r.get(1); a++)
        cubierto[a - lo] = true;
    }
    List<Map<String, Object>> huecos = new ArrayList<>();
    int start = -1;
    for (int i = 0; i <= cubierto.length; i++) {
      boolean c = i < cubierto.length && cubierto[i];
      if (!c && i < cubierto.length && start < 0)
        start = i;
      if ((c || i == cubierto.length) && start >= 0) {
        if (i - start >= 4) {
          int gLo = lo + start, gHi = lo + i - 1;
          Set<String> genericos = new TreeSet<>();
          for (AnalysisDB.Stat s : db.reads.values())
            if (s.addrMin() < gLo && s.addrMax() >= gLo || s.addrMin() <= gHi && s.addrMax() > gHi)
              if (s.addrMax() >= gLo && s.addrMin() <= gHi)
                genericos.add(db.method.getOrDefault(s.pc(), "?"));
          Map<String, Object> hueco = new LinkedHashMap<>();
          hueco.put("rango", List.of(gLo, gHi));
          hueco.put("nota", "sin acceso directo tipado");
          if (!genericos.isEmpty())
            hueco.put("lectores_genericos", new ArrayList<>(genericos));
          huecos.add(hueco);
        }
        start = -1;
      }
    }
    return huecos;
  }

  // ---------- text rendering ----------
  @SuppressWarnings("unchecked")
  public void report() {
    List<Map<String, Object>> all = analyze();
    if (all.isEmpty()) {
      System.out.println("sin registros singleton (no hay bloques reconstruidos por selector)");
      return;
    }
    for (Map<String, Object> rec : all) {
      List<Integer> r = (List<Integer>) rec.get("rango");
      Map<String, Object> t = (Map<String, Object>) rec.get("plantilla");
      System.out.printf("%n##### REGISTRO SINGLETON [%d..%d]  (selector mem[%s]; plantilla: %s registros de %s bytes) #####%n",
          r.get(0), r.get(1), rec.get("selector"), t.get("registros_usados"), t.get("registro_bytes"));
      for (Map<String, Object> f : (List<Map<String, Object>>) rec.get("campos")) {
        List<Integer> fr = (List<Integer>) f.get("rango");
        List<Integer> val = (List<Integer>) f.get("valores");
        System.out.printf("  [%d..%d] %db  R x%d W x%d val[%d..%d]  %s%n",
            fr.get(0), fr.get(1), (int) f.get("bytes"),
            (long) f.get("lecturas"), (long) f.get("escrituras"), val.get(0), val.get(1),
            f.getOrDefault("nombre_propuesto", ""));
        if (f.containsKey("leido_por"))
          System.out.println("      leido por: " + f.get("leido_por"));
        if (f.containsKey("escrito_por"))
          System.out.println("      escrito por: " + f.get("escrito_por"));
        if (f.containsKey("etiquetas"))
          for (String tag : (List<String>) f.get("etiquetas"))
            System.out.println("      <- " + tag);
      }
      for (Map<String, Object> h : (List<Map<String, Object>>) rec.get("huecos")) {
        List<Integer> hr = (List<Integer>) h.get("rango");
        System.out.printf("  [%d..%d] HUECO: %s%s%n", hr.get(0), hr.get(1), h.get("nota"),
            h.containsKey("lectores_genericos")
                ? "; lectores genericos: " + h.get("lectores_genericos") : "");
      }
    }
  }
}
