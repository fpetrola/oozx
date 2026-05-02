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
