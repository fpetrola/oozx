/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
package com.fpetrola.z80.generate;

import com.fpetrola.oozx.fuse.modules.z80.TestFusePhaseProcessor;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.decoder.DefaultFetchNextOpcodeInstruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.UnrolledRegisterBank;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import fuse.tstates.Contention;
import fuse.tstates.PhaseProcessor;

import java.util.*;

/**
 * Lays the specialized instructions out as the switch cascade of a generated core: one method
 * per opcode table, one case per instruction, the MEMPTR and contention aspects woven in.
 */
public class CoreGenerator {
  private final State state;
  private final Instruction[] root;
  private final Specializer spec;
  private final Specializer.Obj memptrUpdater;
  private final PhaseProcessor contention;
  private final List<Case> cases = new ArrayList<>();
  private final Map<String, List<String>> parametersOf = new LinkedHashMap<>();
  private final Set<String> assigned = new HashSet<>();
  private final Set<String> read = new HashSet<>();
  private final Set<String> fields = new LinkedHashSet<>();
  private final Map<String, StringBuilder> methodBodies = new LinkedHashMap<>();

  private record Case(String method, int opcode, List<Statement> body, String comment) {
  }

  public CoreGenerator(State state, Instruction[] root, SourceIndex index) {
    this.state = state;
    this.root = root;
    this.spec = new Specializer(index);
    this.memptrUpdater = spec.of(new MemptrUpdater(state.getMemptr(), state.getMemory()));
    this.contention = new TestFusePhaseProcessor(state, event -> {
    });
  }

  public String generate() {
    FetchedInstructionWrapper[] wrappers = new FetchedInstructionWrapper[0x100];
    MultiOpcodeFetcher.wrapInstructions(root, 0, state.getPc(), wrappers, -1, false, state.getMemory());
    List<Statement> step = new ArrayList<>();
    spec.newCase();
    step.addAll(spec.statementsOf(spec.of(state.getRegisterR()), "increment"));
    step.add(call("decode", read(new NameExpr("PC"), 1)));
    parametersOf.put("decode", List.of());
    table("decode", wrappers, null);
    classify();
    StringBuilder out = new StringBuilder();
    out.append("package com.fpetrola.z80.cpu;\n\n");
    Set<String> imports = new TreeSet<>(spec.imports);
    imports.addAll(List.of("com.fpetrola.z80.memory.Memory", "com.fpetrola.z80.registers.UnrolledRegisterBank", "fuse.tstates.Contention"));
    for (String i : imports)
      out.append("import ").append(i).append(";\n");
    out.append("\n/** Generated from the OOP model by GenerateZ80: the instructions, MEMPTR and the contention, flattened. Do not edit; regenerate. */\n");
    out.append("public abstract class GeneratedZ80 extends UnrolledRegisterBank {\n");
    out.append("  protected final Memory memory;\n  protected final IO io;\n  protected State state;\n\n");
    for (Class<?> support : spec.staticSupport) {
      TypeDeclaration<?> type = spec.index.type(support);
      for (var member : type.getMembers()) {
        if (member instanceof FieldDeclaration f && f.isStatic())
          out.append("  ").append(f.toString().replace("private ", "").replace("protected ", "")).append("\n");
        if (member instanceof InitializerDeclaration init && init.isStatic())
          out.append("  ").append(init.toString().replace("\n", "\n  ")).append("\n");
      }
    }
    out.append("\n");
    for (String field : fields) {
      Specializer.Slot slot = slotNamed(field);
      out.append("  private ").append(slot.type().getSimpleName()).append(' ').append(field).append(" = ").append(literal(slot.initial())).append(";\n");
    }
    out.append("\n  public GeneratedZ80(Memory memory, IO io) {\n    this.memory = memory;\n    this.io = io;\n  }\n\n");
    out.append("  public void attach(State state) {\n    this.state = state;\n  }\n\n");
    out.append("  public abstract void contend(int address, int times, int tstates, Contention.Kind kind);\n\n");
    out.append("  public void step() ").append(new BlockStmt(new NodeList<>(step)).toString().replace("\n", "\n  ")).append("\n\n");
    for (String method : parametersOf.keySet()) {
      String parameters = parametersOf.get(method).stream().map(p -> ", int " + p).reduce("", String::concat);
      String arguments = parametersOf.get(method).stream().map(p -> ", " + p).reduce("", String::concat);
      out.append("  private void ").append(method).append("(int opcode").append(parameters).append(") {\n    switch (opcode >> ").append(GROUP_SHIFT).append(") {\n");
      for (int group = 0; group < 256 >> GROUP_SHIFT; group++)
        if (methodBodies.containsKey(method + "_" + group))
          out.append("      case ").append(group).append(": ").append(method).append("_").append(group).append("(opcode").append(arguments).append(");\n        break;\n");
      out.append("      default:\n        throw new IllegalStateException(\"undefined opcode \" + opcode + \" in ").append(method).append("\");\n    }\n  }\n\n");
      for (int group = 0; group < 256 >> GROUP_SHIFT; group++) {
        StringBuilder body = methodBodies.get(method + "_" + group);
        if (body != null)
          out.append(body).append("      default:\n        throw new IllegalStateException(\"undefined opcode \" + opcode + \" in ").append(method).append("\");\n    }\n  }\n\n");
      }
    }
    out.append("}\n");
    return out.toString();
  }

  /**
   * HotSpot does not compile a method over 8000 bytes of bytecode, and sixteen cases stay well
   * under it. Measured against the alternatives, interleaved, on a bare array: 32 cases per method
   * is the same to within the noise, 8 cases and one case per method are both about 13 percent
   * slower. It stays settable because inlining the accesses will make the methods grow, and this
   * is what regulates them; changing it changes the generated file, which the lock will say.
   */
  private static final int GROUP_SHIFT = Integer.getInteger("oozx.groupshift", 4);

  private void table(String method, FetchedInstructionWrapper[] wrappers, Expression preRead) {
    for (int opcode = 0; opcode < wrappers.length; opcode++) {
      FetchedInstructionWrapper wrapper = wrappers[opcode];
      if (wrapper == null)
        continue;
      if (wrapper instanceof FetchNextOpcodeInstructionWrapper prefix)
        cases.add(new Case(method, opcode, prefix(method, prefix), null));
      else
        cases.add(new Case(method, opcode, leaf(wrapper, preRead), null));
    }
  }

  private List<Statement> prefix(String parent, FetchNextOpcodeInstructionWrapper wrapper) {
    DefaultFetchNextOpcodeInstruction fetch = (DefaultFetchNextOpcodeInstruction) Specializer.get(Specializer.findField(FetchNextOpcodeInstructionWrapper.class, "instruction"), wrapper);
    int increment = (int) Specializer.get(Specializer.findField(DefaultFetchNextOpcodeInstruction.class, "increment"), fetch);
    int incPc = (int) Specializer.get(Specializer.findField(DefaultFetchNextOpcodeInstruction.class, "incPc"), fetch);
    boolean incrementR = (boolean) Specializer.get(Specializer.findField(DefaultFetchNextOpcodeInstruction.class, "incrementR"), fetch);
    String name = (String) Specializer.get(Specializer.findField(DefaultFetchNextOpcodeInstruction.class, "name"), fetch);
    FetchedInstructionWrapper[] wrappers = (FetchedInstructionWrapper[]) Specializer.get(Specializer.findField(DefaultFetchNextOpcodeInstruction.class, "wrappers"), fetch);
    String method = name.equals("DDFDCB") ? parent + "CB" : "decode" + name;
    List<Statement> body = new ArrayList<>();
    spec.newCase();
    if (incrementR)
      body.addAll(spec.statementsOf(spec.of(state.getRegisterR()), "increment"));
    Expression preRead = incPc == 2 ? pcPlus(increment - 1) : null;
    NodeList<Expression> arguments = new NodeList<>(read(pcPlus(increment), incPc));
    if (preRead != null) {
      body.add(Specializer.declare(PrimitiveType.intType(), "displacement", read(preRead.clone(), 0)));
      arguments.add(new NameExpr("displacement"));
    }
    body.add(new ExpressionStmt(new MethodCallExpr(null, method, arguments)));
    body.add(new BreakStmt());
    parametersOf.put(method, preRead != null ? List.of("displacement") : List.of());
    table(method, wrappers, preRead);
    return body;
  }

  private List<Statement> leaf(FetchedInstructionWrapper wrapper, Expression preRead) {
    spec.newCase();
    if (preRead != null)
      spec.knownOperandRead(preRead, 0, "displacement");
    List<Statement> body = new ArrayList<>();
    Specializer.Obj leaf = (Specializer.Obj) spec.call(spec.of(wrapper), "getInstruction", List.of(), body);
    int executionStart = body.size();
    spec.call(memptrUpdater, "updateBefore", List.of(leaf), body);
    spec.call(leaf, "execute", List.of(), body);
    spec.call(memptrUpdater, "updateAfter", List.of(leaf), body);
    Expression nextPC = (Expression) spec.fieldValue(leaf, "nextPC", null);
    List<Statement> execution = new ArrayList<>(body.subList(executionStart, body.size()));
    body = new ArrayList<>(body.subList(0, executionStart));
    body.addAll(weave(execution, contention.contentionOf((Instruction) leaf.value), nextPC));
    int length = ((Instruction) leaf.value).getLength();
    Expression advanced = pcPlus(length);
    body.add(new ExpressionStmt(new AssignExpr(new NameExpr("PC"), new ConditionalExpr(new BinaryExpr(nextPC.clone(), lit(-1), BinaryExpr.Operator.EQUALS), advanced, nextPC.clone()), AssignExpr.Operator.ASSIGN)));
    body.add(new BreakStmt());
    return body;
  }

  private List<Statement> weave(List<Statement> execution, Contention[] plan, Expression nextPC) {
    List<Statement> out = new ArrayList<>();
    for (Contention c : plan)
      if (c.moment() == Contention.Moment.BEFORE_EXECUTION)
        out.add(contend(c, null));
    weaveInto(execution, plan, out, new int[]{0}, new int[]{0});
    Expression jumped = new BinaryExpr(nextPC.clone(), lit(-1), BinaryExpr.Operator.NOT_EQUALS);
    Expression stayed = new BinaryExpr(nextPC.clone(), lit(-1), BinaryExpr.Operator.EQUALS);
    for (Contention c : plan) {
      if (c.moment() == Contention.Moment.AFTER_EXECUTION)
        out.add(contend(c, null));
      if (c.moment() == Contention.Moment.AFTER_EXECUTION_IF_JUMPED)
        out.add(new IfStmt(jumped.clone(), contend(c, null), null));
      if (c.moment() == Contention.Moment.AFTER_EXECUTION_IF_NOT_JUMPED)
        out.add(new IfStmt(stayed.clone(), contend(c, null), null));
    }
    return out;
  }

  private void weaveInto(List<Statement> statements, Contention[] plan, List<Statement> out, int[] reads, int[] writes) {
    for (Statement original : statements) {
      Statement s = original.clone();
      if (s instanceof IfStmt i) {
        List<Statement> then = new ArrayList<>(), otherwise = new ArrayList<>();
        int[] r2 = reads.clone(), w2 = writes.clone();
        weaveInto(statementsOf(i.getThenStmt()), plan, then, reads, writes);
        if (i.getElseStmt().isPresent())
          weaveInto(statementsOf(i.getElseStmt().get()), plan, otherwise, r2, w2);
        out.add(new IfStmt(i.getCondition().clone(), new BlockStmt(new NodeList<>(then)), otherwise.isEmpty() ? null : new BlockStmt(new NodeList<>(otherwise))));
        continue;
      }
      if (s instanceof LabeledStmt l && l.getStatement() instanceof BlockStmt b) {
        List<Statement> inner = new ArrayList<>();
        weaveInto(b.getStatements(), plan, inner, reads, writes);
        out.add(new LabeledStmt(l.getLabel(), new BlockStmt(new NodeList<>(inner))));
        continue;
      }
      MethodCallExpr access = memoryAccess(s);
      if (access != null && access.getNameAsString().equals("write")) {
        writes[0]++;
        for (Contention c : plan)
          if (c.at(Contention.Moment.BEFORE_WRITE, writes[0]))
            out.add(contend(c, access.getArgument(0)));
        out.add(s);
        continue;
      }
      out.add(s);
      if (access != null && access.getNameAsString().equals("read")) {
        reads[0]++;
        for (Contention c : plan)
          if (c.at(Contention.Moment.AFTER_READ, reads[0]))
            out.add(contend(c, access.getArgument(0)));
      }
    }
  }

  private MethodCallExpr memoryAccess(Statement s) {
    List<MethodCallExpr> calls = s.findAll(MethodCallExpr.class, m -> m.getScope().isPresent() && m.getScope().get().toString().equals("memory") && (m.getNameAsString().equals("read") || m.getNameAsString().equals("write")));
    if (calls.size() > 1)
      throw new IllegalStateException("two memory accesses in one statement: " + s);
    return calls.isEmpty() ? null : calls.get(0);
  }

  private Statement contend(Contention c, Expression lastAccess) {
    Expression base = switch (c.base()) {
      case IR -> register(state.getRegister(RegisterName.IR));
      case PC -> new NameExpr("PC");
      case HL -> register(state.getRegister(RegisterName.HL));
      case BC -> register(state.getRegister(RegisterName.BC));
      case DE -> register(state.getRegister(RegisterName.DE));
      case SP -> new NameExpr("SP");
      case LAST_ACCESS -> lastAccess.clone();
    };
    Expression address = c.delta() == 0 ? base : new BinaryExpr(new EnclosedExpr(new BinaryExpr(base, lit(Math.abs(c.delta())), c.delta() > 0 ? BinaryExpr.Operator.PLUS : BinaryExpr.Operator.MINUS)), new IntegerLiteralExpr("0xFFFF"), BinaryExpr.Operator.BINARY_AND);
    return new ExpressionStmt(new MethodCallExpr(null, "contend", new NodeList<>(Folder.fold(address), lit(c.times()), lit(c.tstates()), new FieldAccessExpr(new FieldAccessExpr(new NameExpr("Contention"), "Kind"), c.kind().name()))));
  }

  private Expression register(Register register) {
    List<Statement> none = new ArrayList<>();
    Expression e = (Expression) spec.call(spec.of(register), "read", List.of(), none);
    if (!none.isEmpty())
      throw new IllegalStateException("register read with statements: " + register);
    return e;
  }

  // ------------------------------------------------------------------ slots: literal, local or field

  private void classify() {
    for (Case c : cases) {
      c.body.forEach(s -> s.findAll(AssignExpr.class).forEach(a -> {
        assigned.add(a.getTarget().toString());
        if (a.getOperator() != AssignExpr.Operator.ASSIGN)
          read.add(a.getTarget().toString());
      }));
      c.body.forEach(s -> s.findAll(UnaryExpr.class, u -> u.getOperator().name().contains("CREMENT")).forEach(u -> {
        assigned.add(u.getExpression().toString());
        read.add(u.getExpression().toString());
      }));
      c.body.forEach(s -> s.findAll(NameExpr.class).forEach(n -> {
        if (!(n.getParentNode().orElse(null) instanceof AssignExpr a && a.getTarget() == n))
          read.add(n.getNameAsString());
      }));
    }
    for (Case c : cases)
      for (String slot : slotsIn(c.body))
        if (assigned.contains(slot) && read.contains(slot) && !definitelyAssigned(c.body, slot))
          fields.add(slot);
    Map<String, Integer> masks = masksOfEverything();
    for (Case c : cases)
      simplify(c, masks);
    // Simplifying changes what a name can hold: while PC was assigned "jumped ? there : here" it
    // could be anything, and once each branch says where it goes it is an address again. So it is
    // asked again, and the cases are simplified again, until the answer stops improving.
    for (int round = 0; round < 4; round++) {
      Map<String, Integer> narrower = masksOfEverything();
      if (narrower.equals(masks))
        break;
      masks = narrower;
      for (Case c : cases)
        Simplifier.simplify(c.body, masks);
    }
    for (Case c : cases)
      emit(c);
  }

  /**
   * The width the model actually keeps for every name the generated code uses. Derived, not
   * assumed: a register is a field of the bank that the bank's own views write too, so the bank's
   * source counts as much as the cases do.
   */
  private Map<String, Integer> masksOfEverything() {
    List<com.github.javaparser.ast.Node> roots = new ArrayList<>();
    roots.add(spec.index.type(UnrolledRegisterBank.class));
    for (Case c : cases)
      roots.addAll(c.body);
    Map<String, Integer> seed = new LinkedHashMap<>();
    for (String field : fields) {
      Object initial = slotNamed(field).initial();
      seed.put(field, initial instanceof Integer i && i >= 0 ? i : Folder.UNKNOWN);
    }
    return Simplifier.masksOf(roots, seed);
  }

  private Set<String> slotsIn(List<Statement> body) {
    Set<String> names = new LinkedHashSet<>();
    body.forEach(s -> s.findAll(NameExpr.class).forEach(n -> {
      if (slotNamed(n.getNameAsString()) != null)
        names.add(n.getNameAsString());
    }));
    return names;
  }

  private Specializer.Slot slotNamed(String name) {
    for (Specializer.Slot s : spec.slots.values())
      if (s.name().equals(name))
        return s;
    return null;
  }

  private void simplify(Case c, Map<String, Integer> masks) {
    List<Statement> body = c.body;
    for (String slot : slotsIn(body)) {
      Specializer.Slot s = slotNamed(slot);
      if (!assigned.contains(slot)) {
        Expression value = literal(s.initial());
        body.forEach(st -> st.findAll(NameExpr.class, n -> n.getNameAsString().equals(slot)).forEach(n -> n.replace(value.clone())));
      } else if (!read.contains(slot)) {
        body.removeIf(st -> st instanceof ExpressionStmt es && es.getExpression() instanceof AssignExpr a && a.getTarget().toString().equals(slot) && Specializer.isPure(a.getValue()));
        for (Statement st : body)
          for (ExpressionStmt es : st.findAll(ExpressionStmt.class, x -> x.getExpression() instanceof AssignExpr a && a.getTarget().toString().equals(slot) && Specializer.isPure(a.getValue())))
            es.remove();
        for (Statement st : body)
          for (AssignExpr a : st.findAll(AssignExpr.class, x -> x.getTarget().toString().equals(slot)))
            a.replace(a.getValue().clone());
      } else if (!fields.contains(slot))
        body.add(0, Specializer.declare(new ClassOrInterfaceType(null, s.type().getSimpleName()), slot, s.type() == boolean.class ? new BooleanLiteralExpr(false) : lit(0)));
    }
    Simplifier.simplify(body, masks);
  }

  private void emit(Case c) {
    List<Statement> body = c.body;
    String parameters = parametersOf.getOrDefault(c.method, List.of()).stream().map(p -> ", int " + p).reduce("", String::concat);
    String group = c.method + "_" + (c.opcode >> GROUP_SHIFT);
    String header = "  private void " + group + "(int opcode" + parameters + ") {\n    switch (opcode) {\n";
    methodBodies.computeIfAbsent(group, k -> new StringBuilder(header))
        .append("      case ").append(String.format("0x%02X", c.opcode)).append(": ").append(new BlockStmt(new NodeList<>(body)).toString().replace("\n", "\n      ")).append("\n");
  }

  /** Whether every read of the name in this body comes after an assignment to it on that path. */
  static boolean definitelyAssigned(List<Statement> statements, String name) {
    return flow(statements, name, false) != READS_UNASSIGNED;
  }

  private static final int READS_UNASSIGNED = -1, UNASSIGNED = 0, ASSIGNED = 1;

  private static int flow(List<Statement> statements, String name, boolean assigned) {
    int state = assigned ? ASSIGNED : UNASSIGNED;
    for (Statement s : statements) {
      if (s instanceof IfStmt i) {
        if (state == UNASSIGNED && reads(i.getCondition(), name)) return READS_UNASSIGNED;
        int then = flow(statementsOf(i.getThenStmt()), name, state == ASSIGNED);
        int otherwise = i.getElseStmt().isPresent() ? flow(statementsOf(i.getElseStmt().get()), name, state == ASSIGNED) : state;
        if (then == READS_UNASSIGNED || otherwise == READS_UNASSIGNED) return READS_UNASSIGNED;
        state = then == ASSIGNED && otherwise == ASSIGNED ? ASSIGNED : state;
        continue;
      }
      if (s instanceof LabeledStmt l) {
        List<Statement> inner = statementsOf(l.getStatement());
        int result = flow(inner, name, state == ASSIGNED);
        if (result == READS_UNASSIGNED) return READS_UNASSIGNED;
        if (result == ASSIGNED && l.findAll(BreakStmt.class).isEmpty()) state = ASSIGNED;
        continue;
      }
      if (s instanceof WhileStmt w) {
        if (state == UNASSIGNED && reads(w, name)) return READS_UNASSIGNED;
        continue;
      }
      int result = straight(s, name, state == ASSIGNED);
      if (result == READS_UNASSIGNED) return READS_UNASSIGNED;
      if (result == ASSIGNED) state = ASSIGNED;
    }
    return state;
  }

  /** One statement without branches: an assignment whose value does not read the name assigns it; anything else that names it reads it. */
  private static int straight(Statement s, String name, boolean assigned) {
    List<AssignExpr> assignments = s.findAll(AssignExpr.class, a -> a.getOperator() == AssignExpr.Operator.ASSIGN && a.getTarget().toString().equals(name));
    boolean readsIt = s.findAll(NameExpr.class, n -> n.getNameAsString().equals(name) && !(n.getParentNode().orElse(null) instanceof AssignExpr a && a.getTarget() == n && a.getOperator() == AssignExpr.Operator.ASSIGN)).size() > 0;
    if (!assignments.isEmpty()) {
      boolean valueReads = assignments.stream().anyMatch(a -> reads(a.getValue(), name));
      if (valueReads && !assigned) return READS_UNASSIGNED;
      if (readsIt && !assigned && s.findAll(NameExpr.class, n -> n.getNameAsString().equals(name)).size() > assignments.size() && valueReads) return READS_UNASSIGNED;
      return ASSIGNED;
    }
    return readsIt && !assigned ? READS_UNASSIGNED : (assigned ? ASSIGNED : UNASSIGNED);
  }

  private static List<Statement> statementsOf(Statement s) {
    return s instanceof BlockStmt b ? b.getStatements() : List.of(s);
  }

  private static boolean reads(com.github.javaparser.ast.Node n, String name) {
    return !n.findAll(NameExpr.class, x -> x.getNameAsString().equals(name)).isEmpty();
  }

  // ------------------------------------------------------------------ helpers

  private Expression pcPlus(int delta) {
    return new BinaryExpr(new EnclosedExpr(new BinaryExpr(new NameExpr("PC"), lit(delta), BinaryExpr.Operator.PLUS)), new IntegerLiteralExpr("0xFFFF"), BinaryExpr.Operator.BINARY_AND);
  }

  private MethodCallExpr read(Expression address, int fetching) {
    return new MethodCallExpr(new NameExpr("memory"), "read", new NodeList<>(address, lit(fetching)));
  }

  private static Statement call(String method, Expression... args) {
    return new ExpressionStmt(new MethodCallExpr(null, method, new NodeList<>(args)));
  }

  private static IntegerLiteralExpr lit(int i) {
    return Specializer.intLiteral(i);
  }

  private static Expression literal(Object value) {
    if (value instanceof Boolean b) return new BooleanLiteralExpr(b);
    if (value instanceof Integer i) return lit(i);
    if (value instanceof String s) return new StringLiteralExpr(s);
    throw new UnsupportedOperationException("initial " + value);
  }
}
