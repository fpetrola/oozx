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
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.DirectAccessWordNumber;

import java.util.Collections;

public class ParameterizedBinaryAluInstruction<T extends WordNumber> extends TargetSourceInstruction<T, ImmutableOpcodeReference<T>> {
  public interface BinaryAluOperation<T extends WordNumber> {
    T execute(Register<T> flag, T value1, T value2);
  }

  protected BinaryAluOperation<T> binaryAluOperation;

  public ParameterizedBinaryAluInstruction(OpcodeReference<T> target, ImmutableOpcodeReference<T> source, Register<T> flag, BinaryAluOperation<T> binaryAluOperation) {
    super(target, source, flag);
    this.binaryAluOperation = binaryAluOperation;
  }

  public int execute() {
    T value1 = source.read();

    if (source instanceof Memory8BitReference<T> || source instanceof Memory16BitReference<T>) {
      value1 = (T) new DirectAccessWordNumber(value1.intValue(), -2, Collections.emptySet());
    }
    final T value2 = target.read();
    T execute = binaryAluOperation.execute(flag, value1, value2);
    execute = (T) new DirectAccessWordNumber(execute.intValue(), -1, -1);
    execute = execute.process(value1);
    execute = execute.process(value2);

    if (source instanceof IndirectMemory8BitReference<T> indirectMemory8BitReference) {
      T read = indirectMemory8BitReference.getTarget().read();
      execute = execute.processOrigin(read);
    }
    target.write(execute);
    return cyclesCost;
  }

  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitingParameterizedBinaryAluInstruction(this);
  }
}
