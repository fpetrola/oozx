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
import com.fpetrola.oozx.speccy.devices.zxcf.ZxcfPeripheral;
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

class ZxcfTest {

  private static final int MEMORY_CONTROL = 0x10b4;

  private static int port(IdeChannel.Register register) {
    return 0x00b4 | register.ordinal() << 8;
  }

  private static File aCard() throws IOException {
    File file = File.createTempFile("zxcf", ".hdf");
    file.deleteOnExit();
    IdeChannel.createHdf(file, 16);
    try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
      out.seek(0x216 + 5L * IdeChannel.SECTOR);
      out.write("COMPACT".getBytes(StandardCharsets.US_ASCII));
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

  private ZxcfPeripheral aFortyEightWithAZxcf(Speccy speccy) {
    ZxcfPeripheral zxcf = (ZxcfPeripheral) speccy.peripherals.find(ZxcfPeripheral.class);
    zxcf.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    return zxcf;
  }

  /** The board's ports are even, so the ULA answers them too, and every write to them resets its EAR bit: raised, it answers all ones. */
  private void earHigh(Speccy speccy) {
    out(speccy, 0x00fe, 0x18);
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

  /** Fuse's own vectors, plus the write-enable bit: bank 0, bank 63, memory off. */
  @Test
  void theMemoryRegisterPicksTheBankAndWhetherItCanBeWritten() {
    Speccy speccy = speccy();
    ZxcfPeripheral zxcf = aFortyEightWithAZxcf(speccy);
    assertTrue(zxcf.isPaged(), "a reset leaves bank 0 at the bottom, protected");
    poke(speccy, 0x0000, 0x11);
    assertEquals(0x00, peek(speccy, 0x0000));
    out(speccy, MEMORY_CONTROL, 0x40);
    poke(speccy, 0x0000, 0xA0);
    assertEquals(0xA0, peek(speccy, 0x0000), "bit 6 makes the bank writable");
    out(speccy, MEMORY_CONTROL, 0x7f);
    assertEquals(63, zxcf.bank());
    assertEquals(0x00, peek(speccy, 0x0000));
    out(speccy, MEMORY_CONTROL, 0x00);
    assertEquals(0xA0, peek(speccy, 0x0000), "bank 0 kept what was written");
    poke(speccy, 0x0000, 0x55);
    assertEquals(0xA0, peek(speccy, 0x0000), "and is protected again");
    out(speccy, MEMORY_CONTROL, 0x80);
    assertFalse(zxcf.isPaged());
    assertEquals(0xF3, peek(speccy, 0x0000), "the machine's ROM is back");
    earHigh(speccy);
    assertEquals(0xff, in(speccy, MEMORY_CONTROL), "the register reads back as nothing");
    assertEquals(0x80, zxcf.lastMemoryControl());
  }

  @Test
  void uploadingReadsTheRomWhileWritingTheBank() {
    Speccy speccy = speccy();
    ZxcfPeripheral zxcf = aFortyEightWithAZxcf(speccy);
    out(speccy, MEMORY_CONTROL, 0x41);
    speccy.settings.current.zxcfUpload = true;
    zxcf.refresh();
    assertEquals(0xF3, peek(speccy, 0x0000));
    poke(speccy, 0x0000, 0x33);
    speccy.settings.current.zxcfUpload = false;
    zxcf.refresh();
    assertEquals(0x33, peek(speccy, 0x0000));
  }

  @Test
  void aSectorComesFromTheCardAndThereIsOnlyOne() throws IOException {
    Speccy speccy = speccy();
    ZxcfPeripheral zxcf = aFortyEightWithAZxcf(speccy);
    assertEquals(1, zxcf.units());
    zxcf.insert(IdeChannel.MASTER, aCard());
    out(speccy, port(IdeChannel.Register.HEAD_DRIVE), 0xe0);
    out(speccy, port(IdeChannel.Register.SECTOR_COUNT), 1);
    out(speccy, port(IdeChannel.Register.SECTOR), 5);
    out(speccy, port(IdeChannel.Register.CYLINDER_LOW), 0);
    out(speccy, port(IdeChannel.Register.CYLINDER_HIGH), 0);
    out(speccy, port(IdeChannel.Register.COMMAND_STATUS), 0x20);
    earHigh(speccy);
    assertEquals(IdeChannel.STATUS_DRQ, in(speccy, port(IdeChannel.Register.COMMAND_STATUS)) & IdeChannel.STATUS_DRQ);
    byte[] sector = new byte[IdeChannel.SECTOR];
    for (int i = 0; i < sector.length; i++) {
      sector[i] = (byte) in(speccy, port(IdeChannel.Register.DATA));
    }
    assertEquals("COMPACT", new String(sector, 0, 7, StandardCharsets.US_ASCII));
  }
}
