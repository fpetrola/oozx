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

package com.fpetrola.oozx.speccy.startup;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.peripherals.*;

public class MachinesPeriphStartupModule extends AbstractStartupModule {
  private Machine machine;
  private Spec128 spec128;
  private SpecPlus3 specPlus3;
  private IPeriph periph;
  private Sound sound;
  private SpectrumZ80Clock clock;

  public MachinesPeriphStartupModule(Machine machine, Spec128 spec128, SpecPlus3 specPlus3,
                                     IPeriph periph, Sound sound, SpectrumZ80Clock clock) {
    this.machine = machine;
    this.spec128 = spec128;
    this.specPlus3 = specPlus3;
    this.periph = periph;
    this.sound = sound;
    this.clock = clock;
  }


  public void init() {
    periph.register(new Spec128MemoryPeripheral(spec128));
    periph.register(new SpecPlus3MemoryPeripheral(specPlus3));
    periph.register(new Upd765Peripheral(specPlus3));
    periph.register(new SeMemoryPeripheral(spec128));
    // Declared present for every machine that has one, and never actually there.
    periph.register(new AyPeripheral(sound, clock));
    periph.register(new AyPlus3Peripheral(sound, clock));
  }

  public void endFn() {

  }

}
