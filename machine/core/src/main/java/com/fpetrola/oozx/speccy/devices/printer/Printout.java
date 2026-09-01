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

import java.util.ArrayList;
import java.util.List;

/**
 * The paper that comes out of a ZX Printer: rows of 256 dots, in the order they were burned.
 * <p>
 * Fuse writes these straight into a PBM file and, for the text version, reads them back to guess
 * which characters they were. Here the paper is an object anything can watch, which is what lets a
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
