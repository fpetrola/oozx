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

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.*;

public class FieldRefactoringVisitor extends ModifierVisitor<Void> {

    private final Map<String, Boolean> fieldReadBeforeWrite;
    private final Map<String, Boolean> fieldWriteBeforeRead;
    private final Set<String> modifiedFields;

    public FieldRefactoringVisitor(
        Map<String, Boolean> fieldReadBeforeWrite,
        Map<String, Boolean> fieldWriteBeforeRead,
        Set<String> modifiedFields
    ) {
        this.fieldReadBeforeWrite = fieldReadBeforeWrite;
        this.fieldWriteBeforeRead = fieldWriteBeforeRead;
        this.modifiedFields = modifiedFields;
    }

    @Override
    public Visitable visit(MethodDeclaration method, Void arg) {
        // Step 1: Replace read-before-write fields with parameters
        List<Parameter> newParams = new ArrayList<>();
        fieldReadBeforeWrite.forEach((fieldName, isRead) -> {
            if (isRead) {
                newParams.add(new Parameter()
                    .setType("int") // Extract actual type from class fields
                    .setName(fieldName));
            }
        });

        // Step 2: Replace write-before-read fields with locals
        List<VariableDeclarator> newLocals = new ArrayList<>();
        fieldWriteBeforeRead.forEach((fieldName, isWritten) -> {
            if (isWritten) {
                newLocals.add(new VariableDeclarator()
                    .setType("int")
                    .setName(fieldName)
                    .setInitializer("0")); // Extract initializer from class fields
            }
        });

        // Update method signature and body
        method.setParameters(NodeList.nodeList(newParams));
        if (!newLocals.isEmpty()) {
            VariableDeclarationExpr locals = new VariableDeclarationExpr(
                NodeList.nodeList(newLocals)
            );
            method.getBody().ifPresent(b -> b.addStatement(0, locals));
        }

        // Step 3: Return modified locals
        if (!modifiedFields.isEmpty()) {
            ArrayInitializerExpr returnValues = new ArrayInitializerExpr();
            modifiedFields.forEach(fieldName -> returnValues.getValues().add(new NameExpr(fieldName)));
            method.setType("Object[]");
            method.getBody().ifPresent(b -> b.addStatement(new ReturnStmt(returnValues)));
        }

        return super.visit(method, arg);
    }
}