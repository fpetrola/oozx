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
 * Automatic sprite/graphics discovery (doc/GUIA-ANALISIS-ECUACIONES.md, section 6):
 * 1) find the sites that write into the ZX screen (16384..22527) — the draw routines;
 * 2) walk data-flow backwards to reads whose source is INIT — static graphics data;
 * 3) report the graphics zones with the equations that consume them.
 */
public class SpriteFinder {
  public static final int SCREEN_LO = 16384, SCREEN_HI = 22527;
  public static final int PIXELS_HI = 22527; // 16384..22527 includes attributes 22528.. excluded

  private final AnalysisDB db;

  public SpriteFinder(AnalysisDB db) {
    this.db = db;
  }

  public void report() {
    List<AnalysisDB.Stat> drawers = db.writersIntersecting(SCREEN_LO, PIXELS_HI);
    System.out.println("=== Rutinas de dibujado (write-sites que tocan pantalla) ===\n");
    for (AnalysisDB.Stat w : drawers) {
      System.out.println("DRAW " + db.describe(w.pc()));
      // graphic data feeding this drawer: INIT-rooted read sites reachable backwards
      Map<Integer, long[]> gfxReads = new TreeMap<>();
      collectInitReads(w.pc(), 0, new HashSet<>(List.of(w.pc())), gfxReads);
      for (Map.Entry<Integer, long[]> g : gfxReads.entrySet()) {
        AnalysisDB.Stat r = db.reads.get(g.getKey());
        if (r == null)
          continue;
        System.out.printf("   GFX data: mem[%d..%d] leida por site %d (x%d) stride/bits=%s%n",
            r.addrMin(), r.addrMax(), r.pc(), r.count(), bitsInfo(r));
      }
      System.out.println();
    }

    // bulk copies into the screen: backbuffer -> screen pipelines
    System.out.println("=== Bulk copies hacia pantalla ===");
    for (AnalysisDB.Bulk b : db.bulks.values())
      if (b.dstMax() >= SCREEN_LO && b.dstMin() <= 22527 + 768)
        System.out.println("  " + db.describe(b.pc()));
  }

  /** walks backwards collecting read-sites that have an INIT edge (original data). */
  private void collectInitReads(int pc, int depth, Set<Integer> seen, Map<Integer, long[]> out) {
    if (depth > 4)
      return;
    for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of())) {
      int src = e.src();
      if (src == 0)
        continue;
      boolean fromInit = db.edgesIn.getOrDefault(src, List.of()).stream().anyMatch(x -> x.src() == 0);
      if (fromInit && db.reads.containsKey(src))
        out.computeIfAbsent(src, k -> new long[]{0})[0] += e.count();
      if (seen.add(src))
        collectInitReads(src, depth + 1, seen, out);
    }
  }

  private static String bitsInfo(AnalysisDB.Stat r) {
    int fixed = r.addrAnd() & ~(r.addrAnd() ^ r.addrOr());
    int varying = r.addrAnd() ^ r.addrOr();
    return "fijos=" + Integer.toBinaryString(fixed) + " variables=" + Integer.toBinaryString(varying);
  }
}
