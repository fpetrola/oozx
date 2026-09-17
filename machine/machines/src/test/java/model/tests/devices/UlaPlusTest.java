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
import com.fpetrola.oozx.speccy.devices.ulaplus.UlaPlusPeripheral;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Sixty-four colours a program says for itself. Two ports do all of it: one names a register and
 * the other reads and writes it, sixty-four of those registers being the colours and one more
 * saying whether the machine is painting in them at all.
 * <p>
 * While it is, the two bits that were bright and flash are not those any more: together they name
 * one of four tables of sixteen, the ink is the low eight of that table and the paper the high
 * eight, and nothing flashes because there is no bit left saying it should.
 */
class UlaPlusTest extends MachineTest {
  private static final int REGISTER = 0xbf3b, DATA = 0xff3b;
  private static final int MODE = 0x40, ON = 0x01;

  private final Speccy speccy = silentMachine();

  private Speccy modified(boolean fitted) {
    ((UlaPlusPeripheral) speccy.peripheralRegistry.find(UlaPlusPeripheral.class)).setFitted(fitted);
    speccy.machine.selectDefault();
    return speccy;
  }

  private void out(int port, int value) {
    speccy.ports.write(port, (byte) value);
  }

  private int in(int port) {
    return speccy.ports.read(port) & 0xff;
  }

  private void set(int register, int value) {
    out(REGISTER, register);
    out(DATA, value);
  }

  private void painting() {
    set(MODE, ON);
  }

  private byte ink(int attribute) {
    return speccy.display.colouring.ink((byte) attribute);
  }

  private byte paper(int attribute) {
    return speccy.display.colouring.paper((byte) attribute);
  }

  @Test
  void oneRegisterNamesAColourAndTheOtherPortWritesIt() {
    modified(true);
    set(5, 0xa7);

    assertEquals(0xa7, in(DATA), "the colour it was given back");
    out(REGISTER, 6);
    assertEquals(0x00, in(DATA), "and the one beside it was not touched");
  }

  @Test
  void theRegisterThatIsNotAColourSaysWhetherTheseAreTheColours() {
    modified(true);
    painting();

    assertEquals(ON, in(DATA), "the mode register reads back rather than a colour");
  }

  /**
   * A colour is three bits of green, three of red and two of blue. The blue is short of a bit and
   * the missing one is the other two together, so the darkest blue is black and the brightest is
   * the whole of it.
   */
  @Test
  void aColourIsThreeBitsOfGreenThreeOfRedAndTwoOfBlue() {
    modified(true);
    painting();

    set(0, 0xe0);
    assertEquals(0x00ff00, speccy.picture.palette[0], "all the green");
    set(1, 0x1c);
    assertEquals(0xff0000, speccy.picture.palette[1], "all the red");
    set(2, 0x03);
    assertEquals(0x0000ff, speccy.picture.palette[2], "all the blue, which is two bits that reach the top");
    set(3, 0x01);
    assertEquals(0x00006d, speccy.picture.palette[3], "and the low bit of blue is worth three eighths, not one");
  }

  /**
   * The byte over a cell stops meaning bright and flash: those two name one of four tables, and
   * each table is sixteen colours with the inks first and the papers after them.
   */
  @Test
  void whileItIsPaintingAByteNamesOneOfSixtyFour() {
    modified(true);
    painting();

    assertEquals(3, ink(0x03), "ink three of the first table");
    assertEquals(8 + 2, paper(0x02 << 3), "paper two, which is the ninth of that table");
    assertEquals(16 + 3, ink(0x40 | 0x03), "what was bright names the second table");
    assertEquals(32 + 3, ink((byte) 0x80 | 0x03), "what was flash names the third");
    assertEquals(48 + 8 + 2, paper((byte) 0xc0 | (0x02 << 3)), "and the two together the fourth");
  }

  /** Nothing flashes while these are the colours: the bit that said so is naming a table. */
  @Test
  void nothingFlashesWhileItIsPainting() {
    modified(true);
    painting();
    speccy.display.colouring.reversed(true);

    assertEquals(32 + 1, ink((byte) 0x80 | 0x01), "the ink stayed the ink");
    assertEquals(32 + 8 + 2, paper((byte) 0x80 | (0x02 << 3)), "and the paper the paper");
  }

  @Test
  void turningItOffPutsTheSixteenBack() {
    modified(true);
    painting();
    set(0, 0xe0);
    set(MODE, 0x00);

    assertArrayEquals(new int[]{Picture.SINCLAIR[0]}, new int[]{speccy.picture.palette[0]}, "the first is black again");
    assertEquals(1, ink(0x01), "and a byte is read the way it always was");
    assertEquals(8 + 1, ink(0x41), "with the bright bit lifting the ink again");
  }

  @Test
  void aMachineNobodyModifiedDoesNotAnswerThosePorts() {
    modified(false);
    set(5, 0xa7);

    assertNotEquals(0xa7, in(DATA), "a machine without it answered its data port");
    assertEquals(1, ink(0x01), "and its colours are the sixteen");
  }

  /**
   * The border is a cell with nothing in it but paper, so it is painted out of the same table and
   * changes with it. It used to be painted straight out of the sixteen, which left a picture in
   * these colours framed in colours from somewhere else.
   */
  @Test
  void theBorderIsPaintedOutOfTheSameTable() {
    modified(true);
    speccy.picture.active = true;
    painting();
    // Paper two of the first table, which is where a border of colour two reads its colour from.
    set(8 + 2, 0xe0);
    speccy.ports.write(0xfe, (byte) 0x02);
    speccy.zxClock.setTStates(0);
    speccy.display.frame();

    assertEquals(0x00ff00, speccy.picture.pixels[5 * Picture.STRIDE + 8],
        "the border was not painted in the colour this machine was told that colour is");
  }

  /**
   * A snapshot is a machine as it stood, and one taken of a machine painting in colours of its own
   * comes back painting in them. Which they were has been written in those files and read out of
   * them for a long time; until there was a chip to put them into there was nowhere to put them.
   */
  @Test
  void theColoursASnapshotWasTakenInComeBackWithIt() {
    modified(false);
    com.fpetrola.oozx.speccy.modules.display.ColoursOfItsOwn chip =
        speccy.peripheralRegistry.anyThatIs(com.fpetrola.oozx.speccy.modules.display.ColoursOfItsOwn.class);
    int[] sixtyFour = new int[UlaPlusPeripheral.COLOURS];
    sixtyFour[3] = 0xe0;

    com.fpetrola.emulation.helpers.snapshots.SpectrumState snapshot =
        new com.fpetrola.emulation.helpers.snapshots.SpectrumState();
    snapshot.setULAPlusEnabled(true);
    snapshot.setULAPlusActive(true);
    snapshot.setULAPlusPalette(sixtyFour);
    chip.fitted(snapshot.isULAPlusEnabled());
    speccy.peripheralRegistry.update();
    chip.asItWas(snapshot.getULAPlusPalette(), snapshot.isULAPlusActive());

    assertEquals(0x00ff00, speccy.picture.palette[3], "the colour it was taken in");
    assertEquals(3, ink(0x03), "and it is painting in them");
  }

  /** A machine switched on is painting in the sixteen it was born with, whatever it was doing before. */
  @Test
  void switchingTheMachineOnPutsTheSixteenBack() {
    modified(true);
    painting();
    set(0, 0xe0);

    speccy.machine.reset(true);

    assertEquals(Picture.SINCLAIR[0], speccy.picture.palette[0]);
    assertEquals(1, ink(0x01));
  }
}
