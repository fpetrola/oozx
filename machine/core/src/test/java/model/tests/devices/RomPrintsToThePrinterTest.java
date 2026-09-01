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
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinterPeripheral;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The test the whole printer is for: the 48K ROM's own COPY routine driving it.
 * <p>
 * COPY is a real printer driver written for real hardware - it turns the stylus on and off while
 * spinning on the position encoder, so it only produces a picture if the timing answers as the
 * belt would. Anything self-consistent but wrong prints stripes or nothing at all, and no amount
 * of unit testing the arithmetic against itself would say so.
 */
class RomPrintsToThePrinterTest {

  private static final int COPY = 0x0eac;      // the COPY command routine in the 48K ROM
  private static final int SCREEN = 0x4000;

  private Speccy speccy() {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    ((ZxPrinterPeripheral) speccy.peripherals.find(ZxPrinterPeripheral.class)).plugIn(true);
    return speccy;
  }

  private void runFrames(Speccy speccy, int frames) {
    long previous = speccy.zxClock.getTStates();
    int seen = 0;
    while (seen < frames) {
      speccy.z80.doOpcodes();
      speccy.eventManager.eventDoEvents();
      long now = speccy.zxClock.getTStates();
      if (now < previous) seen++;
      previous = now;
    }
  }

  @Test
  void copyPrintsTheScreen() {
    Speccy speccy = speccy();
    speccy.machine.select(speccy.spec48);
    runFrames(speccy, 60);

    // Something to print: the top character row solid, so a correct printout cannot be blank.
    for (int address = SCREEN; address < SCREEN + 256; address++) {
      speccy.z80.ooz80.getState().getMemory().write(address, (byte) 0xff);
    }

    Printout paper = ((ZxPrinterPeripheral) speccy.peripherals.find(ZxPrinterPeripheral.class)).paper();
    speccy.z80.ooz80.getState().getPc().write(COPY);

    for (int frame = 0; frame < 400 && paper.height() < 9; frame++) {
      runFrames(speccy, 1);
    }

    assertTrue(paper.height() >= 9,
        "COPY printed " + paper.height() + " rows; the ROM drives the printer over the position "
            + "encoder, so nothing coming out means it never saw the belt move");

    // The screen is interleaved: those 256 bytes are the first pixel line of each of the top
    // eight character rows, so a correct printout is a solid line, seven blank, and solid again.
    // Nothing but the right geometry and the right timing produces that shape.
    assertEquals(255, dotsIn(paper.row(0)), "the first line of the screen did not come out solid");
    for (int row = 1; row < 8; row++) {
      assertEquals(0, dotsIn(paper.row(row)), "row " + row + " should be blank paper");
    }
    assertEquals(255, dotsIn(paper.row(8)), "the next character row did not come out solid");
  }

  /**
   * 255 and not 256: the ROM lifts the stylus exactly at the last dot, so that one is left blank -
   * which is what the hardware does and what Fuse's arithmetic produces from the same writes.
   */
  private int dotsIn(boolean[] row) {
    int on = 0;
    for (boolean dot : row) {
      if (dot) on++;
    }
    return on;
  }
}
