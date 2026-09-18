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
package model.tests.ui;

import com.fpetrola.oozx.config.Settings;
import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.peripherals.DefaultsCore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The settings with no emulator open: everything a person can choose there has to be answerable
 * with no machine running, or configuring what machines will start as is not possible at all.
 */
class SettingsWithNoMachineTest {

  private final DefaultsCore defaults = new DefaultsCore(new OOZxConfiguration());

  @Test
  void everyChoiceIsOfferedWithNothingRunning() {
    assertTrue(defaults.getMachineModels().size() > 1, "the machines this build has");
    assertFalse(defaults.getProcessors().isEmpty(), "the processors this build has");
    assertFalse(defaults.deviceSettings().isEmpty(), "what the devices said they can be told");
    assertFalse(defaults.getRomSets().isEmpty(), "the ROM sets of the machine being configured");
  }

  /** What is changed with nothing in front of it is held, not written, until it is applied. */
  @Test
  void changesWaitForApply() {
    Settings.Configurable target = aBooleanSetting();
    String property = aBoolean(target);
    boolean was = Boolean.TRUE.equals(target.values().get(property));

    Settings.Edits edits = new Settings.Edits();
    Settings.Configurable shown = edits.over(List.of(target), false).get(0);
    shown.values().set(property, !was);

    assertEquals(!was, shown.values().get(property), "what the window shows");
    assertEquals(was, target.values().get(property), "what nobody has written yet");

    edits.applyTo(List.of(target));
    assertEquals(!was, Boolean.TRUE.equals(target.values().get(property)), "what was written");

    target.values().set(property, was);
  }

  /** Opened on something, the window holds its settings, and writing them puts them back. */
  @Test
  void whatItWasOpenedOnIsWhatItWrites() {
    Settings.Configurable target = aBooleanSetting();
    String property = aBoolean(target);
    boolean was = Boolean.TRUE.equals(target.values().get(property));

    Settings.Edits edits = new Settings.Edits();
    edits.copyFrom(List.of(target));
    target.values().set(property, !was);

    edits.applyTo(List.of(target));
    assertEquals(was, Boolean.TRUE.equals(target.values().get(property)));
  }

  /** The machine itself is declared like a device: the same controls, and the choices it offers. */
  @Test
  void theMachineItselfIsOneOfTheSettings() {
    Settings.Configurable hardware = defaults.ownSettings();
    assertEquals(List.of("model", "romSet", "processor", "turbo"), hardware.device().properties());
    assertEquals(defaults.getMachineModels(), hardware.device().choicesFor("model"));

    String was = String.valueOf(hardware.values().get("model"));
    try {
      hardware.values().set("model", "Spectrum 128K");
      assertEquals("Spectrum 128K", hardware.values().get("model"));
    } finally {
      // Set here it is set for every machine opened after this, which is what the setting is for:
      // left behind, the next test opens a 128K where it asked for whatever a Spectrum is.
      hardware.values().set("model", was);
    }
  }

  /** What a new machine becomes comes from the binding, and the binding comes from the file. */
  @Test
  void aNewMachineStartsAsWhatTheFileNames() {
    Object was = com.fpetrola.oozx.config.Configuration.shared().valueOf("machine", "model", String.class);
    try {
      com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "model", "Pentagon");
      assertEquals("Pentagon", com.fpetrola.oozx.Speccy.create().machine.defaultModel());

      com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "model", "no such machine");
      assertEquals("Spectrum 48K", com.fpetrola.oozx.Speccy.create().machine.defaultModel(),
          "nothing this build has by that name, so a Spectrum is a 48K");
    } finally {
      com.fpetrola.oozx.config.Configuration.shared().setValue("machine", "model", was);
    }
  }

  private Settings.Configurable aBooleanSetting() {
    return defaults.deviceSettings().stream().filter(one -> aBoolean(one) != null).findFirst()
        .orElseThrow(() -> new IllegalStateException("nothing declared a setting that is on or off"));
  }

  private static String aBoolean(Settings.Configurable of) {
    return of.device().properties().stream()
        .filter(property -> of.device().typeOf(property) == boolean.class).findFirst().orElse(null);
  }
}
