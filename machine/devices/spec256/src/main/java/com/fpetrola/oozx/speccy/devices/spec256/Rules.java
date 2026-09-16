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


package com.fpetrola.oozx.speccy.devices.spec256;

import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/**
 * What a game says about how its colours meet the machine's own, in the file EmuZWin writes
 * beside it: lines of {@code key=value}, and this is the only place those keys are known.
 * <p>
 * Most games say nothing and the defaults are what they get. The defaults are not "do nothing":
 * the top sixty-four colours are mixed with the attribute the machine would have painted with,
 * which is what a game counts on when it wants a highlight to follow the colour of the room.
 */
@Singleton
public final class Rules {
  /** With a picture underneath, the last colour lets it through as the first one does. */
  public boolean backgroundOverTheLast;
  /** The first colour is the attribute's paper and the last one its ink, so an uncoloured graphic looks as it did. */
  public boolean paperForNoneInkForAll;
  /** Where a cell's ink and paper are the same, that colour covers it: it is how a game clears. */
  public boolean hiddenWhereInkIsPaper = true;
  /** How many colours from the top, and from the bottom, are mixed half and half with the attribute's. */
  public int mixedFromTheTop = 64;
  public int mixedFromTheBottom;
  /** Whether the mix uses the bright eight when the attribute says bright, or the plain ones anyway. */
  public boolean brightInTheMix;
  /** Whether the three logical instructions work on colours rather than on bits, in the followers. */
  public boolean levelledOr, levelledAnd, levelledXor;

  /**
   * Whether a follower reads where the machine reads. Only where a picture is: eight planes that
   * all say the same thing are a table, and a follower that looks one up with a colour of its own
   * is right to go where the machine did not - that is how a mirrored sprite keeps its colours.
   */
  public boolean readingWhereTheMachineReads = true;

  /** Which registers a follower takes from the machine before every instruction. */
  public String registersTaken = Alignment.BY_DEFAULT;

  public Rules() {
    asTheyComeByDefault();
  }

  public void asTheyComeByDefault() {
    backgroundOverTheLast = false;
    paperForNoneInkForAll = false;
    hiddenWhereInkIsPaper = true;
    mixedFromTheTop = 64;
    mixedFromTheBottom = 0;
    brightInTheMix = false;
    levelledOr = levelledAnd = levelledXor = false;
    readingWhereTheMachineReads = true;
    registersTaken = Alignment.BY_DEFAULT;
  }

  /**
   * How many colours a game says are mixed. A game that says none says nothing: measured against
   * the Renegade running in the emulator these rules come from, a file that says zero still has
   * the top sixty-four mixed, so zero is the key not being said rather than the answer being none.
   */
  private int saying(String value, int byDefault) {
    int said = number(value, byDefault);
    return said == 0 ? byDefault : said;
  }

  /** Whether a colour is one of those mixed with what the machine would have painted. */
  public boolean mixed(int colour) {
    return colour < mixedFromTheBottom || colour > 255 - mixedFromTheTop;
  }

  /**
   * A game's own file, if it brought one. Whatever it says that is not one of these is some other
   * emulator's business, and whatever it says badly is left at its default.
   */
  public void read(File said) {
    asTheyComeByDefault();
    if (said == null) return;
    try {
      for (String line : Files.readString(said.toPath(), StandardCharsets.ISO_8859_1).split("\\R")) {
        int equals = line.indexOf('=');
        if (equals > 0) says(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
      }
    } catch (IOException unreadable) {
      System.out.printf("oozx: %s is this game's rules but cannot be read: %s%n", said, unreadable.getMessage());
    }
  }

  private void says(String key, String value) {
    switch (key.toLowerCase(Locale.ROOT)) {
      case "bkoverff" -> backgroundOverTheLast = on(value);
      case "paper00inkff" -> paperForNoneInkForAll = on(value);
      case "hidesameinkpaper" -> hiddenWhereInkIsPaper = on(value);
      case "upcolorsmixed" -> mixedFromTheTop = saying(value, mixedFromTheTop);
      case "downcolorsmixed" -> mixedFromTheBottom = number(value, mixedFromTheBottom);
      case "usebrightinmix" -> brightInTheMix = on(value);
      case "gfxleveledor" -> levelledOr = on(value);
      case "gfxleveledand" -> levelledAnd = on(value);
      case "gfxleveledxor" -> levelledXor = on(value);
      case "zxpalignregs" -> registersTaken = value;
      default -> {
      }
    }
  }

  private static boolean on(String value) {
    return !value.isEmpty() && !"0".equals(value);
  }

  private static int number(String value, int otherwise) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException notANumber) {
      return otherwise;
    }
  }
}
