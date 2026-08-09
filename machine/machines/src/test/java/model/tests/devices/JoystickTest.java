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
import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick.JoystickButton;
import com.fpetrola.oozx.speccy.modules.joystick.Joystick.JoystickType;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What each kind of joystick does when a direction is pushed: a Kempston and a Fuller answer on a
 * port of their own, and the ones the Spectrum had no port for hold keys down instead.
 * <p>
 * Written before the joystick was taken apart, said in what a program can see: the byte a port
 * gives back, or the bits of the keyboard's half rows.
 */
class JoystickTest extends MachineTest {
  private static final int KEYS = 0x1F;
  private static final JoystickButton[] DIRECTIONS = {
      JoystickButton.JOYSTICK_BUTTON_LEFT, JoystickButton.JOYSTICK_BUTTON_RIGHT,
      JoystickButton.JOYSTICK_BUTTON_UP, JoystickButton.JOYSTICK_BUTTON_DOWN,
      JoystickButton.JOYSTICK_BUTTON_FIRE};

  private static Speccy on(JoystickType type) {
    Speccy speccy = MachineTest.silentMachine();
    Input.of(speccy).setup.keyboard.output = type;
    return speccy;
  }

  private static void push(Speccy speccy, JoystickButton button, boolean pressed) {
    Input.of(speccy).joystick().press(Input.of(speccy).joystick().JOYSTICK_KEYBOARD, button, pressed);
  }

  /** A Kempston is read on a port, one bit per direction, high while pushed. */
  @Test
  void aKempstonAnswersOnItsPortWithABitPerDirection() {
    Speccy speccy = on(JoystickType.JOYSTICK_TYPE_KEMPSTON);
    int[] bits = {0x02, 0x01, 0x08, 0x04, 0x10};
    for (int i = 0; i < DIRECTIONS.length; i++) {
      push(speccy, DIRECTIONS[i], true);
      assertEquals(bits[i], Input.of(speccy).joystick().kempstonRead(0x1F).value() & 0xFF, DIRECTIONS[i] + " pushed");
      push(speccy, DIRECTIONS[i], false);
      assertEquals(0, Input.of(speccy).joystick().kempstonRead(0x1F).value() & 0xFF, DIRECTIONS[i] + " let go");
    }
  }

  @Test
  void aKempstonHoldsSeveralDirectionsAtOnce() {
    Speccy speccy = on(JoystickType.JOYSTICK_TYPE_KEMPSTON);
    push(speccy, JoystickButton.JOYSTICK_BUTTON_UP, true);
    push(speccy, JoystickButton.JOYSTICK_BUTTON_FIRE, true);
    assertEquals(0x18, Input.of(speccy).joystick().kempstonRead(0x1F).value() & 0xFF, "up and fire together");
  }

  /** A Fuller is the other way round: its bits sit high and a push pulls one low. */
  @Test
  void aFullerAnswersWithItsBitsTheOtherWayRound() {
    Speccy speccy = on(JoystickType.JOYSTICK_TYPE_FULLER);
    assertEquals(0xFF, Input.of(speccy).joystick().fullerRead(0x7F).value() & 0xFF, "nothing pushed");
    push(speccy, JoystickButton.JOYSTICK_BUTTON_FIRE, true);
    assertEquals(0x7F, Input.of(speccy).joystick().fullerRead(0x7F).value() & 0xFF, "fire pulls bit 7 low");
    push(speccy, JoystickButton.JOYSTICK_BUTTON_FIRE, false);
    assertEquals(0xFF, Input.of(speccy).joystick().fullerRead(0x7F).value() & 0xFF, "and lets it go");
  }

  @Test
  void aTimexAnswersOnOneOfItsTwoPorts() {
    Speccy speccy = on(JoystickType.JOYSTICK_TYPE_TIMEX_1);
    push(speccy, JoystickButton.JOYSTICK_BUTTON_UP, true);
    assertEquals(0x01, Input.of(speccy).joystick().timexRead(0, 0) & 0xFF, "up on the first Timex port");
    assertEquals(0x00, Input.of(speccy).joystick().timexRead(0, 1) & 0xFF, "and nothing on the second");

    Speccy second = on(JoystickType.JOYSTICK_TYPE_TIMEX_2);
    push(second, JoystickButton.JOYSTICK_BUTTON_DOWN, true);
    assertEquals(0x02, Input.of(second).joystick().timexRead(0, 1) & 0xFF, "down on the second");
    assertEquals(0x00, Input.of(second).joystick().timexRead(0, 0) & 0xFF, "and nothing on the first");
  }

  /** The joysticks the Spectrum had no port for are keys held down, and each kind holds its own. */
  @Test
  void theKeyboardJoysticksHoldTheKeysTheirMakersChose() {
    Object[][] kinds = {
        // type, left, right, up, down, fire - each as the half row and bit the key sits on
        {JoystickType.JOYSTICK_TYPE_CURSOR, 0xF7, 0x10, 0xEF, 0x04, 0xEF, 0x08, 0xEF, 0x10, 0xEF, 0x01},
        {JoystickType.JOYSTICK_TYPE_SINCLAIR_1, 0xEF, 0x10, 0xEF, 0x08, 0xEF, 0x02, 0xEF, 0x04, 0xEF, 0x01},
        {JoystickType.JOYSTICK_TYPE_SINCLAIR_2, 0xF7, 0x01, 0xF7, 0x02, 0xF7, 0x08, 0xF7, 0x04, 0xF7, 0x10},
    };
    for (Object[] kind : kinds) {
      Speccy speccy = on((JoystickType) kind[0]);
      for (int i = 0; i < DIRECTIONS.length; i++) {
        int high = (Integer) kind[1 + i * 2], bit = (Integer) kind[2 + i * 2];
        push(speccy, DIRECTIONS[i], true);
        assertEquals(KEYS & ~bit, speccy.ports.read(high << 8 | 0xFE) & KEYS,
            kind[0] + " " + DIRECTIONS[i] + " holds its key");
        push(speccy, DIRECTIONS[i], false);
        assertEquals(KEYS, speccy.ports.read(high << 8 | 0xFE) & KEYS,
            kind[0] + " " + DIRECTIONS[i] + " lets it go");
      }
    }
  }

  /** Nothing plugged in takes nothing: the key goes on to the machine's own keyboard. */
  @Test
  void aJoystickThatIsNotThereTakesNothing() {
    Speccy speccy = on(JoystickType.JOYSTICK_TYPE_NONE);
    assertFalse(Input.of(speccy).joystick().press(Input.of(speccy).joystick().JOYSTICK_KEYBOARD, JoystickButton.JOYSTICK_BUTTON_UP, true),
        "with no joystick, the key is not the joystick's");
    assertTrue(Input.of(speccy).joystick().press(0, JoystickButton.JOYSTICK_BUTTON_UP, true),
        "a pad with nothing chosen answers as a Kempston, which is what it did");
  }

  /** Each socket has its own kind, and pushing one does not move the other. */
  @Test
  void thePadsAndTheKeyboardAreThreeDifferentSockets() {
    Speccy speccy = MachineTest.silentMachine();
    Input.of(speccy).setup.joystick1.output = JoystickType.JOYSTICK_TYPE_KEMPSTON;
    Input.of(speccy).setup.joystick2.output = JoystickType.JOYSTICK_TYPE_CURSOR;

    Input.of(speccy).joystick().press(0, JoystickButton.JOYSTICK_BUTTON_FIRE, true);
    assertEquals(0x10, Input.of(speccy).joystick().kempstonRead(0x1F).value() & 0xFF, "the first pad is a Kempston");

    Input.of(speccy).joystick().press(1, JoystickButton.JOYSTICK_BUTTON_FIRE, true);
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xEFFE) & KEYS, "the second holds 0, which is Cursor fire");
  }
}
