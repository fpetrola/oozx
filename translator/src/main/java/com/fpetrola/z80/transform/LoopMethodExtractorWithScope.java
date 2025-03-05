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
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class LoopMethodExtractorWithScope {

    public static void main(String[] args) throws FileNotFoundException {
        String code = "public class Test {\n" +
                      "    public void method() {\n" +
                      "        int i = 0;\n" +
                      "        while (i < 5) {\n" +
                      "            int j = i * 2;\n" + // j declared inside loop
                      "            System.out.println(j);\n" +
                      "            i++;\n" +
                      "        }\n" +
                      "    }\n" +
                      "}\n";

        CompilationUnit cu = StaticJavaParser.parse(new File("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/ZxGame1.java"));

//        CompilationUnit cu = StaticJavaParser.parse(code);
        cu.accept(new LoopMethodExtractorVisitor(), null);
        System.out.println(cu);
    }

    private static class LoopMethodExtractorVisitor extends ModifierVisitor<Void> {
        private int methodCounter = 0;

        @Override
        public Visitable visit(WhileStmt whileStmt, Void arg) { extractLoop(whileStmt); super.visit(whileStmt, arg);
          return whileStmt;
        }
        @Override
        public Visitable visit(ForStmt forStmt, Void arg) { extractLoop(forStmt); super.visit(forStmt, arg);
          return forStmt;
        }
        @Override
        public Visitable visit(DoStmt doStmt, Void arg) { extractLoop(doStmt); super.visit(doStmt, arg);
          return doStmt;
        }

        private void extractLoop(Statement loopStmt) {
            // 1. Find variables DECLARED INSIDE the loop
            List<String> declaredInLoop = getDeclaredVariables(loopStmt);

            // 2. Collect USED and MODIFIED variables (excluding declared-in-loop)
            List<String> usedVars = new ArrayList<>();
            List<String> modifiedVars = new ArrayList<>();
            collectVariables(loopStmt, declaredInLoop, usedVars, modifiedVars);

            // 3. Create method with filtered variables
            String methodName = "extractedLoop" + methodCounter++;
            MethodDeclaration method = createMethod(methodName, loopStmt, usedVars, modifiedVars);
            findParentClass(loopStmt).ifPresent(c -> c.addMember(method));

            // 4. Replace loop with method call
            loopStmt.replace(createReplacement(methodName, usedVars, modifiedVars));
        }

        // Helper 1: Find variables declared IN THE LOOP
        private List<String> getDeclaredVariables(Statement loopStmt) {
            List<String> declared = new ArrayList<>();
            loopStmt.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(VariableDeclarator vd, Void arg) {
                    declared.add(vd.getNameAsString());
                }
            }, null);
            return declared;
        }

        // Helper 2: Collect USED and MODIFIED variables (excluding declared-in-loop)
        private void collectVariables(Statement loopStmt, List<String> declaredInLoop,
                                     List<String> usedVars, List<String> modifiedVars) {
            loopStmt.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(NameExpr n, Void arg) {
                    String name = n.getNameAsString();
                    if (!declaredInLoop.contains(name) && !usedVars.contains(name))
                        usedVars.add(name);
                }

                @Override
                public void visit(AssignExpr expr, Void arg) {
                    if (expr.getTarget().isNameExpr()) {
                        String name = expr.getTarget().asNameExpr().getNameAsString();
                        if (!declaredInLoop.contains(name) && !modifiedVars.contains(name))
                            modifiedVars.add(name);
                    }
                }

                @Override
                public void visit(UnaryExpr expr, Void arg) {
                    if (/*expr.getOperator().isIncrementOrDecrement() &&*/ expr.getExpression().isNameExpr()) {
                        String name = expr.getExpression().asNameExpr().getNameAsString();
                        if (!declaredInLoop.contains(name) && !modifiedVars.contains(name))
                            modifiedVars.add(name);
                    }
                }
            }, null);
        }

        // Helper 3: Create extracted method
        private MethodDeclaration createMethod(String name, Statement loopStmt,
                                             List<String> params, List<String> returns) {
            MethodDeclaration method = new MethodDeclaration();
            method.setName(name)
                .setPrivate(true)
                .setType(returns.isEmpty() ? StaticJavaParser.parseType("void") : new ArrayType(StaticJavaParser.parseType("Object")));

            // Add parameters
            params.forEach(var ->
                method.addParameter(new Parameter(StaticJavaParser.parseType("int"), var))
            );

            // Build method body
            BlockStmt body = new BlockStmt();
            body.addStatement(loopStmt.clone());

            // Add return statement if needed
            if (!returns.isEmpty()) {
                NodeList<Expression> returnValues = new NodeList<>();
                returns.forEach(var -> returnValues.add(new NameExpr(var)));
                body.addStatement(new ReturnStmt(
                    new ArrayCreationExpr(new ArrayType(StaticJavaParser.parseType("Object")), new NodeList(new ArrayCreationLevel[]{new ArrayCreationLevel()}),
                        new ArrayInitializerExpr(returnValues)
                    )));
            }
            method.setBody(body);
            return method;
        }

        // Helper 4: Create replacement statement
        private Statement createReplacement(String methodName, List<String> params, List<String> returns) {
            // Build method call
            MethodCallExpr call = new MethodCallExpr(null, methodName);
            params.forEach(var -> call.addArgument(new NameExpr(var)));

            if (returns.isEmpty()) {
                return new ExpressionStmt(call);
            } else {
                // Create: Object[] result = methodCall();
                VariableDeclarationExpr resultVar = new VariableDeclarationExpr(
                    new VariableDeclarator(new ArrayType(StaticJavaParser.parseType("Object")), "result", call)
                );

                // Assign back modified variables
                BlockStmt block = new BlockStmt();
                block.addStatement(new ExpressionStmt(resultVar));

                for (int i = 0; i < returns.size(); i++) {
                    String var = returns.get(i);
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
            return stmt.findAncestor(ClassOrInterfaceDeclaration.class);
        }
    }
}