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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.config.RomFiles;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import model.harness.MachineTest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same machine was sold with different ROMs in it: another language, a later revision. That is
 * not another machine - the hardware is the one we already have - so it is not in the list of
 * machines but in a list of its own, and choosing from it is choosing which ROMs this machine runs.
 * <p>
 * Which sets there are is what the build knows; which one is running is the person's, and is kept
 * with their settings.
 */
class TheRomsAMachineRunsOnTest extends MachineTest {
  private final Speccy speccy = silentMachine();

  /**
   * Its own settings, holding what the build carries: the running emulator's are one object for
   * the whole process, and choosing ROMs in here would be choosing them for everything else.
   */
  private final RomFiles roms = asTheBuildCarriesThem();

  private static RomFiles asTheBuildCarriesThem() {
    RomFiles mine = new RomFiles();
    mine.files.putAll(com.fpetrola.oozx.config.Configuration.shipped().of(RomFiles.class).files);
    return mine;
  }

  private Object machine(Class<? extends com.fpetrola.oozx.speccy.machine.Spectrum> model) {
    return speccy.machine.model(model);
  }

  @Test
  void aMachineSaysWhichOfItsSetsItIsRunningOn() {
    assertEquals("English", roms.chosenSet(machine(Spec48.class)));
    assertEquals(List.of("English", "Spanish"), List.copyOf(roms.setsFor(machine(Spec48.class)).keySet()));
    assertEquals("Version 4.0", roms.chosenSet(machine(SpecPlus3.class)),
        "what this build carries for a +3 is the first of the two revisions");
  }

  @Test
  void choosingAnotherSetChangesTheRomsItReads() {
    Object oneTwentyEight = machine(Spec128.class);
    List<String> english = roms.setsFor(oneTwentyEight).get("English");

    roms.chooseSet(oneTwentyEight, "Spanish");

    assertEquals("Spanish", roms.chosenSet(oneTwentyEight));
    assertNotEquals(english, roms.setsFor(oneTwentyEight).get("Spanish"), "the two sets are two sets");
    assertEquals(roms.setsFor(oneTwentyEight).get("Spanish"), roms.missingFor(oneTwentyEight),
        "and none of them is here yet, which is what sends somebody to fetch them");
  }

  /** Two machines with the same ROMs in them are one choice: a +2A and a +3 read the same four. */
  @Test
  void aMachineThatSharesItsRomsChoosesForBoth() {
    roms.chooseSet(machine(SpecPlus2A.class), "Version 4.1");

    assertEquals("Version 4.1", roms.chosenSet(machine(SpecPlus3.class)),
        "the +3 was left on a set the +2A is not running");
  }

  @Test
  void aSetNobodyHasHeardOfIsRefusedAndSaysWhichThereAre() {
    IllegalArgumentException noSuchSet = assertThrows(IllegalArgumentException.class,
        () -> roms.chooseSet(machine(Spec48.class), "Portuguese"));

    assertTrue(noSuchSet.getMessage().contains("Spanish"), "it should say what there is: " + noSuchSet.getMessage());
  }

  /**
   * The sets are the build's knowledge, like the places its ROMs are published: a run that saved
   * its settings while the build carried a different list left that list written down, and every
   * later run would offer it instead of the one this build has.
   */
  @Test
  void theSetsAreWhatThisBuildCarriesAndNotWhatAnOlderRunWroteDown() {
    roms.sets.put("Spec48", java.util.Map.of("Esperanto", List.of("48-esperanto.rom")));

    assertEquals(List.of("English", "Spanish"), List.copyOf(roms.setsFor(machine(Spec48.class)).keySet()));
  }
}
