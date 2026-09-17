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

package com.fpetrola.oozx.speccy.devices.scld;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/** The display register of a Timex machine, wired to port 0xff and to nothing else. */
@Singleton
public class ScldPeripheral extends AbstractPeripheral {
  private final ScldPortHandler port;

  @Inject
  public ScldPeripheral(ScldPortHandler port) {
    super(List.of(Wired.at(0x00ff, 0x00ff, port)));
    this.port = port;
  }

  /** The pair its register names, which is what a wide picture is drawn in. */
  public byte pairOfColours() {
    return port.pairOfColours();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    port.reset();
  }

  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return machine.onBoard().contains(ScldPeripheral.class);
  }
}
