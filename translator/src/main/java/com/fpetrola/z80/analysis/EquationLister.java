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
 * Prints the normalized algebraic equations of the capture as an annotated listing:
 * one line per site (Z80 pc) with dynamic execution count and observed W/R ranges.
 * The equations come from sites.json (extractor pass 1.7, varN chains inlined).
 */
public class EquationLister {
  private final AnalysisDB db;

  public EquationLister(AnalysisDB db) {
    this.db = db;
  }

  /** methodFilter (ej: "$37974", con o sin '$') o null; [lo..hi] rango de pcs. */
  public void report(String methodFilter, int lo, int hi) {
    String filter = methodFilter == null ? null
        : methodFilter.startsWith("$") ? methodFilter : "$" + methodFilter;
    List<Integer> pcs = new ArrayList<>(db.equation.keySet());
    Collections.sort(pcs);
    String lastMethod = null;
    int shown = 0;
    for (int pc : pcs) {
      if (pc < lo || pc > hi)
        continue;
      String m = db.method.get(pc);
      if (filter != null && !filter.equals(m))
        continue;
      if (m != null && !m.equals(lastMethod)) {
        System.out.println("\n== " + m + " ==");
        lastMethod = m;
      }
      long exec = db.cfgOut.getOrDefault(pc, List.of()).stream().mapToLong(AnalysisDB.Edge::count).sum();
      StringBuilder ann = new StringBuilder();
      AnalysisDB.Stat w = db.writes.get(pc);
      if (w != null)
        ann.append("  | W[").append(w.addrMin()).append("..").append(w.addrMax())
            .append("]=").append(w.valMin()).append("..").append(w.valMax());
      AnalysisDB.Stat r = db.reads.get(pc);
      if (r != null)
        ann.append("  | R[").append(r.addrMin()).append("..").append(r.addrMax())
            .append("]=").append(r.valMin()).append("..").append(r.valMax());
      AnalysisDB.Bulk b = db.bulks.get(pc);
      if (b != null)
        ann.append("  | BULK[").append(b.srcMin()).append("..").append(b.srcMax())
            .append("]->[").append(b.dstMin()).append("..").append(b.dstMax()).append(']');
      if (db.ioSites.contains(pc))
        ann.append("  | IO");
      String eq = db.equation.get(pc);
      if (eq.length() > 110)
        eq = eq.substring(0, 110) + "...";
      System.out.printf("  %5d %-9s %s%s%n", pc, exec > 0 ? "x" + exec : "", eq, ann);
      shown++;
    }
    if (shown == 0)
      System.out.println("(sin ecuaciones para ese filtro; probar sin filtro o con otro metodo/rango)");
  }
}
