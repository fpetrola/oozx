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

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.LoopNestTree;

import java.util.*;

public class LoopExtractor extends BodyTransformer {

    public static void main(String[] args) {
        PackManager.v().getPack("jtp").add(new Transform("jtp.loopExtractor", new LoopExtractor()));
        soot.Main.main(args);
    }
    @Override
    protected void internalTransform(Body body, String phaseName, Map<String, String> options) {
        // 1. Detect loops in the method body
        BriefUnitGraph units = new BriefUnitGraph(body);
        LoopNestTree loopTree = new LoopNestTree(body);
        for (Loop loop : loopTree) {
            // 2. For each loop, extract logic
            extractLoop(body, loop);
        }
    }

    private void extractLoop(Body originalBody, Loop loop) {
        // 3. Collect variables used in the loop
        Set<Local> usedLocals = collectUsedLocals(loop);

        // 4. Create new method for extracted loop
        SootClass declaringClass = originalBody.getMethod().getDeclaringClass();
        SootMethod extractedMethod = createExtractedMethod(declaringClass, loop, usedLocals);

        // 5. Replace loop with method call and control flow handling
        replaceLoopWithCall(originalBody, loop, extractedMethod, usedLocals);
    }

    private Set<Local> collectUsedLocals(Loop loop) {
        Set<Local> locals = new HashSet<>();
        for (Unit unit : loop.getLoopStatements()) {
            for (ValueBox box : unit.getUseBoxes()) {
                Value val = box.getValue();
                if (val instanceof Local) locals.add((Local) val);
            }
        }
        return locals;
    }

    private SootMethod createExtractedMethod(SootClass clazz, Loop loop, Set<Local> params) {
        // Define method signature
        List<Type> paramTypes = new ArrayList<>();
        for (Local local : params) paramTypes.add(local.getType());

        SootMethod method = new SootMethod(
            "extractedLoop",
            paramTypes,
            Scene.v().getType("java.lang.Object[]"), // Return control flow + variables
            Modifier.PRIVATE | Modifier.STATIC
        );
        clazz.addMethod(method);

        // Build method body (Jimple)
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);

        // Copy loop units into new method
        for (Unit unit : loop.getLoopStatements()) {
            body.getUnits().add((Unit) unit.clone());
        }

        // Add return statement with control flow flags
        Local controlFlow = Jimple.v().newLocal("controlFlow", Scene.v().getType("java.lang.Object[]"));
        body.getUnits().add(
            Jimple.v().newReturnStmt(controlFlow)
        );

        return method;
    }

    private void replaceLoopWithCall(Body originalBody, Loop loop, SootMethod extractedMethod, Set<Local> params) {
        // Remove original loop units
        originalBody.getUnits().removeAll(loop.getLoopStatements());

        // Create method call
        List<Value> args = new ArrayList<>();
        for (Local local : params) args.add(local);

        InvokeExpr invoke = Jimple.v().newStaticInvokeExpr(
            extractedMethod.makeRef(),
            args
        );

        // Assign result to a local variable
        Local result = Jimple.v().newLocal("loopResult", Scene.v().getType("java.lang.Object[]"));
        originalBody.getUnits().add(Jimple.v().newAssignStmt(result, invoke));

        // Handle control flow (break/continue)
        handleControlFlowSignals(originalBody, result);
    }

    private void handleControlFlowSignals(Body body, Local result) {
        // Example: Check if the result indicates a 'break'
        Value breakFlag = Jimple.v().newStaticFieldRef(
            Scene.v().getField("<ControlFlow: boolean BREAK>").makeRef()
        );
        Stmt breakCheck = Jimple.v().newIfStmt(
            Jimple.v().newEqExpr(
                Jimple.v().newArrayRef(result, IntConstant.v(0)),
                breakFlag
            ),
            body.getUnits().getFirst() // Target for break
        );
        body.getUnits().add(breakCheck);
    }
}