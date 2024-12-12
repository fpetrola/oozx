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

import com.fpetrola.z80.bytecode.generators.helpers.WriteArrayVariable;
import com.fpetrola.z80.opcodes.references.MemoryAccessOpcodeReference;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.Variable;

public class OpcodeReferenceVisitor<T extends WordNumber> implements InstructionVisitor<T, Object> {
  private Object result;
  private final boolean isTarget;
  private final RoutineBytecodeGenerator routineByteCodeGenerator;

  public OpcodeReferenceVisitor(boolean isTarget, RoutineBytecodeGenerator routineByteCodeGenerator) {
    this.isTarget = isTarget;
    this.routineByteCodeGenerator = routineByteCodeGenerator;
  }

  @Override
  public void visitOpcodeReference(OpcodeReference opcodeReference) {
    result = ((WordNumber) opcodeReference.read()).intValue();
  }

  public Object getResult() {
    return result;
  }

  public boolean visitRegister(Register register) {
    result = routineByteCodeGenerator.getExistingVariable(register);
//    System.out.println("Cannot virtualize: " + register.getName());
    return true;
  }

  public void visitConstantOpcodeReference(ConstantOpcodeReference<T> constantOpcodeReference) {
    result = constantOpcodeReference.read().intValue();
  }

  public void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference<T> memoryAccessOpcodeReference) {
    Variable memoryField = routineByteCodeGenerator.getField("memory");
    int o = memoryAccessOpcodeReference.getC().read().intValue();
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> o, "");
    else result = getFromMemory(o);
  }

  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
    Register target = (Register) memoryPlusRegister8BitReference.getTarget();
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(isTarget, routineByteCodeGenerator);
    target.accept(opcodeReferenceVisitor);

    Variable variable = (Variable) opcodeReferenceVisitor.getResult();

    byte value = memoryPlusRegister8BitReference.fetchRelative();
    Variable variablePlusDelta = value > 0 ? variable.add(value) : variable;
    if (isTarget)
      result = new WriteArrayVariable(routineByteCodeGenerator, () -> variablePlusDelta, "");
    else {
      result = getFromMemory(variablePlusDelta);
    }
  }


  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    Object variable;
    if (indirectMemory8BitReference.getTarget() instanceof Memory16BitReference<?> memory16BitReference) {
      variable = memory16BitReference.read().intValue();
    } else {
      Register target = (Register) indirectMemory8BitReference.getTarget();
      OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
      target.accept(opcodeReferenceVisitor);

      variable = opcodeReferenceVisitor.getResult();
    }
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> variable, "");
    else {
      result = getFromMemory(variable);
    }
  }

  private Variable getFromMemory(Object variable) {
    return routineByteCodeGenerator.getVariableFromMemory(variable, "");
  }


  @Override
  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    Object variable;
    if (indirectMemory16BitReference.target instanceof Memory16BitReference<?> memory16BitReference) {
      variable = memory16BitReference.read().intValue();
    } else {
      Register target = (Register) indirectMemory16BitReference.target;
      OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
      target.accept(opcodeReferenceVisitor);

      variable = opcodeReferenceVisitor.getResult();
    }
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> variable, "16");
    else {
      result = getFromMemory16(variable);
    }
  }

  private Variable getFromMemory16(Object variable) {
    return routineByteCodeGenerator.getVariableFromMemory(variable, "16");
  }

  @Override
  public void visitImmutableOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {
    result = ((T) immutableOpcodeReference.read()).intValue();
  }

  public <T> Variable process(Register<T> register) {
    register.accept(this);
    return (Variable) getResult();
  }
}
