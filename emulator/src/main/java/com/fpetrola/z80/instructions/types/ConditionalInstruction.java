/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.instructions.types;

import com.fpetrola.z80.instructions.visitor.InstructionVisitor;
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