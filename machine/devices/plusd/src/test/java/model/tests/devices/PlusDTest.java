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
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import com.fpetrola.oozx.speccy.devices.plusd.PlusDPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlusDTest {

  private static final int STATUS = 0xe3;
  private static final int COMMAND = 0xe3;
  private static final int TRACK = 0xeb;
  private static final int SECTOR = 0xf3;
  private static final int DATA = 0xfb;
  private static final int CONTROL = 0xef;

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

  private PlusDPeripheral plusd(Speccy speccy) {
    return (PlusDPeripheral) speccy.peripherals.find(PlusDPeripheral.class);
  }

  private PlusDPeripheral aFortyEightWithAPlusD(Speccy speccy) {
    PlusDPeripheral plusd = plusd(speccy);
    plusd.plugIn(true);
    assertTrue(speccy.peripherals.update(), "a +D arrives with a hard reset");
    speccy.machine.reset(true);
    assertTrue(plusd.isAvailable(), "plusd.rom ships with the emulator and was not read");
    return plusd;
  }

  /** An MGT image, 80 tracks of two sides of ten sectors, with a word at the start of every sector. */
  private static Disk anMgtDisk() throws Exception {
    byte[] image = new byte[2 * 80 * 10 * 512];
    int at = 0;
    for (int cylinder = 0; cylinder < 80; cylinder++) {
      for (int side = 0; side < 2; side++) {
        for (int sector = 1; sector <= 10; sector++) {
          byte[] label = String.format("T%02dS%dR%02d", cylinder, side, sector).getBytes(StandardCharsets.US_ASCII);
          System.arraycopy(label, 0, image, at, label.length);
          at += 512;
        }
      }
    }
    return Disk.openBuffer("test.mgt", image);
  }

  /** Lets the drive turn and the controller work: the clock goes on and the events it set fire. */
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
    for (int i = 0; i < 5000 && (in(speccy, STATUS) & WdFdc.SR_BUSY) != 0; i++) {
      wait(speccy, 3500);
    }
    assertEquals(0, in(speccy, STATUS) & WdFdc.SR_BUSY, "the controller is still busy");
  }

  private byte[] readSector(Speccy speccy, int track, int sector) {
    out(speccy, TRACK, track);
    out(speccy, SECTOR, sector);
    out(speccy, COMMAND, 0x80);
    byte[] data = new byte[512];
    int got = 0;
    for (int i = 0; i < 200000 && got < 512; i++) {
      int status = in(speccy, STATUS);
      if ((status & WdFdc.SR_BUSY) == 0) {
        break;
      }
      if ((status & WdFdc.SR_IDX_DRQ) != 0) {
        data[got++] = (byte) in(speccy, DATA);
      } else {
        wait(speccy, 100);
      }
    }
    assertEquals(512, got, "the sector did not come out whole");
    untilNotBusy(speccy);
    return data;
  }

  private static String labelOf(byte[] sector) {
    return new String(sector, 0, 8, StandardCharsets.US_ASCII);
  }

  @Test
  void theControllerReadsTheSectorsOffAnMgtImage() throws Exception {
    Speccy speccy = speccy();
    PlusDPeripheral plusd = aFortyEightWithAPlusD(speccy);
    plusd.insert(0, anMgtDisk());

    out(speccy, CONTROL, 0x01);                 // drive 1, side 0
    out(speccy, COMMAND, 0x00);                 // restore
    untilNotBusy(speccy);
    assertEquals(0, in(speccy, TRACK));

    assertEquals("T00S0R01", labelOf(readSector(speccy, 0, 1)));
    assertEquals("T00S0R07", labelOf(readSector(speccy, 0, 7)));

    out(speccy, DATA, 5);                       // seek track 5
    out(speccy, COMMAND, 0x10);
    untilNotBusy(speccy);
    assertEquals(5, in(speccy, TRACK));
    assertEquals("T05S0R03", labelOf(readSector(speccy, 5, 3)));

    out(speccy, CONTROL, 0x81);                 // the other side
    assertEquals("T05S1R10", labelOf(readSector(speccy, 5, 10)));
  }

  @Test
  void whatIsWrittenComesBackInTheImage() throws Exception {
    Speccy speccy = speccy();
    PlusDPeripheral plusd = aFortyEightWithAPlusD(speccy);
    Disk disk = anMgtDisk();
    plusd.insert(0, disk);
    out(speccy, CONTROL, 0x01);
    out(speccy, COMMAND, 0x00);
    untilNotBusy(speccy);

    out(speccy, TRACK, 0);
    out(speccy, SECTOR, 2);
    out(speccy, COMMAND, 0xa0);                 // write sector
    byte[] written = "WRITTEN!".getBytes(StandardCharsets.US_ASCII);
    int put = 0;
    for (int i = 0; i < 200000 && put < 512; i++) {
      int status = in(speccy, STATUS);
      if ((status & WdFdc.SR_BUSY) == 0) {
        break;
      }
      if ((status & WdFdc.SR_IDX_DRQ) != 0) {
        out(speccy, DATA, put < written.length ? written[put] : 0);
        put++;
      } else {
        wait(speccy, 100);
      }
    }
    assertEquals(512, put, "the sector was not taken whole");
    untilNotBusy(speccy);
    assertTrue(disk.dirty, "the disk was written and does not say so");

    byte[] image = disk.toImage();
    assertEquals("WRITTEN!", new String(image, 512, 8, StandardCharsets.US_ASCII), "the image does not hold what was written");
    assertEquals("T00S0R01", new String(image, 0, 8, StandardCharsets.US_ASCII), "the sector next to it was disturbed");
  }

  @Test
  void readingPort0xe7PagesTheRomInAndWritingItPagesItOut() {
    Speccy speccy = speccy();
    PlusDPeripheral plusd = aFortyEightWithAPlusD(speccy);
    in(speccy, 0xe7);
    assertTrue(plusd.isPaged());
    assertEquals(0x33, speccy.memory.readByteInternal(6) & 0xff, "the +D ROM jumps to 0x0533 from its start, and it is not there");
    speccy.memory.writeByteInternal2(0x2005, (byte) 0x42);
    assertEquals(0x42, speccy.memory.readByteInternal(0x2005) & 0xff, "the +D's RAM is not at 0x2000");
    out(speccy, 0xe7, 0);
    assertFalse(plusd.isPaged());
    assertEquals(0xAF, speccy.memory.readByteInternal(1) & 0xff, "the machine's ROM did not come back");
  }

  @Test
  void itGoesOnTheSinclairMachinesOnly() {
    Speccy speccy = speccy();
    PlusDPeripheral plusd = plusd(speccy);
    assertTrue(plusd.fitsOn(speccy.spec48));
    assertTrue(plusd.fitsOn(speccy.spec128));
    assertFalse(plusd.fitsOn(speccy.specPlus3));
    assertFalse(plusd.fitsOn(speccy.pentagon));
  }
}
