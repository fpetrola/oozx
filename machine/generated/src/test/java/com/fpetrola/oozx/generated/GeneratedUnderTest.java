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
package com.fpetrola.oozx.generated;

import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.DecodedMemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.z80.ProcessorUnderTest;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.tstates.PhaseProcessor;
import model.harness.MachineTest;

import java.util.Arrays;
import java.util.Set;

/**
 * The generated core under the emulator's batteries. It is made against a machine, so it is handed
 * one machine's objects - never reached, since its pages are its own: sixty four K laid over the
 * memory the tests set up and read back, writable, uncontended, no screen in them. The ports are
 * the tests' recorder and the clock is the processor's own. Registered as a service, so the
 * batteries find it when this module is on the classpath.
 */
public class GeneratedUnderTest implements ProcessorUnderTest {
  private static Speccy machine;
  private final SpectrumZ80Clock clock = new SpectrumZ80Clock();
  /** Its own page tables: the machine's are the OOP core's under the same batteries, and whoever laid them out last would own every read. */
  private final MemoryBus tables =
      new DecodedMemoryBus();
  private GeneratedMachineCore core;

  private static Speccy machine() {
    if (machine == null)
      machine = MachineTest.silentMachine(EmulatorModule.core(OopCore.class));
    return machine;
  }

  public String name() {
    return GeneratedMachineCore.NAME;
  }

  public Memory memory() {
    return new Storage();
  }

  public RegisterBank bank(Memory memory, IO io) {
    Speccy speccy = machine();
    Ram flat = new Ram(((Storage) memory).bytes);
    for (int slot = 0; slot < 4; slot++) {
      tables.slot(slot << 14, new MappedMemory(slot << 14, flat, slot << 14, 0x4000));
    }
    core = new GeneratedMachineCore(() -> GeneratedCores.loaded().orElseThrow(), tables, speccy.ula, clock, speccy.display);
    return core.bank(memory, io);
  }

  public OOZ80 cpu(State state, PhaseProcessor contention) {
    state.clock = clock;
    return core.cpu(state, contention);
  }

  public boolean countsItsOwnContention() {
    return true;
  }

  public Set<String> reportedEvents() {
    return Set.of("PR", "PW", "PC");
  }

  /** Sixty four K, as bytes, so the core's pages can be laid over them. */
  static final class Storage implements Memory {
    final byte[] bytes = new byte[0x10000];

    public int read(int address, int fetching) {
      return bytes[address] & 0xff;
    }

    public void write(int address, int value) {
      bytes[address] = (byte) value;
    }

    public void reset() {
      Arrays.fill(bytes, (byte) 0);
    }

    public int peek(int address) {
      return bytes[address] & 0xff;
    }

    public void poke(int address, int value) {
      bytes[address] = (byte) value;
    }
  }
}
