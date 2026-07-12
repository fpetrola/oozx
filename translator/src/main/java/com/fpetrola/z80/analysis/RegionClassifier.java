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
 * Classifies memory by access-pattern evidence (doc/GUIA-ANALISIS-ECUACIONES.md, section 6):
 * single-address sites become named variables (flag / counter / state), ranged reads with
 * INIT provenance become static data tables, per-frame writers become buffers, etc.
 */
public class RegionClassifier {
  private final AnalysisDB db;
  private final int totalFrames;

  public RegionClassifier(AnalysisDB db, int totalFrames) {
    this.db = db;
    this.totalFrames = totalFrames;
  }

  public void report() {
    System.out.println("=== Variables (direccion unica) ===");
    Map<Integer, List<String>> vars = new TreeMap<>();
    for (AnalysisDB.Stat w : db.writes.values()) {
      if (w.addrMin() != w.addrMax())
        continue;
      String cls = classifyVar(w);
      vars.computeIfAbsent(w.addrMin(), k -> new ArrayList<>())
          .add(String.format("W por %d (x%d) val[%d..%d]%s eq: %s",
              w.pc(), w.count(), w.valMin(), w.valMax(), cls, shortEq(w.pc())));
    }
    vars.forEach((addr, lst) -> {
      System.out.println("  " + addr + ":");
      lst.forEach(s -> System.out.println("    " + s));
    });

    System.out.println("\n=== Tablas de datos estaticos (lecturas con provenance INIT) ===");
    List<int[]> staticRanges = new ArrayList<>();
    for (AnalysisDB.Stat r : db.reads.values()) {
      if (r.addrMin() == r.addrMax())
        continue;
      boolean initRooted = db.edgesIn.getOrDefault(r.pc(), List.of()).stream()
          .anyMatch(e -> e.src() == 0);
      if (initRooted)
        staticRanges.add(new int[]{r.addrMin(), r.addrMax(), r.pc(), (int) Math.min(r.count(), Integer.MAX_VALUE)});
    }
    staticRanges.sort(Comparator.comparingInt(a -> a[0]));
    for (int[] t : staticRanges)
      System.out.printf("  [%5d..%5d] leida por %d (x%d) %s%n", t[0], t[1], t[2], t[3], shortEq(t[2]));

    System.out.println("\n=== Buffers por frame (writes ~1x/frame o mas) ===");
    for (AnalysisDB.Stat w : db.writes.values()) {
      if (w.addrMin() == w.addrMax() || totalFrames <= 0)
        continue;
      double perFrame = (double) w.count() / totalFrames;
      if (perFrame >= 0.5)
        System.out.printf("  [%5d..%5d] site %d: %.1f writes/frame %s%n",
            w.addrMin(), w.addrMax(), w.pc(), perFrame, shortEq(w.pc()));
    }

    System.out.println("\n=== Branches mas ejecutados (decisiones del juego) ===");
    db.cfgOut.entrySet().stream()
        .filter(e -> e.getValue().size() > 1 && db.branchSites.contains(e.getKey()))
        .sorted(Comparator.comparingLong(e -> -e.getValue().stream().mapToLong(AnalysisDB.Edge::count).sum()))
        .limit(15)
        .forEach(e -> {
          long total = e.getValue().stream().mapToLong(AnalysisDB.Edge::count).sum();
          StringBuilder sb = new StringBuilder();
          for (AnalysisDB.Edge x : e.getValue())
            sb.append(String.format(" ->%d (%.1f%%)", x.dst(), 100.0 * x.count() / total));
          System.out.println("  " + e.getKey() + sb + "  eq: " + shortEq(e.getKey()));
        });
  }

  private String classifyVar(AnalysisDB.Stat w) {
    String eq = db.equation.getOrDefault(w.pc(), "");
    if (w.valMin() >= 0 && w.valMax() <= 1)
      return " [FLAG]";
    if (eq.contains("inc(") || eq.contains("dec("))
      return " [CONTADOR]";
    return "";
  }

  private String shortEq(int pc) {
    String eq = db.equation.getOrDefault(pc, "");
    return eq.length() > 70 ? eq.substring(0, 70) + "..." : eq;
  }
}
