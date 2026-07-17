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

import java.util.function.IntUnaryOperator;

/**
 * The smooth alternative to {@link VoxelSpriteBuilder}: instead of one box per pixel, the
 * sprite becomes a single INFLATED surface — the pixel-art-to-3D equivalent of what xBRZ
 * does in 2D. Three continuous steps replace the discrete ones:
 * <ol>
 *   <li><b>silhouette smoothing</b>: the 16x16 mask is bilinearly upsampled onto a 4x
 *       lattice and re-thresholded, which turns pixel staircases into straight diagonals
 *       and rounds corners (the xBRZ-ish part);</li>
 *   <li><b>rounded depth</b>: height = sqrt of the distance to the smoothed silhouette —
 *       same inflation profile as the voxels, but sampled continuously, so the volume is a
 *       balloon instead of a staircase of slabs;</li>
 *   <li><b>one heightfield mesh</b> front + mirrored back, meeting at the rim (h=0), with
 *       normals from the height gradient — smooth shading instead of per-box facets.</li>
 * </ol>
 */
public final class SmoothSpriteBuilder {
  private static final int K = 4;
  private static final float MAX_DEPTH = 4.5f;      // half-depth at the fattest point, pixels
  private static final float INSIDE = 0.45f;        // <.5 keeps single-pixel diagonals connected

  public static Model build(int base, int bytes, int wBytes, IntUnaryOperator memByte) {
    boolean[][] mask = VoxelSpriteBuilder.mask(base, bytes, wBytes, memByte);
    int ny = mask.length * K, nx = mask[0].length * K;

    // corner lattice over the sprite: bilinear sample of the mask = smoothed field
    float[][] h = heights(mask);

    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    MeshPartBuilder part = mb.part("inflated", GL20.GL_TRIANGLES,
        Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.WHITE)));

    short[][] front = vertices(part, h, +1);
    short[][] back = vertices(part, h, -1);
    for (int gy = 0; gy < ny; gy++)
      for (int gx = 0; gx < nx; gx++) {
        if (h[gy][gx] == 0 && h[gy][gx + 1] == 0 && h[gy + 1][gx] == 0 && h[gy + 1][gx + 1] == 0)
          continue;
        // front faces +z (ccw seen from the camera), back mirrored
        part.triangle(front[gy][gx], front[gy + 1][gx], front[gy][gx + 1]);
        part.triangle(front[gy][gx + 1], front[gy + 1][gx], front[gy + 1][gx + 1]);
        part.triangle(back[gy][gx], back[gy][gx + 1], back[gy + 1][gx]);
        part.triangle(back[gy][gx + 1], back[gy + 1][gx + 1], back[gy + 1][gx]);
      }
    return mb.end();
  }

  /** smoothed-silhouette distance field turned into the inflation height, on the corner lattice. */
  private static float[][] heights(boolean[][] mask) {
    int ny = mask.length * K, NX = mask[0].length * K;
    float[][] field = new float[ny + 1][NX + 1];
    for (int gy = 0; gy <= ny; gy++)
      for (int gx = 0; gx <= NX; gx++)
        field[gy][gx] = bilinear(mask, gx / (float) K, gy / (float) K);

    // chamfer distance (lattice steps) to the nearest outside corner
    float inf = (ny + NX) * 2f, sq2 = 1.4142f;
    float[][] d = new float[ny + 1][NX + 1];
    for (int gy = 0; gy <= ny; gy++)
      for (int gx = 0; gx <= NX; gx++)
        d[gy][gx] = field[gy][gx] >= INSIDE ? inf : 0;
    for (int gy = 0; gy <= ny; gy++)
      for (int gx = 0; gx <= NX; gx++)
        pass(d, gx, gy, sq2);
    for (int gy = ny; gy >= 0; gy--)
      for (int gx = NX; gx >= 0; gx--)
        pass(d, gx, gy, sq2);

    float dMax = 0.01f;
    for (float[] row : d)
      for (float v : row)
        dMax = Math.max(dMax, v);
    float[][] h = new float[ny + 1][NX + 1];
    for (int gy = 0; gy <= ny; gy++)
      for (int gx = 0; gx <= NX; gx++)
        h[gy][gx] = MAX_DEPTH * (float) Math.sqrt(d[gy][gx] / dMax);
    return blur(h);
  }

  private static void pass(float[][] d, int gx, int gy, float sq2) {
    if (d[gy][gx] == 0)
      return;
    int ny = d.length - 1, NX = d[0].length - 1;
    for (int dy = -1; dy <= 1; dy++)
      for (int dx = -1; dx <= 1; dx++) {
        int x = gx + dx, y = gy + dy;
        if ((dx | dy) != 0 && x >= 0 && x <= NX && y >= 0 && y <= ny)
          d[gy][gx] = Math.min(d[gy][gx], d[y][x] + (dx * dy != 0 ? sq2 : 1));
      }
  }

  /** the mask sampled bilinearly at pixel-space point (px, py); pixel centers at +.5. */
  private static float bilinear(boolean[][] mask, float px, float py) {
    float u = px - .5f, v = py - .5f;
    int c = (int) Math.floor(u), r = (int) Math.floor(v);
    float fu = u - c, fv = v - r;
    float v00 = at(mask, r, c), v01 = at(mask, r, c + 1);
    float v10 = at(mask, r + 1, c), v11 = at(mask, r + 1, c + 1);
    return (v00 * (1 - fu) + v01 * fu) * (1 - fv) + (v10 * (1 - fu) + v11 * fu) * fv;
  }

  private static float at(boolean[][] mask, int r, int c) {
    return r >= 0 && r < mask.length && c >= 0 && c < mask[r].length && mask[r][c] ? 1 : 0;
  }

  private static float[][] blur(float[][] h) {
    int ny = h.length - 1, NX = h[0].length - 1;
    float[][] out = new float[ny + 1][NX + 1];
    for (int gy = 0; gy <= ny; gy++)
      for (int gx = 0; gx <= NX; gx++) {
        if (h[gy][gx] == 0)
          continue; // the rim stays pinned at zero so front and back keep meeting there
        float sum = 0;
        int n = 0;
        for (int dy = -1; dy <= 1; dy++)
          for (int dx = -1; dx <= 1; dx++) {
            int x = gx + dx, y = gy + dy;
            if (x >= 0 && x <= NX && y >= 0 && y <= ny) {
              sum += h[y][x];
              n++;
            }
          }
        out[gy][gx] = sum / n;
      }
    return out;
  }

  /** one surface of the balloon: the heightfield at z = side*h, normals from the gradient. */
  private static short[][] vertices(MeshPartBuilder part, float[][] h, int side) {
    int ny = h.length - 1, NX = h[0].length - 1;
    float rows = ny / (float) K, w = NX / (float) K;
    short[][] idx = new short[ny + 1][NX + 1];
    for (int gy = 0; gy <= ny; gy++)
      for (int gx = 0; gx <= NX; gx++) {
        float x = gx / (float) K - w / 2f;
        float y = rows / 2f - gy / (float) K;       // screen rows grow down, model y grows up
        float hl = h[gy][Math.max(0, gx - 1)], hr = h[gy][Math.min(NX, gx + 1)];
        float hu = h[Math.max(0, gy - 1)][gx], hd = h[Math.min(ny, gy + 1)][gx];
        float gradX = (hr - hl) * K / 2f, gradY = -(hd - hu) * K / 2f;
        float nx = -gradX * side, nyv = -gradY * side, nz = side;
        float len = (float) Math.sqrt(nx * nx + nyv * nyv + nz * nz);
        idx[gy][gx] = part.vertex(x, y, side * h[gy][gx], nx / len, nyv / len, nz / len);
      }
    return idx;
  }

  private SmoothSpriteBuilder() {
  }
}
