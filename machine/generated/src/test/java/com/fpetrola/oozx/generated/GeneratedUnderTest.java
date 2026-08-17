/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
