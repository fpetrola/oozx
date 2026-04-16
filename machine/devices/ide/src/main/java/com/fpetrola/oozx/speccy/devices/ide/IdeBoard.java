/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fpetrola.oozx.speccy.devices.ide;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * A board with one IDE channel on it and nothing to page: the Simple 8-bit IDE as it is, and
 * the base of the ones that add memory. The channel is reset with the machine.
 */
public abstract class IdeBoard extends PluggablePeripheral implements IdeInterface {

  protected final IdeChannel channel;
  private final int units;
  protected SpectrumMachine on;

  protected IdeBoard(boolean sixteenBit, int units) {
    super(List.of());
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
  }

  @Override
  public void deactivate() {
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

}
