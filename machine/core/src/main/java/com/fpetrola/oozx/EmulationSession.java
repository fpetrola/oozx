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

package com.fpetrola.oozx;

import com.google.inject.Singleton;

/**
 * Whether the emulator should keep running.
 * <p>
 * The flag is written from one thread and read from another: closing the window finishes the
 * session on the event dispatch thread, while the emulation loop reads it on its own. It is
 * volatile for that reason — without it the loop is free to hoist the read and never observe
 * the change, and the emulator keeps running after its window is gone.
 */
@Singleton
public class EmulationSession {

  private volatile boolean alive = true;

  public boolean isAlive() {
    return alive;
  }

  public void finish() {
    alive = false;
  }
}
