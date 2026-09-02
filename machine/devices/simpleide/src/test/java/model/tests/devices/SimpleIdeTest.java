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
package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.devices.simpleide.SimpleIdePeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleIdeTest {

  /** Bit 8 of the port is bit 0 of the register, bits 12-13 are bits 1-2; bit 4 must be low. */
  private static int port(IdeChannel.Register register) {
    int r = register.ordinal();
    return 0x00ef | (r & 0x01) << 8 | (r & 0x06) << 11;
  }

  /** Only the low byte of each word reaches an 8-bit bus, so the mark goes on the even bytes. */
  private static File aDisk() throws IOException {
    File file = File.createTempFile("simpleide", ".hdf");
    file.deleteOnExit();
    IdeChannel.createHdf(file, 16);
    try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
      out.seek(0x216 + 5L * IdeChannel.SECTOR);
      out.write(new byte[] {'S', 0, 'I', 0, 'M', 0, 'P', 0, 'L', 0, 'E', 0});
    }
    return file;
  }

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    speccy.machine.select(speccy.spec48);
    return speccy;
  }

  private int in(Speccy speccy, IdeChannel.Register register) {
    return speccy.peripherals.readPort(port(register)) & 0xff;
  }

  private void out(Speccy speccy, IdeChannel.Register register, int value) {
    speccy.peripherals.writePort(port(register), (byte) value);
  }

  @Test
  void aSectorComesThroughTheLowByteOfTheBus() throws IOException {
    Speccy speccy = speccy();
    SimpleIdePeripheral ide = (SimpleIdePeripheral) speccy.peripherals.find(SimpleIdePeripheral.class);
    ide.plugIn(true);
    speccy.peripherals.update();
    speccy.machine.reset(true);
    ide.insert(IdeChannel.MASTER, aDisk());

    out(speccy, IdeChannel.Register.HEAD_DRIVE, 0xe0);
    out(speccy, IdeChannel.Register.SECTOR_COUNT, 1);
    out(speccy, IdeChannel.Register.SECTOR, 5);
    out(speccy, IdeChannel.Register.CYLINDER_LOW, 0);
    out(speccy, IdeChannel.Register.CYLINDER_HIGH, 0);
    out(speccy, IdeChannel.Register.COMMAND_STATUS, 0x20);
    assertEquals(IdeChannel.STATUS_DRQ, in(speccy, IdeChannel.Register.COMMAND_STATUS) & IdeChannel.STATUS_DRQ);
    byte[] sector = new byte[IdeChannel.SECTOR / 2];
    for (int i = 0; i < sector.length; i++) {
      sector[i] = (byte) in(speccy, IdeChannel.Register.DATA);
    }
    assertEquals("SIMPLE", new String(sector, 0, 6));
    assertEquals(0, in(speccy, IdeChannel.Register.COMMAND_STATUS) & IdeChannel.STATUS_DRQ, "256 bytes are the sector on this bus");
  }

  @Test
  void itFitsEveryMachineHavingNoMemoryOfItsOwn() {
    Speccy speccy = speccy();
    SimpleIdePeripheral ide = (SimpleIdePeripheral) speccy.peripherals.find(SimpleIdePeripheral.class);
    assertTrue(ide.fitsOn(speccy.spec48));
    assertTrue(ide.fitsOn(speccy.specPlus3));
    assertEquals(2, ide.units());
  }
}
