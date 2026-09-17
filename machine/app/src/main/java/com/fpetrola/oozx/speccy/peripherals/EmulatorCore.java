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

package com.fpetrola.oozx.speccy.peripherals;

import com.fpetrola.oozx.EmulatorControl;
import com.fpetrola.oozx.speccy.pokes.PokFile;

import javax.swing.JComponent;
import java.awt.event.KeyListener;

/** A machine as a window has it: everything you can do to one, plus the picture and the keys. */
public interface EmulatorCore extends EmulatorControl {
  JComponent getPanel();

  /**
   * The devices that said they have settings, with somewhere to read and write them. A machine
   * answers with its own devices; what stands for the defaults answers with the file.
   */
  default java.util.List<com.fpetrola.oozx.config.Settings.Configurable> deviceSettings() {
    return java.util.List.of();
  }

  KeyListener getKeyListener();

  default void applyMod(PokFile.PokeMod mod) {
  }

  default void revertMod(PokFile.PokeMod mod) {
  }
}
