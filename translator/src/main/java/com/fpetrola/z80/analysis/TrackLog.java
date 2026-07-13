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

package com.fpetrola.z80.analysis;

import java.util.Arrays;

/**
 * F5 (targeted): per-instance event log for the tracked re-run ("track" command).
 * Static and flat like Tracer; disabled by default so aggregate/baseline runs pay nothing.
 * <p>
 * A single ordered stream of packed-long events keeps draw writes, routine entries and
 * watched-cell changes in exact temporal order, so the offline replay can pair each
 * sprite draw with the coordinate-cell values in effect at that precise moment:
 * <pre>
 *   [type:2 bits 53..52][frame:20 bits 51..32][a:16 bits 31..16][b:16 bits 15..0]
 *   WRITE: a=site, b=addr     ENTRY: a=method entry pc
 *   CELL:  a=addr, b=value    FRAME: boundary marker
 * </pre>
 * Ordering contract with the replay: cell deltas are appended BEFORE an ENTRY marker
 * (the snapshot at entry must include them) and AFTER a FRAME marker (the previous
 * frame's clusters close before the new values apply).
 */
public final class TrackLog {
  public static final int EV_WRITE = 0, EV_ENTRY = 1, EV_CELL = 2, EV_FRAME = 3;

  public static boolean enabled;
  /** draw write-sites whose stores get logged per instance. */
  public static final boolean[] writeSites = new boolean[0x10000];
  /** entry pcs of the draw methods: mark one cluster per invocation. */
  public static final boolean[] entrySites = new boolean[0x10000];

  private static int[] watchCells = new int[0];
  private static int[] prevVals = new int[0];

  private static long[] log = new long[1 << 20];
  private static int logN;

  public static void reset() {
    enabled = false;
    Arrays.fill(writeSites, false);
    Arrays.fill(entrySites, false);
    watchCells = new int[0];
    prevVals = new int[0];
    log = new long[1 << 20];
    logN = 0;
  }

  public static void configure(int[] cells) {
    watchCells = cells.clone();
    prevVals = new int[cells.length];
    Arrays.fill(prevVals, -1); // first snapshot emits every cell
    enabled = true;
  }

  public static void write(int site, int addr) {
    append(EV_WRITE, site, addr);
  }

  public static void entry(int pc, int[] mem) {
    if (!enabled)
      return;
    cellDeltas(mem);
    append(EV_ENTRY, pc, 0);
  }

  public static void onFrame(int frame, int[] mem) {
    if (!enabled)
      return;
    append(EV_FRAME, 0, 0);
    cellDeltas(mem);
  }

  private static void cellDeltas(int[] mem) {
    for (int i = 0; i < watchCells.length; i++) {
      int v = mem[watchCells[i]] & 0xFF;
      if (v != prevVals[i]) {
        prevVals[i] = v;
        append(EV_CELL, watchCells[i], v);
      }
    }
  }

  private static void append(int type, int a, int b) {
    if (!enabled)
      return;
    if (logN == log.length)
      log = Arrays.copyOf(log, log.length * 2);
    log[logN++] = ((long) type << 52) | ((long) (Tracer.currentFrame & 0xFFFFF) << 32)
        | ((long) (a & 0xFFFF) << 16) | (b & 0xFFFF);
  }

  public static int size() {
    return logN;
  }

  public static long event(int i) {
    return log[i];
  }

  public static int type(long ev) {
    return (int) (ev >>> 52) & 3;
  }

  public static int frame(long ev) {
    return (int) (ev >>> 32) & 0xFFFFF;
  }

  public static int a(long ev) {
    return (int) (ev >>> 16) & 0xFFFF;
  }

  public static int b(long ev) {
    return (int) ev & 0xFFFF;
  }

  private TrackLog() {
  }
}
