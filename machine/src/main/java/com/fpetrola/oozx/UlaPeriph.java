/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.modules.Ula;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.oozx.fuse.peripherals.ZxPeripheral;

public class UlaPeriph implements PeriphDelegate {
  private final Ula ula;
  private final ZxClock zxClock;
  private final IPeriph periph;

  public UlaPeriph(Ula ula, ZxClock zxClock, IPeriph periph) {
    this.ula = ula;
    this.zxClock = zxClock;
    this.periph = periph;
  }

  public IPeriph getPeriph() {
    return periph;
  }

  public void writePort(int port, byte b) {
    ula.contendPortEarly(port);
    writePortInternal(port, b);
    ula.contendPortLate(port);
    zxClock.addTstates(1);
  }

  public byte readPort(int port) {
    ula.contendPortEarly(port);
    ula.contendPortLate(port);
    return getPeriph().readPort(port);
  }
}
