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

package com.fpetrola.oozx.fuse.startup;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.fuse.machine.Spec128;
import com.fpetrola.oozx.fuse.machine.SpecPlus3;
import com.fpetrola.oozx.fuse.peripherals.Periph;

public class MachinesPeriphStartupModule extends AbstractStartupModule {
  private Machine machine;
  private Spec128 spec128;
  private SpecPlus3 specPlus3;
  private Periph periph;

  public MachinesPeriphStartupModule(Machine machine, Spec128 spec128, SpecPlus3 specPlus3, Periph periph) {
    this.machine = machine;
    this.spec128 = spec128;
    this.specPlus3 = specPlus3;
    this.periph = periph;
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    periph.register(new Spec128MemoryPeripheral(spec128));
    periph.register(new SpecPlus3MemoryPeripheral(spec128, specPlus3));
    periph.register(new Upd765Peripheral(specPlus3));
    periph.register(new SeMemoryPeripheral(machine));
    return 0;
  }

  public void endFn() {

  }

}
