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
 * Single place that talks to the SQLite tables (sites, frame_cells, coord_pairs,
 * sprite_draws, ...). One lazily opened connection instead of a DriverManager call at
 * every query site. Numeric rows come back as {@code long[]} via {@link #rows}; mixed
 * string/int rows go through a {@link RowMapper} ({@link #query}) or a stateful
 * {@link RowConsumer} ({@link #forEach}) so analyzers never touch Connection/ResultSet.
 */
public final class Db implements AutoCloseable {
  /** maps one {@link ResultSet} row (already positioned) to a value. */
  @FunctionalInterface
  public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
  }

  /** consumes one {@link ResultSet} row — for stateful scans that fold rather than collect. */
  @FunctionalInterface
  public interface RowConsumer {
    void accept(ResultSet rs) throws SQLException;
  }

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

  /** whether a table exists (present but empty still counts — unlike an empty {@link #rows}). */
  public boolean hasTable(String name) {
    return !rows("SELECT name FROM sqlite_master WHERE type='table' AND name=?", name).isEmpty();
  }

  /** each row mapped to a T (mixed string/int columns), in query order. */
  public <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
    List<T> out = new ArrayList<>();
    forEach(sql, rs -> out.add(mapper.map(rs)), params);
    return out;
  }

  /** run {@code consumer} over every row — for scans that accumulate into their own state. */
  public void forEach(String sql, RowConsumer consumer, Object... params) {
    try (PreparedStatement ps = conn().prepareStatement(sql)) {
      for (int i = 0; i < params.length; i++)
        ps.setObject(i + 1, params[i]);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next())
          consumer.accept(rs);
      }
    } catch (SQLException ignored) {
      // a missing side table (track not run) just yields no rows
    }
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
