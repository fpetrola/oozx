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
package com.fpetrola.oozx.speccy.devices.divide;

import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
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
 * registers sit at 0xa3, 0xa7 ... 0xbf, told apart by bits 2-4 of the port. Ported from Fuse's
 * divide.c.
 */
@Singleton
public class DivIdePeripheral extends DivPeripheral {

  public static final int RAM_PAGES = 4;

  private final IdeChannel channel = new IdeChannel(true);

  @Inject
  public DivIdePeripheral(Memory memory, Module module, Cpu cpu, Settings settings) {
    super(memory, module, cpu, settings, RAM_PAGES);
    ports(controlPort(), new DefaultPortHandler(0x00e3, 0x00a3, true, true) {
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
    return IdeChannel.Register.values()[port >> 2 & 0x07];
  }

  @Override
  public String romName() {
    return settings.current.romDivide;
  }

  @Override
  public boolean writeProtected() {
    return settings.current.divideWp;
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
