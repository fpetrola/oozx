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

public class RegisterSyncTransformer1 {

  // Define 16-bit registers and their corresponding 8-bit registers
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
      cls.getMethods().forEach(method -> new MethodVisitor().visit(method, null));
    }
  }

  private static class MethodVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(MethodDeclaration method, Void arg) {
      super.visit(method, arg);
      if (!method.getBody().isPresent()) return;

      BlockStmt body = method.getBody().get();
      List<Statement> statements = new ArrayList<>(body.getStatements());
      ListIterator<Statement> iterator = statements.listIterator();

      // Track split/merge needed for each register
      Map<String, Boolean> splitNeeded = new HashMap<>();
      Map<String, Boolean> mergeNeeded = new HashMap<>();
      REGISTER_PAIRS.values().forEach(reg8s -> reg8s.forEach(reg -> splitNeeded.put(reg, false)));
      REGISTER_PAIRS.keySet().forEach(reg16 -> mergeNeeded.put(reg16, false));

      while (iterator.hasNext()) {
        Statement stmt = iterator.next();
        if (isGenerated(stmt)) continue;

        // Collect reads and writes in this statement
        Set<String> reads = new HashSet<>();
        Set<String> writes = new HashSet<>();
        collectReadsAndWrites(stmt, reads, writes);

        List<Statement> codeToInsert = new ArrayList<>();

        // Process reads for 8-bit registers (split if needed)
        for (String read : reads) {
          for (Map.Entry<String, List<String>> entry : REGISTER_PAIRS.entrySet()) {
            String reg16 = entry.getKey();
            List<String> reg8s = entry.getValue();
            if (reg8s.contains(read) && splitNeeded.getOrDefault(read, false)) {
              // Generate split code for the accessed 8-bit register
              Expression hl = new NameExpr(reg16);
              Expression shiftAmount = new IntegerLiteralExpr("8");
              Expression mask = new IntegerLiteralExpr("0xFF");

              if (read.equals(reg8s.get(0))) {
                // Split high byte (H)
                Expression hExpr =
                    new BinaryExpr(hl.clone(), shiftAmount, BinaryExpr.Operator.SIGNED_RIGHT_SHIFT);
                ExpressionStmt expressionStmt = new ExpressionStmt(new AssignExpr(
                    new NameExpr(reg8s.get(0)), hExpr, AssignExpr.Operator.ASSIGN
                ));
                expressionStmt.addOrphanComment(new LineComment(" generated split "));
                codeToInsert.add(expressionStmt);
                splitNeeded.put(reg8s.get(0), false);
              } else {
                // Split low byte (L)
                Expression lExpr =
                    new BinaryExpr(hl.clone(), mask, BinaryExpr.Operator.BINARY_AND);
                ExpressionStmt expressionStmt = new ExpressionStmt(new AssignExpr(
                    new NameExpr(reg8s.get(1)), lExpr, AssignExpr.Operator.ASSIGN
                ));
                expressionStmt.addOrphanComment(new LineComment(" generated split "));
                codeToInsert.add(expressionStmt);
                splitNeeded.put(reg8s.get(1), false);
              }
              mergeNeeded.put(reg16, false); // HL is now in sync
              break;
            }
          }
        }

        // Process reads for 16-bit registers (merge if needed)
        for (String read : reads) {
          if (REGISTER_PAIRS.containsKey(read) && mergeNeeded.getOrDefault(read, false)) {
            List<String> reg8s = REGISTER_PAIRS.get(read);
            Expression h = new NameExpr(reg8s.get(0));
            Expression l = new NameExpr(reg8s.get(1));

            // Cast to int to handle sign correctly
            Expression hShift = new BinaryExpr(
                h.clone(),
                new IntegerLiteralExpr("8"), BinaryExpr.Operator.LEFT_SHIFT
            );
            Expression lMask = new BinaryExpr(
                l.clone(),
                new IntegerLiteralExpr("0xFF"), BinaryExpr.Operator.BINARY_AND
            );
            Expression hlValue = new BinaryExpr(hShift, lMask, BinaryExpr.Operator.BINARY_OR);

            ExpressionStmt expressionStmt = new ExpressionStmt(new AssignExpr(
                new NameExpr(read), hlValue, AssignExpr.Operator.ASSIGN
            ));
            expressionStmt.addOrphanComment(new LineComment(" generated merge "));
            codeToInsert.add(expressionStmt);
            mergeNeeded.put(read, false);
          }
        }

        // Insert generated code before the current statement
        if (!codeToInsert.isEmpty()) {
          iterator.previous();
          codeToInsert.forEach(iterator::add);
          iterator.next();
        }

        // Update flags based on writes
        for (String write : writes) {
          if (REGISTER_PAIRS.containsKey(write)) {
            // 16-bit write: mark both 8-bit as needing split
            REGISTER_PAIRS.get(write).forEach(reg -> splitNeeded.put(reg, true));
            mergeNeeded.put(write, false);
          } else {
            // 8-bit write: mark corresponding 16-bit for merge
            REGISTER_PAIRS.forEach((reg16, reg8s) -> {
              if (reg8s.contains(write)) {
                mergeNeeded.put(reg16, true);
                splitNeeded.put(write, false); // Written 8-bit is up-to-date
              }
            });
          }
        }
      }

      body.setStatements(NodeList.nodeList(statements));
    }

    private void collectReadsAndWrites(Statement stmt, Set<String> reads, Set<String> writes) {
      stmt.accept(new VoidVisitorAdapter<Expression>() {
        public void visit(NameExpr n, Expression assigned) {
          super.visit(n, assigned);
          String name = n.getNameAsString();
          if (isRegister(name) && !writes.contains(name)) {
            if (n.getParentNode().get() instanceof AssignExpr assignExpr) {
              if (assignExpr.getTarget() == n) {
                return;
              }
            }
            reads.add(name);
          }
        }

        public void visit(AssignExpr expr, Expression assigned) {
          super.visit(expr, assigned);
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
    String code = """
        class Test {
            void test() {
                HL = 0x1234;
                int x = H;
                BC = 0x1266;
                BC++;
                A = mem[BC];
                H = B;
                C = 4;
                HL = 0x3456;
                D = H + 6;
                C = L + 2;
                H = 10;
                L = 20;
                E = mem[HL];
            }
        }
        """;
    CompilationUnit cu = StaticJavaParser.parse(code);
    new RegisterSyncTransformer1().transform(cu);
    System.out.println(cu);
  }
}