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
import com.fpetrola.oozx.speccy.sound.JavaSoundDevice;
import com.fpetrola.oozx.speccy.sound.SilentSoundDevice;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.z80.registers.Plain8BitRegister;
import com.fpetrola.z80.registers.Register;

import java.util.function.Supplier;

public class SpeccyBaseForTests {
  public static Speccy createSpeccy() {
    Speccy speccy = Speccy.create(new SpectrumZ80Clock() {
      public void log(Supplier<String> description, byte data) {
//        Register pc = speccy.z80.ooz80.getState().getPc();
        Register pc = new Plain8BitRegister("PC");
        GetTStatesHistory.addTStateUpdate(data, description, tStates, pc);
      }

      public void addTStates(int tStatesToAdd, String description) {
        log(() -> description, (byte) tStatesToAdd);
        this.tStates += tStatesToAdd;
      }

      public void addTStates(int tStatesToAdd, Supplier<String> description) {
        log(description, (byte) tStatesToAdd);
        addTStates(tStatesToAdd);
      }
    }, binder -> binder.bind(JavaSoundDevice.class).to(SilentSoundDevice.class));

    return speccy;
  }

  public Speccy speccy;
}
