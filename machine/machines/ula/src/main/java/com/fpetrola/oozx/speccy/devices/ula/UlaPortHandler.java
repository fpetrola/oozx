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
 */package com.fpetrola.oozx.speccy.devices.ula;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.keyboard.KeyMatrix;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Port 0xFE: the keys and the tape come in, the border and the speaker go out. The one port
 * every Spectrum has, however much of the address the machine decodes to find it.
 */
@Singleton
public class UlaPortHandler extends DefaultPortHandler {
  /** Bit 6 of the ULA port: what the tape is saying, as against bits 0 to 4, which are the keys. */
  private static final int EAR = 0x40;

  private final KeyMatrix keys;
  private final Tape tape;
  private final Display display;
  private final SpectrumZ80Clock z80Clock;
  private final Sound sound;
  private final Sound.Output soundOutput;
  private SpectrumMachine machine;
  private Beeper speaker;
  private byte idleValue = (byte) 0xff;

  @Inject
  public UlaPortHandler(KeyMatrix keys, Tape tape, Display display, SpectrumZ80Clock z80Clock, Sound sound, Sound.Output soundOutput) {
    super(true, true);
    this.keys = keys;
    this.tape = tape;
    this.display = display;
    this.z80Clock = z80Clock;
    this.sound = sound;
    this.soundOutput = soundOutput;
  }

  /**
   * Takes a speaker for the machine now running: made when switched on rather than when built,
   * because the sound is set up for a machine after that machine has said what it contains.
   */
  public void on(SpectrumMachine machine) {
    this.machine = machine;
    speaker = sound.add(new Beeper(sound, tape, soundOutput));
  }

  public void off() {
    if (speaker != null) {
      sound.remove(speaker);
      speaker = null;
    }
  }

  public BusAnswer read(int port) {
    byte r = (byte) (idleValue & keys.read(port >> 8));
    // Only bit 6 comes from the tape, laid over the idle value: taking the
    // whole byte from it threw the keyboard away for as long as anything was playing.
    if (tape.isEarHigh()) {
      r ^= EAR;
    }
    return BusAnswer.of(r);
  }

  public void write(int port, byte b) {
    display.border.becomes(b & 0x07);
    // While a tape plays its level goes to the speaker too, which is the loading sound. Sampled
    // here rather than on every edge because a loader writes the border once per edge anyway.
    boolean earIn = tape.isTapePlaying() && tape.isEarHigh();
    if (speaker != null) {
      speaker.write(z80Clock.getTStates(),
          ((b & 0x10) != 0 ? 2 : 0) + ((b & 0x08) == 0 || earIn ? 1 : 0),
          machine.separatesTapeFromSpeaker());
    }
    idleValue = machine.ulaPortIdleValue(b);
  }
}
