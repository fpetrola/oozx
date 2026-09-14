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

package com.fpetrola.oozx.speccy.modules.display;

/**
 * A chip that gives a machine colours of its own instead of the sixteen everybody shares.
 * <p>
 * Here because a snapshot is a machine as it stood, and the colours it stood in are part of that:
 * whoever puts a snapshot back has to be able to say "and these were the colours" without knowing
 * which chip it is talking to, and nothing else in the way this emulator is laid out lets it.
 */
public interface ColoursOfItsOwn {
  /** Whether the machine has the chip at all, which is one of the things a snapshot says about it. */
  void fitted(boolean modified);

  /** The colours as they were, and whether the machine was painting in them at the time. */
  void asItWas(int[] colours, boolean painting);
}
