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
import com.fpetrola.z80.minizx.MiniZXScreen;
import com.fpetrola.z80.minizx.ZXScreenComponent;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.InstructionSpy;
import com.fpetrola.z80.spy.NullInstructionSpy;

import javax.swing.*;
import java.util.function.Function;

public class EmulatedMiniZX<T extends WordNumber> {
  public OOZ80<T> ooz80;
  private int pause;

  private String url;
  private boolean showScreen;
  private final int emulateUntil;
  private boolean inThread;
  private InstructionSpy spy;
  private State state;

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread) {
    this(url, pause, showScreen, emulateUntil, inThread, new NullInstructionSpy(), createState());
  }

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread, InstructionSpy spy, State state) {
    this.pause = pause;
    //    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile("file:///home/fernando/detodo/desarrollo/m/zx/zx/jsw.z80");
    this.url = url;
    this.showScreen = showScreen;
    this.emulateUntil = emulateUntil;
    this.inThread = inThread;
    this.spy = spy;
    this.state = state;
  }

  public static void main(String[] args) {
    new EmulatedMiniZX("file:///home/fernando/dynamitedan1.z80", 100, true, -1, true).start();
  }

  public <T extends WordNumber> OOZ80<T> createOOZ80() {
    if (state == null)
      state = createState();
    ((MiniZXIO) state.getIo()).setPc(state.getPc());
    spy.reset(state);
    return new OOZ80(state, Helper.getInstructionFetcher(state, spy, new DefaultInstructionFactory<T>(state), true));
  }

  public static State createState() {
    MiniZXIO io = new MiniZXIO();
    return new State(io, new DefaultRegisterBankFactory().createBank(), new MockedMemory(true));
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> ooz80.getState().getMemory().read(WordNumber.createValue(index), 0).intValue();
  }

  public void start() {
    MiniZXIO io = ((MiniZXIO) state.getIo());
    ooz80 = createOOZ80();
    if (showScreen) {
      MiniZXScreen miniZXScreen1 = new MiniZXScreen(this.getMemFunction());
      ZXScreenComponent zxScreenComponent = new ZXScreenComponent();
      MiniZX.createScreen(io.miniZXKeyboard, zxScreenComponent);
      state.getMemory().addMemoryWriteListener(zxScreenComponent.getWriteListener());
    }

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
      if (i++ % (pause * 10) == 0) this.ooz80.getState().setINTLine(true);
      else {
        if (i % pause == 0)
          this.ooz80.execute();
      }
    }
  }
}
