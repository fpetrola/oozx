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
      phase.acceptBeforeExecution((e) -> addMultipleMCIR(2));
    else if (ld.getTarget().equals(registerI) || ld.getTarget().equals(registerR) || ld instanceof LdAI || ld instanceof LdAR)
      phase.acceptBeforeExecution((e) -> addMultipleMCIR(1));

    if (isMemoryPlus(ld.getTarget()) && isMemory8BitReference(ld.getSource()))
      phase.acceptBeforeWrite((e) -> addMc2PC(2, 3));

    if (!isMemory8BitReference(ld.getSource()) && (isMemoryPlus(ld.getSource()) || isMemoryPlus(ld.getTarget())))
      phase.acceptAfterMR((e) -> {
        if (readCount == 1)
          addMultipleMCIR(5);
      });
  }

  private void addMc2PC(int times, int delta) {
    addMultipleMCPc2(times, delta);
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
    phase.acceptBeforeExecution((e) -> addMultipleMCIR(1));
    phase.acceptAfterExecution((e) -> addForRelativeJump(djnz));
    return false;
  }

  private void addForRelativeJump(JumpInstruction conditionalInstruction) {
    if (conditionalInstruction.getNextPC() != -1) {
      addMultipleMCPc2(5, 1);
    } else {
      addMultipleMCPC3();
    }
  }

  private void addMultipleMCPC3() {
    addMultipleMc(1, 3, 1, registerPC, "readbyte");
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
        addMultipleMCRegister(2, 0);
      });
    }

    if (!afterExecutionActions.isEmpty()) {
      Runnable runnable = computeActions(afterExecutionActions);
      phase.acceptAfterExecution((e) -> runnable.run());
    }

    if (ex.getTarget() instanceof IndirectMemory16BitReference)
      phase.acceptBeforeWrite((e) -> {
        if (writeCount == 0) {
          currentRegister = registerSP;
          addMultipleMcRegister();
        }
      });
  }

  private void addMultipleMcRegister() {
    addMultipleMc(1, 1, 1, currentRegister, null);
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
    int times;

    if (instruction instanceof Outdr || instruction instanceof Outir) {
      currentRegister = registerBC;
      delta = 0;
      times = 0;
    } else if (instruction instanceof Inir) {
      delta = -1;
      currentRegister = this.registerHL;
      times = 0;
    } else if (instruction instanceof Indr) {
      delta = 1;
      currentRegister = this.registerHL;
      times = 0;
    } else if (instruction instanceof Cpir) {
      delta = -1;
      currentRegister = this.registerHL;
      times = 5;
    } else if (instruction instanceof Cpdr) {
      delta = 1;
      currentRegister = this.registerHL;
      times = 5;
    } else if (instruction instanceof Ldir) {
      currentRegister = registerDE;
      delta = -1;
      times = 2;
    } else {
      currentRegister = registerDE;
      delta = 1;
      times = 2;
    }

    if (times > 0) {
      phase.acceptAfterExecution((a) -> {
        addMultipleMCRegister(times, delta);
        if (instruction.getNextPC() != -1) {
          addMultipleMCRegister(5, delta);
        }
      });
    } else {
      addMcBeforeExecution(1);
      phase.acceptAfterExecution((a) -> {
        if (instruction.getNextPC() != -1) {
          addMultipleMCRegister(5, delta);
        }
      });
    }

    return false;
  }

  private void addMultipleMCRegister(int x, int delta1) {
    addMultipleMc(x, 1, delta1, currentRegister, "contend_write_no_mreq");
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    addMcBeforeExecution(1);
  }

  public boolean visitLdi(Ldi ldi) {
    phase.acceptAfterExecution(p -> {
      currentRegister = registerDE;
      addMultipleMCRegister(2, -1);
    });
    return true;
  }

  public boolean visitLdd(Ldd ldd) {
    phase.acceptAfterExecution(p -> {
      currentRegister = registerDE;
      addMultipleMCRegister(2, 1);
    });
    return true;
  }

  public boolean visitCpi(Cpi cpi) {
    phase.acceptAfterExecution(p -> {
      currentRegister = registerHL;
      addMultipleMCRegister(5, -1);
    });
    return true;
  }

  public boolean visitCpd(Cpd cpd) {
    phase.acceptAfterExecution(p -> {
      currentRegister = registerHL;
      addMultipleMCRegister(5, 1);
    });
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
          addMultipleMCPc2(2, 3);
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
          addMultipleMCHL2(1);

        afterMRPhaseVisitor.visit(e);
      });
    });

    return true;
  }

  private void addMultipleMCPc2(int x, int delta) {
    addMultipleMc(x, 1, delta, registerPC, null);
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
        addMultipleMCPc2(5, 2);
      } else if (readCount == 2) {
        addMultipleMcAddress();
      }
    })));

    isIndirectHL(instruction).ifPresent((x) -> phase.acceptBeforeWrite(e -> addMultipleMCHL2(1)));
  }

  public boolean visitRLD(RLD rld) {
    phase.acceptAfterMR(p -> addMultipleMCHL2(4));
    return false;
  }

  private void addMultipleMCHL2(int x) {
    addMultipleMc(x, 1, 0, registerHL, null);
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction instruction) {
    isMemoryPlusOptional(instruction.getSource()).ifPresent(x ->
        phase.acceptAfterMR(e -> {
          if (readCount == 1) addMultipleMCPc2(5, 2);
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
    phase.acceptBeforeExecution(beforeExecution -> addMultipleMCIR(time));
  }

  private void addMultipleMCIR(int time) {
    addMultipleMc(time, 1, 0, this.registerIR, null);
  }

  private void addMcAfterExecution() {
    phase.acceptAfterExecution(afterExecution -> addMultipleMCIR(2));
  }
}
