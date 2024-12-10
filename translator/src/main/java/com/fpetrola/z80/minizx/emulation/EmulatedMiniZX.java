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

package com.fpetrola.z80.minizx.emulation;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.z80.transformations.VirtualRegisterFactory;

import java.util.function.Function;

public class EmulatedMiniZX<T extends WordNumber> {
  public OOZ80<T> ooz80;
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
    new EmulatedMiniZX("file:///home/fernando/dynamitedan1.z80", 1000, true, -1, true).start();
  }

  public <T extends WordNumber> OOZ80<T> createOOZ80(MiniZXIO io) {
    var state = new State(io, new DefaultRegisterBankFactory().createBank(), new MockedMemory(true));
    io.setPc(state.getPc());
    return new OOZ80(state, Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory<T>(state)));
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> ooz80.getState().getMemory().read(WordNumber.createValue(index), 0).intValue();
  }

  public void start() {
    MiniZXIO io = new MiniZXIO();
    ooz80 = createOOZ80(io);
    if (showScreen)
      MiniZX.createScreen(io.miniZXKeyboard, this.getMemFunction());

    RegistersBase registersBase = new RegistersBase<>(ooz80.getState());

    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
    State<T> state = ooz80.getState();
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);

    if (inThread)
      new Thread(this::emulate).start();
    else
      emulate();
  }

  public void emulate() {
    int i = 0;
    while (ooz80.getState().getPc().read().intValue() != emulateUntil) {
      if (i++ % (pause * 1000) == 0) this.ooz80.getState().setINTLine(true);
      else {
        if (i % pause == 0)
          this.ooz80.execute();
      }
    }
  }
}
