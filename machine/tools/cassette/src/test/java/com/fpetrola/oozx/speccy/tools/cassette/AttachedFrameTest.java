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

package com.fpetrola.oozx.speccy.tools.cassette;

import com.fpetrola.oozx.speccy.windows.AttachedFrame;
import org.junit.jupiter.api.Test;

import javax.swing.JInternalFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every window clipped onto a machine does, tried on the cassette deck.
 * <p>
 * The application's RzxPlayerDockingTest covers where such a window sits; this covers what becomes of it
 * when the machine it was clipped to goes away, which is the part that leaves something behind
 * when it is wrong: controls for a picture that is not there any more.
 */
class AttachedFrameTest {

  private static CassetteFrame cassette() {
    return new CassetteFrame();
  }

  private static JInternalFrame machine() {
    JInternalFrame frame = new JInternalFrame("machine");
    frame.setBounds(60, 40, 520, 380);
    return frame;
  }

  @Test
  void clipped_on_it_goes_when_the_machine_goes() {
    CassetteFrame cassette = cassette();
    JInternalFrame machine = machine();
    cassette.setMachineWindow(machine);
    assertTrue(cassette.isAttached(), "should arrive clipped onto the machine");

    machine.dispose();
    assertTrue(cassette.isClosed(),
        "the deck was part of that machine and should have gone with it");
  }

  @Test
  void unplugged_it_stays_when_the_machine_goes() {
    CassetteFrame cassette = cassette();
    JInternalFrame machine = machine();
    cassette.setMachineWindow(machine);

    // Carried away from the computer: it is a window of its own now and outlives the machine.
    cassette.setBounds(900, 700, 300, 120);
    cassette.snapIfNear();
    assertEquals(AttachedFrame.Dock.FREE, cassette.dockedTo(), "should have let go");

    machine.dispose();
    assertFalse(cassette.isClosed(), "a deck nobody plugged in should outlive the machine");
  }

  @Test
  void being_clipped_on_is_what_plugs_it_in() {
    CassetteFrame cassette = cassette();
    assertFalse(cassette.isAttached(), "starts loose, with its lead in nothing");

    cassette.setMachineWindow(machine());
    assertTrue(cassette.isAttached(), "clipped onto a machine is plugged into it");

    cassette.setBounds(900, 700, 300, 120);
    cassette.snapIfNear();
    assertFalse(cassette.isAttached(), "carried away, it is plugged into nothing again");
  }
}
