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

import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.opcodes.references.IndirectMemory16BitReference;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.List;

import static com.fpetrola.z80.registers.RegisterName.*;

class AfterExecutionAdder<T extends WordNumber> extends StatesAdder<T, List<StatesAddition>> {
  private final Z80Cpu<T> cpu;

  public AfterExecutionAdder(Z80Cpu<T> cpu) {
    this.cpu = cpu;
  }

  public static boolean isIndirectHL(TargetInstruction tBitOperation) {
    return tBitOperation.getTarget() instanceof IndirectMemory8BitReference<?> indirectMemory8BitReference && indirectMemory8BitReference.getTarget() instanceof Register<?> register && register.getName().equals(HL.name());
  }

  public void visitEx(Ex<T> ex) {
    if (ex.getTarget() instanceof IndirectMemory16BitReference<T>)
      addResult(2, SP, 0);
  }

  @Override
  public boolean visitingBit(BIT bit) {
    if (isIndirectHL(bit))
      addResult(1, HL, 0);

    return false;
  }

  public boolean visitOuti(Outi<T> outi) {
    if (outi.getNextPC() != null) addResult(5, IR, 0);
    return false;
  }

  public boolean visitIni(Ini<T> tIni) {
    if (tIni.getNextPC() != null) addResult(5, IR, 0);
    return false;
  }

  public boolean visitLdd(Ldd ldd) {
    addResult(2, DE, 1);
    return true;
  }

  public void visitLdi(Ldi<T> tLdi) {
    addResult(2, DE, -1);
  }

  @Override
  public void visitCpi(Cpi<T> cpi) {
    addResult(5, HL, -1);
  }

  @Override
  public boolean visitCpd(Cpd<T> cpd) {
    addResult(5, HL, 1);
    return true;
  }

  public boolean visitRepeatingInstruction(RepeatingInstruction<T> tRepeatingInstruction) {
    if (tRepeatingInstruction instanceof Inir<T>) {
      if (tRepeatingInstruction.getNextPC() != null) {
        addResult(5, HL, -1);
      }
    } else if (tRepeatingInstruction instanceof Indr<T>) {
      if (tRepeatingInstruction.getNextPC() != null) {
        addResult(5, HL, 1);
      }
    } else if (tRepeatingInstruction instanceof Outdr<T> || tRepeatingInstruction instanceof Outir<T>) {
      if (tRepeatingInstruction.getNextPC() != null) {
        addResult(5, BC, 0);
      }
    } else if (tRepeatingInstruction instanceof Ldir<T>) {
      addResult(2, DE, -1);
      if (tRepeatingInstruction.getNextPC() != null) {
        addResult(5, DE, -1);
      }
    } else if (tRepeatingInstruction instanceof Lddr<T>) {
      addResult(2, DE, 1);
      if (tRepeatingInstruction.getNextPC() != null) {
        addResult(5, DE, 1);
      }
    } else if (tRepeatingInstruction instanceof Cpir<T>) {
      addResult(5, HL, -1);
      if (tRepeatingInstruction.getNextPC() != null) {
        addResult(5, HL, -1);
      }
    } else if (tRepeatingInstruction instanceof Cpdr<T>) {
      addResult(5, HL, 1);
      if (tRepeatingInstruction.getNextPC() != null) {
        addResult(5, HL, 1);
      }
    }
    return super.visitRepeatingInstruction(tRepeatingInstruction);
  }

  private void addResult(int time, RegisterName registerName, int delta) {
    result.add(new StatesAddition(time, registerName, delta));
  }

  public void visitingJR(JR jr) {
    if (jr.getNextPC() != null) {
      AddStatesMemoryReadListener.addMc(5, cpu, 1, 1, cpu.getState().getRegister(HL).read().intValue());
    } else {
      AddStatesMemoryReadListener.addMc(1, cpu, 1, 0, cpu.getState().getRegister(IR).read().intValue());
      cpu.getState().tstates += 2;
    }
  }

  public boolean visitingDjnz(DJNZ<T> djnz) {
    if (djnz.getNextPC() != null) {
      AddStatesMemoryReadListener.addMc(5, cpu, 1, 1, cpu.getState().getRegister(PC).read().intValue());
    } else {
      AddStatesMemoryReadListener.addMc(1, cpu, 1, 1, cpu.getState().getRegister(PC).read().intValue());
      cpu.getState().tstates += 2;
    }

    super.visitingDjnz(djnz);
    return true;
  }
}
