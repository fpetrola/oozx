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
