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

package com.fpetrola.z80.blocks;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.JR;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

public class GenerateTestSourceInstructionVisitor implements InstructionVisitor<WordNumber, Integer> {
  StringBuilder result = new StringBuilder();
  private int startAddress;

  public GenerateTestSourceInstructionVisitor(int startAddress) {
    this.startAddress = startAddress;
  }

  public void visitingTargetSourceInstruction(TargetSourceInstruction targetSourceInstruction) {
    add("add(new " + targetSourceInstruction.getClass().getSimpleName() + " (");
    targetSourceInstruction.getTarget().accept(getWordNumberDummyInstructionVisitor());
    add(", ");
    targetSourceInstruction.getSource().accept(getWordNumberDummyInstructionVisitor());
    add(", f()));");
  }

  private void add(String string) {
    result.append(string);
  }

  private InstructionVisitor<WordNumber, ?> getWordNumberDummyInstructionVisitor() {
    InstructionVisitor<WordNumber, ?> instructionVisitor = new InstructionVisitor<>() {
      public boolean visitRegister(Register register) {
        add("r(" + register.getName() + ")");
        return false;
      }

      public void visitConstantOpcodeReference(ConstantOpcodeReference<WordNumber> constantOpcodeReference) {
        add("c(" + constantOpcodeReference + ")");
      }

      public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
        add("iRR(r(" + indirectMemory16BitReference + "))");
      }

      public void visitOpcodeReference(OpcodeReference opcodeReference) {
        add("c(" + opcodeReference.toString() + ")");
      }

      public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference<WordNumber> memoryPlusRegister8BitReference) {
        add("iRRn(");
        memoryPlusRegister8BitReference.getTarget().accept(this);
        add(" , " + memoryPlusRegister8BitReference.fetchRelative() + ")");
      }

      public void visitImmutableOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {
        add("c(" + immutableOpcodeReference.toString() + ")");
      }
    };
    return instructionVisitor;
  }

  public void visitingTargetInstruction(TargetInstruction targetInstruction) {
    add("add(new " + targetInstruction.getClass().getSimpleName() + " (" + targetInstruction.getTarget() + "));");
  }

//  @Override
//  public void visitingJR(JR conditionalInstruction) {
//    String simpleName = conditionalInstruction.getClass().getSimpleName();
//    String replace = conditionalInstruction.getCondition().toString().replace("FlipFlop: ", "");
//    replace= replace.toLowerCase();
//    int jumpAddress = conditionalInstruction.calculateJumpAddress().intValue();
//    String s = STR. "add(new \{ simpleName }(c(\{ jumpAddress }), \{ replace }(), r(PC)));" ;
//    add(s);
//  }

//  @Override
//  public void visitingRet(Ret conditionalInstruction) {
//    String replace = conditionalInstruction.getCondition().toString().replace("FlipFlop: ", "");
//    WordNumber jumpAddress = conditionalInstruction.getJumpAddress();
//    String simpleName = conditionalInstruction.getClass().getSimpleName();
//    add("add(new " + simpleName + " " + replace + ", " + jumpAddress + ");");
//  }

  public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
    String simpleName = conditionalInstruction.getClass().getSimpleName();
    String replace = conditionalInstruction.getCondition().toString().replace("FlipFlop: ", "");
    replace = replace.isBlank() ? ", t()" : ", " + replace.toLowerCase() + "()";
    int jumpAddress = conditionalInstruction.calculateJumpAddress().intValue();
    jumpAddress-= startAddress;

    if (conditionalInstruction instanceof JR) {
      jumpAddress= ((WordNumber) conditionalInstruction.getPositionOpcodeReference().read()).intValue();
    }
    String s = "add(new " + simpleName + "(c(" + jumpAddress + ") " + replace + ", r(PC)));";
    add(s);
  }

  @Override
  public void visitingBitOperation(BitOperation targetSourceInstruction) {
    add("add(new " + targetSourceInstruction.getClass().getSimpleName() + " (");
    targetSourceInstruction.getTarget().accept(getWordNumberDummyInstructionVisitor());
    add(", " + targetSourceInstruction.getN() + ", f()));");
  }

  @Override
  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction dec) {
    add("add(new " + dec.getClass().getSimpleName() + " (");
    dec.getTarget().accept(getWordNumberDummyInstructionVisitor());
    add(", f()));");
    return true;
  }

  public boolean visitingRet(Ret conditionalInstruction) {
    String conditionConstructor = conditionalInstruction.getCondition().toString().replace("FlipFlop: ", "");
    conditionConstructor = conditionConstructor.isBlank() ? "t()" : conditionConstructor.toLowerCase() + "()";
    String s = "add(new Ret(" + conditionConstructor + ", r(SP), mem(), r(PC)));";
    add(s);
    return true;
  }
}
