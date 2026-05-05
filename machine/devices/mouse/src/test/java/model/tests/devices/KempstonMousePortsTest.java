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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.devices.mouse.KempstonMouse;
import com.fpetrola.oozx.speccy.devices.mouse.KempstonMousePeripheral;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Fixed Kempston Mouse port addresses (0xFADF/0xFBDF/0xFFDF): existing 1987-era software reads
 * exactly these, so decoding must match the hardware precisely, not just plausibly.
 */
class KempstonMousePortsTest {

  private static final int BUTTONS = 0xFADF;
  private static final int X = 0xFBDF;
  private static final int Y = 0xFFDF;

  private Speccy plugged(boolean connected) {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    mouseOf(speccy).plugIn(connected);
    speccy.machine.select(speccy.machine.model(Spec48.class));
    speccy.peripheralRegistry.update();
    return speccy;
  }

  private static KempstonMousePeripheral mouseOf(Speccy speccy) {
    return (KempstonMousePeripheral) speccy.peripheralRegistry.find(KempstonMousePeripheral.class);
  }

  private static int read(Speccy speccy, int port) {
    return speccy.ports.read(port) & 0xFF;
  }

  @Test
  void nobody_asked_for_a_mouse_so_there_is_none() {
    Speccy speccy = plugged(false);
    assertFalse(speccy.peripheralRegistry.isActive(KempstonMousePeripheral.class),
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
    // Physical mouse Y increases moving down, opposite of screen coordinates.
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

  /** Motion beyond what two reads can carry is simply dropped, matching a real mouse's
   * resolution limit, rather than queued and delivered late. */
  @Test
  void a_stroke_faster_than_the_mouse_resolves_is_not_reported_at_all() {
    Speccy speccy = plugged(true);
    KempstonMouse mouse = mouseOf(speccy).mouse();

    mouse.moved(300, 0);                        // far exceeds what any single reading can carry

    assertEquals(127, read(speccy, X), "the first reading carries as much as a reading can");
    assertEquals(254, read(speccy, X), "and the second the rest of what was kept");
    assertEquals(254, read(speccy, X), "the other 46 were never resolved, so they never arrive");
    assertEquals(0, mouse.owed(), "and nothing is left owing to arrive late");
  }

  @Test
  void the_counts_wrap_rather_than_stop() {
    Speccy speccy = plugged(true);
    KempstonMouse mouse = mouseOf(speccy).mouse();

    // A stopping counter would falsely imply an edge, so it must wrap instead; 300 exceeds one
    // reading's capacity, so it is split into three within-limit strokes here.
    for (int stroke = 0; stroke < 3; stroke++) {
      mouse.moved(100, 0);
      read(speccy, X);
    }
    assertEquals(300 & 0xFF, read(speccy, X), "the count wraps at eight bits");
    mouse.moved(-1, 0);
    assertNotEquals(300 & 0xFF, read(speccy, X), "and goes backwards as happily as forwards");
  }
}
