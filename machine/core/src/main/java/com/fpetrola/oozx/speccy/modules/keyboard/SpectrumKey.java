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

/** The 40 Spectrum keys, each carrying its own matrix half row and bit rather than an index into a lookup table. */
public enum SpectrumKey {
  CAPS_SHIFT(0, 0x01, "Caps Shift"), Z(0, 0x02, "Z"), X(0, 0x04, "X"), C(0, 0x08, "C"), V(0, 0x10, "V"),
  A(1, 0x01, "A"), S(1, 0x02, "S"), D(1, 0x04, "D"), F(1, 0x08, "F"), G(1, 0x10, "G"),
  Q(2, 0x01, "Q"), W(2, 0x02, "W"), E(2, 0x04, "E"), R(2, 0x08, "R"), T(2, 0x10, "T"),
  ONE(3, 0x01, "1"), TWO(3, 0x02, "2"), THREE(3, 0x04, "3"), FOUR(3, 0x08, "4"), FIVE(3, 0x10, "5"),
  ZERO(4, 0x01, "0"), NINE(4, 0x02, "9"), EIGHT(4, 0x04, "8"), SEVEN(4, 0x08, "7"), SIX(4, 0x10, "6"),
  P(5, 0x01, "P"), O(5, 0x02, "O"), I(5, 0x04, "I"), U(5, 0x08, "U"), Y(5, 0x10, "Y"),
  ENTER(6, 0x01, "Enter"), L(6, 0x02, "L"), K(6, 0x04, "K"), J(6, 0x08, "J"), H(6, 0x10, "H"),
  SPACE(7, 0x01, "Space"), SYMBOL_SHIFT(7, 0x02, "Symbol Shift"), M(7, 0x04, "M"), N(7, 0x08, "N"), B(7, 0x10, "B");

  public static final int HALF_ROWS = 8;

  private final int halfRow;
  private final int bit;
  private final String label;

  SpectrumKey(int halfRow, int bit, String label) {
    this.halfRow = halfRow;
    this.bit = bit;
    this.label = label;
  }

  public int halfRow() {
    return halfRow;
  }

  public int bit() {
    return bit;
  }

  public String label() {
    return label;
  }
}
