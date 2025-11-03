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
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class ParameterizedBinaryAluInstruction<T> extends TargetSourceInstruction<T, ImmutableOpcodeReference<T>> {
  public ParameterizedBinaryAluInstruction(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag, TableAluOperation tableAluOperation) {
    super(target, source, flag);
    this.binaryAluOperation = getTBinaryAluOperation(tableAluOperation);
  }

  public interface BinaryAluOperation<T> {
    T execute(Register<T> flag, T value1, T value2);
  }

  protected BinaryAluOperation<T> binaryAluOperation;

  public ParameterizedBinaryAluInstruction(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag, BinaryAluOperation<T> binaryAluOperation) {
    super(target, source, flag);
    this.binaryAluOperation = binaryAluOperation;
  }

  public int execute() {
    final T value1 = source.read();
    final T value2 = target.read();
    target.write(binaryAluOperation.execute(flag, value1, value2));
    return cyclesCost;
  }

  public <T1> BinaryAluOperation<T1> getTBinaryAluOperation(TableAluOperation tableAluOperation) {
    return (tFlagRegister, a, value) -> {
      int value1 = ((WordNumber) value).intValue();
      int regA = ((WordNumber) a).intValue();
      int[] i = tableAluOperation.executeWithoutCarry2(value1, regA);
      flag.write(createValue(i[1]));
      return createValue(i[0]);
    };
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingParameterizedBinaryAluInstruction(this);
  }
}
