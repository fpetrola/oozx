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

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.flag.TableAluOperation;

public class ParameterizedBinaryAluInstruction extends TargetSourceInstruction<ImmutableOpcodeReference> {
  final protected BinaryAluOperation binaryAluOperation;

  public ParameterizedBinaryAluInstruction(OpcodeReference target, ImmutableOpcodeReference source, Register flag, TableAluOperation tableAluOperation) {
    super(target, source, flag);
    this.binaryAluOperation = getTBinaryAluOperation(tableAluOperation);
  }
  public interface BinaryAluOperation {
    int execute(Register flagRegister, int sourceValue, int targetValue);
  }

  public ParameterizedBinaryAluInstruction(OpcodeReference target, ImmutableOpcodeReference source, Register flag, BinaryAluOperation binaryAluOperation) {
    super(target, source, flag);
    this.binaryAluOperation = binaryAluOperation;
  }

  public void execute() {
    assignTarget(doExecute(source.read(), target.read()));
  }

  protected int doExecute(int sourceValue, int targetValue) {
    return binaryAluOperation.execute(flag, sourceValue, targetValue);
  }

  protected void assignTarget(int execute) {
    target.write(execute);
  }

  public <T1> BinaryAluOperation getTBinaryAluOperation(TableAluOperation tableAluOperation) {
    return (flagRegister, sourceValue, targetValue) -> tableAluOperation.execute2Values(targetValue, sourceValue, flag);
  }

  public void accept(InstructionVisitor<?> visitor) {
    super.accept(visitor);
    visitor.visitingParameterizedBinaryAluInstruction(this);
  }
}
