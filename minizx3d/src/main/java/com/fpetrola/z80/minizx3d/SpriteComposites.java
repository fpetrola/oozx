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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The OBJECTS the game puts on screen, not the pieces they are made of.
 *
 * <p>A catalogue entry is a range of memory that got read together, which for an engine that
 * composes — Exolon draws one character out of several shared pieces — is a fragment: half a
 * torso, a stripe of a leg. Listing those answers "what bytes are graphics" and not "what
 * does this game show", and it is the second question a person actually wants to look at.
 *
 * <p>So this replays the game with the catalogue already built and groups what lands on
 * screen exactly as {@link JSW3D#updateSprites} does in adjacent mode: sprite-owned bytes,
 * flooded by adjacency, become one object; its bitmap is what the game COMPOSED there
 * ({@code pixels & spriteBits}), and its colour is the cell attributes it was painted with.
 * The pieces that fed each object are kept alongside, so the report can say which catalogue
 * entries make it up — the composition, spelled out, instead of implied.
 *
 * <p>Identical objects across frames collapse by content hash, so an animation cycle settles
 * into a handful of entries and what survives is ranked by how often the game drew it.
 */
public final class SpriteComposites {
  /** one distinct composed object: what it looks like, what it is made of, how often. */
  public static final class Composite {
    public int wBytes, rows, count, firstFrame, lastFrame;
    /** the composed pixels, {@code rows * wBytes}, MSB leftmost — the screen's own layout. */
    public byte[] bits;
    /** ink colour per CELL of the bounding box (Spectrum palette index), row-major. */
    public byte[] ink;
    public int cellCols, cellRows;
    /** catalogue base -> how many bytes of this object came from it. */
    public final Map<Integer, Integer> pieces = new LinkedHashMap<>();
  }

  private final Map<Long, Composite> byHash = new HashMap<>();
  private final int minBytes;
  /** the replay this pass ran, kept so a caller with no emulator of its own can read memory */
  private TaintReplay replay;

  public SpriteComposites(int minBytes) {
    this.minBytes = minBytes;
  }

  /**
   * Replays {@code rzx} with {@code catalog} and collects the composed objects. Sampled, and
   * bounded by {@code maxFrames}: the point is to SEE the objects, and a game shows each of
   * them many times over — a few thousand frames is plenty, and a second full-length pass
   * over the RZX would cost as much as the discovery itself.
   */
  public void collect(String rzx, SpriteCatalog catalog, int fromFrame, int maxFrames,
      int sample) throws Exception {
    TaintReplay r = new TaintReplay(rzx, catalog, snap -> {
      if (snap.frame() < fromFrame || (snap.frame() - fromFrame) % sample != 0)
        return;
      scan(snap);
    });
    replay = r;
    r.paced = false;
    r.maxFrames = maxFrames;
    try {
      r.run();
    } catch (RuntimeException e) {
      // "rzx finished" and friends: whatever was collected up to here is still the answer
      System.out.println("compuestos: replay terminado (" + e.getMessage() + ")");
    }
  }

  /** the adjacency flood of {@link JSW3D#updateSprites}, on the offline side. */
  private void scan(TaintReplay.FrameSnapshot snap) {
    int h = 192;
    int[][] grid = new int[h][32];
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7);
      grid[y][i & 31] = snap.owner()[i];
    }
    java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
    List<int[]> cells = new ArrayList<>();
    for (int y0 = 0; y0 < h; y0++)
      for (int c0 = 0; c0 < 32; c0++) {
        if (grid[y0][c0] == 0)
          continue;
        cells.clear();
        queue.add(new int[]{c0, y0});
        grid[y0][c0] = 0;
        int minC = c0, maxC = c0, minR = y0, maxR = y0;
        while (!queue.isEmpty()) {
          int[] p = queue.poll();
          cells.add(p);
          minC = Math.min(minC, p[0]);
          maxC = Math.max(maxC, p[0]);
          minR = Math.min(minR, p[1]);
          maxR = Math.max(maxR, p[1]);
          for (int dy = -1; dy <= 1; dy++)
            for (int dc = -1; dc <= 1; dc++) {
              int c = p[0] + dc, y = p[1] + dy;
              if (c >= 0 && c < 32 && y >= 0 && y < h && grid[y][c] != 0) {
                grid[y][c] = 0;
                queue.add(new int[]{c, y});
              }
            }
        }
        if (cells.size() < minBytes)
          continue;
        add(snap, cells, minC, minR, maxC, maxR);
      }
  }

  private void add(TaintReplay.FrameSnapshot snap, List<int[]> cells,
      int minC, int minR, int maxC, int maxR) {
    int w = maxC - minC + 1, rows = maxR - minR + 1;
    if (w * rows > 4096)
      return; // a screen-wide blob is the catalogue over-claiming, not an object
    byte[] bits = new byte[w * rows];
    Map<Integer, Integer> pieces = new LinkedHashMap<>();
    int cellCols = (maxC >> 0) - minC + 1, cc = w, cr = (rows + 7) / 8;
    byte[] ink = new byte[cc * cr];
    for (int[] q : cells) {
      int i = ((q[1] & 0xC0) << 5) | ((q[1] & 7) << 8) | ((q[1] & 0x38) << 2) | q[0];
      // the object's OWN ink: under a masked engine the rest of the byte is the background
      // it was composed over, and including it grows the object into the scenery
      bits[(q[1] - minR) * w + (q[0] - minC)] = (byte) (snap.pixels()[i] & snap.spriteBits()[i]);
      int base = snap.owner()[i] - 1;
      if (base >= 0)
        pieces.merge(base, 1, Integer::sum);
      int attr = snap.attrs()[(q[1] >> 3) * 32 + q[0]] & 0xff;
      ink[((q[1] - minR) >> 3) * cc + (q[0] - minC)] = (byte) ((attr & 7) | ((attr >> 3) & 8));
    }
    long hash = (w * 31L + rows) * 1099511628211L;
    for (byte b : bits)
      hash = (hash ^ (b & 0xff)) * 1099511628211L;
    boolean lit = false;
    for (byte b : bits)
      lit |= b != 0;
    if (!lit)
      return;
    Composite c = byHash.computeIfAbsent(hash, k -> new Composite());
    if (c.bits == null) {
      c.bits = bits;
      c.ink = ink;
      c.wBytes = w;
      c.rows = rows;
      c.cellCols = cc;
      c.cellRows = cr;
      c.firstFrame = snap.frame();
    }
    c.count++;
    c.lastFrame = snap.frame();
    pieces.forEach((base, n) -> c.pieces.merge(base, n, Integer::sum));
  }

  /** the game's memory as this pass left it, for whoever needs to draw the pieces. */
  public java.util.function.IntUnaryOperator memByte() {
    return replay == null ? a -> 0 : replay::memByte;
  }

  /** the distinct objects, most drawn first, capped so the report stays readable. */
  public List<Composite> top(int max) {
    List<Composite> all = new ArrayList<>(byHash.values());
    all.sort((a, b) -> b.count - a.count);
    return all.size() > max ? all.subList(0, max) : all;
  }
}
