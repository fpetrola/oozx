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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.se.Z80InstructionDriver;
import com.fpetrola.z80.spy.MemorySpy;

import java.util.function.Supplier;

public abstract class DefaultZ80InstructionDriver implements Z80InstructionDriver {
  protected Z80Cpu z80;

  public DefaultZ80InstructionDriver(OOZ80 z80) {
    this.z80 = z80;
  }

  public void step() {
    z80.execute();
  }

  public MockedMemory mem() {
    return (MockedMemory) (getState().getMemory() instanceof MemorySpy memorySpy ? memorySpy.getMemory() : getState().getMemory());
  }

  public MockedMemory initMem(Supplier<Integer[]> supplier) {
    mem().init(supplier);
    return mem();
  }

  public State getState() {
    return z80.getState();
  }
}
