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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;

import java.util.*;

/** What is left to tidy once the objects are gone: constants that fell out, branches that cannot run, names that stand for one value. */
public class Simplifier {
  public static void simplify(List<Statement> body, Map<String, Integer> masks) {
    boolean changed = true;
    for (int round = 0; changed && round < 8; round++) {
      changed = foldAll(body);
      changed |= dropDeadBranches(body);
      changed |= propagateAliases(body);
      changed |= dropDeadStores(body);
      changed |= dropWhatCannotFail(body, masks);
      changed |= dropUnusedLocals(body);
    }
  }

  private static boolean foldAll(List<Statement> body) {
    boolean[] changed = {false};
    for (Statement s : body)
      for (Expression e : topLevelExpressions(s))
        if (e.getParentNode().isPresent() && !(e instanceof LiteralExpr) && !(e instanceof NameExpr)) {
          Expression folded = Folder.fold(e);
          if (!folded.toString().equals(e.toString())) {
            e.replace(folded);
            changed[0] = true;
          }
        }
    return changed[0];
  }

  /** The expressions a statement holds directly, not the ones inside them: folding is recursive already. */
  private static List<Expression> topLevelExpressions(Statement s) {
    List<Expression> result = new ArrayList<>();
    for (Node child : s.getChildNodes())
      if (child instanceof Expression e)
        result.add(e);
      else if (child instanceof Statement)
        continue;
      else
        for (Node grandchild : child.getChildNodes())
          if (grandchild instanceof Expression e)
            result.add(e);
    return result;
  }

  private static boolean dropDeadBranches(List<Statement> body) {
    boolean changed = false;
    for (int i = 0; i < body.size(); i++) {
      Statement s = body.get(i);
      if (s instanceof IfStmt f && f.getCondition() instanceof BooleanLiteralExpr b) {
        List<Statement> chosen = b.getValue() ? statements(f.getThenStmt()) : f.getElseStmt().map(Simplifier::statements).orElse(List.of());
        body.remove(i);
        body.addAll(i, cloneAll(chosen));
        changed = true;
        i--;
      } else if (s instanceof IfStmt f) {
        changed |= simplifyNested(f.getThenStmt());
        if (f.getElseStmt().isPresent()) {
          changed |= simplifyNested(f.getElseStmt().get());
          if (f.getElseStmt().get() instanceof BlockStmt eb && eb.isEmpty()) {
            f.removeElseStmt();
            changed = true;
          }
        }
        if (f.getThenStmt() instanceof BlockStmt tb && tb.isEmpty() && f.getElseStmt().isEmpty() && Specializer.isPure(f.getCondition())) {
          body.remove(i);
          changed = true;
          i--;
        }
      } else if (s instanceof LabeledStmt l && l.getStatement() instanceof BlockStmt lb)
        changed |= simplifyNested(lb);
      else if (s instanceof ExpressionStmt es && es.getExpression() instanceof AssignExpr a && a.getOperator() == AssignExpr.Operator.ASSIGN && a.getTarget().toString().equals(a.getValue().toString())) {
        body.remove(i);
        changed = true;
        i--;
      }
    }
    return changed;
  }

  private static boolean simplifyNested(Statement s) {
    if (s instanceof BlockStmt b) {
      List<Statement> inner = new ArrayList<>(b.getStatements());
      boolean changed = foldAll(inner) | dropDeadBranches(inner);
      if (changed) {
        b.getStatements().clear();
        inner.forEach(b::addStatement);
      }
      return changed;
    }
    return false;
  }

  private static List<Statement> statements(Statement s) {
    return s instanceof BlockStmt b ? b.getStatements() : List.of(s);
  }

  private static List<Statement> cloneAll(List<Statement> statements) {
    List<Statement> result = new ArrayList<>();
    for (Statement s : statements)
      result.add(s.clone());
    return result;
  }

  /** A local declared with a literal or another name and never reassigned is just that value. */
  private static boolean propagateAliases(List<Statement> body) {
    boolean changed = false;
    Map<String, Integer> assignments = new HashMap<>();
    for (Statement s : body) {
      for (AssignExpr a : s.findAll(AssignExpr.class))
        assignments.merge(a.getTarget().toString(), 1, Integer::sum);
      for (UnaryExpr u : s.findAll(UnaryExpr.class, x -> x.getOperator().name().contains("CREMENT")))
        assignments.merge(u.getExpression().toString(), 1, Integer::sum);
    }
    for (int i = 0; i < body.size(); i++) {
      if (!(body.get(i) instanceof ExpressionStmt es && es.getExpression() instanceof VariableDeclarationExpr v && v.getVariables().size() == 1))
        continue;
      VariableDeclarator d = v.getVariable(0);
      if (d.getInitializer().isEmpty() || assignments.containsKey(d.getNameAsString()))
        continue;
      Expression init = d.getInitializer().get();
      if (!(init instanceof LiteralExpr || init instanceof NameExpr n && !assignments.containsKey(n.getNameAsString()) && !isDeclaredLater(body, n.getNameAsString(), i)))
        continue;
      String name = d.getNameAsString();
      for (Statement s : body)
        for (NameExpr n : s.findAll(NameExpr.class, x -> x.getNameAsString().equals(name)))
          n.replace(init.clone());
      body.remove(i);
      changed = true;
      i--;
    }
    return changed;
  }

  private static boolean isDeclaredLater(List<Statement> body, String name, int from) {
    for (int i = from + 1; i < body.size(); i++)
      if (!body.get(i).findAll(VariableDeclarator.class, d -> d.getNameAsString().equals(name)).isEmpty())
        return true;
    return false;
  }

  /**
   * A store nobody reads before the next one to the same name: the zero a slot's declaration
   * gives it, and the same value assigned twice by two inlined bodies. Names here are locals of
   * the case or private fields of the generated class, so nothing between the two stores can see
   * the first one; a branch, a label or any read of the name ends the search.
   */
  /**
   * Masks and comparisons the generated code carries but cannot fail: a byte masked with 0xFF, a
   * jump address compared against -1. The model writes them because it does not know which
   * instance it is; here the instance is known.
   */
  private static boolean dropWhatCannotFail(List<Statement> body, Map<String, Integer> masks) {
    boolean[] changed = {false};
    for (Statement s : body) {
      for (BinaryExpr b : s.findAll(BinaryExpr.class, x -> x.getOperator() == BinaryExpr.Operator.BINARY_AND)) {
        Integer mask = Folder.intValue(b.getRight());
        int known = Folder.maskOf(b.getLeft(), masks);
        if (mask != null && mask >= 0 && known != Folder.UNKNOWN && (known & ~mask) == 0) {
          b.replace(b.getLeft().clone());
          changed[0] = true;
        }
      }
      for (BinaryExpr b : s.findAll(BinaryExpr.class, x -> x.getOperator() == BinaryExpr.Operator.EQUALS || x.getOperator() == BinaryExpr.Operator.NOT_EQUALS)) {
        Integer other = Folder.intValue(b.getRight());
        if (other == null || other >= 0 || Folder.maskOf(b.getLeft(), masks) == Folder.UNKNOWN)
          continue;
        b.replace(new BooleanLiteralExpr(b.getOperator() == BinaryExpr.Operator.NOT_EQUALS));
        changed[0] = true;
      }
    }
    return changed[0];
  }

  /**
   * What each name can hold, as the bits of every value ever stored into it. The registers are
   * fields that outlive an instruction and that the bank's own views write too, so this looks at
   * every case and at the bank's source: a width the model does not keep is a width the generated
   * code cannot assume. Masks only grow, so repeating until nothing changes reaches the answer
   * even when a name feeds itself.
   */
  public static Map<String, Integer> masksOf(List<Node> roots, Map<String, Integer> seed) {
    List<Map.Entry<String, Expression>> stores = new ArrayList<>();
    for (Node root : roots)
      collectStores(root, stores);
    Map<String, Integer> masks = new HashMap<>(seed);
    for (boolean growing = true; growing; ) {
      growing = false;
      for (Map.Entry<String, Expression> store : stores) {
        int before = masks.getOrDefault(store.getKey(), 0);
        if (before == Folder.UNKNOWN)
          continue;
        int value = Folder.maskOf(store.getValue(), masks);
        int after = value == Folder.UNKNOWN ? Folder.UNKNOWN : before | value;
        if (after != before) {
          masks.put(store.getKey(), after);
          growing = true;
        }
      }
    }
    return masks;
  }

  /** Anything that puts a value in a name: a declaration with an initializer, an assignment, an increment. */
  private static void collectStores(Node root, List<Map.Entry<String, Expression>> stores) {
    for (VariableDeclarator d : root.findAll(VariableDeclarator.class))
      d.getInitializer().ifPresent(init -> stores.add(Map.entry(d.getNameAsString(), init)));
    for (AssignExpr a : root.findAll(AssignExpr.class))
      if (a.getTarget() instanceof NameExpr n)
        stores.add(Map.entry(n.getNameAsString(), a.getOperator() == AssignExpr.Operator.ASSIGN ? a.getValue() : new NullLiteralExpr()));
    for (UnaryExpr u : root.findAll(UnaryExpr.class, x -> x.getOperator().name().contains("CREMENT")))
      if (u.getExpression() instanceof NameExpr n)
        stores.add(Map.entry(n.getNameAsString(), new NullLiteralExpr()));
  }

  private static boolean dropDeadStores(List<Statement> body) {
    boolean changed = false;
    for (int i = 0; i < body.size(); i++) {
      String name = storedName(body.get(i));
      Expression value = storedValue(body.get(i));
      if (name == null || value == null || !Specializer.isPure(value))
        continue;
      int overwritten = nextStore(body, i, name);
      if (overwritten < 0)
        continue;
      VariableDeclarator declaration = declaratorOf(body.get(i));
      if (declaration == null)
        body.remove(i--);
      else if (overwritten == i + 1) {
        declaration.setInitializer(storedValue(body.get(overwritten)).clone());
        body.remove(overwritten);
      } else
        declaration.removeInitializer();
      changed = true;
    }
    return changed;
  }

  /** The index of the plain assignment that overwrites the name, or -1 if anything could see it first. */
  private static int nextStore(List<Statement> body, int from, String name) {
    for (int i = from + 1; i < body.size(); i++) {
      Statement s = body.get(i);
      if (!(s instanceof ExpressionStmt))
        return -1;
      if (reads(s, name))
        return -1;
      if (name.equals(storedName(s)) && declaratorOf(s) == null)
        return i;
    }
    return -1;
  }

  private static boolean reads(Statement s, String name) {
    return !s.findAll(NameExpr.class, n -> n.getNameAsString().equals(name)
        && !(n.getParentNode().orElse(null) instanceof AssignExpr a && a.getTarget() == n && a.getOperator() == AssignExpr.Operator.ASSIGN)).isEmpty();
  }

  /** The name a statement stores into, whether it declares it or assigns it. */
  private static String storedName(Statement s) {
    VariableDeclarator d = declaratorOf(s);
    if (d != null)
      return d.getInitializer().isPresent() ? d.getNameAsString() : null;
    return s instanceof ExpressionStmt es && es.getExpression() instanceof AssignExpr a
        && a.getOperator() == AssignExpr.Operator.ASSIGN && a.getTarget() instanceof NameExpr n ? n.getNameAsString() : null;
  }

  private static Expression storedValue(Statement s) {
    VariableDeclarator d = declaratorOf(s);
    if (d != null)
      return d.getInitializer().orElse(null);
    return s instanceof ExpressionStmt es && es.getExpression() instanceof AssignExpr a ? a.getValue() : null;
  }

  private static VariableDeclarator declaratorOf(Statement s) {
    return s instanceof ExpressionStmt es && es.getExpression() instanceof VariableDeclarationExpr v && v.getVariables().size() == 1 ? v.getVariable(0) : null;
  }

  private static boolean dropUnusedLocals(List<Statement> body) {
    boolean changed = false;
    for (int i = 0; i < body.size(); i++) {
      if (!(body.get(i) instanceof ExpressionStmt es && es.getExpression() instanceof VariableDeclarationExpr v && v.getVariables().size() == 1))
        continue;
      VariableDeclarator d = v.getVariable(0);
      String name = d.getNameAsString();
      boolean used = false;
      for (Statement s : body)
        if (s != body.get(i) && !s.findAll(NameExpr.class, x -> x.getNameAsString().equals(name)).isEmpty())
          used = true;
      if (!used && (d.getInitializer().isEmpty() || Specializer.isPure(d.getInitializer().get()))) {
        body.remove(i);
        changed = true;
        i--;
      }
    }
    return changed;
  }
}
