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

import java.sql.*;
import java.util.*;

/**
 * The "explain" command: one recursive narrative of how a memory range gets written.
 * For each writer (equation sites AND bulk copies) it walks the data-flow backwards,
 * separating at the first level how the ADDRESS, the VALUE and the CONDITIONS were
 * built, and classifies every memory read it crosses:
 * <ul>
 *   <li><b>ESTATICA</b>: never written by the game — data loaded from the cassette;</li>
 *   <li><b>DINAMICA</b>: written during the run (with its writer count) — a game
 *       variable; if a previous "track" run identified it as a coordinate or recorded
 *       its per-frame series, that is annotated inline;</li>
 *   <li>roots: <b>INIT</b> (original data) and <b>IO</b> (player input) end a branch.</li>
 * </ul>
 * Buffer hops need no special casing: bulk-copy sites sit in the edge graph, so the
 * chain screen &larr; ldir &larr; backbuffer writer &larr; ... composes naturally.
 */
public class Explainer {
  private final AnalysisDB db;
  private final Map<Integer, Character> coordAxis = new HashMap<>(); // addr -> 'X'/'Y'
  private final Set<Integer> trackedCells = new HashSet<>();         // addrs with frame_cells series

  public Explainer(AnalysisDB db, String dbPath) {
    this.db = db;
    // optional enrichment from a previous "track" run
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      try (ResultSet rs = c.createStatement().executeQuery("SELECT addr, axis FROM coord_cells")) {
        while (rs.next())
          coordAxis.putIfAbsent(rs.getInt(1), rs.getString(2).charAt(0));
      }
      try (ResultSet rs = c.createStatement().executeQuery("SELECT DISTINCT addr FROM frame_cells")) {
        while (rs.next())
          trackedCells.add(rs.getInt(1));
      }
    } catch (SQLException ignored) {
      // no track tables yet: explain still works, without coordinate/series annotations
    }
  }

  public void explain(int lo, int hi, int maxDepth, int fanout) {
    System.out.println("=== EXPLAIN mem[" + lo + ".." + hi + "] ===");
    System.out.println(classifyRange(lo, hi) + "\n");

    List<AnalysisDB.Stat> writers = db.writersIntersecting(lo, hi);
    List<AnalysisDB.Bulk> copies = db.bulks.values().stream()
        .filter(b -> b.dstMax() + Math.max(0, b.lenMax() - 1) >= lo && b.dstMin() <= hi)
        .sorted(Comparator.comparingLong((AnalysisDB.Bulk b) -> -b.count()))
        .toList();
    System.out.println("ESCRITO POR " + writers.size() + " ecuaciones y " + copies.size() + " copias en bloque\n");

    int shown = 0;
    for (AnalysisDB.Bulk b : copies) {
      if (shown++ >= 8) {
        System.out.println("... (más writers, subí el límite con fanout)");
        break;
      }
      System.out.println("COPIA x" + b.count() + "  [" + b.srcMin() + ".." + (b.srcMax() + b.lenMax() - 1)
          + "] -> [" + b.dstMin() + ".." + (b.dstMax() + b.lenMax() - 1) + "]  " + place(b.pc()));
      System.out.println("  el contenido copiado viene de mem[" + b.srcMin() + ".." + (b.srcMax() + b.lenMax() - 1)
          + "] " + classifyRange(b.srcMin(), b.srcMax() + b.lenMax() - 1));
      walk(b.pc(), 1, maxDepth, fanout, new HashSet<>(List.of(b.pc())));
      System.out.println();
    }
    for (AnalysisDB.Stat w : writers) {
      if (shown++ >= 8) {
        System.out.println("... (más writers)");
        break;
      }
      System.out.println("ECUACION x" + w.count() + "  " + eq(w.pc()) + "  " + place(w.pc())
          + "  W[" + w.addrMin() + ".." + w.addrMax() + "] val[" + w.valMin() + ".." + w.valMax() + "]");
      walkGrouped(w.pc(), maxDepth, fanout);
      System.out.println();
    }
  }

  /** first level grouped by role: how the ADDRESS / VALUE / CONDITION was built. */
  private void walkGrouped(int pc, int maxDepth, int fanout) {
    Map<String, List<AnalysisDB.Edge>> buckets = new LinkedHashMap<>();
    for (String b : new String[]{"DIRECCION", "VALOR", "CONDICION", "OTROS"})
      buckets.put(b, new ArrayList<>());
    for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of())) {
      String role = e.role();
      String bucket = role == null ? "OTROS"
          : role.contains("ADDR") ? "DIRECCION"
          : role.contains("VAL") ? "VALOR"
          : role.contains("COND") ? "CONDICION" : "OTROS";
      buckets.get(bucket).add(e);
    }
    for (Map.Entry<String, List<AnalysisDB.Edge>> be : buckets.entrySet()) {
      if (be.getValue().isEmpty())
        continue;
      System.out.println("  " + be.getKey() + ":");
      Set<Integer> seen = new HashSet<>(List.of(pc));
      int shown = 0;
      for (AnalysisDB.Edge e : be.getValue()) {
        if (shown++ >= fanout)
          break;
        printEdge(e, 2);
        if (e.src() != 0 && !db.ioSites.contains(e.src()) && seen.add(e.src()))
          walk(e.src(), 3, maxDepth, fanout, seen);
      }
    }
  }

  private void walk(int pc, int depth, int maxDepth, int fanout, Set<Integer> seen) {
    if (depth > maxDepth)
      return;
    int shown = 0;
    for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of())) {
      if (shown++ >= fanout)
        break;
      printEdge(e, depth);
      if (e.src() != 0 && !db.ioSites.contains(e.src()) && seen.add(e.src()))
        walk(e.src(), depth + 1, maxDepth, fanout, seen);
    }
  }

  private void printEdge(AnalysisDB.Edge e, int depth) {
    String indent = "  ".repeat(depth);
    String label = e.label();
    int src = e.src();
    if (src == 0) {
      System.out.println(indent + "<- x" + e.count() + (label.isEmpty() ? "" : " [" + label + "]")
          + " DATO ORIGINAL DEL CASSETTE (INIT)");
      return;
    }
    StringBuilder sb = new StringBuilder(indent + "<- x" + e.count()
        + (label.isEmpty() ? "" : " [" + label + "]") + " " + eq(src) + "  " + place(src));
    AnalysisDB.Bulk b = db.bulks.get(src);
    if (b != null)
      sb.append("  COPIA [").append(b.srcMin()).append("..").append(b.srcMax() + b.lenMax() - 1)
          .append("] -> [").append(b.dstMin()).append("..").append(b.dstMax() + b.lenMax() - 1).append(']');
    AnalysisDB.Stat r = db.reads.get(src);
    if (r != null)
      sb.append("\n").append(indent).append("     lee mem[").append(r.addrMin()).append("..")
          .append(r.addrMax()).append("] ").append(classifyRange(r.addrMin(), r.addrMax()));
    if (db.ioSites.contains(src))
      sb.append("\n").append(indent).append("     INPUT DEL JUGADOR (RZX) — raíz");
    System.out.println(sb);
  }

  /** ESTATICA (cassette) vs DINAMICA (game variable), with track annotations if present. */
  public String classifyRange(int lo, int hi) {
    List<AnalysisDB.Stat> ws = db.writersIntersecting(lo, hi);
    boolean bulkDst = db.bulks.values().stream()
        .anyMatch(b -> b.dstMax() + Math.max(0, b.lenMax() - 1) >= lo && b.dstMin() <= hi);
    // written sub-ranges inside [lo..hi] (equations + bulk destinations, merged): a few
    // touched cells inside a big table should not turn the whole table "dynamic"
    List<int[]> covered = new ArrayList<>();
    for (AnalysisDB.Stat w : ws)
      covered.add(new int[]{Math.max(lo, w.addrMin()), Math.min(hi, w.addrMax())});
    for (AnalysisDB.Bulk b : db.bulks.values()) {
      int bHi = b.dstMax() + Math.max(0, b.lenMax() - 1);
      if (bHi >= lo && b.dstMin() <= hi)
        covered.add(new int[]{Math.max(lo, b.dstMin()), Math.min(hi, bHi)});
    }
    covered.sort(Comparator.comparingInt(a -> a[0]));
    List<int[]> merged = new ArrayList<>();
    for (int[] r : covered) {
      if (!merged.isEmpty() && r[0] <= merged.get(merged.size() - 1)[1] + 1)
        merged.get(merged.size() - 1)[1] = Math.max(merged.get(merged.size() - 1)[1], r[1]);
      else
        merged.add(new int[]{r[0], r[1]});
    }
    long writtenSpan = merged.stream().mapToLong(r -> r[1] - r[0] + 1).sum();
    int span = hi - lo + 1;
    String subRanges = merged.stream().limit(3)
        .map(r -> "[" + r[0] + ".." + r[1] + "]")
        .reduce((a, b2) -> a + " " + b2).orElse("");

    StringBuilder sb = new StringBuilder();
    if (writtenSpan == 0)
      sb.append("STATIC: never written, loaded from the cassette");
    else if (span >= 64 && writtenSpan <= Math.max(4, span / 20))
      sb.append("mostly STATIC (cassette), except cells written at ").append(subRanges);
    else if (span >= 64 && writtenSpan < span / 2)
      sb.append("MIXED: ").append(100 - writtenSpan * 100 / span)
          .append("% never written (cassette), dynamic at ").append(subRanges);
    else if (ws.isEmpty())
      sb.append("DYNAMIC: written by bulk copies");
    else
      sb.append("DYNAMIC: ").append(ws.size()).append(" equations write it")
          .append(bulkDst ? " (+bulk copies)" : "");
    List<String> coords = new ArrayList<>();
    for (int a = Math.max(lo, hi - 512); a <= hi; a++)
      if (coordAxis.containsKey(a))
        coords.add(coordAxis.get(a) + "=mem[" + a + "]");
    if (!coords.isEmpty())
      sb.append(" | sprite coordinates: ").append(String.join(" ", coords));
    else if (trackedCells.stream().anyMatch(a -> a >= lo && a <= hi))
      sb.append(" | per-frame series available in frame_cells");
    return sb.toString();
  }

  private String eq(int pc) {
    String eq = db.equation.get(pc);
    if (eq == null)
      return "?";
    return eq.length() > 90 ? eq.substring(0, 90) + "..." : eq;
  }

  private String place(int pc) {
    String m = db.method.get(pc);
    return "@" + pc + (m != null ? " [" + m + "]" : "");
  }
}
