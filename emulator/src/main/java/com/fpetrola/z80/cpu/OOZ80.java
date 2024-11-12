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
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.stream.Stream;

import static com.fpetrola.z80.cpu.State.InterruptionMode.IM0;
import static com.fpetrola.z80.cpu.State.InterruptionMode.IM2;
import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.AF;
import static com.fpetrola.z80.registers.RegisterName.IR;

public class OOZ80<T extends WordNumber> implements Z80Cpu<T> {
  protected InstructionFetcher instructionFetcher;
  protected State<T> state;

  public OOZ80(State aState, InstructionFetcher instructionFetcher) {
    this.state = aState;
    this.instructionFetcher = instructionFetcher;
  }

  @Override
  public void reset() {
    instructionFetcher.reset();
    Stream.of(RegisterName.values()).forEach(r -> state.r(r).write(createValue(0xFFFF)));
    state.getRegister(IR).write(createValue(0));
    state.getRegister(AF).write(createValue(0xFFFF));
    state.setIntMode(IM0);
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
    instructionFetcher.fetchNextInstruction();
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
    T value = state.getInterruptionMode() == IM2 ? Memory.read16Bits(state.getMemory(), (state.getRegI().read().left(8)).or(0xff)) : createValue(0x0038);
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

}
