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

package fuse.tstates;

import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.opcodes.references.WordNumber;
import fuse.tstates.phases.AfterExecution;
import fuse.tstates.phases.AfterFetch;
import fuse.tstates.phases.AfterMR;

public class AddStatesMemoryReadListener<T extends WordNumber> implements MemoryReadListener<T> {
  private Runnable lastEvents;
  private final PhaseProcessor<T> phaseProcessor;

  public AddStatesMemoryReadListener(PhaseProcessor<T> phaseProcessor) {
    this.phaseProcessor = phaseProcessor;
  }

  public void readingMemoryAt(T address, T value, int delta, int fetching) {
    if (address.intValue() == -1) {
      phaseProcessor.processPhase(new AfterFetch());
    } else if (address.intValue() == -2) {
      phaseProcessor.processPhase(new AfterExecution());
    } else {
      boolean pendingEvent = lastEvents != null;

      Runnable lastEvents1 = () -> {
        int time1;
        if (fetching != 0)
          time1 = 4 - (fetching == 2 ? 1 : 0);
        else
          time1 = 3;


        phaseProcessor.addMultipleMc(1, time1, 0, address.intValue());
        phaseProcessor.addMr(address, value);

        phaseProcessor.setAddress(address);
        phaseProcessor.processPhase(new AfterMR());
      };

      boolean requiresDelay = fetching == 2 || delta == 3;
      if (!requiresDelay) {
        lastEvents1.run();
      }

      if (pendingEvent) {
        lastEvents.run();
        lastEvents = null;
      }

      if (requiresDelay)
        lastEvents = lastEvents1;
    }
  }
}
