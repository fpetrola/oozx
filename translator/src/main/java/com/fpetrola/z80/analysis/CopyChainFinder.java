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
 * Chains bulk copies whose destination overlaps another's source: the memory pipelines
 * of the game (level data -> backbuffer -> screen, attribute tables, etc.).
 */
public class CopyChainFinder {
  private final AnalysisDB db;

  public CopyChainFinder(AnalysisDB db) {
    this.db = db;
  }

  public void report() {
    List<AnalysisDB.Bulk> all = new ArrayList<>(db.bulks.values());
    all.sort(Comparator.comparingInt(AnalysisDB.Bulk::dstMin));
    System.out.println("=== Bulk copies (ldir) ===");
    for (AnalysisDB.Bulk b : all)
      System.out.printf("  %5d: x%-6d [%5d..%5d] -> [%5d..%5d] len[%d..%d]%n",
          b.pc(), b.count(), b.srcMin(), b.srcMax(), b.dstMin(), b.dstMax(), b.lenMin(), b.lenMax());

    System.out.println("\n=== Cadenas (dst de uno solapa src de otro) ===");
    for (AnalysisDB.Bulk a : all)
      for (AnalysisDB.Bulk b : all) {
        if (a.pc() == b.pc())
          continue;
        int aDstEnd = a.dstMax() + a.lenMax();
        int bSrcEnd = b.srcMax() + b.lenMax();
        boolean overlap = a.dstMin() <= bSrcEnd && b.srcMin() <= aDstEnd;
        if (overlap)
          System.out.printf("  %d [%d..%d]->[%d..%d]  ==>  %d [%d..%d]->[%d..%d]%n",
              a.pc(), a.srcMin(), a.srcMax(), a.dstMin(), a.dstMax(),
              b.pc(), b.srcMin(), b.srcMax(), b.dstMin(), b.dstMax());
      }
  }
}
