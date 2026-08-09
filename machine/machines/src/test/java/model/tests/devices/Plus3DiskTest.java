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
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.Fdd;
import com.fpetrola.oozx.speccy.devices.disk.Upd765Peripheral;
import model.harness.MachineTest;
import com.fpetrola.oozx.config.Configuration;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The +3's uPD765 as a program reaches it: two ports, a main status register that says whose turn
 * it is, and commands whose bytes go in one at a time. Formatting a track, writing a sector into
 * it and reading that sector back is most of the chip - it seeks, it finds address marks, it lays
 * down and checks CRCs - and it is the shortest thing that cannot pass unless it works.
 */
class Plus3DiskTest extends MachineTest {

  private static final int STATUS = 0x2ffd;
  private static final int DATA = 0x3ffd;
  private static final int MEMORY2 = 0x1ffd;

  private static final int RQM = 0x80;
  private static final int DIO = 0x40;

  private static final int ST3_TR00 = 0x10;
  private static final int ST3_READY = 0x20;

  private static final int SECTORS = 9;
  private static final int SECTOR_LENGTH = 512;
  /** The sector length code: 0x80 << 2. */
  private static final int N = 2;

  private Speccy speccy;

  private Speccy aPlus3() throws Exception {
    speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    Fdd drive = ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0);
    Disk disk = Disk.blank(1, 40, Disk.Density.DD, Disk.Type.CPC);
    disk.flag = Disk.FLAG_PLUS3_CPC;
    drive.insert(disk, false);
    speccy.ports.write(MEMORY2, (byte) 0x08);   // the motor is bit 3 of 0x1ffd
    advance(speccy, 4_000_000);                           // and it takes a moment to come up to speed
    assertTrue(drive.ready, "the drive never became ready");
    return speccy;
  }

  private int status() {
    return speccy.ports.read(STATUS) & 0xff;
  }

  /** Waits for the chip to want the byte moved in the direction asked for. */
  private void waitFor(boolean chipToHost) {
    for (int i = 0; i < 20000; i++) {
      int st = status();
      if ((st & RQM) != 0 && ((st & DIO) != 0) == chipToHost) return;
      advance(speccy, 200);
    }
    fail("the controller never asked to be " + (chipToHost ? "read" : "written")
        + "; main status is 0x" + Integer.toHexString(status()));
  }

  private int read() {
    waitFor(true);
    return speccy.ports.read(DATA) & 0xff;
  }

  private void write(int b) {
    waitFor(false);
    speccy.ports.write(DATA, (byte) b);
  }

  private void command(int... bytes) {
    for (int b : bytes) {
      write(b);
    }
  }

  private int[] result(int howMany) {
    int[] bytes = new int[howMany];
    for (int i = 0; i < howMany; i++) {
      bytes[i] = read();
    }
    return bytes;
  }

  /** SPECIFY, then RECALIBRATE and the SENSE INTERRUPT that ends it, which is how a +3 starts. */
  private void ready() {
    command(0x03, 0xa1, 0x03);                            // SPECIFY: step rate, head times, non-DMA
    command(0x07, 0x00);                                  // RECALIBRATE drive 0
    int[] sense = senseInterrupt();
    assertEquals(0x20, sense[0] & 0xe0, "the recalibrate did not end normally");
    assertEquals(0, sense[1], "the head is not on track 0");
  }

  /**
   * SENSE INTERRUPT until the seek has ended. Asked with nothing outstanding it is an invalid
   * command, which answers 0x80 and one byte only - so the cylinder is read only when there is one.
   */
  private int[] senseInterrupt() {
    for (int i = 0; i < 200; i++) {
      advance(speccy, 100_000);
      command(0x08);
      int st0 = read();
      if ((st0 & 0x80) == 0) return new int[] {st0, read()};
    }
    return fail("the seek never finished");
  }

  @Test
  void itSaysWhichChipItIsAndWhatTheDriveIsDoing() throws Exception {
    aPlus3();
    command(0x10);                                        // VERSION
    assertEquals(0x80, result(1)[0], "a uPD765A answers 0x80");

    ready();
    command(0x04, 0x00);                                  // SENSE DRIVE STATUS
    int st3 = result(1)[0];
    assertEquals(ST3_READY, st3 & ST3_READY, "the drive is not ready");
    assertEquals(ST3_TR00, st3 & ST3_TR00, "the head is not over track 0");
  }

  @Test
  void aTrackItFormattedHasTheSectorsItWasToldTo() throws Exception {
    aPlus3();
    ready();
    formatTrack(0);

    command(0x4a, 0x00);                                  // READ ID, MFM
    int[] id = result(7);
    assertEquals(0, id[0] & 0xc0, "reading an ID off the track just formatted failed");
    assertEquals(0, id[3], "the cylinder of the first ID");
    assertEquals(0, id[4], "the head of the first ID");
    assertTrue(id[5] >= 1 && id[5] <= SECTORS, "the sector of the first ID is not one of ours: " + id[5]);
    assertEquals(N, id[6], "the length code of the first ID");
  }

  @Test
  void aSectorComesBackWithWhatWasWrittenIntoIt() throws Exception {
    aPlus3();
    ready();
    formatTrack(0);

    byte[] written = new byte[SECTOR_LENGTH];
    for (int i = 0; i < written.length; i++) {
      written[i] = (byte) (i * 7 + 1);
    }

    command(0x45, 0x00, 0, 0, 1, N, 1, 0x2a, 0xff);       // WRITE DATA, MFM, sector 1 of cylinder 0
    for (byte b : written) {
      write(b & 0xff);
    }
    int[] wrote = result(7);
    assertEquals(0, wrote[1] & 0x02, "the disk came back write protected");

    command(0x46, 0x00, 0, 0, 1, N, 1, 0x2a, 0xff);       // READ DATA, the same sector
    byte[] back = new byte[SECTOR_LENGTH];
    for (int i = 0; i < back.length; i++) {
      back[i] = (byte) read();
    }
    int[] readBack = result(7);
    assertEquals(0, readBack[1] & 0x20, "the sector came back with a CRC error");
    assertEquals(0, readBack[2] & 0x20, "the data field came back with a CRC error");
    assertArrayEqualsWithFirstDifference(written, back);
  }

  @Test
  void readingASectorNobodyFormattedSaysSoInsteadOfAnsweringAnything() throws Exception {
    aPlus3();
    ready();
    formatTrack(0);

    command(0x46, 0x00, 0, 0, SECTORS + 5, N, SECTORS + 5, 0x2a, 0xff);
    int[] answer = result(7);
    assertNotEquals(0, answer[0] & 0x40, "asking for a sector that is not there ended normally");
    assertNotEquals(0, answer[1] & 0x04, "no data is what a missing sector is");
  }

  @Test
  void itStepsTheHeadToTheCylinderItIsSentTo() throws Exception {
    aPlus3();
    ready();

    command(0x0f, 0x00, 3);                               // SEEK drive 0 to cylinder 3
    int[] sense = senseInterrupt();
    assertEquals(0x20, sense[0] & 0xe0, "the seek did not end normally");
    assertEquals(3, sense[1], "the head is not on cylinder 3");
    assertEquals(3, ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0).cylinder(), "the drive disagrees about where its head is");

    formatTrack(3);
    command(0x4a, 0x00);                                  // READ ID
    int[] id = result(7);
    assertEquals(0, id[0] & 0xc0, "reading an ID off cylinder 3 failed");
    assertEquals(3, id[3], "the ID says another cylinder");

    command(0x07, 0x00);                                  // RECALIBRATE, back to track 0
    assertEquals(0, senseInterrupt()[1], "the head did not come back to track 0");
    assertEquals(0, ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0).cylinder());
  }

  /**
   * A disk somebody else's tool wrote, read through the two ports and compared with what the file
   * says is on it. It is "Tasword 2 to Tasword +3 Text File Converter (2000)(Useless Soft)" out of
   * the TOSEC set, a CPCEMU image of nine 512 byte sectors a track numbered 0xc1 up, which is what
   * a +3 disk is. Nothing here wrote it, which is the whole point of reading it.
   */
  @Test
  void itReadsADiskAnotherToolWroteExactlyAsTheFileHasIt() throws Exception {
    byte[] file;
    try (InputStream image = Plus3DiskTest.class.getResourceAsStream("/dsk/tasword-plus3-converter.dsk")) {
      file = image.readAllBytes();
    }

    speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    // A copy, because reading an image writes the corrections back into its track headers.
    ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0).insert(Disk.openBuffer("tasword.dsk", file.clone()), false);
    speccy.ports.write(MEMORY2, (byte) 0x08);
    advance(speccy, 4_000_000);
    ready();

    assertEquals("MV - CPC", new String(file, 0, 8, StandardCharsets.US_ASCII),
        "the image is not a CPC .dsk");
    int sectors = file[0x100 + 0x15] & 0xff;               // the track header says how many
    assertEquals(SECTORS, sectors, "the image is not the nine sectors a track a +3 disk has");
    for (int sector = 0; sector < sectors; sector++) {
      int id = file[0x100 + 0x18 + sector * 8 + 2] & 0xff; // and what each one is called
      command(0x46, 0x00, 0, 0, id, N, id, 0x2a, 0xff);    // READ DATA, MFM
      byte[] got = new byte[SECTOR_LENGTH];
      for (int i = 0; i < got.length; i++) {
        got[i] = (byte) read();
      }
      int[] status = result(7);
      assertEquals(0, status[1] & 0x20, "sector 0x" + Integer.toHexString(id) + " has a CRC error");
      assertEquals(0, status[1] & 0x04, "sector 0x" + Integer.toHexString(id) + " was not found");
      int at = 0x200 + sector * SECTOR_LENGTH;
      assertArrayEqualsWithFirstDifference(Arrays.copyOfRange(file, at, at + SECTOR_LENGTH), got);
      if (sector == 0) {                                   // and it is a +3 disk, not any nine sectors
        assertEquals("DISK", new String(got, 1, 4, StandardCharsets.US_ASCII),
            "the first sector is not the +3DOS specification record");
      }
    }
  }

  /** FORMAT TRACK: the chip lays the whole track down, taking the four ID bytes of each sector. */
  private void formatTrack(int cylinder) {
    command(0x4d, 0x00, N, SECTORS, 0x2a, 0xe5);          // MFM, drive 0 head 0, filler 0xe5
    for (int sector = 1; sector <= SECTORS; sector++) {
      write(cylinder);
      write(0);
      write(sector);
      write(N);
    }
    int[] formatted = result(7);
    assertEquals(0, formatted[0] & 0xc0, "the format did not end normally");
  }

  private static void assertArrayEqualsWithFirstDifference(byte[] expected, byte[] actual) {
    for (int i = 0; i < expected.length; i++) {
      if (expected[i] != actual[i]) {
        fail("byte " + i + " of the sector came back as 0x" + Integer.toHexString(actual[i] & 0xff)
            + " and not 0x" + Integer.toHexString(expected[i] & 0xff));
      }
    }
  }

  /** The +3's disk settings come through the module's mirror, the file never having heard of the device. */
  @Test
  void theFileReachesTheController() throws Exception {
    Path file = Files.createTempFile("oozx", ".json");
    Files.writeString(file, "{\"machine\": {\"plus3\": {\"detectSpeedlock\": true}}}");
    Speccy speccy = silentMachine(binder -> binder.bind(Configuration.class).toInstance(new Configuration(file.toFile())));
    assertTrue(((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).detectSpeedlock());
  }

  /**
   * The drives are what the settings say, and what ships is what the +3 shipped with: its own
   * single-sided forty-track drive in A, and a double-sided eighty-track drive in B. This used
   * to come from a stub of the option enumerators that answered zero for both, which for B is
   * "disabled": every +3 here came up with one drive, and nothing said so.
   */
  @Test
  void theDrivesAreWhatTheSettingsSayAndWhatShippedWithIt() {
    Speccy speccy = silentMachine();
    Upd765Peripheral plus3 = (Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class);
    assertEquals(Fdd.Kind.SINGLE_SIDED_40, plus3.driveA());
    assertEquals(Fdd.Kind.DOUBLE_SIDED_80, plus3.driveB());
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    assertEquals(1, plus3.drive(0).heads(), "drive A is single-sided");
    assertEquals(Fdd.Type.SHUGART, plus3.drive(1).type(), "drive B is there");
    assertEquals(2, plus3.drive(1).heads(), "and double-sided");

    plus3.setDriveB(Fdd.Kind.DISABLED);
    speccy.machine.reset(true);
    assertEquals(Fdd.Type.NONE, plus3.drive(1).type(), "set to none, drive B is not there");
  }
}
