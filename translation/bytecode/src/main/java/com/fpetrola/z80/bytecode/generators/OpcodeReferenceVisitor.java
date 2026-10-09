/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

public class OpcodeReferenceVisitor implements InstructionVisitor<Object> {
  private Object result;
  private final boolean isTarget;
  private final RoutineBytecodeGenerator routineByteCodeGenerator;

  public OpcodeReferenceVisitor(boolean isTarget, RoutineBytecodeGenerator routineByteCodeGenerator) {
    this.isTarget = isTarget;
    this.routineByteCodeGenerator = routineByteCodeGenerator;
  }

  @Override
  public void visitOpcodeReference(OpcodeReference opcodeReference) {
    result = ((Integer) opcodeReference.read());
  }

  public Object getResult() {
    return result;
  }

  public boolean visitRegister(Register register) {
    result = routineByteCodeGenerator.getExistingVariable(register);
//    System.out.println("Cannot virtualize: " + register.getName());
    return true;
  }

  public void visitConstantOpcodeReference(ConstantOpcodeReference constantOpcodeReference) {
    result = constantOpcodeReference.read();
  }

  public void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference memoryAccessOpcodeReference) {
    int o = memoryAccessOpcodeReference.getC().read();
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> o, "");
    else result = getFromMemory(o);
  }

  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    Register target = (Register) memoryPlusRegister8BitReference.getTarget();
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(isTarget, routineByteCodeGenerator);
    target.accept(opcodeReferenceVisitor);

    Variable variable = (Variable) opcodeReferenceVisitor.getResult();

    byte value = memoryPlusRegister8BitReference.fetchRelative();
    Variable variablePlusDelta = variable.add(value);
    if (isTarget)
      result = new WriteArrayVariable(routineByteCodeGenerator, () -> variablePlusDelta, "");
    else {
      result = getFromMemory(variablePlusDelta);
    }
  }


  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    Object variable = addressOf(indirectMemory8BitReference.getTarget());
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> variable, "");
    else {
      result = getFromMemory(variable);
    }
  }

  private Object addressOf(ImmutableOpcodeReference target) {
    if (target instanceof Memory16BitReference nn) {
      Integer operand = modifiedOperand(nn.getDelta(), 2);
      return operand == null ? nn.read() : getFromMemory16(operand);
    }
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
    target.accept(opcodeReferenceVisitor);
    return opcodeReferenceVisitor.getResult();
  }

  private Variable getFromMemory(Object variable) {
    return routineByteCodeGenerator.getVariableFromMemory(variable, "");
  }


  @Override
  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    Object variable = addressOf(indirectMemory16BitReference.getTarget());
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> variable, "16");
    else {
      result = getFromMemory16(variable);
    }
  }

  private Variable getFromMemory16(Object variable) {
    return routineByteCodeGenerator.getVariableFromMemory(variable, "16");
  }

  public boolean visitMemory8BitReference(Memory8BitReference operand) {
    return readsModifiedOperand(operand.getDelta(), 1, this::getFromMemory);
  }

  public boolean visitMemory16BitReference(Memory16BitReference operand) {
    return readsModifiedOperand(operand.getDelta(), 2, this::getFromMemory16);
  }

  private Integer modifiedOperand(int delta, int length) {
    int pc = routineByteCodeGenerator.context.pc.read(), address = routineByteCodeGenerator.context.routineManager.originalAddress(pc) + delta & 0xffff;
    java.util.Set<?> modified = routineByteCodeGenerator.context.symbolicExecutionAdapter.getMutantAddress();
    return java.util.stream.IntStream.of(address, pc + delta & 0xffff).anyMatch(at -> modified.contains(at) || length == 2 && modified.contains(at + 1 & 0xffff)) ? address : null;
  }

  private boolean readsModifiedOperand(int delta, int length, java.util.function.Function<Object, Object> reader) {
    Integer address = modifiedOperand(delta, length);
    if (address == null)
      return false;
    result = reader.apply(address);
    return true;
  }

  @Override
  public void visitImmutableOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {
    result = immutableOpcodeReference.read();
  }

  public  Variable process(Register register) {
    register.accept(this);
    return (Variable) getResult();
  }
}
