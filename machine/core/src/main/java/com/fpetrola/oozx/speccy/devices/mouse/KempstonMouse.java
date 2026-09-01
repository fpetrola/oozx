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

  private int x;
  private int y;
  private int buttons = WRAP;

  /** The mouse was moved by this much, in whatever units the pointer moved on the desk. */
  public void moved(int dx, int dy) {
    x = (x + dx) & WRAP;
    y = (y - dy) & WRAP;
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
  }

  public int x() {
    return x;
  }

  public int y() {
    return y;
  }

  public int buttons() {
    return buttons;
  }

  /** Whether that button is being held, for a window that shows what the machine is being told. */
  public boolean isHeld(int which) {
    return (buttons & 1 << which) == 0;
  }
}
