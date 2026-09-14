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
import com.fpetrola.oozx.speccy.devices.spec256.Alignment;
import com.fpetrola.oozx.speccy.devices.spec256.Planes;
import com.fpetrola.oozx.speccy.devices.spec256.Rules;
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
 * What a game's own file says about how its colours meet the machine's: a line of
 * {@code key=value} for each, and defaults that are not "leave everything alone".
 */
class TheRulesOfAGameTest extends MachineTest {
  private static final int LINE = 40, COLUMN = 3;
  private static final int INK = 7, PAPER = 1;

  @TempDir
  Path where;

  private final byte[] colours = new byte[Planes.LENGTH];
  private byte attribute = (byte) (INK | (PAPER << 3));
  private Speccy speccy;

  private void colours(int address, int... fromTheLeft) {
    for (int pixel = 0; pixel < 8; pixel++) {
      colours[(address - Planes.RAM) * 8 + (7 - pixel)] = (byte) fromTheLeft[pixel];
    }
  }

  private static int addressOf(int line, int column) {
    return Planes.RAM + ((line & 0xc0) << 5) + ((line & 0x07) << 8) + ((line & 0x38) << 2) + column;
  }

  private void started(String said, byte[] background) throws IOException {
    speccy = silentMachine();
    select(speccy, speccy.machine.model(Spec48.class));
    speccy.picture.active = true;
    Files.write(where.resolve("game.GFX"), colours);
    if (said != null) Files.write(where.resolve("game.CFG"), said.getBytes());
    if (background != null) Files.write(where.resolve("game.B00"), background);
    byte[] sna = new byte[27 + 0xc000];
    sna[24] = 0x40;
    sna[27 + 0x1800 + (LINE / 8) * 32 + COLUMN] = attribute;
    // The byte the machine itself shows, which is what says whether a pixel takes ink or paper.
    sna[27 + addressOf(LINE, COLUMN) - Planes.RAM] = (byte) 0b10000000;
    Path snapshot = where.resolve("game.sna");
    Files.write(snapshot, sna);
    Snapshots.of(speccy).load(snapshot.toString());
    speccy.loop.applyWhatWasDeferred();
    speccy.display.refreshAll();
    speccy.zxClock.setTStates(0);
    speccy.display.frame();
  }

  private int pixel(int within) {
    int x = (Display.BORDER_WIDTH_COLS + COLUMN) * 8 + within;
    return speccy.picture.pixels[(Display.BORDER_HEIGHT + LINE) * Picture.STRIDE + x];
  }

  private int rgbOf(int colour) {
    return speccy.picture.palette[colour];
  }

  private static int halfway(int one, int other) {
    return ((one >> 16 & 0xff) + (other >> 16 & 0xff)) / 2 << 16
        | ((one >> 8 & 0xff) + (other >> 8 & 0xff)) / 2 << 8
        | ((one & 0xff) + (other & 0xff)) / 2;
  }

  private byte[] background(int colour) {
    byte[] picture = new byte[320 * 200];
    java.util.Arrays.fill(picture, (byte) colour);
    return picture;
  }

  private Spec256Peripheral session() {
    return (Spec256Peripheral) speccy.peripheralRegistry.find(Spec256Peripheral.class);
  }

  private Rules rules() {
    return session().rules();
  }

  @Test
  void theTopSixtyFourColoursAreMixedWithWhatTheMachineWouldHavePaintedThere() throws IOException {
    colours(addressOf(LINE, COLUMN), 200, 200, 191, 0, 0, 0, 0, 0);
    started(null, null);

    assertEquals(halfway(Picture.SINCLAIR[INK], rgbOf(200)), pixel(0),
        "the pixel is set, so colour 200 meets the cell's ink halfway");
    assertEquals(halfway(Picture.SINCLAIR[PAPER], rgbOf(200)), pixel(1),
        "and beside it, where the machine's own byte is clear, it meets the paper");
    assertEquals(rgbOf(191), pixel(2), "one colour lower and nothing is mixed at all");
  }

  @Test
  void aGameCanSayHowManyOfThemAreMixed() throws IOException {
    colours(addressOf(LINE, COLUMN), 200, 0, 0, 0, 0, 0, 0, 0);
    started("UpColorsMixed=0\n", null);

    assertEquals(rgbOf(200), pixel(0), "none of them, and the colour is its own");
  }

  @Test
  void whereACellsInkAndItsPaperAreTheSameTheGameIsCoveredOver() throws IOException {
    attribute = 0x00;
    colours(addressOf(LINE, COLUMN), 100, 100, 100, 100, 100, 100, 100, 100);
    started(null, null);

    assertEquals(Picture.SINCLAIR[0], pixel(0), "which is how a game clears a part of the screen");
  }

  @Test
  void aGameCanSayThatItDoesNotClearThatWay() throws IOException {
    attribute = 0x00;
    colours(addressOf(LINE, COLUMN), 100, 100, 100, 100, 100, 100, 100, 100);
    started("HideSameInkPaper=0\n", null);

    assertEquals(rgbOf(100), pixel(0));
  }

  @Test
  void aGameCanAskForItsFirstColourToBeThePaperAndItsLastTheInk() throws IOException {
    colours(addressOf(LINE, COLUMN), 255, 0, 0, 0, 0, 0, 0, 0);
    started("Paper00InkFF=1\nUpColorsMixed=0\n", null);

    assertEquals(Picture.SINCLAIR[INK], pixel(0), "the last colour is the cell's ink");
    assertEquals(Picture.SINCLAIR[PAPER], pixel(1), "and the first one its paper");
  }

  @Test
  void withAPictureUnderneathTheFirstColourLetsItThroughAndSoCanTheLast() throws IOException {
    colours(addressOf(LINE, COLUMN), 255, 0, 100, 0, 0, 0, 0, 0);
    started("BkOverFF=1\nUpColorsMixed=0\n", background(77));

    assertEquals(rgbOf(77), pixel(0), "the last colour was told to let the picture through");
    assertEquals(rgbOf(77), pixel(1), "and the first one always does");
    assertEquals(rgbOf(100), pixel(2), "anything else covers it");
  }

  @Test
  void withoutThatRuleTheLastColourCoversThePictureLikeAnyOther() throws IOException {
    colours(addressOf(LINE, COLUMN), 255, 0, 0, 0, 0, 0, 0, 0);
    started("UpColorsMixed=0\n", background(77));

    assertEquals(rgbOf(255), pixel(0));
    assertEquals(rgbOf(77), pixel(1));
  }

  @Test
  void aGameSaysWhichRegistersItsFollowersTakeInTheSameFile() throws IOException {
    colours(addressOf(LINE, COLUMN), 0, 0, 0, 0, 0, 0, 0, 0);
    started("zxpAlignRegs=1PSsHL\n", null);

    assertEquals("1PSsHL", session().alignment().said());
  }

  @Test
  void whenTheGameIsOverItsRulesAreNobodysAnyMore() throws IOException {
    colours(addressOf(LINE, COLUMN), 0, 0, 0, 0, 0, 0, 0, 0);
    started("BkOverFF=1\nUpColorsMixed=0\nzxpAlignRegs=1PSsHL\n", null);
    assertTrue(rules().backgroundOverTheLast);

    speccy.machine.reset(true);
    speccy.loop.applyWhatWasDeferred();

    assertFalse(rules().backgroundOverTheLast, "the next game gets the defaults, not the last one's");
    assertEquals(64, rules().mixedFromTheTop);
    assertEquals(Alignment.BY_DEFAULT, session().alignment().said());
  }

  @Test
  void aGameSaysInTheSameFileWhetherItsLogicalInstructionsWorkOnLevels() throws IOException {
    colours(addressOf(LINE, COLUMN), 0, 0, 0, 0, 0, 0, 0, 0);
    started("GFXLeveledOR=1\nGFXLeveledAND=1\n", null);

    assertTrue(rules().levelledOr);
    assertTrue(rules().levelledAnd);
    assertFalse(rules().levelledXor, "and the one it did not name is left alone");
  }

  @Test
  void aKeyNobodyKnowsIsSomeOtherEmulatorsBusiness() throws IOException {
    colours(addressOf(LINE, COLUMN), 100, 0, 0, 0, 0, 0, 0, 0);
    started("GFXScreenXORbuffered=1\nOrderPaletteSignedBytes=1\nUpColorsMixed=0\n", null);

    assertEquals(rgbOf(100), pixel(0), "and the game is painted anyway");
  }
}
