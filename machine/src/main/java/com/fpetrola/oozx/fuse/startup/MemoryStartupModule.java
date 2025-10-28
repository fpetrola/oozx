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
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.machine.Spec128;
import com.fpetrola.oozx.fuse.machine.SpecPlus3;
import com.fpetrola.oozx.fuse.modules.MemoryModuleInfo;

public class MemoryStartupModule extends AbstractStartupModule {
  private final Memory memory;
  private Machine machine;
  private Spec128 spec128;
  private SpecPlus3 specPlus3;
  private Module module;

  public MemoryStartupModule(Memory memory, Machine machine, Spec128 spec128, SpecPlus3 specPlus3, Module module) {
    this.memory = memory;
    this.machine = machine;
    this.spec128 = spec128;
    this.specPlus3 = specPlus3;
    this.module = module;
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    int init = memory.init(this);
    module.register(new MemoryModuleInfo(memory, machine, spec128, specPlus3));
    return init;
  }

  public void endFn() {
    memory.end();
  }

}
