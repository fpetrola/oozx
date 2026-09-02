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

import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.ZxModule;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A board with one IDE channel on it and nothing to page: the Simple 8-bit IDE as it is, and
 * the base of the ones that add memory. The channel is reset with the machine.
 */
public abstract class IdeBoard extends PluggablePeripheral implements ZxModule, IdeInterface {

  protected final IdeChannel channel;
  private final Module module;
  private final int units;
  protected SpectrumMachine on;

  protected IdeBoard(Module module, boolean sixteenBit, int units) {
    super(List.of());
    this.module = module;
    this.units = units;
    channel = new IdeChannel(sixteenBit);
  }

  public IdeChannel channel() {
    return channel;
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
    module.register(this);
  }

  @Override
  public void deactivate() {
    module.unregister(this);
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    channel.reset();
  }

  @Override
  public int units() {
    return units;
  }

  @Override
  public IdeChannel.Drive drive(int unit) {
    return channel.drive(unit);
  }

  @Override
  public void insert(int unit, File image) throws IOException {
    channel.insert(unit, image);
  }

  @Override
  public void eject(int unit) {
    channel.eject(unit);
  }

  @Override
  public boolean isPaged() {
    return false;
  }

  @Override
  public String status() {
    return "";
  }

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
}
