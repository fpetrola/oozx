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

package com.fpetrola.z80.transform;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileNotFoundException;
import java.util.*;

public class EnhancedLoopExtractor {

  public static void main(String[] args) throws FileNotFoundException {
    String code = """ 
        public class Test {
            private int x;
            public void method() {
                int i = 0;
                int j = 0;
                while (i < 5) {
                    int y= 4;
                    j= 1;
                    x++;           // Instance variable
                    j = i * 2;      // Modified local
                    int k = j + 1 + y;  // Loop-local
                    System.out.println(k);
                    i++;
                }
            }
        }
        """;

    CompilationUnit cu = StaticJavaParser.parse(code);
//    cu = StaticJavaParser.parse(new File("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/ZxGame1.java"));

    cu.accept(new LoopExtractorVisitor(), null);
    System.out.println(cu);
  }

  private static class LoopExtractorVisitor extends ModifierVisitor<Void> {
    private int methodCounter = 0;

    @Override
    public Visitable visit(WhileStmt stmt, Void arg) {
      processLoop(stmt);
      super.visit(stmt, arg);
      return stmt;
    }

    @Override
    public Visitable visit(ForStmt stmt, Void arg) {
      processLoop(stmt);
      super.visit(stmt, arg);
      return stmt;
    }

    @Override
    public Visitable visit(DoStmt stmt, Void arg) {
      processLoop(stmt);
      super.visit(stmt, arg);
      return stmt;
    }

    private void processLoop(Statement loop) {
      // 1. Identify all variables in loop
      Set<String> loopLocals = findLoopLocalVariables(loop);
      VariableUsage usage = analyzeVariableUsage(loop, loopLocals);

      // 2. Create extracted method
      String methodName = "loopMethod" + methodCounter++;
      MethodDeclaration extracted = createExtractedMethod(methodName, loop, usage);
      findParentClass(loop).ifPresent(c -> c.addMember(extracted));

      // 3. Replace loop with method call
      loop.replace(createReplacementCall(methodName, usage));
    }

    private Set<String> findLoopLocalVariables(Statement loop) {
      Set<String> locals = new HashSet<>();
      loop.findAll(VariableDeclarator.class).forEach(vd -> locals.add(vd.getNameAsString()));
      return locals;
    }

    private VariableUsage analyzeVariableUsage(Statement loop, Set<String> loopLocals) {
      VariableUsage usage = new VariableUsage();

      loop.accept(new VoidVisitorAdapter<Void>() {
        @Override
        public void visit(NameExpr n, Void arg) {
          String name = n.getNameAsString();
          if (loopLocals.contains(name)) return;
          if (isLocalVariable(n)) usage.addUsedLocal(name);
        }

        @Override
        public void visit(AssignExpr expr, Void arg) {
          if (expr.getTarget().isNameExpr()) {
            String name = expr.getTarget().asNameExpr().getNameAsString();
            if (!loopLocals.contains(name) && isLocalVariable(expr.getTarget()))
              usage.addModifiedLocal(name);
          }
        }

        @Override
        public void visit(UnaryExpr expr, Void arg) {
          if (expr.getExpression().isNameExpr()) {
            String name = expr.getExpression().asNameExpr().getNameAsString();
            if (!loopLocals.contains(name) && isLocalVariable(expr.getExpression()))
              usage.addModifiedLocal(name);
          }
        }
      }, null);
      return usage;
    }

    private boolean isLocalVariable(Expression expr) {
      Optional<MethodDeclaration> ancestor = expr.findAncestor(MethodDeclaration.class);
      return ancestor
          .map(md -> md.findAll(VariableDeclarator.class).stream()
              .anyMatch(vd -> vd.getNameAsExpression().equals(expr)))
          .orElse(false);
    }

    private MethodDeclaration createExtractedMethod(String name, Statement loop, VariableUsage usage) {
      MethodDeclaration method = new MethodDeclaration()
          .setName(name)
          .setPrivate(true)
          .setType(usage.modifiedLocals.isEmpty() ?
              StaticJavaParser.parseType("void") :
              new ArrayType(StaticJavaParser.parseType("Object")));

      // Add parameters for used locals
      usage.allLocals().forEach(var ->
          method.addParameter(new Parameter(StaticJavaParser.parseType("int"), var)));

      // Build method body
      BlockStmt body = new BlockStmt().addStatement(loop.clone());

      // Add return statement for modified locals
      if (!usage.modifiedLocals.isEmpty()) {
        NodeList<Expression> returns = new NodeList<>();
        usage.modifiedLocals.forEach(var -> returns.add(new NameExpr(var)));
        body.addStatement(new ReturnStmt(
            new ArrayCreationExpr(new ArrayType(StaticJavaParser.parseType("Object")), new NodeList(new ArrayCreationLevel[]{new ArrayCreationLevel()}),
                new ArrayInitializerExpr(returns)
            )));
      }
      return method.setBody(body);
    }

    private Statement createReplacementCall(String methodName, VariableUsage usage) {
      MethodCallExpr call = new MethodCallExpr(null, methodName);
      usage.allLocals().forEach(var -> call.addArgument(new NameExpr(var)));

      if (usage.modifiedLocals.isEmpty()) return new ExpressionStmt(call);

      // Handle modified variables
      BlockStmt block = new BlockStmt();
      VariableDeclarationExpr resultVar = new VariableDeclarationExpr(
          new VariableDeclarator(new ArrayType(StaticJavaParser.parseType("Object")), "result", call));
      block.addStatement(resultVar);

      int index = 0;
      for (String var : usage.modifiedLocals) {
        CastExpr cast = new CastExpr(StaticJavaParser.parseType("int"),
            new ArrayAccessExpr(new NameExpr("result"), new IntegerLiteralExpr(index++)));
        block.addStatement(new ExpressionStmt(new AssignExpr(
            new NameExpr(var), cast, AssignExpr.Operator.ASSIGN
        )));
      }
      return block;
    }

    private Optional<ClassOrInterfaceDeclaration> findParentClass(Node node) {
      return node.findAncestor(ClassOrInterfaceDeclaration.class);
    }
  }

  private static class VariableUsage {
    Set<String> usedLocals = new LinkedHashSet<>();
    Set<String> modifiedLocals = new LinkedHashSet<>();

    void addUsedLocal(String name) {
      usedLocals.add(name);
    }

    void addModifiedLocal(String name) {
      modifiedLocals.add(name);
    }

    Set<String> allLocals() {
      Set<String> all = new LinkedHashSet<>(usedLocals);
      all.addAll(modifiedLocals);
      return all;
    }
  }
}