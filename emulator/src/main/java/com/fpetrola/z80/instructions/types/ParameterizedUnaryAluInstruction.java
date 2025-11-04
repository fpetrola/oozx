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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class ParameterizedUnaryAluInstruction<T> extends DefaultTargetFlagInstruction<T> {
  public ParameterizedUnaryAluInstruction(OpcodeReference target, Register<T> flag, TableAluOperation tableAluOperation) {
    super(target, flag);
    this.unaryAluOperation = getTUnaryAluOperation(tableAluOperation);
    this.flag = flag;
  }

  public interface UnaryAluOperation<T> {
    T execute(T value);
  }

  protected UnaryAluOperation<T> unaryAluOperation;

  public ParameterizedUnaryAluInstruction(OpcodeReference<T> target, Register<T> flag, UnaryAluOperation<T> unaryAluOperation) {
    super(target, flag);
    this.unaryAluOperation = unaryAluOperation;
    this.flag = flag;
  }

  public int execute() {
    final T value2 = target.read();
    T execute = doExecute(value2);
    target.write(execute);
    return cyclesCost;
  }

  protected T doExecute(T value2) {
    return unaryAluOperation.execute(value2);
  }

  public UnaryAluOperation getTUnaryAluOperation(TableAluOperation rrTableAluOperation1) {
    return (a) -> {
      int regA = ((WordNumber) a).value;
      int flagValue = ((WordNumber) flag.read()).value;
      int[] ints = rrTableAluOperation1.executeWithCarry2(regA, flagValue);
      flag.write(createValue(ints[1]));
      return createValue(ints[0]);
    };
  }

  public void accept(InstructionVisitor visitor) {
    if (!visitor.visitingParameterizedUnaryAluInstruction(this))
      super.accept(visitor);
  }
}
