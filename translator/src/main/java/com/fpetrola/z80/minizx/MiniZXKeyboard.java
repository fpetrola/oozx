/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.z80.minizx;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;

public class MiniZXKeyboard implements KeyListener {

  public enum JoystickModel {
    NONE, KEMPSTON, SINCLAIR1, SINCLAIR2, CURSOR, FULLER
  }

  private final int[] rowKey = new int[8];
  private int sjs1;
  private int sjs2;
  private boolean shiftPressed;
  private boolean mapPCKeys;
  private final boolean winBug;
  private final KeyEvent[] keyEventPending = new KeyEvent[8];
  private int kempston, fuller;
  private final JoystickModel joystickModel;
  private JoystickModel shadowJoystick;
  private int capsShiftCounter = 0;

  /*
   * Spectrum Keyboard Map
   *
   * PORT  |  BIT 4   3   2   1   0
   * -----------------------------------------
   * 254 (FEh)    V   C   X   Z   CAPS
   * -----------------------------------------
   * 253 (FDh)    G   F   D   S   A
   * -----------------------------------------
   * 251 (FBh)    T   R   E   W   Q
   * -----------------------------------------
   * 247 (F7h)    5   4   3   2   1
   * -----------------------------------------
   * 239 (EFh)    6   7   8   9   0
   * -----------------------------------------
   * 223 (FDh)    Y   U   I   O   P
   * -----------------------------------------
   * 191 (BFh)    H   J   K   L   ENTER
   * -----------------------------------------
   * 127 (7Fh)    B   N   M   SYM BREAK/SPACE
   * -----------------------------------------
   *
   */
  private static final int KEY_PRESSED_BIT0 = 0xfe;
  private static final int KEY_PRESSED_BIT1 = 0xfd;
  private static final int KEY_PRESSED_BIT2 = 0xfb;
  private static final int KEY_PRESSED_BIT3 = 0xf7;
  private static final int KEY_PRESSED_BIT4 = 0xef;
  private static final int KEY_PRESSED_BIT7 = 0x7f;  // for Fuller fire button
  private static final int KEY_RELEASED_BIT0 = 0x01;
  private static final int KEY_RELEASED_BIT1 = 0x02;
  private static final int KEY_RELEASED_BIT2 = 0x04;
  private static final int KEY_RELEASED_BIT3 = 0x08;
  private static final int KEY_RELEASED_BIT4 = 0x10;
  private static final int KEY_RELEASED_BIT5 = 0x20; // for Kempston fire button 2
  private static final int KEY_RELEASED_BIT6 = 0x40; // for Kempston fire button 3
  private static final int KEY_RELEASED_BIT7 = 0x80; // for Fuller fire button

  public MiniZXKeyboard() {
    reset();
    mapPCKeys = false;
    winBug = System.getProperty("os.name").contains("Windows");
    joystickModel= JoystickModel.CURSOR;
  }

  public final void reset() {
    Arrays.fill(rowKey, 0xff);
    shiftPressed = false;
    kempston = capsShiftCounter = 0;
    sjs1 = sjs2 = fuller = 0xff;
    Arrays.fill(keyEventPending, null);
  }

  public final JoystickModel getJoystickModel() {
    return joystickModel;
  }

  public final void setJoystickModel(JoystickModel model) {
    kempston = 0;
    sjs1 = sjs2 = fuller = 0xff;


  }

  public final void setJoystickModel(int model) {
    switch (model) {
      case 1:
        setJoystickModel(JoystickModel.KEMPSTON);
        break;
      case 2:
        setJoystickModel(JoystickModel.SINCLAIR1);
        break;
      case 3:
        setJoystickModel(JoystickModel.SINCLAIR2);
        break;
      case 4:
        setJoystickModel(JoystickModel.CURSOR);
        break;
      case 5:
        setJoystickModel(JoystickModel.FULLER);
        break;
      default:
        setJoystickModel(JoystickModel.NONE);
    }
  }

  public boolean isMapPCKeys() {
    return mapPCKeys;
  }

  public void setMapPCKeys(boolean state) {
    mapPCKeys = state;
    shiftPressed = false;
    Arrays.fill(keyEventPending, null);
  }

  private void joystickToSJS1(int state) {
    sjs1 = 0xff;

    if (state == 0)
      return;

    if ((state & 0x80) != 0) {
      sjs1 &= KEY_PRESSED_BIT4; // Sinclair 2 Left (6)
    }
    if ((state & 0x20) != 0) {
      sjs1 &= KEY_PRESSED_BIT3; // Sinclair 2 Right (7)
    }
    if ((state & 0x40) != 0) {
      sjs1 &= KEY_PRESSED_BIT2; // Sinclair 2 Down (8)
    }
    if ((state & 0x10) != 0) {
      sjs1 &= KEY_PRESSED_BIT1; // Sinclair 2 Up (9)
    }
    if ((state & 0xF000) != 0) {
      sjs1 &= KEY_PRESSED_BIT0; // Sinclair 2 Fire (0)
    }
  }

  private void joystickToSJS2(int state) {
    sjs2 = 0xff;

    if (state == 0)
      return;

    if ((state & 0x80) != 0) {
      sjs2 &= KEY_PRESSED_BIT0; // Sinclair 1 Left (1)
    }
    if ((state & 0x20) != 0) {
      sjs2 &= KEY_PRESSED_BIT1; // Sinclair 1 Right (2)
    }
    if ((state & 0x40) != 0) {
      sjs2 &= KEY_PRESSED_BIT2; // Sinclair 1 Down (3)
    }
    if ((state & 0x10) != 0) {
      sjs2 &= KEY_PRESSED_BIT3; // Sinclair 1 Up (4)
    }
    if ((state & 0xF000) != 0) {
      sjs2 &= KEY_PRESSED_BIT4; // Sinclair 1 Fire (5)
    }
  }

  public int readKeyboardPort(int port, boolean mapJoysticks) {
    int keys = 0xff;
    int res = port >>> 8;


//        System.out.println(String.format("readKeyboardPort: %04X, %02x, %02x", port, sjs1, sjs2));
    // reading more than a row
    if (res == 0x7f) { // SPACE to 'B' row
      return rowKey[7];
    } else if (res == 0xbf) { // ENTER to 'H' row
      return rowKey[6];
    } else if (res == 0xdf) { // 'P' to 'Y' row
      return rowKey[5];
    } else if (res == 0xef) { // '0' to '6' row
      return rowKey[4] & sjs1;
    } else if (res == 0xf7) { // '1' to '5' row
      return rowKey[3] & sjs2;
    } else if (res == 0xfb) { // 'Q' to 'T' row
      return rowKey[2];
    } else if (res == 0xfd) { // 'A' to 'G' row
      return rowKey[1];
    } else if (res == 0xfe) { //  'SHIFT' to 'V' row
      return rowKey[0];
    } else {
      res = ~res & 0xff;
      for (int row = 0, mask = 0x01; row < 8; row++, mask <<= 1) {
        if ((res & mask) != 0) {
          keys &= rowKey[row];
        }
      }
    }
    return keys;
  }

  @Override
  public void
  keyPressed(KeyEvent evt) {

    if (mapPCKeys) {
      char keychar = evt.getKeyChar();
      if (keychar != KeyEvent.CHAR_UNDEFINED && !evt.isAltDown()) {
//            System.out.println("pressed " + keychar);
        if (pressedKeyChar(keychar)) {
          for (int key = 0; key < keyEventPending.length; key++) {
            if (keyEventPending[key] == null) {
              keyEventPending[key] = evt;
//                        System.out.println(String.format("Key pressed #%d: %c", key, keychar));
              break;
            }
          }
          return;
        }
      }
    }

    int key = evt.getKeyCode();

//        System.out.println(String.format("Press keyCode = %d, modifiers = %d", key, evt.getModifiersEx()));

    /*
     * Windows no envía el keycode VK_ALT_GRAPH y en su lugar envía dos eventos, Ctrl + Alt, en ese orden.
     * Además, una repetición de tecla consiste en múltiples eventos keyPressed y un solo evento keyReleased.
     *
     * El Ctrl es una pulsación normal y el Alt lleva activos los modificadores CTRL y ALT.
     *
     * El problema es que el primer Ctrl nos "presiona" la tecla Symbol-Shift, y hay que quitarla.
     *
     * En cualquier otro caso, la tecla Alt hay que saltársela para que sigan funcionando los
     * atajos de teclado sin producir pulsaciones espureas en el emulador.
     *
     * Además, una repetición de tecla consiste en múltiples eventos keyPressed y un solo evento keyReleased.
     *
     * Algunos teclados de Windows no tienen AltGr sino un Alt derecho. Shit yourself, little parrot!.
     */
    if (winBug && key == KeyEvent.VK_ALT && (evt.getModifiersEx() == (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK)
        || evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT)) {
      key = KeyEvent.VK_ALT_GRAPH;
      rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
    } else {
      // En caso de ser Windows, si se reciben Alt + Control probablemente lo que se pulsó fue AltGr
      // Gracias a pastbytes por detectar (también) este problema e indicarme la manera de reproducirlo.
      if (evt.isAltDown() && !evt.isControlDown())
        return;
    }

    switch (key) {
      // Row B - Break/Space
      case KeyEvent.VK_SPACE:
        rowKey[7] &= KEY_PRESSED_BIT0; // Break/Space
        break;
      case KeyEvent.VK_CONTROL:
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        break;
      case KeyEvent.VK_M:
        rowKey[7] &= KEY_PRESSED_BIT2; // M
        break;
      case KeyEvent.VK_N:
        rowKey[7] &= KEY_PRESSED_BIT3; // N
        break;
      case KeyEvent.VK_B:
        rowKey[7] &= KEY_PRESSED_BIT4; // B
        break;
      // Row ENTER - H
      case KeyEvent.VK_ENTER:
        rowKey[6] &= KEY_PRESSED_BIT0; // ENTER
        break;
      case KeyEvent.VK_L:
        rowKey[6] &= KEY_PRESSED_BIT1; // L
        break;
      case KeyEvent.VK_K:
        rowKey[6] &= KEY_PRESSED_BIT2; // K
        break;
      case KeyEvent.VK_J:
        rowKey[6] &= KEY_PRESSED_BIT3; // J
        break;
      case KeyEvent.VK_H:
        rowKey[6] &= KEY_PRESSED_BIT4; // H
        break;
      // Row P - Y
      case KeyEvent.VK_P:
        rowKey[5] &= KEY_PRESSED_BIT0; // P
        break;
      case KeyEvent.VK_O:
        rowKey[5] &= KEY_PRESSED_BIT1; // O
        break;
      case KeyEvent.VK_I:
        rowKey[5] &= KEY_PRESSED_BIT2; // I
        break;
      case KeyEvent.VK_U:
        rowKey[5] &= KEY_PRESSED_BIT3; // U
        break;
      case KeyEvent.VK_Y:
        rowKey[5] &= KEY_PRESSED_BIT4; // Y
        break;
      // Row 0 - 6
      case KeyEvent.VK_0:
        rowKey[4] &= KEY_PRESSED_BIT0; // 0
        break;
      case KeyEvent.VK_9:
        rowKey[4] &= KEY_PRESSED_BIT1; // 9
        break;
      case KeyEvent.VK_8:
        rowKey[4] &= KEY_PRESSED_BIT2; // 8
        break;
      case KeyEvent.VK_7:
        rowKey[4] &= KEY_PRESSED_BIT3; // 7
        break;
      case KeyEvent.VK_6:
        rowKey[4] &= KEY_PRESSED_BIT4; // 6
        break;
      // Row 1 - 5
      case KeyEvent.VK_1:
        rowKey[3] &= KEY_PRESSED_BIT0; // 1
        break;
      case KeyEvent.VK_2:
        rowKey[3] &= KEY_PRESSED_BIT1; // 2
        break;
      case KeyEvent.VK_3:
        rowKey[3] &= KEY_PRESSED_BIT2; // 3
        break;
      case KeyEvent.VK_4:
        rowKey[3] &= KEY_PRESSED_BIT3; // 4
        break;
      case KeyEvent.VK_5:
        rowKey[3] &= KEY_PRESSED_BIT4; // 5
        break;
      // Row Q - T
      case KeyEvent.VK_Q:
        rowKey[2] &= KEY_PRESSED_BIT0; // Q
        break;
      case KeyEvent.VK_W:
        rowKey[2] &= KEY_PRESSED_BIT1; // W
        break;
      case KeyEvent.VK_E:
        rowKey[2] &= KEY_PRESSED_BIT2; // E
        break;
      case KeyEvent.VK_R:
        rowKey[2] &= KEY_PRESSED_BIT3; // R
        break;
      case KeyEvent.VK_T:
        rowKey[2] &= KEY_PRESSED_BIT4; // T
        break;
      // Row A - G
      case KeyEvent.VK_A:
        rowKey[1] &= KEY_PRESSED_BIT0; // A
        break;
      case KeyEvent.VK_S:
        rowKey[1] &= KEY_PRESSED_BIT1; // S
        break;
      case KeyEvent.VK_D:
        rowKey[1] &= KEY_PRESSED_BIT2; // D
        break;
      case KeyEvent.VK_F:
        rowKey[1] &= KEY_PRESSED_BIT3; // F
        break;
      case KeyEvent.VK_G:
        rowKey[1] &= KEY_PRESSED_BIT4; // G
        break;
      // Row Caps Shift - V
      case KeyEvent.VK_SHIFT:
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        shiftPressed = true;
        break;
      case KeyEvent.VK_Z:
        rowKey[0] &= KEY_PRESSED_BIT1; // Z
        break;
      case KeyEvent.VK_X:
        rowKey[0] &= KEY_PRESSED_BIT2; // X
        break;
      case KeyEvent.VK_C:
        rowKey[0] &= KEY_PRESSED_BIT3; // C
        break;
      case KeyEvent.VK_V:
        rowKey[0] &= KEY_PRESSED_BIT4; // V
        break;
      // Additional keys
      case KeyEvent.VK_BACK_SPACE:
        capsShiftCounter++;
        rowKey[0] &= KEY_PRESSED_BIT0; // CAPS
        rowKey[4] &= KEY_PRESSED_BIT0; // 0
        break;
      case KeyEvent.VK_COMMA:
        rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT3); // Symbol Shift + N (',')
        break;
      case KeyEvent.VK_PERIOD:
        rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT2); // Symbol Shift + M ('.')
        break;
      case KeyEvent.VK_MINUS:
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[6] &= KEY_PRESSED_BIT3; // J
        break;
      case KeyEvent.VK_PLUS:
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[6] &= KEY_PRESSED_BIT2; // K
        break;
      case KeyEvent.VK_EQUALS: // UK Keyboard
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[6] &= KEY_PRESSED_BIT1; // L
        break;
      case KeyEvent.VK_NUMBER_SIGN: // UK Keyboard
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[3] &= KEY_PRESSED_BIT2; // 3
        break;
      case KeyEvent.VK_SLASH: // UK Keyboard
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[0] &= KEY_PRESSED_BIT4; // V
        break;
      case KeyEvent.VK_SEMICOLON: // UK Keyboard
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[5] &= KEY_PRESSED_BIT1; // O
        break;
      case KeyEvent.VK_CAPS_LOCK:
        capsShiftCounter++;
        rowKey[0] &= KEY_PRESSED_BIT0; // CAPS
        rowKey[3] &= KEY_PRESSED_BIT1; // 2  -- Caps Lock
        break;
      case KeyEvent.VK_ESCAPE:
        capsShiftCounter++;
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[7] &= KEY_PRESSED_BIT0; // Space
        break;
      // Joystick emulation
      case KeyEvent.VK_LEFT:
        switch (joystickModel) {
          case NONE:
            rowKey[0] &= KEY_PRESSED_BIT0; // Caps
            capsShiftCounter++;
          case CURSOR:
            rowKey[3] &= KEY_PRESSED_BIT4; // 5  -- Left arrow
            break;
          case KEMPSTON:
            kempston |= KEY_RELEASED_BIT1;
            break;
          case SINCLAIR1:
            rowKey[4] &= KEY_PRESSED_BIT4; // 6 -- Left
            break;
          case SINCLAIR2:
            rowKey[3] &= KEY_PRESSED_BIT0; // 1 -- Left
            break;
          case FULLER:
            fuller &= KEY_PRESSED_BIT2;
            break;
        }
        break;
      case KeyEvent.VK_DOWN:
        switch (joystickModel) {
          case NONE:
            rowKey[0] &= KEY_PRESSED_BIT0; // Caps
            capsShiftCounter++;
          case CURSOR:
            rowKey[4] &= KEY_PRESSED_BIT4; // 6  -- Down arrow
            break;
          case KEMPSTON:
            kempston |= KEY_RELEASED_BIT2;
            break;
          case SINCLAIR1:
            rowKey[4] &= KEY_PRESSED_BIT2; // 8 -- Down
            break;
          case SINCLAIR2:
            rowKey[3] &= KEY_PRESSED_BIT2; // 3 -- Down
            break;
          case FULLER:
            fuller &= KEY_PRESSED_BIT1;
            break;
        }
        break;
      case KeyEvent.VK_UP:
        switch (joystickModel) {
          case NONE:
            rowKey[0] &= KEY_PRESSED_BIT0; // Caps
            capsShiftCounter++;
          case CURSOR:
            rowKey[4] &= KEY_PRESSED_BIT3; // 7  -- Up arrow
            break;
          case KEMPSTON:
            kempston |= KEY_RELEASED_BIT3;
            break;
          case SINCLAIR1:
            rowKey[4] &= KEY_PRESSED_BIT1; // 9 -- Up
            break;
          case SINCLAIR2:
            rowKey[3] &= KEY_PRESSED_BIT3; // 4 -- Up
            break;
          case FULLER:
            fuller &= KEY_PRESSED_BIT0;
            break;
        }
        break;
      case KeyEvent.VK_RIGHT:
        switch (joystickModel) {
          case NONE:
            rowKey[0] &= KEY_PRESSED_BIT0; // Caps
            capsShiftCounter++;
          case CURSOR:
            rowKey[4] &= KEY_PRESSED_BIT2; // 8  -- Right arrow
            break;
          case KEMPSTON:
            kempston |= KEY_RELEASED_BIT0;
            break;
          case SINCLAIR1:
            rowKey[4] &= KEY_PRESSED_BIT3; // 7 -- Right
            break;
          case SINCLAIR2:
            rowKey[3] &= KEY_PRESSED_BIT1; // 2 -- Right
            break;
          case FULLER:
            fuller &= KEY_PRESSED_BIT3;
            break;
        }
        break;
      case KeyEvent.VK_META:
      case KeyEvent.VK_ALT_GRAPH:
        switch (joystickModel) {
          case NONE:
            break;
          case KEMPSTON:
            kempston |= KEY_RELEASED_BIT4;
            break;
          case CURSOR:
          case SINCLAIR1:
            rowKey[4] &= KEY_PRESSED_BIT0; // 0 -- Fire
            break;
          case SINCLAIR2:
            rowKey[3] &= KEY_PRESSED_BIT4; // 5 -- Fire
            break;
          case FULLER:
            fuller &= KEY_PRESSED_BIT7;
            break;
        }
        break;
    }
  }

  @Override
  public void keyReleased(KeyEvent evt) {

    if (mapPCKeys) {
      char keychar = evt.getKeyChar();

      if (keychar != KeyEvent.CHAR_UNDEFINED && !evt.isAltDown()) {
//            System.out.println("released " + keychar);
        for (int key = 0; key < keyEventPending.length; key++) {
          if (keyEventPending[key] != null
              && evt.getKeyCode() == keyEventPending[key].getKeyCode()) {
            keychar = keyEventPending[key].getKeyChar();
            keyEventPending[key] = null;
//                    System.out.println(String.format("Key released #%d: %c\n", key, keychar));
          }
        }

        if (releasedKeyChar(keychar)) {
          return;
        }
      }
    }

    int key = evt.getKeyCode();

//        System.out.println(String.format("Release keyCode = %d, modifiers = %d", key, evt.getModifiersEx()));

    /*
     * Windows no envía el keycode VK_ALT_GRAPH y en su lugar envía dos eventos, Ctrl + Alt, en ese orden.
     *
     * El Ctrl lleva activo el modificador Alt. El Alt es un evento normal.
     *
     * La tecla Alt hay que saltársela para que sigan funcionando los atajos de teclado sin
     * producir pulsaciones espureas en el emulador.
     *
     * Además, una repetición de tecla consiste en múltiples eventos keyPressed y un solo evento keyReleased.
     *
     * Algunos teclados de Windows no tienen AltGr sino un Alt derecho. Shit yourself, little parrot!.
     */
    if (winBug && ((key == KeyEvent.VK_CONTROL && evt.getModifiersEx() == InputEvent.ALT_DOWN_MASK)
        || (key == KeyEvent.VK_ALT && evt.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT))) {
      key = KeyEvent.VK_ALT_GRAPH;
    } else {
      if (evt.isAltDown() && !evt.isControlDown())
        return;
    }

    switch (key) {
      // Row Break/Space - B
      case KeyEvent.VK_SPACE:
        rowKey[7] |= KEY_RELEASED_BIT0; // Break/Space
        break;
      case KeyEvent.VK_CONTROL:
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        break;
      case KeyEvent.VK_M:
        rowKey[7] |= KEY_RELEASED_BIT2; // M
        break;
      case KeyEvent.VK_N:
        rowKey[7] |= KEY_RELEASED_BIT3; // N
        break;
      case KeyEvent.VK_B:
        rowKey[7] |= KEY_RELEASED_BIT4; // B
        break;
      // Row ENTER - H
      case KeyEvent.VK_ENTER:
        rowKey[6] |= KEY_RELEASED_BIT0; // ENTER
        break;
      case KeyEvent.VK_L:
        rowKey[6] |= KEY_RELEASED_BIT1; // L
        break;
      case KeyEvent.VK_K:
        rowKey[6] |= KEY_RELEASED_BIT2; // K
        break;
      case KeyEvent.VK_J:
        rowKey[6] |= KEY_RELEASED_BIT3; // J
        break;
      case KeyEvent.VK_H:
        rowKey[6] |= KEY_RELEASED_BIT4; // H
        break;
      // Row P - Y
      case KeyEvent.VK_P:
        rowKey[5] |= KEY_RELEASED_BIT0; // P
        break;
      case KeyEvent.VK_O:
        rowKey[5] |= KEY_RELEASED_BIT1; // O
        break;
      case KeyEvent.VK_I:
        rowKey[5] |= KEY_RELEASED_BIT2; // I
        break;
      case KeyEvent.VK_U:
        rowKey[5] |= KEY_RELEASED_BIT3; // U
        break;
      case KeyEvent.VK_Y:
        rowKey[5] |= KEY_RELEASED_BIT4; // Y
        break;
      // Row 0 - 6
      case KeyEvent.VK_0:
        rowKey[4] |= KEY_RELEASED_BIT0; // 0
        break;
      case KeyEvent.VK_9:
        rowKey[4] |= KEY_RELEASED_BIT1; // 9
        break;
      case KeyEvent.VK_8:
        rowKey[4] |= KEY_RELEASED_BIT2; // 8
        break;
      case KeyEvent.VK_7:
        rowKey[4] |= KEY_RELEASED_BIT3; // 7
        break;
      case KeyEvent.VK_6:
        rowKey[4] |= KEY_RELEASED_BIT4; // 6
        break;
      // Row 1 - 5
      case KeyEvent.VK_1:
        rowKey[3] |= KEY_RELEASED_BIT0; // 1
        break;
      case KeyEvent.VK_2:
        rowKey[3] |= KEY_RELEASED_BIT1; // 2
        break;
      case KeyEvent.VK_3:
        rowKey[3] |= KEY_RELEASED_BIT2; // 3
        break;
      case KeyEvent.VK_4:
        rowKey[3] |= KEY_RELEASED_BIT3; // 4
        break;
      case KeyEvent.VK_5:
        rowKey[3] |= KEY_RELEASED_BIT4; // 5
        break;
      // Row Q - T
      case KeyEvent.VK_Q:
        rowKey[2] |= KEY_RELEASED_BIT0; // Q
        break;
      case KeyEvent.VK_W:
        rowKey[2] |= KEY_RELEASED_BIT1; // W
        break;
      case KeyEvent.VK_E:
        rowKey[2] |= KEY_RELEASED_BIT2; // E
        break;
      case KeyEvent.VK_R:
        rowKey[2] |= KEY_RELEASED_BIT3; // R
        break;
      case KeyEvent.VK_T:
        rowKey[2] |= KEY_RELEASED_BIT4; // T
        break;
      // Row A - G
      case KeyEvent.VK_A:
        rowKey[1] |= KEY_RELEASED_BIT0; // A
        break;
      case KeyEvent.VK_S:
        rowKey[1] |= KEY_RELEASED_BIT1; // S
        break;
      case KeyEvent.VK_D:
        rowKey[1] |= KEY_RELEASED_BIT2; // D
        break;
      case KeyEvent.VK_F:
        rowKey[1] |= KEY_RELEASED_BIT3; // F
        break;
      case KeyEvent.VK_G:
        rowKey[1] |= KEY_RELEASED_BIT4; // G
        break;
      // Row Caps Shift - V
      case KeyEvent.VK_SHIFT:
        if (capsShiftCounter == 0)
          rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
        shiftPressed = false;
        break;
      case KeyEvent.VK_Z:
        rowKey[0] |= KEY_RELEASED_BIT1; // Z
        break;
      case KeyEvent.VK_X:
        rowKey[0] |= KEY_RELEASED_BIT2; // X
        break;
      case KeyEvent.VK_C:
        rowKey[0] |= KEY_RELEASED_BIT3; // C
        break;
      case KeyEvent.VK_V:
        rowKey[0] |= KEY_RELEASED_BIT4; // V
        break;
      // Additional keys
      case KeyEvent.VK_BACK_SPACE:
        if (winBug || (--capsShiftCounter < 1 && !shiftPressed)) {
          capsShiftCounter = 0;
          rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
        }
        rowKey[4] |= KEY_RELEASED_BIT0; // 0
        break;
      case KeyEvent.VK_COMMA:
        rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT3); // Symbol Shift + N
        break;
      case KeyEvent.VK_PERIOD:
        rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT2); // Symbol Shift + M
        break;
      case KeyEvent.VK_MINUS:
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[6] |= KEY_RELEASED_BIT3; // J
        break;
      case KeyEvent.VK_PLUS:
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[6] |= KEY_RELEASED_BIT2; // K
        break;
      case KeyEvent.VK_EQUALS: // UK Keyboard
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[6] |= KEY_RELEASED_BIT1; // L
        break;
      case KeyEvent.VK_NUMBER_SIGN: // UK Keyboard
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[3] |= KEY_RELEASED_BIT2; // 3
        break;
      case KeyEvent.VK_SLASH: // UK Keyboard
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[0] |= KEY_RELEASED_BIT4; // V
        break;
      case KeyEvent.VK_SEMICOLON: // UK Keyboard
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[5] |= KEY_RELEASED_BIT1; // O
        break;
      case KeyEvent.VK_CAPS_LOCK:
        if (winBug || (--capsShiftCounter < 1 && !shiftPressed)) {
          capsShiftCounter = 0;
          rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
        }
        rowKey[3] |= KEY_RELEASED_BIT1; // 2
        break;
      case KeyEvent.VK_ESCAPE:
        if (winBug || (--capsShiftCounter < 1 && !shiftPressed)) {
          capsShiftCounter = 0;
          rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
        }
        rowKey[7] |= KEY_RELEASED_BIT0; // Space
        break;
      // Joystick emulation
      case KeyEvent.VK_LEFT:
        switch (joystickModel) {
          case NONE:
            if (winBug || (--capsShiftCounter < 1 && !shiftPressed)) {
              capsShiftCounter = 0;
              rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
            }
          case CURSOR:
            rowKey[3] |= KEY_RELEASED_BIT4; // 5 -- Left arrow
            break;
          case KEMPSTON:
            kempston &= KEY_PRESSED_BIT1;
            break;
          case SINCLAIR1:
            rowKey[4] |= KEY_RELEASED_BIT4; // 6 -- Left
            break;
          case SINCLAIR2:
            rowKey[3] |= KEY_RELEASED_BIT0; // 1 -- Left
            break;
          case FULLER:
            fuller |= KEY_RELEASED_BIT2;
            break;
        }
        break;
      case KeyEvent.VK_DOWN:
        switch (joystickModel) {
          case NONE:
            if (winBug || (--capsShiftCounter < 1 && !shiftPressed)) {
              capsShiftCounter = 0;
              rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
            }
          case CURSOR:
            rowKey[4] |= KEY_RELEASED_BIT4; // 6 -- Down arrow
            break;
          case KEMPSTON:
            kempston &= KEY_PRESSED_BIT2;
            break;
          case SINCLAIR1:
            rowKey[4] |= KEY_RELEASED_BIT2; // 8 -- Down
            break;
          case SINCLAIR2:
            rowKey[3] |= KEY_RELEASED_BIT2; // 3 -- Down
            break;
          case FULLER:
            fuller |= KEY_RELEASED_BIT1;
            break;
        }
        break;
      case KeyEvent.VK_UP:
        switch (joystickModel) {
          case NONE:
            if (winBug || (--capsShiftCounter < 1 && !shiftPressed)) {
              capsShiftCounter = 0;
              rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
            }
          case CURSOR:
            rowKey[4] |= KEY_RELEASED_BIT3; // 7  -- Up arrow
            break;
          case KEMPSTON:
            kempston &= KEY_PRESSED_BIT3;
            break;
          case SINCLAIR1:
            rowKey[4] |= KEY_RELEASED_BIT1; // 9 -- Up
            break;
          case SINCLAIR2:
            rowKey[3] |= KEY_RELEASED_BIT3; // 4 -- Up
            break;
          case FULLER:
            fuller |= KEY_RELEASED_BIT0;
            break;
        }
        break;
      case KeyEvent.VK_RIGHT:
        switch (joystickModel) {
          case NONE:
            if (winBug || (--capsShiftCounter < 1 && !shiftPressed)) {
              capsShiftCounter = 0;
              rowKey[0] |= KEY_RELEASED_BIT0; // CAPS
            }
          case CURSOR:
            rowKey[4] |= KEY_RELEASED_BIT2; // 8 -- Right arrow
            break;
          case KEMPSTON:
            kempston &= KEY_PRESSED_BIT0;
            break;
          case SINCLAIR1:
            rowKey[4] |= KEY_RELEASED_BIT3; // 7 -- Right
            break;
          case SINCLAIR2:
            rowKey[3] |= KEY_RELEASED_BIT1; // 2 -- Right
            break;
          case FULLER:
            fuller |= KEY_RELEASED_BIT3;
            break;
        }
        break;
      case KeyEvent.VK_META:
      case KeyEvent.VK_ALT_GRAPH:
        switch (joystickModel) {
          case NONE:
            break;
          case KEMPSTON:
            kempston &= KEY_PRESSED_BIT4;
            break;
          case CURSOR:
          case SINCLAIR1:
            rowKey[4] |= KEY_RELEASED_BIT0;  // 0 -- Fire
            break;
          case SINCLAIR2:
            rowKey[3] |= KEY_RELEASED_BIT4;  // 5  -- Fire
            break;
          case FULLER:
            fuller |= KEY_RELEASED_BIT7;
            break;
        }
        break;
    }
  }

  @Override
  public void keyTyped(java.awt.event.KeyEvent evt) {
    // TODO add your handling code here:
  }

  private boolean pressedKeyChar(char keyChar) {
    boolean done = true;

    if (shiftPressed) {
      rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
    }

    switch (keyChar) {
      case '!':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[3] &= KEY_PRESSED_BIT0; // 1
        break;
      case '"':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[5] &= KEY_PRESSED_BIT0; // P
        break;
      case '#':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[3] &= KEY_PRESSED_BIT2; // 3
        break;
      case '$':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[3] &= KEY_PRESSED_BIT3; // 4
        break;
      case '%':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[3] &= KEY_PRESSED_BIT4; // 5
        break;
      case '&':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[4] &= KEY_PRESSED_BIT4; // 6
        break;
      case '\'':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[4] &= KEY_PRESSED_BIT3; // 7
        break;
      case '(':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[4] &= KEY_PRESSED_BIT2; // 8
        break;
      case ')':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[4] &= KEY_PRESSED_BIT1; // 9
        break;
      case '*':
        rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT4); // Symbol Shift + b
        break;
      case '+':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[6] &= KEY_PRESSED_BIT2; // K
        break;
      case ',':
        rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT3); // Symbol Shift + n
        break;
      case '-':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[6] &= KEY_PRESSED_BIT3; // J
        break;
      case '.':
        rowKey[7] &= (KEY_PRESSED_BIT1 & KEY_PRESSED_BIT2); // Symbol Shift + m
        break;
      case '/':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[0] &= KEY_PRESSED_BIT4; // V
        break;
      case '0':
        rowKey[4] &= KEY_PRESSED_BIT0; // 0
        break;
      case '1':
        rowKey[3] &= KEY_PRESSED_BIT0; // 1
        break;
      case '2':
        rowKey[3] &= KEY_PRESSED_BIT1; // 2
        break;
      case '3':
        rowKey[3] &= KEY_PRESSED_BIT2; // 3
        break;
      case '4':
        rowKey[3] &= KEY_PRESSED_BIT3; // 4
        break;
      case '5':
        rowKey[3] &= KEY_PRESSED_BIT4; // 5
        break;
      case '6':
        rowKey[4] &= KEY_PRESSED_BIT4; // 6
        break;
      case '7':
        rowKey[4] &= KEY_PRESSED_BIT3; // 7
        break;
      case '8':
        rowKey[4] &= KEY_PRESSED_BIT2; // 8
        break;
      case '9':
        rowKey[4] &= KEY_PRESSED_BIT1; // 9
        break;
      case ':':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[0] &= KEY_PRESSED_BIT1; // Z
        break;
      case ';':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[5] &= KEY_PRESSED_BIT1; // O
        break;
      case '<':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[2] &= KEY_PRESSED_BIT3; // R
        break;
      case '=':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[6] &= KEY_PRESSED_BIT1; // L
        break;
      case '>':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[2] &= KEY_PRESSED_BIT4; // T
        break;
      case '?':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[0] &= KEY_PRESSED_BIT3; // C
        break;
      case '@':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[3] &= KEY_PRESSED_BIT1; // 2
        break;
      case 'A':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[1] &= KEY_PRESSED_BIT0; // A
        break;
      case 'B':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[7] &= KEY_PRESSED_BIT4; // B
        break;
      case 'C':
        rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT3); // Caps Shift + c
        break;
      case 'D':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[1] &= KEY_PRESSED_BIT2; // D
        break;
      case 'E':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[2] &= KEY_PRESSED_BIT2; // E
        break;
      case 'F':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[1] &= KEY_PRESSED_BIT3; // F
        break;
      case 'G':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[1] &= KEY_PRESSED_BIT4; // G
        break;
      case 'H':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[6] &= KEY_PRESSED_BIT4; // H
        break;
      case 'I':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[5] &= KEY_PRESSED_BIT2; // I
        break;
      case 'J':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[6] &= KEY_PRESSED_BIT3; // J
        break;
      case 'K':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[6] &= KEY_PRESSED_BIT2; // K
        break;
      case 'L':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[6] &= KEY_PRESSED_BIT1; // L
        break;
      case 'M':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[7] &= KEY_PRESSED_BIT2; // M
        break;
      case 'N':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[7] &= KEY_PRESSED_BIT3; // N
        break;
      case 'O':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[5] &= KEY_PRESSED_BIT1; // O
        break;
      case 'P':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[5] &= KEY_PRESSED_BIT0; // P
        break;
      case 'Q':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[2] &= KEY_PRESSED_BIT0; // Q
        break;
      case 'R':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[2] &= KEY_PRESSED_BIT3; // R
        break;
      case 'S':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[1] &= KEY_PRESSED_BIT1; // S
        break;
      case 'T':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[2] &= KEY_PRESSED_BIT4; // T
        break;
      case 'U':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[5] &= KEY_PRESSED_BIT3; // U
        break;
      case 'V':
        rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT4); // Caps Shift + v
        break;
      case 'W':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[2] &= KEY_PRESSED_BIT1; // W
        break;
      case 'X':
        rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT2); // Caps Shift + x
        break;
      case 'Y':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[5] &= KEY_PRESSED_BIT4; // Y
        break;
      case 'Z':
        rowKey[0] &= (KEY_PRESSED_BIT0 & KEY_PRESSED_BIT1); // Caps Shift + z
        break;
      case '[':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[5] &= KEY_PRESSED_BIT4; // Y
        break;
      case '\\':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[1] &= KEY_PRESSED_BIT2; // D
        break;
      case ']':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[5] &= KEY_PRESSED_BIT3; // U
        break;
      case '_':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[4] &= KEY_PRESSED_BIT0; // 0
        break;
      case 'a':
        rowKey[1] &= KEY_PRESSED_BIT0; // A
        break;
      case 'b':
        rowKey[7] &= KEY_PRESSED_BIT4; // B
        break;
      case 'c':
        rowKey[0] &= KEY_PRESSED_BIT3; // C
        break;
      case 'd':
        rowKey[1] &= KEY_PRESSED_BIT2; // D
        break;
      case 'e':
        rowKey[2] &= KEY_PRESSED_BIT2; // E
        break;
      case 'f':
        rowKey[1] &= KEY_PRESSED_BIT3; // F
        break;
      case 'g':
        rowKey[1] &= KEY_PRESSED_BIT4; // G
        break;
      case 'h':
        rowKey[6] &= KEY_PRESSED_BIT4; // H
        break;
      case 'i':
        rowKey[5] &= KEY_PRESSED_BIT2; // I
        break;
      case 'j':
        rowKey[6] &= KEY_PRESSED_BIT3; // J
        break;
      case 'k':
        rowKey[6] &= KEY_PRESSED_BIT2; // K
        break;
      case 'l':
        rowKey[6] &= KEY_PRESSED_BIT1; // L
        break;
      case 'm':
        rowKey[7] &= KEY_PRESSED_BIT2; // M
        break;
      case 'n':
        rowKey[7] &= KEY_PRESSED_BIT3; // N
        break;
      case 'o':
        rowKey[5] &= KEY_PRESSED_BIT1; // O
        break;
      case 'p':
        rowKey[5] &= KEY_PRESSED_BIT0; // P
        break;
      case 'q':
        rowKey[2] &= KEY_PRESSED_BIT0; // Q
        break;
      case 'r':
        rowKey[2] &= KEY_PRESSED_BIT3; // R
        break;
      case 's':
        rowKey[1] &= KEY_PRESSED_BIT1; // S
        break;
      case 't':
        rowKey[2] &= KEY_PRESSED_BIT4; // T
        break;
      case 'u':
        rowKey[5] &= KEY_PRESSED_BIT3; // U
        break;
      case 'v':
        rowKey[0] &= KEY_PRESSED_BIT4; // V
        break;
      case 'w':
        rowKey[2] &= KEY_PRESSED_BIT1; // W
        break;
      case 'x':
        rowKey[0] &= KEY_PRESSED_BIT2; // X
        break;
      case 'y':
        rowKey[5] &= KEY_PRESSED_BIT4; // Y
        break;
      case 'z':
        rowKey[0] &= KEY_PRESSED_BIT1; // Z
        break;
      case '{':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[1] &= KEY_PRESSED_BIT3; // F
        break;
      case '|':
      case '¦': // Spanish keyboard
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[1] &= KEY_PRESSED_BIT1; // S
        break;
      case '}':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[1] &= KEY_PRESSED_BIT4; // G
        break;
      case '~':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[1] &= KEY_PRESSED_BIT0; // A
        break;
      case '©': // Mac only
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[5] &= KEY_PRESSED_BIT0; // P
        break;
      case '`': // PC only
      case '§': // Mac only
      case '¡': // Spanish keyboard only
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[3] &= KEY_PRESSED_BIT0; // 1 (EDIT mode)
        break;
      case '¬': // PC only
      case '±': // Mac only
      case '¿': // Spanish keyboard only
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[4] &= KEY_PRESSED_BIT1; // 9 (GRAPHICS mode)
        break;
      case '£':
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift
        rowKey[0] &= KEY_PRESSED_BIT2; // x
        break;
      case 'º':
        rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        rowKey[7] &= KEY_PRESSED_BIT1; // Symbol Shift -- Extended Mode
        break;
      default:
        if (shiftPressed) {
          rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        }
        done = false;
    }
    return done;
  }

  private boolean releasedKeyChar(char keyChar) {
    boolean done = true;

    switch (keyChar) {
      case '!':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[3] |= KEY_RELEASED_BIT0; // 1
        break;
      case '"':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[5] |= KEY_RELEASED_BIT0; // P
        break;
      case '#':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[3] |= KEY_RELEASED_BIT2; // 3
        break;
      case '$':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[3] |= KEY_RELEASED_BIT3; // 4
        break;
      case '%':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[3] |= KEY_RELEASED_BIT4; // 5
        break;
      case '&':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[4] |= KEY_RELEASED_BIT4; // 6
        break;
      case '\'':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[4] |= KEY_RELEASED_BIT3; // 7
        break;
      case '(':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[4] |= KEY_RELEASED_BIT2; // 8
        break;
      case ')':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[4] |= KEY_RELEASED_BIT1; // 9
        break;
      case '*':
        rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT4); // Symbol Shift + b
        break;
      case '+':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[6] |= KEY_RELEASED_BIT2; // K
        break;
      case ',':
        rowKey[7] |= (KEY_RELEASED_BIT1 | KEY_RELEASED_BIT3); // Symbol Shift + n
        break;
      case '-':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[6] |= KEY_RELEASED_BIT3; // J
        break;
      case '.':
        rowKey[7] |= 0x06; // Symbol Shift + M
        break;
      case '/':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[0] |= KEY_RELEASED_BIT4; // V
        break;
      case '0':
        rowKey[4] |= KEY_RELEASED_BIT0; // 0
        break;
      case '1':
        rowKey[3] |= KEY_RELEASED_BIT0; // 1
        break;
      case '2':
        rowKey[3] |= KEY_RELEASED_BIT1; // 2
        break;
      case '3':
        rowKey[3] |= KEY_RELEASED_BIT2; // 3
        break;
      case '4':
        rowKey[3] |= KEY_RELEASED_BIT3; // 4
        break;
      case '5':
        rowKey[3] |= KEY_RELEASED_BIT4; // 5
        break;
      case '6':
        rowKey[4] |= KEY_RELEASED_BIT4; // 6
        break;
      case '7':
        rowKey[4] |= KEY_RELEASED_BIT3; // 7
        break;
      case '8':
        rowKey[4] |= KEY_RELEASED_BIT2; // 8
        break;
      case '9':
        rowKey[4] |= KEY_RELEASED_BIT1; // 9
        break;
      case ':':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[0] |= KEY_RELEASED_BIT1; // Z
        break;
      case ';':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[5] |= KEY_RELEASED_BIT1; // O
        break;
      case '<':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[2] |= KEY_RELEASED_BIT3; // R
        break;
      case '=':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[6] |= KEY_RELEASED_BIT1; // L
        break;
      case '>':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[2] |= KEY_RELEASED_BIT4; // T
        break;
      case '?':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[0] |= KEY_RELEASED_BIT3; // C
        break;
      case '@':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[3] |= KEY_RELEASED_BIT1; // 2
        break;
      case 'A':
        rowKey[1] |= KEY_RELEASED_BIT0; // A
        break;
      case 'B':
        rowKey[7] |= KEY_RELEASED_BIT4; // B
        break;
      case 'C':
        rowKey[0] |= KEY_RELEASED_BIT3; // C
        break;
      case 'D':
        rowKey[1] |= KEY_RELEASED_BIT2; // D
        break;
      case 'E':
        rowKey[2] |= KEY_RELEASED_BIT2; // E
        break;
      case 'F':
        rowKey[1] |= KEY_RELEASED_BIT3; // F
        break;
      case 'G':
        rowKey[1] |= KEY_RELEASED_BIT4; // G
        break;
      case 'H':
        rowKey[6] |= KEY_RELEASED_BIT4; // H
        break;
      case 'I':
        rowKey[5] |= KEY_RELEASED_BIT2; // I
        break;
      case 'J':
        rowKey[6] |= KEY_RELEASED_BIT3; // J
        break;
      case 'K':
        rowKey[6] |= KEY_RELEASED_BIT2; // K
        break;
      case 'L':
        rowKey[6] |= KEY_RELEASED_BIT1; // L
        break;
      case 'M':
        rowKey[7] |= KEY_RELEASED_BIT2; // M
        break;
      case 'N':
        rowKey[7] |= KEY_RELEASED_BIT3; // N
        break;
      case 'O':
        rowKey[5] |= KEY_RELEASED_BIT1; // O
        break;
      case 'P':
        rowKey[5] |= KEY_RELEASED_BIT0; // P
        break;
      case 'Q':
        rowKey[2] |= KEY_RELEASED_BIT0; // Q
        break;
      case 'R':
        rowKey[2] |= KEY_RELEASED_BIT3; // R
        break;
      case 'S':
        rowKey[1] |= KEY_RELEASED_BIT1; // S
        break;
      case 'T':
        rowKey[2] |= KEY_RELEASED_BIT4; // T
        break;
      case 'U':
        rowKey[5] |= KEY_RELEASED_BIT3; // U
        break;
      case 'V':
        rowKey[0] |= KEY_RELEASED_BIT4; // V
        break;
      case 'W':
        rowKey[2] |= KEY_RELEASED_BIT1; // W
        break;
      case 'X':
        rowKey[0] |= KEY_RELEASED_BIT2; // X
        break;
      case 'Y':
        rowKey[5] |= KEY_RELEASED_BIT4; // Y
        break;
      case 'Z':
        rowKey[0] |= KEY_RELEASED_BIT1; // Z
        break;
      case '[':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[5] |= KEY_RELEASED_BIT4; // Y
        break;
      case '\\':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[1] |= KEY_RELEASED_BIT2; // D
        break;
      case ']':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[5] |= KEY_RELEASED_BIT3; // U
        break;
      case '_':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[4] |= KEY_RELEASED_BIT0; // 0
        break;
      case 'a':
        rowKey[1] |= KEY_RELEASED_BIT0; // A
        break;
      case 'b':
        rowKey[7] |= KEY_RELEASED_BIT4; // B
        break;
      case 'c':
        rowKey[0] |= KEY_RELEASED_BIT3; // C
        break;
      case 'd':
        rowKey[1] |= KEY_RELEASED_BIT2; // D
        break;
      case 'e':
        rowKey[2] |= KEY_RELEASED_BIT2; // E
        break;
      case 'f':
        rowKey[1] |= KEY_RELEASED_BIT3; // F
        break;
      case 'g':
        rowKey[1] |= KEY_RELEASED_BIT4; // G
        break;
      case 'h':
        rowKey[6] |= KEY_RELEASED_BIT4; // H
        break;
      case 'i':
        rowKey[5] |= KEY_RELEASED_BIT2; // I
        break;
      case 'j':
        rowKey[6] |= KEY_RELEASED_BIT3; // J
        break;
      case 'k':
        rowKey[6] |= KEY_RELEASED_BIT2; // K
        break;
      case 'l':
        rowKey[6] |= KEY_RELEASED_BIT1; // L
        break;
      case 'm':
        rowKey[7] |= KEY_RELEASED_BIT2; // M
        break;
      case 'n':
        rowKey[7] |= KEY_RELEASED_BIT3; // N
        break;
      case 'o':
        rowKey[5] |= KEY_RELEASED_BIT1; // O
        break;
      case 'p':
        rowKey[5] |= KEY_RELEASED_BIT0; // P
        break;
      case 'q':
        rowKey[2] |= KEY_RELEASED_BIT0; // Q
        break;
      case 'r':
        rowKey[2] |= KEY_RELEASED_BIT3; // R
        break;
      case 's':
        rowKey[1] |= KEY_RELEASED_BIT1; // S
        break;
      case 't':
        rowKey[2] |= KEY_RELEASED_BIT4; // T
        break;
      case 'u':
        rowKey[5] |= KEY_RELEASED_BIT3; // U
        break;
      case 'v':
        rowKey[0] |= KEY_RELEASED_BIT4; // V
        break;
      case 'w':
        rowKey[2] |= KEY_RELEASED_BIT1; // W
        break;
      case 'x':
        rowKey[0] |= KEY_RELEASED_BIT2; // X
        break;
      case 'y':
        rowKey[5] |= KEY_RELEASED_BIT4; // Y
        break;
      case 'z':
        rowKey[0] |= KEY_RELEASED_BIT1; // Z
        break;
      case '{':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[1] |= KEY_RELEASED_BIT3; // F
        break;
      case '|':
      case '¦': // Spanish keyboard
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[1] |= KEY_RELEASED_BIT1; // S
        break;
      case '}':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[1] |= KEY_RELEASED_BIT4; // G
        break;
      case '~':
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[1] |= KEY_RELEASED_BIT0; // A
        break;
      case '©': // Mac only
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[5] |= KEY_RELEASED_BIT0; // P
        break;
      case '`':
      case '§': // Mac only
      case '¡': // Spanish keyboard
//                rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
        rowKey[3] |= KEY_RELEASED_BIT0; // 1
        break;
      case '¬':
      case '±': // Mac only
      case '¿': // Spanish keyboard
//                rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
        rowKey[4] |= KEY_RELEASED_BIT1; // G (Graphics mode)
        break;
      case '£': // Pound sign
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift
        rowKey[0] |= KEY_RELEASED_BIT2; // X
        break;
      case 'º': // Spanish keyboard only
        rowKey[0] |= KEY_RELEASED_BIT0; // Caps Shift
        rowKey[7] |= KEY_RELEASED_BIT1; // Symbol Shift -- Extended Mode
        break;
      default:
        if (shiftPressed) {
          rowKey[0] &= KEY_PRESSED_BIT0; // Caps Shift
        }
        done = false;
    }
    return done;
  }
}
