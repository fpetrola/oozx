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

import java.util.ArrayList;
import java.util.List;

public class PhaseProcessor extends PhaseProcessorBase {
  private Register currentRegister;

  public PhaseProcessor(InstructionFetcher instructionFetcher, State state) {
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
      phase.acceptBeforeExecution((e) -> addMultipleMCIR(2, 1, 0));
    else if (ld.getTarget().equals(registerI) || ld.getTarget().equals(registerR) || ld instanceof LdAI || ld instanceof LdAR)
      phase.acceptBeforeExecution((e) -> addMultipleMCIR(1, 1, 0));

    if (isMemoryPlus(ld.getTarget()) && isMemory8BitReference(ld.getSource()))
      phase.acceptBeforeWrite((e) -> addMc2PC(2, 3));

    if (!isMemory8BitReference(ld.getSource()) && (isMemoryPlus(ld.getSource()) || isMemoryPlus(ld.getTarget())))
      phase.acceptAfterMR((e) -> {
        if (readCount == 1)
          addMultipleMCIR(5, 1, 0);
      });
  }

  private void addMc2PC(int times, int delta) {
    addMultipleMCPc2(times, 1, delta);
  }

  private void addAfterExecution(Ld ld) {
    final List<Runnable> afterExecutionActions = new ArrayList<>();

    ld.getTarget().accept(new InstructionVisitor<>() {
      public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference1) {
        if (indirectMemory8BitReference1.getTarget() instanceof Register register
            && (register.getName().equals("BC") || register.getName().equals("DE")) || indirectMemory8BitReference1.getTarget() instanceof Memory16BitReference)
          afterExecutionActions.add(() -> memptr.write(((ld.getSource().read() << 8) | indirectMemory8BitReference1.address + 1 & 0xff) & 0xFFFF));
      }

      public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
        afterExecutionActions.add(() -> memptr.write((indirectMemory16BitReference.address + 1) & 0xFFFF));
      }

      public boolean visitMemory16BitReference(Memory16BitReference memory16BitReference) {
        afterExecutionActions.add(() -> memptr.write((memory16BitReference.read() + 2) & 0xFFFF));
        return false;
      }
    });
    ld.getSource().accept(new InstructionVisitor<>() {
      public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
        boolean b = indirectMemory8BitReference.getTarget() instanceof Register register && (register.getName().equals("BC") || register.getName().equals("DE"));
        if (b || indirectMemory8BitReference.getTarget() instanceof Memory16BitReference)
          afterExecutionActions.add(() -> memptr.write((indirectMemory8BitReference.address + 1) & 0xFFFF));
      }

      public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
        afterExecutionActions.add(() -> memptr.write((indirectMemory16BitReference.address + 1) & 0xFFFF));
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
    phase.acceptBeforeExecution((e) -> addMultipleMCIR(1, 1, 0));
    phase.acceptAfterExecution((e) -> addForRelativeJump(djnz));
    return false;
  }

  private void addForRelativeJump(JumpInstruction conditionalInstruction) {
    if (conditionalInstruction.getNextPC() != -1) {
      addMultipleMCPc2(5, 1, 1);
    } else {
      addMultipleMCPC3(1, 3, 1);
    }
  }

  private void addMultipleMCPC3(int x, int time1, int delta) {
    addMultipleMc(x, time1, delta, registerPC, "readbyte");
  }

  public void visitEx(Ex ex) {
    final List<Runnable> afterExecutionActions = new ArrayList<>();

    if (ex.getTarget() instanceof IndirectMemory16BitReference indirectMemory16BitReference)
      if (indirectMemory16BitReference.getTarget() instanceof Register register && register.getName().equals(RegisterName.SP.name())) {
        afterExecutionActions.add(() -> memptr.write(ex.getSource().read()));
      }

    if (ex.getTarget() instanceof IndirectMemory16BitReference) {
      afterExecutionActions.add(() -> {
        currentRegister = registerSP;
        addMultipleMCRegister(2, 1, 0);
      });
    }

    if (!afterExecutionActions.isEmpty()) {
      Runnable runnable = computeActions(afterExecutionActions);
      phase.acceptAfterExecution((e) -> runnable.run());
    }

    if (ex.getTarget() instanceof IndirectMemory16BitReference)
      phase.acceptBeforeWrite((e) -> {
        if (writeCount == 0)
          addMultipleMc(1, 1, 1, registerSP, null);
      });
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
    Register register;
    int times;

    if (instruction instanceof Outdr || instruction instanceof Outir) {
      register = registerBC;
      delta = 0;
      times = 0;
    } else if (instruction instanceof Inir) {
      delta = -1;
      register = this.registerHL;
      times = 0;
    } else if (instruction instanceof Indr) {
      delta = 1;
      register = this.registerHL;
      times = 0;
    } else if (instruction instanceof Cpir) {
      delta = -1;
      register = this.registerHL;
      times = 5;
    } else if (instruction instanceof Cpdr) {
      delta = 1;
      register = this.registerHL;
      times = 5;
    } else if (instruction instanceof Ldir) {
      register = registerDE;
      delta = -1;
      times = 2;
    } else {
      register = registerDE;
      delta = 1;
      times = 2;
    }

    if (times > 0) {
      phase.acceptAfterExecution((a) -> {
        currentRegister = register;
        addMultipleMCRegister(times, 1, delta);
        if (instruction.getNextPC() != -1) {
          addMultipleMCRegister(5, 1, delta);
        }
      });
    } else {
      addMcBeforeExecution(1);
      phase.acceptAfterExecution((a) -> {
        if (instruction.getNextPC() != -1) {
          currentRegister = register;
          addMultipleMCRegister(5, 1, delta);
        }
      });
    }

    return false;
  }

  private void addMultipleMCRegister(int x, int time1, int delta1) {
    addMultipleMc(x, time1, delta1, currentRegister.read(), "contend_write_no_mreq");
  }

  private void addForBlockInstruction(int times, int delta, Register register) {
    phase.acceptAfterExecution(p -> {
      currentRegister = register;
      addMultipleMCRegister(times, 1, delta);
    });
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
        addMultipleMcAddress();
      }
    };
    processTargetInstruction((TargetInstruction) ldOperation.getInstruction(), afterMRPhaseVisitor);
    return true;
  }

  private void addMultipleMcAddress() {
    addMultipleMc(1, 1, 0, address, null);
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
          addMultipleMCPc2(2, 1, 3);
      }));

      phase.acceptAfterMR((e -> {
        if (readCount == 1) {
          addMultipleMcAddress();
        }

        afterMRPhaseVisitor.visit(e);
      }));
    });

    isIndirectHL(instruction).ifPresent((x) -> {
      phase.acceptAfterMR(e -> {
        if (readCount == 1)
          addMultipleMCHL2(1, 1, 0);

        afterMRPhaseVisitor.visit(e);
      });
    });

    return true;
  }

  private void addMultipleMCPc2(int x, int time1, int delta) {
    addMultipleMc(x, time1, delta, registerPC, null);
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
      if (readCount == 1) {
        addMultipleMCPc2(5, 1, 2);
      } else if (readCount == 2) {
        addMultipleMcAddress();
      }
    })));

    isIndirectHL(instruction).ifPresent((x) -> phase.acceptBeforeWrite(e -> addMultipleMCHL2(1, 1, 0)));
  }

  public boolean visitRLD(RLD rld) {
    phase.acceptAfterMR(p -> addMultipleMCHL2(4, 1, 0));
    return false;
  }

  private void addMultipleMCHL2(int x, int time1, int delta) {
    addMultipleMc(x, time1, delta, registerHL, null);
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction instruction) {
    isMemoryPlusOptional(instruction.getSource()).ifPresent(x ->
        phase.acceptAfterMR(e -> {
          if (readCount == 1) addMultipleMCPc2(5, 1, 2);
        }));
  }

  public boolean visitingCall(Call tCall) {
    phase.acceptBeforeWrite(e -> {
      if (writeCount == 0)
        addMc2PC(1, 2);
    });
    return false;
  }

  private void addMcBeforeExecution(final int time) {
    phase.acceptBeforeExecution(beforeExecution -> addMultipleMCIR(time, 1, 0));
  }

  private void addMultipleMCIR(int time, int time1, int delta) {
    addMultipleMc(time, time1, delta, this.registerIR, null);
  }

  private void addMcAfterExecution() {
    phase.acceptAfterExecution(afterExecution -> addMultipleMCIR(2, 1, 0));
  }
}
