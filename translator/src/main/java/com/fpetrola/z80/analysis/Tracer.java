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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * F1 capture runtime (doc/GUIA-ANALISIS-ECUACIONES.md, section 4).
 * <p>
 * Site IDs are original Z80 instruction addresses (0..65535), so all per-site state
 * lives in flat 64K arrays: zero allocation on the hot path. Aggregates only —
 * no per-instance logging.
 */
public final class Tracer {
  public static final int SITE_INIT = 0;
  private static final int SIZE = 0x10000;

  /** site that last wrote each memory address (SITE_INIT = initial snapshot data). */
  public static final int[] lastWriterMem = new int[SIZE];

  // --- per-site write stats (indexed by z80 pc of the writing instruction) ---
  public static final long[] wCount = new long[SIZE];
  public static final int[] wAddrMin = new int[SIZE], wAddrMax = new int[SIZE];
  public static final int[] wValMin = new int[SIZE], wValMax = new int[SIZE];
  public static final int[] wAddrOr = new int[SIZE], wAddrAnd = new int[SIZE];
  public static final int[] wValOr = new int[SIZE], wValAnd = new int[SIZE];
  public static final int[] wFirstFrame = new int[SIZE], wLastFrame = new int[SIZE];

  // --- per-site read stats ---
  public static final long[] rCount = new long[SIZE];
  public static final int[] rAddrMin = new int[SIZE], rAddrMax = new int[SIZE];
  public static final int[] rValMin = new int[SIZE], rValMax = new int[SIZE];
  public static final int[] rAddrOr = new int[SIZE], rAddrAnd = new int[SIZE];
  public static final int[] rValOr = new int[SIZE], rValAnd = new int[SIZE];
  public static final int[] rFirstFrame = new int[SIZE], rLastFrame = new int[SIZE];

  // --- per-site bulk copy (ldir) stats ---
  public static final long[] bCount = new long[SIZE];
  public static final int[] bSrcMin = new int[SIZE], bSrcMax = new int[SIZE];
  public static final int[] bDstMin = new int[SIZE], bDstMax = new int[SIZE];
  public static final int[] bLenMin = new int[SIZE], bLenMax = new int[SIZE];

  public static int currentPc = -1;
  public static int currentFrame = -1;

  static {
    reset();
  }

  public static void reset() {
    Arrays.fill(lastWriterMem, SITE_INIT);
    Arrays.fill(wCount, 0);
    Arrays.fill(rCount, 0);
    Arrays.fill(bCount, 0);
    for (int[] a : new int[][]{wAddrMin, wValMin, rAddrMin, rValMin, bSrcMin, bDstMin, bLenMin})
      Arrays.fill(a, Integer.MAX_VALUE);
    for (int[] a : new int[][]{wAddrMax, wValMax, rAddrMax, rValMax, bSrcMax, bDstMax, bLenMax})
      Arrays.fill(a, Integer.MIN_VALUE);
    for (int[] a : new int[][]{wAddrOr, wValOr, rAddrOr, rValOr})
      Arrays.fill(a, 0);
    for (int[] a : new int[][]{wAddrAnd, wValAnd, rAddrAnd, rValAnd})
      Arrays.fill(a, -1);
    for (int[] a : new int[][]{wFirstFrame, rFirstFrame, wLastFrame, rLastFrame})
      Arrays.fill(a, -1);
  }

  /** memory made consistent with the loaded snapshot: everything belongs to SITE_INIT again. */
  public static void initDone() {
    Arrays.fill(lastWriterMem, SITE_INIT);
  }

  public static void wr(int site, int addr, int val) {
    wCount[site]++;
    if (addr < wAddrMin[site]) wAddrMin[site] = addr;
    if (addr > wAddrMax[site]) wAddrMax[site] = addr;
    if (val < wValMin[site]) wValMin[site] = val;
    if (val > wValMax[site]) wValMax[site] = val;
    wAddrOr[site] |= addr;
    wAddrAnd[site] &= addr;
    wValOr[site] |= val;
    wValAnd[site] &= val;
    if (wFirstFrame[site] < 0) wFirstFrame[site] = currentFrame;
    wLastFrame[site] = currentFrame;
    lastWriterMem[addr & 0xFFFF] = site;
  }

  public static void rd(int site, int addr, int val) {
    rCount[site]++;
    if (addr < rAddrMin[site]) rAddrMin[site] = addr;
    if (addr > rAddrMax[site]) rAddrMax[site] = addr;
    if (val < rValMin[site]) rValMin[site] = val;
    if (val > rValMax[site]) rValMax[site] = val;
    rAddrOr[site] |= addr;
    rAddrAnd[site] &= addr;
    rValOr[site] |= val;
    rValAnd[site] &= val;
    if (rFirstFrame[site] < 0) rFirstFrame[site] = currentFrame;
    rLastFrame[site] = currentFrame;
  }

  public static void bulk(int site, int src, int dst, int len) {
    if (site < 0 || len <= 0)
      return;
    bCount[site]++;
    if (src < bSrcMin[site]) bSrcMin[site] = src;
    if (src > bSrcMax[site]) bSrcMax[site] = src;
    if (dst < bDstMin[site]) bDstMin[site] = dst;
    if (dst > bDstMax[site]) bDstMax[site] = dst;
    if (len < bLenMin[site]) bLenMin[site] = len;
    if (len > bLenMax[site]) bLenMax[site] = len;
    int end = Math.min(dst + len, SIZE);
    for (int a = Math.max(dst, 0); a < end; a++)
      lastWriterMem[a] = site;
  }

  /** dump all active sites as JSON for inspection (SQLite arrives in F3). */
  public static void dump(String path) {
    StringBuilder sb = new StringBuilder("[\n");
    boolean first = true;
    for (int s = 0; s < SIZE; s++) {
      if (wCount[s] == 0 && rCount[s] == 0 && bCount[s] == 0)
        continue;
      if (!first)
        sb.append(",\n");
      first = false;
      sb.append("  {\"pc\": ").append(s);
      if (wCount[s] > 0)
        sb.append(", \"w\": {\"count\": ").append(wCount[s])
            .append(", \"addr\": [").append(wAddrMin[s]).append(", ").append(wAddrMax[s]).append(']')
            .append(", \"val\": [").append(wValMin[s]).append(", ").append(wValMax[s]).append(']')
            .append(", \"addrBits\": [").append(wAddrAnd[s]).append(", ").append(wAddrOr[s]).append(']')
            .append(", \"valBits\": [").append(wValAnd[s]).append(", ").append(wValOr[s]).append(']')
            .append(", \"frames\": [").append(wFirstFrame[s]).append(", ").append(wLastFrame[s]).append("]}");
      if (rCount[s] > 0)
        sb.append(", \"r\": {\"count\": ").append(rCount[s])
            .append(", \"addr\": [").append(rAddrMin[s]).append(", ").append(rAddrMax[s]).append(']')
            .append(", \"val\": [").append(rValMin[s]).append(", ").append(rValMax[s]).append(']')
            .append(", \"addrBits\": [").append(rAddrAnd[s]).append(", ").append(rAddrOr[s]).append(']')
            .append(", \"valBits\": [").append(rValAnd[s]).append(", ").append(rValOr[s]).append(']')
            .append(", \"frames\": [").append(rFirstFrame[s]).append(", ").append(rLastFrame[s]).append("]}");
      if (bCount[s] > 0)
        sb.append(", \"bulk\": {\"count\": ").append(bCount[s])
            .append(", \"src\": [").append(bSrcMin[s]).append(", ").append(bSrcMax[s]).append(']')
            .append(", \"dst\": [").append(bDstMin[s]).append(", ").append(bDstMax[s]).append(']')
            .append(", \"len\": [").append(bLenMin[s]).append(", ").append(bLenMax[s]).append("]}");
      sb.append('}');
    }
    sb.append("\n]\n");
    try {
      if (Path.of(path).getParent() != null)
        Files.createDirectories(Path.of(path).getParent());
      Files.writeString(Path.of(path), sb.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String summary() {
    int activeW = 0, activeR = 0, activeB = 0;
    long totalW = 0, totalR = 0;
    for (int s = 0; s < SIZE; s++) {
      if (wCount[s] > 0) { activeW++; totalW += wCount[s]; }
      if (rCount[s] > 0) { activeR++; totalR += rCount[s]; }
      if (bCount[s] > 0) activeB++;
    }
    return "Tracer: " + activeW + " write-sites (" + totalW + " ops), "
        + activeR + " read-sites (" + totalR + " ops), " + activeB + " bulk-sites";
  }

  private Tracer() {
  }
}