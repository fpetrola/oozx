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

import model.harness.MachineTest;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.devices.opus.OpusPeripheral;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpusTest extends MachineTest {

  private static File pretendRom() throws IOException {
    byte[] image = new byte[OpusPeripheral.ROM_SIZE];
    image[0] = (byte) 0xAA;
    File file = File.createTempFile("opus", ".rom");
    file.deleteOnExit();
    Files.write(file.toPath(), image);
    return file;
  }

  private Speccy speccy() {
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(Spec48.class));
    return speccy;
  }

  private OpusPeripheral aFortyEightWithAnOpus(Speccy speccy) throws IOException {
    speccy.roms.choose("OpusPeripheral", pretendRom().getPath());
    OpusPeripheral opus = (OpusPeripheral) speccy.peripheralRegistry.find(OpusPeripheral.class);
    opus.plugIn(true);
    assertTrue(speccy.peripheralRegistry.update());
    speccy.machine.reset(true);
    assertTrue(opus.isAvailable());
    return opus;
  }

  private int peek(Speccy speccy, int address) {
    return speccy.memory.peek(address) & 0xff;
  }

  private void poke(Speccy speccy, int address, int value) {
    speccy.memory.poke(address, (byte) value);
  }

  @Test
  void itPagesInAfterTheInstructionAtItsHooksAndOutAt0x1748() throws IOException {
    Speccy speccy = speccy();
    OpusPeripheral opus = aFortyEightWithAnOpus(speccy);
    assertFalse(opus.isPaged());
    speccy.cpu.jump(0x0008);
    speccy.cpu.step();
    assertTrue(opus.isPaged(), "0x0008 did not page the Opus in");
    assertEquals(0xAA, peek(speccy, 0));
    poke(speccy, 0x2001, 0x5a);
    assertEquals(0x5a, peek(speccy, 0x2001), "its RAM is not at 0x2000");
    assertRamPages(speccy, 5, 2, 0);
    speccy.cpu.jump(0x1748);
    speccy.cpu.step();
    assertFalse(opus.isPaged(), "0x1748 did not page it out");
    assertEquals(0xF3, peek(speccy, 0));
  }

  @Test
  void theControllerIsReadThroughMemoryAndEveryByteKnocksOnNmi() throws Exception {
    Speccy speccy = speccy();
    OpusPeripheral opus = aFortyEightWithAnOpus(speccy);
    byte[] image = new byte[80 * 18 * 256];
    System.arraycopy("OPUSDISK".getBytes(StandardCharsets.US_ASCII), 0, image, 3 * 256, 8);
    opus.insert(0, Disk.openBuffer("test.opd", image));
    speccy.cpu.jump(0x0008);
    speccy.cpu.step();

    poke(speccy, 0x3001, 0x04);                 // PIA CRA: sets port A to data mode
    poke(speccy, 0x3000, 0x00);                 // PIA port A: drive 1, side 0
    poke(speccy, 0x2800, 0x08);                 // WD command: RESTORE with head load
    for (int i = 0; i < 5000 && (peek(speccy, 0x2800) & WdFdc.SR_BUSY) != 0; i++) advance(speccy, 3500);
    assertEquals(0, peek(speccy, 0x2800) & WdFdc.SR_BUSY);

    int nmisBefore = nmis(speccy);
    poke(speccy, 0x2801, 0);                    // WD track register
    poke(speccy, 0x2802, 3);                    // WD sector register
    poke(speccy, 0x2800, 0x80);                 // WD command: READ SECTOR
    byte[] data = new byte[256];
    int got = 0;
    for (int i = 0; i < 200000 && got < 256; i++) {
      int status = peek(speccy, 0x2800);
      if ((status & WdFdc.SR_BUSY) == 0) break;
      if ((status & WdFdc.SR_IDX_DRQ) != 0) {
        data[got++] = (byte) peek(speccy, 0x2803);
      }
      advance(speccy, 120);
    }
    assertEquals(256, got, "the sector did not come out whole");
    assertEquals("OPUSDISK", new String(data, 0, 8, StandardCharsets.US_ASCII));
    assertTrue(nmis(speccy) - nmisBefore >= 256, "the Opus pulls /NMI for every byte, and did not");
  }

  private int nmiCount;

  /** Running NMI count, incremented via the CPU's own NMI listener callback. */
  private int nmis(Speccy speccy) {
    if (!counting) {
      counting = true;
      speccy.cpu.onNmi(() -> nmiCount++);
    }
    return nmiCount;
  }

  private boolean counting;

  @Test
  void itGoesOnTheSinclairMachines() {
    Speccy speccy = speccy();
    OpusPeripheral opus = (OpusPeripheral) speccy.peripheralRegistry.find(OpusPeripheral.class);
    assertTrue(opus.fitsOn(speccy.machine.model(Spec48.class)));
    assertTrue(opus.fitsOn(speccy.machine.model(Spec128.class)));
    assertFalse(opus.fitsOn(speccy.machine.model(SpecPlus3.class)));
  }


  /** A hard reset must zero this peripheral's RAM. */
  @Test
  void aHardResetLeavesNothingInItsRam() throws IOException {
    Speccy speccy = speccy();
    OpusPeripheral opus = aFortyEightWithAnOpus(speccy);
    speccy.cpu.jump(0x0008);
    speccy.cpu.step();
    poke(speccy, 0x2001, 0x5a);
    assertEquals(0x5a, peek(speccy, 0x2001));

    speccy.machine.reset(true);
    speccy.cpu.jump(0x0008);
    speccy.cpu.step();
    assertTrue(opus.isPaged(), "it did not page back in, so what follows would be reading the machine's ROM");
    assertEquals(0, peek(speccy, 0x2001), "a hard reset did not clear its RAM");
  }
}
