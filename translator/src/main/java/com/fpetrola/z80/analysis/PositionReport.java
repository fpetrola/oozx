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

import com.fpetrola.z80.analysis.query.Db;

import java.util.*;

/**
 * F5 (targeted) — the "positions" command: prints, for a frame range, every sprite drawn
 * (from {@code sprite_draws}) side by side with the positions predicted by the
 * auto-validated coordinate pairs ({@code coord_pairs} applied to the {@code frame_cells}
 * values of that frame). Requires a previous "track" run.
 */
public class PositionReport {

  public static void print(String dbPath, int frameLo, int frameHi) {
    try (Db q = new Db(dbPath)) {
      // validated (X, Y) cell pairs: xAddr, xT, xOff, yAddr, yT, yOff, rate
      List<Object[]> pairs = new ArrayList<>();
      Set<Integer> pairCells = new HashSet<>();
      q.forEach("SELECT x_addr, x_transform, x_off, y_addr, y_transform, y_off, rate FROM coord_pairs ORDER BY joint DESC", rs -> {
        pairs.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getInt(3),
            rs.getInt(4), rs.getString(5), rs.getInt(6), rs.getDouble(7)});
        pairCells.add(rs.getInt(1));
        pairCells.add(rs.getInt(4));
      });
      // high-confidence single cells not already covered by a pair
      List<Object[]> singles = new ArrayList<>();  // addr, axis, transform, off
      q.forEach("SELECT addr, axis, transform, off FROM coord_cells WHERE rate >= 0.6 ORDER BY matched DESC", rs -> {
        if (!pairCells.contains(rs.getInt(1)))
          singles.add(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)});
      });

      // per-frame draws in range
      Map<Integer, List<String>> drawsByFrame = new TreeMap<>();
      q.forEach("SELECT frame, method, kind, x, y, w, h, gfx, path FROM sprite_draws WHERE frame BETWEEN ? AND ? ORDER BY frame", rs -> {
        int gfx = rs.getInt(8);
        drawsByFrame.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>())
            .add(String.format("$%d %s (%d,%d) %dx%d%s path:%08x", rs.getInt(2), rs.getString(3),
                rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7),
                gfx >= 0 ? " gfx@" + gfx : "", rs.getInt(9)));
      }, frameLo, frameHi);
      if (drawsByFrame.isEmpty()) {
        System.out.println("sin draws en frames [" + frameLo + ".." + frameHi + "]");
        return;
      }

      // watched-cell values: replay deltas up to each frame of the range
      int[] cur = new int[0x10000];
      Arrays.fill(cur, -1);
      Map<Integer, int[]> memAtFrame = new HashMap<>();
      Iterator<Integer> frames = drawsByFrame.keySet().iterator();
      Integer[] next = {frames.hasNext() ? frames.next() : null};
      q.forEach("SELECT frame, addr, val FROM frame_cells WHERE frame <= ? ORDER BY frame, rowid", rs -> {
        int f = rs.getInt(1);
        while (next[0] != null && f > next[0]) {
          memAtFrame.put(next[0], cur.clone());
          next[0] = frames.hasNext() ? frames.next() : null;
        }
        cur[rs.getInt(2)] = rs.getInt(3);
      }, frameHi);
      while (next[0] != null) {
        memAtFrame.put(next[0], cur.clone());
        next[0] = frames.hasNext() ? frames.next() : null;
      }

      for (Map.Entry<Integer, List<String>> e : drawsByFrame.entrySet()) {
        System.out.println("frame " + e.getKey() + ":");
        e.getValue().forEach(d -> System.out.println("  draw  " + d));
        int[] m = memAtFrame.get(e.getKey());
        for (Object[] p : pairs) {
          int xv = m[(int) p[0]], yv = m[(int) p[3]];
          if (xv < 0 || yv < 0)
            continue;
          System.out.printf("  par   (mem[%d]=%d, mem[%d]=%d) -> (%d,%d)  [conf %.0f%%]%n",
              (int) p[0], xv, (int) p[3], yv,
              SpriteTracker.applyTransform((String) p[1], xv) + (int) p[2],
              SpriteTracker.applyTransform((String) p[4], yv) + (int) p[5],
              (double) p[6] * 100);
        }
        for (Object[] s : singles) {
          int v = m[(int) s[0]];
          if (v >= 0)
            System.out.printf("  celda mem[%d]=%d -> %s=%d%n", (int) s[0], v, s[1],
                SpriteTracker.applyTransform((String) s[2], v) + (int) s[3]);
        }
      }
    }
  }
}
