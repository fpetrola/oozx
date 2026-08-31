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
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.Spectrum;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.fpetrola.oozx.MachineCapability.MEMORY_128;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * That a write to the paging port reaches the machine that is running.
 * <p>
 * Peripherals are held in a map keyed by their class, so the machines that share a paging port
 * cannot each keep one registered: the one that answers is whichever registered last. Selecting a
 * machine calls its init, which registers its own again, and that is what keeps the port pointed
 * at the machine in front. A machine that registers only once, at construction, has its own
 * replaced before it ever runs - and its writes go to another machine's paging state, silently,
 * because the memory is shared and the pages still come out roughly right.
 * <p>
 * Asked of every machine with the 128's paging rather than of a list, so a machine added later is
 * covered by having been added.
 */
class PagingReachesItsMachineTest {

  private Speccy speccy() {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    return speccy;
  }

  @Test
  void everyMachineWithAPagingPortOwnsIt() {
    List<String> deaf = new ArrayList<>();
    List<String> asked = new ArrayList<>();

    for (Spectrum model : speccy().machine.getMachineTypes()) {
      if (!model.has(MEMORY_128)) {
        continue;
      }
      Speccy speccy = speccy();
      Spectrum wanted = speccy.machine.getMachineTypes().stream()
          .filter(type -> type.getClass() == model.getClass()).findFirst().orElseThrow();
      speccy.machine.selectDefault();
      speccy.machine.select(wanted);

      asked.add(wanted.getName());
      // Bit 5 of the 128's paging port locks paging, and the machine remembers that it is locked.
      speccy.periph.writePort(0x7ffd, (byte) 0x20);
      if (!speccy.machine.current.getRamInfo().locked) {
        deaf.add(wanted.getName());
      }
    }

    assertTrue(asked.size() >= 4, "expected several machines to page this way, asked " + asked);
    if (!deaf.isEmpty()) {
      fail("the paging port did not reach " + deaf + "; it reached another machine's state instead");
    }
  }
}
