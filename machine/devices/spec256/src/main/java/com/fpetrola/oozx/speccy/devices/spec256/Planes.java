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
import com.google.inject.Singleton;

import java.util.Arrays;

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
@Singleton
public final class Planes {
  public static final int PLANES = 8;
  /** Where the machine's RAM starts, which is as far as a file's colours reach. */
  public static final int RAM = 0x4000;
  private static final int SIZE = 0x10000;
  /** What a file weighs: the colour of every pixel of the RAM of a 48K machine. */
  public static final int LENGTH = (SIZE - RAM) * PLANES;
  /** And what the one for the ROM weighs, where a game colours the letters the machine draws with. */
  public static final int ROM_LENGTH = RAM * PLANES;

  /** The most memory one instruction writes, which is the two bytes of a call, and room to spare. */
  private static final int MOST_WRITES = 4;
  private byte[][] bytes;
  private boolean romColoured;
  private final int[] wroteAt = new int[MOST_WRITES];
  private int wrote;
  private final int[][] heldAt = new int[PLANES][MOST_WRITES], held = new int[PLANES][MOST_WRITES];
  private final int[] holding = new int[PLANES];
  private boolean writingWhereTheMachineWrote = true;

  /** The colours of a whole game, as a file has them. */
  public static Planes of(byte[] file) {
    Planes planes = new Planes();
    planes.take(file);
    return planes;
  }

  /** The colours of a whole game, over whatever was here before. */
  public void take(byte[] file) {
    if (file.length != LENGTH) {
      throw new IllegalArgumentException("The colours of a 48K game are " + LENGTH + " bytes, and these are " + file.length);
    }
    blank();
    slice(file, RAM, SIZE);
  }

  /** The eight colours of each address of a stretch, as a file has them, cut the other way. */
  private void slice(byte[] file, int from, int to) {
    byte[][] planes = bytes();
    for (int address = from; address < to; address++) {
      int at = (address - from) * PLANES;
      for (int fromTheRight = 0; fromTheRight < PLANES; fromTheRight++) {
        int colour = file[at + fromTheRight] & 0xff;
        for (int plane = 0; plane < PLANES; plane++) {
          if ((colour & (1 << plane)) != 0) planes[plane][address] |= (byte) (1 << fromTheRight);
        }
      }
    }
  }

  /**
   * The colours of the ROM, for a game that coloured the letters and the sprites the machine
   * draws with. Without one the ROM is no picture, and the machine's own byte is already what
   * eight planes equal to it would say.
   */
  public void takeTheRom(byte[] file) {
    if (file.length != ROM_LENGTH) {
      throw new IllegalArgumentException("The colours of a ROM are " + ROM_LENGTH + " bytes, and these are " + file.length);
    }
    slice(file, 0, RAM);
    romColoured = true;
  }

  /** Whether the ROM has colours of its own, or is read from the machine as it stands. */
  public boolean romColoured() {
    return romColoured;
  }

  /** No colours at all, which is how much room this takes until a game brings some. */
  public void blank() {
    bytes = new byte[PLANES][SIZE];
    romColoured = false;
    writingWhereTheMachineWrote = true;
    Arrays.fill(holding, 0);
  }

  /**
   * Whether what a follower writes lands where the machine wrote in that same instruction, rather
   * than where the follower's own pointers sent it. Its registers carry colours, and one addition
   * on a pointer is enough to send a write somewhere the machine never wrote; where the machine
   * wrote is the one address nobody has to guess. What it <em>reads</em> is still its own, which
   * is what a table indexed by a colour needs.
   */
  public boolean writingWhereTheMachineWrote() {
    return writingWhereTheMachineWrote;
  }

  public void writingWhereTheMachineWrote(boolean whereTheMachineWrote) {
    writingWhereTheMachineWrote = whereTheMachineWrote;
  }

  /** The machine is about to run the instruction the eight have just run, and to write where it writes. */
  public void machineIsWriting() {
    wrote = 0;
  }

  /**
   * It has written, and what the eight held back lands now: each one's n-th write where the
   * machine's n-th went, and where the machine wrote fewer, where the follower meant it to go.
   */
  public void machineHasWritten() {
    for (int plane = 0; plane < PLANES; plane++) {
      for (int n = 0; n < holding[plane]; n++) {
        int at = n < wrote ? wroteAt[n] : heldAt[plane][n];
        if (at >= RAM) bytes()[plane][at] = (byte) held[plane][n];
      }
      holding[plane] = 0;
    }
  }

  /** Where the machine has just written, in the order it wrote. */
  public void machineWroteAt(int address) {
    if (wrote < MOST_WRITES) wroteAt[wrote++] = address;
  }


  private byte[][] bytes() {
    if (bytes == null) blank();
    return bytes;
  }

  /**
   * Whether there is something at an address with no colours of its own: the eight planes all
   * saying the same thing, which is how a byte that is no picture is written, and saying
   * something rather than nothing. Its pixels come out the last colour where the byte is set,
   * and that is the white somebody looking at a white shape wants to ask about. Eight planes of
   * nothing is not a shape without colours, it is no shape.
   */
  public boolean noColoursOfItsOwn(int address) {
    byte[][] planes = bytes();
    byte first = planes[0][address];
    if (first == 0) return false;
    for (int plane = 1; plane < PLANES; plane++) {
      if (planes[plane][address] != first) return false;
    }
    return true;
  }

  /** The colour of one pixel of the byte at an address, the leftmost being the first. */
  public int colourOf(int address, int pixel) {
    byte[][] planes = bytes();
    int bit = 1 << (7 - pixel);
    int colour = 0;
    for (int plane = 0; plane < PLANES; plane++) {
      if ((planes[plane][address] & bit) != 0) colour |= 1 << plane;
    }
    return colour;
  }

  /**
   * One of the eight as a memory a processor runs on: what is read to decide what to run comes
   * from the machine, so the processor on this plane can never decode an instruction other than
   * the one the machine is running, and everything else is this plane's own colours - a number
   * written into an instruction included, because that is a colour a game can paint. Below the
   * RAM there are none unless the game brought them, and the machine's byte is already what eight
   * equal planes would say.
   */
  public Memory plane(int which, Memory machine) {
    byte[] mine = bytes()[which];
    return new Memory() {
      public int read(int address, int fetching) {
        return fetching != 0 || (address < RAM && !romColoured) ? machine.peek(address) : mine[address] & 0xff;
      }

      public void write(int address, int value) {
        if (!writingWhereTheMachineWrote) {
          if (address >= RAM) mine[address] = (byte) value;
        } else if (holding[which] < MOST_WRITES) {
          heldAt[which][holding[which]] = address;
          held[which][holding[which]++] = value;
        }
      }

      public int peek(int address) {
        return address < RAM && !romColoured ? machine.peek(address) : mine[address] & 0xff;
      }

      public void poke(int address, int value) {
        write(address, value);
      }

      public void reset() {
      }
    };
  }
}
