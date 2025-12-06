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
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Cp extends ParameterizedBinaryAluInstruction {
  public static final TableAluOperation cpTableAluOperation = new TableAluOperation() {
    public int calculate2Values1Boolean(int value1, int value2, int carry) {
      int cptemp = value1 - value2;
      int lookup = ((value1 & 0x88) >> 3) |
                   ((value2 & 0x88) >> 2) |
                   ((cptemp & 0x88) >> 1);
      F = ((cptemp & 0x100) != 0 ? FLAG_C : (cptemp != 0 ? 0 : FLAG_Z)) | FLAG_N |
          halfCarrySubTable(lookup & 0x07) |
          overflowSubTable(lookup >> 4) |
          (value2 & (FLAG_3 | FLAG_5)) |
          (cptemp & FLAG_S);
      Q = F;
      return value1;
    }
  };

  public Cp(OpcodeReference target, ImmutableOpcodeReference source, Register flag) {
    super(target, source, flag, cpTableAluOperation);
  }

  protected int doExecute(int sourceValue, int targetValue) {
    return super.doExecute(targetValue, sourceValue);
  }

  protected void assignTarget(int execute) {
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingCp(this);
  }
}
