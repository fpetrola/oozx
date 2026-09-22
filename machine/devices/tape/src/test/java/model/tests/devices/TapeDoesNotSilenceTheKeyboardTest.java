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

package model.tests.devices;

import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.Speccy;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Regression: port 0xFE must report both tape (bit 6) and keyboard (bits 0-4) simultaneously; a
 * prior implementation returned only the tape byte while a tape was "running", silently
 * disabling the keyboard for as long as a cassette player stayed connected to the ear line.
 */
class TapeDoesNotSilenceTheKeyboardTest {

  private static final int KEYBOARD_PORT = 0xFEFE;
  private static final int EAR = 0x40;
  private static final int KEYS = 0x1F;

  /**
   * With a key HELD DOWN, which is the only way this can be seen: with none held, every key bit
   * reads high and the wrong answer looks exactly like the right one.
   */
  @Test
  void a_key_being_held_is_still_there_while_something_drives_the_ear_line() {
    Speccy speccy = MachineTest.silentMachine();

    speccy.keys.press(SpectrumKey.Z);   // Z is on the row this port reads
    int held = speccy.ports.read(KEYBOARD_PORT) & 0xFF;
    assertNotEquals(KEYS, held & KEYS, "the test needs a key that actually shows in this port");

    Tape.of(speccy).takeEarFrom(() -> false);
    int whileDriven = speccy.ports.read(KEYBOARD_PORT) & 0xFF;

    assertEquals(held & KEYS, whileDriven & KEYS,
        "a key held down must still read as held while something drives the ear line");
  }

  @Test
  void the_ear_line_still_comes_from_whatever_is_driving_it() {
    Speccy speccy = MachineTest.silentMachine();

    Tape.of(speccy).takeEarFrom(() -> true);
    Tape.of(speccy).setEarBit(true);
    int high = speccy.ports.read(KEYBOARD_PORT) & 0xFF;

    Tape.of(speccy).setEarBit(false);
    int low = speccy.ports.read(KEYBOARD_PORT) & 0xFF;

    assertNotEquals(high & EAR, low & EAR, "bit 6 must follow what is driving the ear line");
    assertEquals(high & KEYS, low & KEYS, "and nothing else about the port may move with it");
  }
}
