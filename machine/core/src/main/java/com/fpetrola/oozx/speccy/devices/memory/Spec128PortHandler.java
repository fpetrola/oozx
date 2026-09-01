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

package com.fpetrola.oozx.speccy.devices.memory;

import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import com.fpetrola.oozx.speccy.machine.Spec128;
import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.machine.Paging128;

class Spec128PortHandler extends DefaultPortHandler {
  private final Supplier<? extends Paging128> machine;

  public Spec128PortHandler(int mask, int value, Supplier<? extends Paging128> machine) {
    super(mask, value, false, true);
    this.machine = machine;
  }

  public void write(int port, byte value) {
    machine.get().memoryPortWrite(port, value);
  }

  /**
   * This pager listens loosely enough that the video data the ULA leaves floating reaches it, so
   * reading the port pages the machine - which games use. A machine that drives its bus, the +3
   * family and the Pentagon, leaves nothing there to hear.
   */
  public boolean listensToBusReads() {
    return machine.get().hasFloatingBus();
  }

  public void busRead(int port, byte onTheBus) {
    write(port, onTheBus);
  }
}
