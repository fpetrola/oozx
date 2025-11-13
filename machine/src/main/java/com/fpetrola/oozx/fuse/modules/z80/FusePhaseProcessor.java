/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx.fuse.modules.z80;

import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.SpectrumZ80Clock;
import com.fpetrola.oozx.fuse.modules.Ula;
import fuse.tstates.PhaseProcessor;

final public class FusePhaseProcessor extends PhaseProcessor {
  private final Ula ula;
  private final SpectrumZ80Clock zxClock;
  private final int pageSizeLogarithm;
  private final MemoryPage[] mapRead;

  public FusePhaseProcessor(Z80 z80) {
    super(z80.ooz80.getInstructionFetcher(), z80.ooz80.getState());
    Memory memory = z80.memory;
    ula = z80.ula;
    zxClock = z80.zxClock;
    pageSizeLogarithm = memory.PAGE_SIZE_LOGARITHM;
    mapRead = memory.mapRead;
  }

  private void addMultipleMc(int x, int time) {
    if (mapRead[address >>> pageSizeLogarithm].contended) {
      for (int i = 0; i < x; i++)
        ula.addUlaStates(time);
    } else
      zxClock.addTStates(time * x);
  }

  protected void addMultipleMcRegister() {
    address = currentRegister.read();
    addMultipleMc(1, 1);
  }

  protected void addMultipleMCPC3() {
    address = registerPC.read();
    addMultipleMc(1, 3);
  }

  protected void addMultipleMCRegister(int x, int delta1) {
    address = currentRegister.read();
    addMultipleMc(x, 1);
  }

  protected void addMultipleMcAddress() {
    addMultipleMc(1, 1);
  }

  protected void addMultipleMCPc2(int x, int delta) {
    address = registerPC.read();
    addMultipleMc(x, 1);
  }

  protected void addMultipleMCHL2(int x) {
    address = registerHL.read();
    addMultipleMc(x, 1);
  }

  protected void addMultipleMCIR(int time) {
    address = this.registerIR.read();
    addMultipleMc(time, 1);
  }
}
