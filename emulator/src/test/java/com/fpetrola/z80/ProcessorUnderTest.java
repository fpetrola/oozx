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
package com.fpetrola.z80;

import com.fpetrola.z80.tstates.RecordingPhaseProcessor;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.MockedMemory;

import java.util.ServiceLoader;
import java.util.Set;

/**
 * What the emulator's batteries drive: a Core over a memory the tests set up and read back, and
 * the ports they record. Whoever is registered on the classpath as a service is it, else the OOP
 * core over the harness memory: the tests do not change, the classpath does.
 */
public interface ProcessorUnderTest extends Core {
  static ProcessorUnderTest found() {
    return ServiceLoader.load(ProcessorUnderTest.class).findFirst().orElseGet(Oop::new);
  }

  /** A fresh memory the processor will run on, which the tests peek and poke. */
  Memory memory();

  /** The event types the harness sees from this processor: one that reaches its memory itself reports no memory events. */
  Set<String> reportedEvents();

  /** A processor with nothing watching it: the memory fresh, the contention going nowhere. */
  default OOZ80 processor(IO io) {
    Memory memory = memory();
    State state = new State(io, bank(memory, io), memory);
    return cpu(state, new RecordingPhaseProcessor(state, event -> {
    }));
  }

  /** The OOP core over the harness memory, whose listeners report every access. */
  class Oop extends OopCore implements ProcessorUnderTest {
    public Memory memory() {
      return new MockedMemory(true);
    }

    public Set<String> reportedEvents() {
      return Set.of("MR", "MW", "MC", "PR", "PW", "PC");
    }
  }
}
