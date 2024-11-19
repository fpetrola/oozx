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
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.*;

import java.util.Optional;

import static com.fpetrola.z80.registers.RegisterName.*;
import static com.fpetrola.z80.registers.RegisterName.PC;

public class PhaseProcessor<T extends WordNumber> extends PhaseProcessorBase<T> {
  public PhaseProcessor(Z80Cpu<T> cpu) {
    super(cpu);
  }

  public void visitingRst(RST rst) {
    addResultAfterFetch(1);
  }

  public boolean visitingRet(Ret ret) {
    if (!(ret.getCondition() instanceof ConditionAlwaysTrue))
      addResultAfterFetch(1);
    return false;
  }

  public boolean visitingAdc16(Adc16<T> tAdc16) {
    addResultAfterFetch(7);
    return false;
  }

  public void visitingAdd16(Add16 tAdd16) {
    addResultAfterFetch(7);
  }

  private void addResultAfterFetch(final int time) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        addMc(time, IR, 0);
      }
    });
  }

  public void visitingInc16(Inc16 tInc16) {
    phase.acceptAfterExecution(afterExecution -> addMc(2, IR, 0));
  }

  public void visitingSbc16(Sbc16<T> sbc16) {
    addResultAfterFetch(7);
  }

  public void visitingDec16(Dec16 tDec16) {
    phase.acceptAfterExecution(afterExecution -> addMc(2, IR, 0));
  }

  public void visitPush(Push push) {
    addResultAfterFetch(1);
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    if (blockInstruction instanceof Ini || blockInstruction instanceof Outi)
      addResultAfterFetch(1);
  }

  int lastAddress;

  public boolean visitRepeatingInstruction(RepeatingInstruction<T> instruction) {
    DefaultPhaseVisitor visitor = new DefaultPhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        lastAddress = instruction instanceof Ldir<T> || instruction instanceof Lddr<T> ? getRegister(DE).read().intValue() : getRegister(HL).read().intValue();
      }

      public void visit(AfterExecution afterExecution) {
        if (instruction instanceof Outdr<T> || instruction instanceof Outir<T>)
          lastAddress = getRegister(BC).read().intValue();
        if (instruction.getNextPC() != null)
          addMultipleMc(5, 1, 0, lastAddress);
      }
    };
    instruction.getInstructionToRepeat().accept(PhaseProcessor.this);
    phase.accept(visitor);
    return false;
  }

  public void visitingLd(Ld<T> ld) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        if (isLdSP(ld))
          addMc(2, IR, 0);
      }

      public void visit(AfterMR afterMR) {
        matchesTstate(11).ifPresent(x -> {
          if (!ld.getTarget().equals(getRegister(SP)) || ld.getSource() instanceof Register<T>) {
            addMultipleMc(5, 1, 0, getRegister(IR).read().intValue());
          }
        });
      }
    });
  }

  public void visitingJR(JR jr) {
    phase.acceptAfterExecution(afterExecution -> addForRelativeJump(jr, HL, IR, 0));
  }

  public boolean visitingDjnz(DJNZ<T> djnz) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        addMc(1, IR, 0);
      }

      public void visit(AfterExecution afterExecution) {
        addForRelativeJump(djnz, PC, PC, 1);
      }
    });
    return false;
  }

  private void addForRelativeJump(ConditionalInstruction<T, ?> conditionalInstruction, RegisterName registerName, RegisterName registerName1, int delta) {
    if (conditionalInstruction.getNextPC() != null) {
      addMultipleMc(5, 1, 1, getRegister(registerName).read().intValue());
    } else {
      addMultipleMc(1, 1, delta, getRegister(registerName1).read().intValue());
      getState().tstates += 2;
    }
  }

  public void visitEx(Ex<T> ex) {
    if (ex.getTarget() instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
      phase.accept(new DefaultPhaseVisitor() {
        public void visit(AfterExecution afterExecution) {
          addMc(2, SP, 0);
        }

        public void visit(BeforeWrite beforeWrite) {
          int i = ex.getSource().equals(getRegister(HL)) && indirectMemory16BitReference.target.equals(getRegister(SP)) ? 10 : 14;
          matchesTstate(i).ifPresent(x -> addMc(1, SP, 1));
        }
      });
    }
  }

  public boolean visitingBit(BIT<T> bit) {
    phase.acceptAfterExecution(afterExecution -> isIndirectHL(bit).ifPresent(x -> addMc(1, HL, 0)));
    return false;
  }

  public boolean visitOuti(Outi<T> outi) {
    addIfNextPC(outi);
    return false;
  }

  public boolean visitIni(Ini<T> tIni) {
    addIfNextPC(tIni);
    return false;
  }

  private void addIfNextPC(BlockInstruction<T> tIni) {
    phase.acceptAfterExecution(afterExecution -> {
      if (tIni.getNextPC() != null)
        addMc(5, BC, 0);
    });
  }

  public boolean visitLdd(Ldd ldd) {
    phase.acceptAfterExecution(p -> addMc(2, DE, 1));
    return true;
  }

  public void visitLdi(Ldi<T> tLdi) {
    phase.acceptAfterExecution(p -> addMc(2, DE, -1));
  }

  public void visitCpi(Cpi<T> cpi) {
    phase.acceptAfterExecution(p -> addMc(5, HL, -1));
  }

  public boolean visitCpd(Cpd<T> cpd) {
    phase.acceptAfterExecution(p -> addMc(5, HL, 1));
    return true;
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    phase.acceptAfterMR(p -> addMc14Or19(address));
    return false;
  }

  public void visitingBitOperation(BitOperation tBitOperation) {
    phase.acceptAfterMR(p -> {
      if (!(tBitOperation instanceof RES<?> || tBitOperation instanceof SET<?>) || !addIfIndirectHL(tBitOperation))
        addMc14Or19(address);
    });
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {
    phase.acceptAfterMR(p -> {
      addIfIndirectHL(parameterizedUnaryAluInstruction);
      addMc14Or19(address);
    });

    return false;
  }

  public boolean addIfIndirectHL(TargetInstruction<T> targetInstruction) {
    isIndirectHL(targetInstruction).ifPresent((x) -> matchesTstate(11).ifPresent(x2 -> addMultipleMc(1, 1, 0, getRegister(HL).read().intValue())));
    return false;
  }

  public boolean visitingInc(Inc<T> tInc) {
    addDecInc(tInc);
    return super.visitingInc(tInc);
  }

  public boolean visitingDec(Dec<T> dec) {
    addDecInc(dec);
    return super.visitingDec(dec);
  }

  private void addDecInc(TargetInstruction<T> dec) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(AfterMR afterExecution) {
        if (dec.getTarget() instanceof MemoryPlusRegister8BitReference<T>) {
          matchesTstate(11).ifPresent(x -> addMultipleMc(5, 1, 2, getRegister(PC).read().intValue()));
        }
      }

      public void visit(BeforeWrite beforeWrite) {
        isIndirectHL(dec).ifPresent(x -> addMc(1, HL, 0));
      }
    });
  }

  public boolean visitRLD(RLD<T> rld) {
    phase.acceptAfterMR(p -> matchesTstate(11).ifPresent(x -> addMultipleMc(4, 1, 0, getRegister(HL).read().intValue())));
    return false;
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction parameterizedBinaryAluInstruction) {
    phase.acceptAfterMR(p -> matchesTstate(11).ifPresent(x -> addMultipleMc(5, 1, 0, getRegister(IR).read().intValue())));
  }

  private Optional<Boolean> matchesTstate(int i) {
    return Optional.ofNullable(getState().tstates == i ? true : null);
  }


  public boolean visitingCall(Call tCall) {
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(BeforeWrite beforeWrite) {
        matchesTstate(10).ifPresent(x -> addMc(1, IR, 1));
      }
    });

    return super.visitingCall(tCall);
  }
}
