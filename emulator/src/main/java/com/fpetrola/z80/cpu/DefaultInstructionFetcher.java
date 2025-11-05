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

import com.fpetrola.z80.helpers.CollectionHandler;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.AbstractInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory.RRegister;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import fuse.tstates.CachedPhase;
import fuse.tstates.PhaseProcessor;

public class DefaultInstructionFetcher implements InstructionFetcher {
  protected final MultiOpcodeFetcher multiOpcodeFetcher;
  protected final State state;
  protected int pcValue;
  public Instruction currentInstruction;

  private final CollectionHandler<FetchListener> fetchListeners = new CollectionHandler();
  private int prefetchPC = -1;
  private Instruction prefetchedInstruction;
  protected int rdelta;
  private final boolean prefetch;
  protected final RRegister registerR;
  public PhaseProcessor tPhaseProcessor;
  private final Register pc;

  public DefaultInstructionFetcher(State aState, OpcodeConditions opcodeConditions, InstructionFactory instructionFactory, boolean clone, boolean prefetch) {
    this.state = aState;
    this.prefetch = prefetch;
    multiOpcodeFetcher = new MultiOpcodeFetcher(instructionFactory, state, opcodeConditions, clone);
    pcValue = state.getPc().read();
    this.registerR = (RRegister) state.getRegisterR();
    this.pc = state.getPc();
    tPhaseProcessor = new PhaseProcessor(this, state);
  }

  public DefaultInstructionFetcher(State aState, InstructionFactory instructionFactory, boolean clone, boolean prefetch) {
    this(aState, OpcodeConditions.createOpcodeConditions(aState.getFlag(), aState.getRegister(RegisterName.B)), instructionFactory, clone, prefetch);
  }

  public DefaultInstructionFetcher(State aState, boolean clone, boolean prefetch) {
    this(aState, OpcodeConditions.createOpcodeConditions(aState.getFlag(), aState.getRegister(RegisterName.B)), new DefaultInstructionFactory(aState), clone, prefetch);
  }

  @Override
  public Instruction fetchNextInstruction() {
//    fetchListeners.forAll(FetchListener::beforeFetch);
//    int rValue = registerR.read();
    registerR.increment();
    pcValue = pc.read();
    currentInstruction = fetchInstruction(pcValue);

//    rdelta = registerR.read() - rValue;
//    if (rdelta < 0)
//      System.out.println("adgagadg");
//    ((AbstractInstruction) currentInstruction).setRDelta(rdelta);

    return currentInstruction;
  }

  @Override
  public void afterExecute(Instruction currentInstruction) {
    try {
      if (prefetch) {
        int rValue = registerR.read();
        int nextPC = 0;
        prefetchedInstruction = fetchInstruction(nextPC);
        prefetchPC = nextPC;
        rdelta = registerR.read() - rValue;
        registerR.write(rValue);
      }
    } catch (Exception e) {
      e.printStackTrace();
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
    }
  }

  public Instruction fetchInstruction(int address) {
    Instruction fetchedInstruction = multiOpcodeFetcher.fetchInstruction(address);
    setupPhaseInterceptor((AbstractInstruction) fetchedInstruction);
//    fetchListeners.forAll(l -> l.instructionFetchedAt(address, fetchedInstruction));
    return fetchedInstruction;
  }

  protected void setupPhaseInterceptor(AbstractInstruction fetchedInstruction) {
    CachedPhase phase1 = fetchedInstruction.getCachedPhase();
    if (!phase1.isReady()) {
      tPhaseProcessor.setPhase(phase1);
      fetchedInstruction.accept(tPhaseProcessor);
      phase1.ready();
    }
  }


  @Override
  public void reset() {
    currentInstruction = null;
    multiOpcodeFetcher.reset();
  }

  @Override
  public void addFetchListener(FetchListener fetchListener) {
    fetchListeners.add(fetchListener);
  }

  public void setClone(boolean clone) {
    multiOpcodeFetcher.setClone(clone);
  }

  public Instruction getLastExecutedInstruction() {
    return currentInstruction;
  }

  public void setLastExecutedInstruction(Instruction instruction) {
    currentInstruction = instruction;
  }
}
