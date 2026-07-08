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
package model.tests.devices;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.devices.ide.IdeChannel;
import com.fpetrola.oozx.speccy.devices.simpleide.SimpleIdePeripheral;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleIdeTest extends MachineTest {

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
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(Spec48.class));
    return speccy;
  }

  private int in(Speccy speccy, IdeChannel.Register register) {
    return speccy.ports.read(port(register)) & 0xff;
  }

  private void out(Speccy speccy, IdeChannel.Register register, int value) {
    speccy.ports.write(port(register), (byte) value);
  }

  @Test
  void aSectorComesThroughTheLowByteOfTheBus() throws IOException {
    Speccy speccy = speccy();
    SimpleIdePeripheral ide = (SimpleIdePeripheral) speccy.peripheralRegistry.find(SimpleIdePeripheral.class);
    ide.plugIn(true);
    speccy.peripheralRegistry.update();
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
    SimpleIdePeripheral ide = (SimpleIdePeripheral) speccy.peripheralRegistry.find(SimpleIdePeripheral.class);
    assertTrue(ide.fitsOn(speccy.machine.model(Spec48.class)));
    assertTrue(ide.fitsOn(speccy.machine.model(SpecPlus3.class)));
    assertEquals(2, ide.units());
  }
}
