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

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Snapshot-to-machine resolution: each machine declares its own snapshot model rather than
 * being named in a central switch, so adding a machine needs no change to the loader.
 */
class SnapshotChoosesItsMachineTest {

  private Speccy speccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.picture.active = false;
    return speccy;
  }

  private void goesTo(Spectrum expected, MachineTypes model, Speccy speccy) {
    assertSame(expected, speccy.machine.forSnapshotModel(model).orElse(null),
        "a " + model + " snapshot");
  }

  @Test
  void everyModelASnapshotCanNameHasItsMachine() {
    Speccy speccy = speccy();

    goesTo(speccy.machine.model(Spec48.class), MachineTypes.SPECTRUM48K, speccy);
    goesTo(speccy.machine.model(Spec128.class), MachineTypes.SPECTRUM128K, speccy);
    goesTo(speccy.machine.model(SpecPlus2.class), MachineTypes.SPECTRUMPLUS2, speccy);
    goesTo(speccy.machine.model(SpecPlus2A.class), MachineTypes.SPECTRUMPLUS2A, speccy);
    goesTo(speccy.machine.model(SpecPlus3.class), MachineTypes.SPECTRUMPLUS3, speccy);
  }

  /** An unbuilt model (16K) falls back to the nearest machine sharing its code. */
  @Test
  void aModelThisBuildDoesNotHaveFallsBackToItsCode() {
    Speccy speccy = speccy();
    goesTo(speccy.machine.model(Spec48.class), MachineTypes.SPECTRUM16K, speccy);
  }

  /** A machine is also reachable by its short name, previously a switch over Speccy's fields
   * that could omit a machine even though it was built. */
  @Test
  void aCoreReachesEveryMachineByItsShortName() {
    Speccy speccy = speccy();
    Set<String> shortNames = new HashSet<>();

    for (Spectrum machine : speccy.machine.getMachineTypes()) {
      assertTrue(shortNames.add(machine.shortName()),
          machine.getName() + " shares its short name with another machine: " + machine.shortName());
      speccy.machine.select(speccy.machine.forShortName(machine.shortName()).orElseThrow());
      assertSame(machine, speccy.machine.current, "asking for " + machine.shortName());
    }
  }

  /** An unrecognised short name resolves to nothing, never to an unrelated machine. */
  @Test
  void anUnknownNameIsNobody() {
    assertTrue(speccy().machine.forShortName("TC2048").isEmpty(), "an unknown machine name");
  }

  /** No two machines may claim the same snapshot model; variants like Pentagon/NTSC/+3e claim
   * none, so they are only reachable by explicit selection, never by loading a snapshot. */
  @Test
  void noTwoMachinesAnswerForTheSameModel() {
    Set<MachineTypes> claimed = new HashSet<>();
    for (Spectrum machine : speccy().machine.getMachineTypes()) {
      MachineTypes model = machine.snapshotModel();
      if (model != null) {
        assertTrue(claimed.add(model),
            machine.getName() + " claims " + model + ", which another machine already answers for");
      }
    }
    assertEquals(5, claimed.size(), "the models a snapshot can name here");
  }
}
