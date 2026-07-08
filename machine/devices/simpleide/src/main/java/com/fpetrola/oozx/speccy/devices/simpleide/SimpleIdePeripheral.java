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
package com.fpetrola.oozx.speccy.devices.simpleide;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.devices.ide.IdeBoard;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The Simple 8-bit IDE interface: one channel on the low byte of the bus and nothing else,
 * answering any port with bit 4 low, the register in bits 8, 12 and 13.
 */
@Singleton
public class SimpleIdePeripheral extends IdeBoard {

  @Inject
  public SimpleIdePeripheral() {
    super(false, 2);
    ports(Wired.at(0x0010, 0x0000, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(channel.read(register(port)));
      }

      public void write(int port, byte value) {
        channel.write(register(port), value & 0xff);
      }
    }));
  }

  private static IdeChannel.Register register(int port) {
    return IdeChannel.Register.values()[port >> 8 & 0x01 | port >> 11 & 0x06];
  }
}
