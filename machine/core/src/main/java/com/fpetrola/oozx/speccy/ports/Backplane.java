/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.ports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The handlers registry into the bus, in the order they answer. Counts its changes, so whoever
 * remembered something about them can tell it no longer holds without being told.
 */
public class Backplane {
  private final List<Wired> wired = new ArrayList<>();
  private int version;

  /**
   * @param inFront the first to attach wins the bits it drives, so something registry
   *                in by hand goes ahead of the machine's own chips: a board on the even ports
   *                the ULA answers has to be heard over the keyboard.
   */
  public void attach(Wired[] ports, boolean inFront) {
    for (int i = 0; i < ports.length; i++) {
      wired.add(inFront ? i : wired.size(), ports[i]);
    }
    version++;
  }

  public void detach(Wired[] ports) {
    wired.removeAll(Arrays.asList(ports));
    version++;
  }

  public void clear() {
    wired.clear();
    version++;
  }

  int version() {
    return version;
  }

  List<Wired> wired() {
    return wired;
  }
}
