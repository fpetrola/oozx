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

import com.fpetrola.z80.analysis.query.Closure;
import com.fpetrola.z80.analysis.query.Db;
import com.fpetrola.z80.analysis.query.Eq;
import com.fpetrola.z80.analysis.query.Ranges;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The "structs" command: recovers the record/array structures a routine manipulates,
 * from evidence alone:
 * <ul>
 *   <li><b>fields</b>: the {@code mem[IX + k]} offsets parsed from the site equations,
 *       each with its observed R/W value ranges;</li>
 *   <li><b>array geometry</b>: base = min(observed address - offset); stride = lowest
 *       varying address bit (a cursor stepping 8 bytes flips bit 3 first); element
 *       count from the swept span — cross-checkable with the {@code add16(IX, n)}
 *       increment when it parses;</li>
 *   <li><b>field semantics</b>: a field whose out-edges carry COND is a type/flag
 *       (drives branches); ADDR toward the graphics zones is a sprite selector; ADDR
 *       elsewhere is a pointer/index; coordinate fields come annotated from the
 *       validated track pairs;</li>
 *   <li><b>variants</b>: for each branch whose condition traces back to a field, the
 *       two CFG arms are walked and the fields touched EXCLUSIVELY by each arm are the
 *       per-variant layout (the "vertical vs horizontal guardian" structures).</li>
 * </ul>
 * {@link #analyze(String)} returns the structures as data (consumed by the JSON
 * export); {@link #report(String)} prints them.
 */
public class StructFinder {
  private static final Pattern FIELD = Pattern.compile("mem\\[(I[XY]) \\+ (\\d+)\\]");
  private static final Pattern CMP = Pattern.compile("cp\\([A-Z], (\\d+)\\)");
  private static final Pattern MASK = Pattern.compile("[A-Z] = [A-Z] & (\\d+)");

  private final AnalysisDB db;
  private final String dbPath;
  private final Map<Integer, Character> coordAxis = new HashMap<>();
  private final List<int[]> gfxRegions;
  private final List<CoordinateFinder.Region> screenRegions;
  /** raw sites/accesses behind each struct map, for the type splitter (not serialized). */
  private final Map<Map<String, Object>, Raw> raws = new IdentityHashMap<>();

  public StructFinder(AnalysisDB db, String dbPath) {
    this.db = db;
    this.dbPath = dbPath;
    Explainer explainer = new Explainer(db, dbPath);
    // coordinates only from the STRONG validated pairs: single-axis matches carry noise
    try (Db q = new Db(dbPath)) {
      for (long[] r : q.rows("SELECT x_addr, y_addr FROM coord_pairs WHERE rate >= 0.3")) {
        coordAxis.putIfAbsent((int) r[0], 'X');
        coordAxis.putIfAbsent((int) r[1], 'Y');
      }
    }
    CoordinateFinder.Plan plan = new CoordinateFinder(db).find();
    this.screenRegions = plan.regions();
    Set<Integer> valReads = GameMapper.roleReads(db, plan, "VAL");
    this.gfxRegions = GameMapper.mergeRanges(valReads.stream()
        .map(db.reads::get)
        .filter(r -> {
          String c = explainer.classifyRange(r.addrMin(), r.addrMax());
          return c.startsWith("STATIC") || c.startsWith("mostly") || c.startsWith("MIXED");
        })
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 64)
        .stream().filter(g -> g[1] - g[0] + 1 >= 1024).toList();
  }

  /** all recovered structures as data, ready for the JSON export. */
  public List<Map<String, Object>> analyze(String methodFilter) {
    Map<String, List<Integer>> byMethod = new TreeMap<>();
    db.method.forEach((pc, m) -> byMethod.computeIfAbsent(m, k -> new ArrayList<>()).add(pc));
    List<Map<String, Object>> out = new ArrayList<>();
    List<int[]> expansions = new ArrayList<>();
    for (Map.Entry<String, List<Integer>> me : byMethod.entrySet()) {
      if (methodFilter != null && !me.getKey().contains(methodFilter))
        continue;
      out.addAll(structsOfMethod(me.getKey(), me.getValue()));
      expansions.addAll(findExpansions(me.getValue()));
    }
    linkExpansions(out, expansions);
    return out;
  }

  /**
   * Template -> instance expansion loops: a compact source table is unpacked into a wider
   * working buffer, one record per iteration. The pattern is generic -- an indexed source
   * cursor set to a constant base, a destination base, a per-iteration {@code ldir} copy,
   * and the source cursor advancing -- and covers the "8 entity specs (2 bytes) expand into
   * the 8 entity buffers (8 bytes)" shape without any game-specific knowledge.
   */
  private List<int[]> findExpansions(List<Integer> sites) {
    List<Integer> ord = new ArrayList<>(sites);
    Collections.sort(ord);
    List<int[]> res = new ArrayList<>();
    for (int i = 0; i < ord.size(); i++) {
      String eq = db.equation.get(ord.get(i));
      if (eq == null)
        continue;
      Matcher m = Pattern.compile("^(I[XY]) = (\\d{4,5})\\b").matcher(eq);
      if (!m.find())
        continue;
      String srcReg = m.group(1);
      int src = Integer.parseInt(m.group(2));
      Integer dst = null;
      boolean ldir = false;
      for (int j = i + 1; j < Math.min(i + 40, ord.size()); j++) {
        String e2 = db.equation.get(ord.get(j));
        if (e2 == null)
          continue;
        if (e2.matches("^" + srcReg + " = (inc16|add16)\\(" + srcReg + ".*")) {
          if (dst != null && ldir && dst != src) // the source cursor advances = loop body ends
            res.add(new int[]{src, dst});
          break;
        }
        Matcher md = Pattern.compile("^(DE|HL) = (\\d{4,5})\\b").matcher(e2);
        if (dst == null && md.find())
          dst = Integer.parseInt(md.group(2));
        if (e2.startsWith("ldir"))
          ldir = true;
      }
    }
    return res;
  }

  /** annotate the source struct with {@code expands_to} and the destination with {@code filled_from}. */
  private void linkExpansions(List<Map<String, Object>> structs, List<int[]> expansions) {
    for (int[] ex : expansions) {
      for (Map<String, Object> st : structs) {
        int base = (int) st.get("base");
        if (base == ex[0])
          st.putIfAbsent("expands_to", ex[1]);
        if (base == ex[1])
          st.putIfAbsent("filled_from", ex[0]);
      }
    }
  }

  public void report(String methodFilter) {
    List<Map<String, Object>> structs = analyze(methodFilter);
    for (Map<String, Object> st : structs)
      print(st);
    for (Map<String, Object> rec : canonical(structs))
      printCanonical(rec);
  }

  /**
   * A single canonical record per {@code (base, stride)}, built by UNIONING the partial
   * views that each routine exposes. No routine sees the whole record: the mover reads the
   * position, increment and bounds; the drawer reads the graphics pointer; the initialiser
   * writes the original values. Merging them — exactly as a human disassembler assembles
   * one record definition from several routines — recovers every byte with its best name.
   * Only emitted when at least two routines contribute, since that is what union adds.
   */
  @SuppressWarnings("unchecked")
  public List<Map<String, Object>> canonical(List<Map<String, Object>> structs) {
    Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
    for (Map<String, Object> st : structs)
      groups.computeIfAbsent(st.get("base") + "|" + st.get("record_bytes"), k -> new ArrayList<>())
          .add(st);
    List<Map<String, Object>> out = new ArrayList<>();
    for (List<Map<String, Object>> g : groups.values()) {
      long rutinas = g.stream().map(s -> s.get("routine")).distinct().count();
      if (rutinas < 2)
        continue;
      out.add(mergeGroup(g));
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mergeGroup(List<Map<String, Object>> g) {
    Map<String, Object> first = g.get(0);
    int stride = (int) first.get("record_bytes");
    int base = (int) first.get("base");
    int tableEnd = base;
    for (Map<String, Object> st : g)
      tableEnd = Math.max(tableEnd, ((List<Integer>) st.get("range")).get(1));

    // fold every routine's field at offset (off % stride) into one canonical field
    Map<Integer, Map<String, Object>> byOff = new TreeMap<>();
    Map<Integer, List<String>> aportes = new TreeMap<>();
    for (Map<String, Object> st : g) {
      String rutina = (String) st.get("routine");
      for (Object fo : (List<Object>) st.get("fields")) {
        Map<String, Object> f = (Map<String, Object>) fo;
        int off = ((int) f.get("offset")) % stride;
        Map<String, Object> c = byOff.computeIfAbsent(off, k -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("offset", k);
          m.put("reads", 0L);
          m.put("writes", 0L);
          m.put("values", new int[]{256, -1});
          m.put("semantics", new LinkedHashSet<String>());
          m.put("bits", new LinkedHashSet<String>());
          m.put("proposed_name", (String) null);
          return m;
        });
        c.put("reads", (long) c.get("reads") + (long) f.get("reads"));
        c.put("writes", (long) c.get("writes") + (long) f.get("writes"));
        int[] vr = (int[]) c.get("values");
        List<Integer> fv = (List<Integer>) f.get("values");
        vr[0] = Math.min(vr[0], fv.get(0));
        vr[1] = Math.max(vr[1], fv.get(1));
        if (f.containsKey("semantics"))
          ((Set<String>) c.get("semantics")).addAll((List<String>) f.get("semantics"));
        if (f.containsKey("bit_decomposition"))
          ((Set<String>) c.get("bits")).addAll((List<String>) f.get("bit_decomposition"));
        if (f.containsKey("points_to_graphics"))
          c.putIfAbsent("points_to_graphics", f.get("points_to_graphics"));
        String cand = (String) f.get("proposed_name");
        if (nameScore(cand) > nameScore((String) c.get("proposed_name")))
          c.put("proposed_name", cand);
        if (cand != null)
          aportes.computeIfAbsent(off, k -> new ArrayList<>()).add(rutina + ": " + cand);
      }
    }

    List<Object> campos = new ArrayList<>();
    for (Map<String, Object> c : byOff.values()) {
      int[] vr = (int[]) c.get("values");
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("offset", c.get("offset"));
      out.put("reads", c.get("reads"));
      out.put("writes", c.get("writes"));
      out.put("values", List.of(vr[0] <= vr[1] ? vr[0] : 0, vr[1] >= 0 ? vr[1] : 0));
      Set<String> sem = (Set<String>) c.get("semantics");
      if (!sem.isEmpty())
        out.put("semantics", new ArrayList<>(sem));
      Set<String> bits = (Set<String>) c.get("bits");
      if (!bits.isEmpty())
        out.put("bit_decomposition", new ArrayList<>(bits));
      if (c.containsKey("points_to_graphics"))
        out.put("points_to_graphics", c.get("points_to_graphics"));
      if (c.get("proposed_name") != null)
        out.put("proposed_name", c.get("proposed_name"));
      List<String> ap = aportes.get((int) c.get("offset"));
      if (ap != null)
        out.put("contributed_by", ap);
      campos.add(out);
    }

    Map<String, Object> rec = new LinkedHashMap<>();
    rec.put("base", base);
    rec.put("record_bytes", stride);
    rec.put("range", List.of(base, tableEnd));
    rec.put("routines", g.stream().map(s -> (String) s.get("routine")).distinct().sorted().toList());
    for (Map<String, Object> st : g) {
      if (st.containsKey("terminator"))
        rec.putIfAbsent("terminator", st.get("terminator"));
      if (st.containsKey("expands_to"))
        rec.putIfAbsent("expands_to", st.get("expands_to"));
      if (st.containsKey("filled_from"))
        rec.putIfAbsent("filled_from", st.get("filled_from"));
    }
    rec.put("fields", campos);

    // discriminated union: when the routines test one field against several constants,
    // split the record into its REAL types and re-analyse the fields inside each one
    Integer termValor = rec.containsKey("terminator")
        ? ((Number) ((Map<String, Object>) rec.get("terminator")).get("value")).intValue() : null;
    List<Raw> groupRaws = g.stream().map(raws::get).filter(Objects::nonNull).toList();
    Map<String, Object> tipos = new TypeSplitter(this, db, dbPath)
        .discriminate(base, stride, tableEnd, termValor, groupRaws);
    if (tipos != null)
      rec.putAll(tipos);
    return rec;
  }

  /** how specific/trustworthy a proposed name is, so union keeps the most informative one. */
  private int nameScore(String n) {
    if (n == null)
      return 0;
    if (n.startsWith("position_") || n.startsWith("limit_") || n.startsWith("increment_"))
      return 5;
    if (n.startsWith("animation_frame") || n.startsWith("direction_flag")
        || n.startsWith("type_and_direction"))
      return 4;
    if (n.startsWith("graphic_frame") || n.equals("color"))
      return 3;
    if (n.startsWith("limite") || n.startsWith("counter"))
      return 2;
    return 1; // type_flags, controls_conditions — generic fallbacks
  }

  record FieldAccess(int site, char op, int offset) {
  }

  /** the evidence behind one struct view: the routine's sites and its field accesses. */
  record Raw(String method, List<Integer> sites, List<FieldAccess> accesses) {
  }

  private List<Map<String, Object>> structsOfMethod(String method, List<Integer> sites) {
    Map<String, List<FieldAccess>> byReg = new TreeMap<>();
    for (int pc : sites) {
      String eq = db.equation.get(pc);
      if (eq == null)
        continue;
      Matcher m = FIELD.matcher(eq);
      while (m.find()) {
        String reg = m.group(1);
        int off = Integer.parseInt(m.group(2));
        boolean isWrite = eq.startsWith("mem[" + reg + " + " + off + "] =");
        if (isWrite && db.writes.containsKey(pc))
          byReg.computeIfAbsent(reg, k -> new ArrayList<>()).add(new FieldAccess(pc, 'W', off));
        if (db.reads.containsKey(pc))
          byReg.computeIfAbsent(reg, k -> new ArrayList<>()).add(new FieldAccess(pc, 'R', off));
      }
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map.Entry<String, List<FieldAccess>> re : byReg.entrySet()) {
      // one register can be re-pointed at several tables inside the same routine (a main
      // loop reuses IX for the entity spec, the clock, the input map...). Segment the
      // accesses by the base each one implies so we recover separate arrays instead of one
      // bogus 636-element span from min-to-max.
      for (List<FieldAccess> cluster : segmentByBase(re.getValue())) {
        if (cluster.stream().mapToInt(FieldAccess::offset).distinct().count() < 2)
          continue; // one lone field is not a structure
        Map<String, Object> st = buildStruct(method, sites, re.getKey(), cluster);
        if (st != null)
          out.add(st);
      }
    }
    return out;
  }

  private static final int BASE_GAP = 48;

  /**
   * Split a register's accesses into the distinct arrays it walks. Each access anchors to
   * the base it implies (its lowest observed address minus its offset); accesses whose
   * anchors sit far apart belong to different tables. Generic to any register reused for
   * several structures.
   */
  private List<List<FieldAccess>> segmentByBase(List<FieldAccess> accesses) {
    List<FieldAccess> sorted = new ArrayList<>();
    for (FieldAccess fa : accesses)
      if (anchorOf(fa) != Integer.MIN_VALUE)
        sorted.add(fa);
    sorted.sort(Comparator.comparingInt(this::anchorOf));
    List<List<FieldAccess>> clusters = new ArrayList<>();
    List<FieldAccess> cur = new ArrayList<>();
    int prev = Integer.MIN_VALUE;
    for (FieldAccess fa : sorted) {
      int a = anchorOf(fa);
      if (!cur.isEmpty() && a - prev > BASE_GAP) {
        clusters.add(cur);
        cur = new ArrayList<>();
      }
      cur.add(fa);
      prev = a;
    }
    if (!cur.isEmpty())
      clusters.add(cur);
    return clusters;
  }

  /** the array base an access implies: its lowest touched address minus its field offset. */
  private int anchorOf(FieldAccess fa) {
    AnalysisDB.Stat s = (fa.op() == 'W' ? db.writes : db.reads).get(fa.site());
    return s == null ? Integer.MIN_VALUE : s.addrMin() - fa.offset();
  }

  /**
   * Sentinel that ends a null-terminated array: the leading field is read and compared
   * against a constant sitting at the value extreme (0, 255 or the field's own max), and
   * that test stops the sweep. Generic to any {@code while (mem[cursor] != MARK)} loop.
   */
  private Map<String, Object> detectTerminator(List<Integer> sites, String reg, int minOff,
                                               List<FieldAccess> leadAccesses) {
    int vMax = -1;
    if (leadAccesses != null)
      for (FieldAccess fa : leadAccesses) {
        AnalysisDB.Stat s = (fa.op() == 'W' ? db.writes : db.reads).get(fa.site());
        if (s != null)
          vMax = Math.max(vMax, s.valMax());
      }
    for (int pc : sites) {
      OptionalInt cc = Eq.cmpConst(db, pc);
      if (cc.isEmpty())
        continue;
      int c = cc.getAsInt();
      if (c != 0 && c != 255 && c != vMax) // a sentinel sits at the value extreme
        continue;
      if (tracesToFieldRead(pc, reg, minOff, 3, new HashSet<>())) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("value", c);
        t.put("site", pc);
        t.put("note", "the last slot is an end marker, not a record");
        return t;
      }
    }
    return null;
  }

  /** does site {@code pc} consume, within a few hops, a read of {@code mem[reg + off]}? */
  private boolean tracesToFieldRead(int pc, String reg, int off, int depth, Set<Integer> seen) {
    if (depth < 0)
      return false;
    String eq = db.equation.get(pc);
    if (eq != null && eq.contains("mem[" + reg + " + " + off + "]"))
      return true;
    for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of()))
      if (e.src() != 0 && seen.add(e.src()) && tracesToFieldRead(e.src(), reg, off, depth - 1, seen))
        return true;
    return false;
  }

  /**
   * Bit-field decomposition of a packed byte: the distinct AND-masks applied to the field's
   * reads (in the read site itself or one hop later), rendered as the bit range each isolates.
   * A byte carrying several sub-fields (type + direction + frame) shows up as several masks.
   */
  List<String> fieldBits(List<FieldAccess> fas) {
    Set<Integer> masks = new TreeSet<>();
    for (FieldAccess fa : fas) {
      if (fa.op() != 'R')
        continue;
      collectMasks(db.equation.get(fa.site()), masks);
      for (AnalysisDB.Edge e : db.edgesOut.getOrDefault(fa.site(), List.of()))
        collectMasks(db.equation.get(e.dst()), masks);
    }
    masks.remove(255); // the whole byte is not a sub-field
    List<String> out = new ArrayList<>();
    for (int mk : masks)
      out.add("& " + mk + " (" + maskBits(mk) + ")");
    return out;
  }

  private void collectMasks(String eq, Set<Integer> masks) {
    if (eq == null)
      return;
    Matcher m = Pattern.compile("& (\\d+)").matcher(eq);
    while (m.find())
      masks.add(Integer.parseInt(m.group(1)));
  }

  String maskBits(int mask) {
    if (mask <= 0)
      return "none";
    int lo = Integer.numberOfTrailingZeros(mask);
    int hi = 31 - Integer.numberOfLeadingZeros(mask);
    boolean contiguous = mask == (((1 << (hi - lo + 1)) - 1) << lo);
    if (lo == hi)
      return "bit " + lo;
    return (contiguous ? "bits " : "non-contiguous bits ") + lo + "-" + hi;
  }

  /** graphics zone a field indexes into: an ADDR out-edge landing in a discovered gfx region. */
  List<Integer> fieldGfxTarget(List<FieldAccess> fas) {
    for (FieldAccess fa : fas) {
      if (fa.op() != 'R')
        continue;
      for (AnalysisDB.Edge e : db.edgesOut.getOrDefault(fa.site(), List.of())) {
        if (e.role() == null || !e.role().contains("ADDR"))
          continue;
        AnalysisDB.Stat r = db.reads.get(e.dst());
        if (r == null)
          continue;
        for (int[] g : gfxRegions)
          if (Ranges.intersects(r.addrMin(), r.addrMax(), g[0], g[1]))
            return List.of(g[0], g[1]);
      }
    }
    return null;
  }

  /**
   * Exact step by which the array cursor advances to the next record, parsed from the
   * equations. Handles two shapes:
   *   reg = add16(reg, 8)     -> literal step, works for any size (6, 12, ...)
   *   reg = add16(reg, DE)    -> step held in a register; we resolve DE back to the last
   *                              constant assigned to it (DE = 8) among the routine's sites.
   * Returns null when no cursor advance is found (single-record access, or step is a
   * runtime-varying value rather than a fixed record size).
   */
  private Integer parsedCursorStep(List<Integer> sites, String reg) {
    for (int pc : sites) {
      String eq = db.equation.get(pc);
      if (eq == null)
        continue;
      Matcher m = Pattern.compile(reg + " = add16\\(" + reg + ", (\\w+)\\)").matcher(eq);
      if (!m.find())
        continue;
      String operand = m.group(1);
      if (operand.matches("\\d+"))
        return Integer.parseInt(operand);
      // operand is a register: find the last constant assigned to it in this routine
      Integer resolved = null;
      for (int pc2 : sites) {
        String eq2 = db.equation.get(pc2);
        if (eq2 == null)
          continue;
        Matcher m2 = Pattern.compile("\\b" + operand + " = (\\d+)\\b").matcher(eq2);
        if (m2.find())
          resolved = Integer.parseInt(m2.group(1));
      }
      if (resolved != null)
        return resolved;
    }
    return null;
  }

  private Map<String, Object> buildStruct(String method, List<Integer> sites, String reg,
                                          List<FieldAccess> accesses) {
    // stride: PREFER the exact cursor increment parsed from the equations — it works
    // for ANY record size (6, 12, ... included). The lowest-varying-address-bit trick
    // only sees powers of two (a stride-6 cursor flips bit 1 first and would report 2),
    // so it stays as fallback and cross-check.
    Integer parsedStep = parsedCursorStep(sites, reg);
    int bitStride = 0;
    for (FieldAccess fa : accesses) {
      AnalysisDB.Stat s = (fa.op() == 'W' ? db.writes : db.reads).get(fa.site());
      if (s == null)
        continue;
      int varying = s.addrAnd() ^ s.addrOr();
      if (varying != 0) {
        int lowest = Integer.lowestOneBit(varying);
        bitStride = bitStride == 0 ? lowest : Math.min(bitStride, lowest);
      }
    }
    int stride;
    String strideFuente;
    if (parsedStep != null && parsedStep >= 2 && parsedStep <= 64) {
      stride = parsedStep;
      // sanity: the varying bits of a stride-N cursor must be consistent with N
      strideFuente = "cursor advance" + (bitStride != 0 && stride % bitStride != 0
          ? " (INCONSISTENT with the observed address bits)" : "");
    } else if (bitStride != 0) {
      stride = bitStride;
      strideFuente = "lowest varying address bit (only detects powers of 2)";
    } else {
      stride = accesses.stream().mapToInt(FieldAccess::offset).max().orElse(0) + 1;
      strideFuente = "max offset seen (no advance evidence)";
    }
    int base = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
    for (FieldAccess fa : accesses) {
      AnalysisDB.Stat s = (fa.op() == 'W' ? db.writes : db.reads).get(fa.site());
      if (s == null)
        continue;
      int effOff = fa.offset() % stride;
      base = Math.min(base, s.addrMin() - effOff);
      end = Math.max(end, s.addrMax() - effOff);
    }
    if (base == Integer.MAX_VALUE)
      return null;
    int elems = (end - base) / stride + 1;
    int tableEnd = base + stride * elems - 1;

    Map<String, Object> st = new LinkedHashMap<>();
    st.put("routine", method);
    st.put("cursor", reg);
    st.put("base", base);
    st.put("record_bytes", stride);
    st.put("stride_source", strideFuente);
    st.put("elements", elems);
    st.put("range", List.of(base, tableEnd));
    // record the cursor-advance site only when its parsed step is the stride we adopted,
    // so the reported step never contradicts record_bytes
    if (parsedStep != null && parsedStep == stride) {
      for (int pc : sites) {
        String eq = db.equation.get(pc);
        if (eq != null && eq.matches(".*" + reg + " = add16\\(" + reg + ", .+\\).*")) {
          st.put("cursor_advance", Map.of("step", parsedStep, "site", pc));
          break;
        }
      }
    }

    Map<Integer, List<FieldAccess>> byOffset = new TreeMap<>();
    for (FieldAccess fa : accesses)
      byOffset.computeIfAbsent(fa.offset(), k -> new ArrayList<>()).add(fa);

    // terminator/sentinel: a null-terminated array ends with a marker the loop tests for
    // (the leading field is compared against a constant that stops the sweep). Detecting it
    // keeps the last, marker-only slot from being counted as a real record.
    int minOff = byOffset.isEmpty() ? 0 : Collections.min(byOffset.keySet());
    Map<String, Object> term = detectTerminator(sites, reg, minOff, byOffset.get(minOff));
    if (term != null) {
      st.put("terminator", term);
      if (elems > 1) {
        elems--;
        tableEnd = base + stride * elems - 1;
        st.put("elements", elems);
        st.put("range", List.of(base, tableEnd));
      }
    }

    List<Object> campos = new ArrayList<>();
    for (Map.Entry<Integer, List<FieldAccess>> oe : byOffset.entrySet()) {
      int off = oe.getKey();
      long reads = 0, writes = 0;
      int vMin = 256, vMax = -1;
      for (FieldAccess fa : oe.getValue()) {
        AnalysisDB.Stat s = (fa.op() == 'W' ? db.writes : db.reads).get(fa.site());
        if (s == null)
          continue;
        if (fa.op() == 'R')
          reads += s.count();
        else
          writes += s.count();
        vMin = Math.min(vMin, s.valMin());
        vMax = Math.max(vMax, s.valMax());
      }
      Map<String, Object> f = new LinkedHashMap<>();
      f.put("offset", off);
      f.put("reads", reads);
      f.put("writes", writes);
      f.put("values", List.of(vMin, vMax));
      if (off >= stride)
        f.put("field_of_next_element", off % stride);
      List<String> tags = semantics(base, off % stride, stride, tableEnd, oe.getValue());
      if (!tags.isEmpty())
        f.put("semantics", tags);
      List<String> bits = fieldBits(oe.getValue());
      if (!bits.isEmpty())
        f.put("bit_decomposition", bits);
      List<Integer> gfx = fieldGfxTarget(oe.getValue());
      if (gfx != null)
        f.put("points_to_graphics", gfx);
      campos.add(f);
    }
    st.put("fields", campos);
    List<Object> vars = variants(sites, byOffset);
    st.put("variants", vars);
    enrichRelations(campos, vars, byOffset, sites);
    raws.put(st, new Raw(method, sites, accesses));
    return st;
  }

  // ==================== relaciones campo<->campo y nombres propuestos ====================

  /**
   * How the fields of the structure relate to each other and where each one lands:
   * <ul>
   *   <li><b>updated_with</b>: read→...→write of the SAME field, with the operations
   *       seen on the path (inc/dec/suma/resta/invierte) — counters, positions;</li>
   *   <li><b>writes_to</b>: read of A reaches the write of B — derived fields;</li>
   *   <li><b>compared_with</b>: both fields feed the same {@code cp(...)} — limits;</li>
   *   <li><b>decides_over</b>: A drives a branch whose arms touch other fields
   *       exclusively (from the variants) — type/direction selectors;</li>
   *   <li><b>impacts</b>: forward influence classified — screen position (ADDR de
   *       un write a region tipo-pantalla), pixeles (VAL), color (region de atributos),
   *       graphic choice (ADDR de una lectura de la zona de sprites);</li>
   *   <li><b>proposed_name</b>: síntesis de todo lo anterior.</li>
   * </ul>
   */
  @SuppressWarnings("unchecked")
  void enrichRelations(List<Object> campos, List<Object> variantes,
                               Map<Integer, List<FieldAccess>> byOffset, List<Integer> sites) {
    Map<Integer, Integer> writeSiteOffset = new HashMap<>(), readSiteOffset = new HashMap<>();
    byOffset.forEach((off, fas) -> fas.forEach(fa -> {
      if (fa.op() == 'W')
        writeSiteOffset.put(fa.site(), off);
      else
        readSiteOffset.put(fa.site(), off);
    }));

    // compared_with: cp-sites whose inputs trace back to two different fields
    Map<Integer, Set<Integer>> comparado = new HashMap<>();
    for (int pc : sites) {
      String eq = db.equation.get(pc);
      if (eq == null || !eq.contains("cp("))
        continue;
      Set<Integer> offs = new TreeSet<>();
      // one cp operand may be the field the site itself reads (cp(A, mem[IX+k])),
      // the other traces back through the incoming edges — capture both sides
      if (readSiteOffset.containsKey(pc))
        offs.add(readSiteOffset.get(pc));
      for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of())) {
        if (readSiteOffset.containsKey(e.src()))
          offs.add(readSiteOffset.get(e.src()));
        for (AnalysisDB.Edge e2 : db.edgesIn.getOrDefault(e.src(), List.of()))
          if (readSiteOffset.containsKey(e2.src()))
            offs.add(readSiteOffset.get(e2.src()));
      }
      if (offs.size() >= 2)
        for (int a : offs)
          for (int b : offs)
            if (a != b)
              comparado.computeIfAbsent(a, k -> new TreeSet<>()).add(b);
    }

    // decides_over: from the variants whose condition field is this one; a field whose
    // condition carries mask/comparison is a variant SELECTOR (type field)
    Map<Integer, Set<Integer>> decide = new HashMap<>();
    Set<Integer> selectorOffs = new HashSet<>();
    for (Object vo : variantes) {
      Map<String, Object> v = (Map<String, Object>) vo;
      String cond = (String) v.get("condition");
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("field \\+(\\d+)").matcher(cond);
      if (!m.find())
        continue;
      int condOff = Integer.parseInt(m.group(1));
      if (cond.contains("&") || cond.contains("=="))
        selectorOffs.add(condOff);
      for (Object ro : (List<Object>) v.get("arms"))
        decide.computeIfAbsent(condOff, k -> new TreeSet<>())
            .addAll((List<Integer>) ((Map<String, Object>) ro).get("exclusive_fields"));
      if (decide.containsKey(condOff))
        decide.get(condOff).remove(condOff);
    }

    // which offsets are coordinates, and on which axis — a field that writes INTO a
    // coordinate is its increment/velocity; a field COMPARED against a coordinate but
    // never written is its bound/limit (a static edge from the entity template)
    Map<Integer, Character> coordOffAxis = new HashMap<>();
    for (Object fo : campos) {
      Map<String, Object> f = (Map<String, Object>) fo;
      if (f.containsKey("semantics"))
        for (String s : (List<String>) f.get("semantics")) {
          if (s.equals("X coordinate"))
            coordOffAxis.put((int) f.get("offset"), 'X');
          if (s.equals("Y coordinate"))
            coordOffAxis.put((int) f.get("offset"), 'Y');
        }
    }

    for (Object fo : campos) {
      Map<String, Object> f = (Map<String, Object>) fo;
      int off = (int) f.get("offset");
      List<FieldAccess> fas = byOffset.get(off);

      // forward walk from the field's reads: self-updates, writes to other fields, impacts
      Set<String> selfOps = new TreeSet<>(), escribeA = new TreeSet<>(), impacta = new TreeSet<>();
      for (FieldAccess fa : fas) {
        if (fa.op() != 'R')
          continue;
        walkForward(fa.site(), 0, new HashSet<>(), off, writeSiteOffset, new ArrayList<>(),
            selfOps, escribeA, impacta);
      }
      Map<String, Object> rel = new LinkedHashMap<>();
      if (!selfOps.isEmpty())
        rel.put("updated_with", new ArrayList<>(selfOps));
      if (!escribeA.isEmpty())
        rel.put("writes_to", new ArrayList<>(escribeA));
      if (comparado.containsKey(off))
        rel.put("compared_with", comparado.get(off).stream().map(o -> "+" + o).toList());
      if (decide.containsKey(off) && !decide.get(off).isEmpty())
        rel.put("decides_over", decide.get(off).stream().map(o -> "+" + o).toList());
      if (!impacta.isEmpty())
        rel.put("impacts", new ArrayList<>(impacta));
      if (!rel.isEmpty())
        f.put("relations", rel);

      // axis of a coordinate this field WRITES into (increment) vs one it is COMPARED
      // against without writing (bound)
      Character writesCoordAxis = null, comparedCoordAxis = null;
      for (String w : escribeA) {
        int t = Integer.parseInt(w.substring(1));
        if (coordOffAxis.containsKey(t))
          writesCoordAxis = coordOffAxis.get(t);
      }
      if (comparado.containsKey(off))
        for (int cmp : comparado.get(off))
          if (coordOffAxis.containsKey(cmp))
            comparedCoordAxis = coordOffAxis.get(cmp);

      String nombre = proposeName(f, selfOps, comparado.get(off), decide.get(off), impacta,
          selectorOffs.contains(off), comparedCoordAxis, writesCoordAxis);
      if (nombre != null)
        f.put("proposed_name", nombre);
    }
  }

  /** bounded forward DFS collecting ops on the path and classifying the endpoints. */
  private void walkForward(int pc, int depth, Set<Integer> seen, int selfOff,
                           Map<Integer, Integer> writeSiteOffset, List<String> opsPath,
                           Set<String> selfOps, Set<String> escribeA, Set<String> impacta) {
    if (depth > 4)
      return;
    int shown = 0;
    for (AnalysisDB.Edge e : db.edgesOut.getOrDefault(pc, List.of())) {
      if (shown++ >= 6 || e.src() == e.dst() || !seen.add(e.dst()))
        continue;
      int dst = e.dst();
      String ops = opsOf(dst);
      List<String> path = ops.isEmpty() ? opsPath : concat(opsPath, ops);

      Integer wOff = writeSiteOffset.get(dst);
      if (wOff != null) {
        if (wOff == selfOff && !path.isEmpty())
          selfOps.addAll(path);
        else if (wOff != selfOff)
          escribeA.add("+" + wOff);
      }
      AnalysisDB.Stat w = db.writes.get(dst);
      if (w != null)
        for (CoordinateFinder.Region r : screenRegions)
          if (Ranges.intersects(w.addrMin(), w.addrMax(), r.lo(), r.hi())) {
            int sLo = w.addrMin() + r.delta();
            boolean attr = sLo >= 22528 && sLo <= 23295;
            String role = e.role() == null ? "" : e.role();
            impacta.add(attr ? "color (attributes)"
                : role.contains("ADDR") ? "screen position" : "screen pixels");
            break;
          }
      AnalysisDB.Stat rd = db.reads.get(dst);
      if (rd != null && e.role() != null && e.role().contains("ADDR")
          && gfxRegions.stream().anyMatch(g -> Ranges.intersects(rd.addrMin(), rd.addrMax(), g[0], g[1])))
        impacta.add("graphic choice");
      walkForward(dst, depth + 1, seen, selfOff, writeSiteOffset, path, selfOps, escribeA, impacta);
    }
  }

  private static List<String> concat(List<String> a, String b) {
    List<String> out = new ArrayList<>(a);
    out.add(b);
    return out;
  }

  /** operation keywords readable from a site's equation. */
  private String opsOf(int pc) {
    String eq = db.equation.get(pc);
    if (eq == null)
      return "";
    if (eq.contains("inc("))
      return "increments";
    if (eq.contains("dec("))
      return "decrements";
    if (eq.contains("add("))
      return "adds";
    if (eq.matches(".*= 0 - .*") || eq.contains("sub("))
      return "subtracts/negates";
    if (eq.contains(" ^ "))
      return "flips bits (xor)";
    return "";
  }

  @SuppressWarnings("unchecked")
  private String proposeName(Map<String, Object> f, Set<String> selfOps,
                             Set<Integer> comparadoCon, Set<Integer> decideSobre,
                             Set<String> impacta, boolean isSelector,
                             Character comparedCoordAxis, Character writesCoordAxis) {
    List<String> sem = f.containsKey("semantics") ? (List<String>) f.get("semantics") : List.of();
    for (String s : sem) {
      if (s.equals("X coordinate"))
        return "position_x";
      if (s.equals("Y coordinate"))
        return "position_y";
    }
    long escrituras = (long) f.get("writes");
    boolean mueve = selfOps.contains("increments") || selfOps.contains("decrements")
        || selfOps.contains("adds") || selfOps.contains("subtracts/negates");
    boolean invierte = selfOps.contains("flips bits (xor)") || selfOps.contains("subtracts/negates");
    // a field compared against a coordinate and never written itself is a static
    // edge/bound — even when its value gets copied into the coordinate (the clamp on
    // the bounce); a field that is REWRITTEN and feeds the coordinate is the step
    if (comparedCoordAxis != null && escrituras == 0)
      return "limit_" + Character.toLowerCase(comparedCoordAxis)
          + " (edge on " + comparedCoordAxis + "; reaching it reverses the movement)";
    if (writesCoordAxis != null && (comparedCoordAxis == null || comparedCoordAxis == writesCoordAxis))
      return "increment_" + Character.toLowerCase(writesCoordAxis)
          + " (step added to position " + writesCoordAxis + ")";
    if (impacta.contains("graphic choice") && mueve)
      return "animation_frame (changes and picks the graphic)";
    if (invierte && isSelector)
      return "type_and_direction (type bits + direction that flips on bounce)";
    if (invierte && decideSobre != null && !decideSobre.isEmpty())
      return "direction_flag (flips and decides the movement)";
    if (comparedCoordAxis != null)
      return "limit_" + Character.toLowerCase(comparedCoordAxis)
          + " (edge on " + comparedCoordAxis + ")";
    if (isSelector)
      return "type_flags (its bits select the variant)";
    if (comparadoCon != null && !comparadoCon.isEmpty() && !mueve)
      return "limit (compared with " + comparadoCon.stream().map(o -> "+" + o)
          .reduce((a, b) -> a + " " + b).orElse("") + ")";
    if (impacta.contains("color (attributes)") && !impacta.contains("screen position"))
      return "color";
    if (impacta.contains("graphic choice"))
      return "graphic_frame";
    if (decideSobre != null && decideSobre.size() >= 2)
      return "controls_conditions (possible limit or flag)";
    if (mueve && !impacta.contains("screen position"))
      return "counter";
    return null;
  }

  /** what the field feeds, from its outgoing edges and the track annotations. */
  List<String> semantics(int base, int off, int stride, int tableEnd, List<FieldAccess> fas) {
    List<String> tags = new ArrayList<>();
    for (Map.Entry<Integer, Character> ce : coordAxis.entrySet())
      if (ce.getKey() >= base && ce.getKey() <= tableEnd && (ce.getKey() - base) % stride == off) {
        tags.add(ce.getValue() + " coordinate");
        break;
      }
    boolean cond = false, addrGfx = false, addrOther = false;
    for (FieldAccess fa : fas) {
      if (fa.op() != 'R')
        continue;
      for (AnalysisDB.Edge e : db.edgesOut.getOrDefault(fa.site(), List.of())) {
        String role = e.role();
        if (role == null)
          continue;
        if (role.contains("COND"))
          cond = true;
        if (role.contains("ADDR")) {
          AnalysisDB.Stat r = db.reads.get(e.dst());
          if (r != null && gfxRegions.stream().anyMatch(g -> Ranges.intersects(r.addrMin(), r.addrMax(), g[0], g[1])))
            addrGfx = true;
          else
            addrOther = true;
        }
      }
      for (AnalysisDB.Edge e : db.edgesOut.getOrDefault(fa.site(), List.of()))
        for (AnalysisDB.Edge e2 : db.edgesOut.getOrDefault(e.dst(), List.of()))
          if (e2.role() != null && e2.role().contains("COND"))
            cond = true;
    }
    if (cond)
      tags.add("drives branches (type/flag)");
    if (addrGfx)
      tags.add("graphic selector");
    if (addrOther)
      tags.add("index/pointer");
    return tags;
  }

  /**
   * variant layouts: for each branch whose condition traces back to a field read, walk
   * both CFG arms; the fields accessed EXCLUSIVELY inside one arm belong to that variant.
   */
  List<Object> variants(List<Integer> sites, Map<Integer, List<FieldAccess>> byOffset) {
    Set<Integer> methodSites = new HashSet<>(sites);
    Map<Integer, Integer> siteToOffset = new HashMap<>();
    byOffset.forEach((off, fas) -> fas.forEach(fa -> siteToOffset.put(fa.site(), off)));

    List<Object> out = new ArrayList<>();
    for (int branchPc : sites) {
      if (!db.branchSites.contains(branchPc))
        continue;
      List<AnalysisDB.Edge> succs = db.cfgOut.getOrDefault(branchPc, List.of());
      if (succs.size() != 2)
        continue;
      Integer srcOffset = condSourceOffset(branchPc, siteToOffset, 3, new HashSet<>());
      if (srcOffset == null)
        continue;
      Set<Integer> armA = Closure.cfg(db, succs.get(0).dst(), methodSites, Set.of(), 300);
      Set<Integer> armB = Closure.cfg(db, succs.get(1).dst(), methodSites, Set.of(), 300);
      Set<Integer> onlyA = new TreeSet<>(), onlyB = new TreeSet<>();
      for (int s : armA)
        if (!armB.contains(s) && siteToOffset.containsKey(s))
          onlyA.add(siteToOffset.get(s));
      for (int s : armB)
        if (!armA.contains(s) && siteToOffset.containsKey(s))
          onlyB.add(siteToOffset.get(s));
      if (onlyA.isEmpty() && onlyB.isEmpty())
        continue;
      Map<String, Object> v = new LinkedHashMap<>();
      v.put("branch", branchPc);
      v.put("condition", describeCondition(branchPc, srcOffset));
      List<Object> ramas = new ArrayList<>();
      ramas.add(Map.of("pc", succs.get(0).dst(), "times", succs.get(0).count(),
          "exclusive_fields", new ArrayList<>(onlyA)));
      ramas.add(Map.of("pc", succs.get(1).dst(), "times", succs.get(1).count(),
          "exclusive_fields", new ArrayList<>(onlyB)));
      v.put("arms", ramas);
      out.add(v);
    }
    return out;
  }

  private Integer condSourceOffset(int pc, Map<Integer, Integer> siteToOffset, int depth, Set<Integer> seen) {
    if (depth < 0)
      return null;
    for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of())) {
      if (e.src() == 0 || !seen.add(e.src()))
        continue;
      if (siteToOffset.containsKey(e.src()))
        return siteToOffset.get(e.src());
      Integer r = condSourceOffset(e.src(), siteToOffset, depth - 1, seen);
      if (r != null)
        return r;
    }
    return null;
  }

  /** "field +0 & 7 == 3" cuando la máscara y la comparación se pueden leer de las ecuaciones. */
  private String describeCondition(int branchPc, int offset) {
    String mask = "", cmp = "";
    Set<Integer> seen = new HashSet<>();
    ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(branchPc));
    for (int d = 0; d < 3 && !queue.isEmpty(); d++) {
      int n = queue.size();
      for (int i = 0; i < n; i++) {
        int pc = queue.poll();
        String eq = db.equation.get(pc);
        if (eq != null) {
          Matcher mm = MASK.matcher(eq);
          if (mm.find())
            mask = " & " + mm.group(1);
          Matcher cm = CMP.matcher(eq);
          if (cm.find())
            cmp = " == " + cm.group(1);
        }
        for (AnalysisDB.Edge e : db.edgesIn.getOrDefault(pc, List.of()))
          if (e.src() != 0 && seen.add(e.src()))
            queue.add(e.src());
      }
    }
    return "field +" + offset + mask + cmp;
  }

  @SuppressWarnings("unchecked")
  private int minOffOf(Map<String, Object> st) {
    return ((List<Object>) st.get("fields")).stream()
        .mapToInt(o -> (int) ((Map<String, Object>) o).get("offset")).min().orElse(0);
  }

  @SuppressWarnings("unchecked")
  private void printCanonical(Map<String, Object> rec) {
    List<Integer> rango = (List<Integer>) rec.get("range");
    System.out.printf("%n########## CANONICAL RECORD base=%d, %d bytes [%d..%d] ##########%n",
        (int) rec.get("base"), (int) rec.get("record_bytes"), rango.get(0), rango.get(1));
    System.out.println("  (union of: " + String.join(", ", (List<String>) rec.get("routines")) + ")");
    if (rec.containsKey("terminator")) {
      Map<String, Object> t = (Map<String, Object>) rec.get("terminator");
      System.out.printf("  (value %s marks the END of the table; the last slot is not a record)%n",
          t.get("value"));
    }
    if (rec.containsKey("filled_from"))
      System.out.printf("  (filled by expanding the compact table at %d)%n", rec.get("filled_from"));
    if (rec.containsKey("expands_to"))
      System.out.printf("  (template: expands into the buffer at %d)%n", rec.get("expands_to"));
    for (Object fo : (List<Object>) rec.get("fields")) {
      Map<String, Object> f = (Map<String, Object>) fo;
      List<Integer> val = (List<Integer>) f.get("values");
      System.out.printf("  +%d: R x%d, W x%d, val[%d..%d]  %s%n",
          (int) f.get("offset"), (long) f.get("reads"), (long) f.get("writes"),
          val.get(0), val.get(1), f.getOrDefault("proposed_name", "(unnamed)"));
      if (f.containsKey("bit_decomposition"))
        System.out.println("        bits: " + String.join(", ", (List<String>) f.get("bit_decomposition")));
      if (f.containsKey("points_to_graphics")) {
        List<Integer> gg = (List<Integer>) f.get("points_to_graphics");
        System.out.printf("        points to graphics [%d..%d]%n", gg.get(0), gg.get(1));
      }
      if (f.containsKey("contributed_by"))
        for (String a : (List<String>) f.get("contributed_by"))
          System.out.println("        " + a);
    }
    printTipos(rec);
  }

  @SuppressWarnings("unchecked")
  private void printTipos(Map<String, Object> rec) {
    if (!rec.containsKey("types"))
      return;
    Map<String, Object> disc = (Map<String, Object>) rec.get("discriminant");
    System.out.printf("  DISCRIMINANT: field %s & %s (%s) — observed values: %s%s%n",
        disc.get("field"), disc.get("mask"), disc.get("bits"),
        ((List<Integer>) disc.get("observed_values")).stream()
            .map(String::valueOf).reduce((a, b) -> a + " " + b).orElse(""),
        rec.containsKey("slots_with_observed_data")
            ? "  (" + rec.get("slots_with_observed_data") + " slots with data)" : "");
    for (Object to : (List<Object>) rec.get("types")) {
      Map<String, Object> t = (Map<String, Object>) to;
      System.out.printf("  TYPE %s \"%s\"%s%s%n", t.get("value"), t.get("proposed_name"),
          t.containsKey("spans_records") ? "  (spans " + t.get("spans_records") + " records)" : "",
          t.containsKey("observed_frames") ? "  [frames x" + t.get("observed_frames") + "]" : "");
      ((Map<String, Object>) t.get("selected_in"))
          .forEach((rutina, cond) -> System.out.printf("      in %s: %s%n", rutina, cond));
      if (t.containsKey("bits_varying_outside_mask"))
        System.out.println("      bits varying outside the mask: "
            + t.get("bits_varying_outside_mask"));
      for (Object fo : (List<Object>) t.get("fields")) {
        Map<String, Object> f = (Map<String, Object>) fo;
        List<Integer> val = (List<Integer>) f.get("values");
        System.out.printf("      +%d: R x%d, W x%d, val[%d..%d]  %s%s%n",
            (int) f.get("offset"), (long) f.get("reads"), (long) f.get("writes"),
            val.get(0), val.get(1), f.getOrDefault("proposed_name", "(unnamed)"),
            f.containsKey("field_of_next_record")
                ? "  (= +" + f.get("field_of_next_record") + " of the NEXT record)" : "");
        if (f.containsKey("bit_decomposition"))
          System.out.println("          bits: " + String.join(", ", (List<String>) f.get("bit_decomposition")));
      }
    }
  }

  // ---------- text rendering of the data model ----------
  @SuppressWarnings("unchecked")
  private void print(Map<String, Object> st) {
    System.out.printf("%n=== %s via %s: ARRAY base=%d, %d-byte record, %d elements [%d..%d]%s ===%n",
        st.get("routine"), st.get("cursor"), (int) st.get("base"), (int) st.get("record_bytes"),
        (int) st.get("elements"), ((List<Integer>) st.get("range")).get(0), ((List<Integer>) st.get("range")).get(1),
        st.containsKey("cursor_advance")
            ? "  (cursor advance: +" + ((Map<String, Object>) st.get("cursor_advance")).get("step") + ")" : "");
    String sf = (String) st.get("stride_source");
    if (sf != null && !sf.startsWith("cursor advance"))
      System.out.printf("    (stride estimated by %s)%n", sf);
    if (st.containsKey("terminator")) {
      Map<String, Object> t = (Map<String, Object>) st.get("terminator");
      System.out.printf("    (+%d = value %s marks the END of the table @%d, not counted as a record)%n",
          minOffOf(st), t.get("value"), t.get("site"));
    }
    if (st.containsKey("expands_to"))
      System.out.printf("    (TEMPLATE: expands into the buffer at %d, one record per element)%n",
          st.get("expands_to"));
    if (st.containsKey("filled_from"))
      System.out.printf("    (INSTANCE: filled by expanding the compact table at %d)%n",
          st.get("filled_from"));
    for (Object fo : (List<Object>) st.get("fields")) {
      Map<String, Object> f = (Map<String, Object>) fo;
      List<Integer> val = (List<Integer>) f.get("values");
      StringBuilder sb = new StringBuilder(String.format("  +%d: R x%d, W x%d, val[%d..%d]",
          (int) f.get("offset"), (long) f.get("reads"), (long) f.get("writes"), val.get(0), val.get(1)));
      if (f.containsKey("field_of_next_element"))
        sb.append(String.format("  (= +%d of the next element)", (int) f.get("field_of_next_element")));
      if (f.containsKey("semantics"))
        sb.append("  <- ").append(String.join(", ", (List<String>) f.get("semantics")));
      if (f.containsKey("proposed_name"))
        sb.append("\n      NAME: ").append(f.get("proposed_name"));
      if (f.containsKey("bit_decomposition"))
        sb.append("\n      bits: ").append(String.join(", ", (List<String>) f.get("bit_decomposition")));
      if (f.containsKey("points_to_graphics")) {
        List<Integer> g = (List<Integer>) f.get("points_to_graphics");
        sb.append(String.format("%n      points to graphics [%d..%d]", g.get(0), g.get(1)));
      }
      if (f.containsKey("relations")) {
        Map<String, Object> rel = (Map<String, Object>) f.get("relations");
        for (Map.Entry<String, Object> re : rel.entrySet())
          sb.append("\n      ").append(re.getKey()).append(": ")
              .append(String.join(", ", ((List<String>) re.getValue())));
      }
      System.out.println(sb);
    }
    for (Object vo : (List<Object>) st.get("variants")) {
      Map<String, Object> v = (Map<String, Object>) vo;
      List<Object> ramas = (List<Object>) v.get("arms");
      Map<String, Object> a = (Map<String, Object>) ramas.get(0), b = (Map<String, Object>) ramas.get(1);
      System.out.printf("  VARIANT at @%d (%s): x%d / x%d%n", (int) v.get("branch"), v.get("condition"),
          (long) a.get("times"), (long) b.get("times"));
      for (Map<String, Object> rama : List.of(a, b)) {
        List<Integer> campos = (List<Integer>) rama.get("exclusive_fields");
        if (!campos.isEmpty())
          System.out.println("    arm @" + rama.get("pc") + " uses only: +"
              + campos.stream().map(String::valueOf).reduce((x, y) -> x + " +" + y).orElse(""));
      }
    }
  }
}
