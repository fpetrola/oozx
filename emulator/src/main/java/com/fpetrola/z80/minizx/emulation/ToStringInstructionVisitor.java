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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.Memory8BitReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

public class ToStringInstructionVisitor<T extends WordNumber> implements InstructionVisitor<T, String> {
  String result;

  public String createToString(Instruction<T> instruction) {
    instruction.accept(this);
    String result1 = getResult();
    if (result1 == null || result1.isEmpty())
      return getInstructionName(instruction) + "";
    return result1;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public void visitingTargetSourceInstruction(TargetSourceInstruction instruction) {
    ToStringInstructionVisitor<T> instructionVisitor = new ToStringInstructionVisitor<>();
    instruction.getTarget().accept(instructionVisitor);
    String targetString = instructionVisitor.getResult();
    instruction.getSource().accept(instructionVisitor);
    String sourceString = instructionVisitor.getResult();
    result = getInstructionName(instruction) + " " + targetString + ", " + sourceString;
  }

  public void visitingTargetInstruction(TargetInstruction instruction) {
    toStringForTargetInstruction(instruction.getTarget(), getInstructionName(instruction));
  }

  private String getInstructionName(Instruction instruction) {
    return instruction.getClass().getSimpleName().toUpperCase();
  }

  public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
    conditionalInstruction.calculateJumpAddress();
    WordNumber jumpAddress = conditionalInstruction.getJumpAddress();
    String string = conditionalInstruction.getCondition().toString();
    String s = " " + ((!string.isEmpty()) ? string + ", " : "") + (jumpAddress != null ? Helper.formatAddress(jumpAddress.intValue()) : 0);
    if (conditionalInstruction instanceof Ret<?>)
      s = " " + ((!string.isEmpty()) ? string : "");

    result = getInstructionName(conditionalInstruction) + s;
  }

  public boolean visitingDjnz(DJNZ<T> conditionalInstruction) {
    conditionalInstruction.calculateJumpAddress();
    WordNumber jumpAddress = conditionalInstruction.getJumpAddress();
    result = getInstructionName(conditionalInstruction) + " " + (jumpAddress != null ? Helper.formatAddress(jumpAddress.intValue()) : 0);
    return true;
  }

  public void visitOpcodeReference(OpcodeReference opcodeReference) {
    result = opcodeReference.toString();
  }

  public void visitImmutableOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {
    result = immutableOpcodeReference.toString();
  }

  public boolean visitMemory8BitReference(Memory8BitReference<T> memory8BitReference) {
    result = Helper.formatAddress(memory8BitReference.read().intValue());
    return true;
  }

  public boolean visitRegister(Register register) {
    result = register.toString();
    return true;
  }

  public void visitEx(Ex<T> instruction) {
    ToStringInstructionVisitor<T> instructionVisitor = new ToStringInstructionVisitor<>();
    instruction.getTarget().accept(instructionVisitor);
    String targetString = instructionVisitor.getResult();
    instruction.getSource().accept(instructionVisitor);
    String sourceString = instructionVisitor.getResult();
    result = getInstructionName(instruction) + " " + targetString + ", " + sourceString;
  }


  public void visitingBitOperation(BitOperation tBitOperation) {
    ToStringInstructionVisitor<T> instructionVisitor = new ToStringInstructionVisitor<>();
    tBitOperation.getTarget().accept(instructionVisitor);
    String targetString = instructionVisitor.getResult();

    result = getInstructionName(tBitOperation) + " " + tBitOperation.getN() + ", " + targetString;
  }

  public void visitPush(Push push) {
    toStringForTargetInstruction(push.getTarget(), getInstructionName(push));
  }

  public void visitingPop(Pop pop) {
    toStringForTargetInstruction(pop.getTarget(), getInstructionName(pop));
  }

  private void toStringForTargetInstruction(OpcodeReference opcodeReference, String instructionName) {
    ToStringInstructionVisitor<T> instructionVisitor = new ToStringInstructionVisitor<>();
    opcodeReference.accept(instructionVisitor);
    String targetString = instructionVisitor.getResult();
    result = instructionName + " " + targetString;
  }

  public void visitingInc16(Inc16 tInc16) {
    toStringForTargetInstruction(tInc16.getTarget(), "INC");
  }

  public void visitingDec16(Dec16 tDec16) {
    toStringForTargetInstruction(tDec16.getTarget(), "DEC");
  }

  public boolean visitingAdd16(Add16 tAdd16) {
    toStringForTargetInstruction(tAdd16.getTarget(), "ADD");
    return true;
  }

  public boolean visitingAdc16(Adc16<T> tAdc16) {
    toStringForTargetInstruction(tAdc16.getTarget(), "ADC");
    return true;
  }

  public boolean visitingSbc16(Sbc16<T> sbc16) {
    toStringForTargetInstruction(sbc16.getTarget(), "SBC");
    return true;
  }

  public boolean visitRepeatingInstruction(RepeatingInstruction<T> tRepeatingInstruction) {
    result = getInstructionName(tRepeatingInstruction);
    return true;
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    result = getInstructionName(blockInstruction);
  }
}
