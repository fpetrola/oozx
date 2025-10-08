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

package com.fpetrola.oozx.speccy.modules.joystick;

import com.fpetrola.oozx.speccy.modules.input.Input;
import com.fpetrola.oozx.speccy.modules.keyboard.Keyboard;
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.EnumMap;
import java.util.Map;

/**
 * The three sockets a joystick can be plugged into - two pads and the machine's own keys used as
 * one - and what is plugged into each.
 * <p>
 * What a kind of joystick does when it is pushed is the kind's; this holds one per socket and
 * hands the push on. The ports the ones with a port of their own answer are read here because
 * that is where a peripheral asks: a Kempston interface does not know which socket it is.
 */
@Singleton
public class Joystick {
  /** The machine's own keys used as a joystick, as against the two pads, which are 0 and 1. */
  public final int JOYSTICK_KEYBOARD = 2;

  /** What can be plugged into a socket. The names are the ones the settings and the windows use. */
  public enum JoystickType {
    JOYSTICK_TYPE_NONE, JOYSTICK_TYPE_CURSOR, JOYSTICK_TYPE_KEMPSTON,
    JOYSTICK_TYPE_SINCLAIR_1, JOYSTICK_TYPE_SINCLAIR_2,
    JOYSTICK_TYPE_TIMEX_1, JOYSTICK_TYPE_TIMEX_2, JOYSTICK_TYPE_FULLER
  }

  /** Kept for the callers that name a direction this way. */
  public enum JoystickButton {
    JOYSTICK_BUTTON_LEFT, JOYSTICK_BUTTON_RIGHT, JOYSTICK_BUTTON_UP,
    JOYSTICK_BUTTON_DOWN, JOYSTICK_BUTTON_FIRE
  }

  public final String[] JOYSTICK_NAME = {
      "None", "Cursor", "Kempston", "Sinclair 1", "Sinclair 2", "Timex 1", "Timex 2", "Fuller"
  };

  public final String[] JOYSTICK_CONNECTION = {"None", "Keyboard", "Joystick 1", "Joystick 2"};

  private final Keyboard keyboard;
  private final Input.Setup setup;
  /** One per kind, made once: what a Kempston reads is the same whichever socket it is in. */
  private final Map<JoystickType, JoystickKind> kinds = new EnumMap<>(JoystickType.class);

  @Inject
  public Joystick(Keyboard keyboard, Input.Setup setup) {
    this.keyboard = keyboard;
    this.setup = setup;
    kinds.put(JoystickType.JOYSTICK_TYPE_NONE, new NoJoystick());
    kinds.put(JoystickType.JOYSTICK_TYPE_KEMPSTON, PortedJoystick.kempston());
    kinds.put(JoystickType.JOYSTICK_TYPE_TIMEX_1, PortedJoystick.timex());
    kinds.put(JoystickType.JOYSTICK_TYPE_TIMEX_2, PortedJoystick.timex());
    kinds.put(JoystickType.JOYSTICK_TYPE_FULLER, PortedJoystick.fuller());
    kinds.put(JoystickType.JOYSTICK_TYPE_CURSOR, new KeyedJoystick(keyboard,
        SpectrumKey.FIVE, SpectrumKey.EIGHT, SpectrumKey.SEVEN, SpectrumKey.SIX, SpectrumKey.ZERO));
    kinds.put(JoystickType.JOYSTICK_TYPE_SINCLAIR_1, new KeyedJoystick(keyboard,
        SpectrumKey.SIX, SpectrumKey.SEVEN, SpectrumKey.NINE, SpectrumKey.EIGHT, SpectrumKey.ZERO));
    kinds.put(JoystickType.JOYSTICK_TYPE_SINCLAIR_2, new KeyedJoystick(keyboard,
        SpectrumKey.ONE, SpectrumKey.TWO, SpectrumKey.FOUR, SpectrumKey.THREE, SpectrumKey.FIVE));
  }

  /** What is plugged into that socket, which is a setting; a pad with nothing chosen is a Kempston. */
  private JoystickKind in(int socket) {
    JoystickType type = switch (socket) {
      case 0 -> setup.joystick1.output;
      case 1 -> setup.joystick2.output;
      case JOYSTICK_KEYBOARD -> setup.keyboard.output;
      default -> null;
    };
    if (type == null) {
      type = JoystickType.JOYSTICK_TYPE_KEMPSTON;
    }
    return kinds.get(type);
  }

  /** @return whether the joystick took the push, so whoever pushed does not send it on */
  public boolean press(int socket, JoystickButton button, boolean pushed) {
    JoystickKind kind = socket == 0 || socket == 1 || socket == JOYSTICK_KEYBOARD ? in(socket) : null;
    return kind != null && kind.push(Direction.values()[button.ordinal()], pushed);
  }

  public BusAnswer kempstonRead(int port) {
    return BusAnswer.of(kinds.get(JoystickType.JOYSTICK_TYPE_KEMPSTON).reads());
  }

  /** The Timex has two ports; which one is asked for. */
  public byte timexRead(int port, int which) {
    return kinds.get(which != 0 ? JoystickType.JOYSTICK_TYPE_TIMEX_2 : JoystickType.JOYSTICK_TYPE_TIMEX_1).reads();
  }

  public BusAnswer fullerRead(int port) {
    return BusAnswer.of(kinds.get(JoystickType.JOYSTICK_TYPE_FULLER).reads());
  }
}
