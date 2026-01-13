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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.ula.ContentionTable;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.tstates.Contention.Kind;
import com.fpetrola.z80.tstates.PhaseProcessor;

/** The contention on a real Spectrum: the ULA's, when the page is contended; plain T-states when it is not. */
final public class SpeccyPhaseProcessor extends PhaseProcessor {
  private final Ula ula;
  private final SpectrumZ80Clock zxClock;
  private final int pageSizeLogarithm;
  private final MemoryBus memory;
  /** By length, what a run of one-T-state internal cycles at a contended address takes; a Z80's are seven long at most. */
  private final byte[][] noMreqRun = new byte[8][];

  public SpeccyPhaseProcessor(State state, MemoryBus memory, Ula ula, SpectrumZ80Clock zxClock) {
    super(state);
    this.ula = ula;
    this.zxClock = zxClock;
    pageSizeLogarithm = 11;
    this.memory = memory;
    for (int times = 2; times < noMreqRun.length; times++) {
      noMreqRun[times] = ula.contention.run(times);
    }
  }

  public void contend(int address, int times, int tstates, Kind kind) {
    if (memory.contended(address)) {
      if (tstates == 1 && times > 1 && times < noMreqRun.length)
        zxClock.addTStates(noMreqRun[times][ContentionTable.within(zxClock.getTStates())]);
      else
        for (int i = 0; i < times; i++)
          ula.addUlaStates(kind, tstates);
    } else
      zxClock.addTStates(tstates * times);
  }
}
