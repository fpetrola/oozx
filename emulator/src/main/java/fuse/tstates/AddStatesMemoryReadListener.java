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
import fuse.tstates.phases.AfterMR;

public class AddStatesMemoryReadListener<T extends WordNumber> implements MemoryReadListener<T> {
  private Runnable lastEvents;
  private final PhaseProcessor<T> phaseProcessor;

  public AddStatesMemoryReadListener(PhaseProcessor<T> phaseProcessor) {
    this.phaseProcessor = phaseProcessor;
  }

  public void readingMemoryAt(T address, T value, int delta, int fetching) {
    Runnable lastEvents1 = () -> processEvent(address, value, fetching);

    boolean requiresDelay = fetching == 2 || delta == 3;
    if (!requiresDelay) {
    lastEvents1.run();
  }

    if (lastEvents != null) {
      lastEvents.run();
      lastEvents = null;
    }

    if (requiresDelay)
      lastEvents = lastEvents1;
  }

  protected void processEvent(T address, T value, int fetching) {
    doRead(address, value, fetching);

    addMc(address, fetching == 1 ? 4 : 3);
    phaseProcessor.addMr(address, value);

    phaseProcessor.setAddress(address);
    phaseProcessor.readCount++;
    phaseProcessor.processPhase(new AfterMR());
  }

  protected void doRead(T address, T value, int fetching) {
  }

  protected void addMc(T address, int time1) {
    phaseProcessor.addMultipleMc(1, time1, 0, address.intValue(), "readbyte");
  }
}
