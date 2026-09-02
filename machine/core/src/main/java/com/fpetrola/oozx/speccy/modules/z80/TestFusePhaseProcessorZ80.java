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

import com.fpetrola.oozx.fuse.modules.z80.TestFusePhaseProcessor;
import fuse.tstates.Contention.Kind;

/** The machine under its own tests: the ULA's contention on contended pages, and the T-states either way. */
public class TestFusePhaseProcessorZ80 extends TestFusePhaseProcessor {
  private final Z80 z80;

  public TestFusePhaseProcessorZ80(Z80 z80) {
    super(z80.ooz80.getState(), event -> {
    });
    this.z80 = z80;
  }

  public void contend(int address, int times, int tstates, Kind kind) {
    boolean contended = z80.memory.mapRead[address >>> z80.memory.PAGE_SIZE_LOGARITHM].contended;
    for (int i = 0; i < times; i++) {
      if (contended)
        z80.ula.addUlaStates(0, () -> "ula " + kind.description);
      z80.zxClock.addTStates(tstates, kind.description);
    }
  }
}
