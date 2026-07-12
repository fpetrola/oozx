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

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * F1 of doc/GUIA-ANALISIS-ECUACIONES.md.
 * <p>
 * Reads JetSetWilly2.java (game code where every Z80 instruction is preceded by a
 * pc(NNNNN, len) call) and produces:
 * <ul>
 *   <li>JetSetWilly2Instrumented.java: identical semantics, but every direct
 *       {@code mem[idx] = v} / {@code mem[idx]} array access is rewritten to the
 *       already-existing overridable methods {@code wMem(idx, v, pc)} / {@code mem(idx, pc)},
 *       where pc is the Z80 address of the nearest preceding pc() call. The site ID of
 *       every operation is therefore the original Z80 instruction address.</li>
 *   <li>sites.json: the static equation table (one entry per memory access / ldir / in,
 *       keyed by Z80 pc) used later by the analysis layer.</li>
 * </ul>
 */
public class EquationExtractor {

  static final String DEFAULT_SRC = "translator/src/main/java/com/fpetrola/z80/bytecode/tests/JetSetWilly2.java";
  static final String DEFAULT_OUT_SRC_DIR = "translator/src/main/java";
  static final String DEFAULT_SITES_JSON = "translator/src/main/resources/analysis/sites.json";
  static final String TARGET_CLASS = "com.fpetrola.z80.bytecode.tests.JetSetWilly2";
  static final String GENERATED_PACKAGE = "com.fpetrola.z80.analysis.generated";
  static final String GENERATED_CLASS = "JetSetWilly2Instrumented";

  // roles estáticos por (site, canal): a qué destina el site cada dependencia que le llega
  static final int ROLE_ADDR = 1, ROLE_VAL = 2, ROLE_COND = 4;
  static final Map<String, String[]> REG_CH = new HashMap<>();

  static {
    for (String r : new String[]{"A", "F", "B", "C", "D", "E", "H", "L",
        "IXH", "IXL", "IYH", "IYL", "SP", "I", "R"})
      REG_CH.put(r, new String[]{r});
    REG_CH.put("AF", new String[]{"A", "F"});
    REG_CH.put("BC", new String[]{"B", "C"});
    REG_CH.put("DE", new String[]{"D", "E"});
    REG_CH.put("HL", new String[]{"H", "L"});
    REG_CH.put("IX", new String[]{"IXH", "IXL"});
    REG_CH.put("IY", new String[]{"IYH", "IYL"});
    REG_CH.put("AFx", new String[]{"AX", "FX"});
    REG_CH.put("BCx", new String[]{"BX", "CX"});
    REG_CH.put("DEx", new String[]{"DX", "EX"});
    REG_CH.put("HLx", new String[]{"HX", "LX"});
  }

  static class Site {
    final int pc;
    final String method;
    final int line;
    final String kind;
    final String index;
    final String value;
    final String stmt;

    Site(int pc, String method, int line, String kind, String index, String value, String stmt) {
      this.pc = pc;
      this.method = method;
      this.line = line;
      this.kind = kind;
      this.index = index;
      this.value = value;
      this.stmt = stmt;
    }
  }

  public static void main(String[] args) throws IOException {
    String src = args.length > 0 ? args[0] : DEFAULT_SRC;
    String outSrcDir = args.length > 1 ? args[1] : DEFAULT_OUT_SRC_DIR;
    String sitesJson = args.length > 2 ? args[2] : DEFAULT_SITES_JSON;

    Launcher launcher = new Launcher();
    launcher.addInputResource(src);
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setAutoImports(true);
    launcher.getEnvironment().setComplianceLevel(17);
    launcher.buildModel();
    CtModel model = launcher.getModel();

    CtClass<?> cls = model.getAllTypes().stream()
        .filter(t -> t.getQualifiedName().equals(TARGET_CLASS))
        .filter(t -> t instanceof CtClass<?>)
        .map(t -> (CtClass<?>) t)
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Class not found: " + TARGET_CLASS));
    Factory F = cls.getFactory();

    List<Site> sites = new ArrayList<>();
    Map<Integer, Map<String, Integer>> rolesByPc = new TreeMap<>();
    Map<Integer, TreeMap<Long, String>> eqByPc = new TreeMap<>();
    int readCount = 0, writeCount = 0;
    // global (line -> pc, method) markers for the per-instruction source equations (F2)
    TreeMap<Integer, int[]> pcByLine = new TreeMap<>();
    Map<Integer, String> methodOfLine = new HashMap<>();

    for (CtMethod<?> method : new ArrayList<>(cls.getMethods())) {
      if (!method.getSimpleName().startsWith("$") || method.getBody() == null)
        continue;

      // --- pass 1: pc(N, d) markers by source position ---------------------------------
      TreeMap<Long, Integer> pcByPos = new TreeMap<>();
      for (CtInvocation<?> inv : method.getBody().getElements(new TypeFilter<>(CtInvocation.class))) {
        if (inv.getExecutable() != null && "pc".equals(inv.getExecutable().getSimpleName())
            && inv.getArguments().size() == 2
            && inv.getArguments().get(0) instanceof CtLiteral<?> lit
            && lit.getValue() instanceof Integer address) {
          pcByPos.put(posKey(inv), address);
          if (inv.getPosition().isValidPosition()) {
            pcByLine.put(inv.getPosition().getLine(), new int[]{address});
            methodOfLine.put(inv.getPosition().getLine(), method.getSimpleName());
          }
        }
      }
      int methodAddr = parseMethodAddress(method.getSimpleName());

      // --- collect mem[] reads and writes, resolving their site BEFORE any mutation ----
      List<CtArrayRead<?>> reads = new ArrayList<>();
      for (CtArrayRead<?> r : method.getBody().getElements(new TypeFilter<>(CtArrayRead.class)))
        if (isMemAccess(r.getTarget()))
          reads.add(r);

      List<CtAssignment<?, ?>> writes = new ArrayList<>();
      for (CtAssignment<?, ?> a : method.getBody().getElements(new TypeFilter<>(CtAssignment.class)))
        if (a.getAssigned() instanceof CtArrayWrite<?> aw && isMemAccess(aw.getTarget()))
          writes.add(a);

      Map<CtElement, Integer> siteOf = new IdentityHashMap<>();
      for (CtArrayRead<?> r : reads)
        siteOf.put(r, siteFor(pcByPos, r, methodAddr));
      for (CtAssignment<?, ?> a : writes)
        siteOf.put(a, siteFor(pcByPos, a, methodAddr));

      // --- record sites.json entries (original, untransformed text) --------------------
      for (CtArrayRead<?> r : reads)
        sites.add(new Site(siteOf.get(r), method.getSimpleName(), line(r), "MEM_READ",
            r.getIndexExpression().toString(), null, enclosingStatementText(r)));
      for (CtAssignment<?, ?> a : writes) {
        CtArrayWrite<?> aw = (CtArrayWrite<?>) a.getAssigned();
        sites.add(new Site(siteOf.get(a), method.getSimpleName(), line(a), "MEM_WRITE",
            aw.getIndexExpression().toString(), a.getAssignment().toString(), a.toString()));
      }
      // ldir() and in(port, pc) sites: no transformation needed (captured at runtime by
      // overriding ldir()/in()), but they belong in the static site table.
      for (CtInvocation<?> inv : method.getBody().getElements(new TypeFilter<>(CtInvocation.class))) {
        if (inv.getExecutable() == null)
          continue;
        String name = inv.getExecutable().getSimpleName();
        if ("ldir".equals(name) && inv.getArguments().isEmpty())
          sites.add(new Site(siteFor(pcByPos, inv, methodAddr), method.getSimpleName(), line(inv),
              "BULK_COPY", null, null, "ldir()"));
        else if ("in".equals(name) && inv.getArguments().size() == 2
            && inv.getArguments().get(1) instanceof CtLiteral<?> lit
            && lit.getValue() instanceof Integer pcArg)
          sites.add(new Site(pcArg, method.getSimpleName(), line(inv),
              "IO_IN", inv.getArguments().get(0).toString(), null, inv.toString()));
      }

      // --- pass 1.6: static roles — intra-site def-use following the varN locals -------
      // (must run BEFORE pass 2: it needs the original AST and source positions)
      Map<String, Set<String>> varDeps = new HashMap<>();
      List<CtLocalVariable<?>> locals =
          new ArrayList<>(method.getBody().getElements(new TypeFilter<>(CtLocalVariable.class)));
      locals.sort(Comparator.comparingLong(EquationExtractor::posKey));
      for (CtLocalVariable<?> lv : locals)
        if (lv.getDefaultExpression() != null)
          varDeps.put(lv.getSimpleName(),
              channels(lv.getDefaultExpression(), varDeps, pcByPos, methodAddr, rolesByPc));
      for (CtAssignment<?, ?> a : method.getBody().getElements(new TypeFilter<>(CtAssignment.class)))
        if (a.getAssigned() instanceof CtVariableWrite<?> vw)
          varDeps.merge(vw.getVariable().getSimpleName(),
              channels(a.getAssignment(), varDeps, pcByPos, methodAddr, rolesByPc),
              (x, y) -> {
                Set<String> u = new HashSet<>(x);
                u.addAll(y);
                return u;
              });
      for (CtArrayRead<?> r : reads)
        addRole(rolesByPc, siteOf.get(r),
            channels(r.getIndexExpression(), varDeps, pcByPos, methodAddr, rolesByPc), ROLE_ADDR);
      for (CtAssignment<?, ?> a : writes) {
        CtArrayWrite<?> aw = (CtArrayWrite<?>) a.getAssigned();
        addRole(rolesByPc, siteOf.get(a),
            channels(aw.getIndexExpression(), varDeps, pcByPos, methodAddr, rolesByPc), ROLE_ADDR);
        addRole(rolesByPc, siteOf.get(a),
            channels(a.getAssignment(), varDeps, pcByPos, methodAddr, rolesByPc), ROLE_VAL);
      }
      for (CtInvocation<?> inv : method.getBody().getElements(new TypeFilter<>(CtInvocation.class))) {
        if (inv.getExecutable() == null)
          continue;
        String name = inv.getExecutable().getSimpleName();
        int site = siteFor(pcByPos, inv, methodAddr);
        if (inv.getArguments().size() == 1 && (REG_CH.containsKey(name) || "push".equals(name)))
          addRole(rolesByPc, site,
              channels(inv.getArguments().get(0), varDeps, pcByPos, methodAddr, rolesByPc), ROLE_VAL);
        else if ("ldir".equals(name) && inv.getArguments().isEmpty()) {
          addRole(rolesByPc, site, Set.of("H", "L", "D", "E"), ROLE_ADDR);
          addRole(rolesByPc, site, Set.of("B", "C"), ROLE_COND);
          addRole(rolesByPc, site, Set.of("MEM"), ROLE_VAL);
        }
      }
      // --- pass 1.7: normalized algebraic equations (inline the varN chains) -----------
      Map<String, String> varSym = new HashMap<>();
      for (CtLocalVariable<?> lv : locals)
        if (lv.getDefaultExpression() != null)
          varSym.put(lv.getSimpleName(), sym(lv.getDefaultExpression(), varSym));
      List<CtAssignment<?, ?>> varAsgs = new ArrayList<>();
      for (CtAssignment<?, ?> a : method.getBody().getElements(new TypeFilter<>(CtAssignment.class)))
        if (a.getAssigned() instanceof CtVariableWrite<?>)
          varAsgs.add(a);
      varAsgs.sort(Comparator.comparingLong(EquationExtractor::posKey));
      for (CtAssignment<?, ?> a : varAsgs)
        varSym.put(((CtVariableWrite<?>) a.getAssigned()).getVariable().getSimpleName(),
            sym(a.getAssignment(), varSym));

      for (CtInvocation<?> inv : method.getBody().getElements(new TypeFilter<>(CtInvocation.class))) {
        if (inv.getExecutable() == null)
          continue;
        String name = inv.getExecutable().getSimpleName();
        int site = siteFor(pcByPos, inv, methodAddr);
        if (inv.getArguments().size() == 1 && REG_CH.containsKey(name))
          effect(eqByPc, site, posKey(inv), name + " = " + sym(inv.getArguments().get(0), varSym));
        else if (inv.getArguments().size() == 1 && "push".equals(name))
          effect(eqByPc, site, posKey(inv), "push(" + sym(inv.getArguments().get(0), varSym) + ")");
        else if (inv.getArguments().isEmpty() && "ldir".equals(name))
          effect(eqByPc, site, posKey(inv), "ldir: mem[DE..DE+BC-1] = mem[HL..HL+BC-1]");
        else if (inv.getArguments().isEmpty() && name.startsWith("$"))
          effect(eqByPc, site, posKey(inv), "call " + name);
      }
      for (CtAssignment<?, ?> a : writes) {
        CtArrayWrite<?> aw = (CtArrayWrite<?>) a.getAssigned();
        effect(eqByPc, siteOf.get(a), posKey(a),
            "mem[" + sym(aw.getIndexExpression(), varSym) + "] = " + sym(a.getAssignment(), varSym));
      }

      // --- branch/loop conditions: COND role + normalized equation ---------------------
      List<Object[]> condExprs = new ArrayList<>();
      for (CtIf s : method.getBody().getElements(new TypeFilter<>(CtIf.class)))
        condExprs.add(new Object[]{s.getCondition(), "if"});
      for (CtWhile s : method.getBody().getElements(new TypeFilter<>(CtWhile.class)))
        condExprs.add(new Object[]{s.getLoopingExpression(), "while"});
      for (CtDo s : method.getBody().getElements(new TypeFilter<>(CtDo.class)))
        condExprs.add(new Object[]{s.getLoopingExpression(), "while"});
      for (Object[] ce : condExprs) {
        CtExpression<?> cond = (CtExpression<?>) ce[0];
        if (cond == null)
          continue;
        int site = siteFor(pcByPos, cond, methodAddr);
        addRole(rolesByPc, site, channels(cond, varDeps, pcByPos, methodAddr, rolesByPc), ROLE_COND);
        effect(eqByPc, site, posKey(cond), ce[1] + " (" + sym(cond, varSym) + ")");
      }

      // --- pass 2: replace reads (deepest first so nested reads print correctly) -------
      reads.sort(Comparator.comparingInt(EquationExtractor::depth).reversed());
      for (CtArrayRead<?> r : reads) {
        int pc = siteOf.get(r);
        CtCodeSnippetExpression<?> sn =
            F.Code().createCodeSnippetExpression("mem(" + r.getIndexExpression() + ", " + pc + ")");
        r.replace(sn);
        readCount++;
      }
      // --- then writes (their RHS/index already contain the replaced read snippets) ----
      for (CtAssignment<?, ?> a : writes) {
        int pc = siteOf.get(a);
        CtArrayWrite<?> aw = (CtArrayWrite<?>) a.getAssigned();
        CtCodeSnippetStatement sn = F.Code().createCodeSnippetStatement(
            "wMem(" + aw.getIndexExpression() + ", " + a.getAssignment() + ", " + pc + ")");
        a.replace(sn);
        writeCount++;
      }
    }

    // --- F2: per-instruction source equations + branch sites (from the raw source) -----
    List<String> rawLines = Files.readAllLines(Path.of(src));
    List<Integer> markerLines = new ArrayList<>(pcByLine.keySet());
    for (int i = 0; i < markerLines.size(); i++) {
      int lineNo = markerLines.get(i);
      int pcVal = pcByLine.get(lineNo)[0];
      int endLine = i + 1 < markerLines.size() ? markerLines.get(i + 1) : Math.min(lineNo + 9, rawLines.size() + 1);
      StringBuilder text = new StringBuilder();
      int taken = 0;
      for (int ln = lineNo + 1; ln < endLine && ln <= rawLines.size() && taken < 8; ln++) {
        String t = rawLines.get(ln - 1).trim();
        if (t.isEmpty() || t.equals("}") || t.startsWith("public ") || t.startsWith("//") || t.startsWith("label"))
          continue;
        if (text.length() > 0)
          text.append(' ');
        text.append(t);
        taken++;
      }
      String stmt = text.toString();
      if (stmt.isEmpty())
        continue;
      boolean branch = stmt.contains("if (F()") || stmt.contains("while (F()")
          || stmt.contains("if ((F()") || stmt.contains("while ((F()");
      sites.add(new Site(pcVal, methodOfLine.get(lineNo), lineNo,
          branch ? "BRANCH" : "INSTR", null, null, stmt));
    }

    // --- rename + move to the generated package ---------------------------------------
    cls.getPackage().removeType(cls);
    cls.setSimpleName(GENERATED_CLASS);
    CtPackage generated = F.Package().getOrCreate(GENERATED_PACKAGE);
    generated.addType(cls);

    launcher.setSourceOutputDirectory(outSrcDir);
    launcher.prettyprint();

    writeSitesJson(sites, rolesByPc, eqByPc, Path.of(sitesJson));

    System.out.println("Instrumented class: " + outSrcDir + "/" + GENERATED_PACKAGE.replace('.', '/')
        + "/" + GENERATED_CLASS + ".java");
    System.out.println("Rewritten mem[] reads: " + readCount + ", writes: " + writeCount);
    System.out.println("Sites: " + sites.size() + " -> " + sitesJson);
  }

  private static boolean isMemAccess(CtExpression<?> target) {
    return target != null && "mem".equals(target.toString());
  }

  /** channels (register names / MEM / STK) that feed the given expression. */
  private static Set<String> channels(CtElement e, Map<String, Set<String>> varDeps,
                                      TreeMap<Long, Integer> pcByPos, int methodAddr,
                                      Map<Integer, Map<String, Integer>> roles) {
    Set<String> out = new HashSet<>();
    collectChannels(e, out, varDeps, pcByPos, methodAddr, roles);
    return out;
  }

  private static void collectChannels(CtElement e, Set<String> out, Map<String, Set<String>> varDeps,
                                      TreeMap<Long, Integer> pcByPos, int methodAddr,
                                      Map<Integer, Map<String, Integer>> roles) {
    if (e instanceof CtInvocation<?> inv && inv.getExecutable() != null) {
      String name = inv.getExecutable().getSimpleName();
      if (inv.getArguments().isEmpty()) {
        if (REG_CH.containsKey(name)) {
          out.addAll(Arrays.asList(REG_CH.get(name)));
          return;
        }
        if ("pop".equals(name)) {
          out.add("STK");
          return;
        }
      }
      for (CtExpression<?> arg : inv.getArguments())
        collectChannels(arg, out, varDeps, pcByPos, methodAddr, roles);
      return;
    }
    // a mem read contributes through the MEM channel; whatever fed its index is
    // an ADDR dependency of this same site — recorded right here, not propagated up
    if (e instanceof CtArrayRead<?> r && isMemAccess(r.getTarget())) {
      addRole(roles, siteFor(pcByPos, r, methodAddr),
          channels(r.getIndexExpression(), varDeps, pcByPos, methodAddr, roles), ROLE_ADDR);
      out.add("MEM");
      return;
    }
    if (e instanceof CtVariableRead<?> vr) {
      Set<String> d = varDeps.get(vr.getVariable().getSimpleName());
      if (d != null)
        out.addAll(d);
      return;
    }
    for (CtElement c : e.getDirectChildren())
      collectChannels(c, out, varDeps, pcByPos, methodAddr, roles);
  }

  /** symbolic pretty-print of an expression with the varN locals inlined. */
  private static String sym(CtElement e, Map<String, String> varSym) {
    if (e instanceof CtInvocation<?> inv && inv.getExecutable() != null) {
      String name = inv.getExecutable().getSimpleName();
      if (inv.getArguments().isEmpty() && REG_CH.containsKey(name))
        return name;
      if (inv.getArguments().isEmpty() && "pop".equals(name))
        return "pop()";
      if (inv.getArguments().size() == 2 && "in".equals(name))
        return "in(" + sym(inv.getArguments().get(0), varSym) + ")";
      StringBuilder sb = new StringBuilder(name).append('(');
      for (int i = 0; i < inv.getArguments().size(); i++) {
        if (i > 0)
          sb.append(", ");
        sb.append(sym(inv.getArguments().get(i), varSym));
      }
      return sb.append(')').toString();
    }
    if (e instanceof CtArrayRead<?> r && isMemAccess(r.getTarget()))
      return "mem[" + sym(r.getIndexExpression(), varSym) + "]";
    if (e instanceof CtVariableRead<?> vr) {
      String s = varSym.get(vr.getVariable().getSimpleName());
      return s != null ? s : vr.getVariable().getSimpleName();
    }
    if (e instanceof CtBinaryOperator<?> b)
      return wrap(b.getLeftHandOperand(), varSym) + " " + opText(b.getKind()) + " "
          + wrap(b.getRightHandOperand(), varSym);
    if (e instanceof CtUnaryOperator<?> u) {
      String s = wrap(u.getOperand(), varSym);
      return switch (u.getKind()) {
        case NOT -> "!" + s;
        case NEG -> "-" + s;
        case COMPL -> "~" + s;
        case POSTINC, PREINC -> s + "++";
        case POSTDEC, PREDEC -> s + "--";
        default -> u.toString();
      };
    }
    if (e instanceof CtLiteral<?> l)
      return String.valueOf(l.getValue());
    if (e instanceof CtConditional<?> c)
      return wrap(c.getCondition(), varSym) + " ? " + sym(c.getThenExpression(), varSym)
          + " : " + sym(c.getElseExpression(), varSym);
    return e.toString();
  }

  private static String wrap(CtExpression<?> e, Map<String, String> varSym) {
    String s = sym(e, varSym);
    boolean composite = e instanceof CtBinaryOperator<?> || e instanceof CtConditional<?>
        || (e instanceof CtVariableRead<?> && s.contains(" "));
    return composite ? "(" + s + ")" : s;
  }

  private static String opText(BinaryOperatorKind k) {
    return switch (k) {
      case PLUS -> "+"; case MINUS -> "-"; case MUL -> "*"; case DIV -> "/"; case MOD -> "%";
      case BITAND -> "&"; case BITOR -> "|"; case BITXOR -> "^";
      case SL -> "<<"; case SR -> ">>"; case USR -> ">>>";
      case EQ -> "=="; case NE -> "!="; case LT -> "<"; case GT -> ">"; case LE -> "<="; case GE -> ">=";
      case AND -> "&&"; case OR -> "||";
      default -> k.toString();
    };
  }

  private static void effect(Map<Integer, TreeMap<Long, String>> eqByPc, int pc, long pos, String text) {
    if (pc < 0)
      return;
    eqByPc.computeIfAbsent(pc, k -> new TreeMap<>()).put(pos, text);
  }

  private static void addRole(Map<Integer, Map<String, Integer>> roles, int pc, Set<String> chs, int role) {
    if (pc < 0 || chs.isEmpty())
      return;
    Map<String, Integer> m = roles.computeIfAbsent(pc, k -> new TreeMap<>());
    for (String ch : chs)
      m.merge(ch, role, (a, b) -> a | b);
  }

  private static String rolesToString(Map<String, Integer> chRoles) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Integer> e : chRoles.entrySet()) {
      if (sb.length() > 0)
        sb.append(';');
      sb.append(e.getKey()).append('=');
      int m = e.getValue();
      if ((m & ROLE_ADDR) != 0) sb.append('A');
      if ((m & ROLE_VAL) != 0) sb.append('V');
      if ((m & ROLE_COND) != 0) sb.append('C');
    }
    return sb.toString();
  }

  private static int siteFor(TreeMap<Long, Integer> pcByPos, CtElement e, int methodAddr) {
    Map.Entry<Long, Integer> floor = pcByPos.floorEntry(posKey(e));
    return floor != null ? floor.getValue() : methodAddr;
  }

  private static long posKey(CtElement e) {
    SourcePosition p = e.getPosition();
    if (!p.isValidPosition())
      return -1;
    return (long) p.getLine() * 10000 + Math.max(p.getColumn(), 0);
  }

  private static int line(CtElement e) {
    SourcePosition p = e.getPosition();
    return p.isValidPosition() ? p.getLine() : -1;
  }

  private static int depth(CtElement e) {
    int d = 0;
    for (CtElement c = e; c.isParentInitialized(); c = c.getParent())
      d++;
    return d;
  }

  private static int parseMethodAddress(String name) {
    try {
      return Integer.parseInt(name.substring(1));
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static String enclosingStatementText(CtElement e) {
    CtElement c = e;
    while (c.isParentInitialized() && !(c.getParent() instanceof CtBlock<?>))
      c = c.getParent();
    return c.toString().replace('\n', ' ');
  }

  private static void writeSitesJson(List<Site> sites, Map<Integer, Map<String, Integer>> rolesByPc,
                                     Map<Integer, TreeMap<Long, String>> eqByPc,
                                     Path out) throws IOException {
    sites.sort(Comparator.comparingInt((Site s) -> s.pc).thenComparing(s -> s.kind));
    Set<Integer> rolesEmitted = new HashSet<>(), eqEmitted = new HashSet<>();
    StringBuilder sb = new StringBuilder("[\n");
    for (int i = 0; i < sites.size(); i++) {
      Site s = sites.get(i);
      sb.append("  {\"pc\": ").append(s.pc)
          .append(", \"method\": ").append(json(s.method))
          .append(", \"line\": ").append(s.line)
          .append(", \"kind\": ").append(json(s.kind));
      if (s.index != null)
        sb.append(", \"index\": ").append(json(s.index));
      if (s.value != null)
        sb.append(", \"value\": ").append(json(s.value));
      if (s.stmt != null)
        sb.append(", \"stmt\": ").append(json(s.stmt));
      if (rolesByPc.containsKey(s.pc) && rolesEmitted.add(s.pc))
        sb.append(", \"roles\": ").append(json(rolesToString(rolesByPc.get(s.pc))));
      if (eqByPc.containsKey(s.pc) && eqEmitted.add(s.pc))
        sb.append(", \"equation\": ").append(json(String.join("; ", eqByPc.get(s.pc).values())));
      sb.append("}").append(i < sites.size() - 1 ? "," : "").append("\n");
    }
    sb.append("]\n");
    Files.createDirectories(out.getParent());
    Files.writeString(out, sb.toString());
  }

  private static String json(String s) {
    return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
  }
}
