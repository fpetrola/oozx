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

import com.fpetrola.oozx.DefaultMachine;
import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.Spectrum;
import com.google.inject.Inject;

import java.util.Set;

public class MachineStartupModule extends AbstractStartupModule {
  private final Machine machine;
  private final Set<Spectrum> spectrumMachines;
  private final Spectrum defaultMachine;

  @Inject
  public MachineStartupModule(Machine machine, Set<Spectrum> spectrumMachines, @DefaultMachine Spectrum defaultMachine) {
    super();
    this.machine = machine;
    this.spectrumMachines = spectrumMachines;
    this.defaultMachine = defaultMachine;
  }


  public void init() {
    spectrumMachines.forEach(machine::addMachine);
    machine.setDefaultMachine(defaultMachine);
  }

  public void endFn() {
    machine.end();
  }

}
