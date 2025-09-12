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

package com.fpetrola.oozx;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.opcodes.references.WordNumber;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;

public class Z80 {
  public static long interruptsEnabledAt;
  private static OOZ80<WordNumber> ooz80;

  public static void interrupt() {

  }

  public static void registerStartup() {
    Machine.reset(false);

    MiniZXIO io = new MiniZXIO();
    ooz80 = EmulatedMiniZX.createOOZ80(io);
    MiniZX.createScreen(io.miniZXKeyboard, EmulatedMiniZX.getMemFunction(ooz80));

    RegistersBase registersBase = new RegistersBase<>(ooz80.getState());

    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile("file:///home/fernando/dynamitedan1.z80");
    State<?> state = ooz80.getState();

    PhaseProcessor<WordNumber> phaseProcessor = new PhaseProcessor<>(ooz80);
    Memory<WordNumber> memory = (Memory<WordNumber>) state.getMemory();
    memory.addMemoryReadListener(new AddStatesMemoryReadListener<>(phaseProcessor));
    memory.addMemoryWriteListener(new AddStatesMemoryWriteListener<>(phaseProcessor));
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);
  }

  public static void doOpcodes() {
    int startTstates = ooz80.getState().tstates;
    int tstates = 0;
    while (tstates < EventManager.eventNextEvent) {
      ooz80.execute();
      tstates += (ooz80.getState().tstates - startTstates);
    }
    Spectrum.tstates+= tstates;
  }
}
