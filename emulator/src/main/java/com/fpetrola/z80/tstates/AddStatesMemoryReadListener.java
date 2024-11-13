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

package com.fpetrola.z80.tstates;

import com.fpetrola.z80.memory.MemoryReadListener;

public class AddStatesMemoryReadListener implements MemoryReadListener {
  private final RecordingPhaseProcessor phaseProcessor;

  public AddStatesMemoryReadListener(RecordingPhaseProcessor phaseProcessor) {
    this.phaseProcessor = phaseProcessor;
  }

  public void readingMemoryAt(int address, int value, int fetching) {
    doRead(address, value, fetching);
    phaseProcessor.contend(address, 1, fetching == 1 ? 4 : 3, Contention.Kind.READ);
    phaseProcessor.addMr(address, value);
    phaseProcessor.afterRead(address);
  }

  protected void doRead(int address, int value, int fetching) {
  }
}
