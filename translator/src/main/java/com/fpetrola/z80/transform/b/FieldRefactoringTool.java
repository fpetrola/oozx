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

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;

public class FieldRefactoringTool {
  public static void main(String[] args) throws FileNotFoundException {
    CompilationUnit cu = StaticJavaParser.parse(new File("/home/fernando/detodo/desarrollo/m/zx/my-zx/oozx/translator/src/main/java/com/fpetrola/z80/bytecode/tests/ZxGame1.java"));
    CompilationUnit refactor = new FieldRefactoringTool().refactor(cu);
    System.out.println(refactor);
  }


  public CompilationUnit refactor(CompilationUnit cu) {
    // Step 1: Analyze field usage in execution order
    ExecutionOrderVisitor executionOrderVisitor = new ExecutionOrderVisitor();
    cu.accept(executionOrderVisitor, null);

    // Step 2: Refactor methods
    FieldRefactoringVisitor refactoringVisitor = new FieldRefactoringVisitor(
        executionOrderVisitor.fieldReadBeforeWrite,
        executionOrderVisitor.fieldWriteBeforeRead,
        executionOrderVisitor.modifiedFields
    );
    cu.accept(refactoringVisitor, null);

    // Step 3: Update call sites
    CallSiteUpdaterVisitor callSiteUpdater = new CallSiteUpdaterVisitor();
    cu.accept(callSiteUpdater, null);

    return cu;
  }
}