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

package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.machine.FloppyDrive;

class FdcPortHandler extends DefaultPortHandler {
  private final Supplier<FloppyDrive> machine;

  public FdcPortHandler(int mask, int value, Supplier<FloppyDrive> machine) {
    super(mask, value, true, true);
    this.machine = machine;
  }

  public byte read(int port, byte[] attached) {
    return machine.get().fdcRead(port, attached);
  }

  public void write(int port, byte value) {
    machine.get().fdcWrite(port, value);
  }
}
