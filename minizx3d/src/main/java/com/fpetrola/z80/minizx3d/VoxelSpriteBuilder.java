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
  public static final int SIZE = 16;
  private static final float MAX_DEPTH = 8f;

  /** reads one byte of static game memory (the catalog); sprite = 32 bytes at base. */
  public static Model build(int base, IntUnaryOperator memByte) {
    boolean[][] mask = new boolean[SIZE][SIZE];
    for (int row = 0; row < SIZE; row++)
      for (int half = 0; half < 2; half++) {
        int b = memByte.applyAsInt(base + row * 2 + half);
        for (int bit = 0; bit < 8; bit++)
          mask[row][half * 8 + bit] = (b & (0x80 >> bit)) != 0;
      }

    int[][] dist = distanceToEdge(mask);
    int dMax = 1;
    for (int[] r : dist)
      for (int d : r)
        dMax = Math.max(dMax, d);

    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    MeshPartBuilder part = mb.part("voxels", GL20.GL_TRIANGLES,
        Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.WHITE)));
    for (int y = 0; y < SIZE; y++)
      for (int x = 0; x < SIZE; x++)
        if (mask[y][x]) {
          float depth = Math.max(1f,
              Math.round(MAX_DEPTH * (float) Math.sqrt(dist[y][x] / (float) dMax)));
          // model space: centered, +y up (screen rows grow down), z bulges both ways
          BoxShapeBuilder.build(part,
              x - SIZE / 2f + .5f, SIZE / 2f - y - .5f, 0, 1, 1, depth);
        }
    return mb.end();
  }

  /** chebyshev distance to the nearest empty/outside cell, two-pass over the 16x16 grid. */
  private static int[][] distanceToEdge(boolean[][] mask) {
    int inf = SIZE * 2;
    int[][] d = new int[SIZE][SIZE];
    for (int y = 0; y < SIZE; y++)
      for (int x = 0; x < SIZE; x++)
        d[y][x] = mask[y][x] ? inf : 0;
    for (int y = 0; y < SIZE; y++)
      for (int x = 0; x < SIZE; x++)
        if (d[y][x] > 0) {
          int best = Math.min(y > 0 ? d[y - 1][x] : 0, x > 0 ? d[y][x - 1] : 0);
          if (y > 0 && x > 0) best = Math.min(best, d[y - 1][x - 1]);
          if (y > 0 && x < SIZE - 1) best = Math.min(best, d[y - 1][x + 1]);
          d[y][x] = Math.min(d[y][x], best + 1);
        }
    for (int y = SIZE - 1; y >= 0; y--)
      for (int x = SIZE - 1; x >= 0; x--)
        if (d[y][x] > 0) {
          int best = Math.min(y < SIZE - 1 ? d[y + 1][x] : 0, x < SIZE - 1 ? d[y][x + 1] : 0);
          if (y < SIZE - 1 && x < SIZE - 1) best = Math.min(best, d[y + 1][x + 1]);
          if (y < SIZE - 1 && x > 0) best = Math.min(best, d[y + 1][x - 1]);
          d[y][x] = Math.min(d[y][x], best + 1);
        }
    return d;
  }

  private VoxelSpriteBuilder() {
  }
}
