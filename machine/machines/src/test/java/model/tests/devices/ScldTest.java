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
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Tc2048;
import com.fpetrola.oozx.speccy.devices.scld.TimexMemoryPeripheral;
import com.fpetrola.oozx.speccy.modules.display.ScreenLayout;
import com.fpetrola.oozx.speccy.modules.memory.MemoryPart;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
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

  /**
   * Bit 2 makes a column two bytes instead of one: the second is the same address in the other
   * file, and the two are sixteen pixels of the same line rather than eight of twice the width.
   * A picture drawn this way reads no attribute at all, so the column is twice as wide.
   */
  @Test
  void theThirdBitMakesAColumnTwoBytesAndSixteenPixels() {
    on(Tc2048.class);
    assertEquals(8, speccy.picture.columnWidth(), "eight until something says otherwise");
    out(0xff, 0x04);
    assertEquals(16, speccy.picture.columnWidth(), "a column is worth sixteen now");
    out(0xff, 0x00);
    assertEquals(8, speccy.picture.columnWidth(), "and eight again");
  }

  /**
   * The colours of a picture that has no attributes come from the register itself: three bits pick
   * one of eight pairs, each an ink against its own opposite, and every one of them bright.
   */
  @Test
  void theColoursOfAWidePictureComeFromTheRegisterAndNotFromMemory() {
    on(Tc2048.class);
    poke(ScreenLayout.ATTRIBUTES, 0x07);

    out(0xff, 0x04);
    assertEquals(0x78, speccy.display.layout.pairOfColours & 0xff, "black on white, the first pair");
    out(0xff, 0x04 | 0x38);
    assertEquals(0x47, speccy.display.layout.pairOfColours & 0xff, "white on black, the last");
    assertEquals(0x07, speccy.display.attribute(0, 0) & 0xff, "and what is in memory was never asked");
  }

  /**
   * The bitmap can be read back, and it has to be: the machine's own start-up borrows the eight K
   * at the bottom, and the routine that gives it back reads what was there rather than remembering
   * it. Without the read-back it hands back a zero it never had and runs off into its own text.
   */
  @Test
  void theBitmapSaysWhatItIsHolding() {
    on(Tc2048.class);
    out(0xf4, 0x25);
    assertEquals(0x25, in(0xf4), "the port gives back the bitmap it was given");
  }

  private TimexMemoryPeripheral slots() {
    return (TimexMemoryPeripheral) speccy.peripheralRegistry.find(TimexMemoryPeripheral.class);
  }

  private MemoryPart[] eightChunksOf(int filler) {
    MemoryPart[] chunks = new MemoryPart[8];
    for (int chunk = 0; chunk < 8; chunk++) {
      Ram ram = new Ram(0x2000);
      java.util.Arrays.fill(ram.bytes, (byte) filler);
      chunks[chunk] = ram;
    }
    return chunks;
  }

  /**
   * Eight bits, one per eight K: where a bit is set the cartridge covers what the machine had
   * there, and where it is clear the machine's own memory shows through again. Nothing in between
   * moves, which is the whole point of the port being a bitmap rather than a number.
   */
  @Test
  void eachBitOfTheOtherPortCoversItsOwnEightKAndNothingElse() {
    on(Tc2048.class);
    slots().carrying(eightChunksOf(0x11), eightChunksOf(0x22));
    speccy.memory.poke(0x8000, (byte) 0x99);
    speccy.memory.poke(0xa000, (byte) 0x99);

    out(0xf4, 0x00);
    assertEquals(0x99, speccy.memory.peek(0x8000) & 0xff, "nothing paged, so the machine's own RAM");

    out(0xf4, 0x10);
    assertEquals(0x11, speccy.memory.peek(0x8000) & 0xff, "bit 4 covers 0x8000");
    assertEquals(0x99, speccy.memory.peek(0xa000) & 0xff, "and the eight K above it did not move");

    out(0xf4, 0x00);
    assertEquals(0x99, speccy.memory.peek(0x8000) & 0xff, "and back it comes");
  }

  /** Which of the two cartridges is paged is the top bit of the display register: one chip, both jobs. */
  @Test
  void theTopBitOfTheRegisterChoosesWhichCartridgeIsPaged() {
    on(Tc2048.class);
    slots().carrying(eightChunksOf(0x11), eightChunksOf(0x22));

    out(0xff, 0x00);
    out(0xf4, 0x10);
    assertEquals(0x11, speccy.memory.peek(0x8000) & 0xff, "the one in the slot");

    out(0xff, 0x80);
    assertEquals(0x22, speccy.memory.peek(0x8000) & 0xff, "the one behind it");
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
