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
