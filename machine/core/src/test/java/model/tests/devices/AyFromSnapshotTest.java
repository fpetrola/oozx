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

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.devices.ay.AyPeripheral;
import com.fpetrola.oozx.speccy.peripherals.ZxPeripheral;
import com.fpetrola.oozx.speccy.rzx.RzxSession;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import com.fpetrola.oozx.speccy.devices.ay.AyPlus3Peripheral;

import static com.fpetrola.oozx.MachineCapability.AY;
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
    return Path.of(AyFromSnapshotTest.class.getResource("/rzx/jsw-full.rzx").toURI()).toFile();
  }

  private static long writesTo(Speccy speccy) {
    for (Class<? extends ZxPeripheral> kind : List.of(AyPeripheral.class, AyPlus3Peripheral.class)) {
      ZxPeripheral peripheral = speccy.periph.find(kind);
      if (peripheral instanceof AyPeripheral ay && ay.writes() > 0) {
        return ay.writes();
      }
    }
    return 0;
  }

  @Test
  void openingARecordingSetsUpItsSoundChip() throws Exception {
    OOSpectrumConnector.noTest = true;
    RzxSession session = RzxSession.open(recording());

    assertTrue(session.getSpeccy().machine.current.has(AY),
        "Jet Set Willy 128K should have arrived on a machine with a sound chip");
    assertTrue(writesTo(session.getSpeccy()) >= 16,
        "all sixteen registers should have been put back, and " 
            + writesTo(session.getSpeccy()) + " writes reached the chip");
  }
}
