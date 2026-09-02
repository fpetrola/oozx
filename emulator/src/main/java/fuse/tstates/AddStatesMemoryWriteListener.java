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

package fuse.tstates;

import com.fpetrola.oozx.fuse.modules.z80.TestFusePhaseProcessor;
import com.fpetrola.z80.memory.MemoryWriteListener;

public class AddStatesMemoryWriteListener implements MemoryWriteListener {
  protected final TestFusePhaseProcessor phaseProcessor;

  public AddStatesMemoryWriteListener(TestFusePhaseProcessor phaseProcessor1) {
    phaseProcessor = phaseProcessor1;
  }

  public void writtingMemoryAt(int address, int value) {
    phaseProcessor.beforeWrite(address);
    doWrite(address, value);
    phaseProcessor.contend(address, 1, 3, Contention.Kind.WRITE);
    phaseProcessor.addMw(address, value);
  }

  protected void doWrite(int address, int value) {
  }
}
