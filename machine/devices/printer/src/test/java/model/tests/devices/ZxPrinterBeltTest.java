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

import com.fpetrola.oozx.speccy.devices.printer.Printout;
import com.fpetrola.oozx.speccy.devices.printer.ZxPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ZX Printer belt behaviour in isolation: no buffering, so dot position is purely a function of
 * elapsed time between port writes and the clock, testable with no emulator attached.
 */
class ZxPrinterBeltTest {

  private static final int FRAME = 69888;
  /** Fast-belt timing (bit 1 clear): 220 T-states/dot, 64 dots margin, 256 dots of paper. */
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
    write(0x80);            // bit 7 stylus down, bit 1 clear (fast), motor running
    after(64 + 256);        // clears the margin, then the full paper width
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

  /** Only the portion of the line where the stylus was down should be burned. */
  @Test
  void whereTheStylusGoesDownIsWhereTheDotsStart() {
    write(0x00);
    after(64 + 128);
    write(0x80);            // stylus down partway through the line
    after(128);
    write(0x80);

    boolean[] row = paper.row(0);
    assertFalse(row[0], "the left half should be blank");
    assertFalse(row[127], "the left half should be blank");
    assertTrue(row[128], "the right half should be burned");
    assertTrue(row[255], "the right half should be burned");
    assertEquals(128, dotsBurnedIn(row), "the dots did not land where the stylus went down");
  }

  /** Stopping the motor (bit 2) flushes whatever remains of the in-progress line. */
  @Test
  void stoppingFinishesTheLineInHand() {
    write(0x80);
    after(64 + 100);
    write(0x04);

    assertEquals(1, paper.height(), "stopping did not finish the line");
    assertEquals(256, dotsBurnedIn(paper.row(0)), "the rest of the line was not burned");
  }

  /** The position-feedback bit the ROM polls, indicating belt movement past the last dot. */
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
    after(2 * 384);         // 384 dots is one full belt revolution
    write(0x80);

    assertEquals(2, paper.height(), "a turn of the belt is not a line of paper");
  }
}
