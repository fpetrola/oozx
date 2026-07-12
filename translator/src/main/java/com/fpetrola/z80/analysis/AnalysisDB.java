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
 * F4: loads the analysis SQLite database into memory and offers the primitives the
 * analyzers are built on (equations per site, stats, data-flow adjacency, cfg, bulk copies).
 */
public class AnalysisDB {

  public record Stat(int pc, String op, long count, int addrMin, int addrMax, int addrAnd, int addrOr,
                     int valMin, int valMax, int valAnd, int valOr, int firstFrame, int lastFrame) {
  }

  public record Bulk(int pc, long count, int srcMin, int srcMax, int dstMin, int dstMax, int lenMin, int lenMax) {
  }

  public record Edge(int src, int dst, String ch, String role, long count) {
    /** "H:ADDR" / "MEM:VAL" / "F" (channel without static role). */
    public String label() {
      return ch == null ? "" : role == null ? ch : ch + ":" + role;
    }
  }

  public final Map<Integer, String> equation = new HashMap<>();     // pc -> INSTR/BRANCH stmt
  public final Map<Integer, String> method = new HashMap<>();
  public final Map<Integer, String> kindOf = new HashMap<>();       // pc -> BRANCH if branch site
  public final Map<Integer, Stat> writes = new HashMap<>();
  public final Map<Integer, Stat> reads = new HashMap<>();
  public final Map<Integer, Bulk> bulks = new HashMap<>();
  public final Map<Integer, List<Edge>> edgesIn = new HashMap<>();  // dst -> incoming
  public final Map<Integer, List<Edge>> edgesOut = new HashMap<>(); // src -> outgoing
  public final Map<Integer, List<Edge>> cfgOut = new HashMap<>();
  public final Set<Integer> ioSites = new HashSet<>();
  public final Set<Integer> branchSites = new HashSet<>();

  public AnalysisDB(String dbPath) throws SQLException {
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      try (ResultSet rs = c.createStatement().executeQuery("SELECT pc, method, kind, stmt FROM sites")) {
        while (rs.next()) {
          int pc = rs.getInt(1);
          String kind = rs.getString(3);
          method.putIfAbsent(pc, rs.getString(2));
          if ("INSTR".equals(kind) || "BRANCH".equals(kind)) {
            equation.put(pc, rs.getString(4));
            if ("BRANCH".equals(kind))
              kindOf.put(pc, kind);
          }
        }
      }
      try (ResultSet rs = c.createStatement().executeQuery("SELECT * FROM site_stats")) {
        while (rs.next()) {
          Stat s = new Stat(rs.getInt(1), rs.getString(2), rs.getLong(3), rs.getInt(4), rs.getInt(5),
              rs.getInt(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getInt(10), rs.getInt(11),
              rs.getInt(12), rs.getInt(13));
          ("W".equals(s.op) ? writes : reads).put(s.pc, s);
        }
      }
      try (ResultSet rs = c.createStatement().executeQuery("SELECT * FROM bulk_stats")) {
        while (rs.next())
          bulks.put(rs.getInt(1), new Bulk(rs.getInt(1), rs.getLong(2), rs.getInt(3), rs.getInt(4),
              rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getInt(8)));
      }
      try (ResultSet rs = c.createStatement().executeQuery("SELECT src, dst, ch, role, count FROM edges")) {
        while (rs.next()) {
          Edge e = new Edge(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getLong(5));
          edgesIn.computeIfAbsent(e.dst, k -> new ArrayList<>()).add(e);
          edgesOut.computeIfAbsent(e.src, k -> new ArrayList<>()).add(e);
        }
      }
      try (ResultSet rs = c.createStatement().executeQuery("SELECT src, dst, count FROM cfg")) {
        while (rs.next())
          cfgOut.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>())
              .add(new Edge(rs.getInt(1), rs.getInt(2), null, null, rs.getLong(3)));
      }
      try (ResultSet rs = c.createStatement().executeQuery("SELECT pc, reads_f, io FROM flags")) {
        while (rs.next()) {
          if (rs.getInt(3) == 1)
            ioSites.add(rs.getInt(1));
          if (rs.getInt(2) == 1)
            branchSites.add(rs.getInt(1));
        }
      }
    }
    edgesIn.values().forEach(l -> l.sort(Comparator.comparingLong((Edge e) -> -e.count)));
    edgesOut.values().forEach(l -> l.sort(Comparator.comparingLong((Edge e) -> -e.count)));
  }

  public String describe(int pc) {
    if (pc == 0)
      return "INIT (dato original del snapshot)";
    StringBuilder sb = new StringBuilder(String.valueOf(pc));
    if (method.containsKey(pc))
      sb.append(" [").append(method.get(pc)).append(']');
    Stat w = writes.get(pc), r = reads.get(pc);
    if (w != null)
      sb.append(" W:addr[").append(w.addrMin).append("..").append(w.addrMax)
          .append("] val[").append(w.valMin).append("..").append(w.valMax).append(']');
    if (r != null)
      sb.append(" R:addr[").append(r.addrMin).append("..").append(r.addrMax)
          .append("] val[").append(r.valMin).append("..").append(r.valMax).append(']');
    Bulk b = bulks.get(pc);
    if (b != null)
      sb.append(" BULK:[").append(b.srcMin).append("..").append(b.srcMax).append("]->[")
          .append(b.dstMin).append("..").append(b.dstMax).append("] len[")
          .append(b.lenMin).append("..").append(b.lenMax).append(']');
    if (ioSites.contains(pc))
      sb.append(" [IO/input]");
    String eq = equation.get(pc);
    if (eq != null)
      sb.append("\n      eq: ").append(eq.length() > 100 ? eq.substring(0, 100) + "..." : eq);
    return sb.toString();
  }

  /** write-sites whose address range intersects [lo..hi]. */
  public List<Stat> writersIntersecting(int lo, int hi) {
    return writes.values().stream()
        .filter(s -> s.addrMax >= lo && s.addrMin <= hi)
        .sorted(Comparator.comparingLong((Stat s) -> -s.count))
        .toList();
  }
}
