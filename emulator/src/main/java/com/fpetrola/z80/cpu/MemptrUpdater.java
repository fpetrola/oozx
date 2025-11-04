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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;

@SuppressWarnings("ALL")
public class MemptrUpdater {
  protected final Register memptr;
  private final Memory memory;

  public MemptrUpdater(Register memptr1, Memory memory1) {
    memptr = memptr1;
    this.memory = memory1;
  }

  public void updateBefore(Instruction instruction) {
    if (instruction != null)
      instruction.accept(new InstructionVisitor<java.lang.Integer>() {
        public boolean visitRLD(RLD rld) {
          Integer wordNumber = rld.getHl().read();
          memptr.write( (Integer) new Integer((wordNumber + 1) & 0xFFFF));
          return false;
        }

        public boolean visitingCall(Call tCall) {
          int jumpAddress2 =  tCall.calculateJumpAddress();
          memptr.write(jumpAddress2);
          return false;
        }

        public boolean visiting16BitsOperation(Binary16BitsOperation binary16BitsOperation) {
          Integer wordNumber = ( binary16BitsOperation.getTarget().read());
          memptr.write( (Integer) new Integer((wordNumber + 1) & 0xFFFF));
          return false;
        }

        public boolean visitIni(Ini tIni) {
          Integer wordNumber = tIni.getBc().read();
          memptr.write( (Integer) new Integer((wordNumber + 1) & 0xFFFF));
          return false;
        }

        public boolean visitInd(Ind tInd) {
          Integer wordNumber = tInd.getBc().read();
          memptr.write( (Integer) new Integer((wordNumber + -1) & 0xFFFF));
          return true;
        }

        public boolean visitRepeatingInstruction(RepeatingInstruction tRepeatingInstruction) {
          if (tRepeatingInstruction instanceof Inir || tRepeatingInstruction instanceof Indr || tRepeatingInstruction instanceof Outir || tRepeatingInstruction instanceof Outdr) {
            tRepeatingInstruction.getInstructionToRepeat().accept(this);
          }
          return false;
        }

        public boolean visitCpi(Cpi cpi) {
          memptr.increment();
          return false;
        }

        public boolean visitCpd(Cpd cpd) {
          memptr.decrement();
          return true;
        }

        public void visitIn(In tOut) {
          tOut.getSource().accept(new InstructionVisitor<>() {
            public boolean visitRegister(Register register) {
              Integer wordNumber = ( tOut.getSource().read());
              memptr.write( (Integer) new Integer((wordNumber + 1) & 0xFFFF));
              return false;
            }

            public boolean visitMemory8BitReference(Memory8BitReference memory8BitReference) {
              Integer wordNumber = tOut.getA().read();
              Integer number = ((Integer) (Integer) new Integer((wordNumber << 8) & 0xFFFF));
              int i = tOut.getSource().read() & 0xFFFF;
              Integer wordNumber1 = ( (Integer) new Integer((number | i) & 0xFFFF));
              memptr.write( (Integer) new Integer((wordNumber1 + 1) & 0xFFFF));
              return false;
            }
          });
        }
      });
  }

  public void updateAfter(Instruction instruction) {
    instruction.accept(new InstructionVisitor<java.lang.Integer>() {

      public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
        memptr.write( memoryPlusRegister8BitReference.address);
      }

      public void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {
        target.accept(this);
      }

      public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
        source.accept(this);
      }

      public boolean visitingBit(BIT bit) {
        return false;
      }

      public boolean visitLdOperation(LdOperation ldOperation) {
        return false;
      }

      public boolean visitOuti(Outi outi) {
        Integer wordNumber = outi.getBc().read();
        memptr.write( (Integer) new Integer((wordNumber + 1) & 0xFFFF));

        return false;
      }

      public boolean visitOutd(Outd outd) {
        Integer wordNumber = outd.getBc().read();
        memptr.write( (Integer) new Integer((wordNumber + -1) & 0xFFFF));

        return true;
      }

      public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
        Integer nextPC =  conditionalInstruction.getNextPC();

        if (conditionalInstruction instanceof Call) {
          nextPC =  conditionalInstruction.getJumpAddress();
        } else if (conditionalInstruction instanceof JP jp) {
          if (!(jp.getPositionOpcodeReference() instanceof Register))
            nextPC =  conditionalInstruction.getJumpAddress();
          else
            nextPC = null;
        }
        memptr.write(nextPC == null ?  new Integer(0) : nextPC);
      }

      public void visitingRst(RST rst) {
        memptr.write( rst.getNextPC());
      }

      public void visitOut(Out tOut) {
        if (tOut.getTarget() instanceof Out.OutPortOpcodeReference outPortOpcodeReference) {
          if (outPortOpcodeReference.target instanceof Register) {
            Integer wordNumber = ( tOut.getTarget().read());
            memptr.write( (Integer) new Integer((wordNumber + 1) & 0xFFFF));
          }
          else if (outPortOpcodeReference.target instanceof Memory8BitReference memory8BitReference) {
            Integer wordNumber = tOut.getSource().read();
            memptr.write( (Integer) new Integer((wordNumber << 8) & 0xFFFF));
            Integer wordNumber2 = tOut.getTarget().read();
            Integer wordNumber1 = (Integer) (Integer) new Integer((wordNumber2 + 1) & 0xFFFF);
            Integer number = memptr.read();
            int i = ( (Integer) new Integer((wordNumber1 & 0xff) & 0xFFFF)) & 0xFFFF;
            int and =  (Integer) new Integer((number | i) & 0xFFFF);
            memptr.write(and);
          }
        }
      }

      public boolean visitRepeatingInstruction(RepeatingInstruction repeatingInstruction) {
        repeatingInstruction.accept(new InstructionVisitor() {
          public boolean visitLddr(Lddr lddr) {
            incIfNextPC(0);
            return false;
          }

          public boolean visitLdir(Ldir ldir) {
            incIfNextPC(0);
            return false;
          }

          public boolean visitCpir(Cpir cpir) {
            incIfNextPC(1);
            return false;
          }

          public boolean visitCpdr(Cpdr tCpdr) {
            incIfNextPC(-1);
            return false;
          }

          private void incIfNextPC(int i) {
            Integer nextPC = repeatingInstruction.getNextPC();
            int newValue;
            if (nextPC != null) {
              newValue =  (Integer) new Integer((nextPC + 1) & 0xFFFF);
            } else {
              Integer wordNumber = memptr.read();
              newValue =  (Integer) new Integer((wordNumber + i) & 0xFFFF);
            }
            memptr.write(newValue);
          }
        });

        repeatingInstruction.getInstructionToRepeat().accept(this);

        return true;
      }
    });
  }
}
