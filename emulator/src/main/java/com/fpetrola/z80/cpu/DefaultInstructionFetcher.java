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
import com.fpetrola.z80.instructions.cache.InstructionCloner;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.instructions.factory.InstructionFactory;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeConditions;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.registers.RegisterName;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class DefaultInstructionFetcher<T extends WordNumber> implements InstructionFetcher<T> {
  protected final MultiOpcodeFetcher<T> multiOpcodeFetcher;
  protected State<T> state;
  protected T pcValue;
  public Instruction<T> currentInstruction;

  private final CollectionHandler<FetchListener<T>> fetchListeners = new CollectionHandler<>();
  private int prefetchPC = -1;
  private Instruction<T> prefetchedInstruction;
  protected int rdelta;
  private boolean prefetch = false;
  protected DefaultRegisterBankFactory.RRegister<T> registerR;
  private Memory<T> memory;

  public DefaultInstructionFetcher(State aState, OpcodeConditions opcodeConditions, InstructionFactory instructionFactory, boolean clone, boolean prefetch) {
    this.state = aState;
    this.prefetch = prefetch;
    multiOpcodeFetcher = new MultiOpcodeFetcher<T>(instructionFactory, state, opcodeConditions, clone);
    pcValue = state.getPc().read();
    this.registerR = (DefaultRegisterBankFactory.RRegister<T>) state.getRegisterR();
    this.memory = state.getMemory();
  }

  public DefaultInstructionFetcher(State aState, InstructionFactory instructionFactory, boolean clone, boolean prefetch) {
    this(aState, OpcodeConditions.createOpcodeConditions(aState.getFlag(), aState.getRegister(RegisterName.B)), instructionFactory, clone, prefetch);
  }

  public DefaultInstructionFetcher(State aState, boolean clone, boolean prefetch) {
    this(aState, OpcodeConditions.createOpcodeConditions(aState.getFlag(), aState.getRegister(RegisterName.B)), new DefaultInstructionFactory(aState), clone, prefetch);
  }

  @Override
  public Instruction<T> fetchNextInstruction() {
    fetchListeners.forAll(FetchListener::beforeFetch);
    int rValue = registerR.read().intValue();
    registerR.increment();
    pcValue = state.getPc().read();

    if (prefetchPC != pcValue.intValue()) {
      currentInstruction = fetchInstruction(pcValue);
      prefetchedInstruction = currentInstruction;
    } else {
      currentInstruction = prefetchedInstruction;
      registerR.write(createValue(registerR.read().intValue() + rdelta));
    }

    rdelta = registerR.read().intValue() - rValue;
//    if (rdelta < 0)
//      System.out.println("adgagadg");
//    ((AbstractInstruction) currentInstruction).setRDelta(rdelta);

    return currentInstruction;
  }

  @Override
  public void afterExecute(Instruction<?> currentInstruction) {
    try {
      if (prefetch) {
        int rValue = registerR.read().intValue();
        T nextPC = createValue(0);
        prefetchedInstruction = fetchInstruction(nextPC);
        prefetchPC = nextPC.intValue();
        rdelta = registerR.read().intValue() - rValue;
        registerR.write(createValue(rValue));
      }
    } catch (Exception e) {
      e.printStackTrace();
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
    }
  }

  public Instruction<T> fetchInstruction(T address) {
    int opcodeInt = memory.read(address, 1).intValue();
    Instruction<T> fetchedInstruction = multiOpcodeFetcher.fetchInstruction(opcodeInt);
    if (multiOpcodeFetcher.clone)
      fetchedInstruction = new InstructionCloner<T, T>(multiOpcodeFetcher.instructionFactory).clone(fetchedInstruction);

    Instruction<T> finalBaseInstruction = fetchedInstruction;
    fetchListeners.forAll(l -> l.instructionFetchedAt(address, finalBaseInstruction));
    return fetchedInstruction;
  }


  @Override
  public void reset() {
    currentInstruction = null;
    multiOpcodeFetcher.reset();
  }

  @Override
  public void addFetchListener(FetchListener<T> fetchListener) {
    fetchListeners.add(fetchListener);
  }

  @Override
  public void setPrefetch(boolean prefetch) {
    this.prefetch = prefetch;
  }

  public void setClone(boolean clone) {
    multiOpcodeFetcher.setClone(clone);
  }

  public Instruction<T> getLastExecutedInstruction() {
    return currentInstruction;
  }

  public void setLastExecutedInstruction(Instruction<T> instruction) {
    currentInstruction = instruction;
  }
}
