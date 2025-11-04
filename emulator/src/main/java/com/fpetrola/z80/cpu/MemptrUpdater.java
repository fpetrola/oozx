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
public class MemptrUpdater<T extends WordNumber> {
  protected final Register<T> memptr;
  private final Memory<T> memory;

  public MemptrUpdater(Register<T> memptr1, Memory<T> memory1) {
    memptr = memptr1;
    this.memory = memory1;
  }

  public void updateBefore(Instruction<T> instruction) {
    if (instruction != null)
      instruction.accept(new InstructionVisitor<T, Integer>() {
        public boolean visitRLD(RLD<T> rld) {
          WordNumber wordNumber = rld.getHl().read();
          memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + 1) & 0xFFFF));
          return false;
        }

        public boolean visitingCall(Call tCall) {
          T jumpAddress2 = (T) tCall.calculateJumpAddress();
          memptr.write(jumpAddress2);
          return false;
        }

        public boolean visiting16BitsOperation(Binary16BitsOperation<T> binary16BitsOperation) {
          WordNumber wordNumber = ((T) binary16BitsOperation.getTarget().read());
          memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + 1) & 0xFFFF));
          return false;
        }

        public boolean visitIni(Ini<T> tIni) {
          WordNumber wordNumber = tIni.getBc().read();
          memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + 1) & 0xFFFF));
          return false;
        }

        public boolean visitInd(Ind<T> tInd) {
          WordNumber wordNumber = tInd.getBc().read();
          memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + -1) & 0xFFFF));
          return true;
        }

        public boolean visitRepeatingInstruction(RepeatingInstruction<T> tRepeatingInstruction) {
          if (tRepeatingInstruction instanceof Inir || tRepeatingInstruction instanceof Indr<?> || tRepeatingInstruction instanceof Outir<?> || tRepeatingInstruction instanceof Outdr<T>) {
            tRepeatingInstruction.getInstructionToRepeat().accept(this);
          }
          return false;
        }

        public boolean visitCpi(Cpi<T> cpi) {
          memptr.increment();
          return false;
        }

        public boolean visitCpd(Cpd<T> cpd) {
          memptr.decrement();
          return true;
        }

        public void visitIn(In<T> tOut) {
          tOut.getSource().accept(new InstructionVisitor<T, T>() {
            public boolean visitRegister(Register register) {
              WordNumber wordNumber = ((T) tOut.getSource().read());
              memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + 1) & 0xFFFF));
              return false;
            }

            public boolean visitMemory8BitReference(Memory8BitReference<T> memory8BitReference) {
              WordNumber wordNumber = tOut.getA().read();
              WordNumber number = ((WordNumber) WordNumber.<WordNumber>createValue((wordNumber.value << 8) & 0xFFFF));
              int i = tOut.getSource().read().value & 0xFFFF;
              WordNumber wordNumber1 = ((T) WordNumber.<WordNumber>createValue((number.value | i) & 0xFFFF));
              memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber1.value + 1) & 0xFFFF));
              return false;
            }
          });
        }
      });
  }

  public void updateAfter(Instruction<T> instruction) {
    instruction.accept(new InstructionVisitor<T, Integer>() {

      public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
        memptr.write((T) memoryPlusRegister8BitReference.address);
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

      public boolean visitOuti(Outi<T> outi) {
        WordNumber wordNumber = outi.getBc().read();
        memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + 1) & 0xFFFF));

        return false;
      }

      public boolean visitOutd(Outd<T> outd) {
        WordNumber wordNumber = outd.getBc().read();
        memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + -1) & 0xFFFF));

        return true;
      }

      public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
        T nextPC = (T) conditionalInstruction.getNextPC();

        if (conditionalInstruction instanceof Call) {
          nextPC = (T) conditionalInstruction.getJumpAddress();
        } else if (conditionalInstruction instanceof JP jp) {
          if (!(jp.getPositionOpcodeReference() instanceof Register<?>))
            nextPC = (T) conditionalInstruction.getJumpAddress();
          else
            nextPC = null;
        }
        memptr.write(nextPC == null ? WordNumber.createValue(0) : nextPC);
      }

      public void visitingRst(RST<T> rst) {
        memptr.write((T) rst.getNextPC());
      }

      public void visitOut(Out<T> tOut) {
        if (tOut.getTarget() instanceof Out.OutPortOpcodeReference<?> outPortOpcodeReference) {
          if (outPortOpcodeReference.target instanceof Register<?>) {
            WordNumber wordNumber = ((T) tOut.getTarget().read());
            memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value + 1) & 0xFFFF));
          }
          else if (outPortOpcodeReference.target instanceof Memory8BitReference<?> memory8BitReference) {
            WordNumber wordNumber = tOut.getSource().read();
            memptr.write((T) WordNumber.<WordNumber>createValue((wordNumber.value << 8) & 0xFFFF));
            WordNumber wordNumber2 = tOut.getTarget().read();
            WordNumber wordNumber1 = (WordNumber) WordNumber.<WordNumber>createValue((wordNumber2.value + 1) & 0xFFFF);
            WordNumber number = memptr.read();
            int i = ((T) WordNumber.<WordNumber>createValue((wordNumber1.value & 0xff) & 0xFFFF)).value & 0xFFFF;
            T and = (T) WordNumber.<WordNumber>createValue((number.value | i) & 0xFFFF);
            memptr.write(and);
          }
        }
      }

      public boolean visitRepeatingInstruction(RepeatingInstruction<T> repeatingInstruction) {
        repeatingInstruction.accept(new InstructionVisitor<T, T>() {
          public boolean visitLddr(Lddr lddr) {
            incIfNextPC(0);
            return false;
          }

          public boolean visitLdir(Ldir<T> ldir) {
            incIfNextPC(0);
            return false;
          }

          public boolean visitCpir(Cpir<T> cpir) {
            incIfNextPC(1);
            return false;
          }

          public boolean visitCpdr(Cpdr tCpdr) {
            incIfNextPC(-1);
            return false;
          }

          private void incIfNextPC(int i) {
            T nextPC = repeatingInstruction.getNextPC();
            T newValue;
            if (nextPC != null) {
              newValue = (T) WordNumber.<WordNumber>createValue((nextPC.value + 1) & 0xFFFF);
            } else {
              WordNumber wordNumber = memptr.read();
              newValue = (T) WordNumber.<WordNumber>createValue((wordNumber.value + i) & 0xFFFF);
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
