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
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.opcodes.references.Condition;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.Register;

public abstract class ConditionalInstruction<C extends Condition> extends AbstractInstruction implements JumpInstruction {
  public void setPositionOpcodeReference(ImmutableOpcodeReference positionOpcodeReference) {
    this.positionOpcodeReference = positionOpcodeReference;
  }

  protected ImmutableOpcodeReference positionOpcodeReference;
  protected int jumpAddress;
  protected C condition;
  protected Register pc;

  public ConditionalInstruction(ImmutableOpcodeReference positionOpcodeReference, C condition, Register pc) {
    this.positionOpcodeReference = positionOpcodeReference;
    this.condition = condition;
    this.pc = pc;
    incrementLengthBy(positionOpcodeReference.getLength());
  }

  public int execute() {
    return jumpIfConditionMatches();
  }

  protected int jumpIfConditionMatches() {
    int jumpAddress2 = calculateJumpAddress();
    if (condition.conditionMet(this)) {
      jumpAddress2 = beforeJump(jumpAddress2);
      setJumpAddress(jumpAddress2);
      setNextPC(jumpAddress2);
    } else
      setNextPC(null);

    return cyclesCost;
  }

  public int calculateJumpAddress() {
    return (jumpAddress = positionOpcodeReference.read());
  }

  protected int beforeJump(int jumpAddress) {
    return jumpAddress;
  }

  public int calculateRelativeJumpAddress() {
    Integer wordNumber = pc.read();
    int i = length + (byte) positionOpcodeReference.read();
    return jumpAddress = (wordNumber + i) & 0xFFFF;
  }

  public int getJumpAddress() {
    return jumpAddress;
  }

  public void setJumpAddress(int jumpAddress) {
    this.jumpAddress = jumpAddress;
  }

  public ImmutableOpcodeReference getPositionOpcodeReference() {
    return positionOpcodeReference;
  }

  public C getCondition() {
    return condition;
  }

  public String toString() {
    //  return getClass().getSimpleName() + " " + ((condition.toString().length() > 0) ? condition.toString() + ", " : "") + (jumpAddress != null ? jumpAddress : positionOpcodeReference);
//    return getName() + " " + ((condition.toString().length() > 0) ? condition.toString() + ", " : "") + (jumpAddress != null ? jumpAddress : calculateRelativeJumpAddress());
    int jumpAddress1 = jumpAddress;
    return getName() + " " + ((condition.toString().length() > 0) ? condition.toString() + ", " : "") + (jumpAddress1 != -1 ? Helper.formatAddress(jumpAddress1) : 0);
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    condition.accept(visitor);
    visitor.visitingConditionalInstruction(this);
  }
}