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
  public static void simplify(List<Statement> body) {
    boolean changed = true;
    for (int round = 0; changed && round < 8; round++) {
      changed = foldAll(body);
      changed |= dropDeadBranches(body);
      changed |= propagateAliases(body);
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
