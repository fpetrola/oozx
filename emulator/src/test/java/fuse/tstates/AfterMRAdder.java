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
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.instructions.types.ParameterizedBinaryAluInstruction;
import com.fpetrola.z80.instructions.types.ParameterizedUnaryAluInstruction;
import com.fpetrola.z80.opcodes.references.MemoryPlusRegister8BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import static com.fpetrola.z80.registers.RegisterName.*;

class AfterMRAdder<T extends WordNumber> implements InstructionVisitor<T, Object> {
  private final AddStatesMemoryReadListener addStatesMemoryReadListener;
  private final Z80Cpu<T> cpu;
  private final T address;
  private final State<T> state;

  public AfterMRAdder(AddStatesMemoryReadListener addStatesMemoryReadListener, Z80Cpu<T> cpu, T address, State<T> state) {
    this.addStatesMemoryReadListener = addStatesMemoryReadListener;
    this.cpu = cpu;
    this.address = address;
    this.state = state;
  }

  public boolean visitRLD(RLD<T> rld) {
    if (cpu.getState().getTStatesSinceCpuStart() == 11) {
      AddStatesMemoryReadListener.addMc(4, cpu, 1, 0, cpu.getState().getRegister(HL).read().intValue());
    }
    return InstructionVisitor.super.visitRLD(rld);
  }

  public boolean visitLdOperation(LdOperation ldOperation) {
    addStatesMemoryReadListener.addMc14Or19(address);
    return InstructionVisitor.super.visitLdOperation(ldOperation);
  }

  public void visitingBitOperation(BitOperation tBitOperation) {
    if (!(tBitOperation instanceof RES<?> || tBitOperation instanceof SET<?>) || !addStatesMemoryReadListener.addIfIndirectHL(tBitOperation))
      addStatesMemoryReadListener.addMc14Or19(address);
  }

  public boolean visitingParameterizedUnaryAluInstruction(ParameterizedUnaryAluInstruction parameterizedUnaryAluInstruction) {
    addStatesMemoryReadListener.addIfIndirectHL(parameterizedUnaryAluInstruction);
    addStatesMemoryReadListener.addMc14Or19(address);
    return InstructionVisitor.super.visitingParameterizedUnaryAluInstruction(parameterizedUnaryAluInstruction);
  }

  public boolean visitingInc(Inc<T> tInc) {
    if (cpu.getState().getTStatesSinceCpuStart() == 11)
      if (tInc.getTarget() instanceof MemoryPlusRegister8BitReference<T>) {
        AddStatesMemoryReadListener.addMc(5, cpu, 1, 2, cpu.getState().getRegister(PC).read().intValue());
      }
    return InstructionVisitor.super.visitingInc(tInc);
  }

  public boolean visitingDec(Dec<T> dec) {
    if (cpu.getState().getTStatesSinceCpuStart() == 11)
      if (dec.getTarget() instanceof MemoryPlusRegister8BitReference<T>) {
        AddStatesMemoryReadListener.addMc(5, cpu, 1, 2, cpu.getState().getRegister(PC).read().intValue());
      }
    return InstructionVisitor.super.visitingDec(dec);
  }

  public void visitingLd(Ld<T> ld) {
    if (cpu.getState().getTStatesSinceCpuStart() == 11)
      if (!ld.getTarget().equals(state.getRegister(SP)) || ld.getSource() instanceof Register<T>) {
        AddStatesMemoryReadListener.addMc(5, cpu, 1, 0, cpu.getState().getRegister(IR).read().intValue());
      }
    InstructionVisitor.super.visitingLd(ld);
  }

  public void visitingParameterizedBinaryAluInstruction(ParameterizedBinaryAluInstruction parameterizedBinaryAluInstruction) {
    if (cpu.getState().getTStatesSinceCpuStart() == 11)
      AddStatesMemoryReadListener.addMc(5, cpu, 1, 0, cpu.getState().getRegister(IR).read().intValue());
  }
}
