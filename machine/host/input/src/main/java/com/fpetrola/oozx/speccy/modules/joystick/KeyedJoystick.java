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
