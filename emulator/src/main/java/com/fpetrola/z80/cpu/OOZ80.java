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

import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import static com.fpetrola.z80.cpu.State.InterruptionMode.IM2;
import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class OOZ80<T extends WordNumber> implements Z80Cpu<T> {
  protected InstructionFetcher instructionFetcher;
  private final InstructionExecutor<T> instructionExecutor;
  protected State<T> state;

  public OOZ80(State aState, InstructionFetcher instructionFetcher, InstructionExecutor<T> instructionExecutor) {
    this.state = aState;
    this.instructionFetcher = instructionFetcher;
    this.instructionExecutor = instructionExecutor;
  }

  @Override
  public void reset() {
    instructionFetcher.reset();
    state.reset();
  }

  @Override
  public void execute() {
    if (state.isActiveNMI()) {
      state.setActiveNMI(false);
      return;
    }
    if (state.isIntLine() && state.isIff1() && !state.isPendingEI())
      interruption();

    try {
      execute(1);
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Invalid instruction");
      throw new RuntimeException(e);
    }
    if (state.isPendingEI()) {
      state.setPendingEI(false);
      endInterruption();
    }
  }

  public void execute(int cycles) {
    Instruction<T> currentInstruction = (Instruction<T>) instructionFetcher.fetchNextInstruction();
    instructionExecutor.execute(currentInstruction);
    instructionFetcher.afterExecute(currentInstruction);
  }

  @Override
  public void interruption() {
    doInt();
  }

  private void doInt() {
    Register<T> pc = state.getPc();

    if (state.isHalted()) {
      state.setHalted(false);
      pc.increment();
    }

    state.getRegisterR().increment();
    state.setIff1(false);
    state.setIff2(false);

    Push.doPush(pc.read(), state.getRegisterSP(), state.getMemory());
    T value;
    if (state.getInterruptionMode() == IM2)
      value = Memory.read16Bits(state.getMemory(), (state.getRegI().read().left(8)).or(0xff));
    else
      value = createValue(0x0038);
    pc.write(value);
    state.getMemptr().write(value);
  }

  @Override
  public void endInterruption() {
  }

  public void update() {
    state.getMemory().update();
    instructionFetcher.reset();
  }

  @Override
  public InstructionFetcher getInstructionFetcher() {
    return instructionFetcher;
  }

  @Override
  public State<T> getState() {
    return state;
  }

  @Override
  public InstructionExecutor<T> getInstructionExecutor() {
    return instructionExecutor;
  }
}
