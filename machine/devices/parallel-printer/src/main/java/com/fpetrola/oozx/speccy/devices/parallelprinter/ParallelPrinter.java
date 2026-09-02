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
package com.fpetrola.oozx.speccy.devices.parallelprinter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * A printer on a Centronics port: eight data lines latched by a write, a strobe that says
 * "take it", and BUSY, which this one never is.
 * <p>
 * Fuse's reading of the strobe, kept here: the +3's ROM writes it the wrong way round and some
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

  /** @param ticks the machine's T-states, counted across frames and never going back */
  public ParallelPrinter(LongSupplier ticks) {
    this.ticks = ticks;
  }

  public void write(int data) {
    this.data = data & 0xff;
  }

  /** The strobe line moved; either edge counts, see above. */
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

  /** A character straight from a serial line, or the ZX Printer's output read as text. */
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
