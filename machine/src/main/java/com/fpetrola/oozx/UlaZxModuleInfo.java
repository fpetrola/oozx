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

import com.fpetrola.z80.memory.Memory;

class UlaZxModuleInfo implements ZXModuleInfo {
  private Ula ula;

  UlaZxModuleInfo(Ula ula) {
    this.ula = ula;
  }

  public void reset(int hardReset) {

  }

  public void romcs() {

  }

  public void snapshotEnabled(Libspectrum.Snap snap) {

  }

  public void snapshotFrom(Libspectrum.Snap snap) {
    ula.write(0x00fe, Libspectrum.snapOutUla(snap));
    Spectrum.tstates = Libspectrum.snapTstates(snap);
    Settings.current.issue2 = Libspectrum.snapIssue2(snap);
  }

  public void snapshotTo(Libspectrum.Snap snap) {
    Libspectrum.snapSetOutUla(snap, ula.lastByte);
    Libspectrum.snapSetTstates(snap, Spectrum.tstates);
    Libspectrum.snapSetIssue2(snap, Settings.current.issue2);
  }
}
