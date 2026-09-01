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

package com.fpetrola.oozx.speccy.devices.mouse;

/**
 * What a Kempston Mouse tells the machine: two counters and three buttons.
 * <p>
 * The counters are not a position on the screen. They are eight-bit counts that wrap, and the
 * program reads them, compares them with what it read last time and moves its own pointer by the
 * difference - which is why the mouse can be moved for ever in one direction without running out
 * of numbers, and why nothing here needs to know how big the screen is.
 * <p>
 * The buttons read the other way round from what one expects: a bit that is DOWN is a button
 * being held, and all three rest high. Ported from Fuse's peripherals/kempmouse.c.
 */
public class KempstonMouse {

  /** Vertical counts the opposite way from the screen: down on the desk is down in the count. */
  private static final int WRAP = 0xFF;

  /**
   * Somebody listening in on the wire, for working out why a program behaves as it does.
   * <p>
   * The two ends of this can only be understood together: what the hand did, and what the
   * program actually asked for and when. A program that reads the counters fifty times a second
   * while the hand moves faster than that sees jumps, not movement, and no amount of looking at
   * either end on its own shows it.
   */
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

  /**
   * The most a program can be shown at once without misreading it.
   * <p>
   * A program works out how far to move its pointer from the difference between what it reads
   * now and what it read last, as a signed byte. More than this between two of its readings
   * comes out as a large move the OTHER WAY, so a hand moving steadily produces a pointer that
   * jumps backwards - and the counter cannot even show that it happened, because a difference
   * of more than 127 is exactly what it has no room for.
   */
  private static final int MOST_ONE_READING_CAN_SHOW = 127;

  private int x;
  private int y;
  private int buttons = WRAP;

  /**
   * Movement the program has not been shown yet, and never more than one more reading's worth.
   * <p>
   * A hand can always move faster than a program looks. What is over what a reading can carry is
   * held back and given at the next one, so a quick stroke arrives a fiftieth of a second late
   * rather than leaping backwards.
   * <p>
   * But only that much. Keeping ALL of it turns a hand that is faster than the program into a
   * debt that grows for as long as somebody keeps moving, and a pointer that crawls along
   * playing catch-up minutes later - which is not what a mouse does. Past this, the movement is
   * dropped, exactly as it is on a real one: a mouse that cannot resolve how fast you moved
   * simply does not report it, and the pointer stays under the hand instead of behind it.
   */
  private int owedX;
  private int owedY;

  /**
   * Whether a stroke too quick to be read at once arrives at the next reading, or not at all.
   * <p>
   * Carried, the pointer is exact but a fiftieth of a second behind on quick strokes. Dropped,
   * it is always under the hand and quick strokes simply move it less far, which is how a mouse
   * that cannot count that fast behaves. Neither is wrong; which one feels right depends on the
   * program and on the hand, so it is a knob rather than a decision made here.
   */
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

  /**
   * How far each counter has moved since the program last looked at it.
   * <p>
   * This, and not the size of any one step, is what has to stay inside 127: the program sees
   * only the difference between two readings, so a thousand small movements between them add up
   * to the same unreadable jump as one large one.
   */
  private int shownX;
  private int shownY;

  /** The mouse was moved by this much, in whatever units the pointer moved on the desk. */
  public void moved(int dx, int dy) {
    owedX += dx;
    owedY -= dy;
    payWhatCanBeRead();
    owedX = carryOver ? keepAtMostOneReading(owedX) : 0;
    owedY = carryOver ? keepAtMostOneReading(owedY) : 0;
    watcher.handMoved(dx, dy, x, y);
  }

  /** What is owed beyond one more reading is not late, it is gone, as it would be on a desk. */
  private static int keepAtMostOneReading(int owed) {
    return Math.max(-MOST_ONE_READING_CAN_SHOW, Math.min(MOST_ONE_READING_CAN_SHOW, owed));
  }

  /** Hands over as much as this reading can still carry, and keeps the rest for the next one. */
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

  /** How much more can be added without the difference since the last reading becoming unreadable. */
  private static int whatFits(int owed, int shown) {
    if (owed > 0) {
      return Math.max(0, Math.min(owed, MOST_ONE_READING_CAN_SHOW - shown));
    }
    if (owed < 0) {
      return Math.min(0, Math.max(owed, -MOST_ONE_READING_CAN_SHOW - shown));
    }
    return 0;
  }

  /** Whether the hand has got ahead of the program, and by how much. */
  public int owed() {
    return Math.abs(owedX) + Math.abs(owedY);
  }

  /** Button 0 is the left one, 1 the right, 2 the middle. */
  public void button(int which, boolean down) {
    if (which >= 0 && which < 8) {
      buttons = down ? buttons & ~(1 << which) & WRAP : buttons | 1 << which;
    }
  }

  /** Nothing held, nothing moved: what the machine sees when the mouse is unplugged and back. */
  public void rest() {
    buttons = WRAP;
    owedX = owedY = shownX = shownY = 0;
  }

  public int x() {
    int reading = x;
    watcher.programRead("x", reading);
    // It has looked, so the difference starts again from here and whatever was held back can go.
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

  /** Whether that button is being held, for a window that shows what the machine is being told. */
  public boolean isHeld(int which) {
    return (buttons & 1 << which) == 0;
  }
}
