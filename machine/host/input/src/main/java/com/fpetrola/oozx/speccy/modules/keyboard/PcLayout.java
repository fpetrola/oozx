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

package com.fpetrola.oozx.speccy.modules.keyboard;

import com.fpetrola.oozx.speccy.modules.input.Input;

import java.util.EnumMap;
import java.util.Map;

/** The keys where a PC keyboard has them: a letter is that letter, and what a Spectrum needs a shift for gets one. */
public class PcLayout implements KeyLayout {
  private final Map<Input.InputKey, Combination> produces = new EnumMap<>(Input.InputKey.class);

  public PcLayout() {
    keys();
    symbols();
    modifiers();
    arrows();
  }

  /** The keys a Spectrum has as they are, digits and letters, and the few a PC has that need caps shift. */
  private void keys() {
    for (char letter = 'a'; letter <= 'z'; letter++) {
      put(Input.InputKey.valueOf("INPUT_KEY_" + letter), Combination.of(SpectrumKey.valueOf(String.valueOf(Character.toUpperCase(letter)))));
    }
    SpectrumKey[] digits = {SpectrumKey.ZERO, SpectrumKey.ONE, SpectrumKey.TWO, SpectrumKey.THREE, SpectrumKey.FOUR,
        SpectrumKey.FIVE, SpectrumKey.SIX, SpectrumKey.SEVEN, SpectrumKey.EIGHT, SpectrumKey.NINE};
    for (int digit = 0; digit < 10; digit++) {
      put(Input.InputKey.valueOf("INPUT_KEY_" + digit), Combination.of(digits[digit]));
    }
    put(Input.InputKey.INPUT_KEY_Escape, Combination.shifted(SpectrumKey.CAPS_SHIFT, SpectrumKey.ONE));
    put(Input.InputKey.INPUT_KEY_BackSpace, Combination.shifted(SpectrumKey.CAPS_SHIFT, SpectrumKey.ZERO));
    put(Input.InputKey.INPUT_KEY_Caps_Lock, Combination.shifted(SpectrumKey.CAPS_SHIFT, SpectrumKey.TWO));
    put(Input.InputKey.INPUT_KEY_Return, Combination.of(SpectrumKey.ENTER));
    put(Input.InputKey.INPUT_KEY_space, Combination.of(SpectrumKey.SPACE));
  }

  /** What the Spectrum types with symbol shift and a key, where a PC has a key or shift for it. */
  private void symbols() {
    symbol(Input.InputKey.INPUT_KEY_minus, SpectrumKey.J);
    symbol(Input.InputKey.INPUT_KEY_equal, SpectrumKey.L);
    symbol(Input.InputKey.INPUT_KEY_Tab, SpectrumKey.CAPS_SHIFT);
    symbol(Input.InputKey.INPUT_KEY_semicolon, SpectrumKey.O);
    symbol(Input.InputKey.INPUT_KEY_apostrophe, SpectrumKey.SEVEN);
    symbol(Input.InputKey.INPUT_KEY_numbersign, SpectrumKey.THREE);
    symbol(Input.InputKey.INPUT_KEY_comma, SpectrumKey.N);
    symbol(Input.InputKey.INPUT_KEY_period, SpectrumKey.M);
    symbol(Input.InputKey.INPUT_KEY_slash, SpectrumKey.V);
    symbol(Input.InputKey.INPUT_KEY_asterisk, SpectrumKey.B);
    symbol(Input.InputKey.INPUT_KEY_dollar, SpectrumKey.FOUR);
    symbol(Input.InputKey.INPUT_KEY_exclam, SpectrumKey.ONE);
    symbol(Input.InputKey.INPUT_KEY_less, SpectrumKey.R);
    symbol(Input.InputKey.INPUT_KEY_parenleft, SpectrumKey.EIGHT);
    symbol(Input.InputKey.INPUT_KEY_quotedbl, SpectrumKey.P);
    symbol(Input.InputKey.INPUT_KEY_ampersand, SpectrumKey.SIX);
    symbol(Input.InputKey.INPUT_KEY_percent, SpectrumKey.FIVE);
    symbol(Input.InputKey.INPUT_KEY_at, SpectrumKey.TWO);
    symbol(Input.InputKey.INPUT_KEY_greater, SpectrumKey.T);
    symbol(Input.InputKey.INPUT_KEY_question, SpectrumKey.C);
    symbol(Input.InputKey.INPUT_KEY_underscore, SpectrumKey.ZERO);
    symbol(Input.InputKey.INPUT_KEY_asciicircum, SpectrumKey.H);
    symbol(Input.InputKey.INPUT_KEY_parenright, SpectrumKey.NINE);
    symbol(Input.InputKey.INPUT_KEY_colon, SpectrumKey.Z);
    symbol(Input.InputKey.INPUT_KEY_plus, SpectrumKey.K);
  }

  /** The PC's shifts: shift is caps shift, and the rest of its modifiers are symbol shift. */
  private void modifiers() {
    put(Input.InputKey.INPUT_KEY_Shift_L, Combination.of(SpectrumKey.CAPS_SHIFT));
    put(Input.InputKey.INPUT_KEY_Shift_R, Combination.of(SpectrumKey.CAPS_SHIFT));
    put(Input.InputKey.INPUT_KEY_Control_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Alt_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Meta_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Super_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Hyper_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Hyper_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Super_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Meta_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Alt_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Control_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Mode_switch, Combination.of(SpectrumKey.SYMBOL_SHIFT));
  }

  /** The cursor keys, where a Spectrum has its arrows: 5 to 8. */
  private void arrows() {
    put(Input.InputKey.INPUT_KEY_Left, Combination.of(SpectrumKey.FIVE));
    put(Input.InputKey.INPUT_KEY_Down, Combination.of(SpectrumKey.SIX));
    put(Input.InputKey.INPUT_KEY_Up, Combination.of(SpectrumKey.SEVEN));
    put(Input.InputKey.INPUT_KEY_Right, Combination.of(SpectrumKey.EIGHT));
    put(Input.InputKey.INPUT_KEY_KP_Enter, Combination.of(SpectrumKey.ENTER));
  }

  private void symbol(Input.InputKey host, SpectrumKey key) {
    put(host, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, key));
  }

  private void put(Input.InputKey host, Combination combination) {
    produces.put(host, combination);
  }

  public Combination produces(Input.InputKey pressed) {
    return produces.getOrDefault(pressed, Combination.NONE);
  }
}
