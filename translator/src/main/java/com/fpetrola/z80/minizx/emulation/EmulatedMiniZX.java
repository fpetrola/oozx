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
import com.fpetrola.z80.cpu.SpyInstructionExecutor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.ide.Z80Debugger;
import com.fpetrola.z80.ide.Z80Emulator;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.MiniZXScreen;
import com.fpetrola.z80.minizx.ZXScreenComponent;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.*;

import javax.swing.*;
import java.util.function.Function;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class EmulatedMiniZX<T extends WordNumber> {
  public OOZ80<T> ooz80;
  private int pause;

  private String url;
  private boolean showScreen;
  private final int emulateUntil;
  private boolean inThread;
  private InstructionSpy spy;
  private State<T> state;
  private SpyInstructionExecutor instructionExecutor2;

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread) {
    this(url, pause, showScreen, emulateUntil, inThread, new NullInstructionSpy(), createState(new NullInstructionSpy()));
  }

  private void createSpy() {
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
    InstructionSpy spy = new AbstractInstructionSpy() {
    };
    new EmulatedMiniZX("file:///home/fernando/dynamitedan1.z80", 100, true, -1, true).start();
  }

  public <T extends WordNumber> OOZ80<T> createOOZ80() {
    if (state == null)
      state = createState(spy);
    ((MiniZXIO) state.getIo()).setPc(state.getPc());
    spy.reset(state);
    instructionExecutor2 = new SpyInstructionExecutor(spy, state);
    return new OOZ80(state, Helper.getInstructionFetcher2(state, spy, new DefaultInstructionFactory<T>(state), true, instructionExecutor2));
  }

  public static State createState(InstructionSpy spy1) {
    MiniZXIO io = new MiniZXIO();

    return new State(io, new SpyRegisterBankFactory<>(spy1).createBank(), new MockedMemory(true));
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> ooz80.getState().getMemory().read(createValue(index), 0).intValue();
  }

  public void start() {
    MiniZXIO io = ((MiniZXIO) state.getIo());
    ooz80 = createOOZ80();
    RegistersBase registersBase = new RegistersBase<>(ooz80.getState());

    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
    State<T> state = ooz80.getState();
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);

    if (showScreen) {
      MiniZXScreen miniZXScreen1 = new MiniZXScreen(this.getMemFunction());
      ZXScreenComponent zxScreenComponent = new ZXScreenComponent();
      MiniZX.createScreen(io.miniZXKeyboard, zxScreenComponent);
      MemoryWriteListener<T> writeListener = zxScreenComponent.getWriteListener();
      state.getMemory().addMemoryWriteListener(writeListener);
      for (int i = 0; i < 0xFFFF; i++) {
        zxScreenComponent.onMemoryWrite(i, state.getMemory().getData()[i].intValue());
      }
    }


    if (inThread)
      new Thread(this::emulate).start();
    else
      emulate();
  }


  public void emulate() {
    RegisterSpy<T> pc = (RegisterSpy<T>) ooz80.getState().getPc();
    Z80Emulator emulator1 = new Z80EmulatorBridge(pc, ooz80, emulateUntil, pause);
    SwingUtilities.invokeLater(() -> Z80Debugger.createAndShowGUI(emulator1));
    pc.addRegisterWriteListener(emulator1.getRegisterWriteListener());
  }

}
