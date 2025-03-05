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
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class IfElseMethodExtractor {

    public static void main(String[] args) throws FileNotFoundException {

        String code = "public class Test {\n" +
                      "    public void method() {\n" +
                      "        if (System.currentTimeMillis()== 1) {\n" +
                      "            System.out.println(\"Hello\");\n" +
                      "        } else {\n" +
                      "            System.out.println(\"World\");\n" +
                      "        }\n" +
                      "    }\n" +
                      "}\n";
        
        CompilationUnit cu = StaticJavaParser.parse(new File("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/ZxGame1.java"));
        cu.accept(new IfElseMethodExtractorVisitor(), null);
        System.out.println(cu);
    }

    private static class IfElseMethodExtractorVisitor extends ModifierVisitor<Void> {
        private int methodCounter = 0;

        @Override
        public Visitable visit(IfStmt ifStmt, Void arg) {
            super.visit(ifStmt, arg);
            processBlock(ifStmt.getThenStmt(), stmt -> ifStmt.setThenStmt(stmt), "Then");
            ifStmt.getElseStmt().ifPresent(elseStmt ->
                processBlock(elseStmt, stmt -> ifStmt.setElseStmt(stmt), "Else")
            );
            return ifStmt;
        }

        private void processBlock(Statement block, java.util.function.Consumer<Statement> replacer, String suffix) {
            List<Statement> statements = extractStatements(block);
            if (statements.isEmpty()) {
                return;
            }

            String methodName = "extracted" + suffix + methodCounter++;
            MethodDeclaration method = createExtractedMethod(methodName, statements);

            Optional<ClassOrInterfaceDeclaration> parentClass = findParentClass(block);
            parentClass.ifPresent(classDecl -> classDecl.addMember(method));

            replacer.accept(new ExpressionStmt(new MethodCallExpr(methodName)));
        }

        private List<Statement> extractStatements(Statement stmt) {
            List<Statement> statements = new ArrayList<>();
            if (stmt instanceof BlockStmt) {
                statements.addAll(((BlockStmt) stmt).getStatements());
            } else {
                statements.add(stmt);
            }
            return statements;
        }

        private MethodDeclaration createExtractedMethod(String methodName, List<Statement> body) {
            MethodDeclaration method = new MethodDeclaration();
            method.setName(methodName);
            method.setType(StaticJavaParser.parseType("void"));
            BlockStmt methodBody = new BlockStmt();
            methodBody.setStatements(new NodeList<>(body));
            method.setBody(methodBody);
            method.setPrivate(true); // Assuming extracted methods should be private
            return method;
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