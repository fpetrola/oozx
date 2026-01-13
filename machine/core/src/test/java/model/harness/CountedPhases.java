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

package model.harness;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.z80.tstates.RecordingPhaseProcessor;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.tstates.Contention.Kind;

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
