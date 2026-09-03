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

package model.tests.machine;

import com.fpetrola.oozx.EmulatorModule;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.Spectrum;
import com.fpetrola.oozx.speccy.Emulation;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the window agrees with the machine it is showing.
 * <p>
 * A 128K game under a label saying 48K is not cosmetic: it is the only thing telling anyone which
 * machine they got, and the machine is chosen for them - by the name of a tape, by a snapshot, or
 * from the box itself. The announcement used to be made by the one caller that remembered to, and
 * it was made after the machine had already changed while the test for whether to bother asked the
 * machine, so it never fired. It comes from the change now, so every way of changing it says so.
 */
class MachineLabelTest {

  private Speccy speccy() {
    Emulation.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    return speccy;
  }

  @Test
  void changingTheMachineTellsWhoeverIsWatching() {
    Speccy speccy = speccy();
    List<String> announced = new ArrayList<>();
    speccy.machine.addMachineChangeListener(machine -> announced.add(machine.getName()));

    speccy.machine.select(speccy.spec128);
    assertTrue(announced.contains("Spectrum 128K"), "becoming a 128K was not announced: " + announced);

    speccy.machine.select(speccy.pentagon);
    assertEquals("Pentagon", announced.get(announced.size() - 1), "and neither was the next one");
  }

  /**
   * A name is how the box addresses a machine, so two machines may not share one and every machine
   * must have one the box can find. The list the box used to hold was written out by hand and had
   * no +2A, no +3e and no NTSC in it, so choosing one of those selected nothing and left the label
   * naming the machine before.
   */
  @Test
  void everyMachineHasItsOwnName() {
    Speccy speccy = speccy();
    List<Spectrum> machines = speccy.machine.getMachineTypes();
    // Said out loud because everything below is a loop over this: an empty list passes every
    // assertion in here without touching a machine, and the box would be empty for the same reason.
    assertFalse(machines.isEmpty(), "no machines are registered at all");

    Set<String> names = new HashSet<>();
    for (SpectrumMachine machine : machines) {
      assertTrue(names.add(machine.getName()),
          machine.getClass().getSimpleName() + " shares its name with another machine: " + machine.getName());
    }
    assertEquals(machines.size(), names.size(), "one name per machine");
  }

  /**
   * The one list that is a copy, and the test that keeps it honest.
   * <p>
   * Anything offering machines before one has been built - the browser draws its menus long
   * before a machine exists - has nothing to ask, and building a whole emulator to read eight
   * names back off it is not a thing to do. So the module names them beside the bindings that
   * produce them, and this fails the moment the two disagree, which is what happened to the box
   * on the toolbar when its list was written out by hand and three machines were missing from it.
   */
  @Test
  void theNamesTheModuleDeclaresAreTheMachinesItBinds() {
    Set<String> declared = new HashSet<>(EmulatorModule.MODEL_NAMES);
    Set<String> built = new HashSet<>();
    speccy().machine.getMachineTypes().forEach(machine -> built.add(machine.getName()));

    assertEquals(built, declared,
        "the module's list of machine names is not the machines it binds");
  }

  /** What picking one from the box does: find it by its name, and become it. */
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
}
