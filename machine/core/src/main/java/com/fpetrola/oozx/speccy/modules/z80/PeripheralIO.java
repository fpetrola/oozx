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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.PeripheralBusDelegate;
import com.fpetrola.z80.cpu.IO;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The processor's ports, wired to the peripheral bus.
 * <p>
 * This is what the Z80 used to build for itself inside createIO, which left no way to put
 * anything else on the ports. It is a class rather than a lambda so that it can be replaced —
 * an RZX recording plays back what each IN returned instead of asking the hardware — and it is
 * left open so it can be wrapped: recording is the same thing observed rather than substituted.
 * <p>
 * It holds no state of its own, which is why moving from one built per init path to a single
 * injected instance changes nothing.
 */
@Singleton
public class PeripheralIO implements IO {

  private final PeripheralBusDelegate peripherals;

  @Inject
  public PeripheralIO(PeripheralBusDelegate peripherals) {
    this.peripherals = peripherals;
  }

  @Override
  public int in(int port) {
    return peripherals.readPort(port);
  }

  @Override
  public void out(int port, int value) {
    peripherals.writePort(port, (byte) value);
  }
}
