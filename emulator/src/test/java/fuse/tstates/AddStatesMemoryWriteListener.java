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

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class AddStatesMemoryWriteListener<T extends WordNumber> implements MemoryWriteListener<T> {
  private Z80Cpu<T> cpu;
  private State<T> state;
  private DefaultInstructionFetcher instructionFetcher;

  public AddStatesMemoryWriteListener(Z80Cpu cpu1) {
    cpu = cpu1;
    this.instructionFetcher = (DefaultInstructionFetcher) cpu1.getInstructionFetcher();
    state = cpu.getState();
  }

  @Override
  public void writtingMemoryAt(T address, T value) {
    AddStatesMemoryReadListener.addMcWith(new BeforeWriteAdder<T>(cpu.getState()), null,
        (result) -> AddStatesMemoryReadListener.addMc(result.time(), cpu, 1, result.delta(), cpu.getState().getRegister(result.registerName()).read().intValue()), instructionFetcher);

    AddStatesMemoryReadListener.addMc(1, cpu, 3, 0, address.intValue());
    cpu.getState().addEvent(new Event(0, "MW", address.intValue(), value.intValue()));
  }

}
