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

package model.tests.machine;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Pentagon1024;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.display.ScreenLayout;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A picture whose colours are the bitmap. One machine can read two banks at once and take a column
 * of eight pixels out of four bytes, two from each, and each byte is a colour for its left pixel
 * and another for its right - so every pixel is any of sixteen and nothing is an attribute.
 * <p>
 * Separate from {@code PagingTest}, which is about which page is where: this is about what is done
 * with two of them once they are both being read.
 */
class SixteenColoursTest extends MachineTest {
  /** The port the machine's second byte is written to, the one that says how to read the picture. */
  private static final int SECOND_PORT = 0xeff7;
  private static final int SIXTEEN_COLOURS = 0x01;
  private static final int BLUE = 1, RED = 2, WHITE = 7, BRIGHT_BLUE = 9, BRIGHT_WHITE = 15;

  private final Speccy speccy = silentMachine();

  SixteenColoursTest() {
    speccy.machine.select(speccy.machine.model(Pentagon1024.class));
    speccy.picture.active = true;
  }

  private void out(int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  private void into(int page, int offset, int value) {
    speccy.banks.ram(page).bytes[offset] = (byte) value;
  }

  /** Bytes put straight into a bank were not written through the bus, so nothing knows they moved. */
  private void paintEverything() {
    speccy.display.refreshAll();
    paintTheFrame();
  }

  private void paintTheFrame() {
    speccy.zxClock.setTStates(0);
    speccy.display.frame();
  }

  /** The colour of one pixel of the top-left column of the picture. */
  private int pixel(int which) {
    int x = Display.BORDER_WIDTH_COLS * 8 + which;
    int rgb = speccy.picture.pixels[Display.BORDER_HEIGHT * Picture.STRIDE + x];
    for (int colour = 0; colour < Picture.SINCLAIR.length; colour++) {
      if (Picture.SINCLAIR[colour] == rgb) return colour;
    }
    return -1;
  }

  /**
   * A byte is read the way an attribute is read: the low three bits and bit 6 are one colour, the
   * next three and bit 7 are the other, which is how sixteen fit in eight bits twice over.
   */
  @Test
  void aByteIsTwoPixelsOfAnyOfSixteenColours() {
    out(SECOND_PORT, SIXTEEN_COLOURS);
    into(4, 0, BLUE | (WHITE << 3));
    paintEverything();

    assertEquals(BLUE, pixel(0), "the low three bits, and bit 6 would make it bright");
    assertEquals(WHITE, pixel(1), "the next three, and bit 7 would make that one bright");

    into(4, 0, (BLUE | 0x40) | ((WHITE | 0x08) << 3));
    paintEverything();
    assertEquals(BRIGHT_BLUE, pixel(0), "bit 6 lifts the left one into the bright eight");
    assertEquals(BRIGHT_WHITE, pixel(1), "and bit 7 the right one");
  }

  /**
   * The four bytes of a column come from the two banks alternately and from both display files:
   * the bank below the shown one first, then the shown one, then the same two eight K higher.
   */
  @Test
  void theFourBytesOfAColumnComeFromTwoBanksAndTwoFiles() {
    out(SECOND_PORT, SIXTEEN_COLOURS);
    into(4, 0, RED);
    into(5, 0, BLUE);
    into(4, ScreenLayout.SECOND_FILE, WHITE);
    into(5, ScreenLayout.SECOND_FILE, BRIGHT_BLUE & 7);
    paintEverything();

    assertEquals(RED, pixel(0), "the bank below the shown one");
    assertEquals(BLUE, pixel(2), "then the shown one");
    assertEquals(WHITE, pixel(4), "then the other file of the bank below");
    assertEquals(BRIGHT_BLUE & 7, pixel(6), "and the other file of the shown one");
  }

  /** Writing into the second bank changes the picture, which it only can if it is being watched. */
  @Test
  void theBankBesideTheShownOneIsWatchedTooWhileItIsBeingRead() {
    out(SECOND_PORT, SIXTEEN_COLOURS);
    out(0x7ffd, 4);
    paintEverything();
    speccy.memory.write(0xc000, (byte) RED);
    paintTheFrame();

    assertEquals(RED, pixel(0), "what was written into the second bank was drawn");
  }

  /** Without the bit the machine draws what every other machine draws, out of one bank and an attribute. */
  @Test
  void withoutTheBitAColumnIsABitmapAndAnAttributeAgain() {
    out(SECOND_PORT, SIXTEEN_COLOURS);
    out(SECOND_PORT, 0x00);
    into(5, 0, 0x80);
    into(5, ScreenLayout.ATTRIBUTES, (RED << 3) | BLUE);
    paintEverything();

    assertEquals(BLUE, pixel(0), "the top bit of the bitmap, in ink");
    assertEquals(RED, pixel(1), "and the rest in paper");
  }
}
