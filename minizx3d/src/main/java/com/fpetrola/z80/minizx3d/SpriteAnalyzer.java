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

package com.fpetrola.z80.minizx3d;

import java.util.ArrayList;
import java.util.List;

/** Measures a silhouette into {@link SpriteFeatures}. Pure function of the bitmap. */
public final class SpriteAnalyzer {
  private SpriteAnalyzer() {
  }

  public static SpriteFeatures analyze(SpriteBitmap b) {
    boolean[][] m = b.mask();
    int rows = m.length, w = m[0].length;
    int minX = w, maxX = -1, minY = rows, maxY = -1, lit = 0;
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (m[y][x]) {
          lit++;
          minX = Math.min(minX, x);
          maxX = Math.max(maxX, x);
          minY = Math.min(minY, y);
          maxY = Math.max(maxY, y);
        }
    if (lit == 0)
      return new SpriteFeatures(1, 0, 0, 1, 0, 0, 0, 0, 0, 0);
    int bw = maxX - minX + 1, bh = maxY - minY + 1;

    float fill = lit / (float) (bw * bh);
    float hull = hullArea(m, rows, w);
    float solidity = hull <= 0 ? 1 : Math.min(1, lit / hull);

    // mirror inside the BOUNDING BOX, not the padded bitmap: a sprite sitting off-center in
    // a byte-aligned bitmap is still symmetric
    int agree = 0;
    for (int y = minY; y <= maxY; y++)
      for (int x = minX; x <= maxX; x++)
        if (m[y][x] == m[y][minX + maxX - x])
          agree++;
    float symmetryV = agree / (float) (bw * bh);

    int[][] dist = SpriteFx.chamfer(m);
    int thin = 0;
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (m[y][x] && dist[y][x] <= 1)
          thin++;
    float thinness = thin / (float) lit;

    return new SpriteFeatures((float) bh / bw, fill, solidity, symmetryV,
        components(m, rows, w), holes(m, rows, w), thinness, bw, bh, lit);
  }

  /** connected groups of lit pixels, 8-connectivity. */
  private static int components(boolean[][] m, int rows, int w) {
    boolean[][] seen = new boolean[rows][w];
    int n = 0;
    java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
    for (int y0 = 0; y0 < rows; y0++)
      for (int x0 = 0; x0 < w; x0++) {
        if (!m[y0][x0] || seen[y0][x0])
          continue;
        n++;
        seen[y0][x0] = true;
        q.add(new int[]{y0, x0});
        while (!q.isEmpty()) {
          int[] p = q.poll();
          for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
              int y = p[0] + dy, x = p[1] + dx;
              if (y >= 0 && y < rows && x >= 0 && x < w && m[y][x] && !seen[y][x]) {
                seen[y][x] = true;
                q.add(new int[]{y, x});
              }
            }
        }
      }
    return n;
  }

  /**
   * Enclosed background regions — but only those of at least {@link #MIN_HOLE} pixels. On
   * this hardware a one-pixel gap is dithering or an attribute-cell artifact, not a hole:
   * counting them made JSW's detailed guardians report 7-16 "holes" each and sent almost
   * every sprite down the expensive surface-nets branch.
   */
  private static final int MIN_HOLE = 3;

  private static int holes(boolean[][] m, int rows, int w) {
    boolean[][] seen = new boolean[rows][w];
    java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if ((y == 0 || y == rows - 1 || x == 0 || x == w - 1) && !m[y][x] && !seen[y][x]) {
          seen[y][x] = true;
          q.add(new int[]{y, x});
        }
    while (!q.isEmpty()) { // flood the outside in from the border, 4-connectivity
      int[] p = q.poll();
      int[][] n4 = {{p[0] - 1, p[1]}, {p[0] + 1, p[1]}, {p[0], p[1] - 1}, {p[0], p[1] + 1}};
      for (int[] n : n4)
        if (n[0] >= 0 && n[0] < rows && n[1] >= 0 && n[1] < w && !m[n[0]][n[1]] && !seen[n[0]][n[1]]) {
          seen[n[0]][n[1]] = true;
          q.add(n);
        }
    }
    int holes = 0;
    for (int y0 = 0; y0 < rows; y0++)
      for (int x0 = 0; x0 < w; x0++) {
        if (m[y0][x0] || seen[y0][x0])
          continue;
        int area = 0;
        seen[y0][x0] = true;
        q.add(new int[]{y0, x0});
        while (!q.isEmpty()) {
          int[] p = q.poll();
          area++;
          int[][] n4 = {{p[0] - 1, p[1]}, {p[0] + 1, p[1]}, {p[0], p[1] - 1}, {p[0], p[1] + 1}};
          for (int[] n : n4)
            if (n[0] >= 0 && n[0] < rows && n[1] >= 0 && n[1] < w
                && !m[n[0]][n[1]] && !seen[n[0]][n[1]]) {
              seen[n[0]][n[1]] = true;
              q.add(n);
            }
        }
        if (area >= MIN_HOLE)
          holes++;
      }
    return holes;
  }

  /** area of the convex hull of the lit pixels (monotone chain + shoelace). */
  private static float hullArea(boolean[][] m, int rows, int w) {
    List<int[]> pts = new ArrayList<>();
    for (int y = 0; y < rows; y++) { // only the row extremes can be on the hull
      int lo = -1, hi = -1;
      for (int x = 0; x < w; x++)
        if (m[y][x]) {
          if (lo < 0)
            lo = x;
          hi = x;
        }
      if (lo >= 0) {
        pts.add(new int[]{lo, y});
        pts.add(new int[]{hi + 1, y});
        pts.add(new int[]{lo, y + 1});
        pts.add(new int[]{hi + 1, y + 1});
      }
    }
    if (pts.size() < 3)
      return 0;
    pts.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
    int n = pts.size();
    int[][] hull = new int[2 * n][];
    int k = 0;
    for (int i = 0; i < n; i++) {
      while (k >= 2 && cross(hull[k - 2], hull[k - 1], pts.get(i)) <= 0)
        k--;
      hull[k++] = pts.get(i);
    }
    for (int i = n - 2, t = k + 1; i >= 0; i--) {
      while (k >= t && cross(hull[k - 2], hull[k - 1], pts.get(i)) <= 0)
        k--;
      hull[k++] = pts.get(i);
    }
    float area = 0;
    for (int i = 0; i < k - 1; i++)
      area += hull[i][0] * (float) hull[i + 1][1] - hull[i + 1][0] * (float) hull[i][1];
    return Math.abs(area) / 2f;
  }

  private static long cross(int[] o, int[] a, int[] b) {
    return (long) (a[0] - o[0]) * (b[1] - o[1]) - (long) (a[1] - o[1]) * (b[0] - o[0]);
  }
}
