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

package com.fpetrola.oozx.speccy.modules.keyboard;

/**
 * A key of the Spectrum: where it sits on the matrix, and what is written on it.
 * <p>
 * The forty keys are the machine's, and each one knows its own half row and bit rather than
 * being a number with tables hung off it. The order is the order of the matrix, five keys to a
 * half row, which is how the machine is wired and how it reads.
 */
public enum SpectrumKey {
  CAPS_SHIFT(0, 0x01, "Caps Shift"), Z(0, 0x02, "Z"), X(0, 0x04, "X"), C(0, 0x08, "C"), V(0, 0x10, "V"),
  A(1, 0x01, "A"), S(1, 0x02, "S"), D(1, 0x04, "D"), F(1, 0x08, "F"), G(1, 0x10, "G"),
  Q(2, 0x01, "Q"), W(2, 0x02, "W"), E(2, 0x04, "E"), R(2, 0x08, "R"), T(2, 0x10, "T"),
  ONE(3, 0x01, "1"), TWO(3, 0x02, "2"), THREE(3, 0x04, "3"), FOUR(3, 0x08, "4"), FIVE(3, 0x10, "5"),
  ZERO(4, 0x01, "0"), NINE(4, 0x02, "9"), EIGHT(4, 0x04, "8"), SEVEN(4, 0x08, "7"), SIX(4, 0x10, "6"),
  P(5, 0x01, "P"), O(5, 0x02, "O"), I(5, 0x04, "I"), U(5, 0x08, "U"), Y(5, 0x10, "Y"),
  ENTER(6, 0x01, "Enter"), L(6, 0x02, "L"), K(6, 0x04, "K"), J(6, 0x08, "J"), H(6, 0x10, "H"),
  SPACE(7, 0x01, "Space"), SYMBOL_SHIFT(7, 0x02, "Symbol Shift"), M(7, 0x04, "M"), N(7, 0x08, "N"), B(7, 0x10, "B");

  /** How many half rows the matrix has, and how many keys are on each. */
  public static final int HALF_ROWS = 8;

  private final int halfRow;
  private final int bit;
  private final String label;

  SpectrumKey(int halfRow, int bit, String label) {
    this.halfRow = halfRow;
    this.bit = bit;
    this.label = label;
  }

  /** Which of the eight half rows an address line selects this key on. */
  public int halfRow() {
    return halfRow;
  }

  /** Which of the five bits of that half row goes low while this key is down. */
  public int bit() {
    return bit;
  }

  /** What is written on the key. */
  public String label() {
    return label;
  }
}
