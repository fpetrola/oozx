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

package com.fpetrola.z80.analysis.query;

import com.fpetrola.z80.analysis.AnalysisDB;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The game's 64K memory as loaded, together with a mask of which addresses are runtime state
 * rather than data. Static data — fonts, strings, row-address tables, sprite sheets — lives at
 * the unmasked addresses; a detector reads them straight from here. {@link #present()} is false
 * when no image has been dumped yet (run the RZX analysis first); callers guard on it.
 *
 * <p>"Static" means <b>settled once initialization finished</b>, not "never touched since the RZX
 * snapshot". Plenty of games ship compressed or encrypted and unpack themselves into RAM at
 * startup, so the snapshot holds the packed form and the real data only exists afterwards: for
 * those, {@link #FINAL_MEM} (the memory at the end of the run) carries the data and {@link
 * #initBoundary} says which writes were part of that unpacking.
 *
 * <p>The mask comes from {@link #WRITER_MAP} — who wrote each address, exactly. A per-site
 * {@code addrMin..addrMax} envelope cannot do this job: a blit that writes both the screen and a
 * buffer claims every byte in between, swallowing any static data that happens to sit there.
 * Without those files it falls back to the cassette snapshot and the envelope.
 */
public final class MemoryImage {
  public static final String INIT_MEM = "analysis/init-mem.bin";
  public static final String FINAL_MEM = "analysis/final-mem.bin";
  public static final String WRITER_MAP = "analysis/last-writer.bin";
  /** Tracer.SITE_INIT: the address still holds what the snapshot loaded. */
  private static final int SITE_INIT = 0;

  private final byte[] mem; // null when no image is available
  private final boolean[] written = new boolean[0x10000];

  private MemoryImage(byte[] mem) {
    this.mem = mem;
  }

  /** the image plus the mask of everything that is runtime state. */
  public static MemoryImage of(AnalysisDB db) {
    int[] writerOf = readWriterMap();
    byte[] finalMem = read(FINAL_MEM);
    if (writerOf == null || finalMem == null)
      return envelopeFallback(db);

    // an address is data when nothing wrote it, or when the only thing that did was the
    // initialization unpacking — its content has stood still ever since, so the final image
    // still holds it.
    int boundary = initBoundary(db);
    MemoryImage img = new MemoryImage(finalMem);
    for (int a = 0; a < 0x10000; a++) {
      int site = writerOf[a];
      if (site != SITE_INIT)
        img.written[a] = lastWriteFrame(db, site) > boundary;
    }
    return img;
  }

  /** when the site last wrote anything — a store site or a block copy. Unknown = treat as live. */
  private static int lastWriteFrame(AnalysisDB db, int site) {
    AnalysisDB.Stat w = db.writes.get(site);
    if (w != null)
      return w.lastFrame();
    AnalysisDB.Bulk b = db.bulks.get(site);
    return b != null && b.lastFrame() >= 0 ? b.lastFrame() : Integer.MAX_VALUE;
  }

  /**
   * The frame the game finished unpacking itself at, or -1 when it never did. Unpacking covers
   * (nearly) every byte of a wide range about once and then never runs again — the one write
   * pattern whose result is data rather than state. A screen clear rewrites its range every frame
   * and a sparse walker only touches part of it: neither qualifies. Both a byte loop and an LDIR
   * count, since a loader may use either.
   */
  public static int initBoundary(AnalysisDB db) {
    int maxFrame = 0;
    for (AnalysisDB.Stat w : db.writes.values())
      maxFrame = Math.max(maxFrame, w.lastFrame());
    int boundary = -1;
    for (AnalysisDB.Stat w : db.writes.values())
      if (unpacks(w.addrMin(), w.addrMax(), w.count(), w.lastFrame(), maxFrame))
        boundary = Math.max(boundary, w.lastFrame());
    for (AnalysisDB.Bulk b : db.bulks.values())
      if (unpacks(b.dstMin(), b.dstEnd(), b.count() * b.lenMax(), b.lastFrame(), maxFrame))
        boundary = Math.max(boundary, b.lastFrame());
    return boundary;
  }

  private static boolean unpacks(int lo, int hi, long bytesWritten, int lastFrame, int maxFrame) {
    // a wide early fill of the screen is a loading screen or a first repaint, never unpacking:
    // nothing that lands on the display is static data
    if (hi >= 16384 && lo <= 23295)
      return false;
    long span = hi - lo + 1L;
    return span >= 1024 && bytesWritten >= span / 2 && bytesWritten <= span * 2
        && lastFrame >= 0 && lastFrame <= maxFrame / 20;
  }

  /** pre-{@link #WRITER_MAP} behaviour: the cassette snapshot, masked by per-site address ranges. */
  private static MemoryImage envelopeFallback(AnalysisDB db) {
    MemoryImage img = new MemoryImage(read(INIT_MEM));
    for (AnalysisDB.Stat w : db.writes.values())
      img.mark(w.addrMin(), w.addrMax());
    for (AnalysisDB.Bulk b : db.bulks.values())
      img.mark(b.dstMin(), b.dstMax() + Math.max(0, b.lenMax() - 1));
    return img;
  }

  private static byte[] read(String path) {
    try {
      return Files.readAllBytes(Path.of(path));
    } catch (Exception e) {
      return null;
    }
  }

  /** 64K little-endian site ids: which site last wrote each address. */
  private static int[] readWriterMap() {
    byte[] raw = read(WRITER_MAP);
    if (raw == null || raw.length < 0x10000 * 2)
      return null;
    int[] out = new int[0x10000];
    for (int a = 0; a < out.length; a++)
      out[a] = (raw[a * 2] & 255) | ((raw[a * 2 + 1] & 255) << 8);
    return out;
  }

  private void mark(int lo, int hi) {
    for (int a = Math.max(0, lo); a <= Math.min(0xffff, hi); a++)
      written[a] = true;
  }

  /** whether the cassette image is available (false = run the RZX analysis to dump it first). */
  public boolean present() {
    return mem != null;
  }

  /** whether the game overwrote this address (so it is runtime state, not cassette data). */
  public boolean written(int a) {
    return written[a];
  }

  /** the cassette byte at {@code a}, 0..255. */
  public int byteAt(int a) {
    return mem[a] & 255;
  }

  /** the 16-bit little-endian value at {@code a} (e.g. a row-table entry, a pointer). */
  public int word(int a) {
    return (mem[a] & 255) | ((mem[a + 1] & 255) << 8);
  }

  /**
   * The longest run of never-written addresses within {@code [lo..hi]} at least {@code minLen}
   * bytes long, as {@code {runLo, runHi}}, or null if none qualifies — the static-data span
   * inside a candidate table (glyphs, a row table) with any runtime-patched holes excluded.
   */
  public int[] largestUnwrittenRun(int lo, int hi, int minLen) {
    lo = Math.max(0, lo);
    hi = Math.min(0xffff, hi);
    int bestLo = -1, bestHi = -1, curLo = -1;
    for (int a = lo; a <= hi + 1; a++) {
      boolean blocked = a > hi || written[a];
      if (!blocked) {
        if (curLo < 0)
          curLo = a;
      } else {
        if (curLo >= 0 && a - 1 - curLo > bestHi - bestLo) {
          bestLo = curLo;
          bestHi = a - 1;
        }
        curLo = -1;
      }
    }
    return bestLo >= 0 && bestHi - bestLo + 1 >= minLen ? new int[]{bestLo, bestHi} : null;
  }
}
