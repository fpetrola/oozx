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
 */
public class StructFinder {
  private static final Pattern FIELD = Pattern.compile("mem\\[(I[XY]) \\+ (\\d+)\\]");
  private static final Pattern CMP = Pattern.compile("cp\\([A-Z], (\\d+)\\)");
  private static final Pattern MASK = Pattern.compile("[A-Z] = [A-Z] & (\\d+)");

  private final AnalysisDB db;
  private final Map<Integer, Character> coordAxis = new HashMap<>();
  private final List<int[]> gfxRegions;

  public StructFinder(AnalysisDB db, String dbPath) {
    this.db = db;
    Explainer explainer = new Explainer(db, dbPath);
    try (java.sql.Connection c = java.sql.DriverManager.getConnection("jdbc:sqlite:" + dbPath);
         java.sql.ResultSet rs = c.createStatement().executeQuery(
             "SELECT addr, axis FROM coord_cells WHERE rate >= 0.3")) {
      while (rs.next())
        coordAxis.putIfAbsent(rs.getInt(1), rs.getString(2).charAt(0));
    } catch (java.sql.SQLException ignored) {
    }
    CoordinateFinder.Plan plan = new CoordinateFinder(db).find();
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

  public void report(String methodFilter) {
    Map<String, List<Integer>> byMethod = new TreeMap<>();
    db.method.forEach((pc, m) -> byMethod.computeIfAbsent(m, k -> new ArrayList<>()).add(pc));
    for (Map.Entry<String, List<Integer>> me : byMethod.entrySet()) {
      if (methodFilter != null && !me.getKey().contains(methodFilter))
        continue;
      analyzeMethod(me.getKey(), me.getValue());
    }
  }

  private record FieldAccess(int site, char op, int offset) {
  }

  private void analyzeMethod(String method, List<Integer> sites) {
    // collect mem[IX+k] / mem[IY+k] accesses per index register
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
    if (byReg.isEmpty())
      return;

    for (Map.Entry<String, List<FieldAccess>> re : byReg.entrySet()) {
      List<FieldAccess> accesses = re.getValue();
      if (accesses.stream().mapToInt(FieldAccess::offset).distinct().count() < 2)
        continue; // one lone field is not a structure

      // geometry from observed addresses: stride first (lowest varying address bit),
      // then base/end folding offsets >= stride (those reach the NEXT element: +9 in an
      // 8-byte record is really field +1 of element+1)
      int stride = 0;
      for (FieldAccess fa : accesses) {
        AnalysisDB.Stat s = (fa.op() == 'W' ? db.writes : db.reads).get(fa.site());
        if (s == null)
          continue;
        int varying = s.addrAnd() ^ s.addrOr();
        if (varying != 0) {
          int lowest = Integer.lowestOneBit(varying);
          stride = stride == 0 ? lowest : Math.min(stride, lowest);
        }
      }
      if (stride == 0)
        stride = accesses.stream().mapToInt(FieldAccess::offset).max().orElse(0) + 1;
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
        continue;
      int elems = (end - base) / stride + 1;
      int tableEnd = base + stride * elems - 1;

      // cross-check: the cursor increment parsed from the equations, if present
      String incNote = "";
      for (int pc : sites) {
        String eq = db.equation.get(pc);
        if (eq != null && eq.matches(".*" + re.getKey() + " = add16\\(" + re.getKey() + ", \\d+\\).*")) {
          Matcher m = Pattern.compile("add16\\(" + re.getKey() + ", (\\d+)\\)").matcher(eq);
          if (m.find())
            incNote = "  (avance del cursor: +" + m.group(1) + " @" + pc + ")";
        }
      }

      System.out.printf("%n=== %s via %s: ARREGLO base=%d, registro de %d bytes, %d elementos [%d..%d]%s ===%n",
          method, re.getKey(), base, stride, elems, base, end + stride - 1, incNote);

      // fields with semantics
      Map<Integer, List<FieldAccess>> byOffset = new TreeMap<>();
      for (FieldAccess fa : accesses)
        byOffset.computeIfAbsent(fa.offset(), k -> new ArrayList<>()).add(fa);
      for (Map.Entry<Integer, List<FieldAccess>> oe : byOffset.entrySet()) {
        int off = oe.getKey();
        StringBuilder sb = new StringBuilder(String.format("  +%d:", off));
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
        sb.append(String.format(" R x%d, W x%d, val[%d..%d]", reads, writes, vMin, vMax));
        if (off >= stride)
          sb.append(String.format("  (= +%d del elemento siguiente)", off % stride));
        sb.append(semantics(base, off % stride, stride, tableEnd, oe.getValue()));
        System.out.println(sb);
      }

      variants(method, sites, byOffset);
    }
  }

  /** what the field feeds, from its outgoing edges and the track annotations. */
  private String semantics(int base, int off, int stride, int tableEnd, List<FieldAccess> fas) {
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
          // does the consumer read the graphics zones?
          AnalysisDB.Stat r = db.reads.get(e.dst());
          if (r != null && gfxRegions.stream().anyMatch(g -> r.addrMax() >= g[0] && r.addrMin() <= g[1]))
            addrGfx = true;
          else
            addrOther = true;
        }
      }
      // second hop for COND: the field often goes through a mask/cp first
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
    return tags.isEmpty() ? "" : "  <- " + String.join(", ", tags);
  }

  /**
   * variant layouts: for each branch whose condition traces back to a field read, walk
   * both CFG arms; the fields accessed EXCLUSIVELY inside one arm belong to that variant.
   */
  private void variants(String method, List<Integer> sites, Map<Integer, List<FieldAccess>> byOffset) {
    Set<Integer> methodSites = new HashSet<>(sites);
    Map<Integer, Integer> siteToOffset = new HashMap<>();
    byOffset.forEach((off, fas) -> fas.forEach(fa -> siteToOffset.put(fa.site(), off)));

    for (int branchPc : sites) {
      if (!db.branchSites.contains(branchPc))
        continue;
      List<AnalysisDB.Edge> succs = db.cfgOut.getOrDefault(branchPc, List.of());
      if (succs.size() != 2)
        continue;
      // does the branch condition come (<=3 hops) from a field of this structure?
      Integer srcOffset = condSourceOffset(branchPc, siteToOffset, 3, new HashSet<>());
      if (srcOffset == null)
        continue;
      String condDesc = describeCondition(branchPc, srcOffset);

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
      System.out.printf("  VARIANTE en @%d (%s): x%d / x%d%n", branchPc, condDesc,
          succs.get(0).count(), succs.get(1).count());
      if (!onlyA.isEmpty())
        System.out.println("    rama @" + succs.get(0).dst() + " usa exclusivamente: +"
            + onlyA.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse(""));
      if (!onlyB.isEmpty())
        System.out.println("    rama @" + succs.get(1).dst() + " usa exclusivamente: +"
            + onlyB.stream().map(String::valueOf).reduce((a, b) -> a + " +" + b).orElse(""));
    }
  }

  /** walks the COND input chain of a branch back to a field read of this structure. */
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
}
