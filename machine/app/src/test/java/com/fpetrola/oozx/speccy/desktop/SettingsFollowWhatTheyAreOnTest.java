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

package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.speccy.config.OOZxConfiguration;
import com.fpetrola.oozx.speccy.peripherals.DefaultsCore;
import com.fpetrola.oozx.speccy.peripherals.MockEmulatorCore;
import com.fpetrola.oozx.speccy.windows.AttachedFrame;
import org.junit.jupiter.api.Test;

import javax.swing.JInternalFrame;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Whose settings these are is a question about where the window is, and applying them has to go
 * wherever it is now. Let go of a machine it was clipped onto, it configures what machines start
 * with, and applying it reached back into the machine it came off.
 */
class SettingsFollowWhatTheyAreOnTest {

  /** A machine that writes nothing anywhere and says what it was told. */
  private static class Listening extends MockEmulatorCore {
    private final List<String> told = new ArrayList<>();

    Listening() {
      super(null);
    }

    @Override
    public void setMachineModel(String model) {
      told.add("model=" + model);
    }

    @Override
    public void setGeneralOption(String option, Object value) {
      told.add(option + "=" + value);
    }
  }

  @Test
  void letGoOfTheMachineApplyingLeavesItAlone() {
    Listening machineCore = new Listening();
    OOZxConfiguration config = new OOZxConfiguration();
    SettingsInternalFrame settings =
        new SettingsInternalFrame(window -> machineCore, new DefaultsCore(config), config);

    JInternalFrame machine = new JInternalFrame("machine");
    machine.setBounds(60, 40, 520, 380);
    settings.setMachineWindow(machine);
    assertTrue(settings.isAttached(), "should arrive clipped onto the machine");
    machineCore.told.clear();

    // Carried away from it: what is set here is what machines start with from now on.
    settings.setBounds(900, 700, 680, 520);
    settings.snapIfNear();
    assertEquals(AttachedFrame.Dock.FREE, settings.dockedTo(), "should have let go");

    settings.apply();
    assertEquals(List.of(), machineCore.told,
        "the machine it came off should have been told nothing");
  }
}
