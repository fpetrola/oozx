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
import java.util.regex.Pattern;

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
  private static final Pattern WORD = Pattern.compile("[A-Za-z]{3}");

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

    // 1. glyph tables: a swept read whose BYTE is painted onto the screen as pixel data
    // (arrives at a screen write via VAL — an address table feeding the same write arrives
    // via ADDR and is rejected) and that holds a never-written static run (ROM/cassette
    // data). A sprite sheet also paints via VAL, so the candidate is kept below only when
    // it actually decodes into text.
    List<AnalysisDB.Stat> fonts = Sites.reads(db)
        .sweeping().spanningAtLeast(64)
        .where(f -> largestUnwrittenRun(f.addrMin(), f.addrMax()) != null)
        .where(f -> Flow.forward(db).from(f.pc()).depth(4)
            .reachesVia("VAL", this::writesToScreen))
        .list();

    List<int[]> acceptedRuns = new ArrayList<>();
    for (AnalysisDB.Stat font : fonts) {
      int[] glyphs = largestUnwrittenRun(font.addrMin(), font.addrMax());
      if (glyphs == null || overlapsAccepted(acceptedRuns, glyphs))
        continue;
      acceptedRuns.add(glyphs);
      // 2. character sources: the reads that build the glyph address, PLUS the font-reading
      // instruction itself — inlining can fold the message read and the glyph read onto one
      // shared site, so its RAM sub-ranges carry the strings while its ROM run holds glyphs.
      List<AnalysisDB.Stat> sources = new ArrayList<>(
          Flow.back(db).from(font.pc()).firstHop("ADDR").depth(5)
              .reads().sweeping()
              .where(s -> s.addrMin() != font.addrMin() || s.addrMax() != font.addrMax())
              .list());
      sources.add(font);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("font", Map.of(
          "range", List.of(glyphs[0], glyphs[1]),
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
        // 3. direct decoding of the never-written part of the span, OUTSIDE the glyph run
        // itself: the glyphs are the font (bitmaps that coincidentally read as ASCII), not
        // text. An address/sprite table whose whole span IS its glyph run thus yields no
        // strings here and drops out — only genuine message bytes survive.
        for (int[] seg : outside(decodedLo, decodedHi, glyphs[0], glyphs[1]))
          for (Map<String, Object> str : decodeRuns(seg[0], seg[1], true))
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
      // a real text font decodes into real text; a sprite sheet also painted via VAL yields
      // at most a few incidental fragments. Keep the candidate only with structured per-record
      // text or a meaningful density of strings (or when we could not decode at all).
      if (!recordTexts.isEmpty() || strings.size() >= 6 || memory == null)
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

  /** drop leading/trailing graphics fill (a single character repeated >=4 times, e.g. the
   * {@code UUUU} attribute bytes next to a room name) and surrounding whitespace. */
  private String trimGraphicsFill(String s) {
    int lo = 0, hi = s.length();
    while (lo < hi - 3 && s.charAt(lo) == s.charAt(lo + 1)
        && s.charAt(lo) == s.charAt(lo + 2) && s.charAt(lo) == s.charAt(lo + 3)) {
      char c = s.charAt(lo);
      while (lo < hi && s.charAt(lo) == c)
        lo++;
    }
    while (hi > lo + 3 && s.charAt(hi - 1) == s.charAt(hi - 2)
        && s.charAt(hi - 1) == s.charAt(hi - 3) && s.charAt(hi - 1) == s.charAt(hi - 4)) {
      char c = s.charAt(hi - 1);
      while (hi > lo && s.charAt(hi - 1) == c)
        hi--;
    }
    return s.substring(lo, hi).strip();
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
        String t = trimGraphicsFill((String) run.get("text"));
        // the richest-in-distinct-letters run is the name; a graphics fill bridged in by
        // spaces has few distinct letters even when long
        if (t.length() >= 3 && (best == null
            || t.chars().distinct().count() > best.chars().distinct().count()))
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

  /** true when {@code run} overlaps an already-accepted run by more than half of the smaller
   * — the same font read from sibling sites yields near-identical runs (off by a byte). */
  private boolean overlapsAccepted(List<int[]> accepted, int[] run) {
    for (int[] a : accepted) {
      int[] i = Ranges.intersection(a[0], a[1], run[0], run[1]);
      if (i == null)
        continue;
      int overlap = i[1] - i[0] + 1;
      int smaller = Math.min(a[1] - a[0] + 1, run[1] - run[0] + 1);
      if (overlap * 2 > smaller)
        return true;
    }
    return false;
  }

  /** the (up to two) sub-segments of [lo..hi] that lie outside [exLo..exHi]. */
  private List<int[]> outside(int lo, int hi, int exLo, int exHi) {
    List<int[]> out = new ArrayList<>();
    if (exLo > lo)
      out.add(new int[]{lo, Math.min(hi, exLo - 1)});
    if (exHi < hi)
      out.add(new int[]{Math.max(lo, exHi + 1), hi});
    if (exLo <= lo && exHi >= hi)
      return List.of(); // fully inside the glyph run: nothing to decode
    return out;
  }

  private void flushRun(List<Map<String, Object>> out, StringBuilder run, int start) {
    String text = run.toString().strip();
    // needs a real word: >=3 consecutive letters, >=3 distinct characters (drops repeated
    // fills like "EEEEGGGG"), and letters/spaces the majority — so glyph bitmaps and address
    // bytes that happen to fall in printable ASCII do not read as text.
    if (text.length() < 4 || !WORD.matcher(text).find()
        || text.chars().distinct().count() < 3)
      return;
    long letterSpace = text.chars().filter(c -> Character.isLetter(c) || c == ' ').count();
    if (letterSpace * 2 < text.length())
      return;
    Map<String, Object> s = new LinkedHashMap<>();
    s.put("address", start);
    s.put("text", text);
    out.add(s);
  }

  /**
   * The longest fully never-written sub-run inside [lo..hi], or null when none reaches 64
   * bytes. A glyph font is static reference data, but the reading instruction's aggregated
   * range often spans both the ROM/cassette font and the RAM it also touches — so the font
   * is the never-written island inside that range, not the whole span.
   */
  private int[] largestUnwrittenRun(int lo, int hi) {
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
    return bestLo >= 0 && bestHi - bestLo + 1 >= 64 ? new int[]{bestLo, bestHi} : null;
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
