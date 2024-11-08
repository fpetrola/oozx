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
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterPair;

public class RepeatingInstruction<T extends WordNumber> extends AbstractInstruction<T> implements JumpInstruction<T> {
  protected Instruction<T> instructionToRepeat;
  private  ImmutableOpcodeReference<T> pc;
  protected  RegisterPair<T> bc;

  public RepeatingInstruction(Instruction<T> instructionToRepeat, ImmutableOpcodeReference<T> pc, RegisterPair<T> bc) {
    this.instructionToRepeat = instructionToRepeat;
    this.pc = pc;
    this.bc = bc;
  }

  public int execute() {
    int execute = instructionToRepeat.execute();
    setNextPC(checkLoopCondition() ? pc.read() : null);
    return execute;
  }

  protected boolean checkLoopCondition() {
    return bc.getHigh().read().isNotZero();
  }

  @Override
  public void accept(InstructionVisitor visitor) {
    super.accept(visitor);
    visitor.visitRepeatingInstruction(this);
  }

  public Instruction<T> getInstructionToRepeat() {
    return instructionToRepeat;
  }

  public void setInstructionToRepeat(Instruction<T> instructionToRepeat) {
    this.instructionToRepeat = instructionToRepeat;
  }

  public ImmutableOpcodeReference<T> getPc() {
    return pc;
  }

  public void setPc(ImmutableOpcodeReference<T> pc) {
    this.pc = pc;
  }

  public RegisterPair<T> getBc() {
    return bc;
  }

  public void setBc(RegisterPair<T> bc) {
    this.bc = bc;
  }
}
