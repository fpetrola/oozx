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

package com.fpetrola.z80.base;

import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

/** An instruction as the assembly that was written to get it: {@code PUSH AF}, {@code LD HL, (5C3A)}. */
public class ToStringInstructionVisitor implements InstructionVisitor<String> {
  String result;

  public String createToString(Instruction instruction) {
    instruction.accept(this);
    String result1 = getResult();
    if (result1 == null || result1.isEmpty())
      return getInstructionName(instruction) + "";
    return result1;
  }

  public String getResult() {
    return result;
  }

  /** What one operand reads as. Its own visitor: this one is in the middle of the instruction. */
  private String textOf(OpcodeReferenceBase reference) {
    ToStringInstructionVisitor operand = new ToStringInstructionVisitor();
    reference.accept(operand);
    return operand.getResult();
  }

  /**
   * Every number is hex, four digits when it is an address, and says so: without the prefix a
   * two-digit one is not tellable from the register whose name it spells.
   */
  private static String hex(int value, int digits) {
    return String.format("0x%0" + digits + "X", value & (digits == 2 ? 0xff : 0xffff));
  }

  public void setResult(String result) {
    this.result = result;
  }

  public void visitingTargetSourceInstruction(TargetSourceInstruction instruction) {
    result = getInstructionName(instruction) + " " + textOf(instruction.getTarget()) + ", " + textOf(instruction.getSource());
  }

  public void visitingTargetInstruction(TargetInstruction instruction) {
    toStringForTargetInstruction(instruction.getTarget(), getInstructionName(instruction));
  }

  private String getInstructionName(Instruction instruction) {
    return instruction.getClass().getSimpleName().toUpperCase();
  }

  public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
    conditionalInstruction.calculateJumpAddress();
    Integer jumpAddress = conditionalInstruction.getJumpAddress();
    String string = conditionalInstruction.getCondition().toString();
    String s = " " + ((!string.isEmpty()) ? string + ", " : "") + (jumpAddress != null ? hex(jumpAddress, 4) : 0);
    if (conditionalInstruction instanceof Ret)
      s = " " + ((!string.isEmpty()) ? string : "");

    result = getInstructionName(conditionalInstruction) + s;
  }

  public boolean visitingDjnz(DJNZ conditionalInstruction) {
    conditionalInstruction.calculateJumpAddress();
    Integer jumpAddress = conditionalInstruction.getJumpAddress();
    result = getInstructionName(conditionalInstruction) + " " + (jumpAddress != null ? hex(jumpAddress, 4) : 0);
    return true;
  }

  public void visitOpcodeReference(OpcodeReference opcodeReference) {
    result = opcodeReference.toString();
  }

  public void visitImmutableOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {
    result = immutableOpcodeReference.toString();
  }

  public boolean visitMemory8BitReference(Memory8BitReference memory8BitReference) {
    result = hex(memory8BitReference.read(), 2);
    return true;
  }

  public boolean visitMemory16BitReference(Memory16BitReference memory16BitReference) {
    result = hex(memory16BitReference.read(), 4);
    return true;
  }

  public boolean visitRegister(Register register) {
    result = register.getName();
    return true;
  }

  public void visitEx(Ex instruction) {
    result = getInstructionName(instruction) + " " + textOf(instruction.getTarget()) + ", " + textOf(instruction.getSource());
  }


  public boolean visitingBitOperation(BitOperation tBitOperation) {
    result = getInstructionName(tBitOperation) + " " + tBitOperation.getN() + ", " + textOf(tBitOperation.getTarget());
    return true;
  }

  public void visitPush(Push push) {
    toStringForTargetInstruction(push.getTarget(), getInstructionName(push));
  }

  public void visitingPop(Pop pop) {
    toStringForTargetInstruction(pop.getTarget(), getInstructionName(pop));
  }

  private void toStringForTargetInstruction(OpcodeReference opcodeReference, String instructionName) {
    result = instructionName + " " + textOf(opcodeReference);
  }

  public void visitingInc16(Inc16 tInc16) {
    toStringForTargetInstruction(tInc16.getTarget(), "INC");
  }

  public void visitingDec16(Dec16 tDec16) {
    toStringForTargetInstruction(tDec16.getTarget(), "DEC");
  }

  public boolean visitingAdd16(Add16 tAdd16) {
    toStringForTargetInstruction(tAdd16.getTarget(), "ADD");
    result = result + ", " + textOf(tAdd16.getSource());
    return true;
  }

  public boolean visitingAdc16(Adc16 tAdc16) {
    toStringForTargetInstruction(tAdc16.getTarget(), "ADC");
    return true;
  }

  public boolean visitingSbc16(Sbc16 sbc16) {
    toStringForTargetInstruction(sbc16.getTarget(), "SBC");
    return true;
  }

  public boolean visitRepeatingInstruction(RepeatingInstruction tRepeatingInstruction) {
    result = getInstructionName(tRepeatingInstruction);
    return true;
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    result = getInstructionName(blockInstruction);
  }

  @Override
  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
    // The displacement is read from the memory the instruction was decoded out of: the field
    // that holds it is only filled when the instruction runs, and nothing here has run.
    byte dd = memoryPlusRegister8BitReference.fetchRelative();
    result = "(" + textOf(memoryPlusRegister8BitReference.getTarget())
        + (dd < 0 ? "-" : "+") + "%02X".formatted(Math.abs(dd)) + ")";
  }

  @Override
  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    result = "(" + textOf(indirectMemory8BitReference.getTarget()) + ")";
  }

  @Override
  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    result = "(" + textOf(indirectMemory16BitReference.getTarget()) + ")";
  }

  @Override
  public boolean visitLdOperation(LdOperation ldOperation) {
    String toString = new ToStringInstructionVisitor().createToString(ldOperation.getInstruction());
    result = "LD " + textOf(ldOperation.getTarget()) + ", " + toString;
    return true;
  }
}
