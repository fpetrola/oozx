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

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Single place that talks to the SQLite side tables (frame_cells, coord_pairs,
 * sprite_draws, ...). One lazily opened connection instead of a DriverManager call at
 * every query site; results come back as plain long rows so analyzers stay free of JDBC.
 */
public final class Db implements AutoCloseable {
  private final String path;
  private Connection conn;

  public Db(String path) {
    this.path = path;
  }

  private Connection conn() throws SQLException {
    if (conn == null)
      conn = DriverManager.getConnection("jdbc:sqlite:" + path);
    return conn;
  }

  /** every row of the query as a long[] (one slot per selected column). */
  public List<long[]> rows(String sql, Object... params) {
    List<long[]> out = new ArrayList<>();
    try (PreparedStatement ps = conn().prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++)
        ps.setObject(i + 1, params[i]);
      try (ResultSet rs = ps.executeQuery()) {
        int cols = rs.getMetaData().getColumnCount();
        while (rs.next()) {
          long[] row = new long[cols];
          for (int c = 0; c < cols; c++)
            row[c] = rs.getLong(c + 1);
          out.add(row);
        }
      }
    } catch (SQLException ignored) {
      // a missing side table (track not run) just yields no rows
    }
    return out;
  }

  /** first column of the first row, or the fallback when the query yields nothing. */
  public long scalar(String sql, long fallback, Object... params) {
    List<long[]> rows = rows(sql, params);
    return rows.isEmpty() ? fallback : rows.get(0)[0];
  }

  @Override
  public void close() {
    try {
      if (conn != null)
        conn.close();
    } catch (SQLException ignored) {
    }
    conn = null;
  }
}
