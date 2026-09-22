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
 * Exercises the +3's uPD765 through its two real ports (status/data), byte at a time, covering
 * seek, format, address marks, CRC checks and sector read/write - the chip's whole job.
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
  /** Size code 2, i.e. 128<<2 = 512 bytes. */
  private static final int N = 2;

  private Speccy speccy;

  private Speccy aPlus3() throws Exception {
    speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    Fdd drive = ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0);
    Disk disk = Disk.blank(1, 40, Disk.Density.DD, Disk.Type.CPC);
    disk.flag = Disk.FLAG_PLUS3_CPC;
    drive.insert(disk, false);
    speccy.ports.write(MEMORY2, (byte) 0x08);   // bit 3 of 0x1ffd starts the drive motor
    advance(speccy, 4_000_000);                           // motor spin-up delay
    assertTrue(drive.ready, "the drive never became ready");
    return speccy;
  }

  private int status() {
    return speccy.ports.read(STATUS) & 0xff;
  }

  /** Blocks until RQM/DIO shows the controller ready to transfer in the given direction. */
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

  /** Standard +3 boot sequence for the controller: SPECIFY, RECALIBRATE, then SENSE INTERRUPT. */
  private void ready() {
    command(0x03, 0xa1, 0x03);                            // SPECIFY command byte + its two parameter bytes
    command(0x07, 0x00);                                  // RECALIBRATE, unit 0
    int[] sense = senseInterrupt();
    assertEquals(0x20, sense[0] & 0xe0, "the recalibrate did not end normally");
    assertEquals(0, sense[1], "the head is not on track 0");
  }

  /** Polls SENSE INTERRUPT until a seek completes; with nothing pending it returns just 0x80,
   * so the second (cylinder) byte is only read once a real result is confirmed. */
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
    command(0x10);                                        // VERSION command
    assertEquals(0x80, result(1)[0], "a uPD765A answers 0x80");

    ready();
    command(0x04, 0x00);                                  // SENSE DRIVE STATUS, unit 0
    int st3 = result(1)[0];
    assertEquals(ST3_READY, st3 & ST3_READY, "the drive is not ready");
    assertEquals(ST3_TR00, st3 & ST3_TR00, "the head is not over track 0");
  }

  @Test
  void aTrackItFormattedHasTheSectorsItWasToldTo() throws Exception {
    aPlus3();
    ready();
    formatTrack(0);

    command(0x4a, 0x00);                                  // READ ID (MFM bit set), unit 0
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

    command(0x45, 0x00, 0, 0, 1, N, 1, 0x2a, 0xff);       // WRITE DATA: cylinder 0, sector 1
    for (byte b : written) {
      write(b & 0xff);
    }
    int[] wrote = result(7);
    assertEquals(0, wrote[1] & 0x02, "the disk came back write protected");

    command(0x46, 0x00, 0, 0, 1, N, 1, 0x2a, 0xff);       // READ DATA: cylinder 0, sector 1 again
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

    command(0x0f, 0x00, 3);                               // SEEK: unit 0 to cylinder 3
    int[] sense = senseInterrupt();
    assertEquals(0x20, sense[0] & 0xe0, "the seek did not end normally");
    assertEquals(3, sense[1], "the head is not on cylinder 3");
    assertEquals(3, ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0).cylinder(), "the drive disagrees about where its head is");

    formatTrack(3);
    command(0x4a, 0x00);                                  // READ ID
    int[] id = result(7);
    assertEquals(0, id[0] & 0xc0, "reading an ID off cylinder 3 failed");
    assertEquals(3, id[3], "the ID says another cylinder");

    command(0x07, 0x00);                                  // RECALIBRATE back to track 0
    assertEquals(0, senseInterrupt()[1], "the head did not come back to track 0");
    assertEquals(0, ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0).cylinder());
  }

  /**
   * Cross-checks against a third-party CPCEMU image ("Tasword 2 to Tasword +3 Text File
   * Converter", TOSEC), nine 512-byte sectors numbered from 0xc1, the standard +3 layout;
   * this test only reads it, never writes it.
   */
  @Test
  void itReadsADiskAnotherToolWroteExactlyAsTheFileHasIt() throws Exception {
    byte[] file;
    try (InputStream image = Plus3DiskTest.class.getResourceAsStream("/dsk/tasword-plus3-converter.dsk")) {
      file = image.readAllBytes();
    }

    speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    // clone() avoids mutating file: loading a disk patches its in-memory track headers.
    ((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).drive(0).insert(Disk.openBuffer("tasword.dsk", file.clone()), false);
    speccy.ports.write(MEMORY2, (byte) 0x08);
    advance(speccy, 4_000_000);
    ready();

    assertEquals("MV - CPC", new String(file, 0, 8, StandardCharsets.US_ASCII),
        "the image is not a CPC .dsk");
    int sectors = file[0x100 + 0x15] & 0xff;               // sector count field of the track header
    assertEquals(SECTORS, sectors, "the image is not the nine sectors a track a +3 disk has");
    for (int sector = 0; sector < sectors; sector++) {
      int id = file[0x100 + 0x18 + sector * 8 + 2] & 0xff; // sector-ID field of that entry
      command(0x46, 0x00, 0, 0, id, N, id, 0x2a, 0xff);    // READ DATA for that sector ID
      byte[] got = new byte[SECTOR_LENGTH];
      for (int i = 0; i < got.length; i++) {
        got[i] = (byte) read();
      }
      int[] status = result(7);
      assertEquals(0, status[1] & 0x20, "sector 0x" + Integer.toHexString(id) + " has a CRC error");
      assertEquals(0, status[1] & 0x04, "sector 0x" + Integer.toHexString(id) + " was not found");
      int at = 0x200 + sector * SECTOR_LENGTH;
      assertArrayEqualsWithFirstDifference(Arrays.copyOfRange(file, at, at + SECTOR_LENGTH), got);
      if (sector == 0) {                                   // confirms this is +3DOS, not just 9 generic sectors
        assertEquals("DISK", new String(got, 1, 4, StandardCharsets.US_ASCII),
            "the first sector is not the +3DOS specification record");
      }
    }
  }

  /** Issues FORMAT TRACK, streaming cylinder/head/sector/size for every sector as required. */
  private void formatTrack(int cylinder) {
    command(0x4d, 0x00, N, SECTORS, 0x2a, 0xe5);          // FORMAT TRACK, unit 0, filler byte 0xe5
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

  /** Config JSON for the +3 reaches the controller via the module's settings mirror. */
  @Test
  void theFileReachesTheController() throws Exception {
    Path file = Files.createTempFile("oozx", ".json");
    Files.writeString(file, "{\"machine\": {\"plus3\": {\"detectSpeedlock\": true}}}");
    Speccy speccy = silentMachine(binder -> binder.bind(Configuration.class).toInstance(new Configuration(file.toFile())));
    assertTrue(((Upd765Peripheral) speccy.peripheralRegistry.find(Upd765Peripheral.class)).detectSpeedlock());
  }

  /**
   * Default drives must match the real +3: single-sided 40-track in A, double-sided 80-track
   * in B. A stub enumerator once returned 0 for both, which reads as "disabled" for B, silently
   * leaving every emulated +3 with only one drive.
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
