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

/**
 * Quality mode (doc §3.6): a single smooth skin instead of pixels or a heightfield.
 *
 * <ol>
 *   <li>Build a 3D occupancy field from the silhouette and its per-pixel thickness
 *       ({@link SpriteFx#depthField}), so the volume is the inflated sprite, not a slab.</li>
 *   <li>Blur it with a separable box kernel — this is what fuses neighbouring pixels and
 *       rounds the staircase away; the radius is the config's {@code smoothing}.</li>
 *   <li>Extract the isosurface with NAIVE SURFACE NETS: one vertex per cell that has a sign
 *       change, placed at the average of its crossing edges. Chosen over marching cubes
 *       because it needs no 256-case table and gives a smoother, more even mesh.</li>
 * </ol>
 *
 * <p>Normals come from the field's gradient (central differences), which is both cheaper
 * and smoother than averaging face normals.
 */
public final class SurfaceNetsBuilder {
  private SurfaceNetsBuilder() {
  }

  /** the field is sampled at this many cells per sprite pixel. */
  private static final int RES = 1;
  private static final float ISO = 0.5f;

  /**
   * Isolevel that keeps the blurred volume the same size as the unblurred one. A box blur
   * bleeds mass outward and drops the border below a FIXED 0.5, so raising the smoothing
   * did not just round the voxels, it shrank the whole sprite — at high levels Willy melted
   * to a pebble. Picking the level as the Nth largest value, where N is the solid cell count
   * before blurring, keeps the surface where it was and lets the blur do only what it is
   * wanted for: rounding.
   */
  private static float volumePreservingIso(float[] f, int solidCells) {
    if (solidCells <= 0 || solidCells >= f.length)
      return ISO;
    float[] sorted = f.clone();
    java.util.Arrays.sort(sorted);
    float iso = sorted[sorted.length - solidCells];
    return Math.max(1e-3f, Math.min(ISO, iso));
  }

  public static Model build(SpriteBitmap b, Sprite3DConfig cfg) {
    boolean[][] mask = b.mask();
    int rows = mask.length, w = mask[0].length;
    float[][] depth = SpriteFx.depthField(b, cfg, mask);
    int maxD = 1;
    for (float[] row : depth)
      for (float v : row)
        maxD = Math.max(maxD, (int) Math.ceil(v));
    // one cell of padding all round so the surface closes instead of being cut by the box
    int nx = w * RES + 2, ny = rows * RES + 2, nz = maxD * RES + 2;
    float[] f = new float[nx * ny * nz];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++) {
        if (!mask[y][x])
          continue;
        int half = Math.max(1, (int) Math.ceil(depth[y][x] * RES / 2f));
        int cz = nz / 2;
        for (int z = cz - half; z <= cz + half; z++)
          if (z >= 0 && z < nz)
            f[idx(x * RES + 1, y * RES + 1, z, nx, ny)] = 1;
      }
    int solid = 0;
    for (float v : f)
      if (v >= ISO)
        solid++;
    // smoothLevel is the single dial the viewer's S/X keys drive: 1 rounds the voxel
    // corners barely, and each step melts them further into one another. The field is the
    // SAME occupancy the voxel path uses, so level 1 is recognisably the same object.
    blur(f, nx, ny, nz, Math.max(0, cfg.smoothLevel - 1));
    final float iso = volumePreservingIso(f, solid);

    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    MeshPartBuilder part = mb.part("skin", GL20.GL_TRIANGLES,
        Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.WHITE)));

    // cell -> emitted vertex index, or -1
    int[] cellVertex = new int[(nx - 1) * (ny - 1) * (nz - 1)];
    java.util.Arrays.fill(cellVertex, -1);
    short[] ids = new short[cellVertex.length];
    for (int z = 0; z < nz - 1; z++)
      for (int y = 0; y < ny - 1; y++)
        for (int x = 0; x < nx - 1; x++) {
          // average the crossings on the 12 edges of the cell: the surface-nets vertex
          float sx = 0, sy = 0, sz = 0;
          int n = 0;
          for (int c = 0; c < 8; c++) {
            int cx = x + (c & 1), cy = y + ((c >> 1) & 1), cz = z + ((c >> 2) & 1);
            boolean in = f[idx(cx, cy, cz, nx, ny)] >= iso;
            for (int e = 0; e < 3; e++) {
              int ox = e == 0 ? 1 : 0, oy = e == 1 ? 1 : 0, oz = e == 2 ? 1 : 0;
              int ax = cx + ox, ay = cy + oy, az = cz + oz;
              if (ax >= nx || ay >= ny || az >= nz || (c & (1 << e)) != 0)
                continue;
              boolean in2 = f[idx(ax, ay, az, nx, ny)] >= iso;
              if (in == in2)
                continue;
              float va = f[idx(cx, cy, cz, nx, ny)], vb = f[idx(ax, ay, az, nx, ny)];
              float t = Math.abs(vb - va) < 1e-5f ? .5f : (iso - va) / (vb - va);
              sx += cx + ox * t;
              sy += cy + oy * t;
              sz += cz + oz * t;
              n++;
            }
          }
          if (n == 0)
            continue;
          float vx = sx / n, vy = sy / n, vz = sz / n;
          float[] nrm = gradient(f, nx, ny, nz, Math.round(vx), Math.round(vy), Math.round(vz));
          // sprite space: centered, +y up (screen rows grow down)
          short id = part.vertex(
              vx / RES - w / 2f, rows / 2f - vy / RES, vz / RES - nz / (2f * RES),
              nrm[0], nrm[1], nrm[2]);
          int ci = cellIdx(x, y, z, nx, ny, nz);
          cellVertex[ci] = 1;
          ids[ci] = id;
        }

    // quad per edge with a sign change, joining the four cells around it
    int quads = 0;
    for (int z = 1; z < nz - 1; z++)
      for (int y = 1; y < ny - 1; y++)
        for (int x = 1; x < nx - 1; x++) {
          boolean in = f[idx(x, y, z, nx, ny)] >= iso;
          for (int e = 0; e < 3; e++) {
            int ox = e == 0 ? 1 : 0, oy = e == 1 ? 1 : 0, oz = e == 2 ? 1 : 0;
            if (x + ox >= nx || y + oy >= ny || z + oz >= nz)
              continue;
            if (in == (f[idx(x + ox, y + oy, z + oz, nx, ny)] >= iso))
              continue;
            // the four cells sharing this edge are the ones offset in the other two axes
            int[] du = e == 0 ? new int[]{0, 1, 0} : new int[]{1, 0, 0};
            int[] dv = e == 2 ? new int[]{0, 1, 0} : new int[]{0, 0, 1};
            int c0 = cellIdx(x - du[0] - dv[0], y - du[1] - dv[1], z - du[2] - dv[2], nx, ny, nz);
            int c1 = cellIdx(x - dv[0], y - dv[1], z - dv[2], nx, ny, nz);
            int c2 = cellIdx(x, y, z, nx, ny, nz);
            int c3 = cellIdx(x - du[0], y - du[1], z - du[2], nx, ny, nz);
            if (c0 < 0 || c1 < 0 || c2 < 0 || c3 < 0
                || cellVertex[c0] < 0 || cellVertex[c1] < 0
                || cellVertex[c2] < 0 || cellVertex[c3] < 0)
              continue;
            // The vertex position MIRRORS y (model y grows up, sprite rows grow down), and
            // mirroring one axis reverses orientation: emitting the field-space winding here
            // left every face pointing inward, so the front was culled and the sprite showed
            // up as a hollow shell. These are the field-space orders with two swapped.
            if (in) {
              part.triangle(ids[c0], ids[c2], ids[c1]);
              part.triangle(ids[c0], ids[c3], ids[c2]);
            } else {
              part.triangle(ids[c0], ids[c1], ids[c2]);
              part.triangle(ids[c0], ids[c2], ids[c3]);
            }
            quads++;
          }
        }
    // no isosurface (silhouette too thin for the blur radius): no model, never an empty
    // mesh — that only surfaces as a crash at render time
    return quads == 0 ? null : mb.end();
  }

  /**
   * Rough vertex count: surface nets emits about one vertex per surface cell, so the shell
   * of the volume. Deliberately generous — this feeds the mesh-limit check, where guessing
   * low is what throws "Too many vertices" at bake time.
   */
  public static int vertexEstimate(SpriteBitmap b, Sprite3DConfig cfg) {
    int lit = b.litPixels() * (cfg.epx > 1 ? cfg.epx * cfg.epx : 1);
    return 8 * lit + 64;
  }

  private static int idx(int x, int y, int z, int nx, int ny) {
    return (z * ny + y) * nx + x;
  }

  private static int cellIdx(int x, int y, int z, int nx, int ny, int nz) {
    if (x < 0 || y < 0 || z < 0 || x >= nx - 1 || y >= ny - 1 || z >= nz - 1)
      return -1; // z was unchecked and ran off the end of the cell array
    return (z * (ny - 1) + y) * (nx - 1) + x;
  }

  /** separable box blur: the knob that decides how much the pixels melt together. */
  private static void blur(float[] f, int nx, int ny, int nz, int radius) {
    if (radius <= 0)
      return;
    float[] tmp = new float[f.length];
    for (int pass = 0; pass < 3; pass++) {
      for (int z = 0; z < nz; z++)
        for (int y = 0; y < ny; y++)
          for (int x = 0; x < nx; x++) {
            float s = 0;
            int n = 0;
            for (int k = -radius; k <= radius; k++) {
              int cx = pass == 0 ? x + k : x, cy = pass == 1 ? y + k : y, cz = pass == 2 ? z + k : z;
              if (cx < 0 || cy < 0 || cz < 0 || cx >= nx || cy >= ny || cz >= nz)
                continue;
              s += f[idx(cx, cy, cz, nx, ny)];
              n++;
            }
            tmp[idx(x, y, z, nx, ny)] = n == 0 ? 0 : s / n;
          }
      System.arraycopy(tmp, 0, f, 0, f.length);
    }
  }

  private static float[] gradient(float[] f, int nx, int ny, int nz, int x, int y, int z) {
    x = Math.max(1, Math.min(nx - 2, x));
    y = Math.max(1, Math.min(ny - 2, y));
    z = Math.max(1, Math.min(nz - 2, z));
    float gx = f[idx(x + 1, y, z, nx, ny)] - f[idx(x - 1, y, z, nx, ny)];
    float gy = f[idx(x, y + 1, z, nx, ny)] - f[idx(x, y - 1, z, nx, ny)];
    float gz = f[idx(x, y, z + 1, nx, ny)] - f[idx(x, y, z - 1, nx, ny)];
    // the field grows INTO the solid, so the outward normal is the negated gradient;
    // y is flipped again because sprite rows grow down and model y grows up
    float len = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
    if (len < 1e-5f)
      return new float[]{0, 0, 1};
    return new float[]{-gx / len, gy / len, -gz / len};
  }
}
