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

package com.fpetrola.oozx.speccy.modules.keyboard;

/**
 * What one press by the person at the keyboard puts down on the Spectrum: a key, a shift and a
 * key - a full stop is symbol shift and M - or a shift alone.
 */
public record Combination(SpectrumKey... keys) {
  public static final Combination NONE = new Combination();

  public static Combination of(SpectrumKey key) {
    return new Combination(key);
  }

  public static Combination shifted(SpectrumKey shift, SpectrumKey key) {
    return new Combination(shift, key);
  }

  public void pressOn(KeyMatrix matrix) {
    for (SpectrumKey key : keys) {
      matrix.press(key);
    }
  }

  public void releaseOn(KeyMatrix matrix) {
    for (SpectrumKey key : keys) {
      matrix.release(key);
    }
  }
}
