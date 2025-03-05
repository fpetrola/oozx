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

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class CallSiteUpdaterVisitor extends ModifierVisitor<Void> {

    @Override
    public Visitable visit(MethodCallExpr call, Void arg) {
        // Check if the method is one we refactored
        if (call.getNameAsString().equals("refactoredMethod")) {
            // Add parameters for read-before-write fields
            call.getArguments().add(new NameExpr("fieldX"));

            // Capture returned values and assign back to fields
            EnclosedExpr assignment = new EnclosedExpr(call);
            ExpressionStmt assignStmt = new ExpressionStmt(
                new AssignExpr(
                    new FieldAccessExpr(new ThisExpr(), "fieldX"),
                    new ArrayAccessExpr(assignment, new IntegerLiteralExpr(0)),
                    AssignExpr.Operator.ASSIGN
                )
            );
            return assignStmt;
        }
        return super.visit(call, arg);
    }
}