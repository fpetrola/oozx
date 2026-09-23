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

package com.fpetrola.oozx.plugins;

import dev.crystal.plugins.api.RoleInterface;

import com.fpetrola.oozx.config.RomFiles;
import com.fpetrola.oozx.config.RomsOfItsOwn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A machine that arrives in a jar says what it is made with, and the emulator takes it as
 * knowledge of its own. Only the way in is tried here; that it reaches a running machine is
 * tried where the machines are.
 */
class TheRomsAJarBringsTest {

  @Test
  void whatAJarSaysItsMachinesAreMadeWithIsAWayIn() {
    assertTrue(RomsOfItsOwn.class.isAnnotationPresent(RoleInterface.class), "it should be a way in");
    assertEquals(List.of(), Plugins.found(RomsOfItsOwn.class).stream()
            .filter(one -> one.files().isEmpty()).toList(),
        "a jar that declares nothing at all has nothing to say and should not be one");
  }

  /** The three halves of it, so a jar that only names files does not have to say the rest. */
  @Test
  void onlyTheFilesHaveToBeSaid() {
    RomsOfItsOwn theLeast = () -> Map.of("Whatever", List.of("whatever.rom"));

    assertEquals(Map.of(), theLeast.sets());
    assertEquals(Map.of(), theLeast.sources());
    assertEquals(List.of("whatever.rom"), theLeast.files().get("Whatever"));
  }

  /** What the build knows is worked out again when a jar arrives, or a machine brought now would
   * be one nobody knows the ROM of. */
  @Test
  void whatIsKnownIsWorkedOutAgainWhenAJarArrives() {
    int before = Plugins.generation();
    RomFiles roms = new RomFiles();
    assertEquals(before, Plugins.generation(), "asking should not count as something arriving");
    assertTrue(roms.sourceFor("48.rom") != null || roms.sourceFor("48.rom") == null,
        "reading what the build knows should not throw");
  }
}
