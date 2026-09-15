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
import com.fpetrola.oozx.speccy.devices.spec256.Planes;
import com.fpetrola.oozx.speccy.devices.spec256.Spec256Peripheral;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.snapshot.Snapshots;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The screen of a game in 256 colours: eight pixels of a cell, a colour each, and whatever lies
 * under them where a pixel has no colour at all.
 */
class TheScreenOfAGameTest extends MachineTest {
  private static final int LINE = 40, COLUMN = 3;
  private static final int BACKGROUND_WIDTH = 320;

  @TempDir
  Path where;

  private final byte[] colours = new byte[Planes.LENGTH];
  private Speccy speccy;

  /** The eight colours of one address of the machine's RAM, leftmost pixel first. */
  private void colours(int address, int... fromTheLeft) {
    for (int pixel = 0; pixel < 8; pixel++) {
      colours[(address - Planes.RAM) * 8 + (7 - pixel)] = (byte) fromTheLeft[pixel];
    }
  }

  private byte[] background(int colour) {
    byte[] picture = new byte[BACKGROUND_WIDTH * 200];
    java.util.Arrays.fill(picture, (byte) colour);
    return picture;
  }

  private void started(byte[]... backgrounds) throws IOException {
    speccy = silentMachine();
    select(speccy, speccy.machine.model(Spec48.class));
    speccy.picture.active = true;
    Files.write(where.resolve("game.GFX"), colours);
    for (int which = 0; which < backgrounds.length; which++) {
      Files.write(where.resolve(String.format("game.B%02d", which)), backgrounds[which]);
    }
    Path snapshot = where.resolve("game.sna");
    byte[] sna = new byte[27 + 0xc000];
    sna[24] = 0x40;
    // A cell whose ink and paper differ, or the rule that hides a cleared one covers the game up.
    sna[27 + 0x1800 + (LINE / 8) * 32 + COLUMN] = 0x07;
    Files.write(snapshot, sna);
    Snapshots.of(speccy).load(snapshot.toString());
    speccy.loop.applyWhatWasDeferred();
    paint();
  }

  private void paint() {
    speccy.display.refreshAll();
    speccy.zxClock.setTStates(0);
    speccy.display.frame();
  }

  private int pixel(int column, int line, int within) {
    int x = (Display.BORDER_WIDTH_COLS + column) * 8 + within;
    int y = Display.BORDER_HEIGHT + line;
    return speccy.picture.pixels[y * Picture.STRIDE + x];
  }

  private int rgbOf(int colour) {
    return speccy.picture.palette[colour];
  }

  private Spec256Peripheral session() {
    return (Spec256Peripheral) speccy.peripheralRegistry.find(Spec256Peripheral.class);
  }

  /** Where the machine keeps the eight pixels of a cell, which is not anywhere in order. */
  private static int addressOf(int line, int column) {
    return Planes.RAM + ((line & 0xc0) << 5) + ((line & 0x07) << 8) + ((line & 0x38) << 2) + column;
  }

  @Test
  void eightPixelsOfACellAreEightColoursOfTheirOwn() throws IOException {
    colours(addressOf(LINE, COLUMN), 1, 2, 3, 4, 100, 101, 102, 103);
    started();

    for (int within = 0; within < 8; within++) {
      int expected = within < 4 ? within + 1 : 96 + within;
      assertEquals(rgbOf(expected), pixel(COLUMN, LINE, within),
          "pixel " + within + " is colour " + expected + ", with no attribute anywhere near it");
    }
  }

  @Test
  void whereAPixelHasNoColourTheBackgroundShowsThrough() throws IOException {
    colours(addressOf(LINE, COLUMN), 100, 0, 0, 0, 0, 0, 0, 0);
    started(background(77));

    assertEquals(rgbOf(100), pixel(COLUMN, LINE, 0), "a pixel with a colour keeps it");
    assertEquals(rgbOf(77), pixel(COLUMN, LINE, 1), "and one without shows what lies under the screen");
    assertEquals(1, session().backgrounds());
  }

  @Test
  void withNothingUnderneathAPixelWithNoColourIsTheFirstOne() throws IOException {
    colours(addressOf(LINE, COLUMN), 100, 0, 0, 0, 0, 0, 0, 0);
    started();

    assertEquals(rgbOf(0), pixel(COLUMN, LINE, 1), "which in this palette is black");
    assertEquals(0, session().backgrounds());
  }

  @Test
  void thereCanBeSeveralPicturesUnderTheScreenAndOneOfThemIsShowing() throws IOException {
    colours(addressOf(LINE, COLUMN), 0, 0, 0, 0, 0, 0, 0, 0);
    started(background(77), background(88));

    assertEquals(2, session().backgrounds());
    assertEquals(rgbOf(77), pixel(COLUMN, LINE, 0), "the first of them, to begin with");

    session().show(1);
    paint();
    assertEquals(rgbOf(88), pixel(COLUMN, LINE, 0), "and whichever one is asked for after that");
  }

  @Test
  void theBorderIsStillTheOneTheUlaPaints() throws IOException {
    colours(addressOf(LINE, COLUMN), 100, 100, 100, 100, 100, 100, 100, 100);
    started(background(77));

    int inTheBorder = speccy.picture.pixels[Display.BORDER_HEIGHT * Picture.STRIDE + 4];
    assertNotEquals(rgbOf(77), inTheBorder, "nothing of the game reaches past the screen");
    assertNotEquals(rgbOf(100), inTheBorder);
  }

  /**
   * A white shape is either a white shape the game drew or a shape whose colours never arrived,
   * and they look the same. This is the question that tells them apart.
   */
  @Test
  void theScreenSaysHowMuchOfItHasNoColoursOfItsOwn() throws IOException {
    colours(addressOf(LINE, COLUMN), 1, 2, 3, 4, 5, 6, 7, 8);
    started();

    assertEquals(0, session().cellsWithNoColours(),
        "one cell is painted in eight colours and the rest is blank, which is no shape rather than a shape without colours");
  }

  @Test
  void aShapeWithNoColoursAtAllIsWhatThatCountIsFor() throws IOException {
    // Eight planes all saying the same thing, which is how a byte that is no picture is written.
    for (int within = 0; within < 8; within++) {
      int at = addressOf(LINE - LINE % 8 + within, COLUMN);
      for (int pixel = 0; pixel < 8; pixel++) colours[(at - Planes.RAM) * 8 + (7 - pixel)] = (byte) 0xff;
    }
    started();

    assertEquals(1, session().cellsWithNoColours(), "one cell of white where a game drew something");
  }

  @Test
  void whenTheGameIsOverTheScreenIsTheMachinesAgain() throws IOException {
    colours(addressOf(LINE, COLUMN), 100, 100, 100, 100, 100, 100, 100, 100);
    started();
    speccy.banks.shown().bytes[speccy.display.layout.pixelsAt(LINE, COLUMN)] = (byte) 0xff;
    speccy.banks.shown().bytes[speccy.display.layout.colourAt(LINE, COLUMN)] = 7;

    speccy.machine.reset(true);
    speccy.loop.applyWhatWasDeferred();
    paint();

    assertEquals(Picture.SINCLAIR[7], pixel(COLUMN, LINE, 0), "the byte and its attribute, the way they always were");
  }
}
