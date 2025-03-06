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

package com.fpetrola.z80.transform.b;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ExecutionOrderVisitor extends VoidVisitorAdapter<Void> {

  public final FieldsInfo fieldsInfo = new FieldsInfo();

  @Override
  public void visit(MethodDeclaration method, Void arg) {
    // Reset state for each method
    fieldsInfo.fieldReadBeforeWrite.clear();
    fieldsInfo.fieldWriteBeforeRead.clear();
    fieldsInfo.modifiedFields.clear();

    // Visit statements in execution order
    method.getBody().ifPresent(body -> body.getStatements().forEach(stmt -> stmt.accept(this, arg)));

    // Now you can use fieldReadBeforeWrite, fieldWriteBeforeRead, and modifiedFields
    // to refactor the method (e.g., convert fields to parameters/locals)
  }

  @Override
  public void visit(FieldAccessExpr expr, Void arg) {
    String fieldName = expr.getNameAsString();
    if (!fieldsInfo.fieldWriteBeforeRead.containsKey(fieldName)) {
      fieldsInfo.fieldReadBeforeWrite.put(fieldName, true);
    }
    super.visit(expr, arg);
  }

  @Override
  public void visit(AssignExpr expr, Void arg) {
    if (expr.getTarget().isFieldAccessExpr()) {
      String fieldName = expr.getTarget().asFieldAccessExpr().getNameAsString();
      fieldsInfo.fieldWriteBeforeRead.put(fieldName, true);
      fieldsInfo.modifiedFields.add(fieldName);
    } else
      super.visit(expr, arg);
  }
}