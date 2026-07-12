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
    int readCount = 0, writeCount = 0;

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

    // --- rename + move to the generated package ---------------------------------------
    cls.getPackage().removeType(cls);
    cls.setSimpleName(GENERATED_CLASS);
    CtPackage generated = F.Package().getOrCreate(GENERATED_PACKAGE);
    generated.addType(cls);

    launcher.setSourceOutputDirectory(outSrcDir);
    launcher.prettyprint();

    writeSitesJson(sites, Path.of(sitesJson));

    System.out.println("Instrumented class: " + outSrcDir + "/" + GENERATED_PACKAGE.replace('.', '/')
        + "/" + GENERATED_CLASS + ".java");
    System.out.println("Rewritten mem[] reads: " + readCount + ", writes: " + writeCount);
    System.out.println("Sites: " + sites.size() + " -> " + sitesJson);
  }

  private static boolean isMemAccess(CtExpression<?> target) {
    return target != null && "mem".equals(target.toString());
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

  private static void writeSitesJson(List<Site> sites, Path out) throws IOException {
    sites.sort(Comparator.comparingInt((Site s) -> s.pc).thenComparing(s -> s.kind));
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
