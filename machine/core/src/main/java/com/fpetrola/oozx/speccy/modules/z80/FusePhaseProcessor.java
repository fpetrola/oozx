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

import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.modules.Ula;
import fuse.tstates.Contention.Kind;
import fuse.tstates.PhaseProcessor;

/** The contention on a real Spectrum: the ULA's, when the page is contended; plain T-states when it is not. */
final public class FusePhaseProcessor extends PhaseProcessor {
  private final Ula ula;
  private final SpectrumZ80Clock zxClock;
  private final int pageSizeLogarithm;
  private final MemoryPage[] mapRead;

  public FusePhaseProcessor(Z80 z80) {
    super(z80.ooz80.getState());
    Memory memory = z80.memory;
    ula = z80.ula;
    zxClock = z80.zxClock;
    pageSizeLogarithm = memory.PAGE_SIZE_LOGARITHM;
    mapRead = memory.mapRead;
  }

  public void contend(int address, int times, int tstates, Kind kind) {
    if (mapRead[address >>> pageSizeLogarithm].contended) {
      for (int i = 0; i < times; i++)
        ula.addUlaStates(tstates);
    } else
      zxClock.addTStates(tstates * times);
  }
}
