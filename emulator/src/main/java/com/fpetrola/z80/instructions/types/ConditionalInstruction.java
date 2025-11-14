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
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.opcodes.references.Condition;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.registers.Register;

public abstract class ConditionalInstruction<C extends Condition> extends AbstractInstruction implements JumpInstruction {
  final protected ImmutableOpcodeReference positionOpcodeReference;
  final protected C condition;
  final protected Register pc;
  protected int jumpAddress;

  public ConditionalInstruction(ImmutableOpcodeReference positionOpcodeReference, C condition, Register pc) {
    this.positionOpcodeReference = positionOpcodeReference;
    this.condition = condition;
    this.pc = pc;
    incrementLengthBy(positionOpcodeReference.getLength());
  }

  public void execute() {
    jumpIfConditionMatches();
  }

  protected void jumpIfConditionMatches() {
    int jumpAddress2 = calculateJumpAddress();
    if (condition.conditionMet(this)) {
      jumpAddress2 = beforeJump(jumpAddress2);
      setJumpAddress(jumpAddress2);
      setNextPC(jumpAddress2);
    } else
      setNextPC(-1);
  }

  public int calculateJumpAddress() {
    return (jumpAddress = positionOpcodeReference.read());
  }

  protected int beforeJump(int jumpAddress) {
    return jumpAddress;
  }

  public int calculateRelativeJumpAddress() {
    return jumpAddress = (pc.read() + length + (byte) positionOpcodeReference.read()) & 0xFFFF;
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
    return getName() + " " + ((condition.toString().length() > 0) ? condition.toString() + ", " : "") + (jumpAddress != -1 ? Helper.formatAddress(jumpAddress) : 0);
  }

  public void accept(InstructionVisitor visitor) {
    condition.accept(visitor);
    visitor.visitingConditionalInstruction(this);
  }
}