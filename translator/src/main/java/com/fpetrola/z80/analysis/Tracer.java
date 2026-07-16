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
  public static final int[] bFirstFrame = new int[SIZE], bLastFrame = new int[SIZE];

  public static int currentPc = -1;
  public static int currentFrame = -1;

  // ================= F2: provenance =================
  // register provenance slots (last site that wrote each register)
  public static final int R_A = 0, R_F = 1, R_B = 2, R_C = 3, R_D = 4, R_E = 5, R_H = 6, R_L = 7,
      R_IXH = 8, R_IXL = 9, R_IYH = 10, R_IYL = 11, R_SP = 12,
      R_AX = 13, R_FX = 14, R_BX = 15, R_CX = 16, R_DX = 17, R_EX = 18, R_HX = 19, R_LX = 20,
      R_I = 21, R_R = 22, REG_SLOTS = 23;
  public static final int[] regProv = new int[REG_SLOTS];

  // channels: through which medium a dependency reached the consuming site.
  // 0..22 = the register slots above; MEM = value read from memory; STK = via pop.
  public static final int CH_MEM = REG_SLOTS, CH_STK = REG_SLOTS + 1, CHANNELS = REG_SLOTS + 2;
  public static final String[] CH_NAME = {
      "A", "F", "B", "C", "D", "E", "H", "L", "IXH", "IXL", "IYH", "IYL", "SP",
      "AX", "FX", "BX", "CX", "DX", "EX", "HX", "LX", "I", "R", "MEM", "STK"};

  /** sources (site, channel) read so far by the CURRENT Z80 instruction (one instruction = one site). */
  private static final int[] curSrc = new int[64];
  private static final int[] curCh = new int[64];
  private static int curSrcN;

  /** data-flow edges: (srcSite, dstSite, channel) with count. */
  public static final EdgeMap edges = new EdgeMap(1 << 16);
  /** dynamic CFG: instruction transitions prevPc -> nextPc with count. */
  public static final EdgeMap cfg = new EdgeMap(1 << 16);
  /** sites whose instruction reads the F register (conditional branches / flag users). */
  public static final boolean[] readsF = new boolean[SIZE];
  /** sites that read external input (RZX in()): roots for backward slicing. */
  public static final boolean[] ioSites = new boolean[SIZE];

  /** provenance travelling through the Z80 stack: one source-set snapshot per push. */
  private static final java.util.ArrayDeque<int[]> stackProv = new java.util.ArrayDeque<>();

  public static void src(int site, int ch) {
    if (site < 0)
      return;
    for (int i = 0; i < curSrcN; i++)
      if (curSrc[i] == site && curCh[i] == ch)
        return;
    if (curSrcN < curSrc.length) {
      curSrc[curSrcN] = site;
      curCh[curSrcN++] = ch;
    }
  }

  public static void regRead(int slot) {
    src(regProv[slot], slot);
    if (slot == R_F && currentPc >= 0)
      readsF[currentPc] = true;
  }

  public static void regRead2(int hi, int lo) {
    src(regProv[hi], hi);
    src(regProv[lo], lo);
  }

  public static void regWrite(int slot) {
    if (currentPc >= 0)
      regProv[slot] = currentPc;
  }

  public static void regWrite2(int hi, int lo) {
    if (currentPc >= 0) {
      regProv[hi] = currentPc;
      regProv[lo] = currentPc;
    }
  }

  public static void flagWrite() {
    regWrite(R_F);
  }

  public static void ioIn() {
    if (currentPc >= 0)
      ioSites[currentPc] = true;
  }

  public static void pushProv() {
    stackProv.push(java.util.Arrays.copyOf(curSrc, curSrcN));
  }

  public static void popProv() {
    int[] saved = stackProv.poll();
    if (saved != null)
      for (int s : saved)
        src(s, CH_STK);
  }

  /**
   * instruction boundary: flush data-flow edges of the finished instruction and record
   * the CFG transition. Called from pc(nextPc) BEFORE currentPc is updated.
   */
  public static void boundary(int nextPc) {
    int pc = currentPc;
    if (pc >= 0) {
      for (int i = 0; i < curSrcN; i++)
        if (curSrc[i] != pc)
          edges.increment(curSrc[i], pc, curCh[i]);
      cfg.increment(pc, nextPc);
    }
    curSrcN = 0;
  }
  // ================= end F2 =================

  static {
    reset();
  }

  public static void reset() {
    Arrays.fill(regProv, SITE_INIT);
    curSrcN = 0;
    edges.clear();
    cfg.clear();
    Arrays.fill(readsF, false);
    Arrays.fill(ioSites, false);
    stackProv.clear();
    resetF1();
  }

  private static void resetF1() {
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
    for (int[] a : new int[][]{wFirstFrame, rFirstFrame, wLastFrame, rLastFrame, bFirstFrame, bLastFrame})
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
    src(lastWriterMem[addr & 0xFFFF], CH_MEM);
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
    if (bFirstFrame[site] < 0) bFirstFrame[site] = currentFrame;
    bLastFrame[site] = currentFrame;
    // provenance of the copied block: sample the writers of the source range BEFORE
    // tagging the destination, so copy chains stay connected.
    if (src >= 0 && src < SIZE) {
      edges.increment(lastWriterMem[src], site, CH_MEM);
      int mid = src + len / 2, last = src + len - 1;
      if (mid < SIZE) edges.increment(lastWriterMem[mid], site, CH_MEM);
      if (last < SIZE) edges.increment(lastWriterMem[last], site, CH_MEM);
    }
    int end = Math.min(dst + len, SIZE);
    for (int a = Math.max(dst, 0); a < end; a++)
      lastWriterMem[a] = site;
  }

  /**
   * dump {@link #lastWriterMem} as 64K little-endian site ids. Per-site address ranges are only
   * an envelope — a routine that writes both the screen and a buffer claims everything in
   * between, static data included — while this says EXACTLY who wrote each address, at no extra
   * cost since the hot path maintains it anyway. {@code MemoryImage} masks with it.
   */
  public static void dumpWriterMap(String path) {
    byte[] out = new byte[SIZE * 2];
    for (int a = 0; a < SIZE; a++) {
      out[a * 2] = (byte) lastWriterMem[a];
      out[a * 2 + 1] = (byte) (lastWriterMem[a] >>> 8);
    }
    write(path, out);
  }

  public static void write(String path, byte[] bytes) {
    try {
      if (Path.of(path).getParent() != null)
        Files.createDirectories(Path.of(path).getParent());
      Files.write(Path.of(path), bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** dump all active sites as JSON for inspection (SQLite arrives in F3). */
  public static void dump(String path) {
    StringBuilder sb = new StringBuilder("{\n\"sites\": [\n");
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
      if (readsF[s])
        sb.append(", \"readsF\": true");
      if (ioSites[s])
        sb.append(", \"io\": true");
      sb.append('}');
    }
    sb.append("\n],\n");

    sb.append("\"edges\": [\n");
    StringBuilder eb = new StringBuilder();
    edges.forEach((src, dst, ch, count) -> {
      if (eb.length() > 0) eb.append(",\n");
      eb.append("  {\"src\": ").append(src).append(", \"dst\": ").append(dst)
          .append(", \"ch\": \"").append(CH_NAME[ch])
          .append("\", \"count\": ").append(count).append('}');
    });
    sb.append(eb).append("\n],\n");

    sb.append("\"cfg\": [\n");
    StringBuilder cb = new StringBuilder();
    cfg.forEach((src, dst, ch, count) -> {
      if (cb.length() > 0) cb.append(",\n");
      cb.append("  {\"src\": ").append(src).append(", \"dst\": ").append(dst)
          .append(", \"count\": ").append(count).append('}');
    });
    sb.append(cb).append("\n]\n}\n");
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
        + activeR + " read-sites (" + totalR + " ops), " + activeB + " bulk-sites, "
        + edges.size() + " data-flow edges, " + cfg.size() + " cfg edges";
  }

  private Tracer() {
  }
}