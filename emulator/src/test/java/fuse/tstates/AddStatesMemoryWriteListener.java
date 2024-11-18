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

import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;
import fuse.tstates.phases.BeforeWrite;

public class AddStatesMemoryWriteListener<T extends WordNumber> implements MemoryWriteListener<T> {
  private final PhaseProcessor<T> phaseProcessor;

  public AddStatesMemoryWriteListener(PhaseProcessor<T> phaseProcessor1) {
    phaseProcessor = phaseProcessor1;
  }

  public void writtingMemoryAt(T address, T value) {
    phaseProcessor.processPhase(new BeforeWrite());
    phaseProcessor.addMultipleMc(1, 3, 0, address.intValue());
    phaseProcessor.addMw(address, value);
  }
}
