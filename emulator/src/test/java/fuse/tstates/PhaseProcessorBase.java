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
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.opcodes.references.IndirectMemory8BitReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.Phase;

import java.util.Optional;

import static com.fpetrola.z80.registers.RegisterName.*;

public abstract class PhaseProcessorBase<T extends WordNumber> implements InstructionVisitor<T, Integer> {
  protected Z80Cpu<T> cpu;
  protected Phase phase;
  protected T address;

  public PhaseProcessorBase(Z80Cpu<T> cpu) {
    this.cpu = cpu;
  }

  public void addMw(T address, T value) {
    getAddEvent(new Event(0, "MW", address.intValue(), value.intValue()));
  }

  protected State<T> getState() {
    return cpu.getState();
  }

  public void addMultipleMc(int x, int time1, int delta, int i1) {
    for (int i = 0; i < x; i++) {
      getAddEvent(new Event(time1, "MC", i1 + delta, null));
    }
  }

  private void getAddEvent(Event time1) {
    getState().addEvent(time1);
  }

  public void addMr(T address, T value) {
    getAddEvent(new Event(0, "MR", address.intValue(), value.intValue()));
  }

  public void addMc14Or19(T address) {
    if (getState().tstates == 14) {
      addMultipleMc(2, 1, 3, getRegister(PC).read().intValue());
    } else if (getState().tstates == 19) {
      addMultipleMc(1, 1, 0, address.intValue());
    }
  }

  protected Register<T> getRegister(RegisterName registerName) {
    return getState().getRegister(registerName);
  }

  public void setAddress(T address) {
    this.address = address;
  }

  public Optional<Boolean> isIndirectHL(TargetInstruction<T> targetInstruction) {
    return Optional.ofNullable(targetInstruction.getTarget() instanceof IndirectMemory8BitReference<?> indirectMemory8BitReference && indirectMemory8BitReference.getTarget() instanceof Register<?> register && register.getName().equals(HL.name()) ? true : null);
  }

  public void setPhase(Phase phase) {
    this.phase = phase;
  }

  public <T2 extends WordNumber> boolean isLdSP(Ld<T2> ld) {
    return ld.getTarget().equals(getRegister(SP)) && ld.getSource() instanceof Register<T2>;
  }

  protected void addMc(int time, RegisterName registerName, int delta) {
    addMultipleMc(time, 1, delta, getRegister(registerName).read().intValue());
  }

  public void processPhase(Phase phase) {
    DefaultInstructionFetcher<T> instructionFetcher = (DefaultInstructionFetcher<T>) cpu.getInstructionFetcher();
    Instruction<T> instruction2 = instructionFetcher.instruction2;
    setPhase(phase);
    if (instruction2 != null)
      instruction2.accept(this);
  }
}
