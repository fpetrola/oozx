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
