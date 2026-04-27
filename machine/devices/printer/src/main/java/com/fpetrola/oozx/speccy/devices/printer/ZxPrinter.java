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
 * ZX Printer belt, stylus and paper feed, controlled through one port. There is no dot buffer:
 * the stylus burns wherever it is when fired, so the ROM polls belt position and toggles the
 * stylus itself. Constants match hardware exactly: 440 T-states/dot at full speed, 64 dots
 * margin, a 384-dot belt of which 256 dots cross the paper. Time is tracked as a single
 * monotonically increasing tick count rather than frame number plus within-frame T-states.
 */
public class ZxPrinter {
  private static final int DOTS_PER_LINE = Printout.WIDTH;
  private static final int MARGIN = 64;
  private static final int BELT = 384;
  private static final int TICKS_PER_DOT = 440;
  /** Idle timeout after which feeding stops, avoiding endless blank paper from an idle printer. */
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

  /** The Printout this device writes to, exposed so a window can display it. */
  public Printout paper() {
    return paper;
  }

  /** Control byte: bit 2 motor stop, bit 1 speed select, bit 7 stylus. */
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

  /** Status byte: bit 0 is the belt-position encoder (high once past the last written dot),
   * bit 7 reflects stylus position, remaining bits idle. */
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

  /** Current stylus position, in dots from the paper's left edge. */
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
