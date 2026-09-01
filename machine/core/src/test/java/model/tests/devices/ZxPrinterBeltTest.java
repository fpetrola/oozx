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

import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The belt of a ZX Printer, driven by hand.
 * <p>
 * There is no buffer in one of these: where a dot lands depends only on how much time passed
 * between the writes that turned the stylus on and off, so the printer is a function of the port
 * and the clock and can be tested without an emulator around it. What it must get right is that a
 * dot burned at a given moment lands where the belt was at that moment.
 */
class ZxPrinterBeltTest {

  private static final int FRAME = 69888;
  /** Bit 1 clear is the fast belt: 220 t-states a dot, 64 dots of margin, then 256 of paper. */
  private static final int TICKS_PER_DOT = 220;

  private long now;
  private Printout paper;
  private ZxPrinter printer;

  @BeforeEach
  void setUp() {
    now = 0;
    paper = new Printout();
    printer = new ZxPrinter(paper, () -> now, () -> FRAME);
  }

  private void after(int dots) {
    now += (long) dots * TICKS_PER_DOT;
  }

  private void write(int value) {
    printer.write((byte) value);
  }

  private int dotsBurnedIn(boolean[] row) {
    int burned = 0;
    for (boolean dot : row) {
      if (dot) burned++;
    }
    return burned;
  }

  @Test
  void aStylusHeldDownForAWholeLineBurnsAllOfIt() {
    write(0x80);            // motor on, fast, stylus down
    after(64 + 256);        // past the margin and across the paper
    write(0x80);

    assertEquals(1, paper.height(), "no line came out");
    assertEquals(256, dotsBurnedIn(paper.row(0)), "the line is not solid");
  }

  @Test
  void aStylusHeldUpLeavesTheLineBlank() {
    write(0x00);
    after(64 + 256);
    write(0x00);

    assertEquals(1, paper.height(), "the paper did not advance");
    assertEquals(0, dotsBurnedIn(paper.row(0)), "something was burned with the stylus up");
  }

  /** The half the stylus was down for is the half that comes out black. */
  @Test
  void whereTheStylusGoesDownIsWhereTheDotsStart() {
    write(0x00);
    after(64 + 128);
    write(0x80);            // stylus down, halfway across
    after(128);
    write(0x80);

    boolean[] row = paper.row(0);
    assertFalse(row[0], "the left half should be blank");
    assertFalse(row[127], "the left half should be blank");
    assertTrue(row[128], "the right half should be burned");
    assertTrue(row[255], "the right half should be burned");
    assertEquals(128, dotsBurnedIn(row), "the dots did not land where the stylus went down");
  }

  /** Bit 2 stops the motor, and whatever is left of the line comes out with it. */
  @Test
  void stoppingFinishesTheLineInHand() {
    write(0x80);
    after(64 + 100);
    write(0x04);

    assertEquals(1, paper.height(), "stopping did not finish the line");
    assertEquals(256, dotsBurnedIn(paper.row(0)), "the rest of the line was not burned");
  }

  /** The encoder the ROM spins on: it says the belt has moved past the dot it last wrote. */
  @Test
  void thePositionEncoderFollowsTheBelt() {
    assertEquals((byte) 0x3e, printer.read(), "a stopped printer should look stopped");

    write(0x80);
    after(64);
    byte moving = printer.read();
    assertTrue((moving & 0x01) != 0, "the encoder never moved");
    assertTrue((moving & 0x80) != 0, "the stylus is down and the printer says it is not");
  }

  @Test
  void aLineIsThreeHundredAndEightyFourDotsLongEvenThoughOnlyTwoFiftySixArePaper() {
    write(0x80);
    after(2 * 384);         // two full turns of the belt
    write(0x80);

    assertEquals(2, paper.height(), "a turn of the belt is not a line of paper");
  }
}
