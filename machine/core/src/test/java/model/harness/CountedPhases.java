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

package model.harness;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.z80.tstates.RecordingPhaseProcessor;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.tstates.Contention.Kind;

/** The machine under its own tests: the ULA's contention on contended pages, and the T-states either way. */
/**
 * The phases of a cycle as a test counts them: the ULA's share of a contended access, and the
 * cycle's own T-states, added by hand rather than by the memory the processor reads through.
 */
public class CountedPhases extends RecordingPhaseProcessor {
  private final MemoryBus memory;
  private final Ula ula;
  private final SpectrumZ80Clock zxClock;

  public CountedPhases(State state, MemoryBus memory, Ula ula, SpectrumZ80Clock zxClock) {
    super(state, event -> {
    });
    this.memory = memory;
    this.ula = ula;
    this.zxClock = zxClock;
  }

  public void contend(int address, int times, int tstates, Kind kind) {
    boolean contended = memory.contended(address);
    for (int i = 0; i < times; i++) {
      if (contended)
        ula.addUlaStates(kind, 0);
      zxClock.phase(kind, tstates);
    }
  }
}
