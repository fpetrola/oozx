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

import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.ide.BankedIdePeripheral;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The ZXCF CompactFlash interface: 1M of RAM in 64 banks and one CompactFlash card on a 16-bit
 * channel. The memory register at 0x10b4 - bit 7 memory off, bit 6 writable, bits 0-5 the bank -
 * reads back as 0xff; the channel is at 0xb4 with the register in bits 8-10. From Fuse's zxcf.c.
 */
@Singleton
public class ZxcfPeripheral extends BankedIdePeripheral {

  public static final int BANKS = 64;

  private final Settings settings;
  private boolean writeEnabled;
  private int lastMemoryControl;

  @Inject
  public ZxcfPeripheral(Memory memory, Module module, Settings settings) {
    super(memory, module, BANKS, 1);
    this.settings = settings;
    ports(
        new DefaultPortHandler(0x10f4, 0x10b4, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) 0xff;
          }

          public void write(int port, byte value) {
            memoryControlWrite(value & 0xff);
          }
        },
        new DefaultPortHandler(0x10f4, 0x00b4, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) channel.read(register(port));
          }

          public void write(int port, byte value) {
            channel.write(register(port), value & 0xff);
          }
        });
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
  protected boolean upload() {
    return settings.current.zxcfUpload;
  }

  /** The jumper changed. */
  public void refresh() {
    if (on != null) {
      on.memoryMap();
    }
  }
}
