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

package com.fpetrola.z80.instructions.impl;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.CachedTableAluOperation;
import com.fpetrola.z80.registers.flag.AluOperation;

public class BIT extends BitOperation {
  private static final AluOperation bitAluOperation = new CachedTableAluOperation(
    new AluOperation() {
    protected int calculate3Values(int address, int value1, int bit) {
      F = bit & 1;
      bit = bit >>> 1;
      F = (F & FLAG_C) | FLAG_H | (address & (FLAG_3 | FLAG_5));
      if (((value1) & (0x01 << (bit))) == 0) F |= FLAG_P | FLAG_Z;
      if ((bit) == 7 && ((value1) & 0x80) != 0) F |= FLAG_S;
      Q = F;
      return F;
    }
    }
  );

  public Register getMemptr() {
    return memptr;
  }

  private final Register memptr;

  public BIT(OpcodeReference target, int n, Register flag, Register memptr) {
    super(target, n, flag);
    this.memptr = memptr;
  }

  public void execute() {
    int address;
    if (target instanceof MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
      address = ((memoryPlusRegister8BitReference.getTarget().read() + (int) memoryPlusRegister8BitReference.fetchRelative()) & 0xFFFF) >> 8;
    } else if (target instanceof IndirectMemory8BitReference) {
      address = memptr.read() >>> 8;
    } else {
      address = target.read();
    }
    int nAndCarry = (n << 1) | flag.read() & 1;
    bitAluOperation.execute3Values(address, target.read(), nAndCarry, flag);
  }

  public void accept(InstructionVisitor<?> visitor) {
    if (!visitor.visitingBit(this))
      super.accept(visitor);
  }
}
