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
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxParser;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.jspeccy.Z80B;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.minizx.*;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.*;
import com.fpetrola.z80.transformations.StackAnalyzer;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;
import snapshots.SpectrumState;

import java.util.function.Function;
import java.util.function.Predicate;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class EmulatedMiniZX<T extends WordNumber> {
  public static boolean useRZX = false;
  private static String rzxFile;
  private StackAnalyzer stackAnalyzer;
  private Emulator emulator;
  public OOZ80<T> ooz80;
  private int pause;

  private String url;
  private boolean showScreen;
  private int emulateUntil;
  private boolean inThread;
  private InstructionSpy spy;
  private State<T> state;
  private boolean cachingInstructions = false;

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread, Emulator emulator, StackAnalyzer stackAnalyzer) {
    this(emulator, url, pause, showScreen, emulateUntil, inThread, new NullInstructionSpy(), createState());
//    this.stackAnalyzer = new StackAnalyzer(state);
//    this.stackAnalyzer.setCollecting(true);
    this.stackAnalyzer = stackAnalyzer;
  }

  public static void setRzxFile(String rzxFile) {
    EmulatedMiniZX.rzxFile = rzxFile;
    useRZX = rzxFile != null;
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
    Helper.hex = true;

    String url;
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/wally.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/equinox.z80";
    url = "file:///home/fernando/dynamitedan1.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/emlyn.z80";
    url = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/eawally/eawally.rzx";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/rickdangerous";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/emlynh.z80";
    url = "file:///home/fernando/detodo/desarrollo/m/zx/roms/batman48.z80";
    url = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/batman1.rzx";
    url = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/emlyn/emlyn5.rzx";
    setRzxFile(url);

    new EmulatedMiniZX(url, 10, true, -1, true, new DefaultEmulator(), null).start();
  }

  public <T extends WordNumber> OOZ80<T> createOOZ80() {
    if (state == null)
      state = createState();

    ((MiniZXIO) state.getIo()).setPc(state.getPc());

    OOZ80<T> ooz81 = cachingInstructions ? Z80Factory.createOOZ80(state, new CachedInstructionFetcher<>(state)) : Z80Factory.createOOZ80(state);

    ooz81.getInstructionFetcher().setPrefetch(true);
    if (stackAnalyzer != null) {
      InstructionExecutor<T> instructionExecutor = ooz81.getInstructionExecutor();
      stackAnalyzer.reset(state);
      stackAnalyzer.addExecutionListener(instructionExecutor);
    }
    spy.reset(state);
    spy.addExecutionListeners(ooz81.getInstructionExecutor());

//    addTStatesUpdater(ooz81);
    return ooz81;
  }

  private <T extends WordNumber> void addTStatesUpdater(Z80Cpu<T> ooz81) {
    Memory<T> memory = (Memory<T>) state.getMemory();
    PhaseProcessor<T> phaseProcessor = new PhaseProcessor<>(ooz81);
    memory.addMemoryReadListener(new AddStatesMemoryReadListener<T>(phaseProcessor));
    memory.addMemoryWriteListener(new AddStatesMemoryWriteListener<T>(phaseProcessor));
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
    return useRZX ? new State(new RZXPlayerIO(), new DefaultMemory(true)) : new State(new DefaultMiniZXIO(), new DefaultMemory(true));
  }

  protected Function<Integer, Integer> getMemFunction() {
    return index -> ooz80.getState().getMemory().read(createValue(index), 0).intValue();
  }

  public void start() {
    MiniZXIO io = ((MiniZXIO) state.getIo());
    ooz80 = createOOZ80();
    State<T> state = ooz80.getState();
    RegistersBase registersBase = new RegistersBase<>(state);

    if (!useRZX) {
      String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
      SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);
    } else
      useRzx(registersBase, state, io);


    if (showScreen) {
//      MiniZXScreen miniZXScreen1 = new MiniZXScreen(this.getMemFunction());
      ZXScreenComponent zxScreenComponent = new ZXScreenComponent();
      MiniZX.createScreen(io.getMiniZXKeyboard(), zxScreenComponent);
      MemoryWriteListener<T> writeListener = zxScreenComponent.getWriteListener();
      state.getMemory().addMemoryWriteListener(writeListener);
      for (int i = 0; i < 0xFFFF; i++) {
        zxScreenComponent.onMemoryWrite(i, state.getMemory().getData()[i].intValue());
      }
    }

    Predicate<Integer> continueEmulation = useRZX ? (i) -> emulateUntil == -1 || ((RZXPlayerIO) io).getCurrentFrameIndex() < emulateUntil : (i) -> emulateUntil == -1 || i < emulateUntil;
    Predicate<Integer> interruptionCondition;
    if (useRZX) {
      interruptionCondition = ((RZXPlayerIO) io).getInterruptionCondition();
    } else {
      interruptionCondition = (i) -> (i++ % pause * 100) == pause * 100 - 1;
    }
    if (inThread)
      new Thread(() -> emulator.emulate(ooz80, emulateUntil, pause, continueEmulation, interruptionCondition)).start();
    else
      emulator.emulate(ooz80, emulateUntil, pause, continueEmulation, interruptionCondition);
  }

  private void useRzx(RegistersBase registersBase, State<T> state, MiniZXIO io) {
    String name;
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/wally1.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/exolon.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/greatescape/greatescape.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/dynamitedan/dynamitedan.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/jsw/Jet Set Willy - Mildly Patched.rzx";
    name = "/home/fernando/detodo/desarrollo/m/zx/roms/recordings/eawally/eawally.rzx";
    name = rzxFile;

    RzxFile rzxFile = new RzxParser().parseFile(name);
    SpectrumState spectrumState = RzxParser.loadSnapshot(rzxFile);
    SnapshotLoader.setupStateFromSpectrumState(spectrumState, registersBase, state);
//    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);

    if (io instanceof RZXPlayerIO<?> rzxPlayerIO)
      rzxPlayerIO.setup(rzxFile, ooz80);
  }

}
