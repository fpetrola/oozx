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

import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import com.fpetrola.z80.cpu.Z80Clock;

/**
 * One AY port: 0xFFFD selects one of 16 registers, 0xBFFD writes its value. Decoded by address
 * line (bits 14-15 pick the port, bit 1 must be low) rather than full port number, matching how
 * the real hardware and the rest of this package address it.
 */
class AyPortHandler extends DefaultPortHandler {

  private final AyRegisters registers;
  private final AyPeripheral owner;
  private final boolean selects;
  private final Z80Clock clock;

  public AyPortHandler(boolean selects, AyRegisters registers, AyPeripheral owner,
                       Z80Clock clock) {
    this(selects, registers, owner, clock, false);
  }

  /**
   * @param alsoAnswers if the data port should also read back, like the +3's does; the select
   *                     port always reads back, since read-after-write is how software detects
   *                     the chip's presence.
   */
  public AyPortHandler(boolean selects, AyRegisters registers, AyPeripheral owner,
                       Z80Clock clock, boolean alsoAnswers) {
    super(selects || alsoAnswers, true);
    this.selects = selects;
    this.registers = registers;
    this.owner = owner;
    this.clock = clock;
  }

  @Override
  public void write(int port, byte value) {
    if (selects) {
      registers.select(value);
    } else {
      registers.write(value);
      // The T-state timestamp matters: without it every queued write plays at frame start.
      owner.heard(registers.current(), value & 0xFF, clock.getTStates());
    }
  }

  @Override
  public BusAnswer read(int port) {
    return BusAnswer.of(registers.read());
  }
}
