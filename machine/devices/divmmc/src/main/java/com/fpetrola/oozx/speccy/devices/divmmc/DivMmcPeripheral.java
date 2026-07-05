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
package com.fpetrola.oozx.speccy.devices.divmmc;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.devices.ide.DivPeripheral;
import com.fpetrola.oozx.speccy.devices.ide.MassStorage;
import com.fpetrola.oozx.speccy.devices.ide.MmcCard;
import com.fpetrola.oozx.speccy.devices.ide.MmcSlot;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;

/**
 * The DivMMC: the same EPROM and automapper as the DivIDE with 128K of RAM in sixteen pages,
 * and where that one has an IDE channel this has a card slot - 0xe7 chooses the card, both
 * lines being active low, and every access to 0xeb clocks a byte each way to it. Its
 * second slot is a note about hardware it never had.
 */
@Singleton
public class DivMmcPeripheral extends DivPeripheral {

  public static final int RAM_PAGES = 16;

  private final MmcSlot slot = new MmcSlot(0x00e7, 0x00eb);

  @Inject
  public DivMmcPeripheral(MemoryBus memory, Cpu cpu, com.fpetrola.oozx.speccy.machine.Roms roms) {
    super(memory, cpu, roms, RAM_PAGES);
    ports(controlPort(), slot.selectPort(), slot.dataPort());
  }



  @Override
  public void machineWasReset(boolean hard) {
    super.machineWasReset(hard);
    slot.card().reset();
  }

  public MmcCard card() {
    return slot.card();
  }

  /** The button on the board, which esxDOS answers with its menu. */
  public void nmi() {
    cpu.nmi();
  }

  @Override
  public int units() {
    return 1;
  }

  @Override
  public MassStorage drive(int unit) {
    return slot.card();
  }

  @Override
  public void insert(int unit, File image) throws IOException {
    slot.card().insert(image);
  }

  @Override
  public void eject(int unit) {
    slot.card().eject();
  }

}
