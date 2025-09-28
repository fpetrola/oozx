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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;

import java.util.List;

import static com.fpetrola.oozx.Ui.UIErrorLevel.UI_ERROR_ERROR;
import static com.fpetrola.oozx.Ui.UIErrorLevel.UI_ERROR_INFO;

public class Joystick {

    // Constants
    public static final int JOYSTICK_KEYBOARD = 2;
    public static final int JOYSTICK_TYPE_COUNT = 8;
    public static final int JOYSTICK_CONN_COUNT = 4;

    // Number of joysticks supported
    public static int joysticksSupported = 0;

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
    public static final String[] JOYSTICK_NAME = {
        "None", "Cursor", "Kempston", "Sinclair 1", "Sinclair 2",
        "Timex 1", "Timex 2", "Fuller"
    };

    // Joystick connection names
    public static final String[] JOYSTICK_CONNECTION = {
        "None", "Keyboard", "Joystick 1", "Joystick 2"
    };

    // Bit masks for joystick types
    private static final byte[] KEMPSTON_MASK = { 0x02, 0x01, 0x08, 0x04, 0x10 };
    private static final byte[] TIMEX_MASK = { 0x04, 0x08, 0x01, 0x02, (byte) 0x80 };

    // Keys for joystick emulation
    private static final KeyboardKeyName[] CURSOR_KEY = {
        KeyboardKeyName.KEYBOARD_5, KeyboardKeyName.KEYBOARD_8,
        KeyboardKeyName.KEYBOARD_7, KeyboardKeyName.KEYBOARD_6,
        KeyboardKeyName.KEYBOARD_0
    };

    private static final KeyboardKeyName[] SINCLAIR1_KEY = {
        KeyboardKeyName.KEYBOARD_6, KeyboardKeyName.KEYBOARD_7,
        KeyboardKeyName.KEYBOARD_9, KeyboardKeyName.KEYBOARD_8,
        KeyboardKeyName.KEYBOARD_0
    };

    private static final KeyboardKeyName[] SINCLAIR2_KEY = {
        KeyboardKeyName.KEYBOARD_1, KeyboardKeyName.KEYBOARD_2,
        KeyboardKeyName.KEYBOARD_4, KeyboardKeyName.KEYBOARD_3,
        KeyboardKeyName.KEYBOARD_5
    };

    // Current joystick values
    private static byte kempstonValue = 0x00;
    private static byte timex1Value = 0x00;
    private static byte timex2Value = 0x00;
    private static byte fullerValue = (byte) 0xff;

    // Register startup
    public static void registerStartup() {
        StartupManagerModule[] dependencies = {
            StartupManagerModule.LIBSPECTRUM,
            StartupManagerModule.SETUID
        };
        StartupManager.register(StartupManagerModule.JOYSTICK,
                                dependencies, Joystick::joystickInit, null, Joystick::end);
    }

    // Initialize joysticks
    private static int joystickInit(Object context) {
        joysticksSupported = UiJoystick.init();
        kempstonValue = timex1Value = timex2Value = 0x00;
        fullerValue = (byte) 0xff;

        Module.register(joystickModuleInfo);
        Periph.register(Periph.Type.KEMPSTON, kempstonStrictPeriph);
        Periph.register(Periph.Type.KEMPSTON_LOOSE, kempstonLoosePeriph);

        return 0;
    }

    // Cleanup joysticks
    private static void end() {
        UiJoystick.end();
    }

    // Handle joystick button press/release
    public static boolean press(int which, JoystickButton button, boolean press) {
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
                    Keyboard.press(CURSOR_KEY[button.ordinal()]);
                } else {
                    Keyboard.release(CURSOR_KEY[button.ordinal()]);
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
                    Keyboard.press(SINCLAIR1_KEY[button.ordinal()]);
                } else {
                    Keyboard.release(SINCLAIR1_KEY[button.ordinal()]);
                }
                return true;

            case JOYSTICK_TYPE_SINCLAIR_2:
                if (press) {
                    Keyboard.press(SINCLAIR2_KEY[button.ordinal()]);
                } else {
                    Keyboard.release(SINCLAIR2_KEY[button.ordinal()]);
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
                Ui.error(UI_ERROR_ERROR, "joystick_press: unknown joystick type %d", type.ordinal());
                throw new RuntimeException("Unknown joystick type");
        }
    }

    // Read functions for specific interfaces
    public static byte kempstonRead(int port, byte[] attached) {
        attached[0] = (byte) 0xff; // TODO: Verify correct value
        return kempstonValue;
    }

    public static byte timexRead(int port, int which) {
        return which != 0 ? timex2Value : timex1Value;
    }

    public static byte fullerRead(int port, byte[] attached) {
        attached[0] = (byte) 0xff; // TODO: Verify correct value
        return fullerValue;
    }

    // Snapshot handling
    public static void enabledSnapshot(Libspectrum.Snap snap) {
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
                    Ui.error(UI_ERROR_INFO, "Ignoring unsupported joystick in snapshot %s");
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

    private static void toSnapshot(Libspectrum.Snap snap) {
        if (Settings.current.joyKempston) {
            addJoystick(snap, JoystickType.JOYSTICK_TYPE_KEMPSTON, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_NONE);
        }
        addJoystick(snap, Settings.current.joystickKeyboardOutput, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_KEYBOARD);
        addJoystick(snap, Settings.current.joystick1Output, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_1);
        addJoystick(snap, Settings.current.joystick2Output, Libspectrum.LIBSPECTRUM_JOYSTICK_INPUT_JOYSTICK_2);
    }

    private static void addJoystick(Libspectrum.Snap snap, JoystickType fuseType, int inputs) {
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

    // Module info for snapshot handling
    private static final ModuleInfo joystickModuleInfo = new ModuleInfo(
        null, // reset
        null, // romcs
        Joystick::enabledSnapshot, // snapshot_enabled
        null, // snapshot_from
        Joystick::toSnapshot // snapshot_to
    );

    // Peripheral definitions
    private static final Periph.Port[] kempstonStrictDecoding = {
        new Periph.Port(0x00e0, 0x0000, Joystick::kempstonRead, null),
        new Periph.Port(0, 0, null, null)
    };

    private static final Periph.Peripheral kempstonStrictPeriph = new Periph.Peripheral(
        new boolean[]{Settings.current.joyKempston}, List.of(kempstonStrictDecoding), false, null
    );

    private static final Periph.Port[] kempstonLooseDecoding = {
        new Periph.Port(0x0020, 0x0000, Joystick::kempstonRead, null),
        new Periph.Port(0, 0, null, null)};

    private static final Periph.Peripheral kempstonLoosePeriph = new Periph.Peripheral(
        new boolean[]{Settings.current.joyKempston}, List.of(kempstonLooseDecoding), false, null
    );
}