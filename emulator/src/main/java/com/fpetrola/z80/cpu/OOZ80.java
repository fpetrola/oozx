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

import com.fpetrola.z80.instructions.impl.EI;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;

import static com.fpetrola.z80.cpu.State.InterruptionMode.IM2;

final public class OOZ80 implements Z80Cpu {
  private final InstructionFetcher instructionFetcher;
  private final InstructionExecutor instructionExecutor;
  private final State state;
  private final Register registerR;
  private final Register registerSP;
  private final Register pc;
  private final Register regI;
  private final Register memptr;
  private Memory memory;

  public OOZ80(State aState, InstructionFetcher instructionFetcher, InstructionExecutor instructionExecutor) {
    this.state = aState;
    this.instructionFetcher = instructionFetcher;
    this.instructionExecutor = instructionExecutor;
    registerR = state.getRegisterR();
    registerSP = state.getRegisterSP();
    pc = state.getPc();
    regI = state.getRegI();
    memptr = state.getMemptr();
    memory = state.getMemory();
  }

  public void reset() {
    instructionExecutor.reset();
    instructionFetcher.reset();
    state.reset();
  }

  public void execute() {
    if (state.isActiveNMI()) {
      state.setActiveNMI(false);
      return;
    }
    if (state.isIntLine() && state.isIff1() && !state.isPendingEI())
      interruption();

    Instruction instruction;
    try {
      instruction = execute(1);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Invalid instruction");
      throw new RuntimeException(e);
    }
    if (state.isPendingEI() && !(instruction instanceof EI)) {
      state.setPendingEI(false);
      endInterruption();
    }
  }

  public Instruction execute(int cycles) {
    try {
      Instruction currentInstruction = instructionFetcher.fetchNextInstruction();
      instructionExecutor.execute(currentInstruction);
//      instructionFetcher.afterExecute(currentInstruction);
      return currentInstruction;
    } catch (Exception e) {
      e.printStackTrace();
      state.setRunState(State.RunState.STATE_STOPPED_BREAK);
      return null;
    }
  }

  public void interruption() {
    getState().setINTLine(true);
    doInt();
    getState().setINTLine(false);
  }

  private void doInt() {
    if (state.isHalted()) {
      state.setHalted(false);
      pc.increment();
    }

    registerR.increment();
    Push.doPush(pc.read(), registerSP, memory);
    state.setIff1(false);
    state.setIff2(false);

    int value;
    if (state.getInterruptionMode() == IM2) {
      value = memory.read16Bits(((regI.read() << 8) & 0xFFFF | 0xff) & 0xFFFF);
    } else {
      value = 0x0038;
    }
    pc.write(value);
    memptr.write(value);
  }

  public void endInterruption() {
  }

  public void update() {
    memory.update();
    instructionFetcher.reset();
  }

  public InstructionFetcher getInstructionFetcher() {
    return instructionFetcher;
  }

  public State getState() {
    return state;
  }

  public InstructionExecutor getInstructionExecutor() {
    return instructionExecutor;
  }
}
