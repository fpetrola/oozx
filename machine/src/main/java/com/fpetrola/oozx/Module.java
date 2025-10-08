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

import java.util.*;

public class Module {

  private static LinkedList<ZXModule> registeredModules = new LinkedList<>();

  public static int register(ModuleInfo module) {
    registeredModules.add(new ZXModule(module));
    return 0;
  }

  public static void moduleEnd() {
    if (registeredModules != null) {
      registeredModules.clear();
      registeredModules = null;
    }
  }

  public static void reset(int hardReset) {
    if (registeredModules == null) return;
    for (ZXModule module : registeredModules) {
      module.reset(hardReset);
    }
  }

  public static void romcs() {
    if (registeredModules == null) return;
    for (ZXModule module : registeredModules) {
      module.romcs();
    }
  }

  public static void moduleSnapshotEnabled(Libspectrum.Snap snap) {
    if (registeredModules == null) return;
    for (ZXModule module : registeredModules) {
        module.snapshotEnabled(snap);
    }
  }

  public static void moduleSnapshotFrom(Libspectrum.Snap snap) {
    if (registeredModules == null) return;
    for (ZXModule module : registeredModules) {
        module.snapshotFrom(snap);
    }
  }

  public static void moduleSnapshotTo(Libspectrum.Snap snap) {
    if (registeredModules == null) return;
    for (ZXModule module : registeredModules) {
        module.snapshotTo(snap);
    }
  }

  public static void end() {

  }
}