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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.cpu.DefaultInstructionExecutor;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.NullInstructionSpy;

import java.util.function.Function;

public class EmulatedMiniZX {
  public OOZ80 ooz80;
  private int pause;

  private String url;
  private boolean showScreen;
  private final int emulateUntil;
  private boolean inThread;

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread) {
    this.pause = pause;
    //    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile("file:///home/fernando/detodo/desarrollo/m/zx/zx/jsw.z80");
    this.url = url;
    this.showScreen = showScreen;
    this.emulateUntil = emulateUntil;
    this.inThread = inThread;
  }

  public static void main(String[] args) {
    new EmulatedMiniZX("file:///home/fernando/dynamitedan1.z80", 1, true, -1, true).start();
  }

  public static  OOZ80 createOOZ80(MiniZXIO io) {
    var state = new State(io, new DefaultRegisterBankFactory().createBank(), new MockedMemory(true));
    io.setPc(state.getPc());
    return new OOZ80(state, Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory(state)), new DefaultInstructionExecutor(state, false));
  }

  public static <S extends Integer> Function<java.lang.Integer, java.lang.Integer> getMemFunction(OOZ80 ooz81) {
    return index -> {
      return ooz81.getState().getMemory().read(index, 10);
    };
  }

  public void start() {
    MiniZXIO io = new MiniZXIO();
    ooz80 = createOOZ80(io);
    if (showScreen)
      MiniZX.createScreen(io.miniZXKeyboard, this.getMemFunction(ooz80));

    RegistersBase registersBase = new RegistersBase(ooz80.getState());

    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
    State state = ooz80.getState();
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);

//    PhaseProcessor phaseProcessor = new PhaseProcessor<>(ooz80);
//    Memory memory = state.getMemory();
//    memory.addMemoryReadListener(new AddStatesMemoryReadListener<>(phaseProcessor));
//    memory.addMemoryWriteListener(new AddStatesMemoryWriteListener<>(phaseProcessor));

    if (inThread)
      new Thread(this::emulate).start();
    else
      emulate();
  }

  public void emulate() {
    int i = 0;
    while (true) {
      if (!(ooz80.getState().getPc().read() != emulateUntil)) break;
      if (i++ % (pause * 1000) == 0) this.ooz80.getState().setINTLine(true);
      else {
        if (i % pause == 0)
          this.ooz80.execute();
      }
    }
  }
}
