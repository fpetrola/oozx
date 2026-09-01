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

package com.fpetrola.oozx.speccy.modules;

import com.fpetrola.oozx.UserInterface;

import com.fpetrola.oozx.PeripheralBusDelegate;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.KeyboardKeyName;
import com.fpetrola.oozx.speccy.peripherals.PeripheralBus;
import com.fpetrola.oozx.speccy.devices.joystick.KempstonLoosePeriphPeripheral;
import com.fpetrola.oozx.speccy.devices.joystick.KempstonStrictPeripheral;

@Singleton
public class Joystick implements ZxModule {

  // Constants
  public final int JOYSTICK_KEYBOARD = 2;
  public final int JOYSTICK_TYPE_COUNT = 8;
  public final int JOYSTICK_CONN_COUNT = 4;

  // Number of joysticks supported
  public int joysticksSupported = 0;
  private Keyboard keyboard;
  private PeripheralBus peripherals;
  private Module module;
  private Settings settings;
  private final UserInterface userInterface;

  @Inject
  public Joystick(Keyboard keyboard, PeripheralBusDelegate peripherals, Module module, Settings settings, UserInterface userInterface) {
    this.userInterface = userInterface;
    this.keyboard = keyboard;
    this.peripherals = peripherals;
    this.module = module;
    this.settings = settings;
  }

  public void start() {
    joysticksSupported = UiJoystick.init();
    kempstonValue = timex1Value = timex2Value = 0x00;
    fullerValue = (byte) 0xff;

    peripherals.register(new KempstonStrictPeripheral(this, () -> settings.current.joyKempston));
    peripherals.register(new KempstonLoosePeriphPeripheral(this, () -> settings.current.joyKempston));

    return;
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
        type = settings.current.joystick1Output;
        break;
      case 1:
        type = settings.current.joystick2Output;
        break;
      case JOYSTICK_KEYBOARD:
        type = settings.current.joystickKeyboardOutput;
        break;
      default:
        return false;
    }

    if (type == null)
      type= JoystickType.JOYSTICK_TYPE_KEMPSTON;

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
        userInterface.error(UIErrorLevel.UI_ERROR_ERROR, "joystick_press: unknown joystick type %d", type.ordinal());
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
      JoystickType joystickType;
      switch (snap.joystickList(i)) {
        case JOYSTICK_TYPE_CURSOR:
          joystickType = JoystickType.JOYSTICK_TYPE_CURSOR;
          break;
        case JOYSTICK_TYPE_KEMPSTON:
          joystickType = JoystickType.JOYSTICK_TYPE_KEMPSTON;
          break;
        case JOYSTICK_TYPE_SINCLAIR_1:
          joystickType = JoystickType.JOYSTICK_TYPE_SINCLAIR_1;
          break;
        case JOYSTICK_TYPE_SINCLAIR_2:
          joystickType = JoystickType.JOYSTICK_TYPE_SINCLAIR_2;
          break;
        case JOYSTICK_TYPE_TIMEX_1:
          joystickType = JoystickType.JOYSTICK_TYPE_TIMEX_1;
          break;
        case JOYSTICK_TYPE_TIMEX_2:
          joystickType = JoystickType.JOYSTICK_TYPE_TIMEX_2;
          break;
        case JOYSTICK_TYPE_FULLER:
          joystickType = JoystickType.JOYSTICK_TYPE_FULLER;
          break;
        default:
          userInterface.error(UIErrorLevel.UI_ERROR_INFO, "Ignoring unsupported joystick in snapshot %s");
          continue;
      }

      if (settings.current.joystickKeyboardOutput != joystickType &&
          settings.current.joystick1Output != joystickType &&
          settings.current.joystick2Output != joystickType) {
        UserInterface.ConfirmJoystick result = userInterface.confirmJoystick(snap.joystickList(i), snap.joystickInputs(i));
        switch (result) {
          case KEYBOARD:
            settings.current.joystickKeyboardOutput = joystickType;
            break;
          case JOYSTICK_1:
            settings.current.joystick1Output = joystickType;
            break;
          case JOYSTICK_2:
            settings.current.joystick2Output = joystickType;
            break;
          case NONE:
            break;
        }
      }

      if (joystickType == JoystickType.JOYSTICK_TYPE_KEMPSTON) {
        settings.current.joyKempston = true;
      }
    }
  }

  public void toSnapshot(Libspectrum.Snap snap) {
    if (settings.current.joyKempston) {
      addJoystick(snap, JoystickType.JOYSTICK_TYPE_KEMPSTON, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_NONE);
    }
    addJoystick(snap, settings.current.joystickKeyboardOutput, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_KEYBOARD);
    addJoystick(snap, settings.current.joystick1Output, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_1);
    addJoystick(snap, settings.current.joystick2Output, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_2);
  }

  private void addJoystick(Libspectrum.Snap snap, JoystickType joystickType, int inputs) {
    JoystickType libspectrumType;
    switch (joystickType) {
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