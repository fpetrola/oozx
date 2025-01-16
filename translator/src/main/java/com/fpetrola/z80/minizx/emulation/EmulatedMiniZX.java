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

import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.NullBlockChangesListener;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.factory.Z80Factory;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxParser;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.jspeccy.Z80B;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.minizx.*;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.*;
import snapshots.SpectrumState;

import java.util.function.Function;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class EmulatedMiniZX<T extends WordNumber> {
  private Emulator emulator;
  public OOZ80<T> ooz80;
  private int pause;

  private String url;
  private boolean showScreen;
  private int emulateUntil;
  private boolean inThread;
  private InstructionSpy spy;
  private State<T> state;

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread, Emulator emulator) {
    this(emulator, url, pause, showScreen, emulateUntil, inThread, new NullInstructionSpy(), createState());
  }

  private void createSpy() {
  }

  public EmulatedMiniZX(Emulator emulator, String url, int pause, boolean showScreen, int emulateUntil, boolean inThread, InstructionSpy spy, State state) {
    this.emulator = emulator;
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
    String url = "file:///home/fernando/dynamitedan1.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/rickdangerous";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/wally.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/equinox.z80";

    new EmulatedMiniZX(url, 1, true, -1, true, new DefaultEmulator()).start();
  }

  public <T extends WordNumber> OOZ80<T> createOOZ80() {
    if (state == null)
      state = createState();
    ((MiniZXIO) state.getIo()).setPc(state.getPc());
    OOZ80<T> ooz81 = Z80Factory.createOOZ80(state, new CachedInstructionFetcher<>(state));
//    OOZ80<T> ooz81 = Z80Factory.createOOZ80(state);
    ooz81.getInstructionFetcher().setPrefetch(true);

    spy.reset(state);
    spy.addExecutionListeners(ooz81.getInstructionExecutor());
    return ooz81;
  }

  public <T extends WordNumber> OOZ80<T> createOOZ802() {
    if (state == null)
      state = createState();
//    ((MiniZXIO) state.getIo()).setPc(state.getPc());
    spy.reset(state);

//    new DataflowService() {
//    }, new RoutineFinder(new RoutineManager()));

    BlocksManager blocksManager = new BlocksManager(new NullBlockChangesListener(), false);

    OOZ80 completeZ80 = Z80B.createCompleteZ80(true, spy, blocksManager, state);
    return completeZ80;
  }


  public static State createState() {
    return new State(new RZXPlayerIO(), new DefaultMemory(true));
//    return new State(new DefaultMiniZXIO(), new DefaultMemory(true));
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> ooz80.getState().getMemory().read(createValue(index), 0).intValue();
  }

  public void start() {
    MiniZXIO io = ((MiniZXIO) state.getIo());
    ooz80 = createOOZ80();
    State<T> state = ooz80.getState();
    RegistersBase registersBase = new RegistersBase<>(state);

    if (!true) {
      String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
      SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);
    } else
      useRzx(registersBase, state, io);

    if (showScreen) {
      MiniZXScreen miniZXScreen1 = new MiniZXScreen(this.getMemFunction());
      ZXScreenComponent zxScreenComponent = new ZXScreenComponent();
      MiniZX.createScreen(io.getMiniZXKeyboard(), zxScreenComponent);
      MemoryWriteListener<T> writeListener = zxScreenComponent.getWriteListener();
      state.getMemory().addMemoryWriteListener(writeListener);
      for (int i = 0; i < 0xFFFF; i++) {
        zxScreenComponent.onMemoryWrite(i, state.getMemory().getData()[i].intValue());
      }
    }


    if (inThread)
      new Thread(() -> emulator.emulate(ooz80, emulateUntil, pause)).start();
    else
      emulator.emulate(ooz80, emulateUntil, pause);
  }

  private void useRzx(RegistersBase registersBase, State<T> state, MiniZXIO io) {
    String name;
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/greatescape/greatescape.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/wally1.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/jsw/Jet Set Willy - Mildly Patched.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/exolon.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/dynamitedan/dynamitedan.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/eawally/eawally.rzx";

    RzxFile rzxFile = new RzxParser().parseFile(name);
    SpectrumState spectrumState = RzxParser.loadSnapshot(rzxFile);
    SnapshotLoader.setupStateFromSpectrumState(spectrumState, registersBase, state);
//    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);

    if (io instanceof RZXPlayerIO<?> rzxPlayerIO)
      rzxPlayerIO.setup(rzxFile, ooz80);
  }

}
