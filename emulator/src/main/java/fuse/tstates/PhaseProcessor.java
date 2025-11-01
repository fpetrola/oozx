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

package fuse.tstates;

import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.ConditionAlwaysTrue;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.AfterExecutionPhaseVisitor;
import fuse.tstates.phases.AfterMRPhaseVisitor;
import fuse.tstates.phases.BeforeExecutionPhaseVisitor;

import static com.fpetrola.z80.registers.RegisterName.*;

public class PhaseProcessor<T extends WordNumber> extends PhaseProcessorBase<T> {

  private Register<T> registerI = getRegister(I);
  private Register<T> registerR = getRegister(R);
  private Register<T> registerIR = getRegister(IR);

  public PhaseProcessor(InstructionFetcher<T> instructionFetcher, State<T> state) {
    super(instructionFetcher, state);
  }

  public void visitingRst(RST rst) {
    addMcBeforeExecution(1);
  }

  public boolean visitingRet(Ret ret) {
    if (!(ret.getCondition() instanceof ConditionAlwaysTrue))
      addMcBeforeExecution(1);
    return false;
  }

  public boolean visiting16BitsOperation(Binary16BitsOperation<T> binary16BitsOperation) {
    addMcBeforeExecution(7);
    return true;
  }

  public void visitingInc16(Inc16 tInc16) {
    addMcAfterExecution();
  }

  public void visitingDec16(Dec16 tDec16) {
    addMcAfterExecution();
  }

  public void visitPush(Push push) {
    addMcBeforeExecution(1);
  }

  public void visitingLd(Ld<T> ld) {
    if (isLdSP(ld))
      phase.acceptBeforeExecution((e) -> addMc(2, IR, 0, null));
    else if (ld.getTarget().equals(registerI) || ld.getTarget().equals(registerR) || ld instanceof LdAI<T> || ld instanceof LdAR<T>)
      phase.acceptBeforeExecution((e) -> addMc(1, IR, 0, null));

    if (isMemoryPlus(ld.getTarget()) && isMemory8BitReference(ld.getSource()))
      phase.acceptBeforeWrite((e) -> addMc(2, PC, 3, null));

    if (!isMemory8BitReference(ld.getSource()) && (isMemoryPlus(ld.getSource()) || isMemoryPlus(ld.getTarget())))
      phase.acceptAfterMR((e) -> switchByReadCount(() -> addMultipleMc(5, 1, 0, registerIR.read().intValue(), null)));
  }

  public void visitingJR(JR jr) {
    phase.acceptAfterExecution(afterExecution -> addForRelativeJump(jr));
  }

  public boolean visitingDjnz(DJNZ<T> djnz) {
    phase.acceptBeforeExecution((e) -> addMc(1, IR, 0, null));
    phase.acceptAfterExecution((e) -> addForRelativeJump(djnz));
    return false;
  }

  private void addForRelativeJump(JumpInstruction<T> conditionalInstruction) {
    hasJumped(conditionalInstruction).ifPresentOrElse(
        (a) -> addMultipleMc(5, 1, 1, valueOf(PC), null),
        () -> addMultipleMc(1, 3, 1, valueOf(PC), "readbyte")
    );
  }

  public void visitEx(Ex<T> ex) {
    if (ex.getTarget() instanceof IndirectMemory16BitReference<T>)
      phase.acceptAfterExecution((e) -> addMc(2, SP, 0, "contend_write_no_mreq"));

    if (ex.getTarget() instanceof IndirectMemory16BitReference<T>)
      phase.acceptBeforeWrite((e) -> writeCountIsZero().ifPresent(x -> addMc(1, SP, 1, null)));
  }

  public boolean visitRepeatingInstruction(RepeatingInstruction<T> instruction) {
//    instruction.getInstructionToRepeat().accept(this);

    int delta;
    RegisterName registerName;
    int times;

    if (instruction instanceof Outdr<T> || instruction instanceof Outir<T>) {
      registerName = BC;
      delta = 0;
      times = 0;
    } else if (instruction instanceof Inir<T>) {
      delta = -1;
      registerName = HL;
      times = 0;
    } else if (instruction instanceof Indr<T>) {
      delta = 1;
      registerName = HL;
      times = 0;
    } else if (instruction instanceof Cpir<T>) {
      delta = -1;
      registerName = HL;
      times = 5;
    } else if (instruction instanceof Cpdr<T>) {
      delta = 1;
      registerName = HL;
      times = 5;
    } else if (instruction instanceof Ldir<T>) {
      registerName = DE;
      delta = -1;
      times = 2;
    } else {
      registerName = DE;
      delta = 1;
      times = 2;
    }

    if (times > 0) {
      phase.acceptAfterExecution((a) -> {
        getAfterExecutionPhaseVisitorForBlock(times, registerName, delta).visit(a);
        hasJumped(instruction).ifPresent(x -> addMultipleMc(5, 1, 0, valueOf(registerName) + delta, "contend_write_no_mreq"));
      });
    } else {
      addMcBeforeExecution(1);
      phase.acceptAfterExecution((a) -> {
        hasJumped(instruction).ifPresent(x -> addMultipleMc(5, 1, 0, valueOf(registerName) + delta, "contend_write_no_mreq"));
      });
    }

    return false;
  }

  private AfterExecutionPhaseVisitor addForBlockInstruction(int times, RegisterName registerName, int delta) {
    AfterExecutionPhaseVisitor contendWriteNoMreq = getAfterExecutionPhaseVisitorForBlock(times, registerName, delta);
    phase.acceptAfterExecution(contendWriteNoMreq);
    return contendWriteNoMreq;
  }

  private AfterExecutionPhaseVisitor getAfterExecutionPhaseVisitorForBlock(int times, RegisterName registerName, int delta) {
    return p -> addMc(times, registerName, delta, "contend_write_no_mreq");
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    addMcBeforeExecution(1);
  }

  public boolean visitLdi(Ldi<T> ldi) {
    addForBlockInstruction(2, DE, -1);
    return true;
  }

  public boolean visitLdd(Ldd<T> ldd) {
    addForBlockInstruction(2, DE, 1);
    return true;
  }

  public boolean visitCpi(Cpi<T> cpi) {
    addForBlockInstruction(5, HL, -1);
    return true;
  }

  public boolean visitCpd(Cpd<T> cpd) {
    addForBlockInstruction(5, HL, 1);
    return true;
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    AfterMRPhaseVisitor afterMRPhaseVisitor = p -> {
      if (readCount == 4)
        addMultipleMc(1, 1, 0, address.intValue(), null);
    };
    processTargetInstruction((TargetInstruction<T>) ldOperation.getInstruction(), afterMRPhaseVisitor);
    return true;
  }

  public boolean visitingBitOperation(BitOperation<T> instruction) {
    return processTargetInstruction(instruction, (e) -> {
    });
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction<T> instruction) {
    return processTargetInstruction(instruction, (e) -> {
    });
  }

  private boolean processTargetInstruction(TargetInstruction<T> instruction, AfterMRPhaseVisitor afterMRPhaseVisitor) {
    isMemoryPlusOptional(instruction.getTarget()).ifPresent(x -> {
      phase.acceptBeforeExecution((e -> {
        if (readCount == 0)
          addMultipleMc(2, 1, 3, valueOf(PC), null);
      }));

      phase.acceptAfterMR((e -> {
        if (readCount == 1) {
          addMultipleMc(1, 1, 0, address.intValue(), null);
        }

        afterMRPhaseVisitor.visit(e);
      }));
    });

    isIndirectHL(instruction).ifPresent((x) -> {
      phase.acceptAfterMR(e -> {
        if (readCount == 1)
          addMultipleMc(1, 1, 0, valueOf(HL), null);

        afterMRPhaseVisitor.visit(e);
      });
    });

    return true;
  }

  public boolean visitingInc(Inc<T> tInc) {
    addMcForDecInc(tInc);
    return true;
  }

  public boolean visitingDec(Dec<T> dec) {
    addMcForDecInc(dec);
    return true;
  }

  private void addMcForDecInc(TargetInstruction<T> instruction) {
    isMemoryPlusOptional(instruction.getTarget()).ifPresent(x -> phase.acceptAfterMR((e -> {
      switchByReadCount(
          () -> addMultipleMc(5, 1, 2, valueOf(PC), null),
          () -> addMultipleMc(1, 1, 0, address.intValue(), null)
      );
    })));

    isIndirectHL(instruction).ifPresent((x) -> phase.acceptBeforeWrite(e -> addMc(1, HL, 0, null)));
  }

  public boolean visitRLD(RLD<T> rld) {
    phase.acceptAfterMR(p -> addMultipleMc(4, 1, 0, valueOf(HL), null));
    return false;
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction<T> instruction) {
    isMemoryPlusOptional(instruction.getSource()).ifPresent(x ->
        phase.acceptAfterMR((e -> switchByReadCount(() -> addMultipleMc(5, 1, 0, valueOf(IR), null)))));
  }

  public boolean visitingCall(Call tCall) {
    phase.acceptBeforeWrite(e -> writeCountIsZero().ifPresent(x -> addMc(1, PC, 2, null)));
    return false;
  }

  private BeforeExecutionPhaseVisitor addMcBeforeExecution(final int time) {
    BeforeExecutionPhaseVisitor beforeExecutionPhaseVisitor = beforeExecution -> addMc(time, IR, 0, null);
    phase.acceptBeforeExecution(beforeExecutionPhaseVisitor);
    return beforeExecutionPhaseVisitor;
  }

  private void addMcAfterExecution() {
    phase.acceptAfterExecution(afterExecution -> addMc(2, IR, 0, null));
  }
}
