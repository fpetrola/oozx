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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.config.RomFiles;
import com.fpetrola.oozx.speccy.machine.CzSpectrum;
import com.fpetrola.oozx.speccy.machine.CzSpectrumPlus;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.Tk90x;
import com.fpetrola.oozx.speccy.machine.Tk95;
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
   * Each machine reads the ROM of its own name. These are derived from one another - a TK95 from
   * a TK90X, a CZ Plus from a CZ - and the settings find a machine's ROMs by walking up to the
   * nearest name they know of, so one left out of the list would quietly run the one above it.
   */
  @Test
  void aMachineDerivedFromAnotherStillReadsItsOwnRom() {
    assertEquals(List.of("tk90x.rom"), roms.running(machine(Tk90x.class)));
    assertEquals(List.of("tk95.rom"), roms.running(machine(Tk95.class)));
    assertEquals(List.of("48.rom"), roms.running(machine(CzSpectrum.class)),
        "the one Czerweny sold is a 48K down to the ROM in it");
    assertEquals(List.of("inves.rom"), roms.running(machine(CzSpectrumPlus.class)));
  }

  /**
   * A machine is offered its other ROMs whether or not any of them has been fetched yet: which
   * sets there are is what the build knows, and knowing is not having.
   */
  @Test
  void aMachineIsOfferedItsSetsWhetherOrNotItHasThem() {
    assertEquals(List.of("Portuguese", "Spanish"), List.copyOf(roms.setsFor(machine(Tk90x.class)).keySet()));
    assertEquals(List.of("tk90x.rom"), roms.setsFor(machine(Tk90x.class)).get("Portuguese"));
    assertEquals(List.of("tk90x-spanish.rom"), roms.setsFor(machine(Tk90x.class)).get("Spanish"));
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
