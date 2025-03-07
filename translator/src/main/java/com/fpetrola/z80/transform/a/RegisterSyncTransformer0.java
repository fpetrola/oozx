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

public class RegisterSyncTransformer0 {

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

      // Track split/merge needed for each 16-bit register
      Map<String, Boolean> splitNeeded = new HashMap<>();
      Map<String, Boolean> mergeNeeded = new HashMap<>();
      REGISTER_PAIRS.keySet().forEach(reg -> {
        updateSplitNeeded(reg, splitNeeded, false);
        mergeNeeded.put(reg, false);
      });

      while (iterator.hasNext()) {
        Statement stmt = iterator.next();
        if (isGenerated(stmt)) continue;

        // Collect reads and writes in this statement
        Set<String> reads = new HashSet<>();
        Set<String> writes = new HashSet<>();
        collectReadsAndWrites(stmt, reads, writes);

        // Check if we need to insert split or merge before this statement
        List<Statement> codeToInsert = new ArrayList<>();

        // Process reads for 8-bit registers (check if split needed)
        for (String read : reads) {
          for (Map.Entry<String, List<String>> entry : REGISTER_PAIRS.entrySet()) {
            String reg16 = entry.getKey();
            List<String> reg8s = entry.getValue();
            if (reg8s.contains(read) && isSplitNeeded(splitNeeded, reg16)) {
              // Insert split code
              Expression hl = new NameExpr(reg16);
              Expression hExpr =
                  new BinaryExpr(hl, new IntegerLiteralExpr("8"), BinaryExpr.Operator.SIGNED_RIGHT_SHIFT);
              Expression lExpr =
                  new BinaryExpr(hl.clone(), new IntegerLiteralExpr("0xFF"), BinaryExpr.Operator.BINARY_AND);

              ExpressionStmt expressionStmt = new ExpressionStmt(new AssignExpr(
                  new NameExpr(reg8s.get(0)), hExpr, AssignExpr.Operator.ASSIGN
              ));
              expressionStmt.addOrphanComment(new LineComment(" generated split "));
              Statement hAssign = expressionStmt;
              ExpressionStmt expressionStmt1 = new ExpressionStmt(new AssignExpr(
                  new NameExpr(reg8s.get(1)), lExpr, AssignExpr.Operator.ASSIGN
              ));
              expressionStmt1.addOrphanComment(new LineComment(" generated split "));
              ;
              Statement lAssign = expressionStmt1;

              if (splitNeeded.get(reg8s.get(0)))
                codeToInsert.add(hAssign);
              if (splitNeeded.get(reg8s.get(1)))
                codeToInsert.add(lAssign);
              updateSplitNeeded(reg16, splitNeeded, false);
              mergeNeeded.put(reg16, false);
              break;
            }
          }
        }

        // Process reads for 16-bit registers (check if merge needed)
        for (String read : reads) {
          if (REGISTER_PAIRS.containsKey(read) && isSplitNeeded(mergeNeeded, read)) {
            List<String> reg8s = REGISTER_PAIRS.get(read);
            Expression h = new NameExpr(reg8s.get(0));
            Expression l = new NameExpr(reg8s.get(1));

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
            Statement hlAssign = expressionStmt;

            codeToInsert.add(hlAssign);
            mergeNeeded.put(read, false);
            updateSplitNeeded(read, splitNeeded, false);
          }
        }

        // Insert generated code before the current statement
        if (!codeToInsert.isEmpty()) {
          iterator.previous();
          codeToInsert.forEach(iterator::add);
        }

        // Process writes to update flags
        for (String write : writes) {
          if (REGISTER_PAIRS.containsKey(write)) {
            // 16-bit register written
            updateSplitNeeded(write, splitNeeded, true);
            mergeNeeded.put(write, false);
          } else {
            // Check if it's an 8-bit register
            for (Map.Entry<String, List<String>> entry : REGISTER_PAIRS.entrySet()) {
              String reg16 = entry.getKey();
              List<String> reg8s = entry.getValue();
              if (reg8s.contains(write)) {
                mergeNeeded.put(reg16, true);
                splitNeeded.put(write, false);
//                updateSplitNeeded(reg16, splitNeeded, false);
                break;
              }
            }
          }
        }
      }

      // Replace the method body with modified statements
      body.setStatements(NodeList.nodeList(statements));
    }

    private Boolean isSplitNeeded(Map<String, Boolean> splitNeeded, String reg16) {
      List<String> reg8s = REGISTER_PAIRS.get(reg16);
      Boolean b = splitNeeded.get(reg8s.get(0));
      Boolean b1 = splitNeeded.get(reg8s.get(1));
      if (b == null)
        b = false;
      if (b1 == null)
        b1 = false;
      return b || b1;
    }

    private void updateSplitNeeded(String write, Map<String, Boolean> splitNeeded, boolean value) {
      List<String> reg8s = REGISTER_PAIRS.get(write);
      reg8s.forEach(r -> splitNeeded.put(r, value));
    }

    private void collectReadsAndWrites(Statement stmt, Set<String> reads, Set<String> writes) {
      stmt.accept(new VoidVisitorAdapter<Expression>() {
        @Override
        public void visit(NameExpr n, Expression assigned) {
          super.visit(n, assigned);
          String name = n.getNameAsString();
          if (assigned != null && !assigned.toString().equals(name)) {
            if (isRegister(name)) {
              reads.add(name);
            }
          }
        }

        @Override
        public void visit(AssignExpr expr, Expression arg) {
          super.visit(expr, expr.getTarget());
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
    // Example usage
    String code = """
                class Test { 
                  void test() { 
                     HL = 0x1234; 
                     int x = H; 
        
                     BC = 0x1266; 
                     BC++;
                     A= mem[BC];
                     H= B;
        
                     C= 4;
                     D= H+6;
//                     C=L+2;
                     E= mem[HL];
                  } 
                }
        """;
    CompilationUnit cu = StaticJavaParser.parse(code);
    new RegisterSyncTransformer0().transform(cu);
    System.out.println(cu);
  }
}