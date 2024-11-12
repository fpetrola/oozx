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
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;

public class BIT<T extends WordNumber> extends BitOperation<T> {
  public static final AluOperation testBitTableAluOperation = new AluOperation() {
    public int execute(int bit, int value, int carry) {
      int s = data & SIGN_MASK;
      int f = data;
      f = (f & CARRY_MASK) | HALFCARRY_MASK | (value & (FLAG_3 | FLAG_5));
      int mask = 1 << bit;
      boolean zeroFlag = (mask & value) == 0;

      if ((value & mask) == 0) {
        f |= PARITY_MASK | ZERO_MASK;
      }
      if (mask == SIGN_MASK && !zeroFlag) {
        f |= SIGN_MASK;
      }
//      int reg = value;
//      int mask = 1 << bit;
//      boolean zeroFlag = (mask & reg) == 0;
//
//      sz5h3pnFlags = sz53n_addTable[reg] & ~FLAG_SZP_MASK | HALFCARRY_MASK;
//
//      if (zeroFlag) {
//        sz5h3pnFlags |= (PARITY_MASK | ZERO_MASK);
//      }
//
//      if (mask == SIGN_MASK && !zeroFlag) {
//        sz5h3pnFlags |= SIGN_MASK;
//      }
//      flagQ = true;
//
//      data = sz5h3pnFlags | carry;
      data = f;
      return value;
    }
  };

  public BIT(OpcodeReference target, int n, Register<T> flag) {
    super(target, n, flag);
  }

  public int execute() {
    final T value = target.read();
    testBitTableAluOperation.executeWithCarry(value, WordNumber.createValue(n), flag);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingBit(this))
      super.accept(visitor);
  }
}
