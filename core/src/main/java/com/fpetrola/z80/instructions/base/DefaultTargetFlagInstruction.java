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

import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public abstract class DefaultTargetFlagInstruction<T extends WordNumber> extends DefaultTargetInstruction<T> implements FlagInstruction<T> {
  protected Register<T> flag;

  public DefaultTargetFlagInstruction(OpcodeReference<T> target, Register<T> flag) {
    super(target);
    this.flag = flag;
    incrementLengthBy(target.getLength());
  }

  @Override
  public Register<T> getFlag() {
    return flag;
  }

  public void setFlag(Register<T> flag) {
    this.flag = flag;
  }

  public void accept(InstructionVisitor visitor) {
    visitor.visitingTarget(getTarget(), this);
    visitor.visitingFlag(flag, this);
    visitor.visitingTargetInstruction(this);
  }
}