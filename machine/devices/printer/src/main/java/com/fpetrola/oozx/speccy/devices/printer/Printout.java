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

package com.fpetrola.oozx.speccy.devices.printer;

import java.util.ArrayList;
import java.util.List;

/**
 * The paper that comes out of a ZX Printer: rows of 256 dots, in the order they were burned.
 * <p>
 * The paper is an object anything can watch, which is what lets a
 * window show a printout as it happens and a test read one without a screen.
 */
public class Printout {
  public static final int WIDTH = 256;

  private final List<boolean[]> rows = new ArrayList<>();
  private final List<Listener> listeners = new ArrayList<>();

  public interface Listener {
    void rowPrinted(boolean[] dots);
  }

  void print(boolean[] dots) {
    boolean[] row = dots.clone();
    rows.add(row);
    listeners.forEach(listener -> listener.rowPrinted(row));
  }

  public void whenPrinted(Listener listener) {
    listeners.add(listener);
  }

  public void stopWatching(Listener listener) {
    listeners.remove(listener);
  }

  public int height() {
    return rows.size();
  }

  public boolean[] row(int index) {
    return rows.get(index);
  }

  /** Tearing off the paper: the printout so far is gone and the next row starts a new one. */
  public void tearOff() {
    rows.clear();
    listeners.forEach(listener -> listener.rowPrinted(null));
  }
}
