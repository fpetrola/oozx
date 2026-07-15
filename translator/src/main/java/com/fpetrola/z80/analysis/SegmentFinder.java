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

import java.util.*;

/**
 * The "segments" command: partitions the address space into memory segments by their
 * ACCESS-SET — the routines that touch each address — so we can see which memory is local
 * to a few routines (an encapsulation candidate) and which is shared. It is the foundation
 * of the memory-modularisation pipeline: a private segment can later be rebased to a
 * 0-indexed local array and turned into a class whose methods are its owning routines.
 *
 * <p>Ownership is by WRITER: you own what you write, so segments break where the writer-set
 * changes (a never-written run is owned by its readers instead — ROM/const data). A
 * sweep-line over every read/write range (and bulk copy src/dst) yields the raw partition,
 * then adjacent runs with the same owner-set merge.
 *
 * <p>The leaks section reports what BLOCKS clean encapsulation: bulk copies that pipe data
 * between two segments, and single instructions whose range straddles several segments
 * (wide clears and — from inlining — shared read helpers that see everything).
 */
public class SegmentFinder {
  /** a segment few enough distinct routines touch that it could become a private field. */
  private static final int PRIVATE_MAX = 4;
  /** a regular access wider than this owns nothing local — it is a sweep/shared helper that
   * only defines ownership where no bounded access does (a linear data table), and elsewhere
   * is reported as a cross-segment leak instead of polluting every segment it passes over. */
  private static final int WIDE = 512;

  private final AnalysisDB db;

  public SegmentFinder(AnalysisDB db) {
    this.db = db;
  }

  private String routine(int pc) {
    return db.method.getOrDefault(pc, "?");
  }

  private record Access(String routine, boolean write, int lo, int hi, boolean wide) {
    int span() {
      return hi - lo + 1;
    }
  }

  public record Segment(int lo, int hi, Set<String> writers, Set<String> readers) {
    public int size() {
      return hi - lo + 1;
    }

    public boolean readOnly() {
      return writers.isEmpty();
    }

    /** who defines the segment: its writers, or its readers when nothing writes it. */
    public Set<String> owners() {
      return writers.isEmpty() ? readers : writers;
    }

    public Set<String> all() {
      Set<String> s = new TreeSet<>(writers);
      s.addAll(readers);
      return s;
    }

    public boolean isPrivate() {
      return all().size() <= PRIVATE_MAX;
    }
  }

  /** every read/write, including a bulk copy's destination (write) and source (read). A
   * bulk is a coherent block move, so it always counts as bounded regardless of size. */
  private List<Access> accesses() {
    List<Access> out = new ArrayList<>();
    for (AnalysisDB.Stat w : db.writes.values())
      out.add(access(w.pc(), true, w.addrMin(), w.addrMax(), false));
    for (AnalysisDB.Stat r : db.reads.values())
      out.add(access(r.pc(), false, r.addrMin(), r.addrMax(), false));
    for (AnalysisDB.Bulk b : db.bulks.values()) {
      int extra = Math.max(0, b.lenMax() - 1);
      out.add(access(b.pc(), true, b.dstMin(), b.dstMax() + extra, true));
      out.add(access(b.pc(), false, b.srcMin(), b.srcMax() + extra, true));
    }
    return out;
  }

  private Access access(int pc, boolean write, int lo, int hi, boolean bulk) {
    boolean wide = !bulk && hi - lo + 1 > WIDE;
    return new Access(routine(pc), write, lo, hi, wide);
  }

  private record Ev(int at, int delta, boolean write, boolean wide, String routine) {
  }

  /** the merged segmentation of the whole address space, ordered by address. */
  public List<Segment> analyze() {
    List<Ev> evs = new ArrayList<>();
    for (Access a : accesses()) {
      evs.add(new Ev(a.lo(), +1, a.write(), a.wide(), a.routine()));
      evs.add(new Ev(a.hi() + 1, -1, a.write(), a.wide(), a.routine()));
    }
    TreeMap<Integer, List<Ev>> byAt = new TreeMap<>();
    for (Ev e : evs)
      byAt.computeIfAbsent(e.at(), k -> new ArrayList<>()).add(e);

    // bounded owners define a segment; a wide accessor owns only where no bounded one does
    Map<String, Integer> bw = new TreeMap<>(), br = new TreeMap<>(),
        ww = new TreeMap<>(), wr = new TreeMap<>();
    List<Segment> raw = new ArrayList<>();
    Integer prev = null;
    for (Map.Entry<Integer, List<Ev>> en : byAt.entrySet()) {
      int p = en.getKey();
      if (prev != null && p > prev) {
        Set<String> writers = positive(bw), readers = positive(br);
        if (writers.isEmpty() && readers.isEmpty()) {
          writers = positive(ww);
          readers = positive(wr);
        }
        if (!writers.isEmpty() || !readers.isEmpty())
          raw.add(new Segment(prev, p - 1, writers, readers));
      }
      for (Ev e : en.getValue())
        (e.write() ? (e.wide() ? ww : bw) : (e.wide() ? wr : br))
            .merge(e.routine(), e.delta(), Integer::sum);
      prev = p;
    }
    return mergeAdjacent(raw);
  }

  private Set<String> positive(Map<String, Integer> counts) {
    Set<String> s = new TreeSet<>();
    counts.forEach((k, v) -> {
      if (v > 0)
        s.add(k);
    });
    return s;
  }

  /** fuse contiguous runs that share the same owner-set into one segment. */
  private List<Segment> mergeAdjacent(List<Segment> raw) {
    List<Segment> out = new ArrayList<>();
    for (Segment s : raw) {
      if (!out.isEmpty()) {
        Segment last = out.get(out.size() - 1);
        if (last.hi() + 1 == s.lo() && last.owners().equals(s.owners())) {
          Set<String> w = new TreeSet<>(last.writers());
          w.addAll(s.writers());
          Set<String> r = new TreeSet<>(last.readers());
          r.addAll(s.readers());
          out.set(out.size() - 1, new Segment(last.lo(), s.hi(), w, r));
          continue;
        }
      }
      out.add(s);
    }
    return out;
  }

  // ---------- cross-segment links (what blocks encapsulation) ----------

  /** the segment covering an address, or null. */
  private Segment segAt(NavigableMap<Integer, Segment> idx, int addr) {
    Map.Entry<Integer, Segment> e = idx.floorEntry(addr);
    return e != null && addr <= e.getValue().hi() ? e.getValue() : null;
  }

  private List<Segment> spannedBy(List<Segment> segs, int lo, int hi) {
    List<Segment> r = new ArrayList<>();
    for (Segment s : segs)
      if (Ranges.intersects(s.lo(), s.hi(), lo, hi))
        r.add(s);
    return r;
  }

  // ---------- rendering ----------

  public void report() {
    List<Segment> segs = analyze();
    NavigableMap<Integer, Segment> idx = new TreeMap<>();
    for (Segment s : segs)
      idx.put(s.lo(), s);

    long priv = segs.stream().filter(Segment::isPrivate).count();
    long single = segs.stream().filter(s -> s.owners().size() == 1).count();
    System.out.printf("%n===== MEMORY SEGMENTATION: %d segments, %d private (<=%d routines), %d single-owner =====%n",
        segs.size(), priv, PRIVATE_MAX, single);
    for (Segment s : segs) {
      String owners = String.join(" ", s.owners());
      List<String> consumers = new ArrayList<>(s.readers());
      consumers.removeAll(s.writers());
      String tags = (s.isPrivate() ? " PRIVATE" : "")
          + (s.owners().size() == 1 ? " single-owner" : "");
      System.out.printf("  [%5d..%5d] %5dB  %-10s %-8s owner: %-24s%s%s%n",
          s.lo(), s.hi(), s.size(), s.readOnly() ? "read-only" : "read-write",
          tags.isBlank() ? "" : tags.strip(), owners,
          consumers.isEmpty() ? "" : "  read by: " + String.join(" ", consumers), "");
    }

    // bulk copies as pipes between segments
    System.out.printf("%n===== CROSS-SEGMENT LINKS (data pipes + leaks that block encapsulation) =====%n");
    for (AnalysisDB.Bulk b : db.bulks.values()) {
      int extra = Math.max(0, b.lenMax() - 1);
      Segment src = segAt(idx, b.srcMin()), dst = segAt(idx, b.dstMin());
      if (src != null && dst != null && (src.lo() != dst.lo())) {
        System.out.printf("  copy x%-6d [%d..%d] -> [%d..%d]  (%s: %s -> %s)%n",
            b.count(), b.srcMin(), b.srcMax() + extra, b.dstMin(), b.dstMax() + extra,
            routine(b.pc()), owner(src), owner(dst));
      }
    }

    // single instructions whose range straddles several segments
    Map<String, int[]> spanning = new TreeMap<>(); // routine -> {maxSegs, exampleLo, exampleHi}
    for (Access a : accesses()) {
      int n = spannedBy(segs, a.lo(), a.hi()).size();
      if (n >= 2) {
        int[] cur = spanning.get(a.routine());
        if (cur == null || n > cur[0])
          spanning.put(a.routine(), new int[]{n, a.lo(), a.hi()});
      }
    }
    System.out.println("  -- routines whose access range straddles several segments (shared helpers / movers):");
    spanning.entrySet().stream()
        .sorted((x, y) -> Integer.compare(y.getValue()[0], x.getValue()[0]))
        .forEach(e -> System.out.printf("     %-8s spans %2d segments  e.g. [%d..%d]%n",
            e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2]));
  }

  private String owner(Segment s) {
    return "[" + s.lo() + ".." + s.hi() + "]";
  }
}
