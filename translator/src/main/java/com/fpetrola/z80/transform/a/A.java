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
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ReturnStmt;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class A {

  public static void main(String[] args) throws FileNotFoundException {
    CompilationUnit cu = StaticJavaParser.parse(new File("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/ZxGame1.java"));
    new A().refactorClass(cu);
    System.out.println(cu);

  }
//  public static void setupParser() {
//    StaticJavaParser.getConfiguration()
//        .setSymbolResolver(new JavaSymbolSolver(new ReflectionTypeSolver()));
//  }


  public void refactorClass(CompilationUnit cu) {
    cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
      // Map of field names to their usage data
      Map<String, FieldUsageData> fieldUsageMap = new HashMap<>();

      // Collect all fields in the class
      List<FieldDeclaration> fields = classDecl.getMembers().stream()
          .filter(FieldDeclaration.class::isInstance)
          .map(FieldDeclaration.class::cast)
          .collect(Collectors.toList());

      // Analyze each method
      classDecl.getMethods().forEach(method -> {
        Map<String, FieldUsageData> methodFieldUsage = new HashMap<>();

        method.findAll(AssignExpr.class).forEach(assignExpr -> {
          // Detect field writes (e.g., this.x = 5)
          if (assignExpr.getTarget().isFieldAccessExpr()) {
            String fieldName = assignExpr.getTarget().asFieldAccessExpr().getNameAsString();
            methodFieldUsage.computeIfAbsent(fieldName, k -> new FieldUsageData())
                .writeUsages.add(assignExpr);
          }
        });

        method.findAll(FieldAccessExpr.class).forEach(fieldAccess -> {
          // Detect field reads (e.g., this.x)
          String fieldName = fieldAccess.getNameAsString();
          FieldUsageData data = methodFieldUsage.computeIfAbsent(fieldName, k -> new FieldUsageData());
          data.readUsages.add(fieldAccess);
          if (!data.writeUsages.isEmpty()) {
            data.isReadBeforeWrite = true; // Simplified check
          }
        });

        // Update global field usage map
        methodFieldUsage.forEach((fieldName, data) -> {
          fieldUsageMap.merge(fieldName, data, (oldData, newData) -> {
            oldData.readUsages.addAll(newData.readUsages);
            oldData.writeUsages.addAll(newData.writeUsages);
            return oldData;
          });
        });
      });

      // Now refactor each method based on field usage
      classDecl.getMethods().forEach(method -> refactorMethod(method, fieldUsageMap));
    });
  }

  private void refactorMethod(
      MethodDeclaration method,
      Map<String, FieldUsageData> fieldUsageMap
  ) {
    List<Parameter> newParams = new ArrayList<>();
    List<VariableDeclarator> newLocals = new ArrayList<>();
    List<Expression> modifiedVarsToReturn = new ArrayList<>();

    fieldUsageMap.forEach((fieldName, usageData) -> {
      // Case 1: Field is read before being written -> Convert to parameter
      if (usageData.isReadBeforeWrite) {
        newParams.add(new Parameter()
            .setType(usageData.fieldType) // Extract actual type from AST
            .setName(fieldName));
      }

      // Case 2: Field is written before being read -> Convert to local variable
      else {
        newLocals.add(new VariableDeclarator()
            .setType(int.class)
            .setName(fieldName)
            .setInitializer("0")); // Extract initializer
      }

      // Track modified fields to return them
      if (usageData.isModified) {
        modifiedVarsToReturn.add(new NameExpr(fieldName));
      }
    });

    // Update method signature with new parameters
    method.setParameters(new NodeList<>(newParams));

    // Add local variables at method start
    if (!newLocals.isEmpty()) {
      VariableDeclarationExpr localVars = new VariableDeclarationExpr()
          .setVariables(NodeList.nodeList(newLocals));
      method.getBody().ifPresent(body -> body.addStatement(0, localVars));
    }

    // Update return type to return modified variables (e.g., as Object[])
    if (!modifiedVarsToReturn.isEmpty()) {
      method.setType("Object[]");
      ReturnStmt returnStmt = new ReturnStmt(new ArrayInitializerExpr()
          .setValues(NodeList.nodeList(modifiedVarsToReturn)));
      method.getBody().ifPresent(body -> body.addStatement(returnStmt));
    }
  }
}