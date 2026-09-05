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
