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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Tc2048;
import com.fpetrola.oozx.speccy.modules.display.ScreenLayout;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * What the chip that draws a Timex machine does, asked of the machine through the one port it has.
 * <p>
 * The screen is the same sixteen K of RAM and the same bitmap; what the SCLD decides is which half
 * of it is being shown and whether a colour covers a cell or a single line of one.
 */
class ScldTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  private void on(Class<?> model) {
    speccy.machine.select(speccy.machine.model((Class) model));
  }

  private void out(int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  private int in(int port) {
    return speccy.ports.read(port) & 0xff;
  }

  /** The address of the first byte of the top line, in the bank the screen is read from. */
  private static final int TOP_LINE = 0;

  private void poke(int offset, int value) {
    speccy.banks.shown().bytes[offset] = (byte) value;
  }

  @Test
  void theRegisterIsWhateverWasLastWrittenToIt() {
    on(Tc2048.class);
    out(0xff, 0x3a);
    assertEquals(0x3a, in(0xff), "the SCLD gives its register back");
    out(0xff, 0x00);
    assertEquals(0x00, in(0xff));
  }

  @Test
  void aMachineWithoutTheChipDoesNotAnswerThatPort() {
    on(Spec48.class);
    out(0xff, 0x3a);
    assertNotEquals(0x3a, in(0xff), "a 48K has nothing wired to 0xff");
  }

  /**
   * Bit 0 shows the other display file, which is the same sixteen K read eight K higher. Both
   * files are in the screen's own bank, so a program can draw into one while the other is shown.
   */
  @Test
  void theFirstBitShowsTheSecondDisplayFile() {
    on(Tc2048.class);
    poke(TOP_LINE, 0xaa);
    poke(ScreenLayout.SECOND_FILE + TOP_LINE, 0x55);

    out(0xff, 0x00);
    assertEquals(0xaa, speccy.display.pixels(0, 0), "the usual file");
    out(0xff, 0x01);
    assertEquals(0x55, speccy.display.pixels(0, 0), "the other one");
  }

  /**
   * Bit 1 says a colour covers one line of a cell instead of eight, and the colour of a byte is
   * then at that byte's own address in the other file. The bitmap does not move, so the same
   * picture gains eight times the colour down the screen.
   */
  @Test
  void theSecondBitColoursALineAtATimeFromTheOtherFile() {
    on(Tc2048.class);
    poke(TOP_LINE, 0xaa);
    poke(ScreenLayout.ATTRIBUTES, 0x07);
    poke(ScreenLayout.SECOND_FILE + TOP_LINE, 0x38);

    out(0xff, 0x00);
    assertEquals(0x07, speccy.display.attribute(0, 0), "a cell's colour, as anywhere else");
    out(0xff, 0x02);
    assertEquals(0x38, speccy.display.attribute(0, 0), "the line's own colour");
    assertEquals(0xaa, speccy.display.pixels(0, 0), "and the bitmap did not move");
  }

  /** Timex machines start their picture fifteen T-states before a Sinclair does. */
  @Test
  void thePictureStartsEarlierThanOnASinclair() {
    on(Tc2048.class);
    assertEquals(14321, speccy.machine.current.getTimings().frame().firstPixel());
    on(Spec48.class);
    assertEquals(14336, speccy.machine.current.getTimings().frame().firstPixel());
  }
}
