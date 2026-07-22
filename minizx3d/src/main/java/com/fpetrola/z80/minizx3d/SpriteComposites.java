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
    /** lit pixels of the drawing kept: the fullest sighting wins the picture. */
    public int litPixels;
    public int cellCols, cellRows;
    /** catalogue base -> how many bytes of this object came from it. */
    public final Map<Integer, Integer> pieces = new LinkedHashMap<>();
  }

  private final Map<Long, Composite> byHash = new HashMap<>();
  private final int minBytes;
  /** an object bigger than this is the room, not a thing in it (bytes wide, pixel rows). */
  private final int maxCols = Integer.getInteger("discover.objects.cols", 8);
  private final int maxRows = Integer.getInteger("discover.objects.rows", 48);
  /**
   * How many screen writes may pass between two bytes of the same drawing. Not 1: a routine
   * skips the bytes a mask leaves untouched, and interleaves the odd write of its own
   * bookkeeping. Wide enough to hold a sprite together, narrow enough that the next object
   * starts its own burst.
   */
  /**
   * How far up from the invocation that wrote the pixel the object's own call sits. One
   * level, measured: the leaf is the routine that paints a piece, its caller is the one that
   * decided to draw the thing.
   */
  private final int callUp = Integer.getInteger("discover.objects.up", 1);
  /** how many frames back a drawing at the same place still counts as the same object. */
  private final int window = Integer.getInteger("discover.objects.window", 24);
  /** boxes drawn recently, to grow a sliced repaint back into the whole thing. */
  private final List<int[]> recent = new ArrayList<>();
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
   * The objects, taken from WHO DREW THEM: the call tree.
   *
   * <p>A game that draws a composite object does it from one routine, or from several called
   * together by one that decided to draw the thing, and every byte of it hangs off that node
   * ({@link TaintReplay#writeNode}). Everything tried before this measured the SCREEN —
   * adjacency, colour, write order, a time window — and each one merged or split the wrong
   * pair, because the screen does not say where an object ends. The call node does not need
   * to be told: it never joins two things that were drawn by different calls, which is the
   * case that started all of it (a capsule and the player standing in front of it).
   *
   * <p>Measured on Exolon before switching to it: climbing ONE level from the invocation that
   * wrote the pixel collapses an object into a single node in 3 of 4 objects marked by hand,
   * and for two of them the node's box is the object's own size with a single blob in it.
   * Where the node still holds more than one blob, it is the same object drawn several times
   * or repainted in pieces — both of which the split below and the cross-frame merge already
   * handle.
   */
  private void scan(TaintReplay.FrameSnapshot snap, TaintReplay r) {
    int frame = snap.frame(), h = 192;
    int top = JSW3D.iprop("render.playfield.top", "playfield.top", 0) * 8;
    int end = Math.min(h, top + JSW3D.iprop("render.playfield.rows", "playfield.rows", 24) * 8);
    // the snapshot is published when the frame index CHANGES, so what the screen holds was
    // painted during the previous frame — and that is the frame whose call tree is still live
    Map<Integer, List<Integer>> byNode = new HashMap<>();
    for (int i = 0; i < TaintReplay.PIXEL_BYTES; i++) {
      if ((snap.pixels()[i] & 0xff) == 0 || r.lastWrite[i] != frame - 1)
        continue;
      int n = r.writeNode[i] >= 0 && r.writeNode[i] < r.nodeParent.size ? r.writeNode[i] : 0;
      for (int a = n; ; a = r.nodeParent.get(a)) { // the byte belongs to every ancestor too
        byNode.computeIfAbsent(a, k -> new ArrayList<>()).add(i);
        if (a == 0)
          break;
      }
    }
    // THE OBJECT IS THE HIGHEST CALL THAT STILL LOOKS LIKE ONE. A fixed level cannot work:
    // one level up from the pixel is the object for a missile and the whole room for the
    // routine that repaints the screen — measured, that gave objects of 128x96 px, which is
    // the cap, which is the room. So each node is taken only if it fits an object's size and
    // its parent does not: the deepest place where the drawing is still a thing, not a scene.
    java.util.Set<Integer> emitted = new java.util.HashSet<>();
    List<Integer> nodes = new ArrayList<>(byNode.keySet());
    nodes.sort((a, b) -> byNode.get(b).size() - byNode.get(a).size());
    for (int n : nodes) {
      List<Integer> painted = byNode.get(n);
      if (painted.size() < minBytes || !fits(bbox(painted), painted.size()))
        continue;
      int parent = n == 0 ? -1 : r.nodeParent.get(n);
      if (parent >= 0 && byNode.containsKey(parent)
          && fits(bbox(byNode.get(parent)), byNode.get(parent).size()))
        continue; // its caller still looks like one object: this is a piece of it, not it
      List<Integer> mine = new ArrayList<>();
      for (int i : painted)
        if (emitted.add(i))
          mine.add(i);
      if (mine.size() < minBytes)
        continue;
      // one call can draw the object several times over (Exolon lines up three missiles and
      // paints them in one go), so what the node gives is split into connected pieces
      for (List<Integer> blob : blobs(mine, top, end))
        flush(snap, blob, grow(bbox(blob), frame), top, end);
    }
    recent.removeIf(x -> frame - x[4] > window);
  }

  /**
   * Is this the size of a thing in the room, rather than the room? Half the screen "fits"
   * any cap generous enough for a big object, so density decides the rest: a drawing that
   * leaves a tenth of its own box painted is a scene, not a thing.
   */
  private boolean fits(int[] box, int painted) {
    int w = box[2] - box[0] + 1, h = box[3] - box[1] + 1;
    return w <= maxCols && h <= maxRows && painted >= .2f * w * h;
  }

  /**
   * The ancestor {@code up} levels above a node. The tree is rebuilt every frame, so an id
   * from an older frame means nothing now and counts as the root.
   */
  private static int ancestor(TaintReplay r, int node, int up) {
    int n = node >= 0 && node < r.nodeParent.size ? node : 0;
    for (int i = 0; i < up && n > 0 && r.nodeParent.get(n) > 0; i++)
      n = r.nodeParent.get(n);
    return n;
  }

  /** connected pieces of what one call painted, with a couple of rows of slack for masks. */
  private List<List<Integer>> blobs(List<Integer> painted, int top, int end) {
    java.util.Set<Integer> left = new java.util.HashSet<>(painted);
    List<List<Integer>> out = new ArrayList<>();
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    while (!left.isEmpty()) {
      List<Integer> blob = new ArrayList<>();
      Integer start = left.iterator().next();
      left.remove(start);
      queue.add(start);
      while (!queue.isEmpty()) {
        int i = queue.poll();
        blob.add(i);
        int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
        for (int dy = -2; dy <= 2; dy++)
          for (int dc = -1; dc <= 1; dc++) {
            int yy = y + dy, cc = c + dc;
            if (yy < top || yy >= end || cc < 0 || cc > 31)
              continue;
            int q = ((yy & 0xC0) << 5) | ((yy & 7) << 8) | ((yy & 0x38) << 2) | cc;
            if (left.remove(q))
              queue.add(q);
          }
      }
      if (blob.size() >= minBytes)
        out.add(blob);
    }
    return out;
  }

  /**
   * Grow a drawing's box with the ones that landed on the SAME PLACE in the last frames.
   *
   * <p>A dirty-region engine repaints a big static thing in slices — half a planet this
   * frame, the other half the next — so within one frame there is never a burst that covers
   * it and it kept coming out as a half-dome. The box is what gets captured, and what gets
   * captured is read from the SCREEN, which holds the whole planet the whole time: widening
   * the box with what was drawn there recently is enough to see the object complete.
   */
  private int[] grow(int[] box, int frame) {
    for (int[] r : recent) {
      // real OVERLAP, not proximity, and only while the result still looks like an object:
      // with slack and no cap the boxes chained along the terrain band until the whole
      // screen was one "object" and the run ended with seven of them
      if (box[0] > r[2] || r[0] > box[2] || box[1] > r[3] || r[1] > box[3])
        continue;
      int c0 = Math.min(box[0], r[0]), r0 = Math.min(box[1], r[1]);
      int c1 = Math.max(box[2], r[2]), r1 = Math.max(box[3], r[3]);
      if (c1 - c0 + 1 > maxCols || r1 - r0 + 1 > maxRows)
        continue;
      box[0] = c0;
      box[1] = r0;
      box[2] = c1;
      box[3] = r1;
    }
    recent.removeIf(r -> box[0] <= r[0] && box[1] <= r[1] && box[2] >= r[2] && box[3] >= r[3]);
    recent.add(new int[]{box[0], box[1], box[2], box[3], frame});
    return box;
  }

  private static int[] bbox(List<Integer> burst) {
    int minC = 31, maxC = 0, minR = 191, maxR = 0;
    for (int i : burst) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      minC = Math.min(minC, c);
      maxC = Math.max(maxC, c);
      minR = Math.min(minR, y);
      maxR = Math.max(maxR, y);
    }
    return new int[]{minC, minR, maxC, maxR};
  }

  /** one drawing: its bounding box on screen, captured as the screen shows it. */
  private void flush(TaintReplay.FrameSnapshot snap, List<Integer> burst, int[] box,
      int top, int end) {
    if (burst.size() < minBytes)
      return;
    // clipped to the playfield, not dropped: a drawing that spills a byte into the status
    // area is still an object of the room, and dropping those lost real ones
    int minC = box[0], maxC = box[2], minR = Math.max(top, box[1]), maxR = Math.min(end - 1, box[3]);
    if (maxR < minR)
      return;
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
    // IDENTITY = which graphics compose it, at what size. Hashing the pixels made every
    // sub-byte X offset of the same object a different entry, so the top of the ranking
    // filled with twenty copies of one teapot while the ship, drawn a handful of times,
    // fell off the end. Same pieces at the same size is the same object.
    long hash = (w * 31L + rows) * 1099511628211L;
    for (int base : new java.util.TreeSet<>(pieces.keySet()))
      hash = (hash ^ base) * 1099511628211L;
    Composite c = byHash.computeIfAbsent(hash, k -> new Composite());
    int litPx = 0;
    for (byte b : bits)
      litPx += Integer.bitCount(b & 0xff);
    // keep the FULLEST drawing seen as the picture: a frame that repainted the object whole
    // shows it whole, one that repainted a slice does not
    if (c.bits == null || litPx > c.litPixels) {
      c.litPixels = litPx;
      c.bits = bits;
      c.ink = ink;
      c.paper = paper;
      c.wBytes = w;
      c.rows = rows;
      c.cellCols = cc;
      c.cellRows = cr;
      if (c.firstFrame == 0)
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
