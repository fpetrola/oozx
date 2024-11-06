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

import com.fpetrola.z80.instructions.base.InstructionVisitor;
import com.fpetrola.z80.instructions.base.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class Neg<T extends WordNumber> extends ParameterizedUnaryAluInstruction<T> {
  public static final TableAluOperation negTableAluOperation = new TableAluOperation() {
    public int execute(int a, int carry) {
      data = 0;
      int reg_A = a;
      setHalfCarryFlagSub(0, reg_A, 0);
      setOverflowFlagSub(0, reg_A, 0);
      reg_A = 0 - reg_A;
      if ((reg_A & 0xFF00) != 0)
        setC();
      else
        resetC();
      setN();
      reg_A = reg_A & 0x00FF;
      if (reg_A == 0)
        setZ();
      else
        resetZ();
      if ((reg_A & 0x0080) != 0)
        setS();
      else
        resetS();
      setUnusedFlags(reg_A);
      return reg_A;
    }
  };

  public Neg(OpcodeReference target, Register<T> flag) {
    super(target, flag, (tFlagRegister, reg_A) -> negTableAluOperation.executeWithCarry(reg_A, tFlagRegister));
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingNeg(this);
  }
}
