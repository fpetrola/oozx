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

package com.fpetrola.oozx.speccy.bridge;

import java.util.Objects;

public class TStateUpdate {
  public final long key;
  public final int value;
  public final String description;
  public final int pc;

  public TStateUpdate(long key, int value, String description, int pc) {
    this.key = key;
    this.value = value;
    this.description = description;
    this.pc = pc;
  }

  public String toString() {
    return "TStateUpdate{" +
        "key=" + key +
        ", value=" + value +
        ", description='" + description + '\'' +
//        ", pc='" + pc + '\'' +
        "}\n";
  }

  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TStateUpdate that = (TStateUpdate) o;
    return key == that.key && value == that.value/* && Objects.equals(description, that.description)*/;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }
}
