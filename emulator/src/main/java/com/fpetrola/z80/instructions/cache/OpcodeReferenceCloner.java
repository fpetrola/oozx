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

package com.fpetrola.z80.instructions.cache;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.opcodes.decoder.table.NullOpcodeReference;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

public class OpcodeReferenceCloner implements InstructionVisitor<ImmutableOpcodeReference> {
  private final InstructionFactory instructionFactory;

  public ImmutableOpcodeReference getResult() {
    return result;
  }

  public void setResult(ImmutableOpcodeReference result) {
    this.result = result;
  }

  private ImmutableOpcodeReference result;

  public OpcodeReferenceCloner(InstructionFactory instructionFactory) {
    this.instructionFactory = instructionFactory;
  }

  public void visitConstantOpcodeReference(ConstantOpcodeReference constantOpcodeReference) {
    setResult(constantOpcodeReference);
  }

  public void visitNullOpcodeReference(NullOpcodeReference tNullOpcodeReference) {
    setResult(tNullOpcodeReference);
  }

  @Override
  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    setResult(new IndirectMemory8BitReference(indirectMemory8BitReference.getTarget(), indirectMemory8BitReference.getMemory()));
  }

  @Override
  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    setResult(new IndirectMemory16BitReference(indirectMemory16BitReference.getTarget(), indirectMemory16BitReference.getMemory()));
  }

  @Override
  public boolean visitMemory8BitReference(Memory8BitReference memory8BitReference) {
    setResult(new Memory8BitReference(memory8BitReference.getMemory(), memory8BitReference.getPc(), memory8BitReference.getDelta()));
    return false;
  }

  @Override
  public boolean visitMemory16BitReference(Memory16BitReference memory16BitReference) {
    setResult(new Memory16BitReference(memory16BitReference.getMemory(), memory16BitReference.getPc(), memory16BitReference.getDelta()));
    return false;
  }

  @Override
  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    setResult(new MemoryPlusRegister8BitReference(memoryPlusRegister8BitReference.getTarget(), memoryPlusRegister8BitReference.getMemory(), memoryPlusRegister8BitReference.getPc(), memoryPlusRegister8BitReference.getValueDelta()));
  }

  public boolean visitRegister(Register register) {
    setResult(register);
    return false;
  }
}
