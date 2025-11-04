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

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class ParameterizedUnaryAluInstruction extends DefaultTargetFlagInstruction {
  public ParameterizedUnaryAluInstruction(OpcodeReference target, Register flag, TableAluOperation tableAluOperation) {
    super(target, flag);
    this.unaryAluOperation = getTUnaryAluOperation(tableAluOperation);
    this.flag = flag;
  }

  public interface UnaryAluOperation {
    int execute(int value);
  }

  protected UnaryAluOperation unaryAluOperation;

  public ParameterizedUnaryAluInstruction(OpcodeReference target, Register flag, UnaryAluOperation unaryAluOperation) {
    super(target, flag);
    this.unaryAluOperation = unaryAluOperation;
    this.flag = flag;
  }

  public int execute() {
    final int value2 = target.read();
    int execute = doExecute(value2);
    target.write(execute);
    return cyclesCost;
  }

  protected int doExecute(int value2) {
    return unaryAluOperation.execute(value2);
  }

  public UnaryAluOperation getTUnaryAluOperation(TableAluOperation rrTableAluOperation1) {
    return (a) -> {
      int regA = ((Integer) a);
      int flagValue = ((Integer) flag.read());
      int[] ints = rrTableAluOperation1.executeWithCarry2(regA, flagValue);
      flag.write(ints[1]);
      return ints[0];
    };
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingParameterizedUnaryAluInstruction(this))
      super.accept(visitor);
  }
}
