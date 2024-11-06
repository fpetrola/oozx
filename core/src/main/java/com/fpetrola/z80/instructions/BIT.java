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

package com.fpetrola.z80.instructions;

import com.fpetrola.z80.instructions.base.BitOperation;
import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.AluOperation;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class BIT<T extends WordNumber> extends BitOperation<T> {
  public static final AluOperation testBitTableAluOperation = new AluOperation() {
    public int execute(int bit, int value, int carry) {
      resetS();

      switch (bit) {
        case 0: {
          value = value & setBit0;
          break;
        }
        case 1: {
          value = value & setBit1;
          break;
        }
        case 2: {
          value = value & setBit2;
          break;
        }
        case 3: {
          value = value & setBit3;
          break;
        }
        case 4: {
          value = value & setBit4;
          break;
        }
        case 5: {
          value = value & setBit5;
          break;
        }
        case 6: {
          value = value & setBit6;
          break;
        }
        case 7: {
          value = value & setBit7;
          setS(value != 0);
          break;
        }
      }
      setZ(0 == value);
      setPV(0 == value);
      resetN();
      setH();

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
