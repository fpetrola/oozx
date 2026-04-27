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
