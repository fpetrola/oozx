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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.AbstractStartupModule;

import java.util.ArrayList;

public class MemoryStartupModule extends AbstractStartupModule {
  public MemoryStartupModule() {
    super(SetUidStartupModule.class);
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    Memory.memorySources = new ArrayList<>();

    Memory.sourceRom = Memory.sourceRegister("ROM");
    Memory.sourceRam = Memory.sourceRegister("RAM");
    Memory.sourceDock = Memory.sourceRegister("Timex Dock");
    Memory.sourceExrom = Memory.sourceRegister("Timex EXROM");
    Memory.sourceAny = Memory.sourceRegister("Absolute address");
    Memory.sourceNone = Memory.sourceRegister("None");

    Memory.pool = new ArrayList<>();

    for (int i = 0; i < Memory.SPECTRUM_ROM_PAGES; i++) {
      for (int j = 0; j < Memory.PAGES_IN_16K; j++) {
        MemoryPage page = Memory.mapRom[i * Memory.PAGES_IN_16K + j] = new MemoryPage();
        page.writable = false;
        page.contended = false;
        page.source = Memory.sourceRom;
      }
    }

    for (int i = 0; i < Memory.SPECTRUM_RAM_PAGES; i++) {
      for (int j = 0; j < Memory.PAGES_IN_16K; j++) {
        MemoryPage page = Memory.mapRam[i * Memory.PAGES_IN_16K + j] = new MemoryPage();
        page.setPage(Spectrum.RAM, i, j * Memory.PAGE_SIZE);
        page.pageNum = i;
        page.offset = j * Memory.PAGE_SIZE;
        page.writable = true;
        page.source = Memory.sourceRam;
      }
    }

    Module.register(new MemoryModuleInfo());
    return 0;
  }

  public void endFn() {
    if (Memory.pool != null) {
      for (Memory.MemoryPoolEntry entry : Memory.pool) {
        // Java garbage collector handles memory deallocation
      }
      Memory.pool.clear();
      Memory.pool = null;
    }

    if (Memory.memorySources != null) {
      Memory.memorySources.clear();
      Memory.memorySources = null;
    }
  }

}
