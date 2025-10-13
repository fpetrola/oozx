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
import com.fpetrola.oozx.fuse.machine.Spec128;
import com.fpetrola.oozx.fuse.machine.SpecPlus3;

public class MemoryModuleInfo implements ZXModuleInfo {
  private Memory memory;
  private Machine machine;
  private RAMHolder ramHolder;
  private Spec128 spec128;
  private SpecPlus3 specPlus3;

  public MemoryModuleInfo(Memory memory, Machine machine, RAMHolder ramHolder, Spec128 spec128, SpecPlus3 specPlus3) {
    this.memory = memory;
    this.machine = machine;
    this.ramHolder = ramHolder;
    this.spec128 = spec128;
    this.specPlus3 = specPlus3;
  }

  public void snapshotFrom(Libspectrum.Snap snap) {
    // snapshotFrom
    int capabilities = machine.current.getCapabilities();

    if ((capabilities & Libspectrum.MachineCapability._128_MEMORY) != 0) {
      spec128.memoryPortWrite(0x7ffd, Libspectrum.snapOut128Memoryport(snap));
    }
    if ((capabilities & Libspectrum.MachineCapability.PLUS3_MEMORY) != 0 ||
        (capabilities & Libspectrum.MachineCapability.SCORP_MEMORY) != 0) {
      specPlus3.memoryPort2WriteInternal(0x1ffd, Libspectrum.snapOutPlus3Memoryport(snap));
    }

    for (int i = 0; i < 64; i++) {
      byte[] page = Libspectrum.snapPages(snap, i);
      if (page != null) {
        System.arraycopy(page, 0, ramHolder.getRAM()[i], 0, 0x4000);
      }
    }

    if (Libspectrum.snapCustomRom(snap)) {
      for (int i = 0; i < Libspectrum.snapCustomRomPages(snap) && i < 4; i++) {
        byte[] rom = Libspectrum.snapRoms(snap, i);
        if (rom != null) {
          machine.loadRomBankFromBuffer(memory.mapRom, i, rom, Libspectrum.snapRomLength(snap, i), true);
        }
      }
    }
  }

  public void snapshotTo(Libspectrum.Snap snap) {
    // snapshotTo
    Libspectrum.snapSetOut128Memoryport(snap, machine.current.getRamInfo().lastByte);
    Libspectrum.snapSetOutPlus3Memoryport(snap, machine.current.getRamInfo().lastByte2);

    for (int i = 0; i < 64; i++) {
      if (ramHolder.getRAM()[i] != null) {
        byte[] buffer = new byte[0x4000];
        System.arraycopy(ramHolder.getRAM()[i], 0, buffer, 0, 0x4000);
        Libspectrum.snapSetPages(snap, i, buffer);
      }
    }

    memory.romToSnapshot(snap);
  }
}
