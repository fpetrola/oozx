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
import com.fpetrola.z80.registers.RegisterName;

@SuppressWarnings("ALL")
public class MemptrUpdater<T extends WordNumber> {
  protected final Register<T> memptr;
  private final Memory<T> memory;

  public MemptrUpdater(Register<T> memptr1, Memory<T> memory1) {
    memptr = memptr1;
    this.memory = memory1;
  }

  public void updateBefore(Instruction<T> instruction) {
    memory.canDisable(true);
    memory.disableReadListener();
    instruction.accept(new InstructionVisitor<T, Integer>() {
      public boolean visitingCall(Call tCall) {
        T jumpAddress2 = (T) tCall.calculateJumpAddress();
        memptr.write(jumpAddress2);

        return InstructionVisitor.super.visitingCall(tCall);
      }

      public void visitingAdd16(Add16 tAdd16) {
        memptr.write(((T) tAdd16.getTarget().read()).plus(1));
      }

      public boolean visitIni(Ini<T> tIni) {
        memptr.write(tIni.getBc().read().plus(1));
        return false;
      }

      public boolean visitInd(Ind<T> tInd) {
        memptr.write(tInd.getBc().read().plus(-1));
        return true;
      }

      public boolean visitRepeatingInstruction(RepeatingInstruction<T> tRepeatingInstruction) {
        if (tRepeatingInstruction instanceof Inir || tRepeatingInstruction instanceof Indr<?> || tRepeatingInstruction instanceof Outir<?> || tRepeatingInstruction instanceof Outdr<T>) {
          tRepeatingInstruction.getInstructionToRepeat().accept(this);
        }
        return false;
      }

      public void visitCpi(Cpi<T> cpi) {
        memptr.write(memptr.read().plus(1));
      }

      public boolean visitCpd(Cpd<T> cpd) {
        memptr.write(memptr.read().plus(-1));
        return true;
      }

      public void visitIn(In<T> tOut) {
        tOut.getSource().accept(new InstructionVisitor<T, T>() {
          public boolean visitRegister(Register register) {
            memptr.write(((T) tOut.getSource().read()).plus(1));
            return false;
          }

          public boolean visitMemory8BitReference(Memory8BitReference<T> memory8BitReference) {
            memptr.write((tOut.getA().read().left(8).or(tOut.getSource().read())).plus(1));
            return false;
          }
        });
      }

      public boolean visitingAdc16(Adc16<T> tAdc16) {
        memptr.write(tAdc16.getTarget().read().plus(1));
        return false;
      }

      public void visitingSbc16(Sbc16<T> sbc16) {
        memptr.write(sbc16.getTarget().read().plus(1));
      }

      public boolean visitRLD(RLD<T> rld) {
        memptr.write(rld.getHl().read().plus(1));
        return false;
      }
    });

    memory.enableReadListener();
    memory.canDisable(false);
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
        return InstructionVisitor.super.visitLdOperation(ldOperation);
      }

      public boolean visitOuti(Outi<T> outi) {
        memptr.write(outi.getBc().read().plus(1));

        return InstructionVisitor.super.visitOuti(outi);
      }

      public boolean visitOutd(Outd<T> outd) {
        memptr.write(outd.getBc().read().plus(-1));

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

      public void visitingLd(Ld<T> ld) {
        ImmutableOpcodeReference source = ld.getSource();
        OpcodeReference<T> target = ld.getTarget();

        target.accept(new InstructionVisitor<T, T>() {
          public void visitIndirectMemory8BitReference(IndirectMemory8BitReference<T> indirectMemory8BitReference1) {
            boolean b = indirectMemory8BitReference1.target instanceof Register register && (register.getName().equals("BC") || register.getName().equals("DE"));
            if (b || indirectMemory8BitReference1.getTarget() instanceof Memory16BitReference<T>)
              memptr.write(ld.getSource().read().left(8).or(indirectMemory8BitReference1.address.plus(1).and(0xff)));
          }

          public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
            memptr.write(indirectMemory16BitReference.address.plus(1));
          }

          public void visitMemory16BitReference(Memory16BitReference<T> memory16BitReference) {
            memptr.write(memory16BitReference.fetchedAddress.plus(2));
          }
        });
        source.accept(new InstructionVisitor<T, T>() {
          public void visitIndirectMemory8BitReference(IndirectMemory8BitReference<T> indirectMemory8BitReference) {
            boolean b = indirectMemory8BitReference.target instanceof Register register && (register.getName().equals("BC") || register.getName().equals("DE"));
            if (b || indirectMemory8BitReference.getTarget() instanceof Memory16BitReference<?>)
              memptr.write(((T) indirectMemory8BitReference.address).plus(1));
          }

          public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
            memptr.write(indirectMemory16BitReference.address.plus(1));
          }
        });
      }

      public void visitingRst(RST rst) {
        memptr.write((T) rst.getNextPC());
      }

      public void visitOut(Out<T> tOut) {
        if (tOut.getTarget() instanceof Out.OutPortOpcodeReference<?> outPortOpcodeReference) {
          if (outPortOpcodeReference.target instanceof Register<?>)
            memptr.write(((T) tOut.getTarget().read()).plus(1));
          if (outPortOpcodeReference.target instanceof Memory8BitReference<?> memory8BitReference) {
            memptr.write(tOut.getSource().read().left(8));
            T and = memptr.read().or(tOut.getTarget().read().plus(1).and(0xff));
            memptr.write(and);
          }
        }
      }

      public boolean visitRepeatingInstruction(RepeatingInstruction<T> tRepeatingInstruction) {
        boolean isCpdr = tRepeatingInstruction instanceof Cpdr;
        boolean isCpir = tRepeatingInstruction instanceof Cpir;
        if (tRepeatingInstruction instanceof Ldir || tRepeatingInstruction instanceof Lddr<?> || isCpdr || isCpir) {
          T nextPC = tRepeatingInstruction.getNextPC();
          if (nextPC != null) {
            memptr.write(nextPC.plus(1));
          } else {
            if (isCpdr) {
              memptr.write(memptr.read().plus(-1));
            } else if (isCpir) {
              memptr.write(memptr.read().plus(1));
            }
            tRepeatingInstruction.getInstructionToRepeat().accept(this);
          }
        } else
          tRepeatingInstruction.getInstructionToRepeat().accept(this);
        return true;
      }

      public void visitEx(Ex<T> ex) {
        if (ex.getTarget() instanceof IndirectMemory16BitReference indirectMemory16BitReference)
          if (indirectMemory16BitReference.target instanceof Register<?> register && register.getName().equals(RegisterName.SP.name())) {
            memptr.write(ex.getSource().read());
          }
      }
    });
  }
}
