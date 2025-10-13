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

package com.fpetrola.oozx.fuse.modules;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.*;
import com.fpetrola.oozx.fuse.peripherals.KempstonLoosePeriphPeripheral;
import com.fpetrola.oozx.fuse.peripherals.KempstonStrictPeripheral;
import com.fpetrola.oozx.fuse.peripherals.Periph;

public class Joystick {

  // Constants
  public final int JOYSTICK_KEYBOARD = 2;
  public final int JOYSTICK_TYPE_COUNT = 8;
  public final int JOYSTICK_CONN_COUNT = 4;

  // Number of joysticks supported
  public int joysticksSupported = 0;
  private Keyboard keyboard;
  private Periph periph;

  public Joystick(Keyboard keyboard, Periph periph) {
    this.keyboard = keyboard;
    this.periph = periph;
  }

  public int init() {
    joysticksSupported = UiJoystick.init();
    kempstonValue = timex1Value = timex2Value = 0x00;
    fullerValue = (byte) 0xff;

    Module.register(new JoystickModuleInfo(this));
    periph.register(new KempstonStrictPeripheral(this));
    periph.register(new KempstonLoosePeriphPeripheral(this));

    return 0;
  }

  // Joystick types
  public enum JoystickType {
    JOYSTICK_TYPE_NONE,
    JOYSTICK_TYPE_CURSOR,
    JOYSTICK_TYPE_KEMPSTON,
    JOYSTICK_TYPE_SINCLAIR_1,
    JOYSTICK_TYPE_SINCLAIR_2,
    JOYSTICK_TYPE_TIMEX_1,
    JOYSTICK_TYPE_TIMEX_2,
    JOYSTICK_TYPE_FULLER
  }

  // Joystick buttons (from Input.java)
  public enum JoystickButton {
    JOYSTICK_BUTTON_LEFT,
    JOYSTICK_BUTTON_RIGHT,
    JOYSTICK_BUTTON_UP,
    JOYSTICK_BUTTON_DOWN,
    JOYSTICK_BUTTON_FIRE
  }

  // Joystick names
  public final String[] JOYSTICK_NAME = {
      "None", "Cursor", "Kempston", "Sinclair 1", "Sinclair 2",
      "Timex 1", "Timex 2", "Fuller"
  };

  // Joystick connection names
  public final String[] JOYSTICK_CONNECTION = {
      "None", "Keyboard", "Joystick 1", "Joystick 2"
  };

  // Bit masks for joystick types
  private final byte[] KEMPSTON_MASK = {0x02, 0x01, 0x08, 0x04, 0x10};
  private final byte[] TIMEX_MASK = {0x04, 0x08, 0x01, 0x02, (byte) 0x80};

  // Keys for joystick emulation
  private final KeyboardKeyName[] CURSOR_KEY = {
      KeyboardKeyName.KEYBOARD_5, KeyboardKeyName.KEYBOARD_8,
      KeyboardKeyName.KEYBOARD_7, KeyboardKeyName.KEYBOARD_6,
      KeyboardKeyName.KEYBOARD_0
  };

  private final KeyboardKeyName[] SINCLAIR1_KEY = {
      KeyboardKeyName.KEYBOARD_6, KeyboardKeyName.KEYBOARD_7,
      KeyboardKeyName.KEYBOARD_9, KeyboardKeyName.KEYBOARD_8,
      KeyboardKeyName.KEYBOARD_0
  };

  private final KeyboardKeyName[] SINCLAIR2_KEY = {
      KeyboardKeyName.KEYBOARD_1, KeyboardKeyName.KEYBOARD_2,
      KeyboardKeyName.KEYBOARD_4, KeyboardKeyName.KEYBOARD_3,
      KeyboardKeyName.KEYBOARD_5
  };

  // Current joystick values
  byte kempstonValue = 0x00;
  byte timex1Value = 0x00;
  byte timex2Value = 0x00;
  byte fullerValue = (byte) 0xff;

  // Register startup

//    private  void reg1() {
//        StartupManagerModule[] dependencies = {
//            StartupManagerModule.LIBSPECTRUM,
//            StartupManagerModule.SETUID
//        };
//        StartupManager.register(StartupManagerModule.JOYSTICK,
//                                dependencies, Joystick::joystickInit, null, Joystick::end);
//    }

  // Initialize joysticks

  // Cleanup joysticks
  public void end() {
    UiJoystick.end();
  }

  // Handle joystick button press/release
  public boolean press(int which, JoystickButton button, boolean press) {
    JoystickType type;
    switch (which) {
      case 0:
        type = Settings.current.joystick1Output;
        break;
      case 1:
        type = Settings.current.joystick2Output;
        break;
      case JOYSTICK_KEYBOARD:
        type = Settings.current.joystickKeyboardOutput;
        break;
      default:
        return false;
    }

    switch (type) {
      case JOYSTICK_TYPE_CURSOR:
        if (press) {
          keyboard.press(CURSOR_KEY[button.ordinal()]);
        } else {
          keyboard.release(CURSOR_KEY[button.ordinal()]);
        }
        return true;

      case JOYSTICK_TYPE_KEMPSTON:
        if (press) {
          kempstonValue |= KEMPSTON_MASK[button.ordinal()];
        } else {
          kempstonValue &= ~KEMPSTON_MASK[button.ordinal()];
        }
        return true;

      case JOYSTICK_TYPE_SINCLAIR_1:
        if (press) {
          keyboard.press(SINCLAIR1_KEY[button.ordinal()]);
        } else {
          keyboard.release(SINCLAIR1_KEY[button.ordinal()]);
        }
        return true;

      case JOYSTICK_TYPE_SINCLAIR_2:
        if (press) {
          keyboard.press(SINCLAIR2_KEY[button.ordinal()]);
        } else {
          keyboard.release(SINCLAIR2_KEY[button.ordinal()]);
        }
        return true;

      case JOYSTICK_TYPE_TIMEX_1:
        if (press) {
          timex1Value |= TIMEX_MASK[button.ordinal()];
        } else {
          timex1Value &= ~TIMEX_MASK[button.ordinal()];
        }
        return true;

      case JOYSTICK_TYPE_TIMEX_2:
        if (press) {
          timex2Value |= TIMEX_MASK[button.ordinal()];
        } else {
          timex2Value &= ~TIMEX_MASK[button.ordinal()];
        }
        return true;

      case JOYSTICK_TYPE_FULLER:
        if (press) {
          fullerValue &= ~TIMEX_MASK[button.ordinal()];
        } else {
          fullerValue |= TIMEX_MASK[button.ordinal()];
        }
        return true;

      case JOYSTICK_TYPE_NONE:
        return false;

      default:
        Ui.error(UIErrorLevel.UI_ERROR_ERROR, "joystick_press: unknown joystick type %d", type.ordinal());
        throw new RuntimeException("Unknown joystick type");
    }
  }

  // Read functions for specific interfaces
  public byte kempstonRead(int port, byte[] attached) {
    attached[0] = (byte) 0xff; // TODO: Verify correct value
    return kempstonValue;
  }

  public byte timexRead(int port, int which) {
    return which != 0 ? timex2Value : timex1Value;
  }

  public byte fullerRead(int port, byte[] attached) {
    attached[0] = (byte) 0xff; // TODO: Verify correct value
    return fullerValue;
  }

  // Snapshot handling
  public void enabledSnapshot(Libspectrum.Snap snap) {
    int numJoysticks = snap.joystickActiveCount();
    for (int i = 0; i < numJoysticks; i++) {
      JoystickType fuseType;
      switch (snap.joystickList(i)) {
        case JOYSTICK_TYPE_CURSOR:
          fuseType = JoystickType.JOYSTICK_TYPE_CURSOR;
          break;
        case JOYSTICK_TYPE_KEMPSTON:
          fuseType = JoystickType.JOYSTICK_TYPE_KEMPSTON;
          break;
        case JOYSTICK_TYPE_SINCLAIR_1:
          fuseType = JoystickType.JOYSTICK_TYPE_SINCLAIR_1;
          break;
        case JOYSTICK_TYPE_SINCLAIR_2:
          fuseType = JoystickType.JOYSTICK_TYPE_SINCLAIR_2;
          break;
        case JOYSTICK_TYPE_TIMEX_1:
          fuseType = JoystickType.JOYSTICK_TYPE_TIMEX_1;
          break;
        case JOYSTICK_TYPE_TIMEX_2:
          fuseType = JoystickType.JOYSTICK_TYPE_TIMEX_2;
          break;
        case JOYSTICK_TYPE_FULLER:
          fuseType = JoystickType.JOYSTICK_TYPE_FULLER;
          break;
        default:
          Ui.error(UIErrorLevel.UI_ERROR_INFO, "Ignoring unsupported joystick in snapshot %s");
          continue;
      }

      if (Settings.current.joystickKeyboardOutput != fuseType &&
          Settings.current.joystick1Output != fuseType &&
          Settings.current.joystick2Output != fuseType &&
          !Rzx.playback) {
        Ui.UIConfirmJoystick result = Ui.confirmJoystick(snap.joystickList(i), snap.joystickInputs(i));
        switch (result) {
          case UI_CONFIRM_JOYSTICK_KEYBOARD:
            Settings.current.joystickKeyboardOutput = fuseType;
            break;
          case UI_CONFIRM_JOYSTICK_JOYSTICK_1:
            Settings.current.joystick1Output = fuseType;
            break;
          case UI_CONFIRM_JOYSTICK_JOYSTICK_2:
            Settings.current.joystick2Output = fuseType;
            break;
          case UI_CONFIRM_JOYSTICK_NONE:
            break;
        }
      }

      if (fuseType == JoystickType.JOYSTICK_TYPE_KEMPSTON) {
        Settings.current.joyKempston = true;
      }
    }
  }

  public void toSnapshot(Libspectrum.Snap snap) {
    if (Settings.current.joyKempston) {
      addJoystick(snap, JoystickType.JOYSTICK_TYPE_KEMPSTON, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_NONE);
    }
    addJoystick(snap, Settings.current.joystickKeyboardOutput, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_KEYBOARD);
    addJoystick(snap, Settings.current.joystick1Output, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_1);
    addJoystick(snap, Settings.current.joystick2Output, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_2);
  }

  private void addJoystick(Libspectrum.Snap snap, JoystickType fuseType, int inputs) {
    JoystickType libspectrumType;
    switch (fuseType) {
      case JOYSTICK_TYPE_CURSOR:
        libspectrumType = JoystickType.JOYSTICK_TYPE_CURSOR;
        break;
      case JOYSTICK_TYPE_KEMPSTON:
        libspectrumType = JoystickType.JOYSTICK_TYPE_KEMPSTON;
        break;
      case JOYSTICK_TYPE_SINCLAIR_1:
        libspectrumType = JoystickType.JOYSTICK_TYPE_SINCLAIR_1;
        break;
      case JOYSTICK_TYPE_SINCLAIR_2:
        libspectrumType = JoystickType.JOYSTICK_TYPE_SINCLAIR_2;
        break;
      case JOYSTICK_TYPE_TIMEX_1:
        libspectrumType = JoystickType.JOYSTICK_TYPE_TIMEX_1;
        break;
      case JOYSTICK_TYPE_TIMEX_2:
        libspectrumType = JoystickType.JOYSTICK_TYPE_TIMEX_2;
        break;
      case JOYSTICK_TYPE_FULLER:
        libspectrumType = JoystickType.JOYSTICK_TYPE_FULLER;
        break;
      case JOYSTICK_TYPE_NONE:
      default:
        return;
    }

    int numJoysticks = snap.joystickActiveCount();
    for (int i = 0; i < numJoysticks; i++) {
      if (snap.joystickList(i) == libspectrumType) {
        snap.setJoystickInputs(i, inputs | snap.joystickInputs(i));
        return;
      }
    }

    snap.setJoystickList(numJoysticks, libspectrumType);
    snap.setJoystickInputs(numJoysticks, inputs);
    snap.setJoystickActiveCount(numJoysticks + 1);
  }

}