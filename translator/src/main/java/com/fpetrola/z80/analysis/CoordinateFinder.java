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
 * F5 (targeted) step 1 — automatic discovery, from the aggregate DB alone, of everything
 * the tracked re-run needs. No game knowledge: it all derives from stats, bulk-copy
 * chains and ADDR-role edges.
 * <ul>
 *   <li><b>screen-like regions</b>: the ZX screen plus every buffer that gets bulk-copied
 *       into it (transitively), each with the address delta that maps it onto the real
 *       screen, so a draw into a backbuffer decodes to the same (x, y);</li>
 *   <li><b>draw methods</b>: methods owning write-sites that hit those regions;</li>
 *   <li><b>watch cells</b>: small mutable RAM ranges whose reads feed the ADDRESS
 *       computation of those writes — the coordinate/state variables of the game;</li>
 *   <li><b>lookup tables</b>: INIT-only (static) ranges on the same address paths.</li>
 * </ul>
 */
public class CoordinateFinder {
  public static final int SCREEN_LO = 16384, PIXELS_HI = 22527, ATTR_HI = 23295;
  private static final int MAX_DEPTH = 6;
  private static final int MAX_RANGE_SIZE = 128;
  private static final int MAX_CELLS = 1024;

  /** addresses [lo..hi] map onto the real screen as addr + delta. */
  public record Region(int lo, int hi, int delta) {
    public boolean contains(int a) {
      return a >= lo && a <= hi;
    }
  }

  public record Plan(List<Region> regions,
                     SortedMap<Integer, String> drawMethods,  // entry pc -> method name
                     Set<Integer> drawWriteSites,
                     Map<Integer, Integer> siteMethodEntry,   // write site -> method entry pc
                     int[] watchCells,
                     List<int[]> watchRanges,
                     List<int[]> lookupTables) {
  }

  private final AnalysisDB db;

  public CoordinateFinder(AnalysisDB db) {
    this.db = db;
  }

  public Plan find() {
    List<Region> regions = screenLikeRegions();

    // draw sites & methods: writers whose observed range intersects a screen-like region
    Map<String, Integer> minPcByMethod = new HashMap<>();
    db.method.forEach((pc, m) -> minPcByMethod.merge(m, pc, Math::min));
    SortedMap<Integer, String> drawMethods = new TreeMap<>();
    Set<Integer> drawWriteSites = new TreeSet<>();
    Map<Integer, Integer> siteMethodEntry = new HashMap<>();
    for (AnalysisDB.Stat w : db.writes.values()) {
      if (w.pc() == 0)
        continue;
      boolean hits = regions.stream().anyMatch(r -> w.addrMax() >= r.lo() && w.addrMin() <= r.hi());
      if (!hits)
        continue;
      String m = db.method.get(w.pc());
      if (m == null)
        continue;
      int entry = entryPcOf(m, minPcByMethod);
      drawMethods.put(entry, m);
      drawWriteSites.add(w.pc());
      siteMethodEntry.put(w.pc(), entry);
    }

    // BFS backwards from each draw site: first hop restricted to ADDR-role edges (we only
    // care how the ADDRESS was built), then everything, collecting the read-sites crossed.
    Set<Integer> addrReads = new TreeSet<>();
    for (int wSite : drawWriteSites) {
      Set<Integer> seen = new HashSet<>();
      ArrayDeque<int[]> queue = new ArrayDeque<>();
      for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(wSite, List.of()))
        if (e.src() != 0 && e.role() != null && e.role().contains("ADDR") && seen.add(e.src()))
          queue.add(new int[]{e.src(), 1});
      while (!queue.isEmpty()) {
        int[] cur = queue.poll();
        if (db.reads.containsKey(cur[0]))
          addrReads.add(cur[0]);
        if (cur[1] >= MAX_DEPTH)
          continue;
        for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(cur[0], List.of()))
          if (e.src() != 0 && seen.add(e.src()))
            queue.add(new int[]{e.src(), cur[1] + 1});
      }
    }

    // classify each read range: small + mutable -> watch; never written -> static lookup table
    List<int[]> watchRanges = new ArrayList<>(), lookupTables = new ArrayList<>();
    for (int s : addrReads) {
      AnalysisDB.Stat r = db.reads.get(s);
      int lo = r.addrMin(), hi = r.addrMax();
      if (lo < SCREEN_LO)
        continue; // ROM
      final int flo = lo, fhi = hi;
      if (regions.stream().anyMatch(reg -> fhi >= reg.lo() && flo <= reg.hi()))
        continue; // the draw buffers themselves
      boolean mutable = !db.writersIntersecting(lo, hi).isEmpty()
          || db.bulks.values().stream().anyMatch(b ->
          b.dstMax() + Math.max(0, b.lenMax() - 1) >= flo && b.dstMin() <= fhi);
      if (mutable && hi - lo + 1 <= MAX_RANGE_SIZE)
        watchRanges.add(new int[]{lo, hi});
      else if (!mutable)
        lookupTables.add(new int[]{lo, hi});
    }
    watchRanges = merge(watchRanges, 8);
    lookupTables = merge(lookupTables, 8);

    int[] cells = watchRanges.stream()
        .flatMapToInt(rg -> java.util.stream.IntStream.rangeClosed(rg[0], rg[1]))
        .distinct().sorted().limit(MAX_CELLS).toArray();

    return new Plan(regions, drawMethods, drawWriteSites, siteMethodEntry, cells, watchRanges, lookupTables);
  }

  /** screen + fixpoint over bulk copies whose destination lands in an already-known region. */
  private List<Region> screenLikeRegions() {
    List<Region> regions = new ArrayList<>();
    regions.add(new Region(SCREEN_LO, ATTR_HI, 0));
    boolean changed = true;
    for (int guard = 0; changed && guard < 8; guard++) {
      changed = false;
      for (AnalysisDB.Bulk b : db.bulks.values()) {
        int dstLo = b.dstMin(), dstHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
        int srcLo = b.srcMin(), srcHi = b.srcMax() + Math.max(0, b.lenMax() - 1);
        if (srcLo < 0 || srcHi > 0xFFFF)
          continue;
        for (Region r : List.copyOf(regions)) {
          if (dstHi < r.lo() || dstLo > r.hi())
            continue;
          Region nr = new Region(srcLo, srcHi, r.delta() + (b.dstMin() - b.srcMin()));
          if (regions.stream().noneMatch(x -> x.lo() == nr.lo() && x.hi() == nr.hi() && x.delta() == nr.delta())) {
            regions.add(nr);
            changed = true;
          }
          break; // first matching region decides the delta
        }
      }
    }
    return regions;
  }

  private static int entryPcOf(String method, Map<String, Integer> minPcByMethod) {
    if (method.startsWith("$"))
      try {
        return Integer.parseInt(method.substring(1));
      } catch (NumberFormatException ignored) {
      }
    return minPcByMethod.getOrDefault(method, -1);
  }

  /** merges overlapping or near (gap <= maxGap) ranges. */
  private static List<int[]> merge(List<int[]> ranges, int maxGap) {
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
}
