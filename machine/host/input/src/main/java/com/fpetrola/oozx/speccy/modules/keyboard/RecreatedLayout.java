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

import com.fpetrola.oozx.speccy.modules.input.Input.InputKey;

import java.util.HashMap;
import java.util.Map;

/**
 * The Recreated ZX Spectrum: a keyboard that does not send a key per key. It sends a letter, and
 * which Spectrum key that is depends on whether shift came with it; and a key is let go of by
 * being pressed again, not by being released.
 * <p>
 * So this layout reads what came before: {@link #pressed} takes each letter in, and what it
 * makes of it is a press of one key, or the release of the key that was down.
 */
public class RecreatedLayout implements KeyLayout {
  /** What a letter the keyboard sends means: a Spectrum key, pressed or let go of. */
  private record Sent(InputKey key, boolean letsGo) {
  }

  private final Map<Integer, Sent> sent = new HashMap<>();
  private final KeyLayout keys = new PcLayout();
  private int typed;
  private InputKey produced = InputKey.INPUT_KEY_NONE;
  private boolean letGo;

  {
    key(InputKey.INPUT_KEY_1, alone(InputKey.INPUT_KEY_a), alone(InputKey.INPUT_KEY_b));
    key(InputKey.INPUT_KEY_2, alone(InputKey.INPUT_KEY_c), alone(InputKey.INPUT_KEY_d));
    key(InputKey.INPUT_KEY_3, alone(InputKey.INPUT_KEY_e), alone(InputKey.INPUT_KEY_f));
    key(InputKey.INPUT_KEY_4, alone(InputKey.INPUT_KEY_g), alone(InputKey.INPUT_KEY_h));
    key(InputKey.INPUT_KEY_5, alone(InputKey.INPUT_KEY_i), alone(InputKey.INPUT_KEY_j));
    key(InputKey.INPUT_KEY_6, alone(InputKey.INPUT_KEY_k), alone(InputKey.INPUT_KEY_l));
    key(InputKey.INPUT_KEY_7, alone(InputKey.INPUT_KEY_m), alone(InputKey.INPUT_KEY_n));
    key(InputKey.INPUT_KEY_8, alone(InputKey.INPUT_KEY_o), alone(InputKey.INPUT_KEY_p));
    key(InputKey.INPUT_KEY_9, alone(InputKey.INPUT_KEY_q), alone(InputKey.INPUT_KEY_r));
    key(InputKey.INPUT_KEY_0, alone(InputKey.INPUT_KEY_s), alone(InputKey.INPUT_KEY_t));
    key(InputKey.INPUT_KEY_q, alone(InputKey.INPUT_KEY_u), alone(InputKey.INPUT_KEY_v));
    key(InputKey.INPUT_KEY_w, alone(InputKey.INPUT_KEY_w), alone(InputKey.INPUT_KEY_x));
    key(InputKey.INPUT_KEY_e, alone(InputKey.INPUT_KEY_y), alone(InputKey.INPUT_KEY_z));
    key(InputKey.INPUT_KEY_r, shifted(InputKey.INPUT_KEY_a), shifted(InputKey.INPUT_KEY_b));
    key(InputKey.INPUT_KEY_t, shifted(InputKey.INPUT_KEY_c), shifted(InputKey.INPUT_KEY_d));
    key(InputKey.INPUT_KEY_y, shifted(InputKey.INPUT_KEY_e), shifted(InputKey.INPUT_KEY_f));
    key(InputKey.INPUT_KEY_u, shifted(InputKey.INPUT_KEY_g), shifted(InputKey.INPUT_KEY_h));
    key(InputKey.INPUT_KEY_i, shifted(InputKey.INPUT_KEY_i), shifted(InputKey.INPUT_KEY_j));
    key(InputKey.INPUT_KEY_o, shifted(InputKey.INPUT_KEY_k), shifted(InputKey.INPUT_KEY_l));
    key(InputKey.INPUT_KEY_p, shifted(InputKey.INPUT_KEY_m), shifted(InputKey.INPUT_KEY_n));
    key(InputKey.INPUT_KEY_a, shifted(InputKey.INPUT_KEY_o), shifted(InputKey.INPUT_KEY_p));
    key(InputKey.INPUT_KEY_s, shifted(InputKey.INPUT_KEY_q), shifted(InputKey.INPUT_KEY_r));
    key(InputKey.INPUT_KEY_d, shifted(InputKey.INPUT_KEY_s), shifted(InputKey.INPUT_KEY_t));
    key(InputKey.INPUT_KEY_f, shifted(InputKey.INPUT_KEY_u), shifted(InputKey.INPUT_KEY_v));
    key(InputKey.INPUT_KEY_g, shifted(InputKey.INPUT_KEY_w), shifted(InputKey.INPUT_KEY_x));
    key(InputKey.INPUT_KEY_h, shifted(InputKey.INPUT_KEY_y), shifted(InputKey.INPUT_KEY_z));
    key(InputKey.INPUT_KEY_j, alone(InputKey.INPUT_KEY_0), alone(InputKey.INPUT_KEY_1));
    key(InputKey.INPUT_KEY_k, alone(InputKey.INPUT_KEY_2), alone(InputKey.INPUT_KEY_3));
    key(InputKey.INPUT_KEY_l, alone(InputKey.INPUT_KEY_4), alone(InputKey.INPUT_KEY_5));
    key(InputKey.INPUT_KEY_Return, alone(InputKey.INPUT_KEY_6), alone(InputKey.INPUT_KEY_7));
    key(InputKey.INPUT_KEY_Shift_L, alone(InputKey.INPUT_KEY_8), alone(InputKey.INPUT_KEY_9));
    key(InputKey.INPUT_KEY_z, shifted(InputKey.INPUT_KEY_comma), shifted(InputKey.INPUT_KEY_period));
    key(InputKey.INPUT_KEY_x, alone(InputKey.INPUT_KEY_minus), alone(InputKey.INPUT_KEY_equal));
    key(InputKey.INPUT_KEY_c, alone(InputKey.INPUT_KEY_bracketleft), alone(InputKey.INPUT_KEY_bracketright));
    key(InputKey.INPUT_KEY_v, alone(InputKey.INPUT_KEY_semicolon), shifted(InputKey.INPUT_KEY_semicolon));
    key(InputKey.INPUT_KEY_b, alone(InputKey.INPUT_KEY_comma), alone(InputKey.INPUT_KEY_period));
    key(InputKey.INPUT_KEY_n, alone(InputKey.INPUT_KEY_slash), shifted(InputKey.INPUT_KEY_slash));
    key(InputKey.INPUT_KEY_m, shifted(InputKey.INPUT_KEY_bracketleft), shifted(InputKey.INPUT_KEY_bracketright));
    key(InputKey.INPUT_KEY_Control_R, shifted(InputKey.INPUT_KEY_1), shifted(InputKey.INPUT_KEY_4));
    key(InputKey.INPUT_KEY_space, shifted(InputKey.INPUT_KEY_5), shifted(InputKey.INPUT_KEY_6));
  }

  /** The Spectrum key that one letter presses and another lets go of. */
  private void key(InputKey spectrumKey, int pressedBy, int letGoBy) {
    sent.put(pressedBy, new Sent(spectrumKey, false));
    sent.put(letGoBy, new Sent(spectrumKey, true));
  }

  private static int alone(InputKey letter) {
    return letter.getValue();
  }

  private static int shifted(InputKey letter) {
    return InputKey.INPUT_KEY_Shift_L.getValue() | letter.getValue();
  }

  @Override
  public void pressed(InputKey key) {
    if (key == InputKey.INPUT_KEY_Shift_L) {
      typed |= InputKey.INPUT_KEY_Shift_L.getValue();
    }
    if (key.getValue() >= 0 && key.getValue() < 256) {
      typed = (typed & ~255) | key.getValue();
    }
    Sent meant = sent.getOrDefault(typed, new Sent(InputKey.INPUT_KEY_NONE, false));
    produced = meant.key();
    letGo = meant.letsGo();
    if (produced != InputKey.INPUT_KEY_NONE) {
      typed = 0;
    }
  }

  @Override
  public Combination produces(InputKey pressed) {
    return keys.produces(produced);
  }

  @Override
  public boolean releasedByPressing(InputKey key) {
    return letGo;
  }

  @Override
  public boolean releasedByLettingGo() {
    return false;
  }
}
