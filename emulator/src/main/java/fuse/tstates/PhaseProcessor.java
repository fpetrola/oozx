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

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.*;

import static com.fpetrola.z80.registers.RegisterName.*;
import static com.fpetrola.z80.registers.RegisterName.PC;

public class PhaseProcessor<T extends WordNumber> extends PhaseProcessorBase<T> {
  int lastAddress;

  public PhaseProcessor(Z80Cpu<T> cpu) {
    super(cpu);
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
    return addMcBeforeExecution(7);
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

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    if (blockInstruction instanceof Ini || blockInstruction instanceof Outi)
      addMcBeforeExecution(1);
  }

  public boolean visitRepeatingInstruction(RepeatingInstruction<T> instruction) {
    PhaseVisitor visitor = new DefaultPhaseVisitor() {
      public void visit(BeforeExecution afterFetch) {
        lastAddress = getRegister(instruction instanceof Ldir<T> || instruction instanceof Lddr<T> ? DE : HL).read().intValue();
      }

      public void visit(AfterExecution afterExecution) {
        if (instruction instanceof Outdr<T> || instruction instanceof Outir<T>)
          lastAddress = getRegister(BC).read().intValue();
        if (instruction.getNextPC() != null)
          addMultipleMc(5, 1, 0, lastAddress, "contend_write_no_mreq");
      }
    };
    instruction.getInstructionToRepeat().accept(PhaseProcessor.this);
    phase.accept(visitor);
    return false;
  }

  public void visitingLd(Ld<T> ld) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(BeforeExecution beforeExecution) {
        if (isLdSP(ld))
          addMc(2, IR, 0, null);
        else if (ld.getTarget().equals(getRegister(I)) || ld.getTarget().equals(getRegister(R)) || ld instanceof LdAI<T> || ld instanceof LdAR<T>)
          addMc(1, IR, 0, null);
      }

      public void visit(BeforeWrite beforeWrite) {
        if (isMemoryPlus(ld.getTarget()) && isMemory8BitReference(ld.getSource())) {
          addMc(2, PC, 3, null);
        }
      }

      public void visit(AfterMR afterMR) {
        if (!isMemory8BitReference(ld.getSource()) && (isMemoryPlus(ld.getSource()) || isMemoryPlus(ld.getTarget()))) {
          if (readCount == 1)
            addMultipleMc(5, 1, 0, getRegister(IR).read().intValue(), null);
        }
      }
    });
  }

  public void visitingJR(JR jr) {
    phase.acceptAfterExecution(afterExecution -> addForRelativeJump(jr));
  }

  public boolean visitingDjnz(DJNZ<T> djnz) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(BeforeExecution beforeExecution) {
        addMc(1, IR, 0, null);
      }

      public void visit(AfterExecution afterExecution) {
        addForRelativeJump(djnz);
      }
    });
    return false;
  }

  private void addForRelativeJump(ConditionalInstruction<T, ?> conditionalInstruction) {
    int baseAddress = getRegister(PC).read().intValue();
    if (conditionalInstruction.getNextPC() != null) {
      addMultipleMc(5, 1, 1, baseAddress, null);
    } else {
      addMultipleMc(1, 3, 1, baseAddress, "readbyte");
    }
  }

  public void visitEx(Ex<T> ex) {
    if (ex.getTarget() instanceof IndirectMemory16BitReference<T>) {
      phase.accept(new DefaultPhaseVisitor() {
        public void visit(AfterExecution afterExecution) {
          addMc(2, SP, 0, "contend_write_no_mreq");
        }

        public void visit(BeforeWrite beforeWrite) {
          if (writeCount == 0)
            addMc(1, SP, 1, null);
        }
      });
    }
  }

  public boolean visitOuti(Outi<T> outi) {
    addIfNextPC(outi);
    return false;
  }

  public boolean visitIni(Ini<T> tIni) {
    addIfNextPC(tIni);
    return false;
  }

  private void addIfNextPC(BlockInstruction<T> blockInstruction) {
    phase.acceptAfterExecution(afterExecution -> {
      if (blockInstruction.getNextPC() != null)
        addMc(5, BC, 0, null);
    });
  }

  private boolean addForBlockInstruction(int times, RegisterName registerName, int delta) {
    phase.acceptAfterExecution(p -> addMc(times, registerName, delta, "contend_write_no_mreq"));
    return true;
  }

  public void visitLdi(Ldi<T> ldi) {
    addForBlockInstruction(2, DE, -1);
  }

  public boolean visitLdd(Ldd<T> ldd) {
    return addForBlockInstruction(2, DE, 1);
  }

  public void visitCpi(Cpi<T> cpi) {
    addForBlockInstruction(5, HL, -1);
  }

  public boolean visitCpd(Cpd<T> cpd) {
    return addForBlockInstruction(5, HL, 1);
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    phase.acceptAfterMR(p -> {
      if (readCount == 4)
        addMultipleMc(1, 1, 0, address.intValue(), null);
    });
    return false;
  }

  public boolean visitingBitOperation(BitOperation<T> instruction) {
    return addMcForTargetFlagInstruction(instruction);
  }

  private boolean addMcForTargetFlagInstruction(DefaultTargetFlagInstruction<T> instruction1) {
    phase.acceptAfterMR(p -> {
      switch (readCount) {
        case 1 ->
            isIndirectHL(instruction1).ifPresent((x) -> addMultipleMc(1, 1, 0, getRegister(HL).read().intValue(), null));
        case 2 ->
            isMemoryPlusOptional(instruction1.getTarget()).ifPresent(x -> addMultipleMc(2, 1, 3, getRegister(PC).read().intValue(), null));
        case 3 ->
            isMemoryPlusOptional(instruction1.getTarget()).ifPresent(x -> addMultipleMc(1, 1, 0, address.intValue(), null));
      }
    });

    return true;
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction<T> instruction) {
    return addMcForTargetFlagInstruction(instruction);
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
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(AfterMR afterExecution) {
        isMemoryPlusOptional(instruction.getTarget()).ifPresent(x -> {
          switch (readCount) {
            case 1 -> addMultipleMc(5, 1, 2, getRegister(PC).read().intValue(), null);
            case 2 -> addMultipleMc(1, 1, 0, address.intValue(), null);
          }
        });
      }

      public void visit(BeforeWrite beforeWrite) {
        isIndirectHL(instruction).ifPresent(x -> addMc(1, HL, 0, null));
      }
    });
  }

  public boolean visitRLD(RLD<T> rld) {
    phase.acceptAfterMR(p -> addMultipleMc(4, 1, 0, getRegister(HL).read().intValue(), null));
    return false;
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction<T> instruction) {
    phase.acceptAfterMR(p -> isMemoryPlusOptional(instruction.getSource()).ifPresent(x -> {
      if (readCount == 1)
        addMultipleMc(5, 1, 0, getRegister(IR).read().intValue(), null);
    }));
  }

  public boolean visitingCall(Call tCall) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(BeforeWrite beforeWrite) {
        if (writeCount == 0)
          addMc(1, PC, 2, null);
      }
    });

    return super.visitingCall(tCall);
  }

  private boolean addMcBeforeExecution(final int time) {
    phase.acceptBeforeExecution(beforeExecution -> addMc(time, IR, 0, null));
    return true;
  }

  private void addMcAfterExecution() {
    phase.acceptAfterExecution(afterExecution -> addMc(2, IR, 0, null));
  }
}
