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

package com.fpetrola.oozx.speccy.devices.printer;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * The belt, the stylus and the paper feed of a ZX Printer, driven by one port.
 * <p>
 * There is no buffer in one of these: a spark burns a dot where the stylus happens to be, so the
 * ROM watches the position over the port and turns the stylus on and off as the belt moves. All of
 * the arithmetic is Fuse's printer.c, itself from xz80, with its constants kept exactly - 440
 * t-states per dot at full speed, 64 dots of margin before the paper, and a belt that comes round
 * every 384 dot-times of which 256 are over the paper.
 * <p>
 * Where Fuse keeps a frame counter and a within-frame t-state count and adds them up at every
 * turn, this measures in ticks that never go backwards, which says the same thing in one number
 * and takes the frame bookkeeping out of the middle of the arithmetic.
 */
public class ZxPrinter {
  private static final int DOTS_PER_LINE = Printout.WIDTH;
  private static final int MARGIN = 64;
  private static final int BELT = 384;
  private static final int TICKS_PER_DOT = 440;
  /** Fuse stops feeding after this much of nothing, so an idle printer does not make miles of blank paper. */
  private static final int IDLE_FRAME_LIMIT = 400;

  private final Printout paper;
  private final LongSupplier ticks;
  private final IntSupplier frameLength;

  private final boolean[] line = new boolean[DOTS_PER_LINE];
  private int speed;
  private int newSpeed;
  private long lineStarted;
  private int lastDot = -1;
  private boolean stylus;

  public ZxPrinter(Printout paper, LongSupplier ticks, IntSupplier frameLength) {
    this.paper = paper;
    this.ticks = ticks;
    this.frameLength = frameLength;
  }

  /** Bit 2 stops the motor, bit 1 chooses the speed, bit 7 is the stylus. */
  public void write(byte value) {
    boolean stop = (value & 0x04) != 0;
    int chosenSpeed = (value & 0x02) != 0 ? 1 : 2;
    boolean wantsStylus = (value & 0x80) != 0;

    if (speed == 0) {
      if (!stop) {
        speed = chosenSpeed;
        lineStarted = ticks.getAsLong();
        stylus = wantsStylus;
        lastDot = -1;
      }
      return;
    }

    int ticksPerDot = TICKS_PER_DOT / speed;
    int dot = dotReached(ticksPerDot);

    burn(lastDot, dot);
    if (dot >= DOTS_PER_LINE && lastDot < DOTS_PER_LINE) {
      paper.print(line);
    }

    while (dot >= BELT - MARGIN) {
      lineStarted += (long) ticksPerDot * BELT;
      dot -= BELT;
      if (newSpeed != 0) {
        dot = ((dot + MARGIN) * ticksPerDot) / (TICKS_PER_DOT / newSpeed) - MARGIN;
        speed = newSpeed;
        newSpeed = 0;
        ticksPerDot = TICKS_PER_DOT / speed;
      }
      burn(0, dot);
      if (dot >= DOTS_PER_LINE) {
        paper.print(line);
      }
    }
    if (dot < 0) {
      dot = -1;
    }

    if (stop) {
      if (dot >= 0 && dot < DOTS_PER_LINE) {
        burn(dot, DOTS_PER_LINE);
        paper.print(line);
      }
      speed = 0;
      stylus = false;
    } else {
      lastDot = dot;
      stylus = wantsStylus;
      if (dot < 0) {
        speed = chosenSpeed;
      } else {
        newSpeed = chosenSpeed == speed ? 0 : chosenSpeed;
      }
    }
  }

  /**
   * Bit 0 is the position encoder, which the ROM spins on: it goes high once the belt has passed
   * the last dot written. Bit 7 says the stylus is over the paper. The rest are the idle bits.
   */
  public byte read() {
    if (speed == 0) {
      return 0x3e;
    }

    int ticksPerDot = TICKS_PER_DOT / speed;
    int dot = dotReached(ticksPerDot);
    int lastWritten = lastDot;
    int pending = newSpeed;

    while (dot > BELT - MARGIN) {
      lastWritten = -1;
      dot -= BELT;
      if (pending != 0) {
        dot = ((dot + MARGIN) * ticksPerDot) / (TICKS_PER_DOT / pending) - MARGIN;
        pending = 0;
      }
    }

    int answer = (dot > -10 && dot < 0) || stylus ? 0xbe : 0x3e;
    if (dot > lastWritten) {
      answer |= 1;
    }
    return (byte) answer;
  }

  /** Where the stylus is now, in dots from the left edge of the paper. */
  private int dotReached(int ticksPerDot) {
    long elapsed = Math.min(ticks.getAsLong() - lineStarted,
        (long) IDLE_FRAME_LIMIT * frameLength.getAsInt());
    return (int) (elapsed / ticksPerDot) - MARGIN;
  }

  private void burn(int from, int to) {
    for (int dot = Math.max(from, 0); dot < to && dot < DOTS_PER_LINE; dot++) {
      line[dot] = stylus;
    }
  }
}
