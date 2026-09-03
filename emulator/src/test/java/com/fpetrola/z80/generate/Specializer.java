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

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.decoder.table.MemoryForOpcodes;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.CachedTableAluOperation;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier.*;
import java.util.*;

/**
 * Turns a method of a live object graph into straight-line code: every call on a collaborator is
 * replaced by that collaborator's own method, recursively, every configuration field by its
 * value, and every structural condition by the branch that holds for these objects. What is left
 * touches only the register fields of the generated class, memory, ports and state.
 */
public class Specializer {
  final SourceIndex index;
  private int counter;
  /** Per-execution state a shared instance keeps in a field, seen as a variable of the generated code. */
  public record Slot(String name, Class<?> type, Object initial, String owner, String field) {
  }

  public final Map<String, Slot> slots = new LinkedHashMap<>();
  public final Set<Class<?>> staticSupport = new LinkedHashSet<>();
  public final Set<String> imports = new TreeSet<>();
  /** Operand bytes already read in this case, by the address they were read from. */
  private final Map<String, String> operandReads = new HashMap<>();
  private final Map<String, Expression> pureLocals = new HashMap<>();
  private final Map<Object, Integer> ids = new IdentityHashMap<>();

  public Specializer(SourceIndex index) {
    this.index = index;
  }

  public void newCase() {
    operandReads.clear();
    pureLocals.clear();
  }

  public void knownOperandRead(Expression address, int fetching, String local) {
    operandReads.put(keyOf(address) + "/" + fetching, local);
  }

  // ------------------------------------------------------------------ the object graph

  /** A live object, or one an anonymous class body would create, with what its methods can see around it. */
  public final class Obj {
    final Object value;
    final ObjectCreationExpr anon;
    final Obj outer;
    final Scope creation;
    /**
     * How the generated code reaches this object while it runs, for the ones that are not frozen
     * at generation time: the machine's memory, and whatever is found through it. Null for an
     * object whose whole graph is decided now, which is every instruction.
     */
    final Expression expression;
    final Class<?> type;

    Obj(Object value, ObjectCreationExpr anon, Obj outer, Scope creation) {
      this(value, anon, outer, creation, null, null);
    }

    Obj(Object value, ObjectCreationExpr anon, Obj outer, Scope creation, Expression expression, Class<?> type) {
      this.value = value;
      this.anon = anon;
      this.outer = outer;
      this.creation = creation;
      this.expression = expression;
      this.type = type;
    }

    Class<?> runtimeClass() {
      return value != null ? value.getClass() : type;
    }

    boolean isVirtual() {
      return anon != null;
    }

    int id() {
      return ids.computeIfAbsent(isVirtual() || value == null ? this : value, k -> ids.size() + 1);
    }

    public String toString() {
      return isVirtual() ? "anon(" + anon.getType() + ")" : runtimeClass().getSimpleName() + (expression == null ? "#" + id() : "@" + expression);
    }
  }

  /**
   * Objects the generated class keeps a reference to instead of taking apart: everything found
   * through them is read while it runs. This is the frontier - what is on this map is a field of
   * the generated class, and what is not is decided now and printed as a literal.
   */
  public final Map<Object, String> shared = new IdentityHashMap<>();

  public Obj of(Object value) {
    String held = shared.get(value);
    return held == null ? new Obj(value, null, null, null) : new Obj(value, null, null, null, new NameExpr(held), null);
  }

  private Obj atRunTime(Object value, Expression expression, Class<?> type) {
    return new Obj(value, null, null, null, expression, type);
  }

  final class Scope {
    final Map<String, Object> names = new HashMap<>();
    final Scope parent;
    Obj self;
    TypeDeclaration<?> declaringType;
    Class<?> declaringClass;
    String returnLabel;
    String returnVar;

    Scope(Scope parent) {
      this.parent = parent;
      if (parent != null) {
        self = parent.self;
        declaringType = parent.declaringType;
        declaringClass = parent.declaringClass;
        returnLabel = parent.returnLabel;
        returnVar = parent.returnVar;
      }
    }

    Object lookup(String name) {
      for (Scope s = this; s != null; s = s.parent)
        if (s.names.containsKey(name))
          return s.names.get(name);
      return null;
    }

    boolean has(String name) {
      for (Scope s = this; s != null; s = s.parent)
        if (s.names.containsKey(name))
          return true;
      return false;
    }
  }

  private String fresh(String base) {
    return base + "_" + (++counter);
  }

  // ------------------------------------------------------------------ entry points

  /** The statements {@code receiver.method(args)} amounts to, and its value if it has one. */
  public Object call(Obj receiver, String method, List<Object> args, List<Statement> out) {
    return dispatch(receiver, method, args, out, null);
  }

  public List<Statement> statementsOf(Obj receiver, String method, Object... args) {
    List<Statement> out = new ArrayList<>();
    Object result = call(receiver, method, List.of(args), out);
    if (result instanceof Expression e && hasEffect(e))
      out.add(new ExpressionStmt(e));
    return out;
  }

  // ------------------------------------------------------------------ dispatch

  private Object dispatch(Obj receiver, String name, List<Object> args, List<Statement> out, Scope caller) {
    if (receiver.isVirtual())
      return invokeVirtual(receiver, name, args, out);
    Object value = receiver.value;
    if (value instanceof CachedTableAluOperation cached)
      return aluOperation(receiver, cached, true, name, args, out);
    if (value instanceof AluOperation alu && name.startsWith("execute"))
      return aluOperation(receiver, alu, false, name, args, out);
    if (value instanceof Class<?> c)
      return invokeStatic(c, name, args, out, caller);
    if (value instanceof Lambda lambda)
      return lambda.method == null ? args.get(lambda.passthrough) : dispatch(lambda.target, lambda.method, args, out, caller);
    String terminal = terminalName(value);
    if (terminal != null)
      return invokeTerminal(receiver, terminal, name, args, out);
    Found found = findMethod(receiver.runtimeClass(), name, args.size());
    if (found == null)
      throw new UnsupportedOperationException("no source for " + receiver + "." + name + "/" + args.size());
    return inline(receiver, found, args, out);
  }

  private record Found(TypeDeclaration<?> type, Class<?> declaring, MethodDeclaration method) {
  }

  private Found findMethod(Class<?> c, String name, int arity) {
    for (Class<?> k = c; k != null; k = k.getSuperclass()) {
      Found f = findDeclared(k, name, arity);
      if (f != null)
        return f;
    }
    for (Class<?> k = c; k != null; k = k.getSuperclass())
      for (Class<?> i : allInterfaces(k)) {
        Found f = findDeclared(i, name, arity);
        if (f != null && f.method.getBody().isPresent())
          return f;
      }
    return null;
  }

  private Found findDeclared(Class<?> k, String name, int arity) {
    TypeDeclaration<?> type = index.type(k);
    if (type == null)
      return null;
    return index.method(type, name, arity).map(m -> new Found(type, k, m)).orElse(null);
  }

  private List<Class<?>> allInterfaces(Class<?> k) {
    List<Class<?>> result = new ArrayList<>();
    for (Class<?> i : k.getInterfaces()) {
      result.add(i);
      result.addAll(allInterfaces(i));
    }
    return result;
  }

  private Object invokeVirtual(Obj receiver, String name, List<Object> args, List<Statement> out) {
    for (BodyDeclaration<?> member : receiver.anon.getAnonymousClassBody().get())
      if (member instanceof MethodDeclaration m && m.getNameAsString().equals(name) && m.getParameters().size() == args.size()) {
        Scope scope = new Scope(receiver.creation);
        scope.self = receiver;
        return inlineBody(scope, m, args, out);
      }
    Class<?> declared = classOf(receiver.creation.declaringType, receiver.anon.getType());
    Found found = findMethod(declared, name, args.size());
    if (found == null)
      throw new UnsupportedOperationException("no source for " + receiver + "." + name);
    Scope scope = new Scope(receiver.creation);
    scope.self = receiver;
    scope.declaringType = found.type;
    scope.declaringClass = found.declaring;
    return inlineBody(scope, found.method, args, out);
  }

  private Class<?> classOf(TypeDeclaration<?> from, String simpleName) {
    return index.resolve(from, simpleName).orElseThrow(() -> new UnsupportedOperationException("unknown type " + simpleName));
  }

  /** A type as written, which may be nested in another: {@code Sub.Sub8TableAluOperation}. */
  private Class<?> classOf(TypeDeclaration<?> from, Type type) {
    if (type instanceof com.github.javaparser.ast.type.ClassOrInterfaceType t && t.getScope().isPresent()) {
      Class<?> outer = classOf(from, t.getScope().get());
      Class<?> nested = nestedOf(outer, t.getNameAsString());
      if (nested == null)
        throw new UnsupportedOperationException("unknown nested type " + t);
      return nested;
    }
    return classOf(from, type instanceof com.github.javaparser.ast.type.ClassOrInterfaceType t ? t.getNameAsString() : type.asString());
  }

  private Object inline(Obj receiver, Found found, List<Object> args, List<Statement> out) {
    Scope scope = new Scope(receiver.creation);
    scope.self = receiver;
    scope.declaringType = found.type;
    scope.declaringClass = found.declaring;
    return inlineBody(scope, found.method, args, out);
  }

  private Object inlineBody(Scope scope, MethodDeclaration m, List<Object> args, List<Statement> out) {
    if (m.getBody().isEmpty())
      throw new UnsupportedOperationException("abstract " + m.getNameAsString());
    NodeList<Parameter> parameters = m.getParameters();
    for (int i = 0; i < parameters.size(); i++) {
      Parameter p = parameters.get(i);
      Object a = args.get(i);
      if (a instanceof Obj || (a instanceof Expression e && (isSimple(e) || usesOnce(m.getBody().get(), p.getNameAsString()) && isPure(e))))
        scope.names.put(p.getNameAsString(), a instanceof Expression e && !isSimple(e) ? new EnclosedExpr(e) : a);
      else {
        String local = fresh(p.getNameAsString());
        out.add(declare(p.getType(), local, (Expression) a));
        if (isPure((Expression) a))
          pureLocals.put(local, (Expression) a);
        scope.names.put(p.getNameAsString(), new NameExpr(local));
      }
    }
    BlockStmt body = m.getBody().get();
    List<ReturnStmt> returns = body.findAll(ReturnStmt.class, r -> !insideNested(r, body));
    boolean isVoid = m.getType().isVoidType();
    boolean lastIsReturn = !body.getStatements().isEmpty() && body.getStatement(body.getStatements().size() - 1) instanceof ReturnStmt;
    if (returns.isEmpty() || (returns.size() == 1 && lastIsReturn)) {
      List<Statement> statements = body.getStatements();
      for (int i = 0; i < statements.size() - (lastIsReturn ? 1 : 0); i++)
        rewriteStmt(statements.get(i), scope, out);
      if (lastIsReturn) {
        ReturnStmt r = (ReturnStmt) statements.get(statements.size() - 1);
        Object value = r.getExpression().map(e -> rewriteExpr(e, scope, out)).orElse(null);
        return value instanceof Expression e && !isSimple(e) && !(e instanceof EnclosedExpr) && !(e instanceof MethodCallExpr) && !(e instanceof ArrayAccessExpr) ? new EnclosedExpr(e) : value;
      }
      return null;
    }
    scope.returnLabel = fresh("done");
    scope.returnVar = isVoid ? null : fresh("result");
    if (!isVoid)
      out.add(declare(m.getType(), scope.returnVar, null));
    List<Statement> inner = new ArrayList<>();
    for (Statement s : body.getStatements())
      rewriteStmt(s, scope, inner);
    out.add(new LabeledStmt(scope.returnLabel, new BlockStmt(new NodeList<>(inner))));
    return isVoid ? null : new NameExpr(scope.returnVar);
  }

  private boolean insideNested(ReturnStmt r, BlockStmt body) {
    for (var n = r.getParentNode(); n.isPresent() && n.get() != body; n = n.get().getParentNode())
      if (n.get() instanceof LambdaExpr || n.get() instanceof ObjectCreationExpr || n.get() instanceof MethodDeclaration)
        return true;
    return false;
  }

  private boolean usesOnce(BlockStmt body, String name) {
    return body.findAll(NameExpr.class, n -> n.getNameAsString().equals(name)).size() <= 1;
  }

  // ------------------------------------------------------------------ special receivers

  /**
   * What stays a call instead of being inlined, and the name of the field it is called on. The
   * pure core keeps the memory out; a core generated against a machine inlines that machine's
   * memory and keeps only what is really opaque.
   */
  public final Map<Class<?>, String> terminals = new LinkedHashMap<>(Map.of(Memory.class, "memory", IO.class, "io", State.class, "state"));

  private String terminalName(Object value) {
    for (Map.Entry<Class<?>, String> terminal : terminals.entrySet())
      if (terminal.getKey().isInstance(value))
        return terminal.getValue();
    return null;
  }

  private Object invokeTerminal(Obj receiver, String field, String name, List<Object> args, List<Statement> out) {
    Found found = findMethod(receiver.runtimeClass(), name, args.size());
    if (found != null && found.declaring.isInterface() && found.method.isDefault())
      return inline(receiver, found, args, out);
    if (args.isEmpty() && (name.startsWith("get") || name.startsWith("is"))) {
      Method getter = findReflected(receiver.runtimeClass(), name, 0);
      if (getter != null && !getter.getReturnType().isPrimitive() && !getter.getReturnType().isEnum() && getter.getReturnType() != String.class)
        return of(invoke(getter, receiver.value));
    }
    NodeList<Expression> arguments = new NodeList<>();
    for (Object a : args)
      arguments.add(asExpression(a));
    MethodCallExpr call = new MethodCallExpr(field.isEmpty() ? null : new NameExpr(field), name, arguments);
    if (receiver.value instanceof MemoryForOpcodes && name.equals("read"))
      return operandRead(call, out);
    return call;
  }

  /** The same operand byte read twice in one instruction is one read: what MemoryForOpcodes is for. */
  private Expression operandRead(MethodCallExpr read, List<Statement> out) {
    String key = keyOf(read.getArgument(0)) + "/" + read.getArgument(1);
    String local = operandReads.get(key);
    if (local == null) {
      local = fresh("operand");
      out.add(declare(PrimitiveType.intType(), local, read));
      operandReads.put(key, local);
    }
    return new NameExpr(local);
  }

  private String keyOf(Expression address) {
    Expression expanded = address.clone();
    for (NameExpr n : expanded.findAll(NameExpr.class))
      if (pureLocals.containsKey(n.getNameAsString()))
        n.replace(pureLocals.get(n.getNameAsString()).clone());
    if (expanded instanceof NameExpr n && pureLocals.containsKey(n.getNameAsString()))
      expanded = pureLocals.get(n.getNameAsString()).clone();
    return Folder.fold(expanded).toString();
  }

  private Object invokeStatic(Class<?> c, String name, List<Object> args, List<Statement> out, Scope caller) {
    if (c.isEnum() && name.equals("values") && args.isEmpty())
      return c.getEnumConstants();
    Found found = findDeclared(c, name, args.size());
    if (found == null) {
      NodeList<Expression> arguments = new NodeList<>();
      for (Object a : args)
        arguments.add(asExpression(a));
      imports.add(c.getName().replace('$', '.'));
      return Folder.fold(new MethodCallExpr(new NameExpr(c.getSimpleName()), name, arguments));
    }
    Scope scope = new Scope(null);
    scope.self = null;
    scope.declaringType = found.type;
    scope.declaringClass = c;
    return inlineBody(scope, found.method, args, out);
  }

  /**
   * A cached table is an optimisation of the operation's calculate method; the generated code
   * runs the method, seeded and masked the way the table was built.
   */
  private Object aluOperation(Obj receiver, AluOperation operation, boolean cached, String name, List<Object> args, List<Statement> out) {
    AluOperation delegate = cached ? (AluOperation) get(findField(CachedTableAluOperation.class, "delegate"), operation) : operation;
    Obj delegateObj = of(delegate);
    Obj flag = (Obj) args.get(args.size() - 1);
    Expression v1 = asExpression(args.get(0)), v2 = asExpression(args.get(1));
    Expression v3 = switch (name) {
      case "execute2Values" -> new IntegerLiteralExpr("0");
      case "execute2ValuesAndCarry" -> mask(asExpression(dispatch(flag, "read", List.of(), out, null)), 1);
      case "execute2Values1Boolean" -> mask(asExpression(args.get(2)), 1);
      case "execute3Values" -> mask(asExpression(args.get(2)), 0xff);
      default -> throw new UnsupportedOperationException(name);
    };
    String a = fresh("value1"), b = fresh("value2"), c = fresh("value3");
    out.add(declare(PrimitiveType.intType(), a, cached ? mask(v1, 0xff) : v1));
    out.add(declare(PrimitiveType.intType(), b, cached ? mask(v2, 0xff) : v2));
    out.add(declare(PrimitiveType.intType(), c, v3));
    pureLocals.put(a, cached ? mask(v1, 0xff) : v1);
    pureLocals.put(b, cached ? mask(v2, 0xff) : v2);
    Expression fSlot = (Expression) fieldValue(delegateObj, "F", null);
    Expression seed = cached ? new NameExpr(b) : asExpression(dispatch(flag, "read", List.of(), out, null));
    out.add(new ExpressionStmt(new AssignExpr(fSlot.clone(), seed, AssignExpr.Operator.ASSIGN)));
    Class<?> k = delegate.getClass();
    Object result;
    if (declares(k, "calculate2Values1Boolean"))
      result = dispatch(delegateObj, "calculate2Values1Boolean", List.of(new NameExpr(a), new NameExpr(b), new NameExpr(c)), out, null);
    else if (declares(k, "calculate1Value"))
      result = dispatch(delegateObj, "calculate1Value", List.of(new NameExpr(a)), out, null);
    else if (declares(k, "calculate3Values"))
      result = dispatch(delegateObj, "calculate3Values", List.of(new NameExpr(a), new NameExpr(b), new NameExpr(c)), out, null);
    else
      throw new UnsupportedOperationException("no calculate in " + k);
    String r = fresh("result");
    out.add(declare(PrimitiveType.intType(), r, mask(asExpression(result), 0xff)));
    List<Statement> write = new ArrayList<>();
    dispatch(flag, "write", List.of(mask(fSlot.clone(), 0xff)), write, null);
    out.addAll(write);
    return new NameExpr(r);
  }

  private boolean declares(Class<?> k, String method) {
    for (Method m : k.getDeclaredMethods())
      if (m.getName().equals(method))
        return true;
    return false;
  }

  private Expression mask(Expression e, int m) {
    return Folder.fold(new BinaryExpr(new EnclosedExpr(e), new IntegerLiteralExpr(m > 15 ? "0x" + Integer.toHexString(m).toUpperCase() : "" + m), BinaryExpr.Operator.BINARY_AND));
  }

  // ------------------------------------------------------------------ statements

  void rewriteStmt(Statement s, Scope scope, List<Statement> out) {
    if (s instanceof ExpressionStmt es) {
      Expression e = es.getExpression();
      if (e instanceof VariableDeclarationExpr v) {
        for (VariableDeclarator d : v.getVariables()) {
          String local = fresh(d.getNameAsString());
          if (d.getInitializer().isPresent()) {
            Object init = rewriteExpr(d.getInitializer().get(), scope, out);
            if (init instanceof Obj o) {
              // A local the model declared for something it reaches at run time stays a local here:
              // otherwise every use of it repeats the way it was reached, and a page lookup that
              // the model does once would be done eight times.
              if (o.expression != null && !(o.expression instanceof NameExpr)) {
                imports.add(o.runtimeClass().getName().replace('$', '.'));
                out.add(declare(new ClassOrInterfaceType(null, o.runtimeClass().getSimpleName()), local, o.expression.clone()));
                scope.names.put(d.getNameAsString(), atRunTime(o.value, new NameExpr(local), o.runtimeClass()));
              } else
                scope.names.put(d.getNameAsString(), o);
            }
            else {
              Expression ie = (Expression) init;
              out.add(declare(d.getType(), local, ie));
              if (isPure(ie))
                pureLocals.put(local, ie);
              scope.names.put(d.getNameAsString(), new NameExpr(local));
            }
          } else {
            out.add(declare(d.getType(), local, null));
            scope.names.put(d.getNameAsString(), new NameExpr(local));
          }
        }
        return;
      }
      Object r = rewriteExpr(e, scope, out);
      if (r instanceof Expression re && hasEffect(re))
        out.add(new ExpressionStmt(re));
    } else if (s instanceof BlockStmt b) {
      Scope inner = new Scope(scope);
      for (Statement child : b.getStatements())
        rewriteStmt(child, inner, out);
    } else if (s instanceof IfStmt i) {
      Expression cond = asExpression(rewriteExpr(i.getCondition(), scope, out));
      if (cond instanceof BooleanLiteralExpr lit) {
        if (lit.getValue())
          rewriteStmt(i.getThenStmt(), new Scope(scope), out);
        else if (i.getElseStmt().isPresent())
          rewriteStmt(i.getElseStmt().get(), new Scope(scope), out);
        return;
      }
      List<Statement> then = new ArrayList<>();
      rewriteStmt(i.getThenStmt(), new Scope(scope), then);
      IfStmt result = new IfStmt(cond, block(then), null);
      if (i.getElseStmt().isPresent()) {
        List<Statement> otherwise = new ArrayList<>();
        rewriteStmt(i.getElseStmt().get(), new Scope(scope), otherwise);
        if (!otherwise.isEmpty())
          result.setElseStmt(otherwise.size() == 1 && otherwise.get(0) instanceof IfStmt ? otherwise.get(0) : block(otherwise));
      }
      out.add(result);
    } else if (s instanceof ReturnStmt r) {
      if (scope.returnLabel == null)
        throw new IllegalStateException("return outside an inlined body: " + s);
      if (r.getExpression().isPresent()) {
        Expression value = asExpression(rewriteExpr(r.getExpression().get(), scope, out));
        out.add(new ExpressionStmt(new AssignExpr(new NameExpr(scope.returnVar), value, AssignExpr.Operator.ASSIGN)));
      }
      out.add(new BreakStmt(scope.returnLabel));
    } else if (s instanceof EmptyStmt) {
    } else if (s instanceof WhileStmt w) {
      List<Statement> condStatements = new ArrayList<>();
      Expression cond = asExpression(rewriteExpr(w.getCondition(), scope, condStatements));
      if (!condStatements.isEmpty())
        throw new UnsupportedOperationException("loop condition with statements: " + w);
      List<Statement> body = new ArrayList<>();
      rewriteStmt(w.getBody(), new Scope(scope), body);
      out.add(new WhileStmt(cond, block(body)));
    } else if (s instanceof ThrowStmt t) {
      out.add(new ThrowStmt(asExpression(rewriteExpr(t.getExpression(), scope, out))));
    } else
      throw new UnsupportedOperationException("statement " + s.getClass().getSimpleName() + ": " + s);
  }

  private BlockStmt block(List<Statement> statements) {
    return new BlockStmt(new NodeList<>(statements));
  }

  // ------------------------------------------------------------------ expressions

  Object rewriteExpr(Expression e, Scope scope, List<Statement> out) {
    if (e instanceof NameExpr n)
      return name(n.getNameAsString(), scope);
    if (e instanceof ThisExpr t) {
      if (t.getTypeName().isPresent())
        return outerNamed(scope.self, t.getTypeName().get().getIdentifier());
      return scope.self;
    }
    if (e instanceof FieldAccessExpr f) {
      Object target = rewriteExpr(f.getScope(), scope, out);
      if (target instanceof Obj o)
        return fieldValue(o, f.getNameAsString(), scope);
      throw new UnsupportedOperationException("field access on " + target + ": " + f);
    }
    if (e instanceof MethodCallExpr call) {
      List<Object> args = new ArrayList<>();
      Obj receiver;
      if (call.getScope().isEmpty())
        receiver = scope.self;
      else {
        Object target = call.getScope().get() instanceof SuperExpr ? scope.self : rewriteExpr(call.getScope().get(), scope, out);
        if (!(target instanceof Obj)) {
          for (Expression a : call.getArguments())
            args.add(rewriteExpr(a, scope, out));
          NodeList<Expression> arguments = new NodeList<>();
          for (Object a : args)
            arguments.add(asExpression(a));
          return Folder.fold(new MethodCallExpr(asExpression(target), call.getNameAsString(), arguments));
        }
        receiver = (Obj) target;
      }
      for (Expression a : call.getArguments())
        args.add(rewriteExpr(a, scope, out));
      if (call.getScope().isPresent() && call.getScope().get() instanceof SuperExpr sup)
        return invokeSuper(scope, sup, call.getNameAsString(), args, out);
      if (call.getScope().isEmpty() && receiver == null)
        return invokeStatic(scope.declaringClass, call.getNameAsString(), args, out, scope);
      if (call.getScope().isEmpty() && !hasInstanceMethod(receiver, call.getNameAsString(), args.size(), scope))
        return invokeStatic(scope.declaringClass, call.getNameAsString(), args, out, scope);
      return dispatch(receiver, call.getNameAsString(), args, out, scope);
    }
    if (e instanceof ObjectCreationExpr c)
      return create(c, scope, out);
    if (e instanceof InstanceOfExpr io) {
      Object target = rewriteExpr(io.getExpression(), scope, out);
      if (target instanceof Obj o) {
        Class<?> type = classOf(scope.declaringType, io.getType());
        boolean is = o.isVirtual() ? type.isAssignableFrom(classOf(o.creation.declaringType, o.anon.getType())) : type.isInstance(o.value);
        if (is && io.getPattern().isPresent())
          scope.names.put(io.getPattern().get().getNameAsString(), o);
        return new BooleanLiteralExpr(is);
      }
      return new InstanceOfExpr(asExpression(target), io.getType());
    }
    if (e instanceof CastExpr c) {
      Object inner = rewriteExpr(c.getExpression(), scope, out);
      if (inner instanceof Obj)
        return inner;
      return Folder.fold(new CastExpr(c.getType(), asExpression(inner)));
    }
    if (e instanceof EnclosedExpr en) {
      Object inner = rewriteExpr(en.getInner(), scope, out);
      if (inner instanceof Obj)
        return inner;
      return Folder.fold(new EnclosedExpr(asExpression(inner)));
    }
    if (e instanceof BinaryExpr b) {
      Object left = rewriteExpr(b.getLeft(), scope, out);
      if (left instanceof Obj && b.getOperator() == BinaryExpr.Operator.AND) {
        throw new UnsupportedOperationException(b.toString());
      }
      if (b.getOperator() == BinaryExpr.Operator.AND || b.getOperator() == BinaryExpr.Operator.OR) {
        Expression l = asExpression(left);
        if (l instanceof BooleanLiteralExpr lit) {
          if (b.getOperator() == BinaryExpr.Operator.AND ? !lit.getValue() : lit.getValue())
            return lit;
          return asExpression(rewriteExpr(b.getRight(), scope, out));
        }
        List<Statement> rightStatements = new ArrayList<>();
        Expression r = asExpression(rewriteExpr(b.getRight(), scope, rightStatements));
        if (!rightStatements.isEmpty())
          throw new UnsupportedOperationException("short-circuit operand with statements: " + b);
        return Folder.fold(new BinaryExpr(l, r, b.getOperator()));
      }
      Object right = rewriteExpr(b.getRight(), scope, out);
      if (left instanceof Obj || right instanceof Obj) {
        boolean identity = left instanceof Obj lo && right instanceof Obj ro && !lo.isVirtual() && !ro.isVirtual() && lo.value == ro.value;
        boolean nullTest = (left instanceof Obj && right instanceof NullLiteralExpr) || (right instanceof Obj && left instanceof NullLiteralExpr);
        if (nullTest)
          return new BooleanLiteralExpr(b.getOperator() == BinaryExpr.Operator.NOT_EQUALS);
        if (left instanceof Obj && right instanceof Obj)
          return new BooleanLiteralExpr(b.getOperator() == BinaryExpr.Operator.EQUALS ? identity : !identity);
        throw new UnsupportedOperationException("object in arithmetic: " + b);
      }
      return Folder.fold(new BinaryExpr((Expression) left, (Expression) right, b.getOperator()));
    }
    if (e instanceof UnaryExpr u) {
      if (u.getOperator().name().contains("CREMENT"))
        return new UnaryExpr(assignable(u.getExpression(), scope, out), u.getOperator());
      Expression operand = asExpression(rewriteExpr(u.getExpression(), scope, out));
      return Folder.fold(new UnaryExpr(operand, u.getOperator()));
    }
    if (e instanceof AssignExpr a) {
      Expression value = asExpression(rewriteExpr(a.getValue(), scope, out));
      Expression target = assignable(a.getTarget(), scope, out);
      if (a.getOperator() == AssignExpr.Operator.ASSIGN && value.toString().equals(target.toString()))
        return target;
      if (value instanceof EnclosedExpr en && a.getOperator() == AssignExpr.Operator.ASSIGN)
        value = en.getInner();
      return new AssignExpr(target, value, a.getOperator());
    }
    if (e instanceof ConditionalExpr c) {
      Expression cond = asExpression(rewriteExpr(c.getCondition(), scope, out));
      if (cond instanceof BooleanLiteralExpr lit)
        return rewriteExpr(lit.getValue() ? c.getThenExpr() : c.getElseExpr(), scope, out);
      List<Statement> thenStatements = new ArrayList<>(), elseStatements = new ArrayList<>();
      Expression then = asExpression(rewriteExpr(c.getThenExpr(), scope, thenStatements));
      Expression otherwise = asExpression(rewriteExpr(c.getElseExpr(), scope, elseStatements));
      if (thenStatements.isEmpty() && elseStatements.isEmpty())
        return new ConditionalExpr(cond, then, otherwise);
      String r = fresh("chosen");
      out.add(declare(PrimitiveType.intType(), r, null));
      thenStatements.add(new ExpressionStmt(new AssignExpr(new NameExpr(r), then, AssignExpr.Operator.ASSIGN)));
      elseStatements.add(new ExpressionStmt(new AssignExpr(new NameExpr(r), otherwise, AssignExpr.Operator.ASSIGN)));
      out.add(new IfStmt(cond, block(thenStatements), block(elseStatements)));
      return new NameExpr(r);
    }
    if (e instanceof ArrayAccessExpr a) {
      Object array = rewriteExpr(a.getName(), scope, out);
      Expression idx = asExpression(rewriteExpr(a.getIndex(), scope, out));
      if (array instanceof Object[] values) {
        Class<?> component = values.getClass().getComponentType();
        if (idx instanceof IntegerLiteralExpr n)
          return literal(values[n.asNumber().intValue()], component);
        imports.add(component.getName().replace('$', '.'));
        return new ArrayAccessExpr(new MethodCallExpr(new NameExpr(component.getSimpleName()), "values"), idx);
      }
      if (array instanceof Obj o && o.expression != null && o.runtimeClass().isArray()) {
        Expression element = new ArrayAccessExpr(o.expression.clone(), idx);
        Class<?> component = o.runtimeClass().getComponentType();
        return isValueType(component) ? element : atRunTime(null, element, component);
      }
      return new ArrayAccessExpr(asExpression(array), idx);
    }
    if (e instanceof LiteralExpr)
      return e.clone();
    throw new UnsupportedOperationException("expression " + e.getClass().getSimpleName() + ": " + e);
  }

  private boolean hasInstanceMethod(Obj receiver, String name, int arity, Scope scope) {
    if (receiver.isVirtual()) {
      for (BodyDeclaration<?> member : receiver.anon.getAnonymousClassBody().get())
        if (member instanceof MethodDeclaration m && m.getNameAsString().equals(name))
          return true;
      return true;
    }
    Found found = findMethod(receiver.runtimeClass(), name, arity);
    if (found != null)
      return !found.method.isStatic();
    Method reflected = findReflected(receiver.runtimeClass(), name, arity);
    return reflected != null && !java.lang.reflect.Modifier.isStatic(reflected.getModifiers());
  }

  private Object invokeSuper(Scope scope, SuperExpr sup, String name, List<Object> args, List<Statement> out) {
    Class<?> from = sup.getTypeName().isPresent() ? classOf(scope.declaringType, sup.getTypeName().get().getIdentifier()) : scope.declaringClass.getSuperclass();
    Found found = findMethod(from, name, args.size());
    if (found == null)
      throw new UnsupportedOperationException("no super." + name + " above " + scope.declaringClass.getSimpleName());
    Scope s = new Scope(scope.self.creation);
    s.self = scope.self;
    s.declaringType = found.type;
    s.declaringClass = found.declaring;
    return inlineBody(s, found.method, args, out);
  }

  private Expression assignable(Expression target, Scope scope, List<Statement> out) {
    if (target instanceof EnclosedExpr en)
      return assignable(en.getInner(), scope, out);
    if (target instanceof NameExpr n) {
      Object bound = name(n.getNameAsString(), scope);
      if (bound instanceof NameExpr)
        return (NameExpr) bound;
      if (bound instanceof FieldAccessExpr access)
        return access;
      throw new UnsupportedOperationException("assignment to " + bound + " in " + target);
    }
    if (target instanceof FieldAccessExpr f) {
      Object owner = rewriteExpr(f.getScope(), scope, out);
      if (owner instanceof Obj o) {
        Object v = fieldValue(o, f.getNameAsString(), scope);
        if (v instanceof NameExpr ne)
          return ne;
        if (v instanceof FieldAccessExpr access)
          return access;
      }
    }
    if (target instanceof ArrayAccessExpr)
      return asExpression(rewriteExpr(target, scope, out));
    throw new UnsupportedOperationException("assignment target " + target);
  }

  private Object create(ObjectCreationExpr c, Scope scope, List<Statement> out) {
    if (c.getAnonymousClassBody().isPresent())
      return new Obj(null, c, scope.self, scope);
    Class<?> type = classOf(scope.declaringType, c.getType());
    Object[] args = new Object[c.getArguments().size()];
    for (int i = 0; i < args.length; i++) {
      Expression a = asExpression(rewriteExpr(c.getArgument(i), scope, out));
      if (a instanceof StringLiteralExpr s) args[i] = s.getValue();
      else if (a instanceof IntegerLiteralExpr n) args[i] = n.asNumber().intValue();
      else if (a instanceof BooleanLiteralExpr b) args[i] = b.getValue();
      else throw new UnsupportedOperationException("constructor argument " + a + " in " + c);
    }
    for (var constructor : type.getDeclaredConstructors())
      if (constructor.getParameterCount() == args.length) {
        try {
          constructor.setAccessible(true);
          return of(constructor.newInstance(args));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    throw new UnsupportedOperationException("cannot create " + c);
  }

  // ------------------------------------------------------------------ names and fields

  private Object name(String name, Scope scope) {
    Object bound = scope.lookup(name);
    if (bound != null)
      return bound instanceof Expression e ? e.clone() : bound;
    if (scope.self != null) {
      Object v = fieldValue(scope.self, name, scope);
      if (v != null)
        return v;
    }
    if (scope.declaringClass != null) {
      Field f = findField(scope.declaringClass, name);
      if (f != null && java.lang.reflect.Modifier.isStatic(f.getModifiers()))
        return staticField(f);
      Optional<Class<?>> type = index.resolve(scope.declaringType, name);
      if (type.isPresent())
        return of(type.get());
      Class<?> nested = nestedOf(scope.declaringClass, name);
      if (nested != null)
        return of(nested);
    }
    throw new UnsupportedOperationException("unknown name " + name + " in " + (scope.declaringClass == null ? "?" : scope.declaringClass.getSimpleName()));
  }

  private Class<?> nestedOf(Class<?> c, String simpleName) {
    for (Class<?> k = c; k != null; k = k.getSuperclass())
      for (Class<?> n : k.getDeclaredClasses())
        if (n.getSimpleName().equals(simpleName))
          return n;
    return null;
  }

  private Object outerNamed(Obj self, String typeName) {
    for (Obj o = self; o != null; o = outerOf(o))
      if (!o.isVirtual() && o.runtimeClass().getSimpleName().equals(typeName))
        return o;
    throw new UnsupportedOperationException(typeName + ".this from " + self);
  }

  private Obj outerOf(Obj o) {
    if (o.outer != null)
      return o.outer;
    if (o.isVirtual())
      return null;
    Field f = findField(o.runtimeClass(), "this$0");
    return f == null ? null : of(get(f, o.value));
  }

  /** What a field of this object is to the generated code: a literal, a variable, a register field, or another object. Null when there is no such field. */
  Object fieldValue(Obj o, String name, Scope scope) {
    if (o.isVirtual()) {
      Object captured = o.creation.lookup(name);
      if (captured != null)
        return captured instanceof Expression e ? e.clone() : captured;
      return o.outer == null ? null : fieldValue(o.outer, name, scope);
    }
    if (o.value instanceof Class<?> c) {
      Field f = findField(c, name);
      return f == null ? null : staticField(f);
    }
    Field f = findField(o.runtimeClass(), name);
    if (f == null) {
      Obj outer = outerOf(o);
      return outer == null ? null : fieldValue(outer, name, scope);
    }
    if (java.lang.reflect.Modifier.isStatic(f.getModifiers()))
      return staticField(f);
    if (o.expression != null) {
      Object held = o.value == null ? null : get(f, o.value);
      if (held != null && shared.containsKey(held))
        return of(held);
      // A final field of the one object that is there cannot change: it is a constant of the shape.
      if (isValueType(f.getType()) && java.lang.reflect.Modifier.isFinal(f.getModifiers()) && o.value != null)
        return literal(get(f, o.value), f.getType());
      Expression access = new FieldAccessExpr(o.expression.clone(), name);
      if (isValueType(f.getType()))
        return access;
      return atRunTime(held, access, f.getType());
    }
    Object value = get(f, o.value);
    if (o.value instanceof RegisterBank)
      return new NameExpr(name);
    if (isValueType(f.getType())) {
      if (java.lang.reflect.Modifier.isFinal(f.getModifiers()) || f.getDeclaringClass() == com.fpetrola.z80.spy.ObservableRegister.class)
        return literal(value, f.getType());
      return new NameExpr(slot(o, f, value).name());
    }
    if (value == null)
      return new NullLiteralExpr();
    if (shared.containsKey(value))
      return of(value);
    if (f.getType().isArray())
      throw new UnsupportedOperationException("instance array " + f);
    if (value.getClass().isSynthetic())
      return of(lambdaBehind(o, f));
    return new Obj(value, null, null, null);
  }

  /** What a lambda held in a field does; the object has no source, but the constructor that chose it does. */
  private record Lambda(Obj target, String method, int passthrough) {
  }

  private Lambda lambdaBehind(Obj owner, Field f) {
    if (f.getType() == com.fpetrola.z80.instructions.cache.ConditionPredicate.class)
      return new Lambda(null, null, 0);
    if (owner.value instanceof com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference reference) {
      boolean sp = reference.getTarget() instanceof com.fpetrola.z80.registers.Register register && register.getName().equals("SP");
      return new Lambda(of(reference.getMemory()), sp ? "write16Bits" : "write16BitsReverse", 0);
    }
    throw new UnsupportedOperationException("lambda in " + f);
  }

  private Slot slot(Obj o, Field f, Object value) {
    String key = o.id() + "." + f.getName();
    Object initial = o.value instanceof AluOperation && f.getType() == int.class ? 0 : value;
    return slots.computeIfAbsent(key, k -> new Slot("_" + f.getName() + o.id(), f.getType(), initial, o.runtimeClass().getSimpleName(), f.getName()));
  }

  private Expression staticField(Field f) {
    if (isValueType(f.getType()) && java.lang.reflect.Modifier.isFinal(f.getModifiers()))
      return literal(get(f, null), f.getType());
    staticSupport.add(f.getDeclaringClass());
    return new NameExpr(f.getName());
  }

  private boolean isValueType(Class<?> t) {
    return t.isPrimitive() || t == String.class || t.isEnum() || t == Integer.class || t == Boolean.class;
  }

  private Expression literal(Object value, Class<?> type) {
    if (value == null) return new NullLiteralExpr();
    if (value instanceof Boolean b) return new BooleanLiteralExpr(b);
    if (value instanceof Integer i) return intLiteral(i);
    if (value instanceof Byte b) return intLiteral(b);
    if (value instanceof Long l) return new LongLiteralExpr(l + "L");
    if (value instanceof String s) return new StringLiteralExpr(s);
    if (value instanceof Enum<?> en) {
      imports.add(en.getDeclaringClass().getName().replace('$', '.'));
      return new FieldAccessExpr(new NameExpr(en.getDeclaringClass().getSimpleName()), en.name());
    }
    throw new UnsupportedOperationException("literal of " + type);
  }

  static IntegerLiteralExpr intLiteral(int i) {
    return new IntegerLiteralExpr(i > 15 ? "0x" + Integer.toHexString(i).toUpperCase() : Integer.toString(i));
  }

  static Field findField(Class<?> c, String name) {
    for (Class<?> k = c; k != null; k = k.getSuperclass())
      for (Field f : k.getDeclaredFields())
        if (f.getName().equals(name))
          return f;
    return null;
  }

  static Object get(Field f, Object o) {
    try {
      f.setAccessible(true);
      return f.get(o);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static Method findReflected(Class<?> c, String name, int arity) {
    for (Class<?> k = c; k != null; k = k.getSuperclass())
      for (Method m : k.getDeclaredMethods())
        if (m.getName().equals(name) && m.getParameterCount() == arity)
          return m;
    return null;
  }

  private static Object invoke(Method m, Object o) {
    try {
      m.setAccessible(true);
      return m.invoke(o);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // ------------------------------------------------------------------ helpers

  Expression asExpression(Object o) {
    if (o instanceof Expression e)
      return e;
    if (o instanceof Obj obj && obj.expression != null)
      return obj.expression.clone();
    if (o == null)
      throw new IllegalStateException("void where a value was needed");
    throw new UnsupportedOperationException("object escapes into the generated code: " + o);
  }

  static Statement declare(Type type, String name, Expression init) {
    Type unboxed = unbox(type);
    VariableDeclarator d = init == null ? new VariableDeclarator(unboxed, name) : new VariableDeclarator(unboxed, name, init);
    return new ExpressionStmt(new VariableDeclarationExpr(d));
  }

  /** A local the model declared as a wrapper is an int here: the generated code has no nulls to carry. */
  private static Type unbox(Type type) {
    if (type instanceof ClassOrInterfaceType c)
      switch (c.getNameAsString()) {
        case "Integer": return PrimitiveType.intType();
        case "Boolean": return PrimitiveType.booleanType();
        case "Long": return PrimitiveType.longType();
        case "Byte": return PrimitiveType.byteType();
        case "Short": return PrimitiveType.shortType();
        case "Character": return PrimitiveType.charType();
      }
    return type.clone();
  }

  static boolean isSimple(Expression e) {
    return e instanceof NameExpr || e instanceof LiteralExpr;
  }

  static boolean isPure(Expression e) {
    return e.findAll(MethodCallExpr.class).isEmpty() && e.findAll(AssignExpr.class).isEmpty()
        && e.findAll(UnaryExpr.class, u -> u.getOperator().name().contains("CREMENT")).isEmpty();
  }

  static boolean hasEffect(Expression e) {
    return !(e instanceof NameExpr || e instanceof LiteralExpr || e instanceof BinaryExpr || e instanceof EnclosedExpr || e instanceof CastExpr || e instanceof ConditionalExpr && isPure(e));
  }
}
