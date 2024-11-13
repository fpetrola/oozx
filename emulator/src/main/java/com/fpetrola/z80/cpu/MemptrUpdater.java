/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
      instruction.accept(before);
  }

  public void updateAfter(Instruction instruction) {
    instruction.accept(after);
  }

  /** Made once each: a visitor here has nothing of its own, only the register it writes. */
  private final InstructionVisitor<java.lang.Integer> before = new Before();
  private final InstructionVisitor<java.lang.Integer> after = new After();

  private class Before implements InstructionVisitor<java.lang.Integer> {
    public boolean visitRLD(RLD rld) {
      memptr.write((rld.getHl().read() + 1) & 0xFFFF);
      return false;
    }

    public boolean visitingCall(Call tCall) {
      memptr.write(tCall.calculateJumpAddress());
      return false;
    }

    public boolean visiting16BitsOperation(Binary16BitsOperation binary16BitsOperation) {
      memptr.write(((binary16BitsOperation.getTarget().read()) + 1) & 0xFFFF);
      return false;
    }

    public boolean visitIni(Ini tIni) {
      memptr.write((tIni.getBc().read() + 1) & 0xFFFF);
      return false;
    }

    public boolean visitInd(Ind tInd) {
      memptr.write((tInd.getBc().read() + -1) & 0xFFFF);
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

    /**
     * One past the port the IN read, whichever way it was written. The instruction says what that
     * port is; asking it beats a second visitor over the source, which closed over the instruction
     * and so was an object per IN - one the JIT usually took apart again and, about one run in
     * five, did not, which is the whole of what CostRegressionTest was reading as 12 bytes an
     * access and 192 a frame.
     */
    public void visitIn(In in) {
      memptr.write((in.port() + 1) & 0xFFFF);
    }
  }

  private class After implements InstructionVisitor<java.lang.Integer> {

    public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
      memptr.write(memoryPlusRegister8BitReference.address);
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

    public void visitingLd(Ld ld) {
      if (ld.getTarget() instanceof IndirectMemory8BitReference indirect && throughBcDeOrNn(indirect))
        memptr.write(((ld.getSource().read() << 8) | ((indirect.address + 1) & 0xff)) & 0xFFFF);
      else if (ld.getTarget() instanceof IndirectMemory16BitReference indirect)
        memptr.write((indirect.address + 1) & 0xFFFF);
      if (ld.getSource() instanceof IndirectMemory8BitReference indirect && throughBcDeOrNn(indirect))
        memptr.write((indirect.address + 1) & 0xFFFF);
      else if (ld.getSource() instanceof IndirectMemory16BitReference indirect)
        memptr.write((indirect.address + 1) & 0xFFFF);
    }

    private boolean throughBcDeOrNn(IndirectMemory8BitReference indirect) {
      return indirect.getTarget() instanceof Register register && (register.getName().equals("BC") || register.getName().equals("DE"))
          || indirect.getTarget() instanceof Memory16BitReference;
    }

    public void visitEx(Ex ex) {
      if (ex.getTarget() instanceof IndirectMemory16BitReference indirect && indirect.getTarget() instanceof Register register && register.getName().equals("SP"))
        memptr.write(ex.getSource().read());
    }

    public boolean visitOuti(Outi outi) {
      memptr.write((outi.getBc().read() + 1) & 0xFFFF);

      return false;
    }

    public boolean visitOutd(Outd outd) {
      memptr.write((outd.getBc().read() + -1) & 0xFFFF);

      return true;
    }

    public void visitingConditionalInstruction(ConditionalInstruction conditionalInstruction) {
      int nextPC = conditionalInstruction.getNextPC();

      if (conditionalInstruction instanceof Call) {
        nextPC = conditionalInstruction.getJumpAddress();
      } else if (conditionalInstruction instanceof JP jp) {
        if (!(jp.getPositionOpcodeReference() instanceof Register))
          nextPC = conditionalInstruction.getJumpAddress();
        else
          nextPC = -1;
      }
      memptr.write(nextPC == -1 ? 0 : nextPC);
    }

    public void visitingRst(RST rst) {
      memptr.write(rst.getNextPC());
    }

    public void visitOut(Out tOut) {
      if (tOut.getTarget() instanceof Out.OutPortOpcodeReference outPortOpcodeReference) {
        if (outPortOpcodeReference.target instanceof Register) {
          memptr.write(((tOut.getTarget().read()) + 1) & 0xFFFF);
        } else if (outPortOpcodeReference.target instanceof Memory8BitReference memory8BitReference) {
          memptr.write((tOut.getSource().read() << 8));
          memptr.write((memptr.read() | ((tOut.getTarget().read() + 1) & 0xff)));
        }
      }
    }

    public boolean visitRepeatingInstruction(RepeatingInstruction repeatingInstruction) {
      repeatingInstruction.accept(repeating);
      repeatingInstruction.getInstructionToRepeat().accept(this);
      return true;
    }
  }

  private final InstructionVisitor<java.lang.Integer> repeating = new Repeating();

  private class Repeating implements InstructionVisitor<java.lang.Integer> {
    public boolean visitLddr(Lddr lddr) {
      incIfNextPC(lddr, 0);
      return false;
    }

    public boolean visitLdir(Ldir ldir) {
      incIfNextPC(ldir, 0);
      return false;
    }

    public boolean visitCpir(Cpir cpir) {
      incIfNextPC(cpir, 1);
      return false;
    }

    public boolean visitCpdr(Cpdr tCpdr) {
      incIfNextPC(tCpdr, -1);
      return false;
    }

    private void incIfNextPC(RepeatingInstruction repeating, int i) {
      int nextPC = repeating.getNextPC();
      int newValue;
      if (nextPC != -1) {
        newValue = (nextPC + 1) & 0xFFFF;
      } else {
        Integer wordNumber = memptr.read();
        newValue = (wordNumber + i) & 0xFFFF;
      }
      memptr.write(newValue);
    }
  }
}
