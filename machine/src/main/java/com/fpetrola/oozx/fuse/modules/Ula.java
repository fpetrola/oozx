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

package com.fpetrola.oozx.fuse.modules;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.bridge.GetTStatesHistory;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.oozx.fuse.peripherals.UlaFullDecodePeripheral;
import com.fpetrola.oozx.fuse.peripherals.UlaPeripheral;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.function.Supplier;

import static com.fpetrola.oozx.MachineCapability.PLUS3_MEMORY;
import static com.fpetrola.oozx.MachineCapability._128_MEMORY;

public class Ula implements ZxModule {
  private final Memory memory;

  public final int CONTENTION_SIZE = 80000;

  // How much contention do we get at every tstate when MREQ is active?
  public byte[] contention = new byte[CONTENTION_SIZE];

  // And how much when it is inactive
  public byte[] contentionNoMreq = new byte[CONTENTION_SIZE];

  byte lastByte;

  // What to return if no other input pressed; depends on the last byte output to the ULA
  private byte defaultValue;
  private final Display display;
  private final Supplier<SpectrumMachine> currentMachineSupplier;
  private Keyboard keyboard;
  private SpectrumZ80Clock z80Clock;
  private final IPeriph periph;


  public Ula(Memory memory, Display display, Supplier<SpectrumMachine> machineSupplier, Keyboard keyboard, SpectrumZ80Clock z80Clock, IPeriph periph) {
    this.memory = memory;
    this.display = display;
    this.currentMachineSupplier = machineSupplier;
    this.keyboard = keyboard;
    this.z80Clock = z80Clock;
    this.periph = periph;
  }

  // Initialize ULA module
  public int init(Object context) {
    Module.register(new UlaZxModuleInfo(this, z80Clock));
    periph.register(new UlaPeripheral(this));
    periph.register(new UlaFullDecodePeripheral(this));

    defaultValue = (byte) 0xff;

    return 0;
  }

  @Override
  public void end() {

  }

  // Register ULA with startup manager

  // Read from ULA port
  public byte read(int port, byte[] attached) {
    byte r = defaultValue;
    attached[0] = (byte) 0xff;

    r &= PhantomTypist.ulaRead(port);
    r &= keyboard.read((byte) (port >> 8));
    if (Tape.microphone) r ^= 0x40;

    return r;
  }

  // Write to ULA port
  public void write(int port, byte b) {
    lastByte = b;

    display.setLoresBorder(b & 0x07);
    Sound.beeper(z80Clock.getTStates(),
        ((b & 0x10) != 0 ? 2 : 0) + ((b & 0x08) == 0 || Tape.microphone ? 1 : 0));

    if ((getCurrent().getCapabilities() & PLUS3_MEMORY) != 0) {
      defaultValue = (byte) 0xbf;
    } else if ((getCurrent().getCapabilities() & _128_MEMORY) != 0 || !Settings.current.issue2) {
      defaultValue = (byte) ((b & 0x10) != 0 ? 0xff : 0xbf);
    } else {
      defaultValue = (byte) ((b & 0x18) != 0 ? 0xff : 0xbf);
    }
  }

  private SpectrumMachine getCurrent() {
    return currentMachineSupplier.get();
  }

  // Get the last byte written to the ULA
  public byte lastByte() {
    return lastByte;
  }

  // Get the tape level from the last byte
  public byte tapeLevel() {
    return (byte) (lastByte & 0x08);
  }

  public void contendPortEarly(int port) {
    if (memory.mapRead[port >>> memory.PAGE_SIZE_LOGARITHM].contended) {
      z80Clock.addTStates(contentionNoMreq[(int) z80Clock.getTStates()], "ula_contend_port_early");
    }
    z80Clock.addTStates((byte) 1, "contend_port_early");
  }

  public void contendPortLate(int port) {
    String ulaContendPortLate = "ula_contend_port_late";
    if (getCurrent().getRamInfo().portFromUla(port)) {
      z80Clock.addTStates((contentionNoMreq[(int) z80Clock.getTStates()] + 2), ulaContendPortLate);
    } else {
      if (memory.mapRead[port >>> memory.PAGE_SIZE_LOGARITHM].contended) {
        z80Clock.addTStates((contentionNoMreq[(int) z80Clock.getTStates()] + 1), ulaContendPortLate);
        z80Clock.addTStates((contentionNoMreq[(int) z80Clock.getTStates()] + 1), ulaContendPortLate);
        z80Clock.addTStates(contentionNoMreq[(int) z80Clock.getTStates()], ulaContendPortLate);
      } else {
        z80Clock.addTStates(2, "contend_port_late");
      }
    }
  }

}

