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
import com.fpetrola.z80.analysis.query.Flow;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discriminated-union splitter (analizador de tipos): a record whose routines test one
 * field against several constants — the classic type ladder
 * {@code A = mem[IX+0]; A &= 7; cp 1 / cp 2 / cp 3 ...} — is really a UNION of record
 * types, one per discriminant value. Analysing the fields over all instances mixes the
 * types and produces contradictory names ("counter" from one type, "colour" from another).
 * This class recovers the types:
 * <ol>
 *   <li><b>ladder</b>: branches whose condition traces back to the same field read
 *       through the same AND-mask, tested for equality against constants ({@code cp(A,c)}
 *       or {@code flagZ} = the ==0 test). One test = a flag; two or more = a ladder.</li>
 *   <li><b>arm polarity</b>: in a ladder the not-equal arm falls through to the NEXT
 *       test, so it is identified by elimination; the last test's polarity is calibrated
 *       from the resolved ones (and marked estimated).</li>
 *   <li><b>type values</b>: the REAL values the discriminant held, from the per-frame
 *       cell log ({@code frame_cells}), masked by the union of the ladder masks. This is
 *       ground truth: no enumeration of test constants can see a value that never ran.</li>
 *   <li><b>per-type view</b>: for each value, walk each routine's ladder to its handler
 *       (closure of the equal arm minus closure of the sibling arm) and re-run the field
 *       analysis restricted to those sites. Fields get per-type names; a type whose
 *       handler touches offsets beyond the stride is an EXTENDED record (occupies more
 *       than one slot).</li>
 * </ol>
 * Everything is generic: no address, mask or value is game-specific.
 */
final class TypeSplitter {
  private static final Pattern CMP = Pattern.compile("cp\\([A-Z], (\\d+)\\)");
  private static final Pattern MASK = Pattern.compile("& (\\d+)");

  private final StructFinder sf;
  private final AnalysisDB db;
  private final String dbPath;

  TypeSplitter(StructFinder sf, AnalysisDB db, String dbPath) {
    this.sf = sf;
    this.db = db;
    this.dbPath = dbPath;
  }

  /** one equality test of a ladder. mask -1 = the raw (unmasked) value is compared. */
  private record Test(int branchPc, int offset, int mask, int value, int readSite) {
  }

  /** a resolved test: which CFG arm is "equal" and which falls through to the next test. */
  private record Resolved(Test t, int eqArm, int neArm, long eqCount, long neCount,
                          boolean polaridadEstimada) {
  }

  private record Ladder(String rutina, List<Integer> sites, int offset, int mask,
                        List<Resolved> tests, Set<Integer> readSites) {
  }

  /**
   * @return {@code {discriminante, slots_with_observed_data, tipos}} for the canonical
   * record, or null when no ladder (no discriminated union) is found.
   */
  @SuppressWarnings("unchecked")
  Map<String, Object> discriminate(int base, int stride, int tableEnd, Integer termValor,
                                   List<StructFinder.Raw> raws) {
    List<Ladder> ladders = new ArrayList<>();
    for (StructFinder.Raw raw : raws)
      ladders.addAll(findLadders(raw, termValor));
    if (ladders.isEmpty())
      return null;

    // discriminant = the offset whose ladder is corroborated by the most routines
    // (a type field gets tested by mover AND drawer), then the masked one (type ids
    // are packed sub-fields), then the one with more tests
    Map<Integer, Set<String>> rutinasPorOff = new HashMap<>();
    Map<Integer, Integer> maskedPorOff = new HashMap<>(), testsPorOff = new HashMap<>();
    for (Ladder l : ladders) {
      rutinasPorOff.computeIfAbsent(l.offset(), k -> new HashSet<>()).add(l.rutina());
      if (l.mask() >= 0)
        maskedPorOff.merge(l.offset(), 1, Integer::sum);
      testsPorOff.merge(l.offset(), l.tests().size(), Integer::sum);
    }
    int discOff = rutinasPorOff.keySet().stream()
        .max(Comparator.comparingInt(off -> rutinasPorOff.get(off).size() * 1000
            + maskedPorOff.getOrDefault(off, 0) * 100 + testsPorOff.get(off)))
        .get();
    ladders.removeIf(l -> l.offset() != discOff);

    int m = 0;
    for (Ladder l : ladders)
      m |= l.mask() < 0 ? 255 : l.mask();
    final int mask = m;

    // ground truth of which type values exist: the discriminant cells' per-frame values
    Map<Integer, Long> framesPorValor = new TreeMap<>(); // valor & mask -> frames
    Map<Integer, Integer> bitsExtra = new TreeMap<>();   // valor & mask -> OR de bits fuera de la mascara
    int slotsConDatos = observeDiscriminant(base, stride, tableEnd, discOff, termValor,
        mask, framesPorValor, bitsExtra);
    List<Integer> typeValues = new ArrayList<>(framesPorValor.keySet());
    if (typeValues.isEmpty() || typeValues.size() > 12) {
      // cells not logged (or mask too wide to be a type id): fall back to the tested values
      typeValues.clear();
      for (Ladder l : ladders)
        for (Resolved r : l.tests())
          if (!typeValues.contains(r.t().value() & mask))
            typeValues.add(r.t().value() & mask);
      Collections.sort(typeValues);
    }
    if (typeValues.size() < 2)
      return null;

    // per type: union over routines of the handler-exclusive sites
    Map<Integer, Set<Integer>> typeSites = new TreeMap<>();
    Map<Integer, Map<String, String>> typeCond = new TreeMap<>();  // v -> rutina -> condicion
    Map<Integer, Map<String, Long>> typeVeces = new TreeMap<>();   // v -> rutina -> ejecuciones
    for (int v : typeValues) {
      typeSites.put(v, new TreeSet<>());
      typeCond.put(v, new LinkedHashMap<>());
      typeVeces.put(v, new LinkedHashMap<>());
    }
    for (Ladder l : ladders) {
      Set<Integer> methodSites = new HashSet<>(l.sites());
      for (int v : typeValues) {
        Resolved match = null;
        for (Resolved r : l.tests()) {
          int tm = r.t().mask() < 0 ? 255 : r.t().mask();
          if ((v & tm) == (r.t().value() & tm)) {
            match = r;
            break;
          }
        }
        Set<Integer> handler;
        String cond;
        long veces;
        if (match != null) {
          handler = exclusive(match.eqArm(), match.neArm(), methodSites, l.readSites());
          cond = condText(match.t()) + (match.polaridadEstimada() ? " (estimated polarity)" : "")
              + (match.eqCount() == 0 ? " (branch never executed in this replay)" : "");
          veces = match.eqCount();
        } else { // residual class: falls off the end of the ladder
          Resolved last = l.tests().get(l.tests().size() - 1);
          handler = exclusive(last.neArm(), last.eqArm(), methodSites, l.readSites());
          cond = "rest (no test in " + l.rutina() + " captures it)";
          veces = last.neCount();
        }
        typeSites.get(v).addAll(handler);
        typeCond.get(v).put(l.rutina(), cond);
        typeVeces.get(v).merge(l.rutina(), veces, Long::sum);
      }
    }

    // fields per type, re-analysed over the type's exclusive sites only
    Map<Integer, List<StructFinder.FieldAccess>> allAccesses = new HashMap<>();
    for (StructFinder.Raw raw : raws)
      for (StructFinder.FieldAccess fa : raw.accesses())
        allAccesses.computeIfAbsent(fa.site(), k -> new ArrayList<>()).add(fa);

    List<Object> tipos = new ArrayList<>();
    for (int v : typeValues) {
      Map<String, Object> tipo = new LinkedHashMap<>();
      tipo.put("value", v);
      Set<Integer> sites = typeSites.get(v);
      Map<Integer, List<StructFinder.FieldAccess>> byOffset = new TreeMap<>();
      for (int s : sites)
        for (StructFinder.FieldAccess fa : allAccesses.getOrDefault(s, List.of()))
          byOffset.computeIfAbsent(fa.offset(), k -> new ArrayList<>()).add(fa);

      List<Object> campos = camposDe(byOffset, base, stride, tableEnd);
      List<Integer> siteList = new ArrayList<>(sites);
      List<Object> variantes = sf.variants(siteList, byOffset);
      sf.enrichRelations(campos, variantes, byOffset, siteList);

      int maxOff = byOffset.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
      int ocupa = maxOff / stride + 1;
      tipo.put("proposed_name", nombreDeTipo(campos, ocupa, sites.size()));
      if (framesPorValor.containsKey(v))
        tipo.put("observed_frames", framesPorValor.get(v));
      if (bitsExtra.getOrDefault(v, 0) != 0)
        tipo.put("bits_varying_outside_mask",
            "& " + bitsExtra.get(v) + " (" + sf.maskBits(bitsExtra.get(v)) + ")");
      if (ocupa > 1)
        tipo.put("spans_records", ocupa);
      Map<String, Object> donde = new LinkedHashMap<>();
      typeCond.get(v).forEach((rutina, cond) -> donde.put(rutina,
          cond + " x" + typeVeces.get(v).get(rutina)));
      tipo.put("selected_in", donde);
      tipo.put("fields", campos);
      // sub-variants inside the type (e.g. a direction bit) — only the field-conditioned ones
      List<Object> internas = new ArrayList<>();
      for (Object vo : variantes) {
        Map<String, Object> var = (Map<String, Object>) vo;
        if (((String) var.get("condition")).contains("&"))
          internas.add(var);
      }
      if (!internas.isEmpty())
        tipo.put("inner_variants", internas);
      tipos.add(tipo);
    }

    Map<String, Object> disc = new LinkedHashMap<>();
    disc.put("field", "+" + discOff);
    disc.put("mask", mask);
    disc.put("bits", sf.maskBits(mask));
    disc.put("observed_values", typeValues);
    List<Object> tests = new ArrayList<>();
    for (Ladder l : ladders)
      tests.add(Map.of("routine", l.rutina(),
          "mask", l.mask() < 0 ? 255 : l.mask(),
          "tested_values", l.tests().stream().map(r -> r.t().value()).toList()));
    disc.put("ladders", tests);

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("discriminant", disc);
    if (slotsConDatos > 0)
      out.put("slots_with_observed_data", slotsConDatos);
    out.put("types", tipos);
    return out;
  }

  private String condText(Test t) {
    return "field +" + t.offset() + (t.mask() >= 0 ? " & " + t.mask() : "") + " == " + t.value();
  }

  /** field summaries (same shape as StructFinder's campos) restricted to one type's accesses. */
  private List<Object> camposDe(Map<Integer, List<StructFinder.FieldAccess>> byOffset,
                                int base, int stride, int tableEnd) {
    List<Object> campos = new ArrayList<>();
    for (Map.Entry<Integer, List<StructFinder.FieldAccess>> oe : byOffset.entrySet()) {
      int off = oe.getKey();
      long reads = 0, writes = 0;
      int vMin = 256, vMax = -1;
      for (StructFinder.FieldAccess fa : oe.getValue()) {
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
      f.put("values", List.of(vMin <= vMax ? vMin : 0, vMax >= 0 ? vMax : 0));
      if (off >= stride)
        f.put("field_of_next_record", off % stride);
      List<String> tags = sf.semantics(base, off % stride, stride, tableEnd, oe.getValue());
      if (!tags.isEmpty())
        f.put("semantics", tags);
      List<String> bits = sf.fieldBits(oe.getValue());
      if (!bits.isEmpty())
        f.put("bit_decomposition", bits);
      List<Integer> gfx = sf.fieldGfxTarget(oe.getValue());
      if (gfx != null)
        f.put("points_to_graphics", gfx);
      campos.add(f);
    }
    return campos;
  }

  /**
   * behavioural name for the type: which coordinate axis its handler WRITES says how it
   * moves; offsets beyond the stride say it spills into the next slot; a handler with no
   * field writes is a slot with no behaviour of its own.
   */
  @SuppressWarnings("unchecked")
  private String nombreDeTipo(List<Object> campos, int ocupa, int nSites) {
    boolean wX = false, wY = false;
    long writes = 0;
    for (Object fo : campos) {
      Map<String, Object> f = (Map<String, Object>) fo;
      writes += (long) f.get("writes");
      if ((long) f.get("writes") == 0)
        continue;
      List<String> sem = f.containsKey("semantics") ? (List<String>) f.get("semantics") : List.of();
      if (sem.contains("X coordinate"))
        wX = true;
      if (sem.contains("Y coordinate"))
        wY = true;
    }
    String nombre;
    if (wX && wY)
      nombre = "xy_mover";
    else if (wX)
      nombre = "horizontal_mover";
    else if (wY)
      nombre = "vertical_mover";
    else if (writes > 0)
      nombre = "own_state";
    else if (nSites < 5)
      nombre = "no_behavior (empty slot or type without own code)";
    else
      nombre = "read_only";
    return ocupa > 1 ? nombre + "_extended" : nombre;
  }

  // ==================== ladder discovery ====================

  private List<Ladder> findLadders(StructFinder.Raw raw, Integer termValor) {
    Map<Integer, Integer> readSiteOffset = new HashMap<>();
    for (StructFinder.FieldAccess fa : raw.accesses())
      if (fa.op() == 'R')
        readSiteOffset.put(fa.site(), fa.offset());

    // collect equality tests conditioned on a field, grouped by (offset, mask)
    Map<Long, List<Test>> groups = new LinkedHashMap<>();
    for (int branchPc : raw.sites()) {
      if (!db.branchSites.contains(branchPc))
        continue;
      // one observed successor is fine: a ladder test whose type never occurred in the
      // replay simply never took the other arm (the type still exists, with no sites)
      List<AnalysisDB.Edge> succs = db.cfgOut.getOrDefault(branchPc, List.of());
      if (succs.isEmpty() || succs.size() > 2)
        continue;
      Test t = traceTest(branchPc, readSiteOffset);
      if (t == null)
        continue;
      // the terminator test compares the RAW value against the end marker — not a type
      if (termValor != null && t.mask() < 0 && t.value() == termValor)
        continue;
      // a masked test whose constant does not fit the mask can never be true — noise
      if (t.mask() >= 0 && (t.value() & ~t.mask()) != 0)
        continue;
      groups.computeIfAbsent(((long) t.offset() << 16) | (t.mask() & 0xffff), k -> new ArrayList<>())
          .add(t);
    }

    List<Ladder> out = new ArrayList<>();
    Set<Integer> methodSites = new HashSet<>(raw.sites());
    for (List<Test> tests : groups.values()) {
      Map<Integer, Long> distinct = new HashMap<>();
      for (Test t : tests)
        distinct.merge(t.value(), 1L, Long::sum);
      if (distinct.size() < 2)
        continue; // one test = a flag, not a type ladder
      Set<Integer> readSites = new HashSet<>();
      for (Test t : tests)
        readSites.add(t.readSite());
      List<Resolved> resolved = resolveArms(tests, methodSites, readSites);
      if (resolved.size() >= 2)
        out.add(new Ladder(db.method.getOrDefault(tests.get(0).branchPc(), "?"),
            raw.sites(), tests.get(0).offset(), tests.get(0).mask(), resolved, readSites));
    }
    return out;
  }

  /**
   * condition of a branch, walked back through the dataflow: the nearest {@code cp(A, c)}
   * gives the compared constant (a masked {@code flagZ} is the ==0 test), the nearest
   * AND-mask gives the sub-field, and the chain must reach a read of a record field.
   */
  private Test traceTest(int branchPc, Map<Integer, Integer> readSiteOffset) {
    // a type ladder tests for EQUALITY; threshold branches (if (F < 0)) compare
    // magnitudes (counters, coordinates) and never select a variant record
    String branchEq = db.equation.get(branchPc);
    if (branchEq == null || !(branchEq.contains("== 0") || branchEq.contains("!= 0")))
      return null;
    Integer value = null, maskC = null, offset = null, readSite = null;
    boolean flagZ = false;
    // walk up to four hops back of the branch (its own equation excluded), taking the first
    // compared constant, mask and record-field read the chain reaches
    for (int pc : Flow.back(db).from(branchPc).depth(4).sites()) {
      String eq = db.equation.get(pc);
      if (eq == null)
        continue;
      if (value == null) {
        Matcher cm = CMP.matcher(eq);
        if (cm.find())
          value = Integer.parseInt(cm.group(1));
      }
      if (eq.contains("flagZ("))
        flagZ = true;
      if (maskC == null) {
        Matcher mm = MASK.matcher(eq);
        if (mm.find()) {
          int mk = Integer.parseInt(mm.group(1));
          if (mk != 255) // & 255 is byte truncation, not a sub-field
            maskC = mk;
        }
      }
      if (offset == null && readSiteOffset.containsKey(pc)) {
        offset = readSiteOffset.get(pc);
        readSite = pc;
      }
    }
    if (offset == null)
      return null;
    if (value == null && flagZ && maskC != null)
      value = 0; // A &= m; flagZ  ==  "masked value == 0"
    if (value == null)
      return null;
    return new Test(branchPc, offset, maskC == null ? -1 : maskC, value, readSite);
  }

  /**
   * arm polarity by elimination: the not-equal arm of a ladder test falls through to the
   * NEXT test, so any arm whose closure contains another test of the same ladder is "ne".
   * Tests that elimination cannot resolve (the last one) take the polarity that the
   * resolved tests exhibit (ne = fall-through or ne = jump), marked as estimated.
   */
  private List<Resolved> resolveArms(List<Test> tests, Set<Integer> methodSites,
                                     Set<Integer> readSites) {
    Set<Integer> branchPcs = new HashSet<>();
    for (Test t : tests)
      branchPcs.add(t.branchPc());

    record Arms(Test t, int arm0, int arm1, long c0, long c1, Set<Integer> cl0, Set<Integer> cl1) {
    }
    List<Arms> arms = new ArrayList<>();
    List<Resolved> resolved = new ArrayList<>();
    for (Test t : tests) {
      List<AnalysisDB.Edge> succs = db.cfgOut.get(t.branchPc());
      if (succs.size() == 1) {
        // the other arm never ran: if the taken arm continues the ladder it is the
        // not-equal arm, and the equal arm is an (empty) never-executed handler
        Set<Integer> cl = closure(succs.get(0).dst(), methodSites, readSites);
        if (containsOther(cl, branchPcs, t.branchPc()))
          resolved.add(new Resolved(t, -1, succs.get(0).dst(), 0, succs.get(0).count(), false));
        continue; // otherwise unclassifiable — drop the test
      }
      Set<Integer> cl0 = closure(succs.get(0).dst(), methodSites, readSites);
      Set<Integer> cl1 = closure(succs.get(1).dst(), methodSites, readSites);
      arms.add(new Arms(t, succs.get(0).dst(), succs.get(1).dst(),
          succs.get(0).count(), succs.get(1).count(), cl0, cl1));
    }

    List<Arms> pending = new ArrayList<>();
    int neFallthroughVotes = 0, neJumpVotes = 0;
    for (Arms a : arms) {
      boolean in0 = containsOther(a.cl0(), branchPcs, a.t().branchPc());
      boolean in1 = containsOther(a.cl1(), branchPcs, a.t().branchPc());
      if (in0 == in1) {
        pending.add(a);
        continue;
      }
      int ne = in0 ? a.arm0() : a.arm1();
      int eq = in0 ? a.arm1() : a.arm0();
      long neC = in0 ? a.c0() : a.c1(), eqC = in0 ? a.c1() : a.c0();
      resolved.add(new Resolved(a.t(), eq, ne, eqC, neC, false));
      if (ne == fallthrough(a.t().branchPc(), a.arm0(), a.arm1()))
        neFallthroughVotes++;
      else
        neJumpVotes++;
    }
    boolean neIsFallthrough = neFallthroughVotes >= neJumpVotes;
    for (Arms a : pending) {
      int ft = fallthrough(a.t().branchPc(), a.arm0(), a.arm1());
      int ne = neIsFallthrough ? ft : (ft == a.arm0() ? a.arm1() : a.arm0());
      int eq = ne == a.arm0() ? a.arm1() : a.arm0();
      long neC = ne == a.arm0() ? a.c0() : a.c1(), eqC = ne == a.arm0() ? a.c1() : a.c0();
      resolved.add(new Resolved(a.t(), eq, ne, eqC, neC, true));
    }
    // ladder order: a test whose ne-closure contains another test precedes it
    resolved.sort((x, y) -> {
      Set<Integer> clx = closure(x.neArm(), methodSites, readSites);
      if (clx.contains(y.t().branchPc()))
        return -1;
      Set<Integer> cly = closure(y.neArm(), methodSites, readSites);
      if (cly.contains(x.t().branchPc()))
        return 1;
      return Integer.compare(x.t().branchPc(), y.t().branchPc());
    });
    return resolved;
  }

  private static boolean containsOther(Set<Integer> closure, Set<Integer> branchPcs, int self) {
    for (int pc : closure)
      if (pc != self && branchPcs.contains(pc))
        return true;
    return false;
  }

  /** the successor that is the textual fall-through: the nearest one forward. */
  private static int fallthrough(int branchPc, int arm0, int arm1) {
    boolean f0 = arm0 > branchPc, f1 = arm1 > branchPc;
    if (f0 && !f1)
      return arm0;
    if (f1 && !f0)
      return arm1;
    return Math.abs(arm0 - branchPc) <= Math.abs(arm1 - branchPc) ? arm0 : arm1;
  }

  /** the handler of one arm: its closure minus the sibling arm's closure. */
  private Set<Integer> exclusive(int arm, int sibling, Set<Integer> methodSites,
                                 Set<Integer> readSites) {
    Set<Integer> a = closure(arm, methodSites, readSites);
    a.removeAll(closure(sibling, methodSites, readSites));
    return a;
  }

  /**
   * forward CFG closure that does NOT expand through the discriminant read sites: the
   * sweep loop re-enters through them, and following that back-edge would wrap the
   * closure around the whole loop and dissolve the arm exclusivity.
   */
  private Set<Integer> closure(int start, Set<Integer> methodSites, Set<Integer> stopAt) {
    return Closure.cfg(db, start, methodSites, stopAt, 400);
  }

  // ==================== observed discriminant values ====================

  /**
   * per-frame values of the discriminant cell of every slot, from the track log. Returns
   * how many slots ever held a non-terminator value (observed capacity) and fills
   * frames-per-masked-value plus the OR of the bits each class shows OUTSIDE the mask
   * (direction/animation bits riding on the same byte).
   */
  private int observeDiscriminant(int base, int stride, int tableEnd, int discOff,
                                  Integer termValor, int mask,
                                  Map<Integer, Long> framesPorValor,
                                  Map<Integer, Integer> bitsExtra) {
    int slots = Math.min(64, (tableEnd - base) / stride + 4);
    int conDatos = 0;
    try (Db q = new Db(dbPath)) {
      for (int k = 0; k < slots; k++) {
        int addr = base + k * stride + discOff;
        boolean dato = false;
        for (long[] r : q.rows(
            "SELECT val, COUNT(*) FROM frame_cells WHERE addr = ? GROUP BY val", addr)) {
          int val = (int) r[0];
          long frames = r[1];
          if (termValor != null && val == termValor)
            continue;
          dato = true;
          framesPorValor.merge(val & mask, frames, Long::sum);
          bitsExtra.merge(val & mask, val & ~mask & 255, (x, y) -> x | y);
        }
        if (dato)
          conDatos++;
      }
    }
    return conDatos;
  }
}
