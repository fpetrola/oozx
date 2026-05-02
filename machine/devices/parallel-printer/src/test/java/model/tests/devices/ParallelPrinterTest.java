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

import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParallelPrinterTest {

  @Test
  void twoStrobeEdgesCloseTogetherAreOneCharacter() {
    long[] now = {0};
    ParallelPrinter printer = new ParallelPrinter(() -> now[0]);
    printer.write('H');
    printer.strobe(true);
    now[0] += 40;
    printer.strobe(false);
    printer.write('i');
    now[0] += 500;
    printer.strobe(false);
    now[0] += 40;
    printer.strobe(true);
    assertEquals("Hi", printer.text());
  }

  @Test
  void aLoneEdgeLongAgoIsForgotten() {
    long[] now = {0};
    ParallelPrinter printer = new ParallelPrinter(() -> now[0]);
    printer.write('x');
    printer.strobe(true);
    now[0] += 20000;
    printer.write('y');
    printer.strobe(false);
    assertEquals("", printer.text(), "an edge ten thousand cycles after the last is a new first edge");
    now[0] += 40;
    printer.strobe(true);
    assertEquals("y", printer.text());
  }
}
