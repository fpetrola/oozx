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

package com.fpetrola.oozx.speccy.modules.keyboard;

import com.fpetrola.oozx.speccy.modules.input.Input;

/**
 * What the keys of whoever is typing produce on the Spectrum. A layout, because there is more
 * than one: the keys where they are on a PC, and the Recreated ZX, whose keyboard sends a letter
 * per Spectrum key and needs the ones before it to say which.
 */
public interface KeyLayout {
  /** What that key of the host puts down, or {@link Combination#NONE} for one this layout ignores. */
  Combination produces(Input.InputKey pressed);

  /** Told of every press before {@link #produces}, for a layout that reads a key in what came before it. */
  default void pressed(Input.InputKey key) {
  }

  /** Whether that key is released by pressing it again rather than by letting go, as the Recreated does. */
  default boolean releasedByPressing(Input.InputKey key) {
    return false;
  }
}
