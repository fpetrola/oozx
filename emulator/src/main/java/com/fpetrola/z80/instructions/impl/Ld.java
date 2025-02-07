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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class Ld<T extends WordNumber> extends TargetSourceInstruction<T, ImmutableOpcodeReference<T>> {
  public Ld(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag) {
    super(target, source, flag);
  }

  public int execute() {
    T value = source.read();
    T aLU8Assign = value;
    if (source instanceof IndirectMemory8BitReference<T> indirectMemory8BitReference) {
      T read = indirectMemory8BitReference.getTarget().read();
      aLU8Assign = aLU8Assign.processOrigin(read);
    }

    if (target instanceof IndirectMemory8BitReference<T> indirectMemory8BitReference) {
      T read = indirectMemory8BitReference.getTarget().read();
      aLU8Assign = aLU8Assign.processOrigin(read);
    }
    target.write(aLU8Assign);

    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingLd(this);
  }
}
