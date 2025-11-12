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
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.Phase;

import java.util.Optional;

import static com.fpetrola.z80.registers.RegisterName.*;

public abstract class PhaseProcessorBase implements InstructionVisitor<java.lang.Integer> {
  public int writeCount;
  protected Phase phase;
  protected int address;
  protected boolean processing;
  protected int readCount;
  protected final InstructionFetcher instructionFetcher;
  protected final State state;
  protected final Register registerI;
  protected final Register registerR;
  protected final Register registerIR;
  protected final Register registerSP;
  protected final Register registerPC;
  protected final Register registerDE;
  protected final Register registerBC;
  protected final Register registerHL;
  protected final Register memptr;

  public PhaseProcessorBase(InstructionFetcher instructionFetcher, State state) {
    this.instructionFetcher = instructionFetcher;
    this.state = state;
    memptr = state.getMemptr();
    registerI = getRegister(I);
    registerR = getRegister(R);
    registerIR = getRegister(IR);
    registerSP = getRegister(SP);
    registerPC = getRegister(PC);
    registerDE = getRegister(DE);
    registerBC = getRegister(BC);
    registerHL = getRegister(HL);
  }

  public void addMw(int address, int value) {
    getAddEvent(new Event(0, "MW", address, value));
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
    state.addEvent(time1);
  }

  public void addMr(int address, int value) {
    getAddEvent(new Event(0, "MR", address, value));
  }

  protected Register getRegister(RegisterName registerName) {
    return state.getRegister(registerName);
  }

  public void setAddress(int address) {
    this.address = address;
  }

  public Optional<Boolean> isIndirectHL(TargetInstruction targetInstruction) {
    return Optional.ofNullable(targetInstruction.getTarget() instanceof IndirectMemory8BitReference indirectMemory8BitReference && indirectMemory8BitReference.getTarget() instanceof Register register && register.getName().equals(HL.name()) ? true : null);
  }

  public void setPhase(Phase phase) {
    this.phase = phase;
  }

  public void reset() {
    readCount = 0;
    writeCount = 0;
  }

  public boolean isLdSP(Ld ld) {
    return ld.getTarget().equals(registerSP) && ld.getSource() instanceof Register;
  }

  protected void addMc2(int times, int delta, Register register, String description) {
    addMultipleMc(times, 1, delta, valueOf(register), description);
  }

  public void processPhase(Phase phase) {
    processing = true;
    Instruction lastExecutedInstruction = ((DefaultInstructionFetcher) instructionFetcher).getLastExecutedInstruction();

    if (lastExecutedInstruction != null) {
      CachedPhase cachedPhase = lastExecutedInstruction.getCachedPhase();
      if (!cachedPhase.isSkippable()) {
        setPhase(cachedPhase);
        cachedPhase.execute(phase);
      } else {
        int a = 1;
      }
    }
    processing = false;
  }

  protected boolean isMemory8BitReference(ImmutableOpcodeReference source) {
    return source instanceof Memory8BitReference;
  }

  protected boolean isMemoryPlus(OpcodeReferenceBase target) {
    return target instanceof MemoryPlusRegister8BitReference;
  }

  protected Optional<Boolean> isMemoryPlusOptional(OpcodeReferenceBase target) {
    return Optional.ofNullable(isMemoryPlus(target) ? true : null);
  }

  protected int valueOf(Register register) {
    return register.read();
  }

  protected Optional<Boolean> writeCountIsZero() {
    return Optional.ofNullable(writeCount == 0 ? true : null);
  }
}
