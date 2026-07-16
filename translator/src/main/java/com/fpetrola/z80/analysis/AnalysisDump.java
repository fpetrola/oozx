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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * F3 (doc/GUIA-ANALISIS-ECUACIONES.md, section 5): dumps the Tracer aggregates plus the
 * static equation table (sites.json) into a SQLite database for the analysis layer.
 */
public class AnalysisDump {

  public static void dump(String dbPath, String sitesJsonPath) throws Exception {
    Files.deleteIfExists(Path.of(dbPath));
    if (Path.of(dbPath).getParent() != null)
      Files.createDirectories(Path.of(dbPath).getParent());

    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      c.setAutoCommit(false);
      try (Statement st = c.createStatement()) {
        st.execute("CREATE TABLE sites(pc INT, method TEXT, line INT, kind TEXT, idx TEXT, value TEXT, stmt TEXT, equation TEXT)");
        st.execute("CREATE TABLE site_stats(pc INT, op TEXT, count INT, addr_min INT, addr_max INT," +
            " addr_and INT, addr_or INT, val_min INT, val_max INT, val_and INT, val_or INT," +
            " first_frame INT, last_frame INT)");
        st.execute("CREATE TABLE bulk_stats(pc INT, count INT, src_min INT, src_max INT," +
            " dst_min INT, dst_max INT, len_min INT, len_max INT, first_frame INT, last_frame INT)");
        st.execute("CREATE TABLE edges(src INT, dst INT, ch TEXT, role TEXT, count INT)");
        st.execute("CREATE TABLE cfg(src INT, dst INT, count INT)");
        st.execute("CREATE TABLE flags(pc INT, reads_f INT, io INT)");
        st.execute("CREATE TABLE site_roles(pc INT, ch TEXT, role TEXT)");
      }

      Map<Integer, Map<String, String>> roles = loadSites(c, sitesJsonPath);
      dumpStats(c);
      dumpEdges(c, roles);
      dumpCfg(c);
      dumpFlags(c);

      try (Statement st = c.createStatement()) {
        st.execute("CREATE INDEX i_sites_pc ON sites(pc)");
        st.execute("CREATE INDEX i_stats_pc ON site_stats(pc)");
        st.execute("CREATE INDEX i_edges_dst ON edges(dst)");
        st.execute("CREATE INDEX i_edges_src ON edges(src)");
        st.execute("CREATE INDEX i_cfg_src ON cfg(src)");
      }
      c.commit();
    }
    System.out.println("SQLite dump -> " + dbPath);
  }

  private static void dumpStats(Connection c) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO site_stats VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
      for (int s = 0; s < 0x10000; s++) {
        if (Tracer.wCount[s] > 0)
          addStat(ps, s, "W", Tracer.wCount[s], Tracer.wAddrMin[s], Tracer.wAddrMax[s],
              Tracer.wAddrAnd[s], Tracer.wAddrOr[s], Tracer.wValMin[s], Tracer.wValMax[s],
              Tracer.wValAnd[s], Tracer.wValOr[s], Tracer.wFirstFrame[s], Tracer.wLastFrame[s]);
        if (Tracer.rCount[s] > 0)
          addStat(ps, s, "R", Tracer.rCount[s], Tracer.rAddrMin[s], Tracer.rAddrMax[s],
              Tracer.rAddrAnd[s], Tracer.rAddrOr[s], Tracer.rValMin[s], Tracer.rValMax[s],
              Tracer.rValAnd[s], Tracer.rValOr[s], Tracer.rFirstFrame[s], Tracer.rLastFrame[s]);
      }
      ps.executeBatch();
    }
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO bulk_stats VALUES(?,?,?,?,?,?,?,?,?,?)")) {
      for (int s = 0; s < 0x10000; s++)
        if (Tracer.bCount[s] > 0) {
          ps.setInt(1, s);
          ps.setLong(2, Tracer.bCount[s]);
          ps.setInt(3, Tracer.bSrcMin[s]);
          ps.setInt(4, Tracer.bSrcMax[s]);
          ps.setInt(5, Tracer.bDstMin[s]);
          ps.setInt(6, Tracer.bDstMax[s]);
          ps.setInt(7, Tracer.bLenMin[s]);
          ps.setInt(8, Tracer.bLenMax[s]);
          ps.setInt(9, Tracer.bFirstFrame[s]);
          ps.setInt(10, Tracer.bLastFrame[s]);
          ps.addBatch();
        }
      ps.executeBatch();
    }
  }

  private static void addStat(PreparedStatement ps, int pc, String op, long count,
                              int aMin, int aMax, int aAnd, int aOr,
                              int vMin, int vMax, int vAnd, int vOr,
                              int ff, int lf) throws SQLException {
    ps.setInt(1, pc);
    ps.setString(2, op);
    ps.setLong(3, count);
    ps.setInt(4, aMin);
    ps.setInt(5, aMax);
    ps.setInt(6, aAnd);
    ps.setInt(7, aOr);
    ps.setInt(8, vMin);
    ps.setInt(9, vMax);
    ps.setInt(10, vAnd);
    ps.setInt(11, vOr);
    ps.setInt(12, ff);
    ps.setInt(13, lf);
    ps.addBatch();
  }

  /** data-flow edges: resolves each edge's role by (dst site, channel) from sites.json. */
  private static void dumpEdges(Connection c, Map<Integer, Map<String, String>> roles) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO edges VALUES(?,?,?,?,?)")) {
      final SQLException[] err = new SQLException[1];
      Tracer.edges.forEach((src, dst, ch, count) -> {
        try {
          String chName = Tracer.CH_NAME[ch];
          Map<String, String> siteRoles = roles.get(dst);
          ps.setInt(1, src);
          ps.setInt(2, dst);
          ps.setString(3, chName);
          ps.setString(4, siteRoles != null ? siteRoles.get(chName) : null);
          ps.setLong(5, count);
          ps.addBatch();
        } catch (SQLException e) {
          err[0] = e;
        }
      });
      if (err[0] != null)
        throw err[0];
      ps.executeBatch();
    }
  }

  private static void dumpCfg(Connection c) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO cfg VALUES(?,?,?)")) {
      final SQLException[] err = new SQLException[1];
      Tracer.cfg.forEach((src, dst, ch, count) -> {
        try {
          ps.setInt(1, src);
          ps.setInt(2, dst);
          ps.setLong(3, count);
          ps.addBatch();
        } catch (SQLException e) {
          err[0] = e;
        }
      });
      if (err[0] != null)
        throw err[0];
      ps.executeBatch();
    }
  }

  private static void dumpFlags(Connection c) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO flags VALUES(?,?,?)")) {
      for (int s = 0; s < 0x10000; s++)
        if (Tracer.readsF[s] || Tracer.ioSites[s]) {
          ps.setInt(1, s);
          ps.setInt(2, Tracer.readsF[s] ? 1 : 0);
          ps.setInt(3, Tracer.ioSites[s] ? 1 : 0);
          ps.addBatch();
        }
      ps.executeBatch();
    }
  }

  /**
   * minimal parser for our own sites.json (flat objects, no nesting).
   * Returns the static role table: pc -> channel -> role ("ADDR", "VAL", "COND" or "+"-joined).
   */
  private static Map<Integer, Map<String, String>> loadSites(Connection c, String sitesJsonPath)
      throws SQLException, IOException {
    Map<Integer, Map<String, String>> rolesByPc = new HashMap<>();
    if (sitesJsonPath == null || !Files.exists(Path.of(sitesJsonPath)))
      return rolesByPc;
    Pattern field = Pattern.compile("\"(pc|method|line|kind|index|value|stmt|roles|equation)\": (\\d+|\"(?:[^\"\\\\]|\\\\.)*\")");
    List<String> lines = Files.readAllLines(Path.of(sitesJsonPath));
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO sites VALUES(?,?,?,?,?,?,?,?)");
         PreparedStatement pr = c.prepareStatement("INSERT INTO site_roles VALUES(?,?,?)")) {
      for (String line : lines) {
        if (!line.trim().startsWith("{"))
          continue;
        int pc = -1, ln = -1;
        String method = null, kind = null, idx = null, value = null, stmt = null, roles = null, equation = null;
        Matcher m = field.matcher(line);
        while (m.find()) {
          String k = m.group(1), v = m.group(2);
          String sv = v.startsWith("\"") ? v.substring(1, v.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\") : v;
          switch (k) {
            case "pc" -> pc = Integer.parseInt(v);
            case "line" -> ln = Integer.parseInt(v);
            case "method" -> method = sv;
            case "kind" -> kind = sv;
            case "index" -> idx = sv;
            case "value" -> value = sv;
            case "stmt" -> stmt = sv;
            case "roles" -> roles = sv;
            case "equation" -> equation = sv;
          }
        }
        if (pc < 0)
          continue;
        ps.setInt(1, pc);
        ps.setString(2, method);
        ps.setInt(3, ln);
        ps.setString(4, kind);
        ps.setString(5, idx);
        ps.setString(6, value);
        ps.setString(7, stmt);
        ps.setString(8, equation);
        ps.addBatch();
        if (roles != null) {
          Map<String, String> chRoles = rolesByPc.computeIfAbsent(pc, k2 -> new HashMap<>());
          for (String entry : roles.split(";")) {
            int eq = entry.indexOf('=');
            if (eq <= 0)
              continue;
            String ch = entry.substring(0, eq), role = expandRole(entry.substring(eq + 1));
            chRoles.put(ch, role);
            pr.setInt(1, pc);
            pr.setString(2, ch);
            pr.setString(3, role);
            pr.addBatch();
          }
        }
      }
      ps.executeBatch();
      pr.executeBatch();
    }
    return rolesByPc;
  }

  private static String expandRole(String letters) {
    StringBuilder sb = new StringBuilder();
    for (char ch : letters.toCharArray()) {
      if (sb.length() > 0)
        sb.append('+');
      sb.append(switch (ch) {
        case 'A' -> "ADDR";
        case 'V' -> "VAL";
        case 'C' -> "COND";
        default -> String.valueOf(ch);
      });
    }
    return sb.toString();
  }
}
