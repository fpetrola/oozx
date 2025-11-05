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

import com.fpetrola.z80.cpu.Event;
import fuse.tstates.PhaseProcessor;

public class FusePhaseProcessor extends PhaseProcessor {
  private final Z80 z80;

  public FusePhaseProcessor(Z80 z80) {
    super(z80.ooz80.getInstructionFetcher(), z80.ooz80.getState());
    this.z80 = z80;
  }

  public void addMw(int address, int value) {
  }

  public void addMr(int address, int value) {
  }

  public void addMultipleMc(int x, int time1, int delta, int baseAddress, String description) {
    boolean memoryContended = z80.memory.mapRead[baseAddress >>> z80.memory.PAGE_SIZE_LOGARITHM].contended;
    for (int i = 0; i < x; i++) {
      if (memoryContended)
        z80.ula.addUlaStates(0, () -> "ula " + (description != null ? description : "contend_read_no_mreq"));

      addSingleMc(time1, delta, baseAddress, description);
    }
  }

  protected void getAddEvent(Event event) {
    event.description = getDescription(event);
    z80.zxClock.addTStates(event.getTime(), event.description);
//    getState().addEvent(event);
  }

  private String getDescription(Event event) {
    if (event.description != null)
      return event.description;
    else
      return switch (event.getType()) {
        case "MR" -> "contend_read";
        case "MW" -> "contend_write";
        case "MC" -> "contend_read_no_mreq";
        default -> "unknown";
      };
  }
}
