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
package com.fpetrola.oozx.speccy.devices.zxcf;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.devices.ide.BankedIdePeripheral;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The ZXCF CompactFlash interface: 1M of RAM in 64 banks and one CompactFlash card on a 16-bit
 * channel. The memory register at 0x10b4 - bit 7 memory off, bit 6 writable, bits 0-5 the bank -
 * reads back as 0xff; the channel is at 0xb4 with the register in bits 8-10.
 */
@Singleton
public class ZxcfPeripheral extends BankedIdePeripheral {

  public static final int BANKS = 64;

  private boolean upload;
  private boolean writeEnabled;
  private int lastMemoryControl;

  @Inject
  public ZxcfPeripheral(MemoryBus memory) {
    super(memory, BANKS, 1);
    ports(
        Wired.at(0x10f4, 0x10b4, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            return BusAnswer.of(0xff);
          }

          public void write(int port, byte value) {
            memoryControlWrite(value & 0xff);
          }
        }),
        Wired.at(0x10f4, 0x00b4, new DefaultPortHandler(true, true) {
          public BusAnswer read(int port) {
            return BusAnswer.of(channel.read(register(port)));
          }

          public void write(int port, byte value) {
            channel.write(register(port), value & 0xff);
          }
        }));
  }

  private static IdeChannel.Register register(int port) {
    return IdeChannel.Register.values()[port >> 8 & 0x07];
  }

  public void memoryControlWrite(int value) {
    lastMemoryControl = value;
    writeEnabled = (value & 0x40) != 0;
    select(value & 0x3f, (value & 0x80) == 0);
  }

  public int lastMemoryControl() {
    return lastMemoryControl;
  }

  @Override
  public void machineWasReset(boolean hard) {
    writeEnabled = false;
    super.machineWasReset(hard);
  }

  @Override
  protected boolean writable(int bank) {
    return writeEnabled;
  }

  @Override
  public boolean upload() {
    return upload;
  }

  /** The jumper changed. */
  public void refresh() {
    select(bank(), isPaged());
  }


  public void setUpload(boolean upload) {
    this.upload = upload;
  }
}
