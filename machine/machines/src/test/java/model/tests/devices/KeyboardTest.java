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
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;
import model.harness.MachineTest;
import com.fpetrola.oozx.config.Configuration;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Keyboard behaviour purely as a program observes it: the 5 key bits the ULA returns per half
 * row. Written before a planned refactor, using no internal class beyond pressing a key, so it
 * pins behaviour rather than the current table layout.
 */
class KeyboardTest extends MachineTest {
  /** High-byte value selecting each half row, in the Spectrum's actual wiring order. */
  private static final int[] HALF_ROWS = {0xFE, 0xFD, 0xFB, 0xF7, 0xEF, 0xDF, 0xBF, 0x7F};
  private static final int KEYS = 0x1F;

  /** Snapshots the key bits of every half row at once. */
  private static Map<Integer, Integer> matrixOf(Speccy speccy) {
    Map<Integer, Integer> rows = new LinkedHashMap<>();
    for (int high : HALF_ROWS) {
      rows.put(high, speccy.ports.read(high << 8 | 0xFE) & KEYS);
    }
    return rows;
  }

  private static void assertOnly(Speccy speccy, int high, int bit, String what) {
    Map<Integer, Integer> rows = matrixOf(speccy);
    for (Map.Entry<Integer, Integer> row : rows.entrySet()) {
      int expected = row.getKey() == high ? KEYS & ~bit : KEYS;
      assertEquals(expected, (int) row.getValue(),
          what + ": half row " + Integer.toHexString(row.getKey()) + " should read " + Integer.toBinaryString(expected));
    }
  }

  @Test
  void nothingHeldReadsEveryBitHigh() {
    Speccy speccy = silentMachine();
    for (int high : HALF_ROWS) {
      assertEquals(KEYS, speccy.ports.read(high << 8 | 0xFE) & KEYS,
          "with no key down, half row " + Integer.toHexString(high) + " is all ones");
    }
  }

  /** Full key-to-matrix-position mapping for the entire keyboard. */
  @Test
  void everyKeySitsWhereTheSpectrumPutsIt() {
    Object[][] matrix = {
        {SpectrumKey.CAPS_SHIFT, 0xFE, 0x01}, {SpectrumKey.Z, 0xFE, 0x02},
        {SpectrumKey.X, 0xFE, 0x04}, {SpectrumKey.C, 0xFE, 0x08},
        {SpectrumKey.V, 0xFE, 0x10},
        {SpectrumKey.A, 0xFD, 0x01}, {SpectrumKey.S, 0xFD, 0x02},
        {SpectrumKey.D, 0xFD, 0x04}, {SpectrumKey.F, 0xFD, 0x08},
        {SpectrumKey.G, 0xFD, 0x10},
        {SpectrumKey.Q, 0xFB, 0x01}, {SpectrumKey.W, 0xFB, 0x02},
        {SpectrumKey.E, 0xFB, 0x04}, {SpectrumKey.R, 0xFB, 0x08},
        {SpectrumKey.T, 0xFB, 0x10},
        {SpectrumKey.ONE, 0xF7, 0x01}, {SpectrumKey.TWO, 0xF7, 0x02},
        {SpectrumKey.THREE, 0xF7, 0x04}, {SpectrumKey.FOUR, 0xF7, 0x08},
        {SpectrumKey.FIVE, 0xF7, 0x10},
        {SpectrumKey.ZERO, 0xEF, 0x01}, {SpectrumKey.NINE, 0xEF, 0x02},
        {SpectrumKey.EIGHT, 0xEF, 0x04}, {SpectrumKey.SEVEN, 0xEF, 0x08},
        {SpectrumKey.SIX, 0xEF, 0x10},
        {SpectrumKey.P, 0xDF, 0x01}, {SpectrumKey.O, 0xDF, 0x02},
        {SpectrumKey.I, 0xDF, 0x04}, {SpectrumKey.U, 0xDF, 0x08},
        {SpectrumKey.Y, 0xDF, 0x10},
        {SpectrumKey.ENTER, 0xBF, 0x01}, {SpectrumKey.L, 0xBF, 0x02},
        {SpectrumKey.K, 0xBF, 0x04}, {SpectrumKey.J, 0xBF, 0x08},
        {SpectrumKey.H, 0xBF, 0x10},
        {SpectrumKey.SPACE, 0x7F, 0x01}, {SpectrumKey.SYMBOL_SHIFT, 0x7F, 0x02},
        {SpectrumKey.M, 0x7F, 0x04}, {SpectrumKey.N, 0x7F, 0x08},
        {SpectrumKey.B, 0x7F, 0x10},
    };
    Speccy speccy = silentMachine();
    for (Object[] placed : matrix) {
      SpectrumKey key = (SpectrumKey) placed[0];
      Input.of(speccy).keyboard().press(key);
      assertOnly(speccy, (Integer) placed[1], (Integer) placed[2], key + " held");
      Input.of(speccy).keyboard().release(key);
    }
    assertEquals(KEYS, speccy.ports.read(0xFEFE) & KEYS, "and everything is up again at the end");
  }

  /** Selecting multiple half rows at once ANDs their results, matching the physical wiring. */
  @Test
  void aPortThatSelectsSeveralRowsReadsThemTogether() {
    Speccy speccy = silentMachine();
    Input.of(speccy).keyboard().press(SpectrumKey.Z);       // half row 0xFE, key bit 0x02
    Input.of(speccy).keyboard().press(SpectrumKey.SPACE);   // half row 0x7F, key bit 0x01

    assertEquals(KEYS & ~0x02, speccy.ports.read(0xFEFE) & KEYS, "Z alone on its row");
    assertEquals(KEYS & ~0x01, speccy.ports.read(0x7FFE) & KEYS, "space alone on its row");
    assertEquals(KEYS & ~0x03, speccy.ports.read(0x7EFE) & KEYS, "both rows at once give both keys");
    assertEquals(KEYS & ~0x03, speccy.ports.read(0x00FE) & KEYS, "every row at once gives everything held");
  }

  @Test
  void twoKeysOnOneRowShowTogetherAndReleaseOneAtATime() {
    Speccy speccy = silentMachine();
    Input.of(speccy).keyboard().press(SpectrumKey.Z);
    Input.of(speccy).keyboard().press(SpectrumKey.C);
    assertEquals(KEYS & ~0x0A, speccy.ports.read(0xFEFE) & KEYS, "Z and C together");

    Input.of(speccy).keyboard().release(SpectrumKey.Z);
    assertEquals(KEYS & ~0x08, speccy.ports.read(0xFEFE) & KEYS, "C still down");
    Input.of(speccy).keyboard().release(SpectrumKey.C);
    assertEquals(KEYS, speccy.ports.read(0xFEFE) & KEYS, "and nothing is down");
  }

  @Test
  void pressingTheSameKeyTwiceAndReleasingItOnceLeavesItUp() {
    Speccy speccy = silentMachine();
    Input.of(speccy).keyboard().press(SpectrumKey.A);
    Input.of(speccy).keyboard().press(SpectrumKey.A);
    Input.of(speccy).keyboard().release(SpectrumKey.A);
    assertEquals(KEYS, speccy.ports.read(0xFDFE) & KEYS, "one release is enough, as the hardware has it");
  }

  @Test
  void releasingEverythingLetsGoOfEveryRow() {
    Speccy speccy = silentMachine();
    Input.of(speccy).keyboard().press(SpectrumKey.Q);
    Input.of(speccy).keyboard().press(SpectrumKey.ENTER);
    Input.of(speccy).keyboard().releaseAll();
    for (int high : HALF_ROWS) {
      assertEquals(KEYS, speccy.ports.read(high << 8 | 0xFE) & KEYS,
          "after releasing everything, half row " + Integer.toHexString(high));
    }
  }

  /** Maps a host key to the Spectrum key(s) it produces, including any implied shift. */
  @Test
  void aKeyOfTheHostProducesTheKeysTheSpectrumHasForIt() {
    Speccy speccy = silentMachine();
    Object[][] expected = {
        {Input.InputKey.INPUT_KEY_a, new SpectrumKey[]{SpectrumKey.A}},
        {Input.InputKey.INPUT_KEY_Return, new SpectrumKey[]{SpectrumKey.ENTER}},
        {Input.InputKey.INPUT_KEY_BackSpace, new SpectrumKey[]{SpectrumKey.CAPS_SHIFT, SpectrumKey.ZERO}},
        {Input.InputKey.INPUT_KEY_Escape, new SpectrumKey[]{SpectrumKey.CAPS_SHIFT, SpectrumKey.ONE}},
        {Input.InputKey.INPUT_KEY_period, new SpectrumKey[]{SpectrumKey.SYMBOL_SHIFT, SpectrumKey.M}},
        {Input.InputKey.INPUT_KEY_Shift_L, new SpectrumKey[]{SpectrumKey.CAPS_SHIFT}},
        {Input.InputKey.INPUT_KEY_Control_L, new SpectrumKey[]{SpectrumKey.SYMBOL_SHIFT}},
        {Input.InputKey.INPUT_KEY_Left, new SpectrumKey[]{SpectrumKey.FIVE}},
    };
    for (Object[] row : expected) {
      assertArrayEquals((SpectrumKey[]) row[1], Input.of(speccy).keyboard().produces((Input.InputKey) row[0]).keys(),
          row[0] + " produces");
    }
  }
  /** A simulated host keypress reaches the matrix via Input, shift key included. */
  @Test
  void typingAKeyOfTheHostShowsUpOnTheMatrix() {
    Speccy speccy = silentMachine();
    type(speccy, Input.InputKey.INPUT_KEY_BackSpace, true);
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xEFFE) & KEYS, "backspace holds 0");
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xFEFE) & KEYS, "and caps shift with it");

    type(speccy, Input.InputKey.INPUT_KEY_BackSpace, false);
    assertEquals(KEYS, speccy.ports.read(0xEFFE) & KEYS, "let go of 0");
    assertEquals(KEYS, speccy.ports.read(0xFEFE) & KEYS, "and of caps shift");
  }

  /** The arrows-shifted option makes a cursor key also hold caps shift. */
  @Test
  void theArrowsCanBeAskedToCarryCapsShift() {
    Speccy speccy = silentMachine();
    Input.of(speccy).setup.arrowsShifted = true;
    type(speccy, Input.InputKey.INPUT_KEY_Left, true);
    assertEquals(KEYS & ~0x10, speccy.ports.read(0xF7FE) & KEYS, "left is 5");
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xFEFE) & KEYS, "and caps shift is held with it");

    type(speccy, Input.InputKey.INPUT_KEY_Left, false);
    assertEquals(KEYS, speccy.ports.read(0xF7FE) & KEYS, "both let go");
    assertEquals(KEYS, speccy.ports.read(0xFEFE) & KEYS);
  }

  /** A key bound to a joystick direction is routed there instead of to the key matrix. */
  @Test
  void aKeyGivenToTheJoystickDoesNotReachTheMatrix() {
    Speccy speccy = silentMachine();
    Input.of(speccy).setup.keyboard.output = Joystick.JoystickType.JOYSTICK_TYPE_CURSOR;
    Input.of(speccy).setup.keyboard.left = Input.InputKey.INPUT_KEY_z;
    speccy.peripheralRegistry.update();

    type(speccy, Input.InputKey.INPUT_KEY_z, true);
    assertEquals(KEYS & ~0x10, speccy.ports.read(0xF7FE) & KEYS, "left on the cursor joystick is 5");
    assertEquals(KEYS, speccy.ports.read(0xFEFE) & KEYS, "and Z itself never went down");
  }

  /** An unbound joystick direction defaults to INPUT_KEY_NONE (0); a bug once matched an
   * unmapped key against that same zero value and swallowed it instead of passing it through. */
  @Test
  void aKeyTheJoystickWasNotGivenStillReachesTheMachine() {
    Speccy speccy = silentMachine();
    type(speccy, Input.InputKey.INPUT_KEY_a, true);
    assertEquals(KEYS & ~0x01, speccy.ports.read(0xFDFE) & KEYS, "A goes down while no direction is set to it");
  }

  private static void type(Speccy speccy, Input.InputKey key, boolean down) {
    Input.of(speccy).event(new Input.InputEvent(
        down ? Input.InputEventType.INPUT_EVENT_KEYPRESS : Input.InputEventType.INPUT_EVENT_KEYRELEASE,
        new Input.InputEventKey(key, key)));
  }

  /** Config JSON overrides only the input keys it names; unmentioned keys keep their defaults. */
  @Test
  void theFileReachesTheSetup() throws Exception {
    Path file = Files.createTempFile("oozx", ".json");
    Files.writeString(file, "{\"input\": {\"arrowsShifted\": true, \"keyboard\": {\"output\": \"JOYSTICK_TYPE_CURSOR\"}}}");
    Speccy speccy = silentMachine(binder -> binder.bind(Configuration.class).toInstance(new Configuration(file.toFile())));
    Input.Setup setup = Input.of(speccy).setup;
    assertTrue(setup.arrowsShifted);
    assertEquals(Joystick.JoystickType.JOYSTICK_TYPE_CURSOR, setup.keyboard.output);
    setup.keyboard.fire = Input.InputKey.INPUT_KEY_space;
    Input.Setup.Keys onlyUp = new Input.Setup.Keys();
    onlyUp.up = Input.InputKey.INPUT_KEY_q;
    setup.setKeyboard(onlyUp);
    assertEquals(Input.InputKey.INPUT_KEY_q, setup.keyboard.up);
    assertEquals(Input.InputKey.INPUT_KEY_space, setup.keyboard.fire, "a key the file did not name is still the machine's own");
  }
}
