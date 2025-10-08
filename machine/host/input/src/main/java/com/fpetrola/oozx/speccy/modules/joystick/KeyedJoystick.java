/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import com.fpetrola.oozx.speccy.modules.keyboard.Keyboard;
import com.fpetrola.oozx.speccy.modules.keyboard.SpectrumKey;

/**
 * A joystick wired to the machine's own keys, which is what a Spectrum with no joystick port
 * left everyone doing: a Cursor, and the two a Sinclair Interface 2 has, are the same stick with
 * a different five keys under it.
 */
public final class KeyedJoystick implements JoystickKind {
  private final Keyboard keyboard;
  private final SpectrumKey[] keys;

  /** Left, right, up, down and fire, in that order. */
  public KeyedJoystick(Keyboard keyboard, SpectrumKey... keys) {
    this.keyboard = keyboard;
    this.keys = keys;
  }

  public boolean push(Direction direction, boolean pushed) {
    SpectrumKey key = keys[direction.ordinal()];
    if (pushed) {
      keyboard.press(key);
    } else {
      keyboard.release(key);
    }
    return true;
  }
}
