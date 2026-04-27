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
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterPeripheral;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end printer check: the real 48K ROM's COPY routine drives the stylus by polling the
 * position encoder, so only correct belt timing produces a legible image - a self-consistent
 * but wrong implementation would print garbage, which unit-testing the arithmetic alone misses.
 */
class RomPrintsToThePrinterTest extends MachineTest {

  private static final int COPY = 0x0eac;      // entry point of the 48K ROM's COPY routine
  private static final int SCREEN = 0x4000;

  private Speccy speccy() {
    Speccy speccy = silentMachine();
    ((ZxPrinterPeripheral) speccy.peripheralRegistry.find(ZxPrinterPeripheral.class)).plugIn(true);
    return speccy;
  }

  @Test
  void copyPrintsTheScreen() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.machine.model(Spec48.class));
    // 128, not the previously-used 60: a clock-wrap counting bug had undercounted boot frames.
    runFrames(speccy, 128);

    // Fills the top character row solid, so a blank printout would clearly indicate failure.
    for (int address = SCREEN; address < SCREEN + 256; address++) {
      speccy.cpu.getOoz80().getState().getMemory().write(address, (byte) 0xff);
    }

    Printout paper = ((ZxPrinterPeripheral) speccy.peripheralRegistry.find(ZxPrinterPeripheral.class)).paper();
    speccy.cpu.getOoz80().getState().getPc().write(COPY);

    for (int frame = 0; frame < 400 && paper.height() < 9; frame++) {
      runFrames(speccy, 1);
    }

    assertTrue(paper.height() >= 9,
        "COPY printed " + paper.height() + " rows; the ROM drives the printer over the position "
            + "encoder, so nothing coming out means it never saw the belt move");

    // The Spectrum's interleaved screen layout means those 256 bytes are only the top pixel
    // row of each of 8 character rows, so the expected printout is solid/blank*7/solid.
    assertEquals(255, dotsIn(paper.row(0)), "the first line of the screen did not come out solid");
    for (int row = 1; row < 8; row++) {
      assertEquals(0, dotsIn(paper.row(row)), "row " + row + " should be blank paper");
    }
    assertEquals(255, dotsIn(paper.row(8)), "the next character row did not come out solid");
  }

  /** Real COPY output is 255 dots, not 256: the ROM lifts the stylus exactly on the final dot. */
  private int dotsIn(boolean[] row) {
    int on = 0;
    for (boolean dot : row) {
      if (dot) on++;
    }
    return on;
  }
}
