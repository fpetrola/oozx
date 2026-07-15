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

import com.fpetrola.z80.analysis.query.Db;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The "map" command: the full screen-writing map of the game in one automatic report,
 * no manual linking. It composes everything the pipeline already derives:
 * <ol>
 *   <li>buffers and the copies that compose them into the screen;</li>
 *   <li>draw methods with what they actually draw (sizes from sprite_draws);</li>
 *   <li>static graphics regions (VAL-side, cassette data) with the DYNAMIC cells that
 *       select which graphic gets drawn (their ADDR-side feeders);</li>
 *   <li>static lookup tables (ADDR-side);</li>
 *   <li>the entity record structure: base/stride/slots from the validated coordinate
 *       pairs, offsets of X/Y/graphic-selector inside the record, the copies that load
 *       it and the methods that update it;</li>
 *   <li>the remaining dynamic variables feeding screen writes.</li>
 * </ol>
 * Needs the "track" tables (coord_pairs/frame_cells); runs the track pipeline first if
 * they are missing.
 */
public class GameMapper {
  private static final int MAX_DEPTH = 6;

  private final AnalysisDB db;
  private final Explainer explainer;
  private final CoordinateFinder.Plan plan;
  private final List<double[]> pairRows = new ArrayList<>();     // xAddr, yAddr, rate
  private final Map<Integer, String> pairDesc = new HashMap<>(); // xAddr -> printable pair

  public GameMapper(AnalysisDB db, String dbPath) {
    this.db = db;
    this.explainer = new Explainer(db, dbPath);
    this.plan = new CoordinateFinder(db).find();
    try (Db q = new Db(dbPath)) {
      q.forEach("SELECT x_addr, x_transform, x_off, y_addr, y_transform, y_off, rate FROM coord_pairs ORDER BY x_addr",
          rs -> {
            pairRows.add(new double[]{rs.getInt(1), rs.getInt(4), rs.getDouble(7)});
            pairDesc.put(rs.getInt(1), String.format("X=mem[%d] (%s%+d), Y=mem[%d] (%s%+d), conf %.0f%%",
                rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getString(5), rs.getInt(6),
                rs.getDouble(7) * 100));
          });
    }
  }

  public void report(String dbPath) {
    System.out.println("=== MAPA AUTOMATICO DE LA ESCRITURA EN PANTALLA ===\n");
    buffers();
    drawMethods(dbPath);
    Set<Integer> valReads = reads("VAL"), addrReads = reads("ADDR");
    List<int[]> lookupTables = lookupTables(addrReads, valReads);
    graphics(valReads, lookupTables);
    lookups(lookupTables, addrReads);
    entities();
    dynamicVars();
  }

  // ---------- 1. buffers ----------
  private void buffers() {
    System.out.println("== 1. BUFFERS (composicion hacia la pantalla) ==");
    Set<String> seen = new HashSet<>();
    for (CoordinateFinder.Region r : plan.regions()) {
      if (r.delta() % 256 != 0 || !seen.add(r.lo() + ".." + r.hi()))
        continue; // deltas no alineados = rellenos/scrolls, no buffers
      String tag = r.delta() == 0 ? " PANTALLA (pixels 16384..22527, atributos 22528..23295)"
          : "  -> pantalla con delta " + r.delta();
      if (r.delta() != 0 && explainer.classifyRange(r.lo(), r.hi()).startsWith("STATIC"))
        tag += "  (fuente ESTATICA del cassette, no un buffer)";
      StringBuilder sb = new StringBuilder(String.format("  [%d..%d]%s", r.lo(), r.hi(), tag));
      for (AnalysisDB.Bulk b : db.bulks.values()) {
        int dstHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
        if (dstHi < r.lo() || b.dstMin() > r.hi() || b.srcMin() >= r.lo() && b.srcMin() <= r.hi())
          continue; // fills (src dentro del mismo buffer) no son composicion
        sb.append(String.format("%n      <- copia x%d desde [%d..%d] %s", b.count(),
            b.srcMin(), b.srcMax() + b.lenMax() - 1, place(b.pc())));
      }
      System.out.println(sb);
    }
    System.out.println();
  }

  // ---------- 2. draw methods ----------
  private void drawMethods(String dbPath) {
    System.out.println("== 2. RUTINAS DE DIBUJADO ==");
    // most common cluster size per method/kind, from the track run
    Map<String, long[]> top = new HashMap<>(); // "entry|kind" -> {count, w, h, total}
    try (Db q = new Db(dbPath)) {
      q.forEach("SELECT method, kind, w, h, COUNT(*) FROM sprite_draws GROUP BY 1,2,3,4", rs -> {
        String key = rs.getInt(1) + "|" + rs.getString(2);
        long n = rs.getLong(5);
        long[] cur = top.get(key);
        if (cur == null)
          top.put(key, new long[]{n, rs.getInt(3), rs.getInt(4), n});
        else {
          cur[3] += n;
          if (n > cur[0]) {
            cur[0] = n;
            cur[1] = rs.getInt(3);
            cur[2] = rs.getInt(4);
          }
        }
      });
    }
    for (Map.Entry<Integer, String> m : plan.drawMethods().entrySet()) {
      List<Integer> sites = plan.drawWriteSites().stream()
          .filter(s -> plan.siteMethodEntry().get(s) == m.getKey().intValue()).toList();
      long ops = sites.stream().mapToLong(s -> db.writes.get(s).count()).sum();
      StringBuilder sb = new StringBuilder(String.format("  %s: %d write-sites, x%d", m.getValue(), sites.size(), ops));
      for (String kind : new String[]{"P", "A"}) {
        long[] t = top.get(m.getKey() + "|" + kind);
        if (t != null)
          sb.append(String.format("  | %s: %d dibujados, tipico %dx%d",
              kind.equals("P") ? "pixels" : "atributos", t[3], t[1], t[2]));
      }
      System.out.println(sb);
    }
    System.out.println();
  }

  // ---------- BFS por rol desde los draw sites ----------
  private Set<Integer> reads(String role) {
    return roleReads(db, plan, role);
  }

  /** read-sites reachable backwards from the draw sites, first hop restricted to a role. */
  public static Set<Integer> roleReads(AnalysisDB db, CoordinateFinder.Plan plan, String role) {
    Set<Integer> out = new TreeSet<>();
    for (int w : plan.drawWriteSites()) {
      Set<Integer> seen = new HashSet<>();
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(w, List.of()))
        if (e.src() != 0 && e.role() != null && e.role().contains(role) && seen.add(e.src()))
          queue.add(new int[]{e.src(), 1});
      while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        if (db.reads.containsKey(cur[0]))
          out.add(cur[0]);
        if (cur[1] >= MAX_DEPTH)
          continue;
        for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(cur[0], List.of()))
          if (e.src() != 0 && seen.add(e.src()))
            queue.add(new int[]{e.src(), cur[1] + 1});
      }
    }
    return out;
  }

  private boolean isStaticish(int lo, int hi) {
    String c = explainer.classifyRange(lo, hi);
    return c.startsWith("STATIC") || c.startsWith("mostly") || c.startsWith("MIXED");
  }

  // ---------- 3. graphics ----------
  private void graphics(Set<Integer> valReads, List<int[]> lookupTables) {
    System.out.println("== 3. GRAFICOS ESTATICOS (cassette -> pantalla, lado VALOR) ==");
    List<int[]> regions = mergeRanges(valReads.stream()
        .map(db.reads::get)
        .filter(r -> r.addrMax() - r.addrMin() + 1 >= 16 && isStaticish(r.addrMin(), r.addrMax()))
        // las tablas de consulta chicas tambien viajan como VALOR hacia registros de
        // direccion: van en la seccion 4, no aca
        .filter(r -> lookupTables.stream().noneMatch(t -> t[1] - t[0] + 1 < 1024
            && r.addrMin() >= t[0] - 16 && r.addrMax() <= t[1] + 16))
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 64);
    for (int[] g : regions) {
      System.out.println("  [" + g[0] + ".." + g[1] + "] " + explainer.classifyRange(g[0], g[1]));
      // consumers
      Map<String, Long> byMethod = new TreeMap<>();
      Set<Integer> regionReaders = new HashSet<>();
      for (int s : valReads) {
        AnalysisDB.Stat r = db.reads.get(s);
        if (r.addrMax() >= g[0] && r.addrMin() <= g[1]) {
          regionReaders.add(s);
          byMethod.merge(db.nameOf(s), r.count(), Long::sum);
        }
      }
      byMethod.forEach((m, n) -> System.out.println("      leida por " + m + " x" + n));
      // selectors: dynamic small cells feeding the ADDRESS of these reads
      List<int[]> sel = selectorsOf(regionReaders);
      for (int[] s : sel)
        System.out.println("      selector dinamico del grafico: mem[" + s[0] + ".." + s[1] + "]");
    }
    System.out.println();
  }

  /** dynamic small ranges feeding the ADDR channels of the given read sites. */
  private List<int[]> selectorsOf(Set<Integer> readSites) {
    Set<Integer> found = new TreeSet<>();
    for (int rs : readSites) {
      Set<Integer> seen = new HashSet<>();
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(rs, List.of()))
        if (e.src() != 0 && e.role() != null && e.role().contains("ADDR") && seen.add(e.src()))
          queue.add(new int[]{e.src(), 1});
      while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        AnalysisDB.Stat r = db.reads.get(cur[0]);
        if (r != null && r.addrMin() >= 16384 && r.addrMax() - r.addrMin() + 1 <= 64
            && !isStaticish(r.addrMin(), r.addrMax())
            && !inScreenRegions(r.addrMin(), r.addrMax()))
          found.add(cur[0]);
        if (cur[1] >= 4)
          continue;
        for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(cur[0], List.of()))
          if (e.src() != 0 && seen.add(e.src()))
            queue.add(new int[]{e.src(), cur[1] + 1});
      }
    }
    return mergeRanges(found.stream().map(db.reads::get)
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 8);
  }

  // ---------- 4. lookup tables ----------
  private List<int[]> lookupTables(Set<Integer> addrReads, Set<Integer> valReads) {
    return mergeRanges(addrReads.stream()
        .map(db.reads::get)
        .filter(r -> isStaticish(r.addrMin(), r.addrMax()))
        .filter(r -> r.addrMax() - r.addrMin() + 1 < 1024) // las zonas grandes son graficos
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 16);
  }

  private void lookups(List<int[]> tables, Set<Integer> addrReads) {
    System.out.println("== 4. TABLAS DE CONSULTA ESTATICAS (lado DIRECCION) ==");
    for (int[] t : tables) {
      Set<String> methods = addrReads.stream()
          .filter(s -> db.reads.get(s).addrMax() >= t[0] && db.reads.get(s).addrMin() <= t[1])
          .map(s -> db.nameOf(s)).collect(Collectors.toCollection(TreeSet::new));
      System.out.println("  [" + t[0] + ".." + t[1] + "] usada por " + String.join(" ", methods));
    }
    System.out.println();
  }

  // ---------- 5. entity structure ----------
  private void entities() {
    System.out.println("== 5. ESTRUCTURA DE ENTIDADES (pares (X,Y) validados por track) ==");
    // best confidence-weighted stride run over the X addresses of the STRONG pairs
    // (noise pairs can line up by chance at odd strides, so neither "first found" nor
    // "longest" work: the true record table maximizes the summed confidence)
    List<Integer> xs = pairRows.stream().filter(p -> p[2] >= 0.10)
        .map(p -> (int) p[0]).sorted().distinct().toList();
    Map<Integer, Double> weights = new HashMap<>();
    for (double[] p : pairRows)
      if (p[2] >= 0.10)
        weights.merge((int) p[0], p[2], Double::sum);
    Set<Integer> inRun = new HashSet<>();
    int[] run = bestStrideRun(xs, weights);
    if (run != null) {
      int x0 = run[0], stride = run[1], bestLen = run[2];
      for (int k = 0; k < bestLen; k++)
        inRun.add(x0 + k * stride);
      // base and full size from the copies that load the table
      int base = x0, slots = bestLen;
      List<String> loaders = new ArrayList<>();
      for (AnalysisDB.Bulk b : db.bulks.values()) {
        int dstHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
        if (dstHi >= x0 && b.dstMin() <= x0 + (bestLen - 1) * stride) {
          base = Math.min(base, b.dstMin());
          slots = Math.max(slots, (dstHi - base + 1) / stride);
          loaders.add(String.format("copia x%d desde [%d..%d] (%s) %s", b.count(), b.srcMin(),
              b.srcMax() + b.lenMax() - 1, explainer.classifyRange(b.srcMin(), b.srcMax() + b.lenMax() - 1),
              place(b.pc())));
        }
      }
      System.out.printf("  TABLA base=%d, registros de %d bytes, %d slots:%n", base, stride, slots);
      System.out.printf("    +%d = X, +%d = Y (validado en %d slots)%n",
          (x0 - base) % stride, (pairY(x0) - base) % stride, bestLen);
      for (int k = 0; k < bestLen; k++)
        System.out.println("      slot en " + (x0 + k * stride) + ": " + pairDesc.get(x0 + k * stride));
      // updaters
      Map<String, Long> upd = new TreeMap<>();
      for (AnalysisDB.Stat w : db.writersIntersecting(base, base + slots * stride - 1))
        upd.merge(db.nameOf(w.pc()), w.count(), Long::sum);
      upd.forEach((m, n) -> System.out.println("    actualizada por " + m + " x" + n));
      loaders.forEach(l -> System.out.println("    cargada por " + l));
    }
    // singles (e.g. the player) and weak leftovers
    pairRows.stream()
        .filter(p -> !inRun.contains((int) p[0]))
        .sorted(Comparator.comparingDouble(p -> -p[2]))
        .forEach(p -> System.out.println("  " + (p[2] >= 0.10 ? "SPRITE INDIVIDUAL: " : "par debil (posible ruido): ")
            + pairDesc.get((int) p[0])));
    System.out.println();
  }

  private int pairY(int xAddr) {
    return pairRows.stream().filter(p -> (int) p[0] == xAddr).findFirst()
        .map(p -> (int) p[1]).orElse(xAddr);
  }

  // ---------- 6. dynamic vars ----------
  private void dynamicVars() {
    System.out.println("== 6. VARIABLES DINAMICAS que alimentan la escritura en pantalla ==");
    Set<Integer> coordCells = new HashSet<>();
    for (double[] p : pairRows) {
      coordCells.add((int) p[0]);
      coordCells.add((int) p[1]);
    }
    for (int[] rg : plan.watchRanges()) {
      String note = "";
      boolean hasCoords = coordCells.stream().anyMatch(a -> a >= rg[0] && a <= rg[1]);
      if (hasCoords)
        note = " (contiene coordenadas de sprites, ver seccion 5)";
      System.out.println("  mem[" + rg[0] + ".." + rg[1] + "]" + note
          + "  — serie completa por frame en frame_cells");
    }
    System.out.println();
  }

  // ---------- helpers ----------
  private boolean inScreenRegions(int lo, int hi) {
    return plan.regions().stream().anyMatch(r -> hi >= r.lo() && lo <= r.hi());
  }

  /**
   * best strided run over sorted addresses, weighted: the TRUE record table maximizes
   * the sum of pair confidences, not the run length (noise pairs can line up by chance
   * at odd strides). Returns {start, stride, len} or null.
   */
  public static int[] bestStrideRun(List<Integer> sortedAddrs, Map<Integer, Double> weightByAddr) {
    int bestStart = -1, bestStride = 0, bestLen = 0;
    double bestW = -1;
    for (int start : sortedAddrs)
      for (int stride = 2; stride <= 32; stride++) {
        int len = 1;
        double w = weightByAddr.getOrDefault(start, 0.0);
        while (sortedAddrs.contains(start + len * stride)) {
          w += weightByAddr.getOrDefault(start + len * stride, 0.0);
          len++;
        }
        if (len >= 2 && w > bestW) {
          bestW = w;
          bestLen = len;
          bestStride = stride;
          bestStart = start;
        }
      }
    return bestLen >= 2 ? new int[]{bestStart, bestStride, bestLen} : null;
  }

  public static List<int[]> mergeRanges(List<int[]> ranges, int maxGap) {
    List<int[]> sorted = new ArrayList<>(ranges);
    sorted.sort(Comparator.comparingInt(a -> a[0]));
    List<int[]> out = new ArrayList<>();
    for (int[] r : sorted) {
      if (!out.isEmpty() && r[0] <= out.get(out.size() - 1)[1] + maxGap + 1)
        out.get(out.size() - 1)[1] = Math.max(out.get(out.size() - 1)[1], r[1]);
      else
        out.add(new int[]{r[0], r[1]});
    }
    return out;
  }

  private String place(int pc) {
    String m = db.method.get(pc);
    return "@" + pc + (m != null ? " [" + m + "]" : "");
  }

  /** ensures the track tables exist, running the track pipeline if they are missing. */
  public static void ensureTracked(String dbPath, String rzxPath) throws Exception {
    boolean hasTrack;
    try (Db q = new Db(dbPath)) {
      hasTrack = q.hasTable("coord_pairs");
    }
    if (!hasTrack) {
      System.out.println("Faltan las tablas de track: corriendo el pipeline completo primero...\n");
      SpriteTracker.run(dbPath, rzxPath);
      System.out.println();
    }
  }

  public static void run(String dbPath, String rzxPath) throws Exception {
    ensureTracked(dbPath, rzxPath);
    new GameMapper(new AnalysisDB(dbPath), dbPath).report(dbPath);
  }
}
