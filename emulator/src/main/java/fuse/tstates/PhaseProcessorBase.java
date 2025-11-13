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
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.phases.Phase;

import static com.fpetrola.z80.registers.RegisterName.*;

public abstract class PhaseProcessorBase implements InstructionVisitor<java.lang.Integer> {
  public int writeCount;
  protected Phase phase;
  protected int address;
  protected boolean processing;
  public int readCount;
  protected final InstructionFetcher instructionFetcher;
  public final State state;
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


  private Register getRegister(RegisterName registerName) {
    return state.getRegister(registerName);
  }

  public void setAddress(int address) {
    this.address = address;
  }

  public void setPhase(Phase phase) {
    this.phase = phase;
  }

  public void reset() {
    readCount = 0;
    writeCount = 0;
  }

  public void processPhase(Phase phase) {
    processing = true;
    Instruction lastExecutedInstruction = ((DefaultInstructionFetcher) instructionFetcher).getLastExecutedInstruction();

    if (lastExecutedInstruction != null) {
      CachedPhase cachedPhase = lastExecutedInstruction.getCachedPhase();
      if (!cachedPhase.isSkippable()) {
        cachedPhase.execute(phase);
      }
    }
    processing = false;
  }
}
