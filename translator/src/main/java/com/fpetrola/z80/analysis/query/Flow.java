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
import java.util.function.Function;
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
  private int fanOut = 0; // 0 = unbounded
  private boolean skipSelfLoops = false;
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

  /** follow at most the first {@code n} out-edges examined per node (a fan-out cap that keeps
   * a hot node from exploding the walk). Every examined edge counts toward the cap. */
  public Flow fanOut(int n) {
    this.fanOut = n;
    return this;
  }

  /** never step across a self-edge (src == dst). */
  public Flow skipSelfLoops() {
    this.skipSelfLoops = true;
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

  /** a traversed edge with the destination's BFS depth (1-based) and a role probe. */
  public record FlowEdge(int src, int dst, String ch, String role, long count, int depth) {
    public boolean roleIs(String tag) {
      return role != null && role.contains(tag);
    }
  }

  /**
   * Every edge the walk traverses, with its role and depth — the visitor form of the walk,
   * for the analyzers that classify by edge role at each hop ("this read feeds a COND at
   * depth 1", "an ADDR edge lands in the graphics zone") instead of only collecting the set
   * of reached sites. A node is expanded once, but every edge INTO a node is emitted, so no
   * role is lost to node dedup. {@code firstHop}/{@code depth}/{@code pruneAt} apply as in
   * {@link #sites()}.
   */
  public List<FlowEdge> edges() {
    List<FlowEdge> out = new ArrayList<>();
    Set<Integer> expanded = new HashSet<>();
    List<Integer> frontier = new ArrayList<>(new LinkedHashSet<>(roots));
    for (int d = 0; d < depth && !frontier.isEmpty(); d++) {
      LinkedHashSet<Integer> next = new LinkedHashSet<>();
      for (int node : frontier) {
        if (!expanded.add(node) || prune.test(node))
          continue;
        for (AnalysisDB.Edge e : edgesOf(node)) {
          int to = backward ? e.src() : e.dst();
          if (to == 0)
            continue;
          if (d == 0 && firstHopRole != null
              && (e.role() == null || !e.role().contains(firstHopRole)))
            continue;
          out.add(new FlowEdge(e.src(), e.dst(), e.ch(), e.role(), e.count(), d + 1));
          next.add(to);
        }
      }
      frontier = new ArrayList<>(next);
    }
    return out;
  }

  /** called once per traversed edge with the tokens gathered from the root down to it. */
  @FunctionalInterface
  public interface PathVisitor<T> {
    void visit(FlowEdge edge, List<T> path);
  }

  /**
   * Depth-first provenance walk that threads a per-PATH accumulator — the shape of
   * "classify the endpoint while remembering the operations seen on the way" walks that a
   * flat {@link #edges()} cannot express. {@code token} maps each destination to an optional
   * token (null = none) and the visitor receives every traversed edge with the token list
   * accumulated root→edge. A fresh visited-set per root (a node is walked once per root, by
   * its first-discovery path); {@code depth}, {@link #fanOut} and {@link #skipSelfLoops}
   * apply. Unlike {@link #sites()}/{@link #edges()} the 0 sink is NOT special-cased, so the
   * cap and dedup match a hand-rolled DFS exactly.
   */
  public <T> void dfs(Function<Integer, T> token, PathVisitor<T> visitor) {
    for (int root : roots)
      dfsFrom(root, 0, new ArrayList<>(), new HashSet<>(), token, visitor);
  }

  private <T> void dfsFrom(int pc, int d, List<T> path, Set<Integer> seen,
                           Function<Integer, T> token, PathVisitor<T> visitor) {
    if (d >= depth)
      return;
    int examined = 0;
    for (AnalysisDB.Edge e : edgesOf(pc)) {
      if (fanOut > 0 && examined++ >= fanOut)
        break;
      if (skipSelfLoops && e.src() == e.dst())
        continue;
      int to = backward ? e.src() : e.dst();
      if (!seen.add(to))
        continue;
      if (d == 0 && firstHopRole != null
          && (e.role() == null || !e.role().contains(firstHopRole)))
        continue;
      T tok = token == null ? null : token.apply(to);
      List<T> childPath = tok == null ? path : append(path, tok);
      visitor.visit(new FlowEdge(e.src(), e.dst(), e.ch(), e.role(), e.count(), d + 1), childPath);
      dfsFrom(to, d + 1, childPath, seen, token, visitor);
    }
  }

  private static <T> List<T> append(List<T> a, T b) {
    List<T> out = new ArrayList<>(a);
    out.add(b);
    return out;
  }

  /**
   * The first value {@code fn} returns non-null for, in depth-first pre-order over reached
   * nodes (a fresh visited-set, root excluded; {@code depth} levels). The early-exit form of
   * the walk — "trace back until you hit a site you recognise" — without collecting the rest.
   */
  public <T> T firstMatch(Function<Integer, T> fn) {
    Set<Integer> seen = new HashSet<>();
    for (int root : roots) {
      T hit = firstMatchFrom(root, 0, seen, fn);
      if (hit != null)
        return hit;
    }
    return null;
  }

  private <T> T firstMatchFrom(int pc, int d, Set<Integer> seen, Function<Integer, T> fn) {
    if (d >= depth)
      return null;
    for (AnalysisDB.Edge e : edgesOf(pc)) {
      int to = backward ? e.src() : e.dst();
      if (to == 0 || !seen.add(to))
        continue;
      T hit = fn.apply(to);
      if (hit != null)
        return hit;
      T deeper = firstMatchFrom(to, d + 1, seen, fn);
      if (deeper != null)
        return deeper;
    }
    return null;
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
