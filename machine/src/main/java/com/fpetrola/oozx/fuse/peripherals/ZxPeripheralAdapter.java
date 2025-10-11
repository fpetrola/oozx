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

package com.fpetrola.oozx.fuse.peripherals;

import com.fpetrola.oozx.ZxPeripheral;

public class ZxPeripheralAdapter implements ZxPeripheral {
  private final Periph.Type type;
  private final Peripheral peripheral;

  public ZxPeripheralAdapter(Periph.Type type, Peripheral peripheral) {
    this.type = type;
    this.peripheral = peripheral;
  }

  @Override
  public Periph.Type getType() {
    return type;
  }

  @Override
  public boolean canActivate() {
    return peripheral.activate != null;
  }

  @Override
  public void activate() {
    peripheral.activate.apply();
  }

  @Override
  public PortHandler[] getPorts() {
    return peripheral.ports.toArray(new PortHandler[0]);
  }

  @Override
  public boolean hasOption() {
    return peripheral.option != null;
  }

  @Override
  public boolean[] getOption() {
    return peripheral.option;
  }

  @Override
  public boolean hasHardReset() {
    return peripheral.hardReset;
  }
}
