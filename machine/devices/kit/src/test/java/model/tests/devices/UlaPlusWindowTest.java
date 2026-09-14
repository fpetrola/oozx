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

package model.tests.devices;

import com.fpetrola.oozx.speccy.devices.Equipment;
import com.fpetrola.oozx.speccy.devices.ulaplus.UlaPlusPeripheral;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sixty-four colours as a thing somebody fits: the desk never names it, the jar does, and
 * clipping its window onto a machine is what gives that machine the chip.
 */
class UlaPlusWindowTest {
  @Test
  void theDeskIsOfferedItWithoutKnowingWhatItIs() {
    Equipment offered = ServiceLoader.load(Equipment.class).stream()
        .map(ServiceLoader.Provider::get)
        .filter(kind -> kind.name().equals("ULAplus"))
        .findFirst().orElse(null);

    assertNotNull(offered, "nothing on this classpath offers the sixty-four colours");
  }

  /** Plugging it in is fitting it, which is what a window clipped onto a machine does. */
  @Test
  void pluggingItInIsFittingIt() {
    UlaPlusPeripheral chip = new UlaPlusPeripheral(null);

    chip.plugIn(true);
    assertTrue(chip.isPluggedIn());

    chip.plugIn(false);
    assertTrue(!chip.isPluggedIn());
  }
}
