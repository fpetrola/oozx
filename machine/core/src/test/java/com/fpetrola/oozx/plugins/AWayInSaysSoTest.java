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

import com.fpetrola.oozx.Extension;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Only an interface that says it is a way in can be asked for, so a wrong name is not silence. */
class AWayInSaysSoTest {

  /** Not a way in, and nothing anywhere implements it. */
  private interface SomethingElse {
  }

  @Test
  void anInterfaceThatIsNotAWayInIsRefusedRatherThanAnswered() {
    IllegalArgumentException refused =
        assertThrows(IllegalArgumentException.class, () -> Plugins.found(SomethingElse.class));
    assertTrue(refused.getMessage().contains("@Plugin"), refused.getMessage());
  }

  @Test
  void theWaysInThisBuildHasCanBeAskedFor() {
    for (Class<?> wayIn : new Class<?>[]{Extension.class, BesideTheGame.class}) {
      assertTrue(wayIn.isAnnotationPresent(Plugin.class), wayIn + " should be a way in");
      assertDoesNotThrow(() -> Plugins.found(wayIn), wayIn + " could not be asked for");
    }
  }
}
