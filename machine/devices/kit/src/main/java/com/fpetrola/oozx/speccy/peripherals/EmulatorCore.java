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
import com.fpetrola.oozx.config.Settings;

import javax.swing.JComponent;
import java.awt.event.KeyListener;
import java.util.List;

/** A machine as a window has it: everything you can do to one, plus the picture and the keys. */
public interface EmulatorCore extends EmulatorControl {

  /** The machine this core drives; none for one that only stands for the defaults or for a test. */
  default java.util.Optional<com.fpetrola.oozx.Speccy> machine() {
    return java.util.Optional.empty();
  }
  JComponent getPanel();

  /**
   * The devices that said they have settings, with somewhere to read and write them. A machine
   * answers with its own devices; what stands for the defaults answers with the file.
   */
  default java.util.List<Settings.Configurable> deviceSettings() {
    return java.util.List.of();
  }

  /**
   * The machine itself as settings, beside the ones its devices have: which Spectrum it is, which
   * of its ROM sets it runs on, which processor runs it, and whether it runs as fast as it can.
   * <p>
   * Said the same way a device says what it has, so that these go through whatever a window does
   * with the rest - held until they are applied, written onto whichever machine that window is on
   * - instead of being four controls written out by hand that write where they are pointed at
   * once. Not a mirror of fields: each one is a question this already answers, and being told one
   * is something the machine does - becoming another Spectrum, switching its ROMs - rather than a
   * value changing.
   */
  default Settings.Configurable ownSettings() {
    return Settings.of("machine.hardware",
        new Settings.Property("model", String.class, "Which Spectrum this is, changed under the game that is running",
            this::getMachineModels, this::getCurrentModel, value -> setMachineModel(String.valueOf(value))),
        new Settings.Property("romSet", String.class,
            "The set of ROMs this model can be run with, where it has more than one",
            this::getRomSets, this::getRomSet, value -> setRomSet(String.valueOf(value))),
        new Settings.Property("processor", String.class, "The implementation the machine runs on: the one generated"
            + " from the model, or the model itself, which is the one to debug",
            this::getProcessors, this::getProcessor, value -> setProcessor(String.valueOf(value))),
        new Settings.Property("turbo", boolean.class, "As fast as it can rather than as fast as a Spectrum",
            List::of, this::isTurboMode, value -> setGeneralOption("turbo", value)));
  }

  KeyListener getKeyListener();
}
