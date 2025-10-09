/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse;

import com.fpetrola.oozx.LibspectrumStartupModule;
import com.fpetrola.oozx.SetUidStartupModule;
import com.fpetrola.oozx.StartupManagerModule;

public class KeyboardStartupModule extends AbstractStartupModule {
  public KeyboardStartupModule() {
    super(LibspectrumStartupModule.class, SetUidStartupModule.class);
  }

  public Object getInitContext() {
    return null;
  }

  public int initFn(Object initContext) {
    Keyboard.releaseAll();

    // Populate keyboard_data
    for (KeyInfo entry : Keyboard.KEYBOARD_DATA_TABLE) {
      if (entry.key == KeyboardKeyName.KEYBOARD_NONE) break;
      Keyboard.keyboardData.put(entry.key.getValue(), entry.bit);
    }

    // Populate spectrum_keys
    for (SpectrumKeysWrapper entry : Keyboard.SPECTRUM_KEYS_TABLE) {
      if (entry.input == Input.InputKey.INPUT_KEY_NONE) break;
      int value = entry.input.getValue();
      Keyboard.spectrumKeys.put(value, entry.spectrum);
    }

    Keyboard.KEYSYMS_MAP = SwingKeyboard.KEYSYMS_MAP;
    // Populate keysyms_hash
    for (KeysymsMap entry : Keyboard.KEYSYMS_MAP) {
      if (entry.ui == 0) break;
      Keyboard.keysymsHash.put(entry.ui, entry.fuse);
    }

    // Populate key_text
    for (KeyText entry : Keyboard.KEY_TEXT_TABLE) {
      if (entry.text == null) break;
      Keyboard.keyText.put(entry.key.getValue(), entry.text);
    }

    return 0;
  }

  public void endFn() {
    Keyboard.end();
  }

  public StartupManagerModule getStartupManagerModule() {
    return StartupManagerModule.KEYBOARD;
  }
}
