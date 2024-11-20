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

import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class BIT<T extends WordNumber> extends BitOperation<T> {
  public Register<T> getMemptr() {
    return memptr;
  }

  private final Register<T> memptr;

  public BIT(OpcodeReference<T> target, int n, Register<T> flag, Register<T> memptr) {
    super(target, n, flag);
    this.memptr = memptr;
  }

  public int execute() {
    new BitAluOperation<T>().execute(WordNumber.createValue(n), target, flag, memptr);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingBit(this))
      super.accept(visitor);
  }

  private static class BitAluOperation<T extends WordNumber> extends AluOperation {
    public void execute(T bit_, OpcodeReference<T> target, Register<T> flag, Register<T> memptr) {
      final int value = target.read().intValue();
      int bit = bit_.intValue();

      F = flag.read().intValue();
      int f = F;
      int s = F & SIGN_MASK;
      f = (f & CARRY_MASK) | HALFCARRY_MASK | (value & (FLAG_3 | FLAG_5));
      int mask = 1 << bit;
      boolean zeroFlag = (mask & value) == 0;

      if ((value & mask) == 0) {
        f |= PARITY_MASK | ZERO_MASK;
      }
      if (mask == SIGN_MASK && !zeroFlag) {
        f |= SIGN_MASK;
      }
      F = f;

      int address =value;
      if (target instanceof MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
        address = memoryPlusRegister8BitReference.getTarget().read().plus(memoryPlusRegister8BitReference.fetchRelative()).intValue() >> 8;
      } else if (target instanceof IndirectMemory8BitReference<T>) {
        address = memptr.read().intValue() >>> 8;
      }

      setFlags53From(address);
      flag.write(WordNumber.createValue(F));
    }

  }
}
