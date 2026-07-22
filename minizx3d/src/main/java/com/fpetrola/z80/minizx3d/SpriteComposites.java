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
    /** the frame whose sighting gave the picture, so the dump can be pointed at it. */
    public int pickFrame;
    /** the composed pixels, {@code rows * wBytes}, MSB leftmost — the screen's own layout. */
    public byte[] bits;
    /** ink colour per CELL of the bounding box (Spectrum palette index), row-major. */
    public byte[] ink;
    /** paper colour per cell, so the object is drawn exactly as the screen showed it. */
    public byte[] paper;
    /** lit pixels of the drawing kept: the fullest sighting wins the picture. */
    public int litPixels;
    /** ARGB per PIXEL and the graphic address behind each one, for the encoded sheet. */
    public int[] px, owner;
    public int cellCols, cellRows;
    /** catalogue base -> how many bytes of this object came from it. */
    public final Map<Integer, Integer> pieces = new LinkedHashMap<>();
  }

  /** the Spectrum palette as RGB, for the pixels the encoded sheet stores. */
  private static final int[] PALETTE_RGB = {
      0x000000, 0x0000d7, 0xd70000, 0xd700d7, 0x00d700, 0x00d7d7, 0xd7d700, 0xd7d7d7,
      0x000000, 0x0000ff, 0xff0000, 0xff00ff, 0x00ff00, 0x00ffff, 0xffff00, 0xffffff};

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
  /** how much two boxes may overlap and still be slices of one drawing, not one thing moving */
  private final float slice = Float.parseFloat(System.getProperty("discover.objects.slice",
      "0.35"));
  /** how many frames back a drawing at the same place still counts as the same object. */
  private final int window = Integer.getInteger("discover.objects.window", 24);
  /** drawings of the last frames, to grow a sliced repaint back into the whole thing. */
  private final List<Recent> recent = new ArrayList<>();

  /** one drawing that already happened: its box, ITS OWN bytes, and when. */
  private static final class Recent {
    int[] box;
    java.util.Set<Integer> mask;
    boolean sprite;
    int frame;
  }
  /** -Dobjects.dump=<frame>: the call tree of that frame, node by node, as it was grouped. */
  private final int dumpFrame = Integer.getInteger("objects.dump", -1);

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
    java.util.Set<Integer> loops = loopParents(r, byNode);
    List<Integer> nodes = new ArrayList<>(byNode.keySet());
    nodes.sort((a, b) -> byNode.get(b).size() - byNode.get(a).size());
    for (int n : nodes) {
      List<Integer> painted = byNode.get(n);
      if (painted.size() < minBytes || !fits(bbox(painted), painted.size()))
        continue;
      int parent = n == 0 ? -1 : r.nodeParent.get(n);
      if (parent >= 0 && byNode.containsKey(parent) && !loops.contains(parent)
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
      List<List<Integer>> parts = blobs(snap, mine, top, end);
      if (snap.frame() == dumpFrame) {
        int[] bb = bbox(mine);
        System.out.println(String.format(
            "  nodo %d addr=$%x prof=%d padre=%d%s bytes=%d caja c%d r%d %dx%d -> %d dibujos",
            n, r.nodeAddr.get(n), depth(r, n), parent,
            loops.contains(parent) ? " (padre repite llamada)" : "", mine.size(), bb[0], bb[1],
            bb[2] - bb[0] + 1, bb[3] - bb[1] + 1, parts.size()));
        for (List<Integer> b : parts) {
          int[] pb = bbox(b);
          java.util.Set<Integer> org = new java.util.TreeSet<>();
          for (int i : b)
            org.add(snap.owner()[i] != 0 ? snap.owner()[i] - 1
                : snap.tile()[i] != 0 ? snap.tile()[i] - 1 : -1);
          StringBuilder sb = new StringBuilder();
          int k = 0;
          for (int a : org)
            if (k++ < 6)
              sb.append('$').append(Integer.toHexString(a)).append(' ');
          System.out.println(String.format("    dibujo c%d r%d %dx%d bytes=%d origenes=%d %s",
              pb[0], pb[1], pb[2] - pb[0] + 1, pb[3] - pb[1] + 1, b.size(), org.size(), sb));
        }
      }
      for (List<Integer> blob : parts) {
        java.util.Set<Integer> mask = new java.util.HashSet<>(blob);
        boolean sprite = isSprite(snap, blob.get(0));
        flush(snap, blob, grow(snap, bbox(blob), mask, sprite, frame, top, end), mask, top,
            end);
      }
    }
    recent.removeIf(x -> frame - x.frame > window);
  }

  /** how deep this node sits under the root, for the dump. */
  private static int depth(TaintReplay r, int n) {
    int d = 0;
    for (int a = n; a > 0; a = r.nodeParent.get(a))
      d++;
    return d;
  }

  /**
   * The calls that draw SEVERAL THINGS, one per call, rather than one thing out of pieces.
   *
   * <p>It is the difference between {@code for each sprite: drawSprite} and
   * {@code drawPlayer: drawHead; drawTorso; drawLegs}, and it is written in the tree: the loop
   * calls the SAME routine again and again, the composite calls different ones. So a node that
   * repeats a call is not an object — each of its invocations is — and one that does not is
   * the object its pieces belong to.
   *
   * <p>Without this the climb kept going up to the routine that draws the whole sprite list,
   * and the astronaut came out glued to whatever he was standing next to, which is what the
   * sheet showed: ten entries of the same character with a different companion each time.
   */
  private static java.util.Set<Integer> loopParents(TaintReplay r,
      Map<Integer, List<Integer>> byNode) {
    Map<Integer, java.util.Set<Integer>> callsOf = new HashMap<>();
    java.util.Set<Integer> out = new java.util.HashSet<>();
    for (int n : byNode.keySet()) {
      if (n <= 0)
        continue;
      int p = r.nodeParent.get(n);
      if (!callsOf.computeIfAbsent(p, k -> new java.util.HashSet<>()).add(r.nodeAddr.get(n)))
        out.add(p); // the same routine, twice, from the same caller: a loop over instances
    }
    return out;
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

  /** whether this byte came from a SPRITE graphic rather than from the background. */
  private static boolean isSprite(TaintReplay.FrameSnapshot snap, int i) {
    return snap.owner()[i] != 0;
  }

  /** how many PIXELS of gap still count as the same drawing (a mask leaves holes). */
  private final int gap = Integer.getInteger("discover.objects.gap", 2);

  /**
   * The separate drawings inside what one call painted — flooded PIXEL by pixel.
   *
   * <p>By bytes it could not work, and that is what filled the sheet with pairs of objects:
   * a byte touches its neighbouring column whether or not there are pixels near the seam, so
   * two things standing EIGHT PIXELS apart —the astronaut beside a capsule, which is most of
   * Exolon— came out as one drawing. Flooding the lit pixels with a couple of pixels of slack
   * asks the question that was meant all along: is this one shape, or two next to each other.
   *
   * <p>And the background never joins the sprite standing on it: in a dirty-region engine the
   * call that draws a sprite first repaints the slice of scenery it dirtied, so one node
   * legitimately holds both. The taint tells them apart —a sprite graphic is not a tile— and
   * they are never the same drawing.
   */
  private List<List<Integer>> blobs(TaintReplay.FrameSnapshot snap, List<Integer> painted,
      int top, int end) {
    Map<Integer, Integer> pixByte = new HashMap<>(); // y * 256 + x -> the byte it belongs to
    for (int i : painted) {
      int y = (((i >> 11) & 3) << 6) | (((i >> 5) & 7) << 3) | ((i >> 8) & 7), c = i & 31;
      if (y < top || y >= end)
        continue;
      int v = snap.pixels()[i] & 0xff;
      for (int b = 0; b < 8; b++)
        if ((v & (0x80 >> b)) != 0)
          pixByte.put(y * 256 + c * 8 + b, i);
    }
    java.util.Set<Integer> left = new java.util.HashSet<>(pixByte.keySet());
    List<List<Integer>> out = new ArrayList<>();
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    while (!left.isEmpty()) {
      int start = left.iterator().next();
      left.remove(start);
      queue.add(start);
      boolean sprite = isSprite(snap, pixByte.get(start));
      java.util.Set<Integer> bytes = new java.util.LinkedHashSet<>();
      while (!queue.isEmpty()) {
        int p = queue.poll();
        bytes.add(pixByte.get(p));
        int x = p & 255, y = p >> 8;
        for (int dy = -gap; dy <= gap; dy++)
          for (int dx = -gap; dx <= gap; dx++) {
            int xx = x + dx, yy = y + dy;
            if (xx < 0 || xx > 255 || yy < top || yy >= end)
              continue;
            int q = yy * 256 + xx;
            Integer owner = pixByte.get(q);
            if (owner != null && isSprite(snap, owner) == sprite && left.remove(q))
              queue.add(q);
          }
      }
      if (bytes.size() >= minBytes)
        out.add(new ArrayList<>(bytes));
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
  private int[] grow(TaintReplay.FrameSnapshot snap, int[] box, java.util.Set<Integer> mask,
      boolean sprite, int frame, int top, int end) {
    for (Recent rec : recent) {
      if (rec.sprite != sprite)
        continue; // a sprite's box overlaps the scenery's all the time: that is not the object
      int[] r = rec.box;
      // real OVERLAP, not proximity, and only while the result still looks like an object:
      // with slack and no cap the boxes chained along the terrain band until the whole
      // screen was one "object" and the run ended with seven of them
      if (box[0] > r[2] || r[0] > box[2] || box[1] > r[3] || r[1] > box[3])
        continue;
      int c0 = Math.min(box[0], r[0]), r0 = Math.min(box[1], r[1]);
      int c1 = Math.max(box[2], r[2]), r1 = Math.max(box[3], r[3]);
      if (c1 - c0 + 1 > maxCols || r1 - r0 + 1 > maxRows)
        continue;
      // SLICES, not the same thing moved. A repaint by halves covers a region the previous
      // half did not: the boxes touch and barely overlap. A sprite that walks a pixel a frame
      // gives nearly the SAME box every time, and merging those is how the astronaut ended up
      // dragging along every platform and explosion he passed — one entry per companion
      int ov = Math.max(0, Math.min(box[2], r[2]) - Math.max(box[0], r[0]) + 1)
          * Math.max(0, Math.min(box[3], r[3]) - Math.max(box[1], r[1]) + 1);
      int mine = (box[2] - box[0] + 1) * (box[3] - box[1] + 1);
      int otro = (r[2] - r[0] + 1) * (r[3] - r[1] + 1);
      if (ov > slice * Math.min(mine, otro))
        continue;
      // and only if the two halves are ONE shape. Overlapping boxes is not enough: the
      // astronaut walks over the capsule's box every time he passes it, and merging on that
      // put the two of them in one entry. Slices of the same planet are contiguous; two
      // things that happen to share a rectangle are two blobs, and stay apart
      List<Integer> both = new ArrayList<>(mask);
      both.addAll(rec.mask);
      if (blobs(snap, both, top, end).size() > 1)
        continue;
      box[0] = c0;
      box[1] = r0;
      box[2] = c1;
      box[3] = r1;
      mask.addAll(rec.mask); // its bytes are this drawing's too: the other half of the planet
    }
    recent.removeIf(r -> box[0] <= r.box[0] && box[1] <= r.box[1]
        && box[2] >= r.box[2] && box[3] >= r.box[3]);
    Recent me = new Recent();
    me.box = new int[]{box[0], box[1], box[2], box[3]};
    me.mask = new java.util.HashSet<>(mask);
    me.sprite = sprite;
    me.frame = frame;
    recent.add(me);
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
      java.util.Set<Integer> mask, int top, int end) {
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
    // and the same drawing PIXEL by pixel, with the address behind each one: that is what
    // the encoded sheet carries, and it is the only way the automatic objects come out in
    // the very format the editor produces by hand
    int[] px = new int[w * 8 * rows], owner = new int[w * 8 * rows];
    java.util.Arrays.fill(owner, -1);
    boolean lit = false;
    for (int y = minR; y <= maxR; y++)
      for (int c = minC; c <= maxC; c++) {
        int i = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2) | c;
        // ONLY what this drawing painted. Copying the whole box off the screen was the reason
        // every object came out with the neighbours inside it: the box is a rectangle and the
        // screen is full, so a capsule arrived with an explosion, a slice of platform and a
        // handful of stars glued on. What belongs to the object is the set of bytes its own
        // call wrote — everything else in the rectangle belongs to whoever drew it
        if (!mask.contains(i))
          continue;
        byte v = snap.pixels()[i];
        bits[(y - minR) * w + (c - minC)] = v;
        lit |= v != 0;
        int attr = snap.attrs()[(y >> 3) * 32 + c] & 0xff;
        int at = ((y - minR) >> 3) * cc + (c - minC);
        int inkIdx = (attr & 7) | ((attr >> 3) & 8);
        ink[at] = (byte) inkIdx;
        paper[at] = (byte) (((attr >> 3) & 7) | ((attr >> 3) & 8));
        int origin = snap.owner()[i] != 0 ? snap.owner()[i] - 1
            : snap.tile()[i] != 0 ? snap.tile()[i] - 1 : -1;
        for (int bit = 0; bit < 8; bit++) {
          if ((v & (0x80 >> bit)) == 0)
            continue;
          int at2 = (y - minR) * w * 8 + (c - minC) * 8 + bit;
          px[at2] = 0xff000000 | PALETTE_RGB[inkIdx & 0xf];
          owner[at2] = origin;
        }
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
      c.pickFrame = snap.frame();
      c.px = px;
      c.owner = owner;
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

  /**
   * The distinct objects, most drawn first, capped so the report stays readable — and each
   * one ONCE.
   *
   * <p>The key (which graphics, at what size) says the same object twice whenever the game
   * showed it differently: half repainted, walking behind a rock, one frame of its animation
   * that skips a piece. Every one of those is a different set of pieces at a different size,
   * so the list came out with the same capsule five times over.
   *
   * <p>So two entries are the same object when one's pieces are MOSTLY the other's
   * ({@code discover.objects.merge}, 70% of the smaller set): a partial sighting is a subset
   * of the full one, and two different objects rarely share that much even in an engine that
   * reuses graphics. The fullest drawing keeps the picture, the sightings add up.
   */
  public List<Composite> top(int max) {
    float merge = Float.parseFloat(System.getProperty("discover.objects.merge", "0.7"));
    List<Composite> all = new ArrayList<>(byHash.values());
    // the FULLEST sighting keeps the picture, so the entry shows the object whole and not the
    // two bytes of it a dirty-region engine repaints on most frames — which is what wins if
    // the sightings are ranked by how often they happened (measured: a sheet full of crumbs)
    all.sort((a, b) -> b.litPixels - a.litPixels);
    List<Composite> kept = new ArrayList<>();
    outer:
    for (Composite c : all) {
      for (Composite k : kept)
        if (sameObject(k, c, merge) || sameBank(k, c)) {
          k.count += c.count;
          k.firstFrame = Math.min(k.firstFrame, c.firstFrame);
          k.lastFrame = Math.max(k.lastFrame, c.lastFrame);
          c.pieces.forEach((base, n) -> k.pieces.merge(base, n, Integer::sum));
          continue outer;
        }
      kept.add(c);
    }
    kept.sort((a, b) -> b.count - a.count);
    return kept.size() > max ? kept.subList(0, max) : kept;
  }

  /**
   * The same sprite in its PRE-SHIFTED copies. A game that cannot shift pixels fast enough
   * keeps one copy of the sprite per X offset, one after another in memory, and each copy is
   * a different set of addresses — so the overlap test above sees eight different objects
   * where there is one. Measured in Exolon: the astronaut's frames sit at $efe3, $f043,
   * $f0a3..., blocks of $60 bytes in a row, and he took twelve entries of the sheet.
   *
   * <p>Two tight ranges that touch, at about the same size, are that. Anything whose
   * addresses are scattered across memory is not a bank and is left alone.
   */
  private boolean sameBank(Composite a, Composite b) {
    int a0 = Integer.MAX_VALUE, a1 = 0, b0 = Integer.MAX_VALUE, b1 = 0;
    for (int x : a.pieces.keySet()) {
      a0 = Math.min(a0, x);
      a1 = Math.max(a1, x);
    }
    for (int x : b.pieces.keySet()) {
      b0 = Math.min(b0, x);
      b1 = Math.max(b1, x);
    }
    if (a1 - a0 > bankSpan || b1 - b0 > bankSpan || b0 > a1 + bankGap || a0 > b1 + bankGap)
      return false;
    return Math.min(a.wBytes, b.wBytes) >= .6f * Math.max(a.wBytes, b.wBytes)
        && Math.min(a.rows, b.rows) >= .6f * Math.max(a.rows, b.rows);
  }

  /** how far apart two banks of the same sprite may sit, and how wide a bank can be. */
  private final int bankGap = Integer.getInteger("discover.objects.bankGap", 16);
  private final int bankSpan = Integer.getInteger("discover.objects.bankSpan", 512);

  /** whether {@code b} is the same object as {@code a}: most of the smaller set of pieces. */
  private static boolean sameObject(Composite a, Composite b, float merge) {
    int hit = 0;
    for (int base : b.pieces.keySet())
      if (a.pieces.containsKey(base))
        hit++;
    int smaller = Math.min(a.pieces.size(), b.pieces.size());
    return smaller > 0 && hit >= merge * smaller;
  }
}
