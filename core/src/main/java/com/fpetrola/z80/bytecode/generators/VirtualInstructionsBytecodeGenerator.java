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

package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.bytecode.generators.helpers.PendingFlagUpdate;
import com.fpetrola.z80.bytecode.generators.helpers.SmartComposed16BitRegisterVariable;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.MethodMaker;
import org.cojen.maker.Variable;

public class VirtualInstructionsBytecodeGenerator<T extends WordNumber> extends InstructionsBytecodeGenerator<T> {
  public VirtualInstructionsBytecodeGenerator(MethodMaker methodMaker, int label, RoutineBytecodeGenerator routineByteCodeGenerator, int address, PendingFlagUpdate previousPendingFlag) {
    super(methodMaker, label, routineByteCodeGenerator, address, previousPendingFlag);
  }

  protected void invokeExAF(Register<?> target, Variable variable) {
    if (variable instanceof SmartComposed16BitRegisterVariable existingVariable) {
      existingVariable.setRegister(target);
      Variable invoke = methodMaker.invoke("exAF", RoutineBytecodeGenerator.getRealVariable(existingVariable));
      existingVariable.set(invoke);
    }
  }

  @Override
  protected void invokeRotationInstruction(String name, Variable t) {
    invokeRlc(t, t.get(), name);
  }

  @Override
  protected void invokeLdir(String methodName) {
    Variable invoke = methodMaker.invoke(methodName, routineByteCodeGenerator.getExistingVariable("HL"), routineByteCodeGenerator.getExistingVariable("DE"), routineByteCodeGenerator.getExistingVariable("BC"));
    routineByteCodeGenerator.getExistingVariable("HL").set(invoke.aget(0));
    routineByteCodeGenerator.getExistingVariable("DE").set(invoke.aget(1));
    routineByteCodeGenerator.getExistingVariable("BC").set(invoke.aget(2));
  }

  @Override
  protected void invokeCpir(String methodName) {
    Variable invoke = methodMaker.invoke(methodName, routineByteCodeGenerator.getExistingVariable("HL"), routineByteCodeGenerator.getExistingVariable("BC"), routineByteCodeGenerator.getExistingVariable("A"));
    routineByteCodeGenerator.getExistingVariable("HL").set(invoke.aget(0));
    routineByteCodeGenerator.getExistingVariable("BC").set(invoke.aget(1));
  }
}
