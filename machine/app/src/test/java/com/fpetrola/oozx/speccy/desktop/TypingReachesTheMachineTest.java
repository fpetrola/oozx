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
