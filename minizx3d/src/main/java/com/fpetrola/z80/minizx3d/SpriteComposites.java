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
    /** paper colour per cell, so the object is drawn exactly as the screen showed it. */
    public byte[] paper;
    public int cellCols, cellRows;
    /** catalogue base -> how many bytes of this object came from it. */
    public final Map<Integer, Integer> pieces = new LinkedHashMap<>();
  }

  private final Map<Long, Composite> byHash = new HashMap<>();
  private final int minBytes;
  /** an object bigger than this is the room, not a thing in it (bytes wide, pixel rows). */
  private final int maxCols = Integer.getInteger("discover.objects.cols", 16);
  private final int maxRows = Integer.getInteger("discover.objects.rows", 96);
  /**
   * How many screen writes may pass between two bytes of the same drawing. Not 1: a routine
   * skips the bytes a mask leaves untouched, and interleaves the odd write of its own
   * bookkeeping. Wide enough to hold a sprite together, narrow enough that the next object
   * starts its own burst.
   */
  private final int gap = Integer.getInteger("discover.objects.gap", 48);
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
      scan(snap, replay);
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
   * The objects, taken from WHEN THE GAME DREW THEM. Every write to the display file gets a
   * sequence number ({@link TaintReplay#writeOrder}), so the bytes the game painted one after
   * another are consecutive there: a composed object is a BURST in that order — all of its
   * pieces, whatever their colours or how the taint classified them — and the gaps between
   * bursts are where one drawing ends and the next begins.
   *
   * <p>Everything tried before this cut the picture with a rule of our own (adjacency, then
   * adjacency plus colour) and so kept splitting objects the game considers one: a ship with
   * cyan windows on a white hull came out as two, and anything standing on the floor merged
   * with it. Grouping by the drawing itself needs no rule about shape at all.
   *
   * <p>What is captured is the RECTANGLE as it stands on screen, ink and paper included: the
   * point is to check against the game, and for that it has to be what the game shows.
   */
  private void scan(TaintReplay.FrameSnapshot snap, TaintReplay replay) {
    int h = 192, frame = snap.frame();
    // the bytes painted THIS frame, in the order they were painted
    List<int[]> writes = new ArrayList<>(); // {order, index}
    // the snapshot is published when the frame index CHANGES, so what the screen holds was
    // painted during the frame before the one it is labelled with: accept both
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++)
      if (frame - replay.lastWrite[i] <= 1)
        writes.add(new int[]{replay.writeOrder[i], i});
    if (writes.isEmpty())
      return;
    writes.sort((a, b) -> Integer.compare(a[0], b[0]));
    int top = JSW3D.iprop("render.playfield.top", "playfield.top", 0) * 8;
    int end = Math.min(h, top + JSW3D.iprop("render.playfield.rows", "playfield.rows", 24) * 8);
    List<Integer> burst = new ArrayList<>();
    int prev = -1;
    for (int[] w : writes) {
      if (prev >= 0 && w[0] - prev > gap) {
        flush(snap, burst, top, end);
        burst.clear();
      }
      burst.add(w[1]);
      prev = w[0];
    }
    flush(snap, burst, top, end);
  }

  /** one drawing: its bounding box on screen, captured as the screen shows it. */
  private void flush(TaintReplay.FrameSnapshot snap, List<Integer> burst, int top, int end) {
    if (burst.size() < minBytes)
      return;
    int minC = 31, maxC = 0, minR = 191, maxR = 0;
    for (int i : burst) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      if (y < top || y >= end)
        return; // a drawing that touches the status area is not an object of the room
      minC = Math.min(minC, c);
      maxC = Math.max(maxC, c);
      minR = Math.min(minR, y);
      maxR = Math.max(maxR, y);
    }
    int w = maxC - minC + 1, rows = maxR - minR + 1;
    if (w > maxCols || rows > maxRows)
      return; // a screen-wide burst is the room being repainted, not a thing in it
    byte[] bits = new byte[w * rows];
    int cc = w, cr = (rows + 7) / 8 + 1;
    byte[] ink = new byte[cc * cr], paper = new byte[cc * cr];
    boolean lit = false;
    for (int y = minR; y <= maxR; y++)
      for (int c = minC; c <= maxC; c++) {
        int i = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2) | c;
        byte v = snap.pixels()[i];
        bits[(y - minR) * w + (c - minC)] = v;
        lit |= v != 0;
        int attr = snap.attrs()[(y >> 3) * 32 + c] & 0xff;
        int at = ((y - minR) >> 3) * cc + (c - minC);
        ink[at] = (byte) ((attr & 7) | ((attr >> 3) & 8));
        paper[at] = (byte) (((attr >> 3) & 7) | ((attr >> 3) & 8));
      }
    if (!lit)
      return;
    Map<Integer, Integer> pieces = new LinkedHashMap<>();
    for (int i : burst) {
      // where the pixels CAME FROM, whichever way the taint classified them: a sprite base
      // and a tile leaf are both "a graphic in memory that fed this drawing"
      int origin = snap.owner()[i] != 0 ? snap.owner()[i] - 1
          : snap.tile()[i] != 0 ? snap.tile()[i] - 1 : -1;
      if (origin >= 0)
        pieces.merge(origin, 1, Integer::sum);
    }
    long hash = (w * 31L + rows) * 1099511628211L;
    for (byte b : bits)
      hash = (hash ^ (b & 0xff)) * 1099511628211L;
    for (byte b : ink)
      hash = (hash ^ (b & 0xff)) * 1099511628211L;
    Composite c = byHash.computeIfAbsent(hash, k -> new Composite());
    if (c.bits == null) {
      c.bits = bits;
      c.ink = ink;
      c.paper = paper;
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
