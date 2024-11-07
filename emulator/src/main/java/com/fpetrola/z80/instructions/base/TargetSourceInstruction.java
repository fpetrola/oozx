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

package com.fpetrola.z80.instructions.base;

import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public abstract class TargetSourceInstruction<T extends WordNumber, S extends ImmutableOpcodeReference<T>> extends DefaultTargetFlagInstruction<T> {
  protected S source;

  public TargetSourceInstruction(OpcodeReference<T> target, S source, Register<T> flag) {
    super(target, flag);
    this.source = source;
    incrementLengthBy(source.getLength());
    cyclesCost += 1;
  }

  public String toString() {
    return super.toString() + ", " + source;
  }

  public S getSource() {
    return source;
  }

  public void setSource(S source) {
    this.source = source;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitingFlag(getFlag(), this);
    visitor.visitingSource(getSource(), this);
    visitor.visitingTarget(getTarget(), this);
    visitor.visitingTargetSourceInstruction(this);
  }
}