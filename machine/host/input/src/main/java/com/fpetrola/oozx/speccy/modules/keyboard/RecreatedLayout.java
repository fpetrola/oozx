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
  private final Map<Integer, InputKey> presses = new HashMap<>();
  private final Map<Integer, InputKey> releases = new HashMap<>();
  private final KeyLayout keys = new PcLayout();
  private int typed;
  private InputKey produced = InputKey.INPUT_KEY_NONE;
  private boolean letGo;

  {
    presses.put(InputKey.INPUT_KEY_a.getValue(), InputKey.INPUT_KEY_1);
    presses.put(InputKey.INPUT_KEY_c.getValue(), InputKey.INPUT_KEY_2);
    presses.put(InputKey.INPUT_KEY_e.getValue(), InputKey.INPUT_KEY_3);
    presses.put(InputKey.INPUT_KEY_g.getValue(), InputKey.INPUT_KEY_4);
    presses.put(InputKey.INPUT_KEY_i.getValue(), InputKey.INPUT_KEY_5);
    presses.put(InputKey.INPUT_KEY_k.getValue(), InputKey.INPUT_KEY_6);
    presses.put(InputKey.INPUT_KEY_m.getValue(), InputKey.INPUT_KEY_7);
    presses.put(InputKey.INPUT_KEY_o.getValue(), InputKey.INPUT_KEY_8);
    presses.put(InputKey.INPUT_KEY_q.getValue(), InputKey.INPUT_KEY_9);
    presses.put(InputKey.INPUT_KEY_s.getValue(), InputKey.INPUT_KEY_0);
    presses.put(InputKey.INPUT_KEY_u.getValue(), InputKey.INPUT_KEY_q);
    presses.put(InputKey.INPUT_KEY_w.getValue(), InputKey.INPUT_KEY_w);
    presses.put(InputKey.INPUT_KEY_y.getValue(), InputKey.INPUT_KEY_e);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_a.getValue(), InputKey.INPUT_KEY_r);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_c.getValue(), InputKey.INPUT_KEY_t);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_e.getValue(), InputKey.INPUT_KEY_y);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_g.getValue(), InputKey.INPUT_KEY_u);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_i.getValue(), InputKey.INPUT_KEY_i);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_k.getValue(), InputKey.INPUT_KEY_o);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_m.getValue(), InputKey.INPUT_KEY_p);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_o.getValue(), InputKey.INPUT_KEY_a);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_q.getValue(), InputKey.INPUT_KEY_s);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_s.getValue(), InputKey.INPUT_KEY_d);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_u.getValue(), InputKey.INPUT_KEY_f);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_w.getValue(), InputKey.INPUT_KEY_g);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_y.getValue(), InputKey.INPUT_KEY_h);
    presses.put(InputKey.INPUT_KEY_0.getValue(), InputKey.INPUT_KEY_j);
    presses.put(InputKey.INPUT_KEY_2.getValue(), InputKey.INPUT_KEY_k);
    presses.put(InputKey.INPUT_KEY_4.getValue(), InputKey.INPUT_KEY_l);
    presses.put(InputKey.INPUT_KEY_6.getValue(), InputKey.INPUT_KEY_Return);
    presses.put(InputKey.INPUT_KEY_8.getValue(), InputKey.INPUT_KEY_Shift_L);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_comma.getValue(), InputKey.INPUT_KEY_z);
    presses.put(InputKey.INPUT_KEY_minus.getValue(), InputKey.INPUT_KEY_x);
    presses.put(InputKey.INPUT_KEY_bracketleft.getValue(), InputKey.INPUT_KEY_c);
    presses.put(InputKey.INPUT_KEY_semicolon.getValue(), InputKey.INPUT_KEY_v);
    presses.put(InputKey.INPUT_KEY_comma.getValue(), InputKey.INPUT_KEY_b);
    presses.put(InputKey.INPUT_KEY_slash.getValue(), InputKey.INPUT_KEY_n);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_bracketleft.getValue(), InputKey.INPUT_KEY_m);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_1.getValue(), InputKey.INPUT_KEY_Control_R);
    presses.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_5.getValue(), InputKey.INPUT_KEY_space);
    releases.put(InputKey.INPUT_KEY_b.getValue(), InputKey.INPUT_KEY_1);
    releases.put(InputKey.INPUT_KEY_d.getValue(), InputKey.INPUT_KEY_2);
    releases.put(InputKey.INPUT_KEY_f.getValue(), InputKey.INPUT_KEY_3);
    releases.put(InputKey.INPUT_KEY_h.getValue(), InputKey.INPUT_KEY_4);
    releases.put(InputKey.INPUT_KEY_j.getValue(), InputKey.INPUT_KEY_5);
    releases.put(InputKey.INPUT_KEY_l.getValue(), InputKey.INPUT_KEY_6);
    releases.put(InputKey.INPUT_KEY_n.getValue(), InputKey.INPUT_KEY_7);
    releases.put(InputKey.INPUT_KEY_p.getValue(), InputKey.INPUT_KEY_8);
    releases.put(InputKey.INPUT_KEY_r.getValue(), InputKey.INPUT_KEY_9);
    releases.put(InputKey.INPUT_KEY_t.getValue(), InputKey.INPUT_KEY_0);
    releases.put(InputKey.INPUT_KEY_v.getValue(), InputKey.INPUT_KEY_q);
    releases.put(InputKey.INPUT_KEY_x.getValue(), InputKey.INPUT_KEY_w);
    releases.put(InputKey.INPUT_KEY_z.getValue(), InputKey.INPUT_KEY_e);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_b.getValue(), InputKey.INPUT_KEY_r);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_d.getValue(), InputKey.INPUT_KEY_t);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_f.getValue(), InputKey.INPUT_KEY_y);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_h.getValue(), InputKey.INPUT_KEY_u);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_j.getValue(), InputKey.INPUT_KEY_i);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_l.getValue(), InputKey.INPUT_KEY_o);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_n.getValue(), InputKey.INPUT_KEY_p);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_p.getValue(), InputKey.INPUT_KEY_a);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_r.getValue(), InputKey.INPUT_KEY_s);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_t.getValue(), InputKey.INPUT_KEY_d);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_v.getValue(), InputKey.INPUT_KEY_f);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_x.getValue(), InputKey.INPUT_KEY_g);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_z.getValue(), InputKey.INPUT_KEY_h);
    releases.put(InputKey.INPUT_KEY_1.getValue(), InputKey.INPUT_KEY_j);
    releases.put(InputKey.INPUT_KEY_3.getValue(), InputKey.INPUT_KEY_k);
    releases.put(InputKey.INPUT_KEY_5.getValue(), InputKey.INPUT_KEY_l);
    releases.put(InputKey.INPUT_KEY_7.getValue(), InputKey.INPUT_KEY_Return);
    releases.put(InputKey.INPUT_KEY_9.getValue(), InputKey.INPUT_KEY_Shift_L);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_period.getValue(), InputKey.INPUT_KEY_z);
    releases.put(InputKey.INPUT_KEY_equal.getValue(), InputKey.INPUT_KEY_x);
    releases.put(InputKey.INPUT_KEY_bracketright.getValue(), InputKey.INPUT_KEY_c);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_semicolon.getValue(), InputKey.INPUT_KEY_v);
    releases.put(InputKey.INPUT_KEY_period.getValue(), InputKey.INPUT_KEY_b);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_slash.getValue(), InputKey.INPUT_KEY_n);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_bracketright.getValue(), InputKey.INPUT_KEY_m);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_4.getValue(), InputKey.INPUT_KEY_Control_R);
    releases.put(InputKey.INPUT_KEY_Shift_L.getValue() | InputKey.INPUT_KEY_6.getValue(), InputKey.INPUT_KEY_space);
  }

  @Override
  public void pressed(InputKey key) {
    if (key == InputKey.INPUT_KEY_Shift_L) {
      typed |= InputKey.INPUT_KEY_Shift_L.getValue();
    }
    if (key.getValue() >= 0 && key.getValue() < 256) {
      typed = (typed & ~255) | key.getValue();
    }
    produced = releases.getOrDefault(typed, InputKey.INPUT_KEY_NONE);
    letGo = produced != InputKey.INPUT_KEY_NONE;
    if (!letGo) {
      produced = presses.getOrDefault(typed, InputKey.INPUT_KEY_NONE);
    }
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
}
