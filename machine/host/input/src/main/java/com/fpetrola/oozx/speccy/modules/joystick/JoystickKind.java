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

/**
 * A kind of joystick: what pushing it does. The Spectrum had no port of its own for one, so
 * every maker solved it differently - some added a port, and some wired the stick to keys of the
 * machine's own keyboard - which is why this is a kind and not a flag.
 */
public interface JoystickKind {
  /** @return whether this took the push, so whoever pushed does not also send it to the keyboard */
  boolean push(Direction direction, boolean pushed);

  /** What its port reads, for the kinds that have one. */
  default byte reads() {
    return 0;
  }
}
