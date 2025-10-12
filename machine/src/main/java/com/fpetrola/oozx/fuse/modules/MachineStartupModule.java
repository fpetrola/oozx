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

package com.fpetrola.oozx.fuse.modules;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.Spec128;
import com.fpetrola.oozx.Spec48;
import com.fpetrola.oozx.SpecPlus3;
import com.fpetrola.oozx.fuse.AbstractStartupModule;

public class MachineStartupModule extends AbstractStartupModule {

  private Machine machine;
  private Spec48 spec48;
  private Spec128 spec128;

  public MachineStartupModule(Machine machine, Spec48 spec48, Spec128 spec128) {
    super(MemoryStartupModule.class);
    this.machine = machine;
    this.spec48 = spec48;
    this.spec128 = spec128;
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    int error;

    error = machine.addMachine(spec48::init);
    if (error != 0) return error;
    error = machine.addMachine(spec128::init);
    if (error != 0) return error;
    error = machine.addMachine(SpecPlus3::init);
    if (error != 0) return error;

    return 0;
  }

  public void endFn() {
    machine.end();
  }

}
