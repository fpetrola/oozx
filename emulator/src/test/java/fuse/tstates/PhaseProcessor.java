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
    phase.accept(new PhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        addMc(time, IR, 0);
      }
    });
  }

  public void visitingInc16(Inc16 tInc16) {
    addResultAfterExecution(IR, 0, 2);
  }

  public void visitingSbc16(Sbc16<T> sbc16) {
    addResultAfterFetch(7);
  }

  public void visitingDec16(Dec16 tDec16) {
    addResultAfterExecution(IR, 0, 2);
  }

  public void visitPush(Push push) {
    addResultAfterFetch(1);
  }

  public void visitBlockInstruction(BlockInstruction blockInstruction) {
    if (blockInstruction instanceof Ini || blockInstruction instanceof Outi)
      addResultAfterFetch(1);
  }

  public boolean visitRepeatingInstruction(RepeatingInstruction<T> instruction) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        if (instruction instanceof Inir ||
            instruction instanceof Indr ||
            instruction instanceof Outir ||
            instruction instanceof Outdr
        )
          addMc(1, IR, 0);
      }

      public void visit(AfterExecution afterExecution) {
        if (instruction instanceof Inir<T>) {
          addResultIfNextPC(instruction, HL, -1);
        } else if (instruction instanceof Indr<T>) {
          addResultIfNextPC(instruction, HL, 1);
        } else if (instruction instanceof Outdr<T> || instruction instanceof Outir<T>) {
          addResultIfNextPC(instruction, BC, 0);
        } else if (instruction instanceof Ldir<T>) {
          addRepeatingInstructionResult(instruction, DE, -1, 2);
        } else if (instruction instanceof Lddr<T>) {
          addRepeatingInstructionResult(instruction, DE, 1, 2);
        } else if (instruction instanceof Cpir<T>) {
          addRepeatingInstructionResult(instruction, HL, -1, 5);
        } else if (instruction instanceof Cpdr<T>) {
          addRepeatingInstructionResult(instruction, HL, 1, 5);
        }
      }

      private void addRepeatingInstructionResult(RepeatingInstruction<T> tRepeatingInstruction, RegisterName registerName, int delta, int time) {
        addMc(time, registerName, delta);
        addResultIfNextPC(tRepeatingInstruction, registerName, delta);
      }

      private void addResultIfNextPC(RepeatingInstruction<T> tRepeatingInstruction, RegisterName registerName, int delta) {
        if (tRepeatingInstruction.getNextPC() != null) {
          addMc(5, registerName, delta);
        }
      }
    });
    return super.visitRepeatingInstruction(instruction);
  }

  public void visitingLd(Ld<T> ld) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        if (isLdSP(ld))
          addMc(2, IR, 0);
      }

      public void visit(AfterMR afterMR) {
        if (getState().tstates == 11)
          if (!ld.getTarget().equals(getRegister(SP)) || ld.getSource() instanceof Register<T>) {
            addMultipleMc(5, 1, 0, getRegister(IR).read().intValue());
          }
      }
    });
  }

  public void visitingJR(JR jr) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterExecution afterExecution) {
        addForRelativeJump(jr, HL, IR, 0);
      }
    });
  }

  private void addForRelativeJump(ConditionalInstruction<T, ?> jr, RegisterName registerName, RegisterName registerName1, int delta) {
    if (jr.getNextPC() != null) {
      addMultipleMc(5, 1, 1, getRegister(registerName).read().intValue());
    } else {
      addMultipleMc(1, 1, delta, getRegister(registerName1).read().intValue());
      getState().tstates += 2;
    }
  }

  @Override
  public boolean visitingDjnz(DJNZ<T> djnz) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterFetch afterFetch) {
        addMc(1, IR, 0);
      }

      public void visit(AfterExecution afterExecution) {
        addForRelativeJump(djnz, PC, PC, 1);
      }
    });
    return false;
  }


  public void visitEx(Ex<T> ex) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterExecution afterExecution) {
        if (ex.getTarget() instanceof IndirectMemory16BitReference<T>)
          addMc(2, SP, 0);
      }

      public void visit(BeforeWrite beforeWrite) {
        if (ex.getTarget() instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference) {
          int i = ex.getSource().equals(getRegister(HL)) && indirectMemory16BitReference.target.equals(getRegister(SP)) ? 10 : 14;
          if (getState().tstates == i)
            addMc(1, SP, 1);
        }
      }
    });

  }

  public boolean visitingBit(BIT bit) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterExecution afterExecution) {
        if (isIndirectHL(bit))
          addMc(1, HL, 0);
      }
    });

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
    phase.accept(new PhaseVisitor() {
      public void visit(AfterExecution afterExecution) {
        if (tIni.getNextPC() != null)
          addMc(5, IR, 0);
      }
    });
  }

  public boolean visitLdd(Ldd ldd) {
    addResultAfterExecution(DE, 1, 2);
    return true;
  }

  private void addResultAfterExecution(final RegisterName registerName, final int delta, final int time) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterExecution afterExecution) {
        addMc(time, registerName, delta);
      }
    });
  }

  public void visitLdi(Ldi<T> tLdi) {
    addResultAfterExecution(DE, -1, 2);
  }

  public void visitCpi(Cpi<T> cpi) {
    addResultAfterExecution(HL, -1, 5);
  }

  public boolean visitCpd(Cpd<T> cpd) {
    addResultAfterExecution(HL, 1, 5);
    return true;
  }


  public boolean visitRLD(RLD<T> rld) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterMR afterMR) {
        if (getState().tstates == 11) {
          addMultipleMc(4, 1, 0, getRegister(HL).read().intValue());
        }
      }
    });

    return super.visitRLD(rld);
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterMR afterMR) {
        addMc14Or19(address);
      }
    });
    return super.visitLdOperation(ldOperation);
  }

  public void visitingBitOperation(BitOperation tBitOperation) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterMR afterMR) {
        if (!(tBitOperation instanceof RES<?> || tBitOperation instanceof SET<?>) || !addIfIndirectHL(tBitOperation))
          addMc14Or19(address);
      }
    });
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterMR afterMR) {
        addIfIndirectHL(parameterizedUnaryAluInstruction);
        addMc14Or19(address);
      }
    });

    return super.visitingParameterizedUnaryAluInstruction(parameterizedUnaryAluInstruction);
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
    phase.accept(new PhaseVisitor() {
      public void visit(AfterMR afterExecution) {
        if (getState().tstates == 11)
          if (dec.getTarget() instanceof MemoryPlusRegister8BitReference<T>) {
            addMultipleMc(5, 1, 2, getRegister(PC).read().intValue());
          }
      }

      public void visit(BeforeWrite beforeWrite) {
        if (isIndirectHL(dec))
          addMc(1, HL, 0);
      }
    });
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction parameterizedBinaryAluInstruction) {
    phase.accept(new PhaseVisitor() {
      public void visit(AfterMR afterExecution) {
        if (getState().tstates == 11)
          addMultipleMc(5, 1, 0, getRegister(IR).read().intValue());
      }
    });
  }


  public boolean visitingCall(Call tCall) {
    phase.accept(new PhaseVisitor() {
      public void visit(BeforeWrite beforeWrite) {
        if (getState().tstates == 10)
          addMc(1, IR, 1);
      }
    });

    return super.visitingCall(tCall);
  }
}
