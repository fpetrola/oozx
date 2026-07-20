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

import java.util.function.IntUnaryOperator;

/**
 * A sprite handed to the 3D pipeline, with nothing Spectrum-specific left in it: a 1-bit
 * silhouette plus, optionally, a resolved color per pixel. Everything downstream (analysis,
 * technique selection, mesh baking) works on this and never touches Z80 memory, attributes
 * or the taint — which is also what lets the heavy baking run off the emulation thread.
 *
 * <p>Rows are kept in the ORIGINAL packed byte layout rather than expanded to a pixel array,
 * for one reason: {@link VoxelSpriteBuilder}, {@link SmoothSpriteBuilder} and
 * {@link TileSlabBuilder} already read their bitmaps through an {@code IntUnaryOperator} of
 * byte addresses, so {@link #memView()} lets them be reused verbatim, with no behavioural
 * change to the games that already work.
 *
 * <p>Two origins, both already produced by the viewer:
 * <ul>
 *   <li>{@link #ofMemory} — the graphic's own bitmap in game memory, the clean shape;</li>
 *   <li>{@link #ofScreen} — the pixels as composited on screen, which is the only thing
 *       that exists for a multi-piece or masked object (doc §5, atajo de render).</li>
 * </ul>
 */
public final class SpriteBitmap {
  /** bytes per row; the sprite is {@code wBytes * 8} pixels wide. */
  public final int wBytes;
  public final int rows;
  /** {@code rows * wBytes} packed bits, MSB leftmost — the Spectrum's own layout. */
  public final byte[] data;
  /**
   * Resolved color per pixel ({@code w*h} ARGB), or null when the sprite is a plain
   * silhouette that the renderer tints with its attribute. Kept EXACT, attribute clash
   * included: smoothing colors is a decision for the 3D stage, not for extraction.
   */
  public final int[] argb;
  /** identity of this exact bitmap — the mesh cache key. Animation frames differ. */
  public final long hash;
  /**
   * Catalog base address of the graphic, or -1. This — NOT {@link #hash} — is the key for
   * hand overrides: every animation frame of one graphic has its own hash, so hashing would
   * make you configure a character once per frame and again for every frame you had not
   * seen yet. The base is the same across the whole animation.
   */
  public final int base;

  private SpriteBitmap(int wBytes, int rows, byte[] data, int[] argb, int base) {
    this.wBytes = wBytes;
    this.rows = rows;
    this.data = data;
    this.argb = argb;
    this.base = base;
    long h = (wBytes * 31L + rows) * 1099511628211L;
    for (byte b : data)
      h = (h ^ (b & 0xff)) * 1099511628211L; // FNV-1a, same scheme the viewer already used
    if (argb != null)
      for (int c : argb)
        h = (h ^ c) * 1099511628211L;
    this.hash = h;
  }

  /** the graphic as it lives in game memory: {@code bytes} bytes from {@code base}. */
  public static SpriteBitmap ofMemory(int base, int bytes, int wBytes, IntUnaryOperator memByte) {
    int rows = Math.max(1, bytes / Math.max(1, wBytes));
    byte[] d = new byte[rows * wBytes];
    for (int i = 0; i < d.length; i++)
      d[i] = (byte) memByte.applyAsInt(base + i);
    return new SpriteBitmap(wBytes, rows, d, null, base);
  }

  /** the pixels as already composited on screen, row-major, {@code wBytes} per row. */
  public static SpriteBitmap ofScreen(byte[] rowBytes, int wBytes, int base) {
    int rows = Math.max(1, rowBytes.length / Math.max(1, wBytes));
    return new SpriteBitmap(wBytes, rows, rowBytes.clone(), null, base);
  }

  /** same, carrying a resolved color per pixel for {@link ColorMode#TEXTURE}. */
  public static SpriteBitmap ofScreen(byte[] rowBytes, int wBytes, int base, int[] argb) {
    int rows = Math.max(1, rowBytes.length / Math.max(1, wBytes));
    return new SpriteBitmap(wBytes, rows, rowBytes.clone(), argb, base);
  }

  public int w() {
    return wBytes * 8;
  }

  public int h() {
    return rows;
  }

  public boolean opaque(int x, int y) {
    if (x < 0 || y < 0 || x >= w() || y >= rows)
      return false;
    return (data[y * wBytes + (x >> 3)] & (0x80 >> (x & 7))) != 0;
  }

  /** the silhouette as {@code [row][col]}, the shape every builder here works on. */
  public boolean[][] mask() {
    boolean[][] m = new boolean[rows][w()];
    for (int y = 0; y < rows; y++)
      for (int x = 0; x < w(); x++)
        m[y][x] = opaque(x, y);
    return m;
  }

  public int litPixels() {
    int n = 0;
    for (byte b : data)
      n += Integer.bitCount(b & 0xff);
    return n;
  }

  /** byte-address view for the existing builders: address 0 is this bitmap's first row. */
  public IntUnaryOperator memView() {
    return a -> a >= 0 && a < data.length ? data[a] & 0xff : 0;
  }

  /** this bitmap with its silhouette replaced — for the EPX / upscaling pre-passes. */
  public SpriteBitmap withMask(boolean[][] m) {
    int nw = (m[0].length + 7) / 8;
    byte[] d = new byte[m.length * nw];
    for (int y = 0; y < m.length; y++)
      for (int x = 0; x < m[0].length; x++)
        if (m[y][x])
          d[y * nw + (x >> 3)] |= (byte) (0x80 >> (x & 7));
    return new SpriteBitmap(nw, m.length, d, null, base);
  }
}
