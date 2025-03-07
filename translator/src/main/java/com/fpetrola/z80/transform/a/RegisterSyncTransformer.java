/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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
package com.fpetrola.z80.transform.a;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

public class RegisterSyncTransformer {

  private static final Map<String, List<String>> REGISTER_PAIRS = new HashMap<>();

  static {
    REGISTER_PAIRS.put("AF", Arrays.asList("A", "F"));
    REGISTER_PAIRS.put("BC", Arrays.asList("B", "C"));
    REGISTER_PAIRS.put("DE", Arrays.asList("D", "E"));
    REGISTER_PAIRS.put("HL", Arrays.asList("H", "L"));
    REGISTER_PAIRS.put("IX", Arrays.asList("IXH", "IXL"));
    REGISTER_PAIRS.put("IY", Arrays.asList("IYH", "IYL"));
  }

  public void transform(CompilationUnit cu) {
    cu.accept(new ClassVisitor(), null);
  }

  private static class ClassVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(ClassOrInterfaceDeclaration cls, Void arg) {
      super.visit(cls, arg);
      cls.getMethods().forEach(method -> method.accept(new MethodVisitor(), null));
    }
  }

  private static class MethodVisitor extends VoidVisitorAdapter<Void> {
    private Map<String, Boolean> splitNeeded = new HashMap<>();
    private Map<String, Boolean> mergeNeeded = new HashMap<>();

    @Override
    public void visit(MethodDeclaration method, Void arg) {
      super.visit(method, arg);
      if (!method.getBody().isPresent()) return;

      BlockStmt body = method.getBody().get();
      List<Statement> statements = new ArrayList<>(body.getStatements());
      ListIterator<Statement> iterator = statements.listIterator();

      REGISTER_PAIRS.values().forEach(reg8s -> reg8s.forEach(reg -> splitNeeded.put(reg, false)));
      REGISTER_PAIRS.keySet().forEach(reg16 -> mergeNeeded.put(reg16, false));

      while (iterator.hasNext()) {
        Statement stmt = iterator.next();
        if (isGenerated(stmt)) continue;

        Set<String> reads = new HashSet<>();
        Set<String> writes = new HashSet<>();
        collectReadsAndWrites(stmt, reads, writes);

        List<Statement> codeToInsert = new ArrayList<>();

        for (String read : reads) {
          for (Map.Entry<String, List<String>> entry : REGISTER_PAIRS.entrySet()) {
            String reg16 = entry.getKey();
            List<String> reg8s = entry.getValue();
            if (reg8s.contains(read) && splitNeeded.getOrDefault(read, false)) {
              Expression hl = new NameExpr(reg16);
              if (read.equals(reg8s.get(0))) {
                Expression hExpr = new CastExpr(
                    StaticJavaParser.parseType("byte"),

                    new BinaryExpr(hl.clone(), new IntegerLiteralExpr("8"),
                        BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT

                    ));
                codeToInsert.add(createSplitAssign(reg8s.get(0), hExpr));
                splitNeeded.put(reg8s.get(0), false);
              } else {
                Expression lExpr = new CastExpr(
                    StaticJavaParser.parseType("byte"),
                    new BinaryExpr(hl.clone(), new IntegerLiteralExpr("0xFF"),
                        BinaryExpr.Operator.BINARY_AND)
                );
                codeToInsert.add(createSplitAssign(reg8s.get(1), lExpr));
                splitNeeded.put(reg8s.get(1), false);
              }
              mergeNeeded.put(reg16, false);
            }
          }
        }

        for (String read : reads) {
          if (REGISTER_PAIRS.containsKey(read) && mergeNeeded.getOrDefault(read, false)) {
            List<String> reg8s = REGISTER_PAIRS.get(read);
            Expression h = new CastExpr(StaticJavaParser.parseType("int"), new NameExpr(reg8s.get(0)));
            Expression l = new CastExpr(StaticJavaParser.parseType("int"), new NameExpr(reg8s.get(1)));

            Expression hlValue = new BinaryExpr(
                new BinaryExpr(h, new IntegerLiteralExpr("8"), BinaryExpr.Operator.LEFT_SHIFT),
                l,
                BinaryExpr.Operator.BINARY_OR
            );
            codeToInsert.add(createMergeAssign(read, hlValue));
            mergeNeeded.put(read, false);
          }
        }

        if (!codeToInsert.isEmpty()) {
          iterator.previous();
          codeToInsert.forEach(iterator::add);
          iterator.next();
        }

        for (String write : writes) {
          if (REGISTER_PAIRS.containsKey(write)) {
            REGISTER_PAIRS.get(write).forEach(reg -> splitNeeded.put(reg, true));
            mergeNeeded.put(write, false);
          } else {
            REGISTER_PAIRS.forEach((reg16, reg8s) -> {
              if (reg8s.contains(write)) {
                mergeNeeded.put(reg16, true);
                splitNeeded.put(write, false);
              }
            });
          }
        }
      }
      body.setStatements(NodeList.nodeList(statements));
    }

    private Statement createSplitAssign(String reg, Expression expr) {
      ExpressionStmt expressionStmt = new ExpressionStmt(new AssignExpr(
          new NameExpr(reg), expr, AssignExpr.Operator.ASSIGN
      ));
      expressionStmt.addOrphanComment(new LineComment(" generated split "));
      return expressionStmt;
    }

    private Statement createMergeAssign(String reg, Expression expr) {
      ExpressionStmt expressionStmt = new ExpressionStmt(new AssignExpr(
          new NameExpr(reg), expr, AssignExpr.Operator.ASSIGN
      ));
      expressionStmt.addOrphanComment(new LineComment(" generated merge "));
      return expressionStmt;
    }

    private void collectReadsAndWrites(Statement stmt, Set<String> reads, Set<String> writes) {
      stmt.accept(new VoidVisitorAdapter<Void>() {
        @Override
        public void visit(NameExpr n, Void arg) {
          super.visit(n, arg);
          String name = n.getNameAsString();
          if (isRegister(name) && !writes.contains(name)) {
            reads.add(name);
          }
        }

        @Override
        public void visit(AssignExpr expr, Void arg) {
          super.visit(expr, arg);
          if (expr.getTarget().isNameExpr()) {
            String name = expr.getTarget().asNameExpr().getNameAsString();
            if (isRegister(name)) {
              writes.add(name);
            }
          }
        }
      }, null);
    }

    private boolean isRegister(String name) {
      return REGISTER_PAIRS.containsKey(name) || REGISTER_PAIRS.values().stream()
          .anyMatch(reg8s -> reg8s.contains(name));
    }

    private boolean isGenerated(Statement stmt) {
      return stmt.getComment().map(c -> c.getContent().contains("generated")).orElse(false);
    }
  }

  public static void main(String[] args) {
    String code = "class Test { void test() { HL = 0x1234; method(H); } void method(byte h) { int x = HL; } }";
    CompilationUnit cu = StaticJavaParser.parse(code);
    new RegisterSyncTransformer().transform(cu);
    System.out.println(cu);
  }
}