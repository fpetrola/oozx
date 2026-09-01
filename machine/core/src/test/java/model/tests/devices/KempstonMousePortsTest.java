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
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.devices.mouse.KempstonMouse;
import com.fpetrola.oozx.speccy.devices.mouse.KempstonMousePeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import com.fpetrola.oozx.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Where the mouse answers, which is the whole of what a program has to agree with it about.
 * <p>
 * These three addresses are not ours to choose: software written in 1987 reads 0xFADF, 0xFBDF
 * and 0xFFDF and will find nothing if we decode them differently. The patterns come from Fuse's
 * peripherals/kempmouse.c, and this is what says we copied them correctly rather than plausibly.
 */
class KempstonMousePortsTest {

  private static final int BUTTONS = 0xFADF;
  private static final int X = 0xFBDF;
  private static final int Y = 0xFFDF;

  private Speccy plugged(boolean connected) {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    mouseOf(speccy).plugIn(connected);
    speccy.machine.select(speccy.spec48);
    speccy.peripherals.update();
    return speccy;
  }

  private static KempstonMousePeripheral mouseOf(Speccy speccy) {
    return (KempstonMousePeripheral) speccy.peripherals.find(KempstonMousePeripheral.class);
  }

  private static int read(Speccy speccy, int port) {
    return speccy.peripherals.readPort(port) & 0xFF;
  }

  @Test
  void nobody_asked_for_a_mouse_so_there_is_none() {
    Speccy speccy = plugged(false);
    assertFalse(speccy.peripherals.isActive(KempstonMousePeripheral.class),
        "a mouse that was never plugged in should not be on the bus");
  }

  @Test
  void the_three_ports_answer_where_the_software_of_the_day_looks() {
    Speccy speccy = plugged(true);
    KempstonMouse mouse = mouseOf(speccy).mouse();

    mouse.moved(7, 0);
    assertEquals(7, read(speccy, X), "the horizontal count is read at 0xFBDF");
    assertEquals(0, read(speccy, Y), "and moving sideways must not move the other one");

    mouse.moved(0, 5);
    assertEquals(7, read(speccy, X), "moving up and down must not move the sideways count");
    // Down on the desk counts down, which is Fuse's pos.y -= dy: the screen and the mouse
    // disagree about which way up is, and the mouse is the one that is right about itself.
    assertEquals(0xFB, read(speccy, Y), "five up from zero wraps round the other way");
  }

  @Test
  void a_button_held_reads_as_a_bit_that_is_down() {
    Speccy speccy = plugged(true);
    KempstonMouse mouse = mouseOf(speccy).mouse();

    assertEquals(0xFF, read(speccy, BUTTONS), "all three rest high, which means nothing held");

    mouse.button(0, true);
    assertEquals(0xFE, read(speccy, BUTTONS), "the left button is bit 0, and held means low");

    mouse.button(1, true);
    assertEquals(0xFC, read(speccy, BUTTONS), "the right button is bit 1");

    mouse.button(0, false);
    assertEquals(0xFD, read(speccy, BUTTONS), "letting go puts its bit back up and no other");
  }

  @Test
  void the_counts_wrap_rather_than_stop() {
    Speccy speccy = plugged(true);
    KempstonMouse mouse = mouseOf(speccy).mouse();

    // A program reads the difference from what it read last, so a counter that stopped at its
    // end would freeze the pointer against an edge that does not exist on the desk.
    mouse.moved(300, 0);
    assertEquals(300 & 0xFF, read(speccy, X), "the count wraps at eight bits");
    mouse.moved(-1, 0);
    assertNotEquals(300 & 0xFF, read(speccy, X), "and goes backwards as happily as forwards");
  }
}
