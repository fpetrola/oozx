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

package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.input.Input;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A key struck on the window reaches the machine's matrix.
 * <p>
 * The whole way, because every piece of it worked on its own while the emulator stayed deaf: the
 * table of this toolkit's key codes was read into nothing, so every key came out as no key.
 */
class TypingReachesTheMachineTest {
  private static final int KEYS = 0x1F;

  @Test
  void aKeyStruckOnTheWindowGoesDownOnTheMatrix() {
    Speccy speccy = MachineTest.silentMachine();
    SwingKeyboard typing = new SwingKeyboard(Input.of(speccy).keyboard(), Input.of(speccy));
    JPanel window = new JPanel();

    typing.keyPressed(new KeyEvent(window, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_A, 'a'));
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xFDFE) & KEYS, "A is down");

    typing.keyReleased(new KeyEvent(window, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_A, 'a'));
    assertEquals(KEYS, speccy.ports.read(0xFDFE) & KEYS, "and up again");
  }

  @Test
  void aKeyThatNeedsAShiftBringsItsShift() {
    Speccy speccy = MachineTest.silentMachine();
    SwingKeyboard typing = new SwingKeyboard(Input.of(speccy).keyboard(), Input.of(speccy));
    JPanel window = new JPanel();

    typing.keyPressed(new KeyEvent(window, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_BACK_SPACE, '\b'));
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xEFFE) & KEYS, "backspace holds 0");
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xFEFE) & KEYS, "and caps shift with it");
  }
}
