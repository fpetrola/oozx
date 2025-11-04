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
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

import java.util.function.IntSupplier;

public class BIT<T extends WordNumber> extends BitOperation<T> {
  private BitAluOperation<T> tBitAluOperation;
  public Register<T> getMemptr() {
    return memptr;
  }
  private final Register<T> memptr;

  public BIT(OpcodeReference<T> target, int n, Register<T> flag, Register<T> memptr) {
    super(target, n, flag);
    this.memptr = memptr;
    tBitAluOperation = new BitAluOperation<>(target, memptr);
  }

  public int execute() {
    int f = tBitAluOperation.execute2(n, flag.read().value, target.read().value);
    flag.write((T) new WordNumber(f));

    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingBit(this))
      super.accept(visitor);
  }

  private static class BitAluOperation<T extends WordNumber> extends TableAluOperation {
    private IntSupplier addressP;

    public BitAluOperation(OpcodeReference<T> target, Register<T> memptr) {
      addressP = () -> {
        return target.read().value;
      };
      if (target instanceof MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference)
        addressP = () -> {
          WordNumber wordNumber = memoryPlusRegister8BitReference.getTarget().read();
          int i = memoryPlusRegister8BitReference.fetchRelative();
          return ((WordNumber) (WordNumber) new WordNumber((wordNumber.value + i) & 0xFFFF)).value >> 8;
        };
      else if (target instanceof IndirectMemory8BitReference<T>)
        addressP = () -> {
          return memptr.read().value >>> 8;
        };
    }

    public int execute2(int bit, int F, int value1) {
      int address = addressP.getAsInt();
      F = (F & FLAG_C) | FLAG_H | (address & (FLAG_3 | FLAG_5));
      if (((value1) & (0x01 << (bit))) == 0) F |= FLAG_P | FLAG_Z;
      if ((bit) == 7 && ((value1) & 0x80) != 0) F |= FLAG_S;
      Q = F;
      return F;
    }

  }
}
