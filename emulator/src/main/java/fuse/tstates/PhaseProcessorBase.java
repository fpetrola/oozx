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

package fuse.tstates;

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.ExecutionListener;
import fuse.tstates.phases.AfterExecution;
import fuse.tstates.phases.BeforeExecution;
import fuse.tstates.phases.Phase;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.fpetrola.z80.registers.RegisterName.*;

/**
 * The contention of an instruction is worked out once, by visiting it, and applied while that
 * instruction executes: from before its execution to after it, and at each of its memory
 * accesses in between. Nothing of it lives in the instruction.
 */
public abstract class PhaseProcessorBase implements InstructionVisitor<java.lang.Integer>, ExecutionListener {
  private static final BeforeExecution BEFORE_EXECUTION = new BeforeExecution();
  private static final AfterExecution AFTER_EXECUTION = new AfterExecution();
  public int writeCount;
  protected Phase phase;
  protected int address;
  public int readCount;
  public final State state;
  protected final Register registerI;
  protected final Register registerR;
  protected final Register registerIR;
  protected final Register registerSP;
  protected final Register registerPC;
  protected final Register registerDE;
  protected final Register registerBC;
  protected final Register registerHL;
  private final Map<Instruction, CachedPhase> phases = new IdentityHashMap<>();
  private CachedPhase current;

  public PhaseProcessorBase(State state) {
    this.state = state;
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

  public void beforeExecution(Instruction instruction) {
    current = phases.computeIfAbsent(instruction, this::plan);
    readCount = 0;
    writeCount = 0;
    processPhase(BEFORE_EXECUTION);
  }

  public void afterExecution(Instruction instruction) {
    processPhase(AFTER_EXECUTION);
    current = null;
  }

  private CachedPhase plan(Instruction instruction) {
    CachedPhase planned = new CachedPhase();
    phase = planned;
    instruction.accept(this);
    return planned;
  }

  public void setAddress(int address) {
    this.address = address;
  }

  public void processPhase(Phase phase) {
    if (current != null && !current.isSkippable())
      current.execute(phase);
  }
}
