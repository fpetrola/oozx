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
  private final Map<Integer, Character> coordAxis = new HashMap<>();
  private final List<int[]> gfxRegions;
  private final List<CoordinateFinder.Region> screenRegions;

  public StructFinder(AnalysisDB db, String dbPath) {
    this.db = db;
    Explainer explainer = new Explainer(db, dbPath);
    // coordinates only from the STRONG validated pairs: single-axis matches carry noise
    try (java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbPath);
         java.sql.ResultSet rs = c.createStatement().executeQuery(
             "SELECT x_addr, y_addr FROM coord_pairs WHERE rate >= 0.3")) {
      while (rs.next()) {
        coordAxis.putIfAbsent(rs.getInt(1), 'X');
        coordAxis.putIfAbsent(rs.getInt(2), 'Y');
      }
    } catch (java.sql.SQLException ignored) {
    }
    CoordinateFinder.Plan plan = new CoordinateFinder(db).find();
    this.screenRegions = plan.regions();
    Set<Integer> valReads = GameMapper.roleReads(db, plan, "VAL");
    this.gfxRegions = GameMapper.mergeRanges(valReads.stream()
        .map(db.reads::get)
        .filter(r -> {
          String c = explainer.classifyRange(r.addrMin(), r.addrMax());
          return c.startsWith("ESTATICA") || c.startsWith("mayormente") || c.startsWith("MIXTA");
        })
        .map(r -> new int[]{r.addrMin(), r.addrMax()}).toList(), 64)
        .stream().filter(g -> g[1] - g[0] + 1 >= 1024).toList();
  }

  /** all recovered structures as data, ready for the JSON export. */
  public List<Map<String, Object>> analyze(String methodFilter) {
    Map<String, List<Integer>> byMethod = new TreeMap<>();
    db.method.forEach((pc, m) -> byMethod.computeIfAbsent(m, k -> new ArrayList<>()).add(pc));
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map.Entry<String, List<Integer>> me : byMethod.entrySet()) {
      if (methodFilter != null && !me.getKey().contains(methodFilter))
        continue;
      out.addAll(structsOfMethod(me.getKey(), me.getValue()));
    }
    return out;
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
      groups.computeIfAbsent(st.get("base") + "|" + st.get("registro_bytes"), k -> new ArrayList<>())
          .add(st);
    List<Map<String, Object>> out = new ArrayList<>();
    for (List<Map<String, Object>> g : groups.values()) {
      long rutinas = g.stream().map(s -> s.get("rutina")).distinct().count();
      if (rutinas < 2)
        continue;
      out.add(mergeGroup(g));
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> mergeGroup(List<Map<String, Object>> g) {
    Map<String, Object> first = g.get(0);
    int stride = (int) first.get("registro_bytes");
    int base = (int) first.get("base");
    int tableEnd = base;
    for (Map<String, Object> st : g)
      tableEnd = Math.max(tableEnd, ((List<Integer>) st.get("rango")).get(1));

    // fold every routine's field at offset (off % stride) into one canonical field
    Map<Integer, Map<String, Object>> byOff = new TreeMap<>();
    Map<Integer, List<String>> aportes = new TreeMap<>();
    for (Map<String, Object> st : g) {
      String rutina = (String) st.get("rutina");
      for (Object fo : (List<Object>) st.get("campos")) {
        Map<String, Object> f = (Map<String, Object>) fo;
        int off = ((int) f.get("offset")) % stride;
        Map<String, Object> c = byOff.computeIfAbsent(off, k -> {
          Map<String, Object> m = new LinkedHashMap<>();
          m.put("offset", k);
          m.put("lecturas", 0L);
          m.put("escrituras", 0L);
          m.put("valores", new int[]{256, -1});
          m.put("semantica", new LinkedHashSet<String>());
          m.put("bits", new LinkedHashSet<String>());
          m.put("nombre_propuesto", (String) null);
          return m;
        });
        c.put("lecturas", (long) c.get("lecturas") + (long) f.get("lecturas"));
        c.put("escrituras", (long) c.get("escrituras") + (long) f.get("escrituras"));
        int[] vr = (int[]) c.get("valores");
        List<Integer> fv = (List<Integer>) f.get("valores");
        vr[0] = Math.min(vr[0], fv.get(0));
        vr[1] = Math.max(vr[1], fv.get(1));
        if (f.containsKey("semantica"))
          ((Set<String>) c.get("semantica")).addAll((List<String>) f.get("semantica"));
        if (f.containsKey("descomposicion_bits"))
          ((Set<String>) c.get("bits")).addAll((List<String>) f.get("descomposicion_bits"));
        if (f.containsKey("apunta_a_grafico"))
          c.putIfAbsent("apunta_a_grafico", f.get("apunta_a_grafico"));
        String cand = (String) f.get("nombre_propuesto");
        if (nameScore(cand) > nameScore((String) c.get("nombre_propuesto")))
          c.put("nombre_propuesto", cand);
        if (cand != null)
          aportes.computeIfAbsent(off, k -> new ArrayList<>()).add(rutina + ": " + cand);
      }
    }

    List<Object> campos = new ArrayList<>();
    for (Map<String, Object> c : byOff.values()) {
      int[] vr = (int[]) c.get("valores");
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("offset", c.get("offset"));
      out.put("lecturas", c.get("lecturas"));
      out.put("escrituras", c.get("escrituras"));
      out.put("valores", List.of(vr[0] <= vr[1] ? vr[0] : 0, vr[1] >= 0 ? vr[1] : 0));
      Set<String> sem = (Set<String>) c.get("semantica");
      if (!sem.isEmpty())
        out.put("semantica", new ArrayList<>(sem));
      Set<String> bits = (Set<String>) c.get("bits");
      if (!bits.isEmpty())
        out.put("descomposicion_bits", new ArrayList<>(bits));
      if (c.containsKey("apunta_a_grafico"))
        out.put("apunta_a_grafico", c.get("apunta_a_grafico"));
      if (c.get("nombre_propuesto") != null)
        out.put("nombre_propuesto", c.get("nombre_propuesto"));
      List<String> ap = aportes.get((int) c.get("offset"));
      if (ap != null)
        out.put("aportado_por", ap);
      campos.add(out);
    }

    Map<String, Object> rec = new LinkedHashMap<>();
    rec.put("base", base);
    rec.put("registro_bytes", stride);
    rec.put("rango", List.of(base, tableEnd));
    rec.put("rutinas", g.stream().map(s -> (String) s.get("rutina")).distinct().sorted().toList());
    for (Map<String, Object> st : g)
      if (st.containsKey("terminador"))
        rec.putIfAbsent("terminador", st.get("terminador"));
    rec.put("campos", campos);
    return rec;
  }

  /** how specific/trustworthy a proposed name is, so union keeps the most informative one. */
  private int nameScore(String n) {
    if (n == null)
      return 0;
    if (n.startsWith("posicion_") || n.startsWith("limite_") || n.startsWith("incremento_"))
      return 5;
    if (n.startsWith("frame_animacion") || n.startsWith("direccion_sentido")
        || n.startsWith("tipo_y_direccion"))
      return 4;
    if (n.startsWith("grafico_frame") || n.equals("color"))
      return 3;
    if (n.startsWith("limite") || n.startsWith("contador"))
      return 2;
    return 1; // tipo_flags, controla_condiciones — generic fallbacks
  }

  private record FieldAccess(int site, char op, int offset) {
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
      List<FieldAccess> accesses = re.getValue();
      if (accesses.stream().mapToInt(FieldAccess::offset).distinct().count() < 2)
        continue; // one lone field is not a structure
      Map<String, Object> st = buildStruct(method, sites, re.getKey(), accesses);
      if (st != null)
        out.add(st);
    }
    return out;
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
      String eq = db.equation.get(pc);
      if (eq == null || !eq.contains("cp("))
        continue;
      Matcher m = CMP.matcher(eq);
      if (!m.find())
        continue;
      int c = Integer.parseInt(m.group(1));
      if (c != 0 && c != 255 && c != vMax) // a sentinel sits at the value extreme
        continue;
      if (tracesToFieldRead(pc, reg, minOff, 3, new HashSet<>())) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("valor", c);
        t.put("site", pc);
        t.put("nota", "el ultimo slot es un marcador de fin, no un registro");
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
  private List<String> fieldBits(List<FieldAccess> fas) {
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

  private String maskBits(int mask) {
    if (mask <= 0)
      return "ninguno";
    int lo = Integer.numberOfTrailingZeros(mask);
    int hi = 31 - Integer.numberOfLeadingZeros(mask);
    boolean contiguous = mask == (((1 << (hi - lo + 1)) - 1) << lo);
    if (lo == hi)
      return "bit " + lo;
    return (contiguous ? "bits " : "bits no contiguos ") + lo + "-" + hi;
  }

  /** graphics zone a field indexes into: an ADDR out-edge landing in a discovered gfx region. */
  private List<Integer> fieldGfxTarget(List<FieldAccess> fas) {
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
          if (r.addrMax() >= g[0] && r.addrMin() <= g[1])
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
      strideFuente = "avance del cursor" + (bitStride != 0 && stride % bitStride != 0
          ? " (INCONSISTENTE con los bits de direccion observados)" : "");
    } else if (bitStride != 0) {
      stride = bitStride;
      strideFuente = "bit de direccion mas bajo que varia (solo detecta potencias de 2)";
    } else {
      stride = accesses.stream().mapToInt(FieldAccess::offset).max().orElse(0) + 1;
      strideFuente = "maximo offset visto (sin evidencia de avance)";
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
    st.put("rutina", method);
    st.put("cursor", reg);
    st.put("base", base);
    st.put("registro_bytes", stride);
    st.put("stride_fuente", strideFuente);
    st.put("elementos", elems);
    st.put("rango", List.of(base, tableEnd));
    // record the cursor-advance site only when its parsed step is the stride we adopted,
    // so the reported step never contradicts registro_bytes
    if (parsedStep != null && parsedStep == stride) {
      for (int pc : sites) {
        String eq = db.equation.get(pc);
        if (eq != null && eq.matches(".*" + reg + " = add16\\(" + reg + ", .+\\).*")) {
          st.put("avance_cursor", Map.of("paso", parsedStep, "site", pc));
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
      st.put("terminador", term);
      if (elems > 1) {
        elems--;
        tableEnd = base + stride * elems - 1;
        st.put("elementos", elems);
        st.put("rango", List.of(base, tableEnd));
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
      f.put("lecturas", reads);
      f.put("escrituras", writes);
      f.put("valores", List.of(vMin, vMax));
      if (off >= stride)
        f.put("campo_del_elemento_siguiente", off % stride);
      List<String> tags = semantics(base, off % stride, stride, tableEnd, oe.getValue());
      if (!tags.isEmpty())
        f.put("semantica", tags);
      List<String> bits = fieldBits(oe.getValue());
      if (!bits.isEmpty())
        f.put("descomposicion_bits", bits);
      List<Integer> gfx = fieldGfxTarget(oe.getValue());
      if (gfx != null)
        f.put("apunta_a_grafico", gfx);
      campos.add(f);
    }
    st.put("campos", campos);
    List<Object> vars = variants(sites, byOffset);
    st.put("variantes", vars);
    enrichRelations(campos, vars, byOffset, sites);
    return st;
  }

  // ==================== relaciones campo<->campo y nombres propuestos ====================

  /**
   * How the fields of the structure relate to each other and where each one lands:
   * <ul>
   *   <li><b>se_actualiza_con</b>: read→...→write of the SAME field, with the operations
   *       seen on the path (inc/dec/suma/resta/invierte) — counters, positions;</li>
   *   <li><b>escribe_a</b>: read of A reaches the write of B — derived fields;</li>
   *   <li><b>comparado_con</b>: both fields feed the same {@code cp(...)} — limits;</li>
   *   <li><b>decide_sobre</b>: A drives a branch whose arms touch other fields
   *       exclusively (from the variants) — type/direction selectors;</li>
   *   <li><b>impacta_en</b>: forward influence classified — posicion en pantalla (ADDR de
   *       un write a region tipo-pantalla), pixeles (VAL), color (region de atributos),
   *       eleccion del grafico (ADDR de una lectura de la zona de sprites);</li>
   *   <li><b>nombre_propuesto</b>: síntesis de todo lo anterior.</li>
   * </ul>
   */
  @SuppressWarnings("unchecked")
  private void enrichRelations(List<Object> campos, List<Object> variantes,
                               Map<Integer, List<FieldAccess>> byOffset, List<Integer> sites) {
    Map<Integer, Integer> writeSiteOffset = new HashMap<>(), readSiteOffset = new HashMap<>();
    byOffset.forEach((off, fas) -> fas.forEach(fa -> {
      if (fa.op() == 'W')
        writeSiteOffset.put(fa.site(), off);
      else
        readSiteOffset.put(fa.site(), off);
    }));

    // comparado_con: cp-sites whose inputs trace back to two different fields
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

    // decide_sobre: from the variants whose condition field is this one; a field whose
    // condition carries mask/comparison is a variant SELECTOR (type field)
    Map<Integer, Set<Integer>> decide = new HashMap<>();
    Set<Integer> selectorOffs = new HashSet<>();
    for (Object vo : variantes) {
      Map<String, Object> v = (Map<String, Object>) vo;
      String cond = (String) v.get("condicion");
      java.util.regex.Matcher m = java.util.regex.Pattern.compile("campo \\+(\\d+)").matcher(cond);
      if (!m.find())
        continue;
      int condOff = Integer.parseInt(m.group(1));
      if (cond.contains("&") || cond.contains("=="))
        selectorOffs.add(condOff);
      for (Object ro : (List<Object>) v.get("ramas"))
        decide.computeIfAbsent(condOff, k -> new TreeSet<>())
            .addAll((List<Integer>) ((Map<String, Object>) ro).get("campos_exclusivos"));
      if (decide.containsKey(condOff))
        decide.get(condOff).remove(condOff);
    }

    // which offsets are coordinates, and on which axis — a field that writes INTO a
    // coordinate is its increment/velocity; a field COMPARED against a coordinate but
    // never written is its bound/limit (a static edge from the entity template)
    Map<Integer, Character> coordOffAxis = new HashMap<>();
    for (Object fo : campos) {
      Map<String, Object> f = (Map<String, Object>) fo;
      if (f.containsKey("semantica"))
        for (String s : (List<String>) f.get("semantica")) {
          if (s.equals("coordenada X"))
            coordOffAxis.put((int) f.get("offset"), 'X');
          if (s.equals("coordenada Y"))
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
        rel.put("se_actualiza_con", new ArrayList<>(selfOps));
      if (!escribeA.isEmpty())
        rel.put("escribe_a", new ArrayList<>(escribeA));
      if (comparado.containsKey(off))
        rel.put("comparado_con", comparado.get(off).stream().map(o -> "+" + o).toList());
      if (decide.containsKey(off) && !decide.get(off).isEmpty())
        rel.put("decide_sobre", decide.get(off).stream().map(o -> "+" + o).toList());
      if (!impacta.isEmpty())
        rel.put("impacta_en", new ArrayList<>(impacta));
      if (!rel.isEmpty())
        f.put("relaciones", rel);

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
          if (coordOffAxis.containsKey(cmp) && !escribeA.contains("+" + cmp))
            comparedCoordAxis = coordOffAxis.get(cmp);

      String nombre = proposeName(f, selfOps, comparado.get(off), decide.get(off), impacta,
          selectorOffs.contains(off), comparedCoordAxis, writesCoordAxis);
      if (nombre != null)
        f.put("nombre_propuesto", nombre);
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
          if (w.addrMax() >= r.lo() && w.addrMin() <= r.hi()) {
            int sLo = w.addrMin() + r.delta();
            boolean attr = sLo >= 22528 && sLo <= 23295;
            String role = e.role() == null ? "" : e.role();
            impacta.add(attr ? "color (atributos)"
                : role.contains("ADDR") ? "posicion en pantalla" : "pixeles en pantalla");
            break;
          }
      AnalysisDB.Stat rd = db.reads.get(dst);
      if (rd != null && e.role() != null && e.role().contains("ADDR")
          && gfxRegions.stream().anyMatch(g -> rd.addrMax() >= g[0] && rd.addrMin() <= g[1]))
        impacta.add("eleccion del grafico");
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
      return "incrementa";
    if (eq.contains("dec("))
      return "decrementa";
    if (eq.contains("add("))
      return "suma";
    if (eq.matches(".*= 0 - .*") || eq.contains("sub("))
      return "resta/niega";
    if (eq.contains(" ^ "))
      return "invierte bits (xor)";
    return "";
  }

  @SuppressWarnings("unchecked")
  private String proposeName(Map<String, Object> f, Set<String> selfOps,
                             Set<Integer> comparadoCon, Set<Integer> decideSobre,
                             Set<String> impacta, boolean isSelector,
                             Character comparedCoordAxis, Character writesCoordAxis) {
    List<String> sem = f.containsKey("semantica") ? (List<String>) f.get("semantica") : List.of();
    for (String s : sem) {
      if (s.equals("coordenada X"))
        return "posicion_x";
      if (s.equals("coordenada Y"))
        return "posicion_y";
    }
    long escrituras = (long) f.get("escrituras");
    boolean mueve = selfOps.contains("incrementa") || selfOps.contains("decrementa")
        || selfOps.contains("suma") || selfOps.contains("resta/niega");
    boolean invierte = selfOps.contains("invierte bits (xor)") || selfOps.contains("resta/niega");
    // a field that feeds a coordinate's new value is its step/velocity along that axis
    if (writesCoordAxis != null && (comparedCoordAxis == null || comparedCoordAxis == writesCoordAxis))
      return "incremento_" + Character.toLowerCase(writesCoordAxis)
          + " (paso que se suma a la posicion " + writesCoordAxis + ")";
    // a field compared against a coordinate but never written is a static edge/bound: the
    // guardian reverses when its coordinate reaches it
    if (comparedCoordAxis != null && escrituras == 0)
      return "limite_" + Character.toLowerCase(comparedCoordAxis)
          + " (borde en " + comparedCoordAxis + "; al alcanzarlo invierte el movimiento)";
    if (impacta.contains("eleccion del grafico") && mueve)
      return "frame_animacion (cambia y elige el grafico)";
    if (invierte && isSelector)
      return "tipo_y_direccion (bits de tipo + sentido que se invierte al rebotar)";
    if (invierte && decideSobre != null && !decideSobre.isEmpty())
      return "direccion_sentido (se invierte y decide el movimiento)";
    if (comparedCoordAxis != null)
      return "limite_" + Character.toLowerCase(comparedCoordAxis)
          + " (borde en " + comparedCoordAxis + ")";
    if (isSelector)
      return "tipo_flags (sus bits seleccionan la variante)";
    if (comparadoCon != null && !comparadoCon.isEmpty() && !mueve)
      return "limite (se compara con " + comparadoCon.stream().map(o -> "+" + o)
          .reduce((a, b) -> a + " " + b).orElse("") + ")";
    if (impacta.contains("color (atributos)") && !impacta.contains("posicion en pantalla"))
      return "color";
    if (impacta.contains("eleccion del grafico"))
      return "grafico_frame";
    if (decideSobre != null && decideSobre.size() >= 2)
      return "controla_condiciones (posible limite o flag)";
    if (mueve && !impacta.contains("posicion en pantalla"))
      return "contador";
    return null;
  }

  /** what the field feeds, from its outgoing edges and the track annotations. */
  private List<String> semantics(int base, int off, int stride, int tableEnd, List<FieldAccess> fas) {
    List<String> tags = new ArrayList<>();
    for (Map.Entry<Integer, Character> ce : coordAxis.entrySet())
      if (ce.getKey() >= base && ce.getKey() <= tableEnd && (ce.getKey() - base) % stride == off) {
        tags.add("coordenada " + ce.getValue());
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
          if (r != null && gfxRegions.stream().anyMatch(g -> r.addrMax() >= g[0] && r.addrMin() <= g[1]))
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
      tags.add("controla branches (tipo/flag)");
    if (addrGfx)
      tags.add("selector de grafico");
    if (addrOther)
      tags.add("indice/puntero");
    return tags;
  }

  /**
   * variant layouts: for each branch whose condition traces back to a field read, walk
   * both CFG arms; the fields accessed EXCLUSIVELY inside one arm belong to that variant.
   */
  private List<Object> variants(List<Integer> sites, Map<Integer, List<FieldAccess>> byOffset) {
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
      Set<Integer> armA = cfgClosure(succs.get(0).dst(), methodSites, 300);
      Set<Integer> armB = cfgClosure(succs.get(1).dst(), methodSites, 300);
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
      v.put("condicion", describeCondition(branchPc, srcOffset));
      List<Object> ramas = new ArrayList<>();
      ramas.add(Map.of("pc", succs.get(0).dst(), "veces", succs.get(0).count(),
          "campos_exclusivos", new ArrayList<>(onlyA)));
      ramas.add(Map.of("pc", succs.get(1).dst(), "veces", succs.get(1).count(),
          "campos_exclusivos", new ArrayList<>(onlyB)));
      v.put("ramas", ramas);
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

  /** "campo +0 & 7 == 3" cuando la máscara y la comparación se pueden leer de las ecuaciones. */
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
    return "campo +" + offset + mask + cmp;
  }

  /** bounded forward closure over the dynamic CFG, restricted to the method's sites. */
  private Set<Integer> cfgClosure(int start, Set<Integer> methodSites, int limit) {
    Set<Integer> out = new HashSet<>();
    ArrayDeque<Integer> queue = new ArrayDeque<>(List.of(start));
    while (!queue.isEmpty() && out.size() < limit) {
      int pc = queue.poll();
      if (!methodSites.contains(pc) || !out.add(pc))
        continue;
      for (AnalysisDB.Edge e : db.cfgOut.getOrDefault(pc, List.of()))
        queue.add(e.dst());
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  private int minOffOf(Map<String, Object> st) {
    return ((List<Object>) st.get("campos")).stream()
        .mapToInt(o -> (int) ((Map<String, Object>) o).get("offset")).min().orElse(0);
  }

  @SuppressWarnings("unchecked")
  private void printCanonical(Map<String, Object> rec) {
    List<Integer> rango = (List<Integer>) rec.get("rango");
    System.out.printf("%n########## REGISTRO CANONICO base=%d, %d bytes [%d..%d] ##########%n",
        (int) rec.get("base"), (int) rec.get("registro_bytes"), rango.get(0), rango.get(1));
    System.out.println("  (union de: " + String.join(", ", (List<String>) rec.get("rutinas")) + ")");
    if (rec.containsKey("terminador")) {
      Map<String, Object> t = (Map<String, Object>) rec.get("terminador");
      System.out.printf("  (valor %s marca FIN de la tabla; el ultimo slot no es un registro)%n",
          t.get("valor"));
    }
    for (Object fo : (List<Object>) rec.get("campos")) {
      Map<String, Object> f = (Map<String, Object>) fo;
      List<Integer> val = (List<Integer>) f.get("valores");
      System.out.printf("  +%d: R x%d, W x%d, val[%d..%d]  %s%n",
          (int) f.get("offset"), (long) f.get("lecturas"), (long) f.get("escrituras"),
          val.get(0), val.get(1), f.getOrDefault("nombre_propuesto", "(sin nombre)"));
      if (f.containsKey("descomposicion_bits"))
        System.out.println("        bits: " + String.join(", ", (List<String>) f.get("descomposicion_bits")));
      if (f.containsKey("apunta_a_grafico")) {
        List<Integer> gg = (List<Integer>) f.get("apunta_a_grafico");
        System.out.printf("        apunta a graficos [%d..%d]%n", gg.get(0), gg.get(1));
      }
      if (f.containsKey("aportado_por"))
        for (String a : (List<String>) f.get("aportado_por"))
          System.out.println("        " + a);
    }
  }

  // ---------- text rendering of the data model ----------
  @SuppressWarnings("unchecked")
  private void print(Map<String, Object> st) {
    System.out.printf("%n=== %s via %s: ARREGLO base=%d, registro de %d bytes, %d elementos [%d..%d]%s ===%n",
        st.get("rutina"), st.get("cursor"), (int) st.get("base"), (int) st.get("registro_bytes"),
        (int) st.get("elementos"), ((List<Integer>) st.get("rango")).get(0), ((List<Integer>) st.get("rango")).get(1),
        st.containsKey("avance_cursor")
            ? "  (avance del cursor: +" + ((Map<String, Object>) st.get("avance_cursor")).get("paso") + ")" : "");
    String sf = (String) st.get("stride_fuente");
    if (sf != null && !sf.startsWith("avance del cursor"))
      System.out.printf("    (stride estimado por %s)%n", sf);
    if (st.containsKey("terminador")) {
      Map<String, Object> t = (Map<String, Object>) st.get("terminador");
      System.out.printf("    (+%d = valor %s marca FIN de la tabla @%d, no se cuenta como registro)%n",
          minOffOf(st), t.get("valor"), t.get("site"));
    }
    for (Object fo : (List<Object>) st.get("campos")) {
      Map<String, Object> f = (Map<String, Object>) fo;
      List<Integer> val = (List<Integer>) f.get("valores");
      StringBuilder sb = new StringBuilder(String.format("  +%d: R x%d, W x%d, val[%d..%d]",
          (int) f.get("offset"), (long) f.get("lecturas"), (long) f.get("escrituras"), val.get(0), val.get(1)));
      if (f.containsKey("campo_del_elemento_siguiente"))
        sb.append(String.format("  (= +%d del elemento siguiente)", (int) f.get("campo_del_elemento_siguiente")));
      if (f.containsKey("semantica"))
        sb.append("  <- ").append(String.join(", ", (List<String>) f.get("semantica")));
      if (f.containsKey("nombre_propuesto"))
        sb.append("\n      NOMBRE: ").append(f.get("nombre_propuesto"));
      if (f.containsKey("descomposicion_bits"))
        sb.append("\n      bits: ").append(String.join(", ", (List<String>) f.get("descomposicion_bits")));
      if (f.containsKey("apunta_a_grafico")) {
        List<Integer> g = (List<Integer>) f.get("apunta_a_grafico");
        sb.append(String.format("%n      apunta a graficos [%d..%d]", g.get(0), g.get(1)));
      }
      if (f.containsKey("relaciones")) {
        Map<String, Object> rel = (Map<String, Object>) f.get("relaciones");
        for (Map.Entry<String, Object> re : rel.entrySet())
          sb.append("\n      ").append(re.getKey()).append(": ")
              .append(String.join(", ", ((List<String>) re.getValue())));
      }
      System.out.println(sb);
    }
    for (Object vo : (List<Object>) st.get("variantes")) {
      Map<String, Object> v = (Map<String, Object>) vo;
      List<Object> ramas = (List<Object>) v.get("ramas");
      Map<String, Object> a = (Map<String, Object>) ramas.get(0), b = (Map<String, Object>) ramas.get(1);
      System.out.printf("  VARIANTE en @%d (%s): x%d / x%d%n", (int) v.get("branch"), v.get("condicion"),
          (long) a.get("veces"), (long) b.get("veces"));
      for (Map<String, Object> rama : List.of(a, b)) {
        List<Integer> campos = (List<Integer>) rama.get("campos_exclusivos");
        if (!campos.isEmpty())
          System.out.println("    rama @" + rama.get("pc") + " usa exclusivamente: +"
              + campos.stream().map(String::valueOf).reduce((x, y) -> x + " +" + y).orElse(""));
      }
    }
  }
}
