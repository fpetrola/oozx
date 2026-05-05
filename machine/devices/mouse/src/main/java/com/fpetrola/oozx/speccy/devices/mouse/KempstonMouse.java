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

package com.fpetrola.oozx.speccy.devices.mouse;

/**
 * Kempston Mouse protocol state: two wrapping 8-bit delta counters and three buttons. Programs
 * read the counters as relative movement, not absolute position, and a button bit reads low
 * (not high) while held.
 */
public class KempstonMouse {

  /** Counter wrap value; vertical counts increase moving down, opposite of screen Y. */
  private static final int WRAP = 0xFF;

  /** Observer hook exposing both raw hand motion and program reads together, needed since
   * neither alone explains an apparent pointer jump. */
  public interface Watcher {
    void handMoved(int dx, int dy, int x, int y);

    void programRead(String which, int value);
  }

  private static final Watcher NOBODY = new Watcher() {
    public void handMoved(int dx, int dy, int x, int y) {
    }

    public void programRead(String which, int value) {
    }
  };

  private volatile Watcher watcher = NOBODY;

  public void watch(Watcher listening) {
    watcher = listening == null ? NOBODY : listening;
  }

  /** Largest safe delta between two reads: a program treats the difference as a signed byte,
   * so anything past 127 wraps to a large move in the wrong direction. */
  private static final int MOST_ONE_READING_CAN_SHOW = 127;

  private int x;
  private int y;
  private int buttons = WRAP;

  /** Unreported motion, capped at one reading's worth: excess beyond that is dropped rather
   * than queued indefinitely, matching how a real mouse behaves when outrun. */
  private int owedX;
  private int owedY;

  /** Toggles whether excess motion carries to the next read (accurate but delayed) or is
   * dropped (immediate but undercounts fast strokes); left as a user preference. */
  private boolean carryOver = true;

  public void setCarryOver(boolean wanted) {
    carryOver = wanted;
    if (!wanted) {
      owedX = owedY = 0;
    }
  }

  public boolean isCarryOver() {
    return carryOver;
  }

  /** Accumulated counter movement since the last program read; this total, not any single
   * step, is what must stay within the 127 limit. */
  private int shownX;
  private int shownY;

  /** Accepts a raw hand-motion delta, in whatever units the input device reports. */
  public void moved(int dx, int dy) {
    owedX += dx;
    owedY -= dy;
    payWhatCanBeRead();
    owedX = carryOver ? keepAtMostOneReading(owedX) : 0;
    owedY = carryOver ? keepAtMostOneReading(owedY) : 0;
    watcher.handMoved(dx, dy, x, y);
  }

  /** Clamps to the carry limit; anything beyond it is discarded, not merely delayed. */
  private static int keepAtMostOneReading(int owed) {
    return Math.max(-MOST_ONE_READING_CAN_SHOW, Math.min(MOST_ONE_READING_CAN_SHOW, owed));
  }

  /** Moves as much owed motion into the counters as still fits within the read limit. */
  private void payWhatCanBeRead() {
    int payX = whatFits(owedX, shownX);
    int payY = whatFits(owedY, shownY);
    x = (x + payX) & WRAP;
    y = (y + payY) & WRAP;
    owedX -= payX;
    owedY -= payY;
    shownX += payX;
    shownY += payY;
  }

  /** Remaining headroom before the shown delta would exceed what a signed byte can carry. */
  private static int whatFits(int owed, int shown) {
    if (owed > 0) {
      return Math.max(0, Math.min(owed, MOST_ONE_READING_CAN_SHOW - shown));
    }
    if (owed < 0) {
      return Math.min(0, Math.max(owed, -MOST_ONE_READING_CAN_SHOW - shown));
    }
    return 0;
  }

  /** Total unreported motion still pending, summed across both axes. */
  public int owed() {
    return Math.abs(owedX) + Math.abs(owedY);
  }

  /** Sets one button's state; button numbering is 0=left, 1=right, 2=middle. */
  public void button(int which, boolean down) {
    if (which >= 0 && which < 8) {
      buttons = down ? buttons & ~(1 << which) & WRAP : buttons | 1 << which;
    }
  }

  /** Resets to the unplugged state: no buttons held, no pending or shown movement. */
  public void rest() {
    buttons = WRAP;
    owedX = owedY = shownX = shownY = 0;
  }

  public int x() {
    int reading = x;
    watcher.programRead("x", reading);
    // Resets the read baseline and releases any motion that was waiting for room.
    shownX = 0;
    payWhatCanBeRead();
    return reading;
  }

  public int y() {
    int reading = y;
    watcher.programRead("y", reading);
    shownY = 0;
    payWhatCanBeRead();
    return reading;
  }

  public int buttons() {
    watcher.programRead("buttons", buttons);
    return buttons;
  }

  /** True if the given button is currently held (its bit reads low). */
  public boolean isHeld(int which) {
    return (buttons & 1 << which) == 0;
  }
}
