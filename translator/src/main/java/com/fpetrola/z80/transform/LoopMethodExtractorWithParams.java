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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LoopMethodExtractorWithParams {

  public static void main(String[] args) throws FileNotFoundException {
    String code = "public class Test {\n" +
        "    public void method() {\n" +
        "        int i = 0;\n" +
        "        int j = 0;\n" +
        "        while (i < 5) {\n" +
        "            int a=(i + j);\n" +
        "            i++;\n" +
        "            j += 2;\n" +
        "        }\n" +
        "    }\n" +
        "}\n";

    CompilationUnit cu = StaticJavaParser.parse(new File("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/ZxGame1.java"));

//    CompilationUnit cu = StaticJavaParser.parse(code);
    cu.accept(new LoopMethodExtractorVisitor(), null);
    System.out.println(cu);
  }

  private static class LoopMethodExtractorVisitor extends ModifierVisitor<Void> {
    private int methodCounter = 0;

    @Override
    public Visitable visit(WhileStmt whileStmt, Void arg) {
      extractLoop(whileStmt);
      super.visit(whileStmt, arg);
      return whileStmt;
    }

    @Override
    public Visitable visit(ForStmt forStmt, Void arg) {
      extractLoop(forStmt);
      super.visit(forStmt, arg);
      return forStmt;
    }

    @Override
    public Visitable visit(DoStmt doStmt, Void arg) {
      extractLoop(doStmt);
      super.visit(doStmt, arg);
      return doStmt;
    }

    private void extractLoop(Statement loopStmt) {
      List<String> usedVars = new ArrayList<>();
      List<String> modifiedVars = new ArrayList<>();
      collectVariables(loopStmt, usedVars, modifiedVars);

      String methodName = "extractedLoop" + methodCounter++;
      MethodDeclaration method = createExtractedMethod(methodName, loopStmt, usedVars, modifiedVars);
      Optional<ClassOrInterfaceDeclaration> parentClass = findParentClass(loopStmt);
      parentClass.ifPresent(c -> c.addMember(method));

      Statement replacement = createReplacementStatement(methodName, usedVars, modifiedVars);
      loopStmt.replace(replacement);
    }

    private void collectVariables(Statement loopStmt, List<String> usedVars, List<String> modifiedVars) {
      loopStmt.accept(new VoidVisitorAdapter<Void>() {
        @Override
        public void visit(NameExpr n, Void arg) {
          super.visit(n, arg);
          if (!usedVars.contains(n.getNameAsString())) {
            usedVars.add(n.getNameAsString());
          }
        }

        @Override
        public void visit(AssignExpr expr, Void arg) {
          super.visit(expr, arg);
          if (expr.getTarget().isNameExpr()) {
            String varName = expr.getTarget().asNameExpr().getNameAsString();
            if (!modifiedVars.contains(varName)) {
              modifiedVars.add(varName);
            }
          }
        }

        @Override
        public void visit(UnaryExpr expr, Void arg) {
          super.visit(expr, arg);
          if (/*expr.getOperator().isIncrementOrDecrement() && */expr.getExpression().isNameExpr()) {
            String varName = expr.getExpression().asNameExpr().getNameAsString();
            if (!modifiedVars.contains(varName)) {
              modifiedVars.add(varName);
            }
          }
        }
      }, null);
    }

    private MethodDeclaration createExtractedMethod(
        String methodName, Statement loopStmt, List<String> usedVars, List<String> modifiedVars
    ) {
      MethodDeclaration method = new MethodDeclaration();
      method.setName(methodName);
      method.setPrivate(true);

      // Add parameters for all used variables
      for (String var : usedVars) {
        method.addParameter(new Parameter(StaticJavaParser.parseType("int"), var));
      }

      // Set return type (Object[] if modified variables exist)
      if (!modifiedVars.isEmpty()) {
        method.setType(new ArrayType(StaticJavaParser.parseType("Object")));
      } else {
        method.setType(StaticJavaParser.parseType("void"));
      }

      // Clone loop and build method body
      BlockStmt methodBody = new BlockStmt();
      methodBody.addStatement(loopStmt.clone());

      // Add return statement for modified variables
      if (!modifiedVars.isEmpty()) {
        NodeList<Expression> returnExpressions = new NodeList<>();
        for (String var : modifiedVars) {
          returnExpressions.add(new NameExpr(var));
        }
        methodBody.addStatement(new ReturnStmt(
            new ArrayCreationExpr(new ArrayType(StaticJavaParser.parseType("Object")), new NodeList(new ArrayCreationLevel[]{new ArrayCreationLevel()}),
                new ArrayInitializerExpr(returnExpressions)
            )));
      }

      method.setBody(methodBody);
      return method;
    }

    private Statement createReplacementStatement(
        String methodName, List<String> usedVars, List<String> modifiedVars
    ) {
      NodeList<Expression> args = new NodeList<>();
      for (String var : usedVars) {
        args.add(new NameExpr(var));
      }

      MethodCallExpr methodCall = new MethodCallExpr(null, methodName, args);

      if (modifiedVars.isEmpty()) {
        return new ExpressionStmt(methodCall);
      } else {
        // Create: Object[] result = extractedLoopX(...);
        VariableDeclarationExpr resultVar = new VariableDeclarationExpr(
            new VariableDeclarator(new ArrayType(StaticJavaParser.parseType("Object")), "result", methodCall)
        );

        // Assign back modified variables from result array
        BlockStmt block = new BlockStmt();
        block.addStatement(new ExpressionStmt(resultVar));

        for (int i = 0; i < modifiedVars.size(); i++) {
          String var = modifiedVars.get(i);
          Expression assignment = new AssignExpr(
              new NameExpr(var),
              new CastExpr(
                  StaticJavaParser.parseType("int"),
                  new ArrayAccessExpr(new NameExpr("result"), new IntegerLiteralExpr(i))

              ),
              AssignExpr.Operator.ASSIGN
          );
          block.addStatement(new ExpressionStmt(assignment));
        }

        return block;
      }
    }

    private Optional<ClassOrInterfaceDeclaration> findParentClass(Statement stmt) {
      Node parent = stmt.getParentNode().orElse(null);
      while (parent != null) {
        if (parent instanceof ClassOrInterfaceDeclaration) {
          return Optional.of((ClassOrInterfaceDeclaration) parent);
        }
        parent = parent.getParentNode().orElse(null);
      }
      return Optional.empty();
    }
  }
}