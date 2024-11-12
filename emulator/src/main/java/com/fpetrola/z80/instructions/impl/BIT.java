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
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class BIT<T extends WordNumber> extends BitOperation<T> {
  public static final AluOperation testBitTableAluOperation = new AluOperation() {
    @Override
    public <T extends WordNumber> T executeWithCarry2(T value_, T bit_, int carry, Register<T> flag) {
      data = flag.read().intValue();
      int f = data;
      int value = value_.intValue();

      int s = data & SIGN_MASK;
      f = (f & CARRY_MASK) | HALFCARRY_MASK | (value & (FLAG_3 | FLAG_5));
      int bit = bit_.intValue();
      int mask = 1 << bit;
      boolean zeroFlag = (mask & value) == 0;

      if ((value & mask) == 0) {
        f |= PARITY_MASK | ZERO_MASK;
      }
      if (mask == SIGN_MASK && !zeroFlag) {
        f |= SIGN_MASK;
      }
      data = f;
      SetFlags53From(carry);
      return WordNumber.createValue(data);
    }

  };

  public BIT(OpcodeReference target, int n, Register<T> flag) {
    super(target, n, flag);
  }

  public int execute() {
    final T value = target.read();
    int address = value.intValue();
    if (target instanceof MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
      address = memoryPlusRegister8BitReference.getTarget().read().plus(memoryPlusRegister8BitReference.fetchRelative()).intValue() >> 8;
    }
    flag.write(testBitTableAluOperation.executeWithCarry2(value, WordNumber.createValue(n), address, flag));
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingBit(this))
      super.accept(visitor);
  }
}
