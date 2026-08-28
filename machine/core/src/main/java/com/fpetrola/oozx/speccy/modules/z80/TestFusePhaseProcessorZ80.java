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
import com.fpetrola.z80.cpu.Event;

public class TestFusePhaseProcessorZ80 extends TestFusePhaseProcessor {
  private final Z80 z80;

  public TestFusePhaseProcessorZ80(Z80 z80) {
    super(z80.ooz80.getInstructionFetcher(), z80.ooz80.getState());
    this.z80 = z80;
  }

  public void addMultipleMc(final int x, final int time1, final int delta, final int baseAddress, final String description) {
    boolean memoryContended = z80.memory.mapRead[baseAddress >>> z80.memory.PAGE_SIZE_LOGARITHM].contended;
    for (int i = 0; i < x; i++) {
      if (memoryContended) {
        z80.ula.addUlaStates(0, getAddMultipleMcStringSupplier(description));
      }

      addSingleMc(time1, delta, baseAddress, description);
    }
  }

  protected void getAddEvent(Event event) {
    event.description = getDescription(event);
    z80.zxClock.addTStates(event.getTime(), event.description);
  }
}
