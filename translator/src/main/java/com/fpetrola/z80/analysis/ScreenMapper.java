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

import com.fpetrola.z80.analysis.query.Ranges;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The "screen" command: recovers the mapping between screen memory and (x, y) pixel
 * coordinates, so the non-linear screen can be lifted to a plain rectangle. On the Spectrum
 * consecutive pixel rows are NOT consecutive in memory (thirds + interleaved scanlines), so
 * a game keeps a ROW-ADDRESS TABLE: {@code rowTable[y]} = the address of row y's first byte.
 * A draw is then {@code mem[rowTable[y] + (x>>3)]}.
 *
 * <p>Recovery is generic — no hardware layout is assumed. The row table is found as a
 * never-written run of 16-bit little-endian values that all land inside one screen/buffer
 * region and is read (as an address) by the routine drawing into that buffer. Its values
 * give the forward transform {@code T(x,y)} directly; inverting the base→y map gives the
 * anti-transform {@code T-1(addr) -> (x,y)} that turns the buffer into {@code rect[y][x]}.
 */
public class ScreenMapper {
  private static final String INIT_MEM = "analysis/init-mem.bin";
  private static final int MIN_ROWS = 16;

  private final AnalysisDB db;
  private final byte[] memory;
  private final boolean[] written = new boolean[0x10000];
  private final List<CoordinateFinder.Region> screenRegions;

  public ScreenMapper(AnalysisDB db) {
    this.db = db;
    this.screenRegions = new CoordinateFinder(db).find().regions();
    byte[] mem = null;
    try {
      mem = Files.readAllBytes(Path.of(INIT_MEM));
    } catch (Exception ignored) {
    }
    this.memory = mem;
    for (AnalysisDB.Stat w : db.writes.values())
      mark(w.addrMin(), w.addrMax());
    for (AnalysisDB.Bulk b : db.bulks.values())
      mark(b.dstMin(), b.dstMax() + Math.max(0, b.lenMax() - 1));
  }

  private void mark(int lo, int hi) {
    for (int a = Math.max(0, lo); a <= Math.min(0xffff, hi); a++)
      written[a] = true;
  }

  private int word(int a) {
    return (memory[a] & 255) | ((memory[a + 1] & 255) << 8);
  }

  /** a recovered row-address table and the screen rectangle it describes. */
  public record RowTable(int tableLo, int tableHi, int[] rows, int bufLo, int bufHi,
                         int width, List<String> readers) {
    public int height() {
      return rows.length;
    }

    public int pixelsWide() {
      return width * 8;
    }
  }

  /**
   * The longest never-written run of 16-bit LE values that all fall inside one screen region
   * — the row-address table. Width is the uniform gap between sorted row bases (bytes/row).
   */
  public RowTable detect() {
    if (memory == null)
      return null;
    RowTable best = null;
    for (CoordinateFinder.Region region : screenRegions) {
      int lo = region.lo(), hi = region.hi();
      int a = 0;
      while (a + 1 <= 0xffff) {
        if (written[a] || written[a + 1] || !inScreen(word(a), lo, hi)) {
          a += 2;
          continue;
        }
        int start = a;
        List<Integer> vals = new ArrayList<>();
        while (a + 1 <= 0xffff && !written[a] && !written[a + 1] && inScreen(word(a), lo, hi)) {
          vals.add(word(a));
          a += 2;
        }
        if (vals.size() >= MIN_ROWS && (best == null || vals.size() > best.rows.length)) {
          RowTable rt = build(start, a - 1, vals, lo, hi);
          if (rt != null)
            best = rt;
        }
      }
    }
    return best;
  }

  private boolean inScreen(int v, int lo, int hi) {
    return v >= lo && v <= hi;
  }

  private RowTable build(int tableLo, int tableHi, List<Integer> vals, int bufLo, int bufHi) {
    int[] rows = vals.stream().mapToInt(Integer::intValue).toArray();
    int[] sorted = rows.clone();
    Arrays.sort(sorted);
    int width = Integer.MAX_VALUE;
    for (int i = 1; i < sorted.length; i++)
      if (sorted[i] - sorted[i - 1] > 0)
        width = Math.min(width, sorted[i] - sorted[i - 1]);
    if (width == Integer.MAX_VALUE || width <= 0)
      return null;
    List<String> readers = readersOf(tableLo, tableHi);
    return new RowTable(tableLo, tableHi, rows, sorted[0], bufHi, width, readers);
  }

  /** routines that read an address in the table range (they consume it as a screen address). */
  private List<String> readersOf(int lo, int hi) {
    Set<String> out = new TreeSet<>();
    for (AnalysisDB.Stat r : db.reads.values())
      if (Ranges.intersects(r.addrMin(), r.addrMax(), lo, hi))
        out.add(db.method.getOrDefault(r.pc(), "?"));
    return new ArrayList<>(out);
  }

  // ---------- the transform and its inverse ----------

  /** T(x,y) = rowTable[y] + (x>>3): the address of pixel (x,y). */
  public int addressOf(RowTable rt, int x, int y) {
    return rt.rows()[y] + (x >> 3);
  }

  /** T-1: the (x,y) an address decodes to, or null if it is not inside a row. */
  public int[] coordsOf(RowTable rt, int addr) {
    Map<Integer, Integer> baseToY = new HashMap<>();
    for (int y = 0; y < rt.rows().length; y++)
      baseToY.putIfAbsent(rt.rows()[y], y);
    int base = addr - Math.floorMod(addr - rt.bufLo(), rt.width());
    Integer y = baseToY.get(base);
    if (y == null)
      return null;
    return new int[]{(addr - base) * 8, y};
  }

  // ---------- rendering ----------

  public void report() {
    if (memory == null) {
      System.out.println("no " + INIT_MEM + " (run RZXAnalysisRunner first)");
      return;
    }
    RowTable rt = detect();
    if (rt == null) {
      System.out.println("no row-address table found (screen addresses may be computed"
          + " arithmetically rather than looked up)");
      return;
    }
    System.out.printf("%n===== SCREEN GEOMETRY (recovered from the row-address table) =====%n");
    System.out.printf("  row table:     [%d..%d]  %d entries (16-bit LE), read by %s%n",
        rt.tableLo(), rt.tableHi(), rt.height(), String.join(" ", rt.readers()));
    System.out.printf("  target buffer: [%d..%d]  copied to the screen for display%n",
        rt.bufLo(), rt.bufHi());
    System.out.printf("  rectangle:     %d x %d pixels  (%d bytes/row, %d rows)%n",
        rt.pixelsWide(), rt.height(), rt.width(), rt.height());
    System.out.printf("  forward  T(x,y) = rowTable[y] + (x>>3)   (byte holds 8 px, bit = 7-(x&7))%n");
    System.out.printf("  rowTable[y] (the non-linear y->addr): %s ...%n",
        Arrays.toString(Arrays.copyOf(rt.rows(), Math.min(10, rt.height()))));

    System.out.println("  anti-transform T-1 (addr -> x,y), samples:");
    List<Integer> probes = List.of(rt.bufLo(), rt.bufLo() + 1,
        rt.rows()[Math.min(1, rt.height() - 1)], rt.rows()[Math.min(8, rt.height() - 1)], rt.bufHi());
    for (int p : new LinkedHashSet<>(probes)) {
      int[] c = coordsOf(rt, p);
      System.out.printf("     %5d -> %s%n", p, c == null ? "(outside rows)" : "(x=" + c[0] + ", y=" + c[1] + ")");
    }
    validate(rt);
  }

  /** decode the drawing routine's write extent through T-1: it should tile the rectangle. */
  private void validate(RowTable rt) {
    int xMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE, yMin = Integer.MAX_VALUE, yMax = Integer.MIN_VALUE;
    long hits = 0, total = 0;
    for (AnalysisDB.Stat w : db.writes.values()) {
      if (!Ranges.intersects(w.addrMin(), w.addrMax(), rt.bufLo(), rt.bufHi()))
        continue;
      for (int a : new int[]{w.addrMin(), w.addrMax()}) {
        total++;
        int[] c = coordsOf(rt, a);
        if (c == null)
          continue;
        hits++;
        xMin = Math.min(xMin, c[0]);
        xMax = Math.max(xMax, c[0]);
        yMin = Math.min(yMin, c[1]);
        yMax = Math.max(yMax, c[1]);
      }
    }
    if (hits == 0) {
      System.out.println("  validation: no buffer writes decoded (unexpected)");
      return;
    }
    System.out.printf("  validation: %d/%d buffer-write endpoints decode to the rectangle,"
            + " covering x[%d..%d] y[%d..%d] of %dx%d%n",
        hits, total, xMin, xMax, yMin, yMax, rt.pixelsWide(), rt.height());
  }
}
