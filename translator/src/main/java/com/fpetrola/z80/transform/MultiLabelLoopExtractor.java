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

import java.util.*;

public class MultiLabelLoopExtractor {

  public static void main(String[] args) {
    String code = "public class Test {\n" +
        "    public void method() {\n" +
        "        outer:\n" +
        "        while (true) {\n" +
        "            inner:\n" +
        "            for (int i = 0; i < 10; i++) {\n" +
        "                if (i == 3) break outer;\n" +
        "                if (i == 5) continue inner;\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "}\n";

    CompilationUnit cu = StaticJavaParser.parse(code);
    cu.accept(new MultiLabelVisitor(), null);
    System.out.println(cu);
  }

  private static class MultiLabelVisitor extends ModifierVisitor<Void> {
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
      // Collect all labels referenced in control flow statements
      Set<String> labels = new HashSet<>();
      loop.accept(new VoidVisitorAdapter<Void>() {
        @Override
        public void visit(BreakStmt stmt, Void arg) {
          stmt.getLabel().ifPresent(e -> labels.add(e.asString()));
        }

        @Override
        public void visit(ContinueStmt stmt, Void arg) {
          stmt.getLabel().ifPresent(e -> labels.add(e.asString()));
        }
      }, null);

      // Generate extracted method and replacement logic
      String methodName = "extractedLoop" + methodCounter++;
      MethodDeclaration extracted = createExtractedMethod(loop, methodName, labels);
      findParentClass(loop).ifPresent(c -> c.addMember(extracted));
      loop.replace(createReplacementLogic(methodName, labels));
    }

    private MethodDeclaration createExtractedMethod(Statement loop, String methodName, Set<String> labels) {
      MethodDeclaration method = new MethodDeclaration()
          .setName(methodName)
          .setPrivate(true)
          .setType(new ArrayType(StaticJavaParser.parseType("Object")));

      // Replace control flow with return flags
      Statement clonedLoop = loop.clone();
      clonedLoop.accept(new VoidVisitorAdapter<Void>() {
        @Override
        public void visit(BreakStmt stmt, Void arg) {
          NodeList<Expression> args = new NodeList<>();
          args.add(new StringLiteralExpr("break"));
          stmt.getLabel().ifPresent(l -> args.add(new StringLiteralExpr(l.asString())));
          stmt.replace(new ReturnStmt(createControlFlowArray(args)));
        }

        @Override
        public void visit(ContinueStmt stmt, Void arg) {
          NodeList<Expression> args = new NodeList<>();
          args.add(new StringLiteralExpr("continue"));
          stmt.getLabel().ifPresent(l -> args.add(new StringLiteralExpr(l.asString())));
          stmt.replace(new ReturnStmt(createControlFlowArray(args)));
        }
      }, null);

      method.setBody(new BlockStmt().addStatement(clonedLoop));
      return method;
    }

    private ArrayCreationExpr createControlFlowArray(NodeList<Expression> elements) {
      return new ArrayCreationExpr(new ArrayType(StaticJavaParser.parseType("Object")), new NodeList(new ArrayCreationLevel[]{new ArrayCreationLevel()}),
          new ArrayInitializerExpr(elements)
      );
    }

    private Statement createReplacementLogic(String methodName, Set<String> labels) {
      BlockStmt block = new BlockStmt();

      // Call extracted method: Object[] result = extractedLoop();
      VariableDeclarationExpr resultVar = new VariableDeclarationExpr(
          new VariableDeclarator(new ArrayType(StaticJavaParser.parseType("Object")), "result",
              new MethodCallExpr(null, methodName))
      );
      block.addStatement(new ExpressionStmt(resultVar));

      // Generate label handlers
      for (String label : labels) {
        // Break handler
        BinaryExpr binaryExpr = new BinaryExpr(
            new ArrayAccessExpr(new NameExpr("result"), new IntegerLiteralExpr(0)),
            new StringLiteralExpr("break"),
            BinaryExpr.Operator.EQUALS
        );
        BinaryExpr binaryExpr1 = new BinaryExpr(
            new ArrayAccessExpr(new NameExpr("result"), new IntegerLiteralExpr(1)),
            new StringLiteralExpr(label),
            BinaryExpr.Operator.EQUALS
        );

        BinaryExpr binaryExpr2 = new BinaryExpr(
            binaryExpr, binaryExpr1,
            BinaryExpr.Operator.AND
        );


        block.addStatement(new IfStmt(binaryExpr2, new BreakStmt(label), null));

        // Continue handler
        BinaryExpr binaryExpr3 = new BinaryExpr(
            new ArrayAccessExpr(new NameExpr("result"), new IntegerLiteralExpr(0)),
            new StringLiteralExpr("continue"),
            BinaryExpr.Operator.EQUALS
        );
        BinaryExpr binaryExpr4 = new BinaryExpr(
            new ArrayAccessExpr(new NameExpr("result"), new IntegerLiteralExpr(1)),
            new StringLiteralExpr(label),
            BinaryExpr.Operator.EQUALS
        );

        BinaryExpr binaryExpr5 = new BinaryExpr(
            binaryExpr3, binaryExpr4,
            BinaryExpr.Operator.AND
        );

        block.addStatement(new IfStmt(binaryExpr5, new ContinueStmt(label), null));
      }

      return block;
    }

    private Optional<ClassOrInterfaceDeclaration> findParentClass(Node node) {
      return node.findAncestor(ClassOrInterfaceDeclaration.class);
    }
  }
}