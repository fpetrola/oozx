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
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

public class OpcodeReferenceCloner<T extends WordNumber> implements InstructionVisitor<T, ImmutableOpcodeReference<T>> {
  private final InstructionFactory instructionFactory;

  public ImmutableOpcodeReference<T> getResult() {
    return result;
  }

  public void setResult(ImmutableOpcodeReference<T> result) {
    this.result = result;
  }

  private ImmutableOpcodeReference<T> result;

  public OpcodeReferenceCloner(InstructionFactory instructionFactory) {
    this.instructionFactory = instructionFactory;
  }

  @Override
  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference<T> indirectMemory8BitReference) {
    setResult(new IndirectMemory8BitReference<>(indirectMemory8BitReference.target, indirectMemory8BitReference.getMemory()));
  }

  @Override
  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    setResult(new IndirectMemory16BitReference<>(indirectMemory16BitReference.target, indirectMemory16BitReference.getMemory()));
  }

  @Override
  public boolean visitMemory8BitReference(Memory8BitReference<T> memory8BitReference) {
    T read = memory8BitReference.read();
    setResult(new CachedMemory8BitReference(memory8BitReference.fetchedAddress, memory8BitReference.getMemory(), memory8BitReference.getPc(), memory8BitReference.getDelta()));
    return false;
  }

  @Override
  public boolean visitMemory16BitReference(Memory16BitReference<T> memory16BitReference) {
    T read = memory16BitReference.read();
    setResult(new CachedMemory16BitReference<>(memory16BitReference.fetchedAddress, memory16BitReference.getMemory(), memory16BitReference.getPc(), memory16BitReference.getDelta()));
    return false;
  }

  @Override
  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
    T read = memoryPlusRegister8BitReference.read();
    setResult(new CachedMemoryPlusRegister8BitReference<>(memoryPlusRegister8BitReference.fetchedRelative, memoryPlusRegister8BitReference.getTarget(), memoryPlusRegister8BitReference.getMemory(), memoryPlusRegister8BitReference.getPc(), memoryPlusRegister8BitReference.getValueDelta()));
  }

  public boolean visitRegister(Register register) {
    setResult(register);
    return false;
  }
}
