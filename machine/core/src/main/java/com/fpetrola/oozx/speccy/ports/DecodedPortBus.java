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
