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

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.AfterExecutionPhaseVisitor;
import fuse.tstates.phases.AfterMRPhaseVisitor;
import fuse.tstates.phases.BeforeExecutionPhaseVisitor;

import java.util.ArrayList;
import java.util.List;

import static com.fpetrola.z80.registers.RegisterName.*;

public class PhaseProcessor extends PhaseProcessorBase {

  private Register registerI = getRegister(I);
  private Register registerR = getRegister(R);
  private Register registerIR = getRegister(IR);
  private Register registerSP = getRegister(SP);
  private Register registerPC = getRegister(PC);
  private Register registerDE = getRegister(DE);
  private Register registerBC = getRegister(BC);
  private Register registerHL = getRegister(HL);
  private Register memptr;
  private Runnable dummyRunnable = () -> {
  };

  public PhaseProcessor(InstructionFetcher instructionFetcher, State state) {
    super(instructionFetcher, state);
    memptr = state.getMemptr();
  }

  public void visitingRst(RST rst) {
    addMcBeforeExecution(1);
  }

  public boolean visitingRet(Ret ret) {
    if (!(ret.getCondition() instanceof ConditionAlwaysTrue))
      addMcBeforeExecution(1);
    return false;
  }

  public boolean visiting16BitsOperation(Binary16BitsOperation binary16BitsOperation) {
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

  public void visitingLd(Ld ld) {
    addAfterExecution(ld);

    if (isLdSP(ld))
      phase.acceptBeforeExecution((e) -> addMc2(2, 0, null, this.registerIR));
    else if (ld.getTarget().equals(registerI) || ld.getTarget().equals(registerR) || ld instanceof LdAI || ld instanceof LdAR)
      phase.acceptBeforeExecution((e) -> addMc2(1, 0, null, this.registerIR));

    if (isMemoryPlus(ld.getTarget()) && isMemory8BitReference(ld.getSource()))
      phase.acceptBeforeWrite((e) -> addMc2(2, 3, null, registerPC));

    if (!isMemory8BitReference(ld.getSource()) && (isMemoryPlus(ld.getSource()) || isMemoryPlus(ld.getTarget())))
      phase.acceptAfterMR((e) -> switchByReadCount(() -> {
        addMultipleMc(5, 1, 0, registerIR.read(), null);
      }));
  }

  private void addAfterExecution(Ld ld) {
    final List<Runnable> afterExecutionActions = new ArrayList<>();

    ld.getTarget().accept(new InstructionVisitor<>() {
      public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference1) {
        boolean b = indirectMemory8BitReference1.getTarget() instanceof Register register && (register.getName().equals("BC") || register.getName().equals("DE"));
        if (b || indirectMemory8BitReference1.getTarget() instanceof Memory16BitReference)
          afterExecutionActions.add(() -> {
            Integer wordNumber = ld.getSource().read();
            Integer wordNumber1 = (indirectMemory8BitReference1.address + 1) & 0xFFFF;
            Integer number = (wordNumber << 8) & 0xFFFF;
            int i = ((wordNumber1 & 0xff) & 0xFFFF) & 0xFFFF;
            memptr.write((number | i) & 0xFFFF);
          });
      }

      public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
        afterExecutionActions.add(() -> {
          memptr.write((indirectMemory16BitReference.address + 1) & 0xFFFF);
        });
      }

      public boolean visitMemory16BitReference(Memory16BitReference memory16BitReference) {
        afterExecutionActions.add(() -> {
          memptr.write((memory16BitReference.read() + 2) & 0xFFFF);
        });
        return false;
      }
    });
    ld.getSource().accept(new InstructionVisitor<>() {
      public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
        boolean b = indirectMemory8BitReference.getTarget() instanceof Register register && (register.getName().equals("BC") || register.getName().equals("DE"));
        if (b || indirectMemory8BitReference.getTarget() instanceof Memory16BitReference)
          afterExecutionActions.add(() -> {
            memptr.write((indirectMemory8BitReference.address + 1) & 0xFFFF);
          });
      }

      public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
        afterExecutionActions.add(() -> {
          memptr.write((indirectMemory16BitReference.address + 1) & 0xFFFF);
        });
      }
    });

    if (!afterExecutionActions.isEmpty()) {
      Runnable runnable = computeActions(afterExecutionActions);
      phase.acceptAfterExecution((e) -> runnable.run());
    }
  }

  public void visitingJR(JR jr) {
    phase.acceptAfterExecution(afterExecution -> addForRelativeJump(jr));
  }

  public boolean visitingDjnz(DJNZ djnz) {
    phase.acceptBeforeExecution((e) -> addMc2(1, 0, null, this.registerIR));
    phase.acceptAfterExecution((e) -> addForRelativeJump(djnz));
    return false;
  }

  private void addForRelativeJump(JumpInstruction conditionalInstruction) {
    hasJumped(conditionalInstruction).ifPresentOrElse(
        (a) -> addMultipleMc(5, 1, 1, valueOf(registerPC), null),
        () -> addMultipleMc(1, 3, 1, valueOf(registerPC), "readbyte")
    );
  }

  public void visitEx(Ex ex) {
    final List<Runnable> afterExecutionActions = new ArrayList<>();

    if (ex.getTarget() instanceof IndirectMemory16BitReference indirectMemory16BitReference)
      if (indirectMemory16BitReference.getTarget() instanceof Register register && register.getName().equals(RegisterName.SP.name())) {
        afterExecutionActions.add(() -> memptr.write(ex.getSource().read()));
      }

    if (ex.getTarget() instanceof IndirectMemory16BitReference) {
      afterExecutionActions.add(() -> addMc2(2, 0, "contend_write_no_mreq", registerSP));
    }

    if (!afterExecutionActions.isEmpty()) {
      Runnable runnable = computeActions(afterExecutionActions);
      phase.acceptAfterExecution((e) -> runnable.run());
    }

    if (ex.getTarget() instanceof IndirectMemory16BitReference)
      phase.acceptBeforeWrite((e) -> writeCountIsZero().ifPresent(x -> addMc2(1, 1, null, registerSP)));
  }

  private Runnable computeActions(List<Runnable> afterExecutionActions) {
    Runnable action = afterExecutionActions.get(0);
    if (afterExecutionActions.size() == 2) {
      Runnable action1 = afterExecutionActions.get(0);
      Runnable action2 = afterExecutionActions.get(1);
      action = () -> {
        action1.run();
        action2.run();
      };
    }
    return action;
  }

  public boolean visitRepeatingInstruction(RepeatingInstruction instruction) {
//    instruction.getInstructionToRepeat().accept(this);

    int delta;
    Register registerName;
    int times;

    if (instruction instanceof Outdr || instruction instanceof Outir) {
      registerName = registerBC;
      delta = 0;
      times = 0;
    } else if (instruction instanceof Inir) {
      delta = -1;
      registerName = this.registerHL;
      times = 0;
    } else if (instruction instanceof Indr) {
      delta = 1;
      registerName = this.registerHL;
      times = 0;
    } else if (instruction instanceof Cpir) {
      delta = -1;
      registerName = this.registerHL;
      times = 5;
    } else if (instruction instanceof Cpdr) {
      delta = 1;
      registerName = this.registerHL;
      times = 5;
    } else if (instruction instanceof Ldir) {
      registerName = registerDE;
      delta = -1;
      times = 2;
    } else {
      registerName = registerDE;
      delta = 1;
      times = 2;
    }

    if (times > 0) {
      phase.acceptAfterExecution((a) -> {
        getAfterExecutionPhaseVisitorForBlock(times, delta, registerName).visit(a);
        hasJumped(instruction).ifPresent(x -> {
          addMultipleMc(5, 1, 0, registerName.read() + delta, "contend_write_no_mreq");
        });
      });
    } else {
      addMcBeforeExecution(1);
      phase.acceptAfterExecution((a) -> {
        hasJumped(instruction).ifPresent(x -> {
          addMultipleMc(5, 1, 0, registerName.read() + delta, "contend_write_no_mreq");
        });
      });
    }

    return false;
  }

  private AfterExecutionPhaseVisitor addForBlockInstruction(int times, int delta, Register register) {
    AfterExecutionPhaseVisitor contendWriteNoMreq = getAfterExecutionPhaseVisitorForBlock(times, delta, register);
    phase.acceptAfterExecution(contendWriteNoMreq);
    return contendWriteNoMreq;
  }

  private AfterExecutionPhaseVisitor getAfterExecutionPhaseVisitorForBlock(int times, int delta, Register register) {
    return p -> addMc2(times, delta, "contend_write_no_mreq", register);
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    addMcBeforeExecution(1);
  }

  public boolean visitLdi(Ldi ldi) {
    addForBlockInstruction(2, -1, registerDE);
    return true;
  }

  public boolean visitLdd(Ldd ldd) {
    addForBlockInstruction(2, 1, registerDE);
    return true;
  }

  public boolean visitCpi(Cpi cpi) {
    addForBlockInstruction(5, -1, registerHL);
    return true;
  }

  public boolean visitCpd(Cpd cpd) {
    addForBlockInstruction(5, 1, registerHL);
    return true;
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    AfterMRPhaseVisitor afterMRPhaseVisitor = p -> {
      if (readCount == 4) {
        addMultipleMc(1, 1, 0, address, null);
      }
    };
    processTargetInstruction((TargetInstruction) ldOperation.getInstruction(), afterMRPhaseVisitor);
    return true;
  }

  public boolean visitingBitOperation(BitOperation instruction) {
    return processTargetInstruction(instruction, (e) -> {
    });
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction instruction) {
    return processTargetInstruction(instruction, (e) -> {
    });
  }

  private boolean processTargetInstruction(TargetInstruction instruction, AfterMRPhaseVisitor afterMRPhaseVisitor) {
    isMemoryPlusOptional(instruction.getTarget()).ifPresent(x -> {
      phase.acceptBeforeExecution((e -> {
        if (readCount == 0)
          addMultipleMc(2, 1, 3, valueOf(registerPC), null);
      }));

      phase.acceptAfterMR((e -> {
        if (readCount == 1) {
          addMultipleMc(1, 1, 0, address, null);
        }

        afterMRPhaseVisitor.visit(e);
      }));
    });

    isIndirectHL(instruction).ifPresent((x) -> {
      phase.acceptAfterMR(e -> {
        if (readCount == 1)
          addMultipleMc(1, 1, 0, valueOf(registerHL), null);

        afterMRPhaseVisitor.visit(e);
      });
    });

    return true;
  }

  private Register getRegisterHL() {
    return this.registerHL;
  }

  public boolean visitingInc(Inc tInc) {
    addMcForDecInc(tInc);
    return true;
  }

  public boolean visitingDec(Dec dec) {
    addMcForDecInc(dec);
    return true;
  }

  private void addMcForDecInc(TargetInstruction instruction) {
    isMemoryPlusOptional(instruction.getTarget()).ifPresent(x -> phase.acceptAfterMR((e -> {
      switchByReadCount(
          () -> addMultipleMc(5, 1, 2, valueOf(registerPC), null),
          () -> {
            addMultipleMc(1, 1, 0, address, null);
          }
      );
    })));

    isIndirectHL(instruction).ifPresent((x) -> phase.acceptBeforeWrite(e -> addMc2(1, 0, null, registerHL)));
  }

  public boolean visitRLD(RLD rld) {
    phase.acceptAfterMR(p -> addMultipleMc(4, 1, 0, valueOf(registerHL), null));
    return false;
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction instruction) {
    isMemoryPlusOptional(instruction.getSource()).ifPresent(x ->
        phase.acceptAfterMR((e -> switchByReadCount(() -> addMultipleMc(5, 1, 0, valueOf(registerIR), null)))));
  }

  public boolean visitingCall(Call tCall) {
    phase.acceptBeforeWrite(e -> writeCountIsZero().ifPresent(x -> addMc2(1, 2, null, registerPC)));
    return false;
  }

  private BeforeExecutionPhaseVisitor addMcBeforeExecution(final int time) {
    BeforeExecutionPhaseVisitor beforeExecutionPhaseVisitor = beforeExecution -> addMc2(time, 0, null, this.registerIR);
    phase.acceptBeforeExecution(beforeExecutionPhaseVisitor);
    return beforeExecutionPhaseVisitor;
  }

  private void addMcAfterExecution() {
    phase.acceptAfterExecution(afterExecution -> addMc2(2, 0, null, this.registerIR));
  }
}
