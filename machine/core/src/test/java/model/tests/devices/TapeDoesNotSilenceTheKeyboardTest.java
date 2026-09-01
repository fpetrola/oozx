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

package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.bridge.SpeccyBaseForTests;
import org.junit.jupiter.api.Test;

import com.fpetrola.oozx.speccy.KeyboardKeyName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * One port answers about the tape AND about the keyboard, and the ULA has to say both.
 * <p>
 * Bit 6 of port 0xFE is what the tape is saying; bits 0 to 4 are which keys are down. While a
 * tape was running the read handed back the tape's byte and nothing else, so the keys went out
 * with it. Nobody noticed while the only thing that made a tape "running" was a file playing -
 * you do not type during a load - but a real cassette player wired to the ear line is running
 * for as long as it is plugged in, and with it plugged in the machine was deaf.
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
    Speccy speccy = SpeccyBaseForTests.createSpeccy();
    speccy.init();

    speccy.keyboard.press(KeyboardKeyName.KEYBOARD_z);   // Z is on the row this port reads
    int held = speccy.peripherals.readPort(KEYBOARD_PORT) & 0xFF;
    assertNotEquals(KEYS, held & KEYS, "the test needs a key that actually shows in this port");

    speccy.tape.takeEarFrom(() -> false);
    int whileDriven = speccy.peripherals.readPort(KEYBOARD_PORT) & 0xFF;

    assertEquals(held & KEYS, whileDriven & KEYS,
        "a key held down must still read as held while something drives the ear line");
  }

  @Test
  void the_ear_line_still_comes_from_whatever_is_driving_it() {
    Speccy speccy = SpeccyBaseForTests.createSpeccy();
    speccy.init();

    speccy.tape.takeEarFrom(() -> true);
    speccy.tape.setEarBit(true);
    int high = speccy.peripherals.readPort(KEYBOARD_PORT) & 0xFF;

    speccy.tape.setEarBit(false);
    int low = speccy.peripherals.readPort(KEYBOARD_PORT) & 0xFF;

    assertNotEquals(high & EAR, low & EAR, "bit 6 must follow what is driving the ear line");
    assertEquals(high & KEYS, low & KEYS, "and nothing else about the port may move with it");
  }
}
