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

public class ZXModule {
  private final ModuleInfo module;

  public ZXModule(ModuleInfo module) {
    this.module = module;
  }

  public void reset(int hardReset) {
    if (module.reset != null)
      module.reset.apply(hardReset);
  }

  public void romcs() {
    if (module.romcs != null)
      module.romcs.apply();
  }

  public void snapshotEnabled(Libspectrum.Snap snap) {
    if (module.snapshotEnabled != null)
      module.snapshotEnabled.apply(snap);
  }

  public void snapshotFrom(Libspectrum.Snap snap) {
    if (module.snapshotFrom != null)
      module.snapshotFrom.accept(snap);
  }

  public void snapshotTo(Libspectrum.Snap snap) {
    if (module.snapshotTo != null)
      module.snapshotTo.accept(snap);
  }
}
