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
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Cp<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final TableAluOperation cpTableAluOperation = new TableAluOperation() {
    public int execute(int A, int value, int carry) {
      int cptemp = A - value;
      int lookup = ((A & 0x88) >> 3) |
          ((value & 0x88) >> 2) |
          ((cptemp & 0x88) >> 1);
      F = ((cptemp & 0x100) != 0 ? FLAG_C : (cptemp != 0 ? 0 : FLAG_Z)) | FLAG_N |
          halfCarrySubTable(lookup & 0x07) |
          overflowSubTable(lookup >> 4) |
          (value & (FLAG_3 | FLAG_5)) |
          (cptemp & FLAG_S);
      Q = F;
      return A;
    }
  };

  public Cp(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, v, reg_A) -> cpTableAluOperation.executeWithoutCarry(v, reg_A, tFlagRegister));
  }

  public int execute() {
    final T value1 = target.read();
    final T value2 = source.read();
    binaryAluOperation.execute(flag, value2, value1);
    return cyclesCost;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingCp(this);
  }
}
