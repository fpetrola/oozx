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
import com.fpetrola.oozx.speccy.devices.didaktik.DidaktikPeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DidaktikTest {

  /** Fourteen kilobytes with a mark at the start of each of its three pieces. */
  private static File pretendRom() throws IOException {
    byte[] image = new byte[DidaktikPeripheral.ROM_SIZE];
    image[0] = (byte) 0xA1;
    image[0x2000] = (byte) 0xA2;
    image[0x3000] = (byte) 0xA3;
    File file = File.createTempFile("didaktik", ".rom");
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
    speccy.machine.select(speccy.spec48);
    return speccy;
  }

  private DidaktikPeripheral aFortyEightWithADidaktik(Speccy speccy) throws IOException {
    speccy.settings.current.romDidaktik80 = pretendRom().getPath();
    DidaktikPeripheral didaktik = (DidaktikPeripheral) speccy.peripherals.find(DidaktikPeripheral.class);
    didaktik.plugIn(true);
    assertTrue(speccy.peripherals.update());
    speccy.machine.reset(true);
    assertTrue(didaktik.isAvailable());
    return didaktik;
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

  private int peek(Speccy speccy, int address) {
    return speccy.memory.readByteInternal(address) & 0xff;
  }

  @Test
  void itsRomIsInThreePiecesWithItsRamAboveThem() throws IOException {
    Speccy speccy = speccy();
    DidaktikPeripheral didaktik = aFortyEightWithADidaktik(speccy);
    assertFalse(didaktik.isPaged());
    speccy.z80.jump(0x0008);
    speccy.z80.step();
    assertTrue(didaktik.isPaged(), "0x0008 did not page it in");
    assertEquals(0xA1, peek(speccy, 0x0000));
    assertEquals(0xA2, peek(speccy, 0x2000));
    assertEquals(0xA3, peek(speccy, 0x3000));
    speccy.memory.writeByteInternal2(0x3801, (byte) 0x77);
    assertEquals(0x77, peek(speccy, 0x3801), "its RAM is not at 0x3800");
    speccy.z80.jump(0x1700);
    speccy.z80.step();
    assertFalse(didaktik.isPaged(), "0x1700 did not page it out");
  }

  @Test
  void aSectorComesThroughItsPortsWithTheDriveOnFromAux() throws Exception {
    Speccy speccy = speccy();
    DidaktikPeripheral didaktik = aFortyEightWithADidaktik(speccy);
    byte[] image = new byte[180 + 2 * 80 * 9 * 512];
    image[0xb1] = 0x10;                        // two sides
    image[0xb2] = 80;
    image[0xb3] = 9;
    System.arraycopy("DIDAKTIK".getBytes(StandardCharsets.US_ASCII), 0, image, 2 * 512, 8);
    didaktik.insert(0, Disk.openBuffer("test.d80", image));

    out(speccy, 0x89, 0x05);                    // drive 0 selected, its motor on
    out(speccy, 0x81, 0x08);                    // restore
    for (int i = 0; i < 5000 && (in(speccy, 0x81) & WdFdc.SR_BUSY) != 0; i++) wait(speccy, 3500);
    assertEquals(0, in(speccy, 0x81) & WdFdc.SR_BUSY);
    out(speccy, 0x83, 0);
    out(speccy, 0x85, 3);
    out(speccy, 0x81, 0x80);                    // read sector 3
    byte[] data = new byte[512];
    int got = 0;
    for (int i = 0; i < 200000 && got < 512; i++) {
      int status = in(speccy, 0x81);
      if ((status & WdFdc.SR_BUSY) == 0) break;
      if ((status & WdFdc.SR_IDX_DRQ) != 0) data[got++] = (byte) in(speccy, 0x87);
      wait(speccy, 120);
    }
    assertEquals(512, got, "the sector did not come out whole");
    assertEquals("DIDAKTIK", new String(data, 0, 8, StandardCharsets.US_ASCII));
  }

  @Test
  void snapTurnsTheNmiIntoARst0ThatPagesTheRomIn() throws IOException {
    Speccy speccy = speccy();
    DidaktikPeripheral didaktik = aFortyEightWithADidaktik(speccy);
    speccy.z80.jump(0x8000);
    speccy.z80.step();
    assertFalse(didaktik.isPaged());
    int sp = speccy.z80.ooz80.getState().getRegisterSP().read();

    didaktik.button();
    speccy.z80.doOpcodes();
    speccy.eventManager.eventDoEvents();
    assertEquals(0x0066, speccy.z80.ooz80.getState().getPc().read(), "the NMI was not taken");
    speccy.z80.step();
    speccy.z80.step();
    assertTrue(didaktik.isPaged(), "RST 0 at the NMI vector did not page the Didaktik in");
    assertEquals(0x0067, peek(speccy, sp - 4) | peek(speccy, sp - 3) << 8, "RST 0 pushes the address after it");
  }

  @Test
  void itWasSoldForTheFortyEight() {
    Speccy speccy = speccy();
    DidaktikPeripheral didaktik = (DidaktikPeripheral) speccy.peripherals.find(DidaktikPeripheral.class);
    assertTrue(didaktik.fitsOn(speccy.spec48));
    assertFalse(didaktik.fitsOn(speccy.spec128));
  }
}
