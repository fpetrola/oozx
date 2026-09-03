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
import com.fpetrola.oozx.speccy.devices.zxatasp.ZxataspPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZxataspTest {

  private static final int PORT_A = 0x009f;
  private static final int PORT_B = 0x019f;
  private static final int PORT_C = 0x029f;
  private static final int CONTROL = 0x039f;
  private static final int ALL_OUTPUTS = 0x80;
  private static final int A_AND_B_INPUTS = 0x92;
  private static final int PRIMARY = 0x20;
  private static final int WRITE = 0x08;
  private static final int READ = 0x10;

  private static File aDisk() throws IOException {
    File file = File.createTempFile("zxatasp", ".hdf");
    file.deleteOnExit();
    IdeChannel.createHdf(file, 16);
    try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
      out.seek(0x216 + 5L * IdeChannel.SECTOR);
      out.write("ZXATASP!".getBytes(StandardCharsets.US_ASCII));
    }
    return file;
  }

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.machine.select(speccy.spec48);
    return speccy;
  }

  private ZxataspPeripheral aFortyEightWithAZxatasp(Speccy speccy) {
    ZxataspPeripheral zxatasp = (ZxataspPeripheral) speccy.peripherals.find(ZxataspPeripheral.class);
    zxatasp.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    return zxatasp;
  }

  private int in(Speccy speccy, int port) {
    return speccy.peripherals.readPort(port) & 0xff;
  }

  private void out(Speccy speccy, int port, int value) {
    speccy.peripherals.writePort(port, (byte) value);
  }

  private int peek(Speccy speccy, int address) {
    return speccy.memory.readByteInternal(address) & 0xff;
  }

  private void poke(Speccy speccy, int address, int value) {
    speccy.memory.writeByteInternal2(address, (byte) value);
  }

  /** A register written through the 8255: the value on port A, the register and a write strobe on port C. */
  private void writeRegister(Speccy speccy, IdeChannel.Register register, int value) {
    out(speccy, PORT_A, value);
    out(speccy, PORT_C, PRIMARY | register.ordinal());
    out(speccy, PORT_C, PRIMARY | WRITE | register.ordinal());
  }

  private void readStrobe(Speccy speccy, IdeChannel.Register register) {
    out(speccy, PORT_C, PRIMARY | register.ordinal());
    out(speccy, PORT_C, PRIMARY | READ | register.ordinal());
  }

  /** Fuse's own vectors: the latch picks the bank, and bit 7 with it turns the memory off. */
  @Test
  void theRamLatchOnPortCPicksTheBank() {
    Speccy speccy = speccy();
    ZxataspPeripheral zxatasp = aFortyEightWithAZxatasp(speccy);
    assertTrue(zxatasp.isPaged(), "a reset leaves bank 0 at the bottom");
    assertEquals(0, zxatasp.bank());
    out(speccy, CONTROL, ALL_OUTPUTS);
    out(speccy, PORT_C, 0x40);
    poke(speccy, 0x0000, 0xA0);
    assertEquals(0xA0, peek(speccy, 0x0000), "bank 0 is writable");
    out(speccy, PORT_C, 0x41);
    assertEquals(1, zxatasp.bank());
    assertEquals(0x00, peek(speccy, 0x0000));
    out(speccy, PORT_C, 0x5f);
    assertEquals(31, zxatasp.bank());
    out(speccy, PORT_C, 0x40);
    assertEquals(0xA0, peek(speccy, 0x0000));
    out(speccy, PORT_C, 0xc0);
    assertFalse(zxatasp.isPaged());
    assertEquals(0xF3, peek(speccy, 0x0000), "the machine's ROM is back");
  }

  @Test
  void theJumpersProtectTheOddBanksAndUploadBehindTheRom() {
    Speccy speccy = speccy();
    ZxataspPeripheral zxatasp = aFortyEightWithAZxatasp(speccy);
    out(speccy, CONTROL, ALL_OUTPUTS);
    speccy.settings.current.zxataspWp = true;
    out(speccy, PORT_C, 0x41);
    poke(speccy, 0x0000, 0x11);
    assertEquals(0x00, peek(speccy, 0x0000), "bank 1 is protected by the jumper");
    out(speccy, PORT_C, 0x42);
    poke(speccy, 0x0000, 0x22);
    assertEquals(0x22, peek(speccy, 0x0000), "bank 2 is not");
    speccy.settings.current.zxataspUpload = true;
    zxatasp.refresh();
    assertEquals(0xF3, peek(speccy, 0x0000), "uploading, reads see the ROM");
    poke(speccy, 0x0000, 0x33);
    speccy.settings.current.zxataspUpload = false;
    zxatasp.refresh();
    assertEquals(0x33, peek(speccy, 0x0000), "while writes went to the bank");
  }

  @Test
  void aSectorComesThroughThe8255ABytePerPort() throws IOException {
    Speccy speccy = speccy();
    ZxataspPeripheral zxatasp = aFortyEightWithAZxatasp(speccy);
    zxatasp.insert(IdeChannel.MASTER, aDisk());
    out(speccy, CONTROL, ALL_OUTPUTS);
    writeRegister(speccy, IdeChannel.Register.HEAD_DRIVE, 0xe0);
    writeRegister(speccy, IdeChannel.Register.SECTOR_COUNT, 1);
    writeRegister(speccy, IdeChannel.Register.SECTOR, 5);
    writeRegister(speccy, IdeChannel.Register.CYLINDER_LOW, 0);
    writeRegister(speccy, IdeChannel.Register.CYLINDER_HIGH, 0);
    writeRegister(speccy, IdeChannel.Register.COMMAND_STATUS, 0x20);

    out(speccy, CONTROL, A_AND_B_INPUTS);
    readStrobe(speccy, IdeChannel.Register.COMMAND_STATUS);
    assertEquals(IdeChannel.STATUS_DRQ, in(speccy, PORT_A) & IdeChannel.STATUS_DRQ, "the status comes back on port A");
    byte[] sector = new byte[IdeChannel.SECTOR];
    for (int i = 0; i < sector.length; i += 2) {
      readStrobe(speccy, IdeChannel.Register.DATA);
      sector[i] = (byte) in(speccy, PORT_A);
      sector[i + 1] = (byte) in(speccy, PORT_B);
    }
    assertEquals("ZXATASP!", new String(sector, 0, 8, StandardCharsets.US_ASCII));
  }

  @Test
  void aBitOfPortCCanBeSetThroughTheControlPort() {
    Speccy speccy = speccy();
    aFortyEightWithAZxatasp(speccy);
    out(speccy, CONTROL, ALL_OUTPUTS);
    out(speccy, PORT_C, 0x00);
    out(speccy, CONTROL, 0x0d);
    assertEquals(0x40, in(speccy, PORT_C), "bit 6 set");
    out(speccy, CONTROL, 0x0c);
    assertEquals(0x00, in(speccy, PORT_C), "bit 6 reset");
  }
}
