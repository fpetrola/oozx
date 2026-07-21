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
  /** an object bigger than this is the room, not a thing in it (bytes wide, pixel rows). */
  private final int maxCols = Integer.getInteger("discover.objects.cols", 16);
  private final int maxRows = Integer.getInteger("discover.objects.rows", 96);
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

  /**
   * The flood that finds objects. Not over sprite-owned bytes: in a game like Exolon the
   * planets, the capsules, the cannons and the columns are CLASSIFIED AS SCENERY, so a pass
   * that only groups what the taint calls a sprite shows the little characters and misses
   * every big thing on screen — which is exactly what it did.
   *
   * <p>So it groups whatever is LIT, and cuts where the INK changes: on this hardware colour
   * is the only thing that says where one object ends, and it is what separates a green
   * cannon from the yellow floor it stands on (the same rule the relief's blobs use). The
   * terrain band spans the screen and is thrown out by the size cap; a planet, a column or a
   * capsule fits and comes out whole.
   */
  private void scan(TaintReplay.FrameSnapshot snap) {
    int h = 192;
    boolean[][] lit = new boolean[h][32];
    byte[][] px = new byte[h][32];
    int[][] ink = new int[h][32];
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      // a sprite-owned byte contributes only the sprite's OWN ink: the rest of the byte is
      // the background it was composed over, and it would grow the object into the scenery
      int v = snap.owner()[i] != 0
          ? snap.pixels()[i] & snap.spriteBits()[i] & 0xff : snap.pixels()[i] & 0xff;
      px[y][c] = (byte) v;
      lit[y][c] = v != 0;
      int attr = snap.attrs()[(y >> 3) * 32 + c] & 0xff;
      // the cut ignores BRIGHT: a planet drawn half bright and half not is one planet, and
      // splitting on it gave two half-discs. The colour drawn in the sheet keeps it.
      ink[y][c] = attr & 7;
    }
    boolean[][] seen = new boolean[h][32];
    java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
    List<int[]> cells = new ArrayList<>();
    // the status area is not an object: its text is frequent and would crowd out the game
    int top = JSW3D.iprop("render.playfield.top", "playfield.top", 0) * 8;
    int end = Math.min(h, top + JSW3D.iprop("render.playfield.rows", "playfield.rows", 24) * 8);
    for (int y0 = top; y0 < end; y0++)
      for (int c0 = 0; c0 < 32; c0++) {
        if (!lit[y0][c0] || seen[y0][c0])
          continue;
        cells.clear();
        seen[y0][c0] = true;
        queue.add(new int[]{c0, y0});
        int ink0 = ink[y0][c0], minC = c0, maxC = c0, minR = y0, maxR = y0;
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
              if (c >= 0 && c < 32 && y >= top && y < end && lit[y][c] && !seen[y][c]
                  && ink[y][c] == ink0) {
                seen[y][c] = true;
                queue.add(new int[]{c, y});
              }
            }
        }
        if (cells.size() < minBytes)
          continue;
        // the size cap is what tells an OBJECT from the room: a floor band or a wall runs
        // the width of the screen, a planet does not
        if (maxC - minC + 1 > maxCols || maxR - minR + 1 > maxRows)
          continue;
        add(snap, px, cells, minC, minR, maxC, maxR);
      }
  }

  private void add(TaintReplay.FrameSnapshot snap, byte[][] px, List<int[]> cells,
      int minC, int minR, int maxC, int maxR) {
    int w = maxC - minC + 1, rows = maxR - minR + 1;
    byte[] bits = new byte[w * rows];
    Map<Integer, Integer> pieces = new LinkedHashMap<>();
    int cc = w, cr = (rows + 7) / 8 + 1;
    byte[] ink = new byte[cc * cr];
    for (int[] q : cells) {
      int i = ((q[1] & 0xC0) << 5) | ((q[1] & 7) << 8) | ((q[1] & 0x38) << 2) | q[0];
      bits[(q[1] - minR) * w + (q[0] - minC)] = px[q[1]][q[0]];
      // where the pixels CAME FROM, whichever way the taint classified them: a sprite base
      // and a tile leaf are both "a graphic in memory that fed this object"
      int origin = snap.owner()[i] != 0 ? snap.owner()[i] - 1
          : snap.tile()[i] != 0 ? snap.tile()[i] - 1 : -1;
      if (origin >= 0)
        pieces.merge(origin, 1, Integer::sum);
      int attr = snap.attrs()[(q[1] >> 3) * 32 + q[0]] & 0xff;
      ink[((q[1] - minR) >> 3) * cc + (q[0] - minC)] = (byte) ((attr & 7) | ((attr >> 3) & 8));
    }
    long hash = (w * 31L + rows) * 1099511628211L;
    for (byte b : bits)
      hash = (hash ^ (b & 0xff)) * 1099511628211L;
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
