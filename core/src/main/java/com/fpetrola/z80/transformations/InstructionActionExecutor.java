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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

import java.util.function.Consumer;

@SuppressWarnings("ALL")
public class InstructionActionExecutor<T extends WordNumber> implements InstructionVisitor<T> {
  private int tick;
  private Consumer<VirtualRegister<?>> actionExecutor;

  public InstructionActionExecutor(Consumer<VirtualRegister<?>> actionExecutor) {
    this.actionExecutor = actionExecutor;
  }

  @Override
  public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
    source.accept(this);
  }

  @Override
  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    indirectMemory8BitReference.target.accept(this);
  }

  @Override
  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    indirectMemory16BitReference.target.accept(this);
  }

  @Override
  public void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {
    target.accept(this);
  }

  public boolean visitRegister(Register register) {
    if (register instanceof VirtualRegister<?> virtualRegister) {
      actionExecutor.accept(virtualRegister);
    }
    return false;
  }

  private void executeAction(Object cloneable) {
    if (cloneable instanceof MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
      executeAction(memoryPlusRegister8BitReference.getTarget());
    } else if (cloneable instanceof IndirectMemory8BitReference indirectMemory8BitReference) {
      executeAction(indirectMemory8BitReference.getTarget());
    } else if (cloneable instanceof IndirectMemory16BitReference indirectMemory16BitReference) {
      executeAction(indirectMemory16BitReference.target);
    } else if (cloneable instanceof VirtualRegister<?> virtualRegister) {
      actionExecutor.accept(virtualRegister);
    }
  }

  public void visitingInc16(Inc16 inc16) {
    executeAction(inc16.getTarget());
  }

  @Override
  public void visitingDec16(Dec16 tDec16) {
    executeAction(tDec16.getTarget());
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {
    executeAction(parameterizedUnaryAluInstruction.getTarget());
    executeAction(parameterizedUnaryAluInstruction.getFlag());
    return false;
  }

  @Override
  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction parameterizedBinaryAluInstruction) {
    executeAction(parameterizedBinaryAluInstruction.getTarget());
    executeAction(parameterizedBinaryAluInstruction.getSource());
    executeAction(parameterizedBinaryAluInstruction.getFlag());
  }

  public void visitingDjnz(DJNZ<T> djnz) {
    executeAction(djnz.getPositionOpcodeReference());

    djnz.accept(new ConditionVisitor());
  }

  public void visitingJR(JR jr) {
    executeAction(jr.getPositionOpcodeReference());
    jr.accept(new ConditionVisitor());
  }

  public void visitingJP(JP jp) {
    executeAction(jp.getPositionOpcodeReference());
    jp.accept(new ConditionVisitor());
  }

  @Override
  public boolean visitingCall(Call tCall) {
    executeAction(tCall.getPositionOpcodeReference());
    tCall.accept(new ConditionVisitor());
    return false;
  }

  @Override
  public boolean visitingRet(Ret ret) {
    executeAction(ret.getPositionOpcodeReference());
    ret.accept(new ConditionVisitor());
    return false;
  }

  public void visitingVirtualAssignmentInstruction(VirtualAssignmentInstruction virtualAssignmentInstruction) {
    executeAction(virtualAssignmentInstruction.getRegister());
    executeAction(virtualAssignmentInstruction.getLastRegister().get());
  }

  @Override
  public void visitRepeatingInstruction(RepeatingInstruction tRepeatingInstruction) {
    executeAction(tRepeatingInstruction.getBc());
    executeAction(tRepeatingInstruction.getInstructionToRepeat());
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    executeAction(blockInstruction.getBc());
    executeAction(blockInstruction.getHl());
    executeAction(blockInstruction.getFlag());
  }

  @Override
  public void visitLdir(Ldir ldir) {
    Ldi instructionToRepeat = (Ldi) ldir.getInstructionToRepeat();
    executeAction(instructionToRepeat.getDe());
  }

  @Override
  public void visitCpir(Cpir cpir) {
    Cpi instructionToRepeat = (Cpi) cpir.getInstructionToRepeat();
    executeAction(instructionToRepeat.getA());
  }

  public void visitingPop(Pop pop) {
    executeAction(pop.getFlag());
    executeAction(pop.getTarget());
  }

  @Override
  public void visitingBitOperation(BitOperation tBitOperation) {
    executeAction(tBitOperation.getFlag());
    executeAction(tBitOperation.getTarget());
  }

  @Override
  public void visitPush(Push push) {
    executeAction(push.getTarget());
  }

  @Override
  public void visitIn(In tIn) {
    executeAction(tIn.getTarget());
    executeAction(tIn.getSource());
    executeAction(tIn.getA());
    executeAction(tIn.getBc());
    executeAction(tIn.getFlag());
  }


  public void visitEx(Ex ex) {
    executeAction(ex.getSource());
    executeAction(ex.getTarget());
  }

  @Override
  public void visitExx(Exx exx) {
    executeAction(exx.get_bc());
    executeAction(exx.get_hl());
    executeAction(exx.get_de());
    executeAction(exx.getBc());
    executeAction(exx.getHl());
    executeAction(exx.getDe());
  }

  @Override
  public void visitingFlag(Register<T> flag, DefaultTargetFlagInstruction targetSourceInstruction) {
    executeAction(flag);
  }

  public void executeAction(Instruction<T> instruction) {
    instruction.accept(this);
  }

  private class ConditionVisitor implements InstructionVisitor {
    public void visitingConditionFlag(ConditionFlag conditionFlag) {
      executeAction(conditionFlag.getRegister());
    }

    public void visitBNotZeroCondition(BNotZeroCondition bNotZeroCondition) {
      executeAction(bNotZeroCondition.getB());
    }
  }
}
