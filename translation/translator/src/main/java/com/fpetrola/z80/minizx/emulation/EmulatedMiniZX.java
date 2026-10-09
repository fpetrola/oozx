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
import com.fpetrola.z80.cpu.FetchListener;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.bytecode.RegistersBase;
import com.fpetrola.emulation.helpers.snapshots.SnapshotLoader;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.MiniZXScreen;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.minizx.MiniZXKeyboard;
import com.fpetrola.z80.tstates.UncontendedTiming;
import com.fpetrola.z80.cpu.InstructionFetcher;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.fpetrola.z80.ide.rzx.RzxFile;
import com.fpetrola.z80.ide.rzx.RzxParser;
import com.fpetrola.z80.ide.rzx.SnapshotBlock;
import com.fpetrola.z80.minizx.RZXPlayerIO;
import com.fpetrola.z80.minizx.RzxPlayback;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.function.Function;

public class EmulatedMiniZX {
  public OOZ80 ooz80;
  private int pause;

  private String url;
  private boolean showScreen;
  private final int emulateUntil;
  private boolean inThread;
  private StackAnalyzer stackAnalyzer;
  private String rzxFile;
  private RzxPlayback playback;
  private int stopAt = -1;
  private FetchListener fetchListener;
  private boolean timed;
  private MemoryWriteListener memoryWriteListener;
  private int[] program;
  private int programEntry, programStack, interruptEvery;

  public EmulatedMiniZX listening(FetchListener fetchListener) {
    this.fetchListener = fetchListener;
    return this;
  }

  public EmulatedMiniZX listening(MemoryWriteListener memoryWriteListener) {
    this.memoryWriteListener = memoryWriteListener;
    return this;
  }

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread) {
    this.pause = pause;
    //    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile("file:///home/fernando/detodo/desarrollo/m/zx/zx/jsw.z80");
    this.url = url;
    this.showScreen = showScreen;
    this.emulateUntil = emulateUntil;
    this.inThread = inThread;
  }

  public EmulatedMiniZX(String url, int pause, boolean showScreen, int emulateUntil, boolean inThread, StackAnalyzer stackAnalyzer) {
    this(url, pause, showScreen, emulateUntil, inThread);
    this.stackAnalyzer = stackAnalyzer;
  }

  public EmulatedMiniZX interruptingEvery(int instructions) {
    interruptEvery = instructions;
    return this;
  }

  public EmulatedMiniZX stoppingAt(int address) {
    stopAt = address;
    return this;
  }

  public int playbackFetches() {
    return playback.getFetchCounter();
  }

  private static class Reached extends RuntimeException {
  }

  public static EmulatedMiniZX ofProgram(int[] memory, int entry, int stack, int instructions, StackAnalyzer stackAnalyzer) {
    EmulatedMiniZX emulatedMiniZX = new EmulatedMiniZX(null, 1, false, instructions, false, stackAnalyzer);
    emulatedMiniZX.program = memory;
    emulatedMiniZX.programEntry = entry;
    emulatedMiniZX.programStack = stack;
    return emulatedMiniZX;
  }

  public static EmulatedMiniZX ofRecording(String rzxFile, int frames, StackAnalyzer stackAnalyzer) {
    EmulatedMiniZX emulatedMiniZX = new EmulatedMiniZX(null, 1, false, frames, false, stackAnalyzer);
    emulatedMiniZX.rzxFile = rzxFile;
    return emulatedMiniZX;
  }

  public static void main(String[] args) {
    new EmulatedMiniZX("file:///home/fernando/dynamitedan1.z80", 1, true, -1, true).start();
  }

  public static  OOZ80 createOOZ80(MiniZXIO io) {
    var state = new State(io, new DefaultRegisterBankFactory().createBank(), new MockedMemory(true));
    io.setPc(state.getPc());
    return new OOZ80(state, Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory(state)), new DefaultInstructionExecutor(state, false));
  }

  /** The same machine counting its T-states as an uncontended Z80 would: the plan of every instruction, four per port access, seven per interrupt acknowledge. */
  public static OOZ80 createTimedOOZ80(MiniZXIO io) {
    TimedIO ports = new TimedIO(io);
    UncontendedTiming timing = new UncontendedTiming();
    State state = ports.state = new State(ports, new DefaultRegisterBankFactory().createBank(), timing.memory());
    io.setPc(state.getPc());
    DefaultInstructionExecutor executor = new DefaultInstructionExecutor(state, false);
    InstructionFetcher fetcher = Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory(state));
    timing.attach(state, executor, fetcher);
    return new OOZ80(state, fetcher, executor);
  }

  private static class TimedIO implements MiniZXIO {
    private final MiniZXIO io;
    private State state;

    TimedIO(MiniZXIO io) {
      this.io = io;
    }

    public int in(int port) {
      state.clock.addTStates(UncontendedTiming.PORT_ACCESS);
      return io.in(port);
    }

    public void out(int port, int value) {
      state.clock.addTStates(UncontendedTiming.PORT_ACCESS);
      io.out(port, value);
    }

    public MiniZXKeyboard getMiniZXKeyboard() {
      return io.getMiniZXKeyboard();
    }

    public void setPc(Register pc) {
      io.setPc(pc);
    }
  }

  public EmulatedMiniZX timed() {
    timed = true;
    return this;
  }

  public static <S extends Integer> Function<java.lang.Integer, java.lang.Integer> getMemFunction(OOZ80 ooz81) {
    return index -> {
      return ooz81.getState().getMemory().read(index, 10);
    };
  }

  public void start() {
    MiniZXIO io = rzxFile == null ? new DefaultMiniZXIO() : new RZXPlayerIO();
    ooz80 = timed ? createTimedOOZ80(io) : createOOZ80(io);
    if (fetchListener != null)
      ooz80.getInstructionFetcher().addFetchListener(fetchListener);
    if (memoryWriteListener != null)
      ooz80.getState().getMemory().addMemoryWriteListener(memoryWriteListener);
    if (stackAnalyzer != null) {
      stackAnalyzer.reset(ooz80.getState());
      stackAnalyzer.addExecutionListener(ooz80.getInstructionExecutor());
    }
    if (showScreen)
      MiniZX.createScreen(io.getMiniZXKeyboard(), new MiniZXScreen(getMemFunction(ooz80)));

    RegistersBase registersBase = new RegistersBase(ooz80.getState());

    State state = ooz80.getState();
    if (program != null) {
      System.arraycopy(program, 0, state.getMemory().getData(), 0, program.length);
      state.getPc().write(programEntry);
      state.getRegisterSP().write(programStack);
      state.setIntMode(State.InterruptionMode.IM0);
    } else if (rzxFile == null)
      SnapshotLoader.setupStateWithSnapshot(registersBase, com.fpetrola.z80.helpers.Helper.getSnapshotFile(url), state);
    else {
      RzxFile recording = new RzxParser().parseFile(rzxFile);
      SnapshotLoader.setupStateWithSnapshot(registersBase, snapshotFileOf(recording), state);
      playback = new RzxPlayback(ooz80, (RZXPlayerIO) io, recording, tstates -> {
      }, () -> {
        if (state.getPc().read() == stopAt)
          throw new Reached();
        ooz80.execute();
      });
    }
    state.getMemory().protect(0, SpectrumApplication.ROM_END);

//    PhaseProcessor phaseProcessor = new PhaseProcessor(ooz80);
//    Memory memory = state.getMemory();
//    memory.addMemoryReadListener(new AddStatesMemoryReadListener<>(phaseProcessor));
//    memory.addMemoryWriteListener(new AddStatesMemoryWriteListener<>(phaseProcessor));

    if (inThread)
      new Thread(this::emulate).start();
    else
      emulate();
  }

  private static String snapshotFileOf(RzxFile recording) {
    SnapshotBlock block = recording.getSnapshotBlock();
    try {
      Path file = Files.createTempFile("rzx-snapshot", "." + block.getSnapshotExtension());
      Files.write(file, block.getSnapshotData());
      return file.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void emulate() {
    if (playback != null) {
      try {
        playback.playFrames(emulateUntil < 0 ? Integer.MAX_VALUE : emulateUntil);
      } catch (Reached reached) {
      }
      return;
    }
    int every = emulateUntil < 0 ? pause * 1000 : interruptEvery;
    for (int i = 0; emulateUntil < 0 || i < emulateUntil; i++) {
      if (every > 0 && i % every == 0)
        ooz80.getState().setINTLine(true);
      else if (emulateUntil >= 0 || i % pause == 0)
        ooz80.execute();
    }
  }

}
