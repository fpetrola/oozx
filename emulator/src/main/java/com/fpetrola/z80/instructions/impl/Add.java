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

public class Add<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  public static final TableAluOperation adc8TableAluOperation = new TableAluOperation() {
    public int execute(int a, int value, int carry) {
      data = carry;
      int reg_A = a;
      int local_reg_A = reg_A;
      setHalfCarryFlagAdd(local_reg_A, value, carry);
      setOverflowFlagAdd(local_reg_A, value, carry);
      local_reg_A = local_reg_A + value + carry;
      setS((local_reg_A & 0x0080) != 0);
      setC((local_reg_A & 0xff00) != 0);
      local_reg_A = local_reg_A & 0x00ff;
      setZ(local_reg_A == 0);
      resetN();
      reg_A = local_reg_A;
      setUnusedFlags(reg_A);
      return reg_A;
    }
  };

  public Add(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, (tFlagRegister, value, regA) -> adc8TableAluOperation.executeWithoutCarry(value, regA, tFlagRegister));
  }

  @Override
  public int execute() {
    final T value1 = source.read();
    final T value2 = target.read();
    target.write(binaryAluOperation.execute(flag, value1, value2));
    return cyclesCost;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingAdd(this);
  }
}
