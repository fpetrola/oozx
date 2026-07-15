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

import com.fpetrola.z80.analysis.query.Flow;
import com.fpetrola.z80.analysis.query.Ranges;
import com.fpetrola.z80.analysis.query.Sites;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The "texts" command: finds the game's TEXT — where the strings live and what they say
 * — from the printing pipeline's dataflow plus the initial memory image:
 * <ol>
 *   <li><b>glyph table (font)</b>: a swept read over a static mid-sized range whose
 *       VALUES feed screen writes and whose ADDRESS depends on other memory reads —
 *       the classic {@code fontBase + code*8} lookup;</li>
 *   <li><b>character sources</b>: the reads feeding that address (first hop ADDR):
 *       their swept spans are where the strings live. Their aggregated min/max mixes
 *       every message the routine ever printed, so the spans are decoded rather than
 *       split;</li>
 *   <li><b>decoding</b>: printable-ASCII runs inside the source spans, taken from the
 *       initial memory image ({@code analysis/init-mem.bin}) — only at addresses the
 *       game never writes (true cassette data);</li>
 *   <li><b>per-record projection</b>: a span portion inside a selector-rebuilt block is
 *       transient — its permanent home is the template catalogue, so the field is
 *       projected to {@code base + k*stride + offset} and decoded for EVERY record:
 *       that recovers e.g. the name of every screen.</li>
 * </ol>
 * Generic: no address, encoding table or message is assumed (ASCII printability is the
 * only alphabet heuristic).
 */
public class TextFinder {
  private static final String INIT_MEM = "analysis/init-mem.bin";

  private final AnalysisDB db;
  private final String dbPath;
  private final List<CoordinateFinder.Region> screenRegions;
  private final byte[] memory;
  private final boolean[] written = new boolean[0x10000];

  public TextFinder(AnalysisDB db, String dbPath) {
    this.db = db;
    this.dbPath = dbPath;
    this.screenRegions = new CoordinateFinder(db).find().regions();
    byte[] mem = null;
    try {
      mem = Files.readAllBytes(Path.of(INIT_MEM));
    } catch (Exception ignored) {
    }
    this.memory = mem;
    // addresses the game writes are not cassette data: exclude them from decoding
    for (AnalysisDB.Stat w : db.writes.values())
      mark(w.addrMin(), w.addrMax());
    for (AnalysisDB.Bulk b : db.bulks.values())
      mark(b.dstMin(), b.dstMax() + Math.max(0, b.lenMax() - 1));
  }

  private void mark(int lo, int hi) {
    for (int a = Math.max(0, lo); a <= Math.min(0xffff, hi); a++)
      written[a] = true;
  }

  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> analyze() {
    List<Map<String, Object>> rebuilds = new RebuildFinder(db, dbPath).analyze();
    List<Map<String, Object>> out = new ArrayList<>();

    // 1. glyph tables: static swept reads that feed the screen and are indexed by data
    List<AnalysisDB.Stat> fonts = Sites.reads(db)
        .sweeping().spanningAtLeast(64).spanningAtMost(8192)
        .where(f -> mostlyUnwritten(f.addrMin(), f.addrMax()))
        .where(f -> Flow.forward(db).from(f.pc()).firstHop("VAL").depth(3)
            .reaches(pc -> writesToScreen(pc)))
        .where(f -> !Flow.back(db).from(f.pc()).firstHop("ADDR").depth(5)
            .reads().sweeping().isEmpty())
        .list();

    Set<String> seenFonts = new HashSet<>();
    for (AnalysisDB.Stat font : fonts) {
      if (!seenFonts.add(font.addrMin() + ".." + font.addrMax()))
        continue;
      // 2. character sources: the reads that build the glyph address
      List<AnalysisDB.Stat> sources = Flow.back(db).from(font.pc()).firstHop("ADDR").depth(5)
          .reads().sweeping()
          .where(s -> s.addrMin() != font.addrMin() || s.addrMax() != font.addrMax())
          .list();
      if (sources.isEmpty())
        continue;
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("font", Map.of(
          "range", List.of(font.addrMin(), font.addrMax()),
          "read_by", db.method.getOrDefault(font.pc(), "?")));
      List<String> srcDesc = new ArrayList<>();
      List<Map<String, Object>> strings = new ArrayList<>();
      List<Map<String, Object>> recordTexts = new ArrayList<>();
      Set<Integer> decodedFrom = new HashSet<>();
      for (AnalysisDB.Stat s : sources) {
        srcDesc.add(db.method.getOrDefault(s.pc(), "?")
            + " [" + s.addrMin() + ".." + s.addrMax() + "]");
        // 4. the span portion inside a rebuilt block lives permanently in the catalogue
        int decodedLo = s.addrMin(), decodedHi = s.addrMax();
        for (Map<String, Object> block : rebuiltBlocks(rebuilds)) {
          List<Integer> dst = (List<Integer>) block.get("dst");
          int[] part = Ranges.intersection(s.addrMin(), s.addrMax(), dst.get(0), dst.get(1));
          if (part == null)
            continue;
          Map<String, Object> rt = decodePerRecord(block, part[0] - dst.get(0),
              part[1] - dst.get(0), db.method.getOrDefault(s.pc(), "?"));
          if (rt != null)
            recordTexts.add(rt);
        }
        // 3. direct decoding of the never-written part of the span
        for (Map<String, Object> str : decodeRuns(decodedLo, decodedHi, true))
          if (decodedFrom.add((Integer) str.get("address")))
            strings.add(str);
      }
      result.put("char_sources", srcDesc);
      if (memory == null)
        result.put("note", "run RZXAnalysisRunner to produce " + INIT_MEM
            + "; without it the strings cannot be decoded");
      if (!strings.isEmpty())
        result.put("strings", strings);
      if (!recordTexts.isEmpty())
        result.put("record_texts", recordTexts);
      out.add(result);
    }
    return out;
  }

  /** the selector-rebuilt destinations with their template geometry. */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> rebuiltBlocks(List<Map<String, Object>> rebuilds) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> f : rebuilds)
      for (Object co : (List<Object>) f.get("copies_indexed_by_selector")) {
        Map<String, Object> c = (Map<String, Object>) co;
        Map<String, Object> t = (Map<String, Object>) c.get("indexed_table");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("dst", c.get("destination"));
        b.put("base", ((Number) t.get("base")).intValue());
        b.put("stride", ((Number) t.get("record_bytes")).intValue());
        b.put("count", ((Number) t.get("used_records")).intValue());
        out.add(b);
      }
    return out;
  }

  /** decode the same field of every catalogue record; kept when enough records read as text. */
  private Map<String, Object> decodePerRecord(Map<String, Object> block, int offLo, int offHi,
                                              String printedBy) {
    if (memory == null)
      return null;
    int base = (int) block.get("base"), stride = (int) block.get("stride"),
        count = (int) block.get("count");
    List<Map<String, Object>> texts = new ArrayList<>();
    for (int k = 0; k < count; k++) {
      int lo = base + k * stride + offLo, hi = base + k * stride + offHi;
      String best = null;
      for (Map<String, Object> run : decodeRuns(lo, hi, false)) {
        String t = (String) run.get("text");
        if (best == null || t.length() > best.length())
          best = t;
      }
      if (best != null)
        texts.add(Map.of("record", k, "text", best));
    }
    if (texts.size() < Math.max(2, count / 3))
      return null; // not a text field: too few records decode as strings
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("template_base", base);
    out.put("record_bytes", stride);
    out.put("field_offset", List.of(offLo, offHi));
    out.put("printed_by", printedBy);
    out.put("texts", texts);
    return out;
  }

  /**
   * maximal printable-ASCII runs in [lo..hi] of the initial memory. A byte with bit 7
   * set whose low 7 bits are printable closes the run as its last character (the common
   * end-marker idiom). Runs need length >= 3, at least two distinct characters and one
   * letter or digit, so padding and graphics noise do not read as text.
   */
  private List<Map<String, Object>> decodeRuns(int lo, int hi, boolean requireUnwritten) {
    List<Map<String, Object>> out = new ArrayList<>();
    if (memory == null)
      return out;
    StringBuilder run = new StringBuilder();
    int start = -1;
    for (int a = Math.max(0, lo); a <= Math.min(0xffff, hi) + 1; a++) {
      int b = a <= hi ? memory[a] & 255 : 0;
      boolean usable = a <= hi && (!requireUnwritten || !written[a]);
      boolean printable = b >= 32 && b <= 126;
      boolean terminator = (b & 128) != 0 && (b & 127) >= 32 && (b & 127) <= 126;
      if (usable && (printable || terminator)) {
        if (run.isEmpty())
          start = a;
        run.append((char) (b & 127));
        if (!terminator)
          continue;
      }
      flushRun(out, run, start);
      run.setLength(0);
    }
    return out;
  }

  private void flushRun(List<Map<String, Object>> out, StringBuilder run, int start) {
    String text = run.toString().strip();
    if (text.length() < 3 || text.chars().distinct().count() < 3
        || text.chars().noneMatch(Character::isLetterOrDigit))
      return;
    Map<String, Object> s = new LinkedHashMap<>();
    s.put("address", start);
    s.put("text", text);
    out.add(s);
  }

  private boolean mostlyUnwritten(int lo, int hi) {
    int w = 0;
    for (int a = lo; a <= hi; a++)
      if (written[a])
        w++;
    return w * 5 <= (hi - lo + 1); // at most 20% touched
  }

  private boolean writesToScreen(int pc) {
    AnalysisDB.Stat w = db.writes.get(pc);
    if (w == null)
      return false;
    for (CoordinateFinder.Region r : screenRegions)
      if (Ranges.intersects(w.addrMin(), w.addrMax(), r.lo(), r.hi()))
        return true;
    return false;
  }

  // ---------- text rendering ----------
  @SuppressWarnings("unchecked")
  public void report() {
    List<Map<String, Object>> all = analyze();
    if (all.isEmpty()) {
      System.out.println("no glyph tables found (no static swept read feeding the screen"
          + " with a data-dependent address)");
      return;
    }
    for (Map<String, Object> f : all) {
      Map<String, Object> font = (Map<String, Object>) f.get("font");
      System.out.printf("%n===== FONT %s read by %s =====%n", font.get("range"), font.get("read_by"));
      System.out.println("  char sources: " + f.get("char_sources"));
      if (f.containsKey("note"))
        System.out.println("  NOTE: " + f.get("note"));
      if (f.containsKey("strings"))
        for (Map<String, Object> s : (List<Map<String, Object>>) f.get("strings"))
          System.out.printf("  @%d \"%s\"%n", (int) s.get("address"), s.get("text"));
      if (f.containsKey("record_texts"))
        for (Map<String, Object> rt : (List<Map<String, Object>>) f.get("record_texts")) {
          System.out.printf("  per-record field offset %s of the %d-byte records at %d (printed by %s):%n",
              rt.get("field_offset"), (int) rt.get("record_bytes"),
              (int) rt.get("template_base"), rt.get("printed_by"));
          for (Map<String, Object> t : (List<Map<String, Object>>) rt.get("texts"))
            System.out.printf("    record %2d: \"%s\"%n", (int) t.get("record"), t.get("text"));
        }
    }
  }
}
