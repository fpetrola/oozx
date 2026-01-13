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

package com.fpetrola.oozx.speccy.devices.ay;


import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;

/**
 * The same sound chip, wired as a +2A and a +3 wire it.
 * <p>
 * It differs in one wire: the data port answers when read, where on a 128K it does not.
 */
@com.google.inject.Singleton
public class AyPlus3Peripheral extends AyPeripheral {

  @Inject
  public AyPlus3Peripheral(Sound sound, Z80Clock clock) {
    super(sound, clock, true);
  }

}
