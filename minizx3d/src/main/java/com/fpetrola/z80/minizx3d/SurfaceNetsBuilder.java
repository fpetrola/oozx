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
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A skin stretched over the voxels: the same occupancy the voxel path builds, sampled on a
 * finer grid, blurred, and turned into one surface. The dial goes from "the cubes with their
 * corners just cut" to "fully melted" without the volume underneath changing.
 *
 * <p>This is a port of Mikola Lysenko's NAIVE SURFACE NETS (MIT) — its edge table and rolling
 * neighbour buffer — and not a hand-rolled isosurface. The hand-rolled one carried two bugs
 * that writing it from scratch invites: a cell index that never bounds-checked z, and a face
 * winding that ignored the y mirror. Four details matter, and all four were wrong before:
 *
 * <ol>
 *   <li><b>{@link #resolutionFor Supersampling}</b>: the field is sampled at ~3 cells per
 *       sprite pixel. At one cell per pixel there is nowhere for a rounded surface to live
 *       and the blur radius collapses to a useless integer.</li>
 *   <li><b>The dial is a FLOAT</b> ({@code smoothing}). It used to be an int, so anything
 *       under 1 truncated to zero: the knob did nothing at all and then did too much.
 *       Radius is {@code smoothing * res * 1.6}, in cells.</li>
 *   <li><b>Inside is NEGATIVE</b> ({@code field = 0.5 - occupancy}, iso at 0) — what the
 *       algorithm's mask test expects.</li>
 *   <li><b>The material does not cull.</b> Sprite y is mirrored into model space, which
 *       reverses winding; rather than depend on getting that right, both faces are drawn.
 *       A wrong winding here reads as a hollow shell, which is exactly what it did.</li>
 * </ol>
 */
public final class SurfaceNetsBuilder {
  private SurfaceNetsBuilder() {
  }

  private static final int PAD = 2;
  /** a libGDX mesh indexes its vertices with shorts. */
  private static final int MAX_VERTICES = 32000;

  private static final int[] CUBE_EDGES = new int[24];
  private static final int[] EDGE_TABLE = new int[256];

  static {
    int k = 0;
    for (int i = 0; i < 8; ++i)
      for (int j = 1; j <= 4; j <<= 1) {
        int p = i ^ j;
        if (i <= p) {
          CUBE_EDGES[k++] = i;
          CUBE_EDGES[k++] = p;
        }
      }
    for (int i = 0; i < 256; ++i) {
      int em = 0;
      for (int j = 0; j < 24; j += 2) {
        boolean a = (i & (1 << CUBE_EDGES[j])) != 0;
        boolean b = (i & (1 << CUBE_EDGES[j + 1])) != 0;
        em |= a != b ? (1 << (j >> 1)) : 0;
      }
      EDGE_TABLE[i] = em;
    }
  }

  /** cells per sprite pixel: finer for small sprites, coarser as they grow. */
  private static int resolutionFor(int w, int h) {
    int d = Math.max(w, h);
    return d <= 40 ? 3 : d <= 72 ? 2 : 1;
  }

  public static Model build(SpriteBitmap b, Sprite3DConfig cfg) {
    boolean[][] mask = b.mask();
    int rows = mask.length, w = mask[0].length;
    float[][] thickness = SpriteFx.depthField(b, cfg, mask);
    float maxT = .05f;
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w; x++)
        if (mask[y][x])
          maxT = Math.max(maxT, thickness[y][x]);

    int res = resolutionFor(w, rows);
    int fx, fy, fz;
    for (; ; ) {
      fx = (w - 1) * res + 1 + 2 * PAD;
      fy = (rows - 1) * res + 1 + 2 * PAD;
      fz = (int) Math.ceil(maxT * res) + 1 + 2 * PAD;
      if ((long) fx * fy * fz <= 1_100_000L || res <= 1)
        break;
      res--;
    }

    float half = maxT / 2f;
    float[] occ = new float[fx * fy * fz];
    for (int c = 0; c < fz; c++) {
      float gz = (c - PAD) / (float) res - half;
      for (int bb = 0; bb < fy; bb++) {
        int py = Math.round((bb - PAD) / (float) res);
        for (int a = 0; a < fx; a++) {
          int px = Math.round((a - PAD) / (float) res);
          if (px < 0 || px >= w || py < 0 || py >= rows || !mask[py][px])
            continue;
          float t = thickness[py][px];
          boolean in = cfg.doubleSided ? Math.abs(gz) <= t / 2 : gz >= 0 && gz <= t;
          if (in)
            occ[a + fx * (bb + fy * c)] = 1;
        }
      }
    }

    // A box blur only takes an INTEGER radius, so the dial jumped: 0 did nothing and the
    // first step already rounded a lot. Blurring at the radius above and then LERPING back
    // toward the sharp field by the fractional part makes every value of the dial count.
    float rf = cfg.smoothing * res * 1.6f;
    int ri = (int) Math.ceil(rf);
    if (ri > 0) {
      float[] sharp = occ.clone();
      float[] tmp = new float[occ.length];
      for (int axis = 0; axis < 3; axis++) {
        blurAxis(occ, tmp, fx, fy, fz, axis, ri);
        float[] swap = occ;
        occ = tmp;
        tmp = swap;
      }
      float t = Math.min(1f, rf / ri);
      for (int i = 0; i < occ.length; i++)
        occ[i] = sharp[i] + (occ[i] - sharp[i]) * t;
    }
    // iso at ZERO with inside negative: what the mask test in surfaceNets expects
    float[] field = new float[occ.length];
    for (int i = 0; i < occ.length; i++)
      field[i] = .5f - occ[i];

    List<float[]> verts = new ArrayList<>();
    List<int[]> quads = new ArrayList<>();
    surfaceNets(field, fx, fy, fz, verts, quads);
    if (verts.isEmpty() || quads.isEmpty() || verts.size() > MAX_VERTICES)
      return null;

    // positions in the SAME model space the voxel builder uses, so moving the dial does not
    // make the sprite jump: pixel (x,y) sits at (x - w/2 + .5, rows/2 - y - .5)
    float[] pos = new float[verts.size() * 3];
    for (int i = 0; i < verts.size(); i++) {
      float[] v = verts.get(i);
      float ex = (v[0] - PAD) / res, ey = (v[1] - PAD) / res, ez = (v[2] - PAD) / res - half;
      pos[i * 3] = ex - w / 2f + .5f;
      pos[i * 3 + 1] = rows / 2f - ey - .5f;
      pos[i * 3 + 2] = ez;
    }
    float[] nrm = vertexNormals(pos, quads);

    ModelBuilder mb = new ModelBuilder();
    mb.begin();
    Material m = new Material(ColorAttribute.createDiffuse(Color.WHITE));
    m.set(new IntAttribute(IntAttribute.CullFace, GL20.GL_NONE));
    MeshPartBuilder part = mb.part("skin", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, m);
    short[] ids = new short[verts.size()];
    for (int i = 0; i < verts.size(); i++)
      ids[i] = part.vertex(pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2],
          nrm[i * 3], nrm[i * 3 + 1], nrm[i * 3 + 2]);
    for (int[] q : quads) {
      part.triangle(ids[q[0]], ids[q[1]], ids[q[2]]);
      part.triangle(ids[q[0]], ids[q[2]], ids[q[3]]);
    }
    return mb.end();
  }

  /** Naive Surface Nets (Mikola Lysenko, MIT): one vertex per sign-changing cell. */
  private static void surfaceNets(float[] data, int dx, int dy, int dz,
                                  List<float[]> vertices, List<int[]> faces) {
    int n = 0;
    int[] x = new int[3];
    int[] R = {1, dx + 1, (dx + 1) * (dy + 1)};
    float[] grid = new float[8];
    int bufNo = 1;
    int[] buffer = new int[R[2] * 2];

    for (x[2] = 0; x[2] < dz - 1; ++x[2], n += dx, bufNo ^= 1, R[2] = -R[2]) {
      int m = 1 + (dx + 1) * (1 + bufNo * (dy + 1));
      for (x[1] = 0; x[1] < dy - 1; ++x[1], ++n, m += 2)
        for (x[0] = 0; x[0] < dx - 1; ++x[0], ++n, ++m) {
          int mask = 0, g = 0, idx = n;
          for (int k = 0; k < 2; ++k, idx += dx * (dy - 2))
            for (int j = 0; j < 2; ++j, idx += dx - 2)
              for (int i = 0; i < 2; ++i, ++g, ++idx) {
                float p = data[idx];
                grid[g] = p;
                mask |= p < 0 ? 1 << g : 0;
              }
          if (mask == 0 || mask == 0xff)
            continue;
          int edgeMask = EDGE_TABLE[mask];
          float[] v = new float[3];
          int eCount = 0;
          for (int i = 0; i < 12; ++i) {
            if ((edgeMask & (1 << i)) == 0)
              continue;
            ++eCount;
            int e0 = CUBE_EDGES[i << 1], e1 = CUBE_EDGES[(i << 1) + 1];
            float g0 = grid[e0], g1 = grid[e1];
            float t = g0 - g1;
            if (Math.abs(t) > 1e-6f)
              t = g0 / t;
            else
              continue;
            for (int j = 0, kb = 1; j < 3; ++j, kb <<= 1) {
              int a = e0 & kb, bb = e1 & kb;
              if (a != bb)
                v[j] += a != 0 ? 1 - t : t;
              else
                v[j] += a != 0 ? 1 : 0;
            }
          }
          float s = 1f / eCount;
          for (int i = 0; i < 3; ++i)
            v[i] = x[i] + s * v[i];
          buffer[m] = vertices.size();
          vertices.add(v);
          for (int i = 0; i < 3; ++i) {
            if ((edgeMask & (1 << i)) == 0)
              continue;
            int iu = (i + 1) % 3, iv = (i + 2) % 3;
            if (x[iu] == 0 || x[iv] == 0)
              continue;
            int du = R[iu], dv = R[iv];
            if ((mask & 1) != 0)
              faces.add(new int[]{buffer[m], buffer[m - du], buffer[m - du - dv], buffer[m - dv]});
            else
              faces.add(new int[]{buffer[m], buffer[m - dv], buffer[m - du - dv], buffer[m - du]});
          }
        }
    }
  }

  /**
   * Smooth shading: average the faces meeting at each vertex, then NEGATE.
   *
   * <p>The negation is the whole point. Mirroring y into model space reverses orientation,
   * so a normal derived from the mirrored positions points INTO the solid. three.js hides
   * that — with {@code side: DoubleSide} its shader flips the normal on back-facing
   * fragments — but libGDX's DefaultShader does not, so the surface came out lit from
   * behind: uniformly matte, with no highlight, exactly as if it ignored the light.
   */
  private static float[] vertexNormals(float[] pos, List<int[]> quads) {
    float[] nrm = new float[pos.length];
    for (int[] q : quads)
      for (int t = 0; t < 2; t++) {
        int i0 = q[0], i1 = t == 0 ? q[1] : q[2], i2 = t == 0 ? q[2] : q[3];
        float ax = pos[i1 * 3] - pos[i0 * 3], ay = pos[i1 * 3 + 1] - pos[i0 * 3 + 1],
            az = pos[i1 * 3 + 2] - pos[i0 * 3 + 2];
        float bx = pos[i2 * 3] - pos[i0 * 3], by = pos[i2 * 3 + 1] - pos[i0 * 3 + 1],
            bz = pos[i2 * 3 + 2] - pos[i0 * 3 + 2];
        float cx = ay * bz - az * by, cy = az * bx - ax * bz, cz = ax * by - ay * bx;
        for (int i : new int[]{i0, i1, i2}) {
          nrm[i * 3] += cx;
          nrm[i * 3 + 1] += cy;
          nrm[i * 3 + 2] += cz;
        }
      }
    for (int i = 0; i < nrm.length; i += 3) {
      float len = (float) Math.sqrt(
          nrm[i] * nrm[i] + nrm[i + 1] * nrm[i + 1] + nrm[i + 2] * nrm[i + 2]);
      if (len < 1e-6f)
        nrm[i + 2] = 1;
      else { // negated: see the note above on the y mirror
        nrm[i] /= -len;
        nrm[i + 1] /= -len;
        nrm[i + 2] /= -len;
      }
    }
    return nrm;
  }

  private static void blurAxis(float[] src, float[] dst, int dx, int dy, int dz, int axis, int r) {
    for (int z = 0; z < dz; z++)
      for (int y = 0; y < dy; y++)
        for (int x = 0; x < dx; x++) {
          float acc = 0;
          int cnt = 0;
          for (int k = -r; k <= r; k++) {
            int cx = x, cy = y, cz = z;
            if (axis == 0)
              cx += k;
            else if (axis == 1)
              cy += k;
            else
              cz += k;
            if (cx < 0 || cy < 0 || cz < 0 || cx >= dx || cy >= dy || cz >= dz)
              continue;
            acc += src[cx + dx * (cy + dy * cz)];
            cnt++;
          }
          dst[x + dx * (y + dy * z)] = cnt == 0 ? 0 : acc / cnt;
        }
  }

  /** rough upper bound for the mesh-limit check before baking. */
  public static int vertexEstimate(SpriteBitmap b, Sprite3DConfig cfg) {
    int res = resolutionFor(b.w(), b.rows);
    return Math.min(MAX_VERTICES, 12 * res * res * b.litPixels() + 64);
  }
}
