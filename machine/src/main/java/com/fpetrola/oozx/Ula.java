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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.GetTStatesHistory;
import com.fpetrola.oozx.fuse.Keyboard;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.peripherals.Periph;

public class Ula {
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

  public Ula(Memory memory, Display display) {
    this.memory = memory;
    this.display = display;
  }

  // Initialize ULA module
  int init(Object context) {
    Module.register(new UlaZxModuleInfo(this));
    Periph.register(new UlaPeripheral());
    Periph.register(new UlaFullDecodePeripheral());

    defaultValue = (byte) 0xff;

    return 0;
  }

  // Register ULA with startup manager

  // Read from ULA port
  public byte read(int port, byte[] attached) {
    byte r = defaultValue;
    attached[0] = (byte) 0xff;

    Loader.detectLoader();

    r &= PhantomTypist.ulaRead(port);
    r &= Keyboard.read((byte) (port >> 8));
    if (Tape.microphone) r ^= 0x40;

    return r;
  }

  // Write to ULA port
  public void write(int port, byte b) {
    lastByte = b;

    display.setLoresBorder(b & 0x07);
    Sound.beeper(Spectrum.tstates,
        ((b & 0x10) != 0 ? 2 : 0) + ((b & 0x08) == 0 || Tape.microphone ? 1 : 0));

    if (Machine.current.timex) {
      defaultValue = (byte) 0x5f;
    } else if ((Machine.current.capabilities & Libspectrum.MachineCapability.PLUS3_MEMORY) != 0) {
      defaultValue = (byte) 0xbf;
    } else if ((Machine.current.capabilities & Libspectrum.MachineCapability._128_MEMORY) != 0 || !Settings.current.issue2) {
      defaultValue = (byte) ((b & 0x10) != 0 ? 0xff : 0xbf);
    } else {
      defaultValue = (byte) ((b & 0x18) != 0 ? 0xff : 0xbf);
    }
  }

  // Get the last byte written to the ULA
  public byte lastByte() {
    return lastByte;
  }

  // Get the tape level from the last byte
  public byte tapeLevel() {
    return (byte) (lastByte & 0x08);
  }

  // Load ULA state from snapshot

  // Save ULA state to snapshot

  // Handle contention for port access (early phase)
  public void contendPortEarly(int port) {
//    System.out.println("port2 "+ port);
    if (memory.mapRead[port >>> memory.PAGE_SIZE_LOGARITHM].contended) {
      GetTStatesHistory.addTStateUpdate(contentionNoMreq[(int) Spectrum.tstates], "ula_contend_port_early", (int) Spectrum.tstates);
      Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
    }
    GetTStatesHistory.addTStateUpdate((byte) 1, "contend_port_early", (int) Spectrum.tstates);
    Spectrum.tstates++;
  }

  // Handle contention for port access (late phase)
  public void contendPortLate(int port) {
    if (Machine.current.ramInfo.portFromUla.apply(port)) {
      GetTStatesHistory.addTStateUpdate((byte) (contentionNoMreq[(int) Spectrum.tstates] + 2), "ula_contend_port_late", (int) Spectrum.tstates);
      Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
      Spectrum.tstates += 2;
    } else {
      if (memory.mapRead[port >>> memory.PAGE_SIZE_LOGARITHM].contended) {
        GetTStatesHistory.addTStateUpdate((byte) (contentionNoMreq[(int) Spectrum.tstates] + 1), "ula_contend_port_late", (int) Spectrum.tstates);
        Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
        Spectrum.tstates++;
        GetTStatesHistory.addTStateUpdate((byte) (contentionNoMreq[(int) Spectrum.tstates] + 1), "ula_contend_port_late", (int) Spectrum.tstates);
        Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
        Spectrum.tstates++;
        GetTStatesHistory.addTStateUpdate((byte) (contentionNoMreq[(int) Spectrum.tstates]), "ula_contend_port_late", (int) Spectrum.tstates);
        Spectrum.tstates += contentionNoMreq[(int) Spectrum.tstates];
      } else {
        GetTStatesHistory.addTStateUpdate((byte) 2, "contend_port_late", (int) Spectrum.tstates);
        Spectrum.tstates += 2;
      }
    }
  }

}

