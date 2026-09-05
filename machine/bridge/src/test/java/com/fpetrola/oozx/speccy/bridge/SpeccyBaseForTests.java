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

package com.fpetrola.oozx.speccy.bridge;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.modules.sound.SoundCard;
import com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.registers.Register;

import java.util.function.Supplier;

/**
 * The machine the bridge's tests hold: it counts its own T-states and writes down every one it
 * hands out, so that a run can be compared with Fuse's to the cycle.
 * <p>
 * In the test tree, where it belongs: an emulator has no use for either half of it, and the model
 * used to carry the wiring for this and pick it by asking a static boolean who was looking.
 */
public class SpeccyBaseForTests {
  public static Speccy createSpeccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock() {
      public void log(Supplier<String> description, byte data) {
//        Register pc = speccy.z80.ooz80.getState().getPc();
        Register pc = new Plain8BitRegister("PC");
        GetTStatesHistory.addTStateUpdate(data, description, tStates, pc);
      }

      public void contend(com.fpetrola.z80.tstates.Contention.Kind cycle, int tStates) {
        log(() -> "ula " + cycle.description, (byte) tStates);
        this.tStates += tStates;
      }

      public void acknowledge(int tStates) {
        log(() -> "interrupt", (byte) tStates);
        this.tStates += tStates;
      }

      public void phase(com.fpetrola.z80.tstates.Contention.Kind kind, int tStates) {
        log(() -> kind.description, (byte) tStates);
        this.tStates += tStates;
      }
    }, binder -> binder.bind(SoundCard.class).to(SilentSoundDevice.class), model.harness.CountingWiring.instead());

    return speccy;
  }

  public Speccy speccy;
}
