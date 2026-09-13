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


package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/** The second paging port of the machine that needs one, and of nothing else. */
@Singleton
public class Pentagon1024MemoryPeripheral extends AbstractPeripheral {
  private Pentagon1024 machine;

  @Inject
  public Pentagon1024MemoryPeripheral() {
    super(List.of());
    ports(Wired.at(0xf008, 0xe000, new DefaultPortHandler(false, true) {
      @Override
      public void write(int port, byte value) {
        if (machine != null) machine.secondPortWrite(value);
      }
    }));
  }

  @Override
  public void activate(SpectrumMachine on) {
    machine = on instanceof Pentagon1024 one ? one : null;
  }

  @Override
  public void deactivate() {
    machine = null;
  }

  @Override
  public boolean fitsOn(SpectrumMachine on) {
    return on instanceof Pentagon1024;
  }
}
