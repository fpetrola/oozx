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
import com.fpetrola.z80.opcodes.references.Condition;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public abstract class ConditionalInstruction<T extends WordNumber, C extends Condition> extends AbstractInstruction<T> implements JumpInstruction<T> {
  public void setPositionOpcodeReference(ImmutableOpcodeReference<T> positionOpcodeReference) {
    this.positionOpcodeReference = positionOpcodeReference;
  }

  protected ImmutableOpcodeReference<T> positionOpcodeReference;
  protected T jumpAddress;
  protected C condition;
  protected Register<T> pc;

  public ConditionalInstruction(ImmutableOpcodeReference<T> positionOpcodeReference, C condition, Register<T> pc) {
    this.positionOpcodeReference = positionOpcodeReference;
    this.condition = condition;
    this.pc = pc;
    incrementLengthBy(positionOpcodeReference.getLength());
  }

  public int execute() {
    return jumpIfConditionMatches();
  }

  protected int jumpIfConditionMatches() {
    T jumpAddress2 = calculateJumpAddress();
    if (condition.conditionMet(this)) {
      jumpAddress2 = beforeJump(jumpAddress2);
      setJumpAddress(jumpAddress2);
      setNextPC(jumpAddress2);
    } else
      setNextPC(null);

    return cyclesCost;
  }

  public T calculateJumpAddress() {
    return (jumpAddress= positionOpcodeReference.read());
  }

  protected T beforeJump(T jumpAddress) {
    return jumpAddress;
  }

  public T calculateRelativeJumpAddress() {
    return jumpAddress = pc.read().plus(length + (byte) positionOpcodeReference.read().intValue());
  }

  public T getJumpAddress() {
    return jumpAddress;
  }

  public void setJumpAddress(T jumpAddress) {
    this.jumpAddress = jumpAddress;
  }

  public ImmutableOpcodeReference<T> getPositionOpcodeReference() {
    return positionOpcodeReference;
  }

  public C getCondition() {
    return condition;
  }

  public String toString() {
  //  return getClass().getSimpleName() + " " + ((condition.toString().length() > 0) ? condition.toString() + ", " : "") + (jumpAddress != null ? jumpAddress : positionOpcodeReference);
    return getName() + " " + ((condition.toString().length() > 0) ? condition.toString() + ", " : "") + (jumpAddress != null ? jumpAddress : calculateRelativeJumpAddress());
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    condition.accept(visitor);
    visitor.visitingConditionalInstruction(this);
  }
}