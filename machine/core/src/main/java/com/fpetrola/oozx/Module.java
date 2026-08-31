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

package com.fpetrola.oozx;

import com.fpetrola.oozx.speccy.modules.RomcsDevice;
import com.fpetrola.oozx.speccy.modules.ZxModule;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * The modules that want to hear about a machine reset, and those that can page a ROM over it.
 * <p>
 * It exists because Machine has to reset the Z80 and cannot hold one: that reference is the
 * construction cycle this refactor began by breaking. A module registers itself and is called
 * back, which is an observer and not an accident.
 * <p>
 * Modules used to register a five-method adapter of themselves rather than themselves - a
 * translation of the struct of function pointers Fuse keeps, where three of the five fields were
 * for a snapshot path this codebase never reaches, since nothing here ever builds a
 * Libspectrum.Snap. The objects register themselves now, and the two lists say what they can do.
 */
@Singleton
public class Module {

  private final List<ZxModule> modules = new ArrayList<>();
  /** Sorted at registration, so paging a ROM costs nothing on a machine with no such device. */
  private final List<RomcsDevice> romcsDevices = new ArrayList<>();

  public void register(ZxModule module) {
    modules.add(module);
    if (module instanceof RomcsDevice romcs) {
      romcsDevices.add(romcs);
    }
  }

  public void moduleEnd() {
    modules.clear();
    romcsDevices.clear();
  }

  public void machineWasReset(boolean hard) {
    for (ZxModule module : modules) {
      module.machineWasReset(hard);
    }
  }

  public void romcs() {
    for (RomcsDevice device : romcsDevices) {
      device.mapRom();
    }
  }
}
