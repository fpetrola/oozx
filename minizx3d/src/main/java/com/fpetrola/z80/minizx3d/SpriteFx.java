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

/**
 * Shared silhouette maths for the baking strategies: the EPX contour pre-pass, the distance
 * transform, and the per-pixel depth profiles.
 */
public final class SpriteFx {
  private SpriteFx() {
  }

  /**
   * Hard cap on total depth, in voxels (-Dsprite3d.maxdepth). The volume is otherwise scaled
   * by each sprite's own radius, so a 16px-wide enemy got 16 deep and read as a barrel. With
   * the cap at 8 an 8px-wide Willy is 8 wide and 8 deep at the thickest point of the oval,
   * and anything wider stays at 8 instead of growing with it.
   */
  public static float MAX_DEPTH = Float.parseFloat(System.getProperty("sprite3d.maxdepth", "8"));

  /** half of {@link #MAX_DEPTH}: the surface is built as +h and -h around the sprite plane. */
  public static float maxHalfDepth() {
    return MAX_DEPTH / 2f;
  }

  /** the EPX/Scale2x passes a config asks for; {@code epx <= 1} returns the sprite as is. */
  public static SpriteBitmap preprocess(SpriteBitmap b, Sprite3DConfig c) {
    if (c.epx <= 1)
      return b;
    boolean[][] m = b.mask();
    for (int k = c.epx; k > 1; k /= 2)
      m = epx(m);
    return b.withMask(m);
  }

  /**
   * EPX / Scale2x on the 1-bit silhouette: doubles it, turning staircase diagonals into
   * clean ones. Comparison is on the MASK, not on color — a Spectrum cell only carries two
   * colors, so running EPX on color here would mostly compare attribute clash, not shape.
   *
   * <pre>
   *   E0 = (C==A && C!=D && A!=B) ? A : P     E1 = (A==B && A!=C && B!=D) ? B : P
   *   E2 = (D==C && D!=B && C!=A) ? C : P     E3 = (B==D && B!=A && D!=C) ? D : P
   * </pre>
   * Out-of-range neighbours read as P, which keeps the border from inventing detail.
   */
  public static boolean[][] epx(boolean[][] src) {
    int h = src.length, w = src[0].length;
    boolean[][] out = new boolean[h * 2][w * 2];
    for (int y = 0; y < h; y++)
      for (int x = 0; x < w; x++) {
        boolean p = src[y][x];
        boolean a = y > 0 ? src[y - 1][x] : p;
        boolean b = x < w - 1 ? src[y][x + 1] : p;
        boolean cc = x > 0 ? src[y][x - 1] : p;
        boolean d = y < h - 1 ? src[y + 1][x] : p;
        out[y * 2][x * 2] = (cc == a && cc != d && a != b) ? a : p;
        out[y * 2][x * 2 + 1] = (a == b && a != cc && b != d) ? b : p;
        out[y * 2 + 1][x * 2] = (d == cc && d != b && cc != a) ? cc : p;
        out[y * 2 + 1][x * 2 + 1] = (b == d && b != a && d != cc) ? d : p;
      }
    return out;
  }

  /**
   * Per-pixel thickness for a config: a hand-painted map wins, then the primitive profile
   * for {@link Sprite3DConfig.Technique#PRIMITIVE}, and otherwise the distance transform —
   * the shape-adaptive default that needs no classification to look right.
   */
  public static float[][] depthField(SpriteBitmap b, Sprite3DConfig c, boolean[][] mask) {
    int rows = mask.length, w = mask[0].length;
    float[][] d = new float[rows][w];
    if (c.customDepthMap != null && c.customDepthMap.length >= rows * w) {
      for (int y = 0; y < rows; y++)
        for (int x = 0; x < w; x++)
          d[y][x] = mask[y][x] ? Math.max(.5f, c.customDepthMap[y * w + x] * c.depth * 4f) : 0;
      return d;
    }
    if (c.technique == Sprite3DConfig.Technique.PRIMITIVE) {
      // (u,v) normalized to [-1,1] over the SILHOUETTE's bounding box, so the profile sits
      // on the sprite itself and not on the padding of its byte-aligned bitmap
      int minX = w, maxX = -1, minY = rows, maxY = -1;
      for (int y = 0; y < rows; y++)
        for (int x = 0; x < w; x++)
          if (mask[y][x]) {
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
          }
      if (maxX < 0)
        return d;
      float cx = (minX + maxX) / 2f, cy = (minY + maxY) / 2f;
      float rx = Math.max(.5f, (maxX - minX) / 2f), ry = Math.max(.5f, (maxY - minY) / 2f);
      // SAME scale as the smooth path (SmoothSpriteBuilder): radius-sized volume, gain, and
      // the roundness blend against the sprite's own relief, both sides normalized. If the
      // two paths disagreed, toggling the voxel look would also change the depth.
      float radiusPx = Math.min(rx, ry);
      int[][] dist0 = chamfer(mask);
      float dmax = 0;
      for (int[] row : dist0)
        for (int v0 : row)
          dmax = Math.max(dmax, v0);
      for (int y = 0; y < rows; y++)
        for (int x = 0; x < w; x++)
          if (mask[y][x]) {
            float u = (x - cx) / rx, v = (y - cy) / ry;
            float p = profile(c.primitive, u, v);
            float own = radiusPx * (dmax <= 0 ? 0 : dist0[y][x] / dmax);
            float geo = radiusPx * p;
            d[y][x] = 2f * c.depth * SmoothSpriteBuilder.GAIN
                * ((1 - c.roundness) * own + c.roundness * geo);
          }
      // scale to the cap instead of clipping: clipping flattens the top of the volume and
      // the oval reads as a box with a flat lid
      float peak = 0;
      for (float[] row : d)
        for (float v : row)
          peak = Math.max(peak, v);
      if (peak > MAX_DEPTH && peak > 0)
        for (float[] row : d)
          for (int i = 0; i < row.length; i++)
            row[i] *= MAX_DEPTH / peak;
      for (int y = 0; y < rows; y++)
        for (int x = 0; x < w; x++)
          if (mask[y][x])
            d[y][x] = Math.max(.5f, d[y][x]);
      return d;
    }
    int[][] dist = chamfer(mask);
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (mask[y][x])
          d[y][x] = Math.max(.5f, c.depth * 2f * (float) Math.sqrt(dist[y][x]));
    return d;
  }

  /** the primitive surfaces of doc §3.4, evaluated at {@code (u,v)} in [-1,1]. */
  public static float profile(Sprite3DConfig.Primitive p, float u, float v) {
    switch (p) {
      case SPHERE:
        return (float) Math.sqrt(Math.max(0, 1 - u * u - v * v));
      case OVOID: // fuller than a sphere: reads better on heads and torsos
        return (float) Math.sqrt(Math.max(0, 1 - Math.pow(u * u + v * v, 0.72)));
      case CYL_V:
        return (float) Math.sqrt(Math.max(0, 1 - u * u));
      case CYL_H:
        return (float) Math.sqrt(Math.max(0, 1 - v * v));
      case CONE:
        return (float) Math.max(0, 1 - Math.sqrt(u * u + v * v));
      case PILLOW:
        // super-ellipse. The exponent decides how boxy it is, and 8 was far too boxy: the
        // profile stayed at ~1 across the whole bounding box, so the sprite came out as a
        // flat-topped slab filling its box — a deformed square. 3 is a rounded cushion.
        return (float) Math.sqrt(Math.max(0, 1 - Math.pow(Math.max(Math.abs(u), Math.abs(v)), 3)));
      default:
        return 1;
    }
  }

  /**
   * Fills the silhouette's ENCLOSED background, in place on the packed rows.
   *
   * <p>An 8-bit graphic draws a solid object as its outline: Monty's items are rings of ink
   * with the paper showing through the middle. Inflating that silhouette literally gives a
   * donut — the object reads as a flat pierced tile, not as a body with volume. Whatever
   * background a flood from the border never reaches is INSIDE the object, so it becomes
   * part of the shape and the inflate has something to give depth to.
   *
   * <p>4-connectivity for the flood, so a diagonal chain of ink still encloses what it
   * surrounds, which is how these outlines are drawn.
   */
  public static byte[] fillHoles(byte[] packed, int wBytes) {
    int rows = packed.length / Math.max(1, wBytes), w = wBytes * 8;
    if (rows <= 2 || w <= 2)
      return packed;
    boolean[][] outside = new boolean[rows][w];
    java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if ((y == 0 || y == rows - 1 || x == 0 || x == w - 1)
            && !lit(packed, wBytes, x, y) && !outside[y][x]) {
          outside[y][x] = true;
          q.add(new int[]{y, x});
        }
    while (!q.isEmpty()) {
      int[] p = q.poll();
      int[][] n4 = {{p[0] - 1, p[1]}, {p[0] + 1, p[1]}, {p[0], p[1] - 1}, {p[0], p[1] + 1}};
      for (int[] nb : n4)
        if (nb[0] >= 0 && nb[0] < rows && nb[1] >= 0 && nb[1] < w
            && !outside[nb[0]][nb[1]] && !lit(packed, wBytes, nb[1], nb[0])) {
          outside[nb[0]][nb[1]] = true;
          q.add(nb);
        }
    }
    byte[] out = packed.clone();
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (!outside[y][x])
          out[y * wBytes + (x >> 3)] |= (byte) (0x80 >> (x & 7));
    return out;
  }

  private static boolean lit(byte[] packed, int wBytes, int x, int y) {
    return (packed[y * wBytes + (x >> 3)] & (0x80 >> (x & 7))) != 0;
  }

  /**
   * Two-pass chamfer distance to the nearest background pixel, in whole steps. Same notion
   * the voxel and smooth builders already use; kept here so every technique shares one.
   */
  public static int[][] chamfer(boolean[][] mask) {
    int rows = mask.length, w = mask[0].length;
    int inf = rows + w;
    int[][] d = new int[rows][w];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        d[y][x] = mask[y][x] ? inf : 0;
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (mask[y][x]) {
          int best = d[y][x];
          if (y > 0)
            best = Math.min(best, d[y - 1][x] + 1);
          if (x > 0)
            best = Math.min(best, d[y][x - 1] + 1);
          d[y][x] = best;
        }
    for (int y = rows - 1; y >= 0; y--)
      for (int x = w - 1; x >= 0; x--)
        if (mask[y][x]) {
          int best = d[y][x];
          if (y < rows - 1)
            best = Math.min(best, d[y + 1][x] + 1);
          if (x < w - 1)
            best = Math.min(best, d[y][x + 1] + 1);
          d[y][x] = best;
        }
    return d;
  }
}
