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

/**
 * A device carrying a ROM of its own, which appears over the machine's while /ROMCS is held low.
 * <p>
 * Separate from {@link ZxModule} because carrying a ROM is something a device either does or does
 * not: the Beta 128, the Interface 1, DivIDE and the Multifaces do, and a sound chip never will.
 * Seventeen of Fuse's thirty-six modules are these, and they are the seventeen with a ROM - it is
 * not a statistic, it is the definition.
 * <p>
 * Named for the line rather than for the effect, because every device still to be migrated calls
 * it that.
 */
public interface RomcsDevice {

  /** This device's ROM is paged in over the machine's; map it. */
  void mapRom();
}
