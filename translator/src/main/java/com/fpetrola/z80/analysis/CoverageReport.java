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

import com.fpetrola.z80.analysis.query.Db;

import java.util.*;

/**
 * The "coverage" command: runs every detector against a capture and classifies what it
 * found — so pointing the pipeline at a NEW game turns "which detectors will fail?" from a
 * guess into a per-game diagnosis. Each detector is one of:
 * <ul>
 *   <li><b>FIRED</b> — produced a confident result;</li>
 *   <li><b>LOW-CONF</b> — fired but on weak evidence (the silent-wrong risk: uncertain
 *       coordinate transforms, fallback strides, heavily-shared memory);</li>
 *   <li><b>EMPTY</b> — its assumption was not met (safe: the detector simply did not fire);</li>
 *   <li><b>N/A</b> — needs the track side-tables, which have not been produced yet;</li>
 *   <li><b>ERROR</b> — it threw. On a new game a crash IS a finding (e.g. banked addresses),
 *       so every detector runs guarded and its failure is reported, not propagated.</li>
 * </ul>
 * Two foundation alarms flag the tricks that are not "just another case" but challenge the
 * flat-64K / static-instruction model: self-modifying code (writes into executed code) and
 * memory paging (128K) — the latter a known blind spot until OUT is instrumented.
 */
public class CoverageReport {
  private final AnalysisDB db;
  private final String dbPath;

  public CoverageReport(AnalysisDB db, String dbPath) {
    this.db = db;
    this.dbPath = dbPath;
  }

  private record Check(String name, String status, String detail) {
  }

  private static Check ok(String n, String d) {
    return new Check(n, "FIRED", d);
  }

  private static Check low(String n, String d) {
    return new Check(n, "LOW-CONF", d);
  }

  private static Check empty(String n, String d) {
    return new Check(n, "EMPTY", d);
  }

  private static Check na(String n, String d) {
    return new Check(n, "N/A", d);
  }

  /** run a detector guarded: any throw becomes an ERROR row instead of aborting the report. */
  private Check guard(String name, java.util.function.Supplier<Check> body) {
    try {
      return body.get();
    } catch (Throwable t) {
      String msg = t.getClass().getSimpleName() + (t.getMessage() != null ? ": " + t.getMessage() : "");
      return new Check(name, "ERROR", msg);
    }
  }

  @SuppressWarnings("unchecked")
  public void report() {
    try (Db q = new Db(dbPath)) {
      System.out.printf("%n===== COVERAGE REPORT (%s) =====%n", dbPath);
      System.out.printf("capture: %d sites, %d write-sites, %d read-sites, %d bulk copies, %d edges%n",
          db.method.size(), db.writes.size(), db.reads.size(), db.bulks.size(),
          db.edgesOut.values().stream().mapToInt(List::size).sum());

      List<Check> checks = new ArrayList<>();

      checks.add(guard("structures", () -> {
        StructFinder sf = new StructFinder(db, dbPath);
        List<Map<String, Object>> structs = sf.analyze(null);
        if (structs.isEmpty())
          return empty("structures", "no register-relative arrays found");
        long canon = sf.canonical(structs).size();
        long fallback = structs.stream().filter(s -> {
          String ss = (String) s.get("stride_source");
          return ss != null && !ss.startsWith("cursor advance");
        }).count();
        String d = structs.size() + " structs, " + canon + " canonical records";
        return fallback * 2 > structs.size()
            ? low("structures", d + " (" + fallback + " strides guessed, not from a cursor advance)")
            : ok("structures", d);
      }));

      checks.add(guard("entity-types", () -> {
        StructFinder sf = new StructFinder(db, dbPath);
        long types = sf.canonical(sf.analyze(null)).stream().filter(c -> c.containsKey("types")).count();
        return types > 0 ? ok("entity-types", types + " discriminated union(s)")
            : empty("entity-types", "no cp/flagZ type ladder found (jump-table dispatch is not detected)");
      }));

      checks.add(guard("reconstruction", () -> {
        RebuildFinder rf = new RebuildFinder(db, dbPath);
        int copies = rf.analyze().size();
        int drawing = rf.analyzeDrawing().size();
        if (copies + drawing == 0)
          return empty("reconstruction", "no selector + bulk-copy cluster nor selector-indexed"
              + " table walks (a decompressor is not detected as one)");
        String d = copies + " copy-based, " + drawing + " drawing-based selector rebuild(s)";
        return ok("reconstruction", d);
      }));

      checks.add(guard("singleton-records", () -> {
        int n = new RecordFinder(db, dbPath).analyze().size();
        return n > 0 ? ok("singleton-records", n + " rebuilt record(s)")
            : empty("singleton-records", "no reconstructed singleton block");
      }));

      checks.add(guard("text", () -> {
        List<Map<String, Object>> t = new TextFinder(db, dbPath).analyze();
        if (t.isEmpty())
          return empty("text", "no glyph font feeding the screen (compressed/non-ASCII text is not decoded)");
        int strings = t.stream().mapToInt(f -> ((List<?>) f.getOrDefault("strings", List.of())).size()
            + ((List<?>) f.getOrDefault("record_texts", List.of())).size()).sum();
        return strings > 0 ? ok("text", t.size() + " font(s), " + strings + " text field(s) decoded")
            : low("text", t.size() + " font(s) but nothing decoded as text");
      }));

      checks.add(guard("coordinates", () -> {
        if (!q.hasTable("coord_pairs"))
          return na("coordinates", "run `track` to correlate positions");
        long strong = q.scalar("SELECT COUNT(*) FROM coord_pairs WHERE rate >= 0.1", 0);
        long weak = q.scalar("SELECT COUNT(*) FROM coord_pairs WHERE rate < 0.1", 0);
        if (strong == 0 && weak == 0)
          return empty("coordinates", "no cell correlates with a drawn position (16-bit/scrolled coords?)");
        return strong > 0 ? ok("coordinates", strong + " strong pair(s), " + weak + " weak")
            : low("coordinates", "only " + weak + " weak pairs — transforms uncertain");
      }));

      checks.add(guard("sprite-tracking", () -> {
        if (!q.hasTable("sprite_draws"))
          return na("sprite-tracking", "run `track`");
        long draws = q.scalar("SELECT COUNT(*) FROM sprite_draws", 0);
        return draws > 0 ? ok("sprite-tracking", draws + " drawn sprites recorded")
            : empty("sprite-tracking", "no sprite draws clustered");
      }));

      checks.add(guard("variables", () -> {
        if (!q.hasTable("frame_cells"))
          return na("variables", "run `track`");
        long cells = q.scalar("SELECT COUNT(DISTINCT addr) FROM frame_cells", 0);
        return cells > 0 ? ok("variables", cells + " dynamic cells observed")
            : empty("variables", "no per-frame cells tracked");
      }));

      checks.add(guard("copy-pipelines", () -> db.bulks.isEmpty()
          ? empty("copy-pipelines", "no bulk (ldir-style) copies")
          : ok("copy-pipelines", db.bulks.size() + " bulk copy site(s)")));

      checks.add(guard("segments", () -> {
        Map<String, Object> seg = new SegmentFinder(db).data();
        long total = (long) (int) seg.get("segments_total");
        long priv = (long) seg.get("private");
        int spanners = ((List<?>) seg.get("spanners")).size();
        String d = total + " segments, " + priv + " private, " + spanners + " cross-segment routines";
        return priv * 5 < total * 2 // < 40% private = heavily-shared memory, hard to modularise
            ? low("segments", d + " (memory is heavily shared)")
            : ok("segments", d);
      }));

      int fired = 0, weak = 0;
      System.out.println("\nDETECTORS:");
      for (Check c : checks) {
        System.out.printf("  %-18s %-9s %s%n", c.name(), c.status(), c.detail());
        if (c.status().equals("FIRED"))
          fired++;
        else if (c.status().equals("LOW-CONF"))
          weak++;
      }

      System.out.println("\nFOUNDATION ALARMS (tricks that break the flat-64K / static-instruction model):");
      smc();
      paging();

      System.out.printf("%nSUMMARY: %d fired, %d low-confidence, %d empty/na/error of %d detectors%n",
          fired, weak, checks.size() - fired - weak, checks.size());
    }
  }

  /** writes whose target lands on an executed instruction address = self-modifying code. */
  private void smc() {
    int[] code = db.method.keySet().stream().mapToInt(Integer::intValue).sorted().toArray();
    int hits = 0, examples = 0;
    StringBuilder eg = new StringBuilder();
    for (AnalysisDB.Stat w : db.writes.values()) {
      int i = lowerBound(code, w.addrMin());
      if (i < code.length && code[i] <= w.addrMax()) {
        hits++;
        if (examples++ < 3)
          eg.append(" ").append(db.nameOf(w.pc())).append("@").append(w.pc())
              .append("->").append(code[i]);
      }
    }
    System.out.printf("  self-modifying code   %-9s %s%n",
        hits == 0 ? "CLEAN" : "PRESENT",
        hits == 0 ? "no write targets an executed instruction"
            : hits + " write-site(s) patch code (equations at those PCs are not stable):" + eg
                + "  [operand-only patches within an instruction may be missed]");
  }

  /** 128K paging is invisible in the flat-64K model until OUT is instrumented. */
  private void paging() {
    int maxAddr = 0;
    for (AnalysisDB.Stat s : db.writes.values())
      maxAddr = Math.max(maxAddr, s.addrMax());
    for (AnalysisDB.Stat s : db.reads.values())
      maxAddr = Math.max(maxAddr, s.addrMax());
    System.out.printf("  memory paging (128K)  %-9s replay is 64K-flat (max address %d, %d IO sites); "
            + "OUT to the paging port is not instrumented — a 128K game needs bank-aware capture%n",
        "BLIND", maxAddr, db.ioSites.size());
  }

  /** first index into the sorted array whose value is >= key. */
  private static int lowerBound(int[] a, int key) {
    int lo = 0, hi = a.length;
    while (lo < hi) {
      int mid = (lo + hi) >>> 1;
      if (a[mid] < key)
        lo = mid + 1;
      else
        hi = mid;
    }
    return lo;
  }
}
