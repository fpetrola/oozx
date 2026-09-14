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

import com.fpetrola.z80.memory.Memory;

/**
 * The colour of every pixel a game has, held as eight memories the shape of the one the game runs
 * in.
 * <p>
 * A file has the eight colours of an address side by side, the rightmost pixel first, and this is
 * the only place that order is known. Sliced the other way, bit <i>w</i> of plane <i>v</i> is bit
 * <i>v</i> of the colour of pixel <i>w</i>: a plane is then a memory a processor can run on, and
 * the colour of a pixel is one bit taken out of each of the eight.
 * <p>
 * A byte that is not a picture is written as eight planes all equal to it, so its pixels come out
 * as the last colour where the byte is set and the first where it is not, which is what an
 * uncoloured graphic looks like.
 */
public final class Planes {
  public static final int PLANES = 8;
  /** Where the machine's RAM starts, which is as far as a file's colours reach. */
  public static final int RAM = 0x4000;
  private static final int SIZE = 0x10000;
  /** What a file weighs: the colour of every pixel of the RAM of a 48K machine. */
  public static final int LENGTH = (SIZE - RAM) * PLANES;

  private final byte[][] bytes = new byte[PLANES][SIZE];

  private Planes() {
  }

  /** The colours of a whole game, as a file has them. */
  public static Planes of(byte[] file) {
    if (file.length != LENGTH) {
      throw new IllegalArgumentException("The colours of a 48K game are " + LENGTH + " bytes, and these are " + file.length);
    }
    Planes planes = new Planes();
    for (int address = RAM; address < SIZE; address++) {
      int at = (address - RAM) * PLANES;
      for (int fromTheRight = 0; fromTheRight < PLANES; fromTheRight++) {
        int colour = file[at + fromTheRight] & 0xff;
        for (int plane = 0; plane < PLANES; plane++) {
          if ((colour & (1 << plane)) != 0) planes.bytes[plane][address] |= (byte) (1 << fromTheRight);
        }
      }
    }
    return planes;
  }

  /** The colour of one pixel of the byte at an address, the leftmost being the first. */
  public int colourOf(int address, int pixel) {
    int bit = 1 << (7 - pixel);
    int colour = 0;
    for (int plane = 0; plane < PLANES; plane++) {
      if ((bytes[plane][address] & bit) != 0) colour |= 1 << plane;
    }
    return colour;
  }

  /**
   * One of the eight as a memory a processor runs on: whatever is read to be executed comes from
   * the machine, so that the processor on this plane can never decode an instruction other than
   * the one the machine is running, and everything else is this plane's own colours. Below the
   * RAM there are none, and the machine's byte is already what eight equal planes would say.
   */
  public Memory plane(int which, Memory machine) {
    byte[] mine = bytes[which];
    return new Memory() {
      public int read(int address, int fetching) {
        return fetching != 0 || address < RAM ? machine.peek(address) : mine[address] & 0xff;
      }

      public void write(int address, int value) {
        if (address >= RAM) mine[address] = (byte) value;
      }

      public int peek(int address) {
        return address < RAM ? machine.peek(address) : mine[address] & 0xff;
      }

      public void poke(int address, int value) {
        write(address, value);
      }

      public void reset() {
      }
    };
  }
}
