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

import com.fpetrola.oozx.speccy.modules.input.Input;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The Spectrum's keyboard: the {@link KeyMatrix} the machine reads, and the {@link KeyLayout}
 * that says what the keys of whoever is typing put down on it.
 * <p>
 * Two sides on purpose. Below, the machine asks {@link #read} and the emulator's own typists -
 * the tape loader, the joystick emulated on keys - press and release keys of the Spectrum.
 * Above, a key of the host arrives and the layout turns it into a {@link Combination}.
 */
@Singleton
public class Keyboard {
  private final KeyMatrix matrix;
  private KeyLayout layout = new PcLayout();

  @Inject
  public Keyboard(KeyMatrix matrix) {
    this.matrix = matrix;
  }

  /** The keys as the machine reads them. */
  public KeyMatrix matrix() {
    return matrix;
  }

  /** Which keyboard is being typed on: a PC's, or a Recreated ZX. */
  public void layout(KeyLayout layout) {
    this.layout = layout == null ? new PcLayout() : layout;
  }

  public KeyLayout layout() {
    return layout;
  }

  public byte read(int high) {
    return matrix.read(high);
  }

  public void press(SpectrumKey key) {
    matrix.press(key);
  }

  public void release(SpectrumKey key) {
    matrix.release(key);
  }

  public void releaseAll() {
    matrix.releaseAll();
  }

  /** What that key of the host puts down on the Spectrum. */
  public Combination produces(Input.InputKey pressed) {
    return layout.produces(pressed);
  }
}
