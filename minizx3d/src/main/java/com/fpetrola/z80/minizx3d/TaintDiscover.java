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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Catalog DISCOVERY by taint instead of by draw-routine spying: replays the RZX with an
 * empty {@link OriginTaint} and asks, for every lit screen byte of sampled frames, which
 * original memory addresses its value was built from ({@link OriginTaint#leavesSorted}).
 * Those addresses ARE the game's graphics — no matter how the game drew them (direct blit,
 * pre-shifted copies, masked compositing, piecewise assembly), which is exactly where the
 * per-invocation {@code SpriteTracker} fuses everything into giant blocks (Exolon).
 *
 * <p>The unit of observation is the connected BLOB of lit bytes: its leaf set, split into
 * contiguous PIECES at address gaps, gives one observation per piece per frame — a sprite
 * standing on a platform yields its own 32-byte piece plus the platform's tile piece,
 * already separated. Aggregation mirrors the tracker's: extent = mode of observed ends,
 * clipped fragments absorbed into the covering entry.
 *
 * <p>Discriminators (doc/DETECCION-SPRITES-3D.md §5 paso 1). Buffers are filtered at the
 * leaf level: an address that keeps being WRITTEN is a work buffer that leaked into the
 * taint, not a static graphic ({@code max(discover.maxwrites, frame/64)} writes). Then a
 * piece is a SPRITE only if every signal agrees it is a small, moving, non-stamped thing:
 * <ul>
 *   <li><b>bpo</b> (footprint per stamp, {@code -Ddiscover.bg}): one observation of a
 *       sprite paints ~8-40 bytes; a backdrop paints hundreds. Per-STAMP, not per-frame,
 *       because JSW's lives row stamps one Willy frame 4+ times per frame.</li>
 *   <li><b>fresh</b> ({@code -Ddiscover.freshwin/freshfrac}): sprites get redrawn,
 *       painted-once backgrounds do not. Useless on whole-screen re-blitters (JSW).</li>
 *   <li><b>stamps</b> per frame ({@code -Ddiscover.stamps}): entities are few, animated
 *       fill patterns (menu stripes) are dozens.</li>
 *   <li><b>reuse</b> ({@code -Ddiscover.reuse}): screen bytes per distinct graphic byte —
 *       a blit is ~1:1, a tile bitmap stamped over a 10-cell platform is 10:1. THE signal
 *       that catches a room's tile zone even when the layout scatters it everywhere.</li>
 *   <li><b>mobility</b> ({@code -Ddiscover.mobility}): distinct cells ever painted vs one
 *       stamp — platforms never move. Known tradeoff: an in-place multi-frame animator
 *       (JSW's Maria) reads as static; flip its row by hand if it matters.</li>
 *   <li><b>drift</b> ({@code -Ddiscover.drift}, off by default): frame-to-frame movement of
 *       the painted cell-set — the dirty-region pair below.</li>
 * </ul>
 *
 * <p>Dirty-region engines (Exolon rewrites only 2-10% of the lit screen per frame) need the
 * {@code -Ddiscover.gate} + {@code -Ddiscover.drift} pair: the trails such engines leave lit
 * carry stale taint that inverts every other discriminator at once. See those two fields for
 * why neither works without the other. Whole-screen re-blitters (JSW, MM) want both off.
 * Background rows go out as {@code methods="udg taint"} (tile zone); all metrics ride in
 * the methods column for hand curation.
 *
 * <p>Output: table {@code sprites_found} (same shape the tracker writes, so
 * {@link SpriteCatalog} loads it unchanged) in a db that needs nothing else — the pass is
 * self-contained: {@code TaintDiscover <rzx> <db> [maxFrames]}. Existing rows are REPLACED;
 * back up curated dbs first.
 */
public final class TaintDiscover {
  static final int SCREEN = TaintReplay.SCREEN, PIXEL_BYTES = TaintReplay.PIXEL_BYTES;
  /** first address that can hold game graphics: above ROM + screen + attributes. */
  static final int RAM_START = 0x5B00;

  /** screen index of byte (row y, column col): the interleaved Spectrum layout. */
  private static final int[] IDX = new int[192 * 32];

  static {
    for (int y = 0; y < 192; y++)
      for (int col = 0; col < 32; col++)
        IDX[y * 32 + col] = ((y & 0xC0) << 5) | ((y & 7) << 8) | ((y & 0x38) << 2) | col;
  }

  final int sample = Integer.getInteger("discover.sample", 5);
  final int from = Integer.getInteger("discover.from", 500);
  final int gap = Integer.getInteger("discover.gap", 16);
  final int bgBytesPerStamp = Integer.getInteger("discover.bg", 64);
  final int maxWrites = Integer.getInteger("discover.maxwrites", 16);
  final int minVeces = Integer.getInteger("discover.min", 3);
  final int leafCap = Integer.getInteger("discover.cap", 32);
  /** a screen byte written within this many frames counts as freshly redrawn. */
  final int freshWin = Integer.getInteger("discover.freshwin", 12);
  /**
   * GATE (-Ddiscover.gate=N, 0=off): in the GATED sweep, ignore lit bytes not rewritten
   * within N frames. A dirty-region engine (Exolon rewrites 2-10% of the lit screen per
   * frame, Dynamite Dan likewise) leaves a sprite's abandoned pixels lit and carrying its
   * taint for tens of frames, and those ghosts make a moving sprite look like it never
   * leaves. Gating is what lets {@link #minDrift} see the motion.
   *
   * <p>It applies to that sweep ONLY. Gating the coverage metrics was tried and is wrong:
   * it shatters a stamped region into the slivers repainted this frame, and {@code reuse}
   * and {@code stamps} — measured WITHIN a blob — go blind exactly where they were meant to
   * bite, which is how Exolon's rock texture came out a sprite with {@code reuse=0.9}.
   */
  final int gateWin = Integer.getInteger("discover.gate", 8);
  /** minimum fraction of a piece's painted bytes that must be fresh to call it a sprite. */
  final float freshFrac = Float.parseFloat(System.getProperty("discover.freshfrac", "0.35"));
  /** more stamps per frame than this is a repeating pattern (fill/border), not entities. */
  final int maxStamps = Integer.getInteger("discover.stamps", 16);
  /** pieces smaller than this are noise (a stray byte), dropped outright. */
  final int minSize = Integer.getInteger("discover.minsize", 4);
  /** only the top N cell rows are sampled: the status area (lives row = one blob of
   *  shoulder-to-shoulder Willys, score text) poisons the stamp metrics, and entities
   *  live in the playfield. Runtime classification still covers the whole screen — the
   *  lives row draws from the same addresses the playfield discovered. */
  final int playfieldRows = Integer.getInteger("discover.rows", 16);
  /**
   * Mobility: distinct screen cells a graphic ever painted, relative to one stamp's
   * footprint. A platform/conveyor repaints the SAME cells forever (~1), an entity moves
   * (>2). This is the discriminator that survives engines that re-blit the whole screen
   * every frame (JSW), where write-freshness says nothing.
   */
  final float minMobility = Float.parseFloat(System.getProperty("discover.mobility", "2"));
  /**
   * Reuse: screen bytes painted per distinct graphic byte used, per observation. A sprite
   * maps ~1 screen byte per graphic byte (a bijective blit; masks push it BELOW 1), a
   * tile bitmap is stamped over many cells (a 10-cell platform = 80 screen bytes from 8
   * bytes). This is what tells a room's tile-bitmap zone from a guardian even when the
   * layout scatters it everywhere (mobility fails there).
   */
  final float maxReuse = Float.parseFloat(System.getProperty("discover.reuse", "2"));
  /**
   * Drift: how much a graphic's painted cell-set MOVES between consecutive sampled frames
   * (symmetric difference / union). An entity walks, so almost none of its cells repeat
   * (~1); a backdrop element repaints exactly where it was (~0). Unlike {@link #minMobility}
   * this is a LOCAL measurement, which is what makes it work on flip-screen games: Exolon's
   * scenery sits in different places in different rooms, so accumulated mobility is high
   * for backdrop and entities alike, but frame-to-frame that scenery does not budge.
   * It also survives trail-leaving engines, where write-freshness reads background-low for
   * moving sprites (their abandoned trail stays lit and un-rewritten).
   *
   * <p>Off by default, and only meaningful TOGETHER WITH {@link #gateWin} — the two are the
   * dirty-region-engine pair. Without the gate, abandoned trails keep a moving sprite's
   * cell-set nailed in place and drift collapses toward 0 for exactly the entities it is
   * meant to find; with the gate, {@code mob} in turn loses its bite on a flip-screen game
   * (scenery is only ever observed right after a room flip, at a different spot per room,
   * so it accumulates high mobility), leaving drift as the only local-in-time signal.
   *
   * <p>Caveat — ALIASING: an entity on a short cyclic path can return to the same cells in
   * exactly {@code discover.sample} frames and read as static. That is what it does on JSW,
   * whose guardians oscillate over a couple of cells: 32-byte guardian bitmaps score
   * drift=0.00. JSW does not need it (no trails, so freshness and mobility already work),
   * which is the other reason this defaults off rather than being tied to the gate.
   */
  final float minDrift = Float.parseFloat(System.getProperty("discover.drift", "0"));
  // NOTE: a content-change signal (rescue in-place animators like Maria via pixel churn)
  // was tried and REVERTED from the classifier: multi-frame animators alternate between
  // whole pieces (each frame paints ITS constant pixels -> 0 changes per piece), so Maria
  // gained nothing, while short conveyors (1-2 cells, reuse ~1) got rescued wrongly. The
  // chg= metric still goes to the methods column for hand curation.

  /** per base address: support, screen footprint, frame span, histogram of observed ends. */
  static final class Agg {
    long veces, screenBytes, freshBytes;
    /** distinct graphic bytes used, summed per observation — the reuse denominator. */
    long leafBytes;
    int frames, frameFirst = Integer.MAX_VALUE, frameLast;
    final Map<Integer, Integer> lastHist = new HashMap<>();
    /** every screen byte this graphic ever painted, for the mobility discriminator. */
    final java.util.BitSet cells = new java.util.BitSet();
    /** last pixel value seen per exclusive cell + change events — in-place animation. */
    final Map<Integer, Integer> cellVal = new HashMap<>();
    long changes;
    /** cells painted in the previous sampled frame + the frame it was, for drift. */
    Set<Integer> prevCells;
    int prevFrame = -1;
    /** summed symmetric-difference fraction between consecutive frames, and its count. */
    double driftSum;
    long driftFrames;
  }

  private final Map<Integer, Agg> byBase = new TreeMap<>();
  private final Map<Integer, int[]> leafMemo = new HashMap<>();
  private long sampledFrames, saturatedBytes;
  private TaintReplay replay;

  public static void main(String[] args) throws Exception {
    // discovery WANTS deep leaves: old origins in long compositing chains are the catalog
    if (System.getProperty("taint.depth") == null)
      System.setProperty("taint.depth", "512");
    String rzx = args.length > 0 ? args[0]
        : "/home/fernando/detodo/spectrum/oozx/Jet Set Willy - Mildly Patched.rzx";
    String db = args.length > 1 ? args[1] : "analysis/analysis.db";
    int maxFrames = args.length > 2 ? Integer.parseInt(args[2])
        : Integer.getInteger("max.frames", Integer.MAX_VALUE);
    // discover.* now come from the same places the viewer's other settings do, so a knob
    // calibrated in the TAB menu (saved to the per-game config file) applies on re-catalog.
    // Precedence, highest first: explicit -D > per-game config file (live edits) > games.json
    // per-game > games.json global. Needs -Dgame to know which game's files to read.
    loadDiscoverSettings(System.getProperty("game"));
    if (Boolean.getBoolean("discover.report.only"))
      new TaintDiscover().reportOnly(rzx, db);
    else
      new TaintDiscover().run(rzx, db, maxFrames);
  }

  /** seed discover.* system properties from the config file then games.json (only-if-absent). */
  private static void loadDiscoverSettings(String game) {
    // one config file for everything: the discover knobs calibrated live sit in
    // games.<juego>.config.discover, and the games.json properties are the defaults under it
    if (game != null) {
      com.badlogic.gdx.utils.JsonValue g = GameProfile.gameNode(game, false);
      com.badlogic.gdx.utils.JsonValue cfg = g == null ? null : g.get("config");
      GameProfile.applyProps("discover", cfg == null ? null : cfg.get("discover"));
    }
    GameProfile.applyGamesJson(game);
  }

  void run(String rzx, String db, int maxFrames) throws Exception {
    long t0 = System.currentTimeMillis();
    TaintReplay r = new TaintReplay(rzx,
        new OriginTaint(new int[0x10000], new boolean[0x10000]), this::onFrame);
    replay = r;
    r.paced = false;
    r.maxFrames = maxFrames;
    r.run();
    System.out.println("TaintDiscover: " + sampledFrames + " frames sampled (every " + sample
        + " from " + from + "), " + r.taint.nodeCount() + " union nodes, "
        + saturatedBytes + " saturated bytes, " + (System.currentTimeMillis() - t0) / 1000 + "s");
    emit(rzx, db);
  }

  private void onFrame(TaintReplay.FrameSnapshot snap) {
    int frame = snap.frame();
    if (frame < from || (frame - from) % sample != 0)
      return;
    sampledFrames++;
    if (frame % 5000 < sample)
      System.out.println("  frame " + frame + ": " + byBase.size() + " bases, "
          + replay.taint.nodeCount() + " nodes");
    // buffers get hotter as the run advances: keep a floor early, scale with time later
    int writeCap = Math.max(maxWrites, frame >> 6);
    byte[] px = snap.pixels();
    Set<Integer> basesThisFrame = new HashSet<>();
    Map<Integer, Set<Integer>> frameCells = new HashMap<>(); // base -> cells painted now
    // Each signal is measured in the view where it is VALID (doc §5.1). Ungated, a blob is
    // the whole painted region, which is the only way "how much area does this graphic cover
    // at once" (reuse, stamps, bpo) can be seen — gating shatters a stamped terrain band
    // into the slivers repainted this frame and those two go blind, which is how Exolon's
    // rock texture passed as a sprite. Gated, only what the game is painting NOW survives,
    // which is the only way "did it move" (drift) can be seen — ungated, the trails a
    // dirty-region engine leaves lit nail a moving sprite's cell-set in place.
    scan(px, frame, writeCap, basesThisFrame, frameCells, false);
    if (minDrift > 0)
      scan(px, frame, writeCap, basesThisFrame, frameCells, true);
    // drift: compare each graphic's footprint with the one it had in the PREVIOUS sampled
    // frame. Only consecutive samples are comparable — a graphic that was absent in between
    // restarts the measurement rather than reporting a jump it did not make.
    frameCells.forEach((base, now) -> {
      Agg agg = byBase.get(base);
      if (agg == null)
        return; // seen only in the gated view: no aggregate to attach the drift to
      if (agg.prevFrame == frame - sample) {
        int common = 0;
        for (int si : now)
          if (agg.prevCells.contains(si))
            common++;
        agg.driftSum += 1 - common / (double) (now.size() + agg.prevCells.size() - common);
        agg.driftFrames++;
      }
      agg.prevCells = now;
      agg.prevFrame = frame;
    });
  }

  /**
   * One flood-fill sweep of the screen. {@code gated} selects the view: ungated feeds the
   * coverage metrics, gated feeds drift only (see {@link #onFrame}).
   */
  private void scan(byte[] px, int frame, int writeCap, Set<Integer> basesThisFrame,
                    Map<Integer, Set<Integer>> frameCells, boolean gated) {
    boolean[] seen = new boolean[PIXEL_BYTES];
    int maxY = playfieldRows * 8;
    List<int[]> blob = new ArrayList<>(); // {y, col} cells of the current flood fill
    java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
    for (int y0 = 0; y0 < maxY; y0++)
      for (int c0 = 0; c0 < 32; c0++) {
        int i0 = IDX[y0 * 32 + c0];
        if (!lit(px, i0, frame, gated) || seen[i0])
          continue;
        blob.clear();
        seen[i0] = true;
        queue.add(new int[]{y0, c0});
        while (!queue.isEmpty()) {
          int[] p = queue.poll();
          blob.add(p);
          for (int dy = -1; dy <= 1; dy++)
            for (int dc = -1; dc <= 1; dc++) {
              int y = p[0] + dy, c = p[1] + dc;
              if (y < 0 || y >= maxY || c < 0 || c >= 32)
                continue;
              int i = IDX[y * 32 + c];
              if (lit(px, i, frame, gated) && !seen[i]) {
                seen[i] = true;
                queue.add(new int[]{y, c});
              }
            }
        }
        processBlob(blob, px, frame, writeCap, basesThisFrame, frameCells, gated);
      }
  }

  /** a byte counts as painted content: it has pixels and, under the gate, is not a ghost. */
  private boolean lit(byte[] px, int i, int frame, boolean gated) {
    return px[i] != 0 && (!gated || gateWin == 0 || frame - replay.lastWrite[i] <= gateWin);
  }

  /** one connected group of lit bytes: leaf union -> pieces -> one observation per piece. */
  private void processBlob(List<int[]> blob, byte[] px, int frame, int writeCap,
                           Set<Integer> basesThisFrame, Map<Integer, Set<Integer>> frameCells,
                           boolean gated) {
    List<int[]> byteLeaves = new ArrayList<>(blob.size());
    int total = 0;
    for (int[] p : blob) {
      int i = IDX[p[0] * 32 + p[1]];
      int[] lv = replay.taint.leavesSorted(replay.taint.mem[SCREEN + i], leafCap, leafMemo);
      if (lv == null) { // mixes too many origins to identify anything
        saturatedBytes++;
        byteLeaves.add(null);
        continue;
      }
      int n = 0;
      int[] keep = new int[lv.length];
      for (int a : lv)
        if (a >= RAM_START && replay.memWrites[a] <= writeCap)
          keep[n++] = a;
      int[] k = n == lv.length ? lv : java.util.Arrays.copyOf(keep, n);
      byteLeaves.add(n == 0 ? null : k);
      total += n;
    }
    if (total == 0)
      return;
    int[] all = new int[total];
    int n = 0;
    for (int[] lv : byteLeaves)
      if (lv != null)
        for (int a : lv)
          all[n++] = a;
    java.util.Arrays.sort(all);
    // contiguous pieces: a gap wider than -Ddiscover.gap separates two graphics.
    // uniq = distinct addresses of the piece this observation actually used
    List<int[]> pieces = new ArrayList<>(); // {lo, hi, screenBytes, freshBytes, uniq}
    int lo = all[0], hi = all[0], uniq = 1;
    for (int i = 1; i < n; i++) {
      if (all[i] > hi + gap) {
        pieces.add(new int[]{lo, hi, 0, 0, uniq});
        lo = all[i];
        uniq = 0;
      }
      if (all[i] != hi || uniq == 0)
        uniq++;
      hi = all[i];
    }
    pieces.add(new int[]{lo, hi, 0, 0, uniq});
    // footprint: how many screen bytes each piece painted this frame (a composited byte
    // counts for every piece that fed it — that is the point of per-pixel reasoning),
    // and how many of those were freshly REDRAWN (sprites are, backgrounds are not)
    int[] los = new int[pieces.size()];
    for (int i = 0; i < pieces.size(); i++)
      los[i] = pieces.get(i)[0];
    for (int bi = 0; bi < blob.size(); bi++) {
      int[] lv = byteLeaves.get(bi);
      if (lv == null)
        continue;
      int[] cell = blob.get(bi);
      int si = IDX[cell[0] * 32 + cell[1]];
      boolean fresh = frame - replay.lastWrite[si] <= freshWin;
      int last = -1, touched = 0;
      for (int a : lv) {
        int p = java.util.Arrays.binarySearch(los, a);
        if (p < 0)
          p = -p - 2; // insertion point - 1: the piece whose lo <= a
        if (p != last) {
          if (gated) {
            frameCells.computeIfAbsent(los[p], k -> new HashSet<>()).add(si);
          } else {
            pieces.get(p)[2]++;
            if (fresh)
              pieces.get(p)[3]++;
            byBase.computeIfAbsent(los[p], k -> new Agg()).cells.set(si);
          }
          last = p;
          touched++;
        }
      }
      if (!gated && touched == 1) { // exclusive byte: its pixels describe THIS piece's content
        Agg agg = byBase.get(los[last]);
        Integer prev = agg.cellVal.put(si, px[si] & 0xff);
        if (prev != null && prev != (px[si] & 0xff))
          agg.changes++;
      }
    }
    if (gated)
      return; // the gated view exists only to say WHERE each graphic is painting right now
    for (int[] piece : pieces) {
      Agg agg = byBase.computeIfAbsent(piece[0], k -> new Agg());
      agg.veces++;
      agg.screenBytes += piece[2];
      agg.freshBytes += piece[3];
      agg.leafBytes += piece[4];
      agg.lastHist.merge(piece[1], 1, Integer::sum);
      agg.frameFirst = Math.min(agg.frameFirst, frame);
      agg.frameLast = Math.max(agg.frameLast, frame);
      if (basesThisFrame.add(piece[0]))
        agg.frames++;
    }
  }

  /**
   * The objects found automatically, written in the SAME shape the editor produces by hand:
   * an encoded sheet ({@link ObjectSheet}, every pixel carrying the address of the graphic
   * behind it) plus their entries in the one config file. So the detection and the hand work
   * meet in one format — the viewer already knows how to match and render these, and the
   * editor can open them, fix the ones that came out wrong and drop the rest.
   *
   * <p>Kept apart from what was marked by hand ({@code objetosAuto} vs {@code objetos}):
   * a re-catalogue overwrites these, and nobody's afternoon of marking should ride on that.
   */
  private void writeAutoObjects(String game, List<SpriteComposites.Composite> objects) {
    if (game == null || objects.isEmpty())
      return;
    // TWO graphics is still a tile and its neighbour — measured on Exolon: at 2 the list
    // filled with pairs of scenery bytes that then matched half the screen at score 1.00
    int minPieces = Integer.getInteger("discover.objects.minPiezas", 3);
    int minSeen = Integer.getInteger("discover.objects.minVeces", 5);
    try {
      List<String> names = new ArrayList<>();
      List<int[]> cells = new ArrayList<>(), pixels = new ArrayList<>(), owners = new ArrayList<>();
      StringBuilder json = new StringBuilder("{\"objetos\": [");
      java.util.Set<java.util.Set<Integer>> already = new java.util.HashSet<>();
      int x = 0, y = 1, shelf = 0, n = 0; // row 0 belongs to the sheet's legend
      for (SpriteComposites.Composite c : objects) {
        if (c.px == null)
          continue;
        int w = c.wBytes * 8, h = c.rows;
        if (x + w > 512 && x > 0) {
          x = 0;
          y += shelf + 2;
          shelf = 0;
        }
        java.util.Set<Integer> gfx = new java.util.LinkedHashSet<>();
        for (int a : c.owner)
          if (a >= 0)
            gfx.add(a);
        // one graphic is a TILE, and the tile path already draws those; a definition with a
        // single address matches any patch that happens to contain it with a perfect score,
        // so writing them would only make the matcher pick noise over the real objects.
        // Rare sightings go the same way: seen twice is as likely a torn frame as a thing
        if (gfx.size() < minPieces || c.count < minSeen || !already.add(gfx))
          continue; // the same object at another size: the fullest sighting came first
        String name = "auto" + (++n);
        names.add(name);
        cells.add(new int[]{x, y, w, h});
        pixels.add(c.px);
        owners.add(c.owner);
        StringBuilder piezas = new StringBuilder();
        for (int a : gfx)
          piezas.append(piezas.length() > 0 ? "," : "").append('$').append(Integer.toHexString(a));
        json.append(n > 1 ? "," : "").append("{\"nombre\": \"").append(name)
            .append("\", \"piezas\": \"").append(piezas)
            // the size it was drawn at, which is what the matcher uses to tell this thing
            // from a chain of cells that happens to share a graphic
            .append("\", \"rects\": \"0 0 ").append(w).append(' ').append(h)
            .append("\", \"hoja\": \"").append(x).append(' ').append(y).append(' ')
            .append(w).append(' ').append(h).append("\", \"veces\": ").append(c.count)
            .append('}');
        x += w + 2;
        shelf = Math.max(shelf, h);
      }
      json.append("]}");
      if (names.isEmpty())
        return;
      String png = "doc/objetos-auto-" + game + ".png";
      ObjectSheet.write(png, names, cells, pixels, owners);
      com.badlogic.gdx.utils.JsonValue list = new com.badlogic.gdx.utils.JsonReader()
          .parse(json.toString()).get("objetos");
      GameProfile.put(GameProfile.gameNode(game, true), "objetosAuto",
          list.toJson(com.badlogic.gdx.utils.JsonWriter.OutputType.json));
      GameProfile.save();
      System.out.println("objetos automaticos: " + names.size() + " en " + png
          + " y en games.json (objetosAuto)");
    } catch (Exception e) {
      System.out.println("no se pudieron escribir los objetos automaticos: " + e);
    }
  }

  /**
   * {@code -Ddiscover.report.only=true}: rebuild the readable catalogue from the catalogue
   * that is ALREADY in the db, without re-running discovery. Re-rendering the report (or
   * re-capturing the composed objects with different bounds) is a minutes-long job over a
   * table that is already there; re-deriving it would be half an hour of replay for nothing.
   */
  private void reportOnly(String rzx, String db) throws Exception {
    Map<Integer, long[]> rows = new TreeMap<>();
    try (java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + db);
         java.sql.Statement st = c.createStatement();
         java.sql.ResultSet rs = st.executeQuery(
             "SELECT base, last, veces, frame_first, frame_last, methods FROM sprites_found")) {
      while (rs.next()) {
        String methods = rs.getString(6);
        rows.put(rs.getInt(1), new long[]{rs.getInt(2), rs.getInt(3), rs.getInt(4),
            rs.getInt(5), methods != null && methods.startsWith("udg") ? 1 : 0});
      }
    }
    System.out.println("reporte desde " + db + ": " + rows.size() + " entradas");
    writeReport(rows, rzx, db);
  }

  /**
   * The same catalogue as something readable, and as what the viewer actually loads: one
   * Markdown per game with every graphic drawn in ASCII plus a contact sheet
   * ({@link SpriteReport}). Off unless {@code -Dgame} says which game this is, since the
   * file is named after it; {@code -Ddiscover.report} sets an explicit path.
   */
  private void writeReport(Map<Integer, long[]> rows, String rzxPath, String dbPath) {
    String game = System.getProperty("game");
    String path = System.getProperty("discover.report",
        game == null ? null : "doc/catalogo-" + game + ".md");
    if (path == null)
      return;
    try {
      List<SpriteReport.Entry> entries = new ArrayList<>();
      boolean fromDb = rows.values().stream().anyMatch(a -> a.length == 5);
      for (Map.Entry<Integer, long[]> e : rows.entrySet()) {
        long[] a = e.getValue();
        int base = e.getKey(), last = (int) a[0], size = last - base + 1;
        // rebuilt from the db the classification is already made (its methods column says
        // udg/taint); measured live it comes from the metrics
        boolean bg = fromDb ? a[4] != 0 : isBackground(a);
        // 2 bytes per row is the 16px sprite every one of these games draws; the curated
        // widths (DD's 1..3) live in the db's methods column and are kept when re-read
        entries.add(new SpriteReport.Entry(base, last, size, (int) a[1],
            (int) a[2], (int) a[3],
            bg ? "fondo" : "sprite", bg ? 1 : 2,
            fromDb ? "" : "bpo=" + bpo(a) + " fresh=" + (int) (freshOf(a) * 100) + "% stamps="
                + (int) stampsOf(a) + " mob=" + String.format("%.1f", mobilityOf(a))
                + " reuse=" + String.format("%.1f", reuseOf(a))
                + " drift=" + String.format("%.2f", driftOf(a))));
      }
      // A SECOND, SHORT PASS with the catalogue just written: the objects the game composes
      // can only be seen once there IS a catalogue to attribute pixels to, and grouping them
      // is the viewer's own adjacency flood. Bounded — a few thousand frames show every
      // object many times, and a full-length second replay would cost as much as discovery.
      List<SpriteComposites.Composite> objects = List.of();
      java.util.function.IntUnaryOperator mem = replay != null ? replay::memByte : a -> 0;
      if (Boolean.parseBoolean(System.getProperty("discover.objects", "true"))) {
        try {
          SpriteComposites comp = new SpriteComposites(4);
          comp.collect(rzxPath, new SpriteCatalog(dbPath, 128),
              Integer.getInteger("discover.objects.from", from),
              Integer.getInteger("discover.objects.frames", 6000),
              Integer.getInteger("discover.objects.sample", 4));
          objects = comp.top(Integer.getInteger("discover.objects.max", 240));
          if (replay == null)
            mem = comp.memByte(); // rebuilt from the db: this pass is the only emulator around
          System.out.println("compuestos: " + objects.size() + " objetos distintos");
        } catch (Exception ex) {
          System.out.println("no se pudieron capturar los objetos compuestos: " + ex);
        }
      }
      SpriteReport.write(path, game == null ? "juego" : game, entries, mem, objects);
      writeAutoObjects(game, objects);
    } catch (Exception ex) {
      System.out.println("no se pudo escribir el catalogo legible: " + ex);
    }
  }

  /** aggregate, absorb clipped fragments, classify, and REPLACE sprites_found. */
  private void emit(String rzxPath, String dbPath) throws Exception {
    // extent = MODE of observed ends (ties -> the larger end), like the tracker: a rare
    // fusion with a neighbour must not poison the sprite
    Map<Integer, long[]> rows = new TreeMap<>(); // base -> {last, veces, f0, f1, screenBytes, freshBytes, frames}
    byBase.forEach((base, a) -> {
      if (a.veces < minVeces)
        return;
      int last = a.lastHist.entrySet().stream()
          .max(Comparator.<Map.Entry<Integer, Integer>>comparingInt(Map.Entry::getValue)
              .thenComparingInt(Map.Entry::getKey)).get().getKey();
      if (last - base + 1 < minSize)
        return;
      rows.put(base, new long[]{last, a.veces, a.frameFirst, a.frameLast, a.screenBytes,
          a.freshBytes, a.frames, a.cells.cardinality(), a.leafBytes, a.changes,
          // drift is a mean: carry numerator and count so absorbing re-averages correctly
          Math.round(a.driftSum * 1000), a.driftFrames});
    });
    // absorb what looks like a CLIPPED read of the covering entry: starts inside it and
    // ends within it (a neighbour graphic ends beyond and stays apart)
    Integer prev = null;
    for (int base : new ArrayList<>(rows.keySet())) {
      if (prev != null && base <= rows.get(prev)[0] && rows.get(base)[0] <= rows.get(prev)[0]) {
        long[] into = rows.get(prev), fromR = rows.remove(base);
        into[1] += fromR[1];
        into[2] = Math.min(into[2], fromR[2]);
        into[3] = Math.max(into[3], fromR[3]);
        into[4] += fromR[4];
        into[5] += fromR[5];
        into[6] += fromR[6];
        into[7] += fromR[7]; // cells: sum approximates the union — same graphic anyway
        into[8] += fromR[8];
        into[9] += fromR[9];
        into[10] += fromR[10];
        into[11] += fromR[11];
      } else
        prev = base;
    }
    int sprites = 0, tiles = 0;
    try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      c.setAutoCommit(false);
      try (Statement st = c.createStatement()) {
        st.execute("DROP TABLE IF EXISTS sprites_found");
        st.execute("CREATE TABLE sprites_found(base INT, last INT, size INT, veces INT," +
            " frame_first INT, frame_last INT, methods TEXT)");
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO sprites_found VALUES(?,?,?,?,?,?,?)")) {
        for (Map.Entry<Integer, long[]> e : rows.entrySet()) {
          long[] a = e.getValue();
          if (isBackground(a))
            tiles++;
          else
            sprites++;
          ps.setInt(1, e.getKey());
          ps.setInt(2, (int) a[0]);
          ps.setInt(3, (int) a[0] - e.getKey() + 1);
          ps.setInt(4, (int) a[1]);
          ps.setInt(5, (int) a[2]);
          ps.setInt(6, (int) a[3]);
          ps.setString(7, (isBackground(a) ? "udg taint" : "taint") + " bpo=" + bpo(a)
              + " fresh=" + (int) (freshOf(a) * 100) + "% stamps=" + (int) stampsOf(a)
              + " mob=" + String.format("%.1f", mobilityOf(a))
              + " reuse=" + String.format("%.1f", reuseOf(a))
              + " drift=" + String.format("%.2f", driftOf(a)) + " chg=" + a[9]);
          ps.addBatch();
        }
        ps.executeBatch();
      }
      c.commit();
    }
    writeReport(rows, rzxPath, dbPath);
    System.out.println("=== TAINT-DISCOVER: " + sprites + " sprites + " + tiles
        + " zonas de fondo (sprites_found REEMPLAZADA en " + dbPath + ") ===");
    rows.entrySet().stream()
        .sorted(Comparator.comparingLong(e -> -e.getValue()[1]))
        .limit(24)
        .forEach(e -> {
          long[] a = e.getValue();
          System.out.printf("  $%04x..$%04x (%d bytes) x%d frames %d..%d bpo=%d fresh=%d%%"
                  + " stamps=%d mob=%.1f reuse=%.1f drift=%.2f chg=%d %s%n",
              e.getKey(), a[0], a[0] - e.getKey() + 1, a[1], a[2], a[3], bpo(a),
              (int) (freshOf(a) * 100), (int) stampsOf(a), mobilityOf(a), reuseOf(a),
              driftOf(a), a[9], isBackground(a) ? "FONDO" : "sprite");
        });
  }

  /** screen bytes painted per observation: one stamp of the graphic, however many stamps. */
  private long bpo(long[] a) {
    return a[1] == 0 ? 0 : a[4] / a[1];
  }

  /** fraction of the painted bytes that had been written within the freshness window. */
  private double freshOf(long[] a) {
    return a[4] == 0 ? 0 : a[5] / (double) a[4];
  }

  /** stamps of the graphic per frame it appeared in: entities are few, patterns many. */
  private double stampsOf(long[] a) {
    return a[1] / (double) Math.max(1, a[6]);
  }

  /** distinct cells ever painted relative to one stamp: statics ~1, moving entities >2. */
  private double mobilityOf(long[] a) {
    return a[7] / (double) Math.max(1, bpo(a));
  }

  /** screen bytes painted per distinct graphic byte used: blits ~1, stamped tiles >>2. */
  private double reuseOf(long[] a) {
    return a[4] / (double) Math.max(1, a[8]);
  }

  /** mean frame-to-frame change of the painted cell-set: scenery ~0, walking entity ~1. */
  private double driftOf(long[] a) {
    return a[11] == 0 ? 1 : a[10] / 1000.0 / a[11]; // never measured -> do not veto
  }

  /** background = big stamps (a backdrop), painted-once-and-left (walls, menus), stamped
   *  everywhere every frame (an animated fill), stamped many cells per graphic byte (a
   *  room's tile bitmaps, which the layout scatters everywhere so mobility looks high),
   *  or never moving (platforms — the signal that survives whole-screen re-blitters). */
  private boolean isBackground(long[] a) {
    return bpo(a) > bgBytesPerStamp || freshOf(a) < freshFrac || stampsOf(a) > maxStamps
        || reuseOf(a) > maxReuse || mobilityOf(a) < minMobility || driftOf(a) < minDrift;
  }
}
