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

package com.fpetrola.oozx.speccy.ports;

/**
 * The data lines: what a byte read from the bus is, given who answered.
 * <p>
 * The first to drive the bus wins it; one that answers without driving still pulls
 * down the bits it holds low, which is how the Disciple is heard on a port it does not own; and
 * a bus nobody drove reads as whatever floats, which is only asked for then, since finding out
 * costs a look at the screen. Locals and no object, so a read costs the same whether or not the
 * JIT saw through the calls around it.
 */
final class BusArbiter {
  private BusArbiter() {
  }

  static byte settle(PortHandler[] readers, int port, DecodedPortBus.FloatingBus floating) {
    int value = 0xff;
    boolean driven = false;
    for (PortHandler reader : readers) {
      BusAnswer answer = reader.read(port);
      value &= answer.value();
      driven |= answer.driven();
    }
    return (byte) (driven ? value : value & floating.at(port));
  }
}
