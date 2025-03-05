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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;

import java.util.List;

public class MethodExtractor {

    public static void main(String[] args) {
        String code = "public class MyClass {\n" +
                      "    public void myMethod() {\n" +
                      "        int a = 5;\n" +
                      "        int b = 10;\n" +
                      "        int sum = a + b;\n" +
                      "        System.out.println(sum);\n" +
                      "    }\n" +
                      "}";

        // Parse the code
        ParseResult<CompilationUnit> cu = new JavaParser().parse(code);

        // Find the method and its body
        CompilationUnit compilationUnit = cu.getResult().get();
        MethodDeclaration method = compilationUnit.findAll(MethodDeclaration.class).get(0);
        BlockStmt block = method.getBody().orElseThrow(() -> new RuntimeException("Method body not found"));

        // Extract the last two statements into a new method
        List<Statement> statements = block.getStatements();
        BlockStmt extractedBlock = new BlockStmt();
        extractedBlock.getStatements().addAll(statements.subList(1, statements.size())); // Extract statements

        // Create a new method
        MethodDeclaration newMethod = new MethodDeclaration();
        newMethod.setName("extractedMethod");
        newMethod.setType("void");
        newMethod.setBody(extractedBlock);

        // Add the new method to the class
        compilationUnit.getClassByName("MyClass").get().addMember(newMethod);

        // Replace the extracted block with a method call
        block.getStatements().removeAll(extractedBlock.getStatements());
        block.addStatement("extractedMethod();");

        // Print the modified code
        PrettyPrinter printer = new PrettyPrinter(new PrettyPrinterConfiguration());
        System.out.println(printer.print(compilationUnit));
    }
}