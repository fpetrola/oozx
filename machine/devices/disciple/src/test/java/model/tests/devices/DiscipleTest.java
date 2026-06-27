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

    out(speccy, 0x1f, 0x01);                    // drive 1, side 0
    out(speccy, 0x1b, 0x00);                    // restore
    untilNotBusy(speccy);
    out(speccy, 0x5b, 0);
    out(speccy, 0x9b, 3);
    out(speccy, 0x1b, 0x80);                    // read sector 3
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


  /** Its RAM is RAM: the machine going off and on again leaves nothing in it. */
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
