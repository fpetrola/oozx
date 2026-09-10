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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.fpetrola.oozx.rzx.RzxSession;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import com.fpetrola.oozx.speccy.devices.ay.AyPlus3Peripheral;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A snapshot carries the sound chip's state, and it was being dropped.
 * <p>
 * A recording begins mid-game, which for a 128K game means mid-tune: the chip is holding a note,
 * an envelope is running, the mixer says which channels are open. Restoring the machine without
 * any of that leaves the chip set to whatever the last one was set to, and the recording never
 * puts it right, because a recording replays the writes that come after - not the ones that had
 * already happened when it started.
 */
class AyFromSnapshotTest {

  private static File recording() throws Exception {
    return model.harness.TestFiles.testFile("/rzx/jsw-full.rzx");
  }

  private static long writesTo(Speccy speccy) {
    for (Class<? extends Peripheral> kind : List.of(AyPeripheral.class, AyPlus3Peripheral.class)) {
      Peripheral peripheral = speccy.peripheralRegistry.find(kind);
      if (peripheral instanceof AyPeripheral ay && ay.writes() > 0) {
        return ay.writes();
      }
    }
    return 0;
  }

  @Test
  void openingARecordingSetsUpItsSoundChip() throws Exception {
    RzxSession session = RzxSession.open(recording());

    assertTrue(session.getSpeccy().machine.current.hasOnBoard(AyPeripheral.class),
        "Jet Set Willy 128K should have arrived on a machine with a sound chip");
    assertTrue(writesTo(session.getSpeccy()) >= 16,
        "all sixteen registers should have been put back, and " 
            + writesTo(session.getSpeccy()) + " writes reached the chip");
  }
}
