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

package com.fpetrola.z80.bytecode.generators.helpers;

import com.fpetrola.z80.bytecode.generators.OpcodeReferenceVisitor;
import com.fpetrola.z80.bytecode.generators.RoutineBytecodeGenerator;
import com.fpetrola.z80.instructions.types.FlagInstruction;
import com.fpetrola.z80.transformations.Virtual8BitsRegister;
import com.fpetrola.z80.transformations.VirtualAssignmentInstruction;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.Variable;

import java.util.List;
import java.util.function.Supplier;

public class PendingFlagUpdate {
  public final Supplier<Variable> targetVariableSupplier;
  public final FlagInstruction targetFlagInstruction;
  private final RoutineBytecodeGenerator routineByteCodeGenerator;
  public final int address;
  public Supplier<Object> sourceVariableSupplier;
  public boolean processed;

  public PendingFlagUpdate(Supplier<Variable> targetVariable, FlagInstruction targetFlagInstruction, RoutineBytecodeGenerator routineByteCodeGenerator, int address) {
    this.targetVariableSupplier = targetVariable;
    this.targetFlagInstruction = targetFlagInstruction;
    this.routineByteCodeGenerator = routineByteCodeGenerator;
    this.address = address;
  }

  public PendingFlagUpdate(Supplier<Variable> targetVariable, FlagInstruction targetFlagInstruction, RoutineBytecodeGenerator routineByteCodeGenerator, int address, Supplier<Object> sourceVariable) {
    this(targetVariable, targetFlagInstruction, routineByteCodeGenerator, address);
    this.sourceVariableSupplier = sourceVariable;
  }

  public void update(boolean force) {
    VirtualRegister virtualRegister = (VirtualRegister) targetFlagInstruction.getFlag();
    List<VirtualRegister<?>> dependants = virtualRegister.getDependants();
    if (force || dependants.stream().anyMatch(d -> d instanceof Virtual8BitsRegister<?> virtual8BitsRegister && virtual8BitsRegister.instruction instanceof VirtualAssignmentInstruction<?>)) {
      OpcodeReferenceVisitor variableAdapter = new OpcodeReferenceVisitor(true, routineByteCodeGenerator);
      targetFlagInstruction.getFlag().accept(variableAdapter);
      Object targetVariable = targetVariableSupplier.get();
      if (!(targetVariable instanceof WriteArrayVariable))
        ((Variable) variableAdapter.getResult()).set(RoutineBytecodeGenerator.getRealVariable(targetVariable));
    }
  }
}
