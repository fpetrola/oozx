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

import com.fpetrola.oozx.Libspectrum;

public class JoystickModuleInfo implements ZXModuleInfo {
  private Joystick joystick;

  public JoystickModuleInfo(Joystick joystick) {
    this.joystick = joystick;
  }

  public void snapshotEnabled(Libspectrum.Snap snap) {
    joystick.enabledSnapshot(snap); // snapshot_enabled
  }

  public void snapshotTo(Libspectrum.Snap snap) {
    joystick.toSnapshot(snap); // snapshot_to
  }
}
