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
  /**
   * Rebase of the depth slider for the PRIMITIVE path only. The old inflation peaked around
   * {@code 2*sqrt(radius)}; the primitive solid peaks at {@code radius}, so a slider that a
   * user had calibrated for the old curve (0.41 was a real saved value) made every sprite a
   * flat medallion — depth 3 px on a 16 px body. This puts that setting back at "as deep as
   * it is wide", and D/C keeps working from there. Only the primitives are rebased: the
   * plain INFLATE path still means exactly what it always meant.
   */
  static final float GAIN = 2.4f;
  private static final float INSIDE = 0.45f;        // <.5 keeps single-pixel diagonals connected

  /**
   * {@code smoothLevel} drives how far from pixel art the surface departs: the silhouette
   * mixes nearest (blocky) toward bilinear (rounded) sampling, and the height field gets
   * that many blur passes. Depth is shape-adaptive like the voxel mode — sqrt of the local
   * distance to the silhouette, in pixels — times the user's {@code depthScale}.
   */
  public static Model build(int base, int bytes, int wBytes, IntUnaryOperator memByte,
                            int smoothLevel, float depthScale) {
    return build(base, bytes, wBytes, memByte, smoothLevel, depthScale, null, 1f);
  }

  /**
   * Same mesh, but the volume can come from a PRIMITIVE instead of the distance transform:
   * the thickness at each point is the primitive's surface over the silhouette's bounding
   * box, and the usual blur passes then smooth it — "geometric shape first, smoothing
   * after". The silhouette machinery is untouched, so the outline is as clean as ever and
   * the rim still closes at zero, which is what lets the front and back halves join.
   *
   * @param primitive null keeps the distance-transform inflation (the original behaviour)
   * @param roundness 0 = flat slab of the silhouette, 1 = the primitive's full curvature
   */
  public static Model build(int base, int bytes, int wBytes, IntUnaryOperator memByte,
                            int smoothLevel, float depthScale,
                            Sprite3DConfig.Primitive primitive, float roundness) {
    boolean[][] mask = VoxelSpriteBuilder.mask(base, bytes, wBytes, memByte);
    int ny = mask.length * K, nx = mask[0].length * K;

    // corner lattice over the sprite: sampled mask = the (possibly smoothed) silhouette
    float[][] h = heights(mask, smoothLevel, depthScale, primitive, roundness);

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

  /**
   * Vertices this builder would emit for a {@code wBytes} x {@code rows} bitmap: the whole
   * corner lattice, front and mirrored back. Unlike the voxel mode this does not depend on
   * how many pixels are lit — the lattice is allocated over the bounding box either way.
   */
  /**
   * Peak half-thickness this configuration would give, in pixels, without touching GL.
   * Total depth is twice this. Exists so "is this sprite as deep as it is wide?" can be
   * answered with a number instead of by squinting at a screenshot.
   */
  public static float peakHeight(boolean[][] mask, int smoothLevel, float depthScale,
                                 Sprite3DConfig.Primitive primitive, float roundness) {
    float[][] h = heights(mask, smoothLevel, depthScale, primitive, roundness);
    float max = 0;
    for (float[] row : h)
      for (float v : row)
        max = Math.max(max, v);
    return max;
  }

  public static int vertexCount(int wBytes, int rows) {
    return 2 * (rows * K + 1) * (wBytes * 8 * K + 1);
  }

  /** smoothed-silhouette distance field turned into the inflation height, on the corner lattice. */
  private static float[][] heights(boolean[][] mask, int smoothLevel, float depthScale,
                                   Sprite3DConfig.Primitive primitive, float roundness) {
    int ny = mask.length * K, NX = mask[0].length * K;
    float t = Math.min(1f, smoothLevel / 3f); // 0 = pixelated silhouette, 1 = fully rounded
    float[][] field = new float[ny + 1][NX + 1];
    for (int gy = 0; gy <= ny; gy++)
      for (int gx = 0; gx <= NX; gx++) {
        float bi = bilinear(mask, gx / (float) K, gy / (float) K);
        float nn = at(mask, (int) ((gy - .01f) / K), (int) ((gx - .01f) / K));
        field[gy][gx] = nn * (1 - t) + bi * t;
      }

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

    float[][] h = new float[ny + 1][NX + 1];
    if (primitive == null) {
      for (int gy = 0; gy <= ny; gy++)
        for (int gx = 0; gx <= NX; gx++)
          h[gy][gx] = depthScale * 2f * (float) Math.sqrt(d[gy][gx] / K); // lattice -> pixels
    } else {
      // (u,v) in [-1,1] over the SILHOUETTE's bounding box, so the shape sits on the sprite
      // and not on the padding of its byte-aligned bitmap
      int minX = NX, maxX = 0, minY = ny, maxY = 0;
      for (int gy = 0; gy <= ny; gy++)
        for (int gx = 0; gx <= NX; gx++)
          if (d[gy][gx] > 0) {
            minX = Math.min(minX, gx);
            maxX = Math.max(maxX, gx);
            minY = Math.min(minY, gy);
            maxY = Math.max(maxY, gy);
          }
      float cx = (minX + maxX) / 2f, cy = (minY + maxY) / 2f;
      float rx = Math.max(1f, (maxX - minX) / 2f), ry = Math.max(1f, (maxY - minY) / 2f);
      // The volume is scaled by the sprite's OWN radius in pixels, so a sphere really is a
      // sphere: half-thickness = half-width, i.e. the object is as deep as it is wide. A
      // fixed factor made everything a flat medallion no matter its size. The smaller half
      // extent is the one used, so an elongated sprite gets a cylinder as thick as it is
      // narrow instead of one absurdly deeper than its own height.
      float radiusPx = Math.min(rx, ry) / K;
      float dmax = 0;
      for (int gy = 0; gy <= ny; gy++)
        for (int gx = 0; gx <= NX; gx++)
          dmax = Math.max(dmax, d[gy][gx]);
      for (int gy = 0; gy <= ny; gy++)
        for (int gx = 0; gx <= NX; gx++) {
          if (d[gy][gx] <= 0)
            continue;
          float p = SpriteFx.profile(primitive, (gx - cx) / rx, (gy - cy) / ry);
          // taper to zero within a pixel of the rim: the front and back halves have to
          // meet at h=0 or the silhouette shows a cliff instead of an edge
          float taper = Math.min(1f, (float) Math.sqrt(d[gy][gx] / K));
          // roundness blends the PRIMITIVE against the sprite's OWN relief instead of
          // against a constant, so the character still looks like itself. Both sides are
          // NORMALIZED to the same peak (radiusPx) first: mixing raw magnitudes let the
          // distance term — which is much smaller — drag the whole body flat, which is why
          // a "rounder" setting was coming out shallower instead of just less generic.
          // LINEAR in the distance, not sqrt: the square root compressed everything inward
          // toward the full radius, so a thin arm ended up as thick as the torso and the
          // character read as an undifferentiated blob. Linear keeps the sprite's own
          // proportions while the thickest point still reaches "as deep as it is wide".
          float own = radiusPx * (dmax <= 0 ? 0 : d[gy][gx] / dmax);
          float geo = radiusPx * p;
          h[gy][gx] = depthScale * GAIN * ((1 - roundness) * own + roundness * geo) * taper;
        }
    }
    for (int i = 0; i < Math.max(1, smoothLevel); i++)
      h = blur(h);
    return h;
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
