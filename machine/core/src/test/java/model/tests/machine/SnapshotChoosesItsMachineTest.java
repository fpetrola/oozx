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
import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.Spectrum;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.OOSpectrumConnector;
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which machine a snapshot becomes. The loader used to name the classes itself, in a switch that a
 * new machine could not be added to without editing the Z80; now each machine says which model a
 * snapshot taken on it would be called, and the loader looks for whoever answers.
 */
class SnapshotChoosesItsMachineTest {

  private Speccy speccy() {
    OOSpectrumConnector.noTest = true;
    Speccy speccy = Speccy.create(new SpectrumZ80Clock(),
        binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));
    speccy.init();
    speccy.uiDisplay.active = false;
    speccy.z80.bridgeCommand = (a, b) -> null;
    return speccy;
  }

  private void goesTo(Spectrum expected, MachineTypes model, Speccy speccy) {
    assertSame(expected, speccy.machine.forSnapshotModel(model).orElse(null),
        "a " + model + " snapshot");
  }

  @Test
  void everyModelASnapshotCanNameHasItsMachine() {
    Speccy speccy = speccy();

    goesTo(speccy.spec48, MachineTypes.SPECTRUM48K, speccy);
    goesTo(speccy.spec128, MachineTypes.SPECTRUM128K, speccy);
    goesTo(speccy.specPlus2, MachineTypes.SPECTRUMPLUS2, speccy);
    goesTo(speccy.specPlus2a, MachineTypes.SPECTRUMPLUS2A, speccy);
    goesTo(speccy.specPlus3, MachineTypes.SPECTRUMPLUS3, speccy);
  }

  /** No 16K is built here, so its snapshot takes the nearest machine running the same code. */
  @Test
  void aModelThisBuildDoesNotHaveFallsBackToItsCode() {
    Speccy speccy = speccy();
    goesTo(speccy.spec48, MachineTypes.SPECTRUM16K, speccy);
  }

  /**
   * What makes looking for whoever answers deterministic: no two machines claim the same model.
   * The variants - Pentagon, NTSC, +3e - name none, so they are reached by choosing them, never
   * by loading a file.
   */
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
