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

package com.fpetrola.oozx.speccy.modules.scheduler;

/**
 * Something the machine has to do at a T-state. Whoever owns it registers it once with the
 * scheduler and asks for it as often as it is wanted; when it is for is the scheduler's business.
 */
public abstract class Task {
  /** What it is called: the name of its class, so it cannot drift from what it does. */
  public String name() {
    return getClass().getSimpleName();
  }

  /** Told the T-state it was due at: a task that repeats counts the next one from there. */
  public abstract void run(long due);
}
