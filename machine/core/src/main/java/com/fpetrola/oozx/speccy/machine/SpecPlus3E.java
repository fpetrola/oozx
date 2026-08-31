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
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;

@Singleton
public class SpecPlus3E extends SpecPlus3 {
  @Inject
  public SpecPlus3E(Memory memory, Display display, MachinesPeriph machinesPeriph, PeriphDelegate periph, Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module, Fdd fdd1, UPDFdc uPDFdc1, Sound sound, UserInterface userInterface) {
    super(memory, display, machinesPeriph, periph, settings, fdd1, uPDFdc1, eventManager, cpu, timer, module, sound, userInterface);
    init();
  }

  public int reset() {
    doReset(settings.current.romPlus3e0, settings.defaults.romPlus3e0,
        settings.current.romPlus3e1, settings.defaults.romPlus3e1,
        settings.current.romPlus3e2, settings.defaults.romPlus3e2,
        settings.current.romPlus3e3, settings.defaults.romPlus3e3);

    resetStep2();

    return 0;
  }

  public int unattachedPort(int port) {
    return unattachedPortAmstrad(port);
  }

  public String getName() {
    return "Amstrad Spectrum +3e";
  }
}