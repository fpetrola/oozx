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

/** Port 0xFE: keys and tape in, border colour and speaker out - present on every Spectrum
 * model, regardless of how much of the address it decodes. */
@Singleton
public class UlaPortHandler extends DefaultPortHandler {
  /** Tape-in bit position within the port byte; bits 0-4 are the keyboard. */
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

  /** Creates this handler's speaker on activation, not construction, since sound setup needs
   * the machine to have already declared its hardware. */
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
    // Only the tape bit is overlaid on the idle value; substituting the whole byte would
    // mask the keyboard for as long as a tape played.
    if (tape.isEarHigh()) {
      r ^= EAR;
    }
    return BusAnswer.of(r);
  }

  public void write(int port, byte b) {
    display.border.becomes(b & 0x07);
    // Tape audio is mixed in on every border write, since a loader already toggles the
    // border once per tape edge anyway.
    boolean earIn = tape.isTapePlaying() && tape.isEarHigh();
    if (speaker != null) {
      speaker.write(z80Clock.getTStates(),
          ((b & 0x10) != 0 ? 2 : 0) + ((b & 0x08) == 0 || earIn ? 1 : 0),
          machine.separatesTapeFromSpeaker());
    }
    idleValue = machine.ulaPortIdleValue(b);
  }
}
