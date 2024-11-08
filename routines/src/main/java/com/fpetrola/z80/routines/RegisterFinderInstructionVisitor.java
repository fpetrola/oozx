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

package com.fpetrola.z80.routines;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.visitor.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

@SuppressWarnings("ALL")
public abstract class RegisterFinderInstructionVisitor implements InstructionVisitor<WordNumber> {
  protected boolean isTarget;
  protected boolean isSource;

  public void visitPush(Push push) {
    isSource = true;
    push.getTarget().accept(this);
  }

  public void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {
    isSource = false;
    isTarget = true;
    target.accept(this);
  }

  public void visitingFlag(Register<WordNumber> flag, DefaultTargetFlagInstruction targetSourceInstruction) {
    isSource = true;
    isTarget = true;
    flag.accept(this);
  }

  public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
    isSource = true;
    isTarget = false;
    source.accept(this);
  }

  public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
    isSource = true;
    conditionalInstruction.getPositionOpcodeReference().accept(this);
  }

  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    indirectMemory8BitReference.target.accept(this);
  }

  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    indirectMemory16BitReference.target.accept(this);
  }

  public void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference memoryAccessOpcodeReference) {
    memoryAccessOpcodeReference.getC().accept(this);
  }

  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    memoryPlusRegister8BitReference.getTarget().accept(this);
  }

  public void visitingPop(Pop pop) {
    isTarget = true;
    pop.getTarget().accept(this);
  }

  public void visitEx(Ex ex) {
    isTarget = true;
    ex.getSource().accept(this);
    ex.getTarget().accept(this);
  }

  public void visitLdi(Ldi blockInstruction) {
    isTarget = true;
    blockInstruction.getBc().accept(this);
    blockInstruction.getHl().accept(this);
    blockInstruction.getDe().accept(this);
    blockInstruction.getFlag().accept(this);
  }

  @Override
  public void visitCpi(Cpi cpi) {
    isTarget = true;
    cpi.getBc().accept(this);
    cpi.getHl().accept(this);
    cpi.getFlag().accept(this);
  }

  @Override
  public void visitRepeatingInstruction(RepeatingInstruction repeatingInstruction) {
    isTarget = true;
    repeatingInstruction.getInstructionToRepeat().accept(this);
    repeatingInstruction.getBc().accept(this);
  }
}
