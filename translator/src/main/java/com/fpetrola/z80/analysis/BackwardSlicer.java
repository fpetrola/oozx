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
 * Walks data-flow edges backwards from the sites that write a memory range, composing
 * the per-site equations into information-flow chains. Stops at INIT (original game
 * data) and IO (player input) roots.
 */
public class BackwardSlicer {
  private final AnalysisDB db;

  public BackwardSlicer(AnalysisDB db) {
    this.db = db;
  }

  public void slice(int addrLo, int addrHi, int maxDepth, int fanout) {
    slice(addrLo, addrHi, maxDepth, fanout, null);
  }

  /**
   * roleFilter (null = todos): "ADDR" | "VAL" | "COND" — filtra los edges entrantes del
   * ROOT por rol (cómo se construyó la dirección vs. el valor vs. la condición); los
   * niveles más profundos se recorren completos, con el rol etiquetado en cada edge.
   */
  public void slice(int addrLo, int addrHi, int maxDepth, int fanout, String roleFilter) {
    List<AnalysisDB.Stat> writers = db.writersIntersecting(addrLo, addrHi);
    System.out.println("=== Backward slice de mem[" + addrLo + ".." + addrHi + "] ==="
        + (roleFilter != null ? " [rol=" + roleFilter + "]" : ""));
    System.out.println(writers.size() + " write-sites tocan el rango\n");
    for (AnalysisDB.Stat w : writers) {
      System.out.println("ROOT " + db.describe(w.pc()));
      walk(w.pc(), 1, maxDepth, fanout, new HashSet<>(List.of(w.pc())), roleFilter);
      System.out.println();
    }
  }

  private void walk(int pc, int depth, int maxDepth, int fanout, Set<Integer> seen, String roleFilter) {
    if (depth > maxDepth)
      return;
    List<AnalysisDB.Edge> in = db.edgesIn.getOrDefault(pc, List.of());
    int shown = 0;
    for (AnalysisDB.Edge e : in) {
      if (roleFilter != null && (e.role() == null || !e.role().contains(roleFilter)))
        continue;
      if (shown++ >= fanout)
        break;
      String indent = "  ".repeat(depth);
      String label = e.label();
      System.out.println(indent + "<- x" + e.count() + (label.isEmpty() ? "" : " [" + label + "]")
          + " " + db.describe(e.src()));
      if (e.src() != 0 && !db.ioSites.contains(e.src()) && seen.add(e.src()))
        walk(e.src(), depth + 1, maxDepth, fanout, seen, null);
    }
  }
}
