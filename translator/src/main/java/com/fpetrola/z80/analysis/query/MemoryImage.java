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
 * The initial 64K memory image (the cassette load, {@code analysis/init-mem.bin}) together with
 * a mask of which addresses the game later overwrote. Static data — fonts, strings, row-address
 * tables, sprite sheets — lives at addresses that are present in the image and NEVER written; a
 * detector reads them straight from here. Anything the game writes is runtime state, not
 * cassette data, so it is masked out.
 *
 * <p>This bundles what {@code TextFinder} and {@code ScreenMapper} each used to carry by hand
 * (the {@code byte[]} load, the {@code boolean[] written} mask, {@code mark}/{@code word}, the
 * largest-unwritten-run scan). {@link #present()} is false when the image has not been dumped
 * yet (run the RZX analysis first); callers guard on it before decoding.
 */
public final class MemoryImage {
  public static final String INIT_MEM = "analysis/init-mem.bin";

  private final byte[] mem; // null when analysis/init-mem.bin is absent
  private final boolean[] written = new boolean[0x10000];

  private MemoryImage(byte[] mem) {
    this.mem = mem;
  }

  /** load the cassette image and mark every address the game's writes and bulk copies touch. */
  public static MemoryImage of(AnalysisDB db) {
    byte[] mem = null;
    try {
      mem = Files.readAllBytes(Path.of(INIT_MEM));
    } catch (Exception ignored) {
    }
    MemoryImage img = new MemoryImage(mem);
    for (AnalysisDB.Stat w : db.writes.values())
      img.mark(w.addrMin(), w.addrMax());
    for (AnalysisDB.Bulk b : db.bulks.values())
      img.mark(b.dstMin(), b.dstMax() + Math.max(0, b.lenMax() - 1));
    return img;
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
