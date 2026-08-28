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

package com.fpetrola.oozx.speccy.modules;

import com.google.inject.Singleton;

import com.fpetrola.oozx.speccy.*;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class Keyboard implements ZxModule {

  // Static fields
  public byte[] returnValues = new byte[8]; // keyboard_return_values
  public byte defaultValue = (byte) 0xFF; // keyboard_default_value
  Map<Integer, KeyBit> keyboardData = new HashMap<>();
  Map<Integer, SpectrumKeys> spectrumKeys = new HashMap<>();
  Map<Integer, Input.InputKey> keysymsHash = new HashMap<>();
  Map<Integer, String> keyText = new HashMap<>();

  // Initialize keyboard

  // Clean up keyboard
  public void end() {
    keyboardData.clear();
    spectrumKeys.clear();
    keysymsHash.clear();
    keyText.clear();
  }

  // Register startup

  // Read keyboard port
  public byte read(byte porth) {
    byte data = (byte) 0xFF;
    for (int i = 0; i < 8; i++, porth >>= 1) {
      if ((porth & 0x01) == 0) {
        data &= returnValues[i];
      }
    }
    return data;
  }

  // Press a key
  public void press(KeyboardKeyName key) {
    KeyBit ptr = keyboardData.get(key.getValue());
    if (ptr != null) {
      returnValues[ptr.port] &= ~ptr.bit;
    }
  }

  // Release a key
  public void release(KeyboardKeyName key) {
    KeyBit ptr = keyboardData.get(key.getValue());
    if (ptr != null) {
      returnValues[ptr.port] |= ptr.bit;
    }
  }

  // Release all keys
  public int releaseAll() {
    for (int i = 0; i < 8; i++) {
      returnValues[i] = (byte) 0xFF;
    }
    return 0;
  }

  // Get Spectrum keys for input key
  public SpectrumKeys getSpectrumKeys(Input.InputKey keysym) {
    return spectrumKeys.get(keysym.getValue());
  }

  // Remap UI keysym to Speccy input key
  public Input.InputKey remap(int uiKeysym) {
    return keysymsHash.getOrDefault(uiKeysym, Input.InputKey.INPUT_KEY_NONE);
  }

  // Get textual representation of a key
  public String keyText(KeyboardKeyName key) {
    return keyText.getOrDefault(key.getValue(), "[Unknown key]");
  }

  // Simulate keypress for ULA read
  public byte simulateKeypress(byte porth, KeyboardKeyName key) {
    byte r = (byte) 0xFF;
    KeyBit data = keyboardData.get(key.getValue());
    if (data != null) {
      byte mask = (byte) (1 << data.port);
      if ((porth & mask) == 0) {
        r &= ~data.bit;
      }
    }
    return r;
  }

  // Key mapping tables
  static final SpectrumKeysWrapper[] SPECTRUM_KEYS_TABLE = {
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Escape, new SpectrumKeys(KeyboardKeyName.KEYBOARD_1, KeyboardKeyName.KEYBOARD_Caps)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_1, new SpectrumKeys(KeyboardKeyName.KEYBOARD_1, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_2, new SpectrumKeys(KeyboardKeyName.KEYBOARD_2, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_3, new SpectrumKeys(KeyboardKeyName.KEYBOARD_3, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_4, new SpectrumKeys(KeyboardKeyName.KEYBOARD_4, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_5, new SpectrumKeys(KeyboardKeyName.KEYBOARD_5, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_6, new SpectrumKeys(KeyboardKeyName.KEYBOARD_6, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_7, new SpectrumKeys(KeyboardKeyName.KEYBOARD_7, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_8, new SpectrumKeys(KeyboardKeyName.KEYBOARD_8, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_9, new SpectrumKeys(KeyboardKeyName.KEYBOARD_9, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_0, new SpectrumKeys(KeyboardKeyName.KEYBOARD_0, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_minus, new SpectrumKeys(KeyboardKeyName.KEYBOARD_j, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_equal, new SpectrumKeys(KeyboardKeyName.KEYBOARD_l, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_BackSpace, new SpectrumKeys(KeyboardKeyName.KEYBOARD_0, KeyboardKeyName.KEYBOARD_Caps)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Tab, new SpectrumKeys(KeyboardKeyName.KEYBOARD_Caps, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_q, new SpectrumKeys(KeyboardKeyName.KEYBOARD_q, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_w, new SpectrumKeys(KeyboardKeyName.KEYBOARD_w, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_e, new SpectrumKeys(KeyboardKeyName.KEYBOARD_e, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_r, new SpectrumKeys(KeyboardKeyName.KEYBOARD_r, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_t, new SpectrumKeys(KeyboardKeyName.KEYBOARD_t, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_y, new SpectrumKeys(KeyboardKeyName.KEYBOARD_y, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_u, new SpectrumKeys(KeyboardKeyName.KEYBOARD_u, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_i, new SpectrumKeys(KeyboardKeyName.KEYBOARD_i, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_o, new SpectrumKeys(KeyboardKeyName.KEYBOARD_o, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_p, new SpectrumKeys(KeyboardKeyName.KEYBOARD_p, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Caps_Lock, new SpectrumKeys(KeyboardKeyName.KEYBOARD_2, KeyboardKeyName.KEYBOARD_Caps)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_a, new SpectrumKeys(KeyboardKeyName.KEYBOARD_a, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_s, new SpectrumKeys(KeyboardKeyName.KEYBOARD_s, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_d, new SpectrumKeys(KeyboardKeyName.KEYBOARD_d, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_f, new SpectrumKeys(KeyboardKeyName.KEYBOARD_f, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_g, new SpectrumKeys(KeyboardKeyName.KEYBOARD_g, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_h, new SpectrumKeys(KeyboardKeyName.KEYBOARD_h, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_j, new SpectrumKeys(KeyboardKeyName.KEYBOARD_j, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_k, new SpectrumKeys(KeyboardKeyName.KEYBOARD_k, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_l, new SpectrumKeys(KeyboardKeyName.KEYBOARD_l, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_semicolon, new SpectrumKeys(KeyboardKeyName.KEYBOARD_o, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_apostrophe, new SpectrumKeys(KeyboardKeyName.KEYBOARD_7, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_numbersign, new SpectrumKeys(KeyboardKeyName.KEYBOARD_3, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Return, new SpectrumKeys(KeyboardKeyName.KEYBOARD_Enter, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Shift_L, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Caps)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_z, new SpectrumKeys(KeyboardKeyName.KEYBOARD_z, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_x, new SpectrumKeys(KeyboardKeyName.KEYBOARD_x, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_c, new SpectrumKeys(KeyboardKeyName.KEYBOARD_c, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_v, new SpectrumKeys(KeyboardKeyName.KEYBOARD_v, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_b, new SpectrumKeys(KeyboardKeyName.KEYBOARD_b, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_n, new SpectrumKeys(KeyboardKeyName.KEYBOARD_n, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_m, new SpectrumKeys(KeyboardKeyName.KEYBOARD_m, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_comma, new SpectrumKeys(KeyboardKeyName.KEYBOARD_n, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_period, new SpectrumKeys(KeyboardKeyName.KEYBOARD_m, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_slash, new SpectrumKeys(KeyboardKeyName.KEYBOARD_v, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Shift_R, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Caps)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_asterisk, new SpectrumKeys(KeyboardKeyName.KEYBOARD_b, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_dollar, new SpectrumKeys(KeyboardKeyName.KEYBOARD_4, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_exclam, new SpectrumKeys(KeyboardKeyName.KEYBOARD_1, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_less, new SpectrumKeys(KeyboardKeyName.KEYBOARD_r, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_parenright, new SpectrumKeys(KeyboardKeyName.KEYBOARD_9, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_colon, new SpectrumKeys(KeyboardKeyName.KEYBOARD_z, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_plus, new SpectrumKeys(KeyboardKeyName.KEYBOARD_k, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Control_L, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Alt_L, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Meta_L, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Super_L, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Hyper_L, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_space, new SpectrumKeys(KeyboardKeyName.KEYBOARD_space, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Hyper_R, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Super_R, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Meta_R, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Alt_R, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Control_R, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Mode_switch, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_Symbol)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Left, new SpectrumKeys(KeyboardKeyName.KEYBOARD_5, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Down, new SpectrumKeys(KeyboardKeyName.KEYBOARD_6, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Up, new SpectrumKeys(KeyboardKeyName.KEYBOARD_7, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_Right, new SpectrumKeys(KeyboardKeyName.KEYBOARD_8, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_KP_Enter, new SpectrumKeys(KeyboardKeyName.KEYBOARD_Enter, KeyboardKeyName.KEYBOARD_NONE)),
      new SpectrumKeysWrapper(Input.InputKey.INPUT_KEY_NONE, new SpectrumKeys(KeyboardKeyName.KEYBOARD_NONE, KeyboardKeyName.KEYBOARD_NONE))
  };

  static final KeyInfo[] KEYBOARD_DATA_TABLE = {
      new KeyInfo(KeyboardKeyName.KEYBOARD_1, new KeyBit(3, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_2, new KeyBit(3, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_3, new KeyBit(3, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_4, new KeyBit(3, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_5, new KeyBit(3, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_6, new KeyBit(4, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_7, new KeyBit(4, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_8, new KeyBit(4, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_9, new KeyBit(4, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_0, new KeyBit(4, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_q, new KeyBit(2, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_w, new KeyBit(2, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_e, new KeyBit(2, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_r, new KeyBit(2, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_t, new KeyBit(2, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_y, new KeyBit(5, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_u, new KeyBit(5, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_i, new KeyBit(5, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_o, new KeyBit(5, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_p, new KeyBit(5, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_a, new KeyBit(1, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_s, new KeyBit(1, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_d, new KeyBit(1, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_f, new KeyBit(1, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_g, new KeyBit(1, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_h, new KeyBit(6, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_j, new KeyBit(6, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_k, new KeyBit(6, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_l, new KeyBit(6, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_Enter, new KeyBit(6, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_Caps, new KeyBit(0, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_z, new KeyBit(0, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_x, new KeyBit(0, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_c, new KeyBit(0, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_v, new KeyBit(0, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_b, new KeyBit(7, (byte) 0x10)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_n, new KeyBit(7, (byte) 0x08)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_m, new KeyBit(7, (byte) 0x04)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_Symbol, new KeyBit(7, (byte) 0x02)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_space, new KeyBit(7, (byte) 0x01)),
      new KeyInfo(KeyboardKeyName.KEYBOARD_NONE, new KeyBit(0, (byte) 0x00))
  };

  static final KeyText[] KEY_TEXT_TABLE = {
      new KeyText(KeyboardKeyName.KEYBOARD_NONE, "Nothing"),
      new KeyText(KeyboardKeyName.KEYBOARD_space, "Space"),
      new KeyText(KeyboardKeyName.KEYBOARD_0, "0"),
      new KeyText(KeyboardKeyName.KEYBOARD_1, "1"),
      new KeyText(KeyboardKeyName.KEYBOARD_2, "2"),
      new KeyText(KeyboardKeyName.KEYBOARD_3, "3"),
      new KeyText(KeyboardKeyName.KEYBOARD_4, "4"),
      new KeyText(KeyboardKeyName.KEYBOARD_5, "5"),
      new KeyText(KeyboardKeyName.KEYBOARD_6, "6"),
      new KeyText(KeyboardKeyName.KEYBOARD_7, "7"),
      new KeyText(KeyboardKeyName.KEYBOARD_8, "8"),
      new KeyText(KeyboardKeyName.KEYBOARD_9, "9"),
      new KeyText(KeyboardKeyName.KEYBOARD_a, "A"),
      new KeyText(KeyboardKeyName.KEYBOARD_b, "B"),
      new KeyText(KeyboardKeyName.KEYBOARD_c, "C"),
      new KeyText(KeyboardKeyName.KEYBOARD_d, "D"),
      new KeyText(KeyboardKeyName.KEYBOARD_e, "E"),
      new KeyText(KeyboardKeyName.KEYBOARD_f, "F"),
      new KeyText(KeyboardKeyName.KEYBOARD_g, "G"),
      new KeyText(KeyboardKeyName.KEYBOARD_h, "H"),
      new KeyText(KeyboardKeyName.KEYBOARD_i, "I"),
      new KeyText(KeyboardKeyName.KEYBOARD_j, "J"),
      new KeyText(KeyboardKeyName.KEYBOARD_k, "K"),
      new KeyText(KeyboardKeyName.KEYBOARD_l, "L"),
      new KeyText(KeyboardKeyName.KEYBOARD_m, "M"),
      new KeyText(KeyboardKeyName.KEYBOARD_n, "N"),
      new KeyText(KeyboardKeyName.KEYBOARD_o, "O"),
      new KeyText(KeyboardKeyName.KEYBOARD_p, "P"),
      new KeyText(KeyboardKeyName.KEYBOARD_q, "Q"),
      new KeyText(KeyboardKeyName.KEYBOARD_r, "R"),
      new KeyText(KeyboardKeyName.KEYBOARD_s, "S"),
      new KeyText(KeyboardKeyName.KEYBOARD_t, "T"),
      new KeyText(KeyboardKeyName.KEYBOARD_u, "U"),
      new KeyText(KeyboardKeyName.KEYBOARD_v, "V"),
      new KeyText(KeyboardKeyName.KEYBOARD_w, "W"),
      new KeyText(KeyboardKeyName.KEYBOARD_x, "X"),
      new KeyText(KeyboardKeyName.KEYBOARD_y, "Y"),
      new KeyText(KeyboardKeyName.KEYBOARD_z, "Z"),
      new KeyText(KeyboardKeyName.KEYBOARD_Enter, "Enter"),
      new KeyText(KeyboardKeyName.KEYBOARD_Caps, "Caps Shift"),
      new KeyText(KeyboardKeyName.KEYBOARD_Symbol, "Symbol Shift"),
      new KeyText(KeyboardKeyName.KEYBOARD_JOYSTICK_FIRE, "Joystick Fire"),
      new KeyText(KeyboardKeyName.KEYBOARD_NONE, null)
  };

  // Placeholder for keysyms_map (to be populated based on UI-specific keysyms)
  static KeysymsMap[] KEYSYMS_MAP = {};

  public int init(Object initContext) {
    releaseAll();

    // Populate keyboard_data
    for (KeyInfo entry : KEYBOARD_DATA_TABLE) {
      if (entry.key == KeyboardKeyName.KEYBOARD_NONE) break;
      keyboardData.put(entry.key.getValue(), entry.bit);
    }

    // Populate spectrum_keys
    for (SpectrumKeysWrapper entry : SPECTRUM_KEYS_TABLE) {
      if (entry.input == Input.InputKey.INPUT_KEY_NONE) break;
      int value = entry.input.getValue();
      spectrumKeys.put(value, entry.spectrum);
    }

    KEYSYMS_MAP = SwingKeyboard.KEYSYMS_MAP;
    // Populate keysyms_hash
    for (KeysymsMap entry : KEYSYMS_MAP) {
      if (entry.ui == 0) break;
      keysymsHash.put(entry.ui, entry.speccy);
    }

    // Populate key_text
    for (KeyText entry : KEY_TEXT_TABLE) {
      if (entry.text == null) break;
      keyText.put(entry.key.getValue(), entry.text);
    }

    return 0;
  }
}
