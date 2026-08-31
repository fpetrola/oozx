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

package com.fpetrola.oozx.speccy.devices.ay;

import com.fpetrola.oozx.speccy.peripherals.Periph;

import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.function.BooleanSupplier;

/**
 * A box with an AY in it, for a machine that has not got one.
 * <p>
 * The Melodik decodes the chip exactly as a 128 does, which is the point of it: music written for
 * a 128K plays on a 48K with one of these plugged in. So it is that same chip, and the only
 * difference is that a machine comes with one of those and somebody chose this.
 */
public class MelodikPeripheral extends AyPeripheral {

  private final BooleanSupplier wanted;

  public MelodikPeripheral(Sound sound, Z80Clock clock, BooleanSupplier wanted) {
    super(Periph.Type.MELODIK, sound, clock, false);
    this.wanted = wanted;
  }

  @Override
  public boolean isWanted() {
    return wanted.getAsBoolean();
  }
}
