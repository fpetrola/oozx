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
import com.fpetrola.oozx.speccy.devices.beta128.PluggedBeta128Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.Beta128Peripheral;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Beta128Test {

  /** A 16K "TR-DOS" whose entry at 0x3d00 is RET, and whose first byte tells it from the 48K ROM. */
  private static File pretendTrDos() throws IOException {
    byte[] image = new byte[Beta128Peripheral.ROM_SIZE];
    image[0] = (byte) 0xAA;
    image[0x3d00] = (byte) 0xC9;
    File file = File.createTempFile("trdos", ".rom");
    file.deleteOnExit();
    Files.write(file.toPath(), image);
    return file;
  }

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    return speccy;
  }

  private Beta128Peripheral plugged(Speccy speccy) {
    return (Beta128Peripheral) speccy.peripherals.find(PluggedBeta128Peripheral.class);
  }

  private Beta128Peripheral aFortyEightWithABeta(Speccy speccy, String rom) {
    speccy.machine.select(speccy.spec48);
    speccy.settings.current.romBeta128 = rom;
    Beta128Peripheral beta = plugged(speccy);
    beta.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    assertTrue(beta.isAvailable(), "the ROM was not read");
    return beta;
  }

  private void wait(Speccy speccy, int tstates) {
    while (tstates > 0) {
      int step = Math.min(tstates, 1000);
      speccy.zxClock.addTStates(step);
      speccy.eventManager.eventDoEvents();
      tstates -= step;
    }
  }

  private int in(Speccy speccy, int port) {
    return speccy.peripherals.readPort(port) & 0xff;
  }

  private void out(Speccy speccy, int port, int value) {
    speccy.peripherals.writePort(port, (byte) value);
  }

  private void untilNotBusy(Speccy speccy) {
    for (int i = 0; i < 5000 && (in(speccy, 0x1f) & WdFdc.SR_BUSY) != 0; i++) {
      wait(speccy, 3500);
    }
    assertEquals(0, in(speccy, 0x1f) & WdFdc.SR_BUSY, "the controller is still busy");
  }

  @Test
  void reachingTrDosEntryPointsPagesItInAndRunningAboveTheRomPagesItOut() throws IOException {
    Speccy speccy = speccy();
    Beta128Peripheral beta = aFortyEightWithABeta(speccy, pretendTrDos().getPath());
    assertFalse(beta.isPaged(), "with the system switch off, a 48K boots into BASIC");

    speccy.z80.jump(0x3d00);
    speccy.z80.step();
    assertTrue(beta.isPaged(), "reaching 0x3d00 did not page TR-DOS in");
    assertEquals(0xAA, speccy.memory.readByteInternal(0) & 0xff);

    speccy.z80.jump(0x8000);
    speccy.z80.step();
    assertFalse(beta.isPaged(), "running above the ROM did not page TR-DOS out");
    assertEquals(0xF3, speccy.memory.readByteInternal(0) & 0xff);
  }

  @Test
  void itsPortsOnlyAnswerWhilePagedIn() throws Exception {
    Speccy speccy = speccy();
    Beta128Peripheral beta = aFortyEightWithABeta(speccy, pretendTrDos().getPath());
    byte[] image = new byte[2 * 80 * 16 * 256];
    image[8 * 256 + 227] = 0x16;
    image[8 * 256 + 231] = 0x10;
    System.arraycopy("TRDOSDISK".getBytes(StandardCharsets.US_ASCII), 0, image, 4 * 256, 9);
    beta.insert(0, Disk.openBuffer("test.trd", image));

    assertEquals(0xff, in(speccy, 0x3f), "not paged, the track register is not on the bus");
    speccy.z80.jump(0x3d00);
    speccy.z80.step();
    out(speccy, 0xff, 0x3c);                    // drive A, side 0, MFM, HLT
    out(speccy, 0x1f, 0x08);                    // restore, head loaded - which on a Beta is READY
    untilNotBusy(speccy);
    out(speccy, 0x3f, 0);
    out(speccy, 0x5f, 5);
    out(speccy, 0x1f, 0x80);                    // read sector 5
    byte[] data = new byte[256];
    int got = 0;
    for (int i = 0; i < 200000 && got < 256; i++) {
      int status = in(speccy, 0x1f);
      if ((status & WdFdc.SR_BUSY) == 0) break;
      if ((in(speccy, 0xff) & 0x40) != 0) data[got++] = (byte) in(speccy, 0x7f); else wait(speccy, 100);
    }
    assertEquals(256, got, "the sector did not come out whole");
    assertEquals("TRDOSDISK", new String(data, 0, 9, StandardCharsets.US_ASCII));
  }

  @Test
  void itGoesOnAFortyEightOrA128AndAPentagonHasItsOwn() {
    Speccy speccy = speccy();
    Beta128Peripheral plugged = plugged(speccy);
    Beta128Peripheral builtIn = (Beta128Peripheral) speccy.peripherals.find(Beta128Peripheral.class);
    assertTrue(plugged.fitsOn(speccy.spec48));
    assertTrue(plugged.fitsOn(speccy.spec128));
    assertFalse(plugged.fitsOn(speccy.specPlus3));
    assertFalse(plugged.fitsOn(speccy.pentagon));
    assertTrue(builtIn.fitsOn(speccy.pentagon));
    assertFalse(builtIn.fitsOn(speccy.spec128));
  }

  /** With a TR-DOS ROM, if one is on this machine: a Pentagon starts in it, and its ROM is at the bottom. */
  @Test
  void aPentagonStartsInTrDos() throws IOException {
    File rom = new File(System.getProperty("user.home"), "detodo/spectrum/Roms/trdos.rom");
    Assumptions.assumeTrue(rom.isFile(), "no TR-DOS ROM on this machine");
    Speccy speccy = speccy();
    speccy.settings.current.romPentagon2 = rom.getPath();
    speccy.machine.select(speccy.pentagon);
    Beta128Peripheral builtIn = (Beta128Peripheral) speccy.peripherals.find(Beta128Peripheral.class);
    assertTrue(speccy.peripherals.isActive(Beta128Peripheral.class), "a Pentagon comes with its Beta");
    assertTrue(builtIn.isAvailable());
    assertTrue(builtIn.isPaged(), "a Pentagon starts in TR-DOS");
    byte[] image = Files.readAllBytes(rom.toPath());
    for (int address = 0; address < 16; address++) {
      assertEquals(image[address] & 0xff, speccy.memory.readByteInternal(address) & 0xff, "TR-DOS is not at " + address);
    }
    speccy.z80.step();
    assertEquals(1, speccy.z80.ooz80.getState().getPc().read(), "the processor did not start on it");
  }
}
