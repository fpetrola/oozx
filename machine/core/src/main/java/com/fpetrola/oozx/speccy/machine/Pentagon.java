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

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.PeriphDelegate;
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.modules.Display;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Set;

import static com.fpetrola.oozx.MachineCapability.AY;
import static com.fpetrola.oozx.MachineCapability.MEMORY_128;
import com.fpetrola.emulation.helpers.machine.MachineTypes;

/**
 * A 1991-era Pentagon 128, the Russian clone with an AY and TR-DOS built in.
 * <p>
 * It pages like a 128 and is timed like nothing else here: 320 lines of 224 clocks at 3.584MHz,
 * and not one address or port is contended. That last part is why Pentagon software runs fast and
 * why timing-exact 48K demos break on it.
 * <p>
 * Built without its disk: the Beta 128 needs a WD1793 and a way to read a disk image, neither of
 * which exists yet. So this machine does not claim TRDOS_DISK - a capability that says yes while
 * nothing answers is how the +3 ended up with a drive nobody could switch off.
 */
@Singleton
public class Pentagon extends Spec128 {

  @Inject
  public Pentagon(Memory memory, Display display, MachinesPeriph machinesPeriph, PeriphDelegate periph,
                  Settings settings, EventManager eventManager, Cpu cpu, Timer timer, Module module,
                  Sound sound, UserInterface userInterface) {
    super(memory, display, machinesPeriph, periph, settings, eventManager, cpu, timer, module, sound,
        userInterface);
  }


  @Override
  public Set<MachineCapability> getCapabilities() {
    return Set.of(AY, MEMORY_128);
  }

  public String shortName() {
    return "Pentagon";
  }

  /** No snapshot format here names a Pentagon, so it is never chosen by one. */
  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public int reset() {
    return doReset(settings.current.romPentagon0, settings.defaults.romPentagon0,
        settings.current.romPentagon1, settings.defaults.romPentagon1);
  }

  @Override
  protected boolean contendsMemory() {
    return false;
  }

  @Override
  protected void installPeripherals() {
    machinesPeriph.machinesPeriphPentagon();
  }

  @Override
  public int contendDelay(long time) {
    return contendDelayNone(time);
  }

  @Override
  public int contendDelayNoMreq(long time) {
    return contendDelayNone(time);
  }

  @Override
  public int unattachedPort(int port) {
    return spectrumUnattachedPortNone();
  }

  @Override
  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3584000, 1750000, TimingsHandler.PENTAGON);
  }

  @Override
  public String getName() {
    return "Pentagon";
  }
}
