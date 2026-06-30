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
package com.fpetrola.oozx.speccy.devices.divide;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.devices.ide.DivPeripheral;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;

/**
 * The DivIDE: the shared EPROM, 32K of RAM in four pages, and one 16-bit IDE channel whose eight
 * registers sit at 0xa3, 0xa7 ... 0xbf, told apart by bits 2-4 of the port.
 */
@Singleton
public class DivIdePeripheral extends DivPeripheral {

  public static final int RAM_PAGES = 4;

  private final IdeChannel channel = new IdeChannel(true);

  @Inject
  public DivIdePeripheral(MemoryBus memory, Cpu cpu, com.fpetrola.oozx.speccy.machine.Roms roms) {
    super(memory, cpu, roms, RAM_PAGES);
    ports(controlPort(), Wired.at(0x00e3, 0x00a3, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(channel.read(register(port)));
      }

      public void write(int port, byte value) {
        channel.write(register(port), value & 0xff);
      }
    }));
  }

  private static IdeChannel.Register register(int port) {
    return IdeChannel.Register.values()[port >> 2 & 0x07];
  }



  @Override
  public void machineWasReset(boolean hard) {
    super.machineWasReset(hard);
    channel.reset();
  }

  public IdeChannel channel() {
    return channel;
  }

  /** The button on the board, which the firmware answers with its menu. */
  public void nmi() {
    cpu.nmi();
  }

  @Override
  public int units() {
    return 2;
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

}
