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

package com.fpetrola.oozx.speccy.peripherals.t;

import org.junit.jupiter.api.Test;

import javax.swing.JInternalFrame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every window clipped onto a machine does, tried on the cassette deck.
 * <p>
 * {@link RzxPlayerDockingTest} covers where such a window sits; this covers what becomes of it
 * when the machine it was clipped to goes away, which is the part that leaves something behind
 * when it is wrong: controls for a picture that is not there any more.
 */
class AttachedFrameTest {

  private static TapeBrowserInternalFrame cassette() {
    return new TapeBrowserInternalFrame(machine -> null, () -> { }, file -> { });
  }

  private static JInternalFrame machine() {
    JInternalFrame frame = new JInternalFrame("machine");
    frame.setBounds(60, 40, 520, 380);
    return frame;
  }

  @Test
  void clipped_on_it_goes_when_the_machine_goes() {
    TapeBrowserInternalFrame cassette = cassette();
    JInternalFrame machine = machine();
    cassette.setMachineWindow(machine);
    assertTrue(cassette.isAttached(), "should arrive clipped onto the machine");

    machine.dispose();
    assertTrue(cassette.isClosed(),
        "the deck was part of that machine and should have gone with it");
  }

  @Test
  void unplugged_it_stays_when_the_machine_goes() {
    TapeBrowserInternalFrame cassette = cassette();
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
    TapeBrowserInternalFrame cassette = cassette();
    assertFalse(cassette.isAttached(), "starts loose, with its lead in nothing");

    cassette.setMachineWindow(machine());
    assertTrue(cassette.isAttached(), "clipped onto a machine is plugged into it");

    cassette.setBounds(900, 700, 300, 120);
    cassette.snapIfNear();
    assertFalse(cassette.isAttached(), "carried away, it is plugged into nothing again");
  }
}
