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

package com.fpetrola.oozx.fuse.machine;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.modules.Timer;
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.oozx.fuse.peripherals.SeMemoryPeripheral;
import com.fpetrola.oozx.fuse.peripherals.Spec128MemoryPeripheral;

public class SpecPlus2 extends Spec128 {
  public SpecPlus2(Memory memory, Display display, MachinesPeriph machinesPeriph, IPeriph periph, Settings settings, EventManager eventManager, Z80 z80, Timer timer, Module module) {
    super(memory, display, machinesPeriph, periph, settings, eventManager, z80, timer, module);
    init();
  }

  public void init() {
    periph.register(new Spec128MemoryPeripheral(this));
    periph.register(new SeMemoryPeripheral(this));
  }

  public int reset() {
    return doReset(settings.current.romPlus20, settings.defaults.romPlus20,
        settings.current.romPlus21, settings.defaults.romPlus21);
  }

  public String getName() {
    return "Spectrum Plus 2";
  }
}