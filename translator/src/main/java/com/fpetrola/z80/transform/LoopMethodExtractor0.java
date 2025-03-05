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
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.Optional;

public class LoopMethodExtractor0 {

    public static void main(String[] args) {
        String code = "public class Test {\n" +
                      "    public void method() {\n" +
                      "        int i = 0;\n" +
                      "        while (i < 5) {\n" +
                      "            System.out.println(i);\n" +
                      "            i++;\n" +
                      "        }\n" +
                      "    }\n" +
                      "}\n";
        
        CompilationUnit cu = StaticJavaParser.parse(code);
        cu.accept(new LoopMethodExtractorVisitor(), null);
        System.out.println(cu);
    }

    private static class LoopMethodExtractorVisitor extends ModifierVisitor<Void> {
        private int methodCounter = 0;

        @Override
        public Visitable visit(WhileStmt whileStmt, Void arg) {
            extractLoop(whileStmt);
            super.visit(whileStmt, arg); // Visit children after replacement
          return whileStmt;
        }

        @Override
        public Visitable visit(ForStmt forStmt, Void arg) {
            extractLoop(forStmt);
            super.visit(forStmt, arg);
          return forStmt;
        }

        public Visitable visit(DoStmt doStmt, Void arg) {
            extractLoop(doStmt);
            super.visit(doStmt, arg);
          return doStmt;
        }

        private void extractLoop(Statement loopStmt) {
            // Generate a unique method name
            String methodName = "extractedLoop" + methodCounter++;
            
            // Create the extracted method
            MethodDeclaration method = createExtractedMethod(methodName, loopStmt);
            
            // Add the method to the parent class
            Optional<ClassOrInterfaceDeclaration> parentClass = findParentClass(loopStmt);
            parentClass.ifPresent(c -> c.addMember(method));
            
            // Replace the loop with a method call
            ExpressionStmt methodCall = new ExpressionStmt(new MethodCallExpr(methodName));
            loopStmt.replace(methodCall);
        }

        private MethodDeclaration createExtractedMethod(String methodName, Statement loopStmt) {
            MethodDeclaration method = new MethodDeclaration();
            method.setName(methodName);
            method.setType(StaticJavaParser.parseType("void"));
            
            // Clone the loop to avoid AST corruption
            Statement clonedLoop = loopStmt.clone();
            
            // Build the method body with the cloned loop
            BlockStmt methodBody = new BlockStmt();
            methodBody.addStatement(clonedLoop);
            method.setBody(methodBody);
            
            method.setPrivate(true); // Make the helper method private
            return method;
        }

        private Optional<ClassOrInterfaceDeclaration> findParentClass(Statement stmt) {
            return stmt.findAncestor(ClassOrInterfaceDeclaration.class);
        }
    }
}