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
import com.fpetrola.z80.instructions.base.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class And<T extends WordNumber> extends ParameterizedBinaryAluInstruction<T> {
  protected static final TableAluOperation andTableAluOperation = new TableAluOperation() {
    public int execute(int result, int value, int carry) {
      data = 0x10;
      setS((result & 0x0080) != 0);
      setZ(result == 0);
      setPV(parity[result]);
      setUnusedFlags(result);
      return result;
    }
  };

  public And(OpcodeReference target, ImmutableOpcodeReference source, Register<T> flag) {
    super(target, source, flag, null);
  }

  @Override
  public int execute() {
    final T value1 = source.read();
    final T value2 = target.read();

    T result = value1.and(value2);

    T i = andTableAluOperation.executeWithoutCarry(value1, result, flag);

    target.write(i);
    return cyclesCost;
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingAnd(this);
  }
}
