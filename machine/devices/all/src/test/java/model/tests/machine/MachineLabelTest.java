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

package model.tests.machine;

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: the machine-change announcement used to be an opt-in call some callers forgot,
 * and its own guard checked the already-changed machine so it never fired. Now the change
 * itself fires the notification, so every path that switches machines reports it.
 */
class MachineLabelTest {

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  @Test
  void changingTheMachineTellsWhoeverIsWatching() {
    Speccy speccy = speccy();
    List<String> announced = new ArrayList<>();
    speccy.machine.addMachineChangeListener(machine -> announced.add(machine.getName()));

    speccy.machine.select(speccy.machine.model(Spec128.class));
    assertTrue(announced.contains("Spectrum 128K"), "becoming a 128K was not announced: " + announced);

    speccy.machine.select(speccy.machine.model(SpecPlus3.class));
    assertEquals("Spectrum Plus 3", announced.get(announced.size() - 1), "and neither was the next one");
  }

  /** Every registered machine must have a unique name; a past hand-written model list omitted
   * the +2A, +3e and NTSC variants entirely. */
  @Test
  void everyMachineHasItsOwnName() {
    Speccy speccy = speccy();
    List<Spectrum> machines = speccy.machine.getMachineTypes();
    // Asserted explicitly: an empty machine list would vacuously pass every loop below.
    assertFalse(machines.isEmpty(), "no machines are registered at all");

    Set<String> names = new HashSet<>();
    for (SpectrumMachine machine : machines) {
      assertTrue(names.add(machine.getName()),
          machine.getClass().getSimpleName() + " shares its name with another machine: " + machine.getName());
    }
    assertEquals(machines.size(), names.size(), "one name per machine");
  }

  /** Every registered machine must be findable again by the exact name it reports. */
  @Test
  void aMachineCanBeFoundByTheNameTheBoxShows() {
    Speccy speccy = speccy();
    assertFalse(speccy.machine.getMachineTypes().isEmpty(), "no machines are registered at all");
    for (Spectrum wanted : speccy.machine.getMachineTypes()) {
      String name = wanted.getName();
      Spectrum found = speccy.machine.getMachineTypes().stream()
          .filter(type -> type.getName().equals(name)).findFirst().orElse(null);
      assertSame(wanted, found, "no machine answers to " + name);
    }
  }
  /** Regression: selecting an unregistered machine used to silently return 1 (unread by any
   * caller) and leave the current machine unchanged, instead of failing loudly. */
  @Test
  void askingForAMachineThisBuildHasNotSaysSo() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    SpectrumMachine notHere = (SpectrumMachine) Proxy.newProxyInstance(
        SpectrumMachine.class.getClassLoader(), new Class<?>[] {SpectrumMachine.class},
        (proxy, method, args) -> method.getName().equals("getName") ? "a Jupiter Ace" : null);

    IllegalStateException said = assertThrows(IllegalStateException.class, () -> speccy.machine.select(notHere));
    assertTrue(said.getMessage().contains("Jupiter Ace"), "it does not say which one: " + said.getMessage());
  }
}
