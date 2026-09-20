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

  /** How long after a read the joystick still counts as in use: a few frames. */
  private static final long IN_USE_NANOS = 250_000_000L;

  private long lastKempstonRead = Long.MIN_VALUE / 2;
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

  /** The machine reading its Kempston port, which is also what says the joystick is being used. */
  public BusAnswer kempstonRead(int port) {
    lastKempstonRead = System.nanoTime();
    return BusAnswer.of(kempstonReads());
  }

  /** What that port answers, for anything showing the joystick rather than playing with it. */
  public byte kempstonReads() {
    return kinds.get(JoystickType.JOYSTICK_TYPE_KEMPSTON).reads();
  }

  /**
   * Whether something on the machine has just read the Kempston port.
   * <p>
   * A game that reads it reads it every frame, whether or not anything is pushed, so this is on
   * throughout a game that uses the joystick and off in one that does not - which is what tells
   * the keys standing in for a pad whether the machine wants them as a joystick or as keys.
   */
  public boolean kempstonInUse() {
    return System.nanoTime() - lastKempstonRead < IN_USE_NANOS;
  }

  /** The Timex has two ports; which one is asked for. */
  public byte timexRead(int port, int which) {
    return kinds.get(which != 0 ? JoystickType.JOYSTICK_TYPE_TIMEX_2 : JoystickType.JOYSTICK_TYPE_TIMEX_1).reads();
  }

  public BusAnswer fullerRead(int port) {
    return BusAnswer.of(kinds.get(JoystickType.JOYSTICK_TYPE_FULLER).reads());
  }
}
