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

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.AbstractStartupModule;

import java.util.ArrayList;

public class MemoryStartupModule extends AbstractStartupModule {
  private final Memory memory;
  private RAMHolder ramHolder;
  private Machine machine;
  private Spec128 spec128;
  private SpecPlus3 specPlus3;

  public MemoryStartupModule(Memory memory, RAMHolder ramHolder, Machine machine, Spec128 spec128, SpecPlus3 specPlus3) {
    this.memory = memory;
    this.ramHolder = ramHolder;
    this.machine = machine;
    this.spec128 = spec128;
    this.specPlus3 = specPlus3;
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    memory.memorySources = new ArrayList<>();

    memory.sourceRom = memory.sourceRegister("ROM");
    memory.sourceRam = memory.sourceRegister("RAM");
    memory.sourceDock = memory.sourceRegister("Timex Dock");
    memory.sourceExrom = memory.sourceRegister("Timex EXROM");
    memory.sourceAny = memory.sourceRegister("Absolute address");
    memory.sourceNone = memory.sourceRegister("None");

    memory.pool = new ArrayList<>();

    for (int i = 0; i < memory.SPECTRUM_ROM_PAGES; i++) {
      for (int j = 0; j < memory.PAGES_IN_16K; j++) {
        MemoryPage page = memory.mapRom[i * memory.PAGES_IN_16K + j] = new MemoryPage();
        page.writable = false;
        page.contended = false;
        page.source = memory.sourceRom;
      }
    }

    for (int i = 0; i < memory.SPECTRUM_RAM_PAGES; i++) {
      for (int j = 0; j < memory.PAGES_IN_16K; j++) {
        MemoryPage page = memory.mapRam[i * memory.PAGES_IN_16K + j] = new MemoryPage();
        page.setPage(ramHolder.getRAM(), i, j * memory.PAGE_SIZE);
        page.pageNum = i;
        page.offset = j * memory.PAGE_SIZE;
        page.writable = true;
        page.source = memory.sourceRam;
      }
    }

    Module.register(new MemoryModuleInfo(memory, machine, ramHolder, spec128, specPlus3));
    return 0;
  }

  public void endFn() {
    if (memory.pool != null) {
      for (Memory.MemoryPoolEntry entry : memory.pool) {
        // Java garbage collector handles memory deallocation
      }
      memory.pool.clear();
      memory.pool = null;
    }

    if (memory.memorySources != null) {
      memory.memorySources.clear();
      memory.memorySources = null;
    }
  }

}
