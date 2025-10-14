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

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;

import java.util.Arrays;

public class MachineStartupModule extends AbstractStartupModule {
  private Machine machine;
  private SpectrumMachine[] spectrumMachines;

  public MachineStartupModule(Machine machine, SpectrumMachine... spectrumMachines) {
    super(MemoryStartupModule.class);
    this.machine = machine;
    this.spectrumMachines = spectrumMachines;
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    Arrays.stream(spectrumMachines).forEach(machine::addMachine);
    return 0;
  }

  public void endFn() {
    machine.end();
  }

}
