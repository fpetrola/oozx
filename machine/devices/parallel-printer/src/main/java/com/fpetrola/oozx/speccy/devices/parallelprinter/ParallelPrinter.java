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
package com.fpetrola.oozx.speccy.devices.parallelprinter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * A printer on a Centronics port: eight data lines latched by a write, a strobe that says
 * "take it", and BUSY, which this one never is.
 * <p>
 * The reading of the strobe: the +3's ROM writes it the wrong way round and some
 * programs write it the right way, so no one edge can be trusted as "print now". Two edges
 * within ten thousand T-states of each other are one character; a lone edge is remembered and
 * forgotten if the next is too far off.
 */
public class ParallelPrinter {

  static final int STROBE_MAX_CYCLES = 10000;

  private final LongSupplier ticks;
  private final StringBuilder text = new StringBuilder();
  private final List<Runnable> listeners = new ArrayList<>();
  private int data;
  private int lastData;
  private long lastStrobeAt = -1;

  /** @param ticks a monotonically increasing T-state count, not reset each frame */
  public ParallelPrinter(LongSupplier ticks) {
    this.ticks = ticks;
  }

  public void write(int data) {
    this.data = data & 0xff;
  }

  /** Records a strobe transition; either edge direction is treated as valid (see class doc). */
  public void strobe(boolean on) {
    long now = ticks.getAsLong();
    if (lastStrobeAt >= 0 && now - lastStrobeAt < STROBE_MAX_CYCLES) {
      print(lastData);
      lastStrobeAt = -1;
    } else {
      lastData = data;
      lastStrobeAt = now;
    }
  }

  /** Appends one character, whether from a serial line or decoded ZX Printer output. */
  public void print(int character) {
    text.append((char) character);
    listeners.forEach(Runnable::run);
  }

  public String text() {
    return text.toString();
  }

  public void tearOff() {
    text.setLength(0);
    listeners.forEach(Runnable::run);
  }

  public void onChange(Runnable listener) {
    listeners.add(listener);
  }
}
