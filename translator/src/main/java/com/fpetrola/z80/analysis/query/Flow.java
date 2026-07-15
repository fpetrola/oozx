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

package com.fpetrola.z80.analysis.query;

import com.fpetrola.z80.analysis.AnalysisDB;

import java.util.*;
import java.util.function.IntPredicate;

/**
 * The dataflow walk every analyzer hand-rolls: a bounded BFS over the provenance edges,
 * optionally restricted by ROLE on the first hop (where roles are precise — deeper hops
 * mix roles because the aggregation loses per-instance pairing). Backward answers
 * "where did this come from", forward "what does this feed".
 *
 * <pre>
 *   Flow.back(db).from(bulkPc).firstHop("ADDR").depth(4).reads().singleCell()...
 * </pre>
 */
public final class Flow {
  private final AnalysisDB db;
  private final boolean backward;
  private final List<Integer> roots = new ArrayList<>();
  private String firstHopRole;
  private int depth = 4;
  private IntPredicate prune = pc -> false;

  private Flow(AnalysisDB db, boolean backward) {
    this.db = db;
    this.backward = backward;
  }

  public static Flow back(AnalysisDB db) {
    return new Flow(db, true);
  }

  public static Flow forward(AnalysisDB db) {
    return new Flow(db, false);
  }

  public Flow from(int pc) {
    roots.add(pc);
    return this;
  }

  public Flow from(Collection<Integer> pcs) {
    roots.addAll(pcs);
    return this;
  }

  /** only follow first-hop edges whose role contains this tag (ADDR / VAL / COND). */
  public Flow firstHop(String role) {
    this.firstHopRole = role;
    return this;
  }

  public Flow depth(int d) {
    this.depth = d;
    return this;
  }

  /** matching nodes are still reported but not expanded through. */
  public Flow pruneAt(IntPredicate p) {
    this.prune = p;
    return this;
  }

  /** every site reached (roots excluded), in BFS order. */
  public List<Integer> sites() {
    List<Integer> out = new ArrayList<>();
    Set<Integer> seen = new HashSet<>(roots);
    ArrayDeque<int[]> queue = new ArrayDeque<>();
    for (int root : roots)
      for (AnalysisDB.Edge e : edgesOf(root)) {
        int next = backward ? e.src() : e.dst();
        if (next == 0 || seen.contains(next))
          continue;
        if (firstHopRole != null && (e.role() == null || !e.role().contains(firstHopRole)))
          continue;
        seen.add(next);
        queue.add(new int[]{next, 1});
      }
    while (!queue.isEmpty()) {
      int[] cur = queue.poll();
      out.add(cur[0]);
      if (cur[1] >= depth || prune.test(cur[0]))
        continue;
      for (AnalysisDB.Edge e : edgesOf(cur[0])) {
        int next = backward ? e.src() : e.dst();
        if (next != 0 && seen.add(next))
          queue.add(new int[]{next, cur[1] + 1});
      }
    }
    return out;
  }

  public Sites reads() {
    return Sites.reads(db, sites());
  }

  /** does the walk reach a site matching the predicate? */
  public boolean reaches(IntPredicate p) {
    for (int pc : sites())
      if (p.test(pc))
        return true;
    return false;
  }

  /**
   * does the walk reach a site matching {@code p} through an edge whose role contains
   * {@code role}? Distinguishes "the byte lands here as data (VAL)" from "the byte becomes
   * this site's address (ADDR)" — the difference between a glyph font and an address table
   * that both feed the same screen write.
   */
  public boolean reachesVia(String role, IntPredicate p) {
    Set<Integer> seen = new HashSet<>(roots);
    ArrayDeque<int[]> queue = new ArrayDeque<>();
    for (int root : roots)
      queue.add(new int[]{root, 0});
    while (!queue.isEmpty()) {
      int[] cur = queue.poll();
      if (cur[1] >= depth || prune.test(cur[0]))
        continue;
      for (AnalysisDB.Edge e : edgesOf(cur[0])) {
        int next = backward ? e.src() : e.dst();
        if (next == 0)
          continue;
        boolean firstHopBlocked = cur[1] == 0 && firstHopRole != null
            && (e.role() == null || !e.role().contains(firstHopRole));
        if (firstHopBlocked)
          continue;
        if (e.role() != null && e.role().contains(role) && p.test(next))
          return true;
        if (seen.add(next))
          queue.add(new int[]{next, cur[1] + 1});
      }
    }
    return false;
  }

  private List<AnalysisDB.Edge> edgesOf(int pc) {
    return (backward ? db.edgesIn : db.edgesOut).getOrDefault(pc, List.of());
  }
}
