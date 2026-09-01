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

package com.fpetrola.oozx.speccy.modules;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.sound.Beeper;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.peripherals.PeripheralBus;
import com.fpetrola.oozx.speccy.devices.ula.UlaFullDecodePeripheral;
import com.fpetrola.oozx.speccy.devices.ula.UlaPeripheral;

import java.util.function.Supplier;

@Singleton
public class Ula implements ZxModule, MachineChangeListener {
  private final Memory memory;

//  public final int CONTENTION_SIZE = 80000;
  public final int CONTENTION_SIZE = 0xFFFFF;

  // How much contention do we get at every tstate when MREQ is active?
  public final byte[] contention = new byte[CONTENTION_SIZE];

  // And how much when it is inactive
  public final byte[] contentionNoMreq = new byte[CONTENTION_SIZE];

  byte lastByte;

  // What to return if no other input pressed; depends on the last byte output to the ULA
  private byte defaultValue;
  private final Display display;
  private final Keyboard keyboard;
  private final SpectrumZ80Clock z80Clock;
  private final PeripheralBus peripherals;
  private final String contendPortLate = "contend_port_late";
  private final String contendPortEarly = "contend_port_early";
  private final Module module;
  private final Settings settings;
  private final Tape tape;
  private SpectrumMachine spectrumMachine;
  private Sound sound;
  /** The speaker, which is part of this and not a thing anybody plugged in. */
  private Beeper speaker;

@Inject
  public Ula(Memory memory, Display display, Keyboard keyboard, SpectrumZ80Clock z80Clock, PeripheralBus peripherals, Module module, Settings settings, Tape tape, Sound sound) {
    this.memory = memory;
    this.display = display;
    this.keyboard = keyboard;
    this.z80Clock = z80Clock;
    this.peripherals = peripherals;
    this.module = module;
    this.settings = settings;
    this.tape = tape;
    this.sound = sound;
  }

  // Initialize ULA module
  /**
   * Takes a speaker for the machine now running.
   * <p>
   * Made when this is switched on rather than when it is built, because the sound is set up for
   * a machine after that machine has said what it contains - the same door a sound chip comes
   * through. Every Spectrum has one of these, so it comes through it every time.
   */
  public void attachSpeaker() {
    speaker = sound.add(new Beeper(sound, tape, settings));
  }

  /** The machine this belonged to is gone, or its ULA was switched off. */
  public void detachSpeaker() {
    if (speaker != null) {
      sound.remove(speaker);
      speaker = null;
    }
  }

  public void start() {
    defaultValue = (byte) 0xff;

    return;
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
    byte read = keyboard.read((byte) (port >> 8));
    r &= read;
    if (tape.microphone) r ^= 0x40;

    int earBit = tape.getEarBit();
    if (tape.isTapeRunning()) {
//        if (earBit != 0xff)
//          System.out.println("dasgag");
      r = (byte) earBit;
    }

    return r;
  }

  // Write to ULA port
  public void write(int port, byte b) {
    lastByte = b;

    display.setLoresBorder(b & 0x07);

      // While a tape plays, its level goes to the speaker as well, which is the loading sound.
      // Sampling it here rather than on every edge is enough because a loader writes the border
      // once per edge to draw the stripes, so this runs at the rate of the signal itself.
      boolean earIn = tape.isTapePlaying() && tape.isEarHigh();
      if (speaker != null) {
        speaker.write(z80Clock.getTStates(),
            ((b & 0x10) != 0 ? 2 : 0) + ((b & 0x08) == 0 || tape.microphone || earIn ? 1 : 0),
            getCurrent().separatesTapeFromSpeaker());
      }

//    sound.beeper(z80Clock.getTStates(), (int) (Math.random() * 4));

    defaultValue = getCurrent().ulaPortIdleValue(b);
  }

  private SpectrumMachine getCurrent() {
    return spectrumMachine;
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
      addUlaStates(0, contendPortEarly);
    }
    z80Clock.addTStates((byte) 1, contendPortEarly);
  }

  public void contendPortLate(int port) {
    if (getCurrent().portFromUla(port)) {
      addUlaStates(2, contendPortLate);
    } else {
      if (memory.mapRead[port >>> memory.PAGE_SIZE_LOGARITHM].contended) {
        addUlaStates(1, contendPortLate);
        addUlaStates(1, contendPortLate);
        addUlaStates(0, contendPortLate);
      } else {
        z80Clock.addTStates(2, contendPortLate);
      }
    }
  }

  private void addUlaStates(int i, String description) {
    addUlaStates(i, () -> "ula_" + description);
  }

  public void addUlaStates(int states, Supplier<String> descriptionSupplier) {
    z80Clock.addTStates(contentionNoMreq[z80Clock.getTStates()] + states, descriptionSupplier);
  }

  public void addUlaStates(int states) {
    z80Clock.addTStates(contentionNoMreq[z80Clock.getTStates()] + states);
  }

  @Override
  public void machineChanged(SpectrumMachine newMachine) {
    spectrumMachine = newMachine;
  }
}

