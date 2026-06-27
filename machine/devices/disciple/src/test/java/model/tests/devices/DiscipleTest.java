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
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.disciple.DisciplePeripheral;
import com.fpetrola.oozx.speccy.devices.disk.Disk;
import com.fpetrola.oozx.speccy.devices.disk.WdFdc;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscipleTest extends MachineTest {

  private Speccy speccy() {
    Speccy speccy = silentMachine();
    speccy.machine.select(speccy.machine.model(Spec48.class));
    return speccy;
  }

  private DisciplePeripheral disciple(Speccy speccy) {
    return (DisciplePeripheral) speccy.peripheralRegistry.find(DisciplePeripheral.class);
  }

  private DisciplePeripheral aFortyEightWithADisciple(Speccy speccy) {
    DisciplePeripheral disciple = disciple(speccy);
    disciple.plugIn(true);
    assertTrue(speccy.peripheralRegistry.update());
    speccy.machine.reset(true);
    assertTrue(disciple.isAvailable(), "disciple.rom ships with the emulator and was not read");
    return disciple;
  }

  private int in(Speccy speccy, int port) {
    return speccy.ports.read(port) & 0xff;
  }

  private void out(Speccy speccy, int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  private void untilNotBusy(Speccy speccy) {
    for (int i = 0; i < 5000 && (in(speccy, 0x1b) & WdFdc.SR_BUSY) != 0; i++) {
      advance(speccy, 3500);
    }
    assertEquals(0, in(speccy, 0x1b) & WdFdc.SR_BUSY, "the controller is still busy");
  }

  @Test
  void itComesUpPagedInAndTheHalvesCanSwap() {
    Speccy speccy = speccy();
    DisciplePeripheral disciple = aFortyEightWithADisciple(speccy);
    assertTrue(disciple.isPaged(), "the DISCiPLE comes up paged in at a reset");
    assertEquals(0xD3, speccy.memory.peek(2) & 0xff, "the DISCiPLE ROM begins DI; XOR A; OUT (0x1f),A");
    speccy.memory.poke(0x2003, (byte) 0x99);
    assertEquals(0x99, speccy.memory.peek(0x2003) & 0xff, "the RAM is not at 0x2000");
    assertRamPages(speccy, 5, 2, 0);

    out(speccy, 0x7b, 0);
    assertTrue(disciple.isSwapped());
    assertEquals(0x99, speccy.memory.peek(0x0003) & 0xff, "swapped, the RAM is at the bottom");
    in(speccy, 0x7b);
    assertFalse(disciple.isSwapped());

    out(speccy, 0xbb, 0);
    assertFalse(disciple.isPaged());
    assertEquals(0x11, speccy.memory.peek(2) & 0xff, "the machine's ROM did not come back");
    in(speccy, 0xbb);
    assertTrue(disciple.isPaged());
  }

  @Test
  void itReadsASectorThroughItsOwnPorts() throws Exception {
    Speccy speccy = speccy();
    DisciplePeripheral disciple = aFortyEightWithADisciple(speccy);
    byte[] image = new byte[2 * 80 * 10 * 512];
    System.arraycopy("DISCIPLE".getBytes(StandardCharsets.US_ASCII), 0, image, 2 * 512, 8);
    disciple.insert(0, Disk.openBuffer("d.mgt", image));

    out(speccy, 0x1f, 0x01);                    // control register: drive 1, side 0
    out(speccy, 0x1b, 0x00);                    // WD command: RESTORE
    untilNotBusy(speccy);
    out(speccy, 0x5b, 0);
    out(speccy, 0x9b, 3);
    out(speccy, 0x1b, 0x80);                    // WD command: READ SECTOR
    byte[] data = new byte[512];
    int got = 0;
    for (int i = 0; i < 200000 && got < 512; i++) {
      int status = in(speccy, 0x1b);
      if ((status & WdFdc.SR_BUSY) == 0) break;
      if ((status & WdFdc.SR_IDX_DRQ) != 0) data[got++] = (byte) in(speccy, 0xdb); else advance(speccy, 100);
    }
    assertEquals(512, got);
    assertEquals("DISCIPLE", new String(data, 0, 8, StandardCharsets.US_ASCII));
  }

  @Test
  void itWasSoldForTheFortyEight() {
    Speccy speccy = speccy();
    DisciplePeripheral disciple = disciple(speccy);
    assertTrue(disciple.fitsOn(speccy.machine.model(Spec48.class)));
    assertFalse(disciple.fitsOn(speccy.machine.model(Spec128.class)));
    assertFalse(disciple.fitsOn(speccy.machine.model(SpecPlus3.class)));
  }


  /** A hard reset must zero this peripheral's own RAM. */
  @Test
  void aHardResetLeavesNothingInItsRam() {
    Speccy speccy = speccy();
    DisciplePeripheral disciple = aFortyEightWithADisciple(speccy);
    speccy.memory.poke(0x2003, (byte) 0x99);
    assertEquals(0x99, speccy.memory.peek(0x2003) & 0xff);

    speccy.machine.reset(true);
    assertTrue(disciple.isPaged(), "it did not come up paged, so what follows would be reading the machine's ROM");
    assertEquals(0, speccy.memory.peek(0x2003) & 0xff, "a hard reset did not clear its RAM");
  }
}
