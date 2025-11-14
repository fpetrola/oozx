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

import com.fpetrola.oozx.fuse.modules.ZXModuleInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Module {
  private List<ZXModuleInfo> registeredModules = new ArrayList<>();

  public void register(ZXModuleInfo e) {
    registeredModules.add(e);
  }

  public void moduleEnd() {
    if (registeredModules != null) {
      registeredModules.clear();
      registeredModules = null;
    }
  }

  public void reset(int hardReset) {
    if (registeredModules == null) return;
    for (ZXModuleInfo module : registeredModules) {
      module.reset(hardReset);
    }
  }

  public void romcs() {
    if (registeredModules == null) return;
    for (ZXModuleInfo module : registeredModules) {
      module.romcs();
    }
  }

  public void moduleSnapshotEnabled(Libspectrum.Snap snap) {
    if (registeredModules == null) return;
    for (ZXModuleInfo module : registeredModules) {
      module.snapshotEnabled(snap);
    }
  }

  public void moduleSnapshotFrom(Libspectrum.Snap snap) {
    if (registeredModules == null) return;
    for (ZXModuleInfo module : registeredModules) {
      module.snapshotFrom(snap);
    }
  }

  public void moduleSnapshotTo(Libspectrum.Snap snap) {
    if (registeredModules == null) return;
    for (ZXModuleInfo module : registeredModules) {
      module.snapshotTo(snap);
    }
  }

  public void end() {

  }
}