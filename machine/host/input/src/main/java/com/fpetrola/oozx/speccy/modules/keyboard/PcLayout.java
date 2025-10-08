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

import com.fpetrola.oozx.speccy.modules.input.Input;

import java.util.EnumMap;
import java.util.Map;

/** The keys where a PC keyboard has them: a letter is that letter, and what a Spectrum needs a shift for gets one. */
public class PcLayout implements KeyLayout {
  private final Map<Input.InputKey, Combination> produces = new EnumMap<>(Input.InputKey.class);

  public PcLayout() {
    put(Input.InputKey.INPUT_KEY_Escape, Combination.shifted(SpectrumKey.CAPS_SHIFT, SpectrumKey.ONE));
    put(Input.InputKey.INPUT_KEY_1, Combination.of(SpectrumKey.ONE));
    put(Input.InputKey.INPUT_KEY_2, Combination.of(SpectrumKey.TWO));
    put(Input.InputKey.INPUT_KEY_3, Combination.of(SpectrumKey.THREE));
    put(Input.InputKey.INPUT_KEY_4, Combination.of(SpectrumKey.FOUR));
    put(Input.InputKey.INPUT_KEY_5, Combination.of(SpectrumKey.FIVE));
    put(Input.InputKey.INPUT_KEY_6, Combination.of(SpectrumKey.SIX));
    put(Input.InputKey.INPUT_KEY_7, Combination.of(SpectrumKey.SEVEN));
    put(Input.InputKey.INPUT_KEY_8, Combination.of(SpectrumKey.EIGHT));
    put(Input.InputKey.INPUT_KEY_9, Combination.of(SpectrumKey.NINE));
    put(Input.InputKey.INPUT_KEY_0, Combination.of(SpectrumKey.ZERO));
    put(Input.InputKey.INPUT_KEY_minus, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.J));
    put(Input.InputKey.INPUT_KEY_equal, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.L));
    put(Input.InputKey.INPUT_KEY_BackSpace, Combination.shifted(SpectrumKey.CAPS_SHIFT, SpectrumKey.ZERO));
    put(Input.InputKey.INPUT_KEY_Tab, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.CAPS_SHIFT));
    put(Input.InputKey.INPUT_KEY_q, Combination.of(SpectrumKey.Q));
    put(Input.InputKey.INPUT_KEY_w, Combination.of(SpectrumKey.W));
    put(Input.InputKey.INPUT_KEY_e, Combination.of(SpectrumKey.E));
    put(Input.InputKey.INPUT_KEY_r, Combination.of(SpectrumKey.R));
    put(Input.InputKey.INPUT_KEY_t, Combination.of(SpectrumKey.T));
    put(Input.InputKey.INPUT_KEY_y, Combination.of(SpectrumKey.Y));
    put(Input.InputKey.INPUT_KEY_u, Combination.of(SpectrumKey.U));
    put(Input.InputKey.INPUT_KEY_i, Combination.of(SpectrumKey.I));
    put(Input.InputKey.INPUT_KEY_o, Combination.of(SpectrumKey.O));
    put(Input.InputKey.INPUT_KEY_p, Combination.of(SpectrumKey.P));
    put(Input.InputKey.INPUT_KEY_Caps_Lock, Combination.shifted(SpectrumKey.CAPS_SHIFT, SpectrumKey.TWO));
    put(Input.InputKey.INPUT_KEY_a, Combination.of(SpectrumKey.A));
    put(Input.InputKey.INPUT_KEY_s, Combination.of(SpectrumKey.S));
    put(Input.InputKey.INPUT_KEY_d, Combination.of(SpectrumKey.D));
    put(Input.InputKey.INPUT_KEY_f, Combination.of(SpectrumKey.F));
    put(Input.InputKey.INPUT_KEY_g, Combination.of(SpectrumKey.G));
    put(Input.InputKey.INPUT_KEY_h, Combination.of(SpectrumKey.H));
    put(Input.InputKey.INPUT_KEY_j, Combination.of(SpectrumKey.J));
    put(Input.InputKey.INPUT_KEY_k, Combination.of(SpectrumKey.K));
    put(Input.InputKey.INPUT_KEY_l, Combination.of(SpectrumKey.L));
    put(Input.InputKey.INPUT_KEY_semicolon, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.O));
    put(Input.InputKey.INPUT_KEY_apostrophe, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.SEVEN));
    put(Input.InputKey.INPUT_KEY_numbersign, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.THREE));
    put(Input.InputKey.INPUT_KEY_Return, Combination.of(SpectrumKey.ENTER));
    put(Input.InputKey.INPUT_KEY_Shift_L, Combination.of(SpectrumKey.CAPS_SHIFT));
    put(Input.InputKey.INPUT_KEY_z, Combination.of(SpectrumKey.Z));
    put(Input.InputKey.INPUT_KEY_x, Combination.of(SpectrumKey.X));
    put(Input.InputKey.INPUT_KEY_c, Combination.of(SpectrumKey.C));
    put(Input.InputKey.INPUT_KEY_v, Combination.of(SpectrumKey.V));
    put(Input.InputKey.INPUT_KEY_b, Combination.of(SpectrumKey.B));
    put(Input.InputKey.INPUT_KEY_n, Combination.of(SpectrumKey.N));
    put(Input.InputKey.INPUT_KEY_m, Combination.of(SpectrumKey.M));
    put(Input.InputKey.INPUT_KEY_comma, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.N));
    put(Input.InputKey.INPUT_KEY_period, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.M));
    put(Input.InputKey.INPUT_KEY_slash, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.V));
    put(Input.InputKey.INPUT_KEY_Shift_R, Combination.of(SpectrumKey.CAPS_SHIFT));
    put(Input.InputKey.INPUT_KEY_asterisk, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.B));
    put(Input.InputKey.INPUT_KEY_dollar, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.FOUR));
    put(Input.InputKey.INPUT_KEY_exclam, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.ONE));
    put(Input.InputKey.INPUT_KEY_less, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.R));
    put(Input.InputKey.INPUT_KEY_parenright, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.NINE));
    put(Input.InputKey.INPUT_KEY_colon, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.Z));
    put(Input.InputKey.INPUT_KEY_plus, Combination.shifted(SpectrumKey.SYMBOL_SHIFT, SpectrumKey.K));
    put(Input.InputKey.INPUT_KEY_Control_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Alt_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Meta_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Super_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Hyper_L, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_space, Combination.of(SpectrumKey.SPACE));
    put(Input.InputKey.INPUT_KEY_Hyper_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Super_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Meta_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Alt_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Control_R, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Mode_switch, Combination.of(SpectrumKey.SYMBOL_SHIFT));
    put(Input.InputKey.INPUT_KEY_Left, Combination.of(SpectrumKey.FIVE));
    put(Input.InputKey.INPUT_KEY_Down, Combination.of(SpectrumKey.SIX));
    put(Input.InputKey.INPUT_KEY_Up, Combination.of(SpectrumKey.SEVEN));
    put(Input.InputKey.INPUT_KEY_Right, Combination.of(SpectrumKey.EIGHT));
    put(Input.InputKey.INPUT_KEY_KP_Enter, Combination.of(SpectrumKey.ENTER));
  }

  private void put(Input.InputKey host, Combination combination) {
    produces.put(host, combination);
  }

  public Combination produces(Input.InputKey pressed) {
    return produces.getOrDefault(pressed, Combination.NONE);
  }
}
