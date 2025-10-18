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
import com.fpetrola.z80.instructions.impl.Dec;
import com.fpetrola.z80.instructions.impl.Inc;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.BeforeExecution;
import fuse.tstates.phases.DefaultPhaseVisitor;
import fuse.tstates.phases.Phase;

import java.util.Optional;

import static com.fpetrola.z80.registers.RegisterName.*;

public abstract class PhaseProcessorBase<T extends WordNumber> implements InstructionVisitor<T, Integer> {
  protected Z80Cpu<T> cpu;
  protected Phase phase;
  protected T address;
  protected boolean processing;
  protected int readCount;
  public int writeCount;

  public PhaseProcessorBase(Z80Cpu<T> cpu) {
    this.cpu = cpu;
  }

  public void addMw(T address, T value) {
    getAddEvent(new Event(0, "MW", address.intValue(), value.intValue()));
  }

  protected State<T> getState() {
    return cpu.getState();
  }

  public void addMultipleMc(int x, int time1, int delta, int baseAddress, String description) {
    for (int i = 0; i < x; i++) {
      addSingleMc(time1, delta, baseAddress, description);
    }
  }

  public void addSingleMc(int time1, int delta, int baseAddress, String description) {
    getAddEvent(new Event(time1, "MC", baseAddress + delta, null, description));
  }

  protected void getAddEvent(Event time1) {
    getState().addEvent(time1);
  }

  public void addMr(T address, T value) {
    getAddEvent(new Event(0, "MR", address.intValue(), value.intValue()));
  }

  protected boolean isNotIncDec(Instruction<T> targetInstruction) {
    return !(targetInstruction instanceof Inc) && !(targetInstruction instanceof Dec);
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
    phase.accept(new DefaultPhaseVisitor() {
      public void visit(BeforeExecution beforeExecution) {
        reset();
      }
    });
  }

  private void reset() {
    readCount = 0;
    writeCount = 0;
  }

  public boolean isLdSP(Ld<T> ld) {
    return ld.getTarget().equals(getRegister(SP)) && ld.getSource() instanceof Register<T>;
  }

  protected void addMc(int times, RegisterName registerName, int delta, String description) {
    addMultipleMc(times, 1, delta, getRegister(registerName).read().intValue(), description);
  }

  protected void addMc(int times, int address, int delta, String description) {
    addMultipleMc(times, 1, delta, address, description);
  }

  public void processPhase(Phase phase) {
    processing = true;
    DefaultInstructionFetcher<T> instructionFetcher = (DefaultInstructionFetcher<T>) cpu.getInstructionFetcher();
    Instruction<T> instruction2 = instructionFetcher.getLastExecutedInstruction();
    setPhase(phase);
    if (instruction2 != null)
      instruction2.accept(this);

    processing = false;
  }

  protected boolean isMemory8BitReference(ImmutableOpcodeReference<T> source) {
    return source instanceof Memory8BitReference<T>;
  }

  protected boolean isMemoryPlus(ImmutableOpcodeReference<T> target) {
    return target instanceof MemoryPlusRegister8BitReference<T>;
  }

  protected Optional<Boolean> isMemoryPlusOptional(ImmutableOpcodeReference<T> target) {
    return Optional.ofNullable(isMemoryPlus(target) ? true : null);
  }

}
