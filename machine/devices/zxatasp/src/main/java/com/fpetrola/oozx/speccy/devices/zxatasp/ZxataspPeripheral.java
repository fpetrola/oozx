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
package com.fpetrola.oozx.speccy.devices.zxatasp;

import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.ide.BankedIdePeripheral;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The ZXATASP: 512K of RAM in 32 banks and an IDE channel behind an 8255, at 0x9f, 0x19f, 0x29f
 * and 0x39f. Port A carries the low byte and port B the high one; port C is the register in
 * bits 0-2, the write and read strobes in 3 and 4, the primary select in 5, and with bit 6 up it
 * is the RAM latch instead - bits 0-4 the bank, bit 7 the memory off - where otherwise bit 7
 * selects the secondary channel nothing ever used. Strobes act on their rising edge, and only
 * while the upper half of port C is an output. With the write-protect jumper on, only the odd
 * banks are protected. From Fuse's zxatasp.c.
 */
@Singleton
public class ZxataspPeripheral extends BankedIdePeripheral {

  public static final int BANKS = 32;
  private static final int PORT_C_LOW_INPUT = 0x01;
  private static final int PORT_B_INPUT = 0x02;
  private static final int PORT_C_HIGH_INPUT = 0x08;
  private static final int PORT_A_INPUT = 0x10;
  private static final int SET_MODE = 0x80;
  private static final int IDE_REGISTER = 0x07;
  private static final int RAM_BANK = 0x1f;
  private static final int IDE_WRITE = 0x08;
  private static final int IDE_READ = 0x10;
  private static final int IDE_PRIMARY = 0x20;
  private static final int RAM_LATCH = 0x40;
  private static final int RAM_DISABLE = 0x80;
  private static final int IDE_SECONDARY = 0x80;
  private static final int STROBES = RAM_LATCH | IDE_READ | IDE_WRITE;

  private final Settings settings;
  private final IdeChannel secondary = new IdeChannel(true);
  private int control;
  private int portA, portB, portC;

  @Inject
  public ZxataspPeripheral(Memory memory, Module module, Settings settings) {
    super(memory, module, BANKS, 2);
    this.settings = settings;
    ports(
        new DefaultPortHandler(0x039f, 0x009f, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) portA;
          }

          public void write(int port, byte value) {
            if ((control & PORT_A_INPUT) == 0) portA = value & 0xff;
          }
        },
        new DefaultPortHandler(0x039f, 0x019f, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) portB;
          }

          public void write(int port, byte value) {
            if ((control & PORT_B_INPUT) == 0) portB = value & 0xff;
          }
        },
        new DefaultPortHandler(0x039f, 0x029f, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) portC;
          }

          public void write(int port, byte value) {
            portCWrite(value & 0xff);
          }
        },
        new DefaultPortHandler(0x039f, 0x039f, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) control;
          }

          public void write(int port, byte value) {
            controlWrite(value & 0xff);
          }
        });
  }

  /** Bit 7 set: a mode word. Clear: set or reset one bit of port C. */
  public void controlWrite(int value) {
    if ((value & SET_MODE) != 0) {
      control = value;
      resetPorts();
    } else {
      int bit = 1 << (value >> 1 & 0x07);
      portCWrite((value & 0x01) != 0 ? portC | bit : portC & ~bit);
    }
  }

  private static boolean selected(int c, int channel, int strobe) {
    return (c & (channel | STROBES)) == (channel | strobe);
  }

  public void portCWrite(int value) {
    int old = portC;
    int c = ((control & PORT_C_LOW_INPUT) != 0 ? old : value) & 0x0f
        | ((control & PORT_C_HIGH_INPUT) != 0 ? old : value) & 0xf0;
    portC = c;
    if ((control & PORT_C_HIGH_INPUT) != 0) {
      return;
    }
    IdeChannel.Register register = IdeChannel.Register.values()[c & IDE_REGISTER];
    if (selected(c, IDE_PRIMARY, IDE_READ) && !selected(old, IDE_PRIMARY, IDE_READ)) {
      readIde(channel, register);
    } else if (selected(c, IDE_SECONDARY, IDE_READ) && !selected(old, IDE_SECONDARY, IDE_READ)) {
      readIde(secondary, register);
    } else if (selected(c, IDE_PRIMARY, IDE_WRITE) && !selected(old, IDE_PRIMARY, IDE_WRITE)) {
      writeIde(channel, register);
    } else if (selected(c, IDE_SECONDARY, IDE_WRITE) && !selected(old, IDE_SECONDARY, IDE_WRITE)) {
      writeIde(secondary, register);
    } else if ((c & RAM_LATCH) != 0) {
      select(c & RAM_BANK, (c & RAM_DISABLE) == 0);
    }
  }

  private void readIde(IdeChannel from, IdeChannel.Register register) {
    int low = from.read(register);
    int high = register == IdeChannel.Register.DATA ? from.read(register) : 0xff;
    if ((control & PORT_A_INPUT) != 0) portA = low;
    if ((control & PORT_B_INPUT) != 0) portB = high;
  }

  private void writeIde(IdeChannel to, IdeChannel.Register register) {
    to.write(register, (control & PORT_A_INPUT) != 0 ? 0xff : portA);
    if (register == IdeChannel.Register.DATA) {
      to.write(register, (control & PORT_B_INPUT) != 0 ? 0xff : portB);
    }
  }

  /** Inputs read 0xff until the IDE puts something there; outputs start at zero. */
  private void resetPorts() {
    portA = (control & PORT_A_INPUT) != 0 ? 0xff : 0x00;
    portB = (control & PORT_B_INPUT) != 0 ? 0xff : 0x00;
    portC = ((control & PORT_C_LOW_INPUT) != 0 ? 0x0f : 0x00) | ((control & PORT_C_HIGH_INPUT) != 0 ? 0xf0 : 0x00);
  }

  @Override
  public void machineWasReset(boolean hard) {
    super.machineWasReset(hard);
    control = SET_MODE | PORT_A_INPUT | PORT_B_INPUT | PORT_C_HIGH_INPUT | PORT_C_LOW_INPUT;
    resetPorts();
    secondary.reset();
  }

  @Override
  protected boolean writable(int bank) {
    return !(settings.current.zxataspWp && (bank & 1) != 0);
  }

  @Override
  protected boolean upload() {
    return settings.current.zxataspUpload;
  }

  /** The jumpers changed. */
  public void refresh() {
    if (on != null) {
      on.memoryMap();
    }
  }
}
