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
package com.fpetrola.oozx.speccy.devices.ide;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A board that is a card slot and nothing else: the ZXMMC. The DivMMC has the same slot behind
 * its own memory, so the slot is {@link MmcSlot} and this is only what holds one.
 */
public abstract class MmcBoard extends PluggablePeripheral implements IdeInterface {

  protected final MmcSlot slot;
  protected SpectrumMachine on;

  protected MmcBoard(int selectPort, int dataPort) {
    super(List.of());
    slot = new MmcSlot(selectPort, dataPort);
    ports(slot.selectPort(), slot.dataPort());
  }

  public MmcCard card() {
    return slot.card();
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  /** Nothing but ports: it goes on any edge connector. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return true;
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
  }

  @Override
  public void deactivate() {
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    slot.card().reset();
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

  @Override
  public boolean isPaged() {
    return false;
  }

  @Override
  public String status() {
    return "";
  }

}
