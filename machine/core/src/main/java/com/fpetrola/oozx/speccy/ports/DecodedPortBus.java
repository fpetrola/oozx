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

package com.fpetrola.oozx.speccy.ports;

/**
 * The bottom of the {@link PortBus} stack, the bus proper: whoever answers the port fights over
 * the data lines, and whoever latches on any I/O cycle captures the result.
 * <p>
 * No clock, no machine, no peripherals: it only knows handlers. Everything expensive - working
 * out who answers each of the 65536 ports - is hidden in {@link PortDecoder}, and everything
 * electrical in {@link BusArbiter}.
 */
public final class DecodedPortBus implements PortBus {
  /** What a port nobody drove reads as, which is the machine's to say. */
  public interface FloatingBus {
    byte at(int port);
  }

  private final PortDecoder readers;
  private final PortDecoder writers;
  private final PortDecoder latches;
  private final FloatingBus floatingBus;

  public DecodedPortBus(Backplane backplane, FloatingBus floatingBus) {
    this.readers = new PortDecoder(backplane, PortHandler::isReader);
    this.writers = new PortDecoder(backplane, PortHandler::isWriter);
    this.latches = new PortDecoder(backplane, h -> h.isWriter() && h.ignoresReadWriteLine());
    this.floatingBus = floatingBus;
  }

  public byte read(int port) {
    byte value = BusArbiter.settle(readers.at(port), port, floatingBus);
    for (PortHandler latch : latches.at(port)) {
      latch.write(port, value);
    }
    return value;
  }

  public void write(int port, byte value) {
    for (PortHandler writer : writers.at(port)) {
      writer.write(port, value);
    }
  }
}
