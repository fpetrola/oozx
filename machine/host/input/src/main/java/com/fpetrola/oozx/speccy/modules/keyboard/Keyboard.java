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
