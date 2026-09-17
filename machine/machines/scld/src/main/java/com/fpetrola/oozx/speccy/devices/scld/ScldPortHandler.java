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

package com.fpetrola.oozx.speccy.devices.scld;

import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Painting;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.display.ScreenLayout;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The one byte that decides what a Timex machine shows, written to and read back from port 0xff.
 * <p>
 * Its bottom three bits pick between the two display files and between colour by cell and colour
 * by line; the three above them are the pair of colours a hi-res picture is drawn in. A machine
 * with no such chip answers that port from the bus instead, which is why this is a device.
 */
@Singleton
public class ScldPortHandler extends DefaultPortHandler {
  /** The second display file rather than the first. */
  public static final int SECOND_FILE = 0x01;
  /** Colour a line at a time, taken from the other file. */
  public static final int COLOUR_PER_LINE = 0x02;
  /** Five hundred and twelve pixels across, in one pair of colours. */
  public static final int HI_RES = 0x04;
  /** Which of the eight pairs those colours are, in bits 3 to 5. */
  public static final int COLOUR_PAIR = 0x38;

  /**
   * The eight pairs, as the attribute byte that says the same thing: bright, and ink against its
   * own opposite. A picture drawn this way has no attributes in memory, so there is nothing for a
   * cell to clash with.
   */
  private static final byte[] PAIRS = {
      0x78, 0x71, 0x6a, 0x63, 0x5c, 0x55, 0x4e, 0x47
  };

  private final Display display;
  private final SpectrumMemory banks;
  private final java.util.List<Runnable> whenWritten = new java.util.ArrayList<>();
  private byte register;

  /** The one pair of colours the whole picture is drawn in while a column is two bytes wide. */
  private byte pairOfColours;

  @Inject
  public ScldPortHandler(Display display, SpectrumMemory banks) {
    super(true, true);
    this.display = display;
    this.banks = banks;
  }

  /**
   * How this chip paints when it is showing five hundred and twelve pixels across: two bytes of
   * the same line, one from each display file, in the one pair of colours the register names, and
   * no attribute read from memory at all, so nothing can clash.
   */
  /** A wide picture reads no attribute from memory, so nothing in it can flash. */
  private final Painting.Line wide = new Painting.Line() {
    public void paint(int y, int bits) {
      paintHiRes(y, bits);
    }

    public boolean cellsCanFlash() {
      return false;
    }
  };

  private void paintHiRes(int y, int bits) {
    byte[] screen = banks.shown().bytes;
    ScreenLayout layout = display.layout;
    Picture canvas = display.picture();
    int ink = canvas.palette[display.colouring.ink(pairOfColours) & 0xff];
    int paper = canvas.palette[display.colouring.paper(pairOfColours) & 0xff];
    int row = (y + Display.BORDER_HEIGHT) * Picture.STRIDE;
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      int wide = ((screen[layout.pixelsAt(y, x)] & 0xff) << 8) | (screen[layout.secondByteAt(y, x)] & 0xff);
      int at = row + (x + Display.BORDER_WIDTH_COLS) * 16;
      for (int i = 0; i < 16; i++) {
        canvas.pixels[at + i] = (wide & (0x8000 >> i)) != 0 ? ink : paper;
      }
    }
  }

  @Override
  public BusAnswer read(int port) {
    return BusAnswer.of(register & 0xff);
  }

  @Override
  public void write(int port, byte value) {
    if (value == register) return;
    display.screenChanging();
    register = value;
    ScreenLayout layout = display.layout;
    layout.showing((value & SECOND_FILE) != 0 ? ScreenLayout.SECOND_FILE : 0,
        (value & COLOUR_PER_LINE) != 0);
    boolean hiRes = (value & HI_RES) != 0;
    pairOfColours = PAIRS[(value & COLOUR_PAIR) >> 3];
    display.painting.line(hiRes ? wide : null);
    display.picture().columnWidth(hiRes ? 16 : 8);
    display.refreshAll();
    whenWritten.forEach(Runnable::run);
  }

  /** The top bit says which cartridge is paged, so whoever pages them has to hear about a write. */
  public void onWrite(Runnable listener) {
    whenWritten.add(listener);
  }

  public byte register() {
    return register;
  }

  public byte pairOfColours() {
    return pairOfColours;
  }

  public void reset() {
    write(0, (byte) 0);
  }
}
