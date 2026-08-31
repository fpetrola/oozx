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

import com.fpetrola.oozx.PeriphDelegate;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.IPeriph;
import com.fpetrola.oozx.speccy.peripherals.SeMemoryPeripheral;
import com.fpetrola.oozx.speccy.peripherals.Spec128MemoryPeripheral;

@Singleton
public class SpecPlus2 extends Spec128 {
  @Inject
  public SpecPlus2(Memory memory, Display display, MachinesPeriph machinesPeriph, PeriphDelegate periph, Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module, Sound sound, UserInterface userInterface) {
    super(memory, display, machinesPeriph, periph, settings, eventManager, cpu, timer, module, sound, userInterface);
  }

  public int reset() {
    return doReset(settings.current.romPlus20, settings.defaults.romPlus20,
        settings.current.romPlus21, settings.defaults.romPlus21);
  }

  public String getName() {
    return "Spectrum Plus 2";
  }
}