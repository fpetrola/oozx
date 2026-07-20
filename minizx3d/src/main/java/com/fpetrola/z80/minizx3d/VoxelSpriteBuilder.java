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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;

import java.util.function.IntUnaryOperator;

/**
 * Turns a 16x16 1-bit Spectrum sprite into an inflated voxel model. Depth comes from the
 * chebyshev distance to the sprite's silhouette edge: pixels deep inside the shape bulge
 * further out than pixels at the rim, with a sqrt profile so the volume ROUNDS toward its
 * silhouette instead of extruding as a flat slab — a balloon squeezed from the sprite,
 * front and back symmetric.
 *
 * <p>One box per solid pixel column (x, y): 1x1 footprint, depth from the profile. 256
 * boxes worst case, built once per sprite base and cached by the caller.
 */
public final class VoxelSpriteBuilder {
  public static final int WIDTH = 16;

  /** the graphic's 1-bit mask: wBytes*8 px wide (sprites are 2 bytes/row, 8x8 tiles are 1). */
  static boolean[][] mask(int base, int bytes, int wBytes, IntUnaryOperator memByte) {
    int rows = Math.max(1, bytes / wBytes);
    boolean[][] mask = new boolean[rows][wBytes * 8];
    for (int row = 0; row < rows; row++)
      for (int half = 0; half < wBytes; half++) {
        int b = memByte.applyAsInt(base + row * wBytes + half);
        for (int bit = 0; bit < 8; bit++)
          mask[row][half * 8 + bit] = (b & (0x80 >> bit)) != 0;
      }
    return mask;
  }

  /**
   * Depth is SHAPE-ADAPTIVE: each pixel bulges by the sqrt of its own distance to the
   * silhouette (in pixels), so a thin arm inflates barely and a fat body inflates more —
   * no normalization by the sprite's max, which made every sprite equally deep.
   * {@code smoothLevel} blurs the depth field across neighbouring voxels (the boxes stay:
   * smoothing OVER the voxel look), {@code depthScale} is the user's global multiplier.
   */
  public static Model build(int base, int bytes, int wBytes, IntUnaryOperator memByte,
                            int smoothLevel, float depthScale) {
    boolean[][] mask = mask(base, bytes, wBytes, memByte);
    int rows = mask.length, w = mask[0].length;
    int[][] dist = distanceToEdge(mask);
    float[][] d = new float[rows][w];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        d[y][x] = dist[y][x];
    for (int i = 0; i < smoothLevel; i++)
      d = blurInside(d);

    float[][] depth = new float[rows][w];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        depth[y][x] = Math.max(1f, Math.round(depthScale * 2f * (float) Math.sqrt(d[y][x])));
    return buildWithDepth(mask, depth, 1f, true);
  }

  /**
   * The box emission on its own, driven by a depth given PER PIXEL from outside. This is
   * what lets the primitive profiles and hand-painted depth maps reuse the voxel look
   * instead of re-deriving it: {@link #build} is just this with the distance transform as
   * the depth source.
   *
   * @param fill        0..1 box size, below 1 leaves the gaps of the "Lego" look
   * @param doubleSided centered in z (solid volume) vs bulging toward the camera only
   */
  public static Model buildWithDepth(boolean[][] mask, float[][] depth, float fill,
                                     boolean doubleSided) {
    int rows = mask.length, w = mask[0].length;
    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    MeshPartBuilder part = mb.part("voxels", GL20.GL_TRIANGLES,
        Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.WHITE)));
    float s = Math.max(.05f, Math.min(1f, fill));
    boolean any = false;
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (mask[y][x]) {
          float dz = Math.max(.5f, depth[y][x]);
          // model space: centered, +y up (screen rows grow down). A one-sided sprite keeps
          // its back at z=0 so it sits ON the backdrop instead of sinking through it.
          BoxShapeBuilder.build(part, x - w / 2f + .5f, rows / 2f - y - .5f,
              doubleSided ? 0 : dz / 2f, s, s, dz);
          any = true;
        }
    // An empty bitmap must yield NO model, not an empty one: a mesh with no index buffer
    // blows up at RENDER time ("No buffer allocated"), far from the cause. Callers already
    // treat null as "leave it 2D".
    return any ? mb.end() : null;
  }

  /** vertices this builder would emit: one box (6 quads) per solid pixel. */
  public static int vertexCount(int litPixels) {
    return 24 * litPixels;
  }

  /** 3x3 blur over the solid pixels only — the silhouette (zeros outside) stays put. */
  private static float[][] blurInside(float[][] d) {
    int rows = d.length, w = d[0].length;
    float[][] out = new float[rows][w];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++) {
        if (d[y][x] == 0)
          continue;
        float sum = 0;
        int n = 0;
        for (int dy = -1; dy <= 1; dy++)
          for (int dx = -1; dx <= 1; dx++) {
            int yy = y + dy, xx = x + dx;
            if (yy >= 0 && yy < rows && xx >= 0 && xx < w && d[yy][xx] > 0) {
              sum += d[yy][xx];
              n++;
            }
          }
        out[y][x] = sum / n;
      }
    return out;
  }

  /** chebyshev distance to the nearest empty/outside cell, two-pass over the grid. */
  private static int[][] distanceToEdge(boolean[][] mask) {
    int rows = mask.length, w = mask[0].length, inf = (rows + w) * 2;
    int[][] d = new int[rows][w];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        d[y][x] = mask[y][x] ? inf : 0;
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (d[y][x] > 0) {
          int best = Math.min(y > 0 ? d[y - 1][x] : 0, x > 0 ? d[y][x - 1] : 0);
          if (y > 0 && x > 0) best = Math.min(best, d[y - 1][x - 1]);
          if (y > 0 && x < w - 1) best = Math.min(best, d[y - 1][x + 1]);
          d[y][x] = Math.min(d[y][x], best + 1);
        }
    for (int y = rows - 1; y >= 0; y--)
      for (int x = w - 1; x >= 0; x--)
        if (d[y][x] > 0) {
          int best = Math.min(y < rows - 1 ? d[y + 1][x] : 0, x < w - 1 ? d[y][x + 1] : 0);
          if (y < rows - 1 && x < w - 1) best = Math.min(best, d[y + 1][x + 1]);
          if (y < rows - 1 && x > 0) best = Math.min(best, d[y + 1][x - 1]);
          d[y][x] = Math.min(d[y][x], best + 1);
        }
    return d;
  }

  private VoxelSpriteBuilder() {
  }
}
