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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Sound;

/**
 * The same sound chip, wired as a +2A and a +3 wire it.
 * <p>
 * Those machines are given {@link Periph.Type#AY_PLUS3} rather than {@link Periph.Type#AY}, and
 * that entry carried no class - so the chip was declared always present on a +3 and nothing was
 * there to be present. A 128K game arriving on a +2A had a beeper and no music, which is the same
 * silence the plain AY had before it was given a class, in the same enum, one line below.
 * <p>
 * It differs in one wire: the data port answers when read, where on a 128K it does not.
 */
public class AyPlus3Peripheral extends AyPeripheral {

  public AyPlus3Peripheral(Sound sound, SpectrumZ80Clock clock) {
    super(Periph.Type.AY_PLUS3, sound, clock, true);
  }
}
