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
