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

package com.fpetrola.oozx.speccy.modules.z80;

import com.fpetrola.z80.cpu.GeneratedZ80;
import com.fpetrola.z80.cpu.GeneratedZ80Cpu;
import com.fpetrola.z80.registers.RegisterBank;
import fuse.tstates.Contention;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.speccy.*;
import com.fpetrola.oozx.speccy.machine.TimingsHandler;
import com.fpetrola.oozx.speccy.modules.*;
import com.fpetrola.oozx.speccy.modules.Timer;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.peripherals.*;
import com.fpetrola.oozx.speccy.pokes.PokFile;
import com.fpetrola.oozx.speccy.pokes.PokInstruction;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.bytecode.RegistersBase;
import com.fpetrola.emulation.helpers.snapshots.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.AbstractMemory;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.MemptrUpdateInstructionSpy;
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.oozx.fuse.modules.z80.TestFusePhaseProcessor;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;

import java.io.File;

import static com.fpetrola.z80.registers.RegisterName.*;
import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.oozx.speccy.Emulation;

@Singleton
public class Z80 implements ZxModule, Cpu {
  public static double emulationSpeed;
  private final EventManager eventManager;
  public final com.fpetrola.oozx.Memory memory;

  public long interruptsEnabledAt;
  public OOZ80 ooz80;
  public LibretroCore.bridge_command bridgeCommand;
  private PhaseProcessor phaseProcessor;

  private final IO io;
  private final UserInterface userInterface;
  private int z80_interrupt_event;
  //   ZXScreenComponent<WordNumber> zxScreenComponent = new ZXScreenComponent();
//   MemoryWriteListener<WordNumber> writeListener = zxScreenComponent.getWriteListener();
  private boolean init;
  //  public Audio audio;
  private final Display display;
  public final Ula ula;
  private final Machine machine;
  private Keyboard keyboard;
  public SpectrumZ80Clock zxClock;
  private Input input;
  private final PeripheralBus peripherals;
  private final UiDisplay uiDisplay;
  private volatile boolean emulatorPaused;

  public boolean isPaused() {
    return emulatorPaused;
  }

  public void setPaused(boolean paused) {
    emulatorPaused = paused;
  }

  public UserInterface userInterface() {
    return userInterface;
  }
  private final Timer timer;
  public EmulatorControl mockCore;
  /**
   * Work asked for from outside the emulation, run where stopping is safe.
   * <p>
   * Volatile because the asking is done on the event thread and the running on this one.
   */
  /**
   * Work asked for from another thread - the window, a menu - and done here, between instructions.
   * Selecting a machine rebuilds the port list and changing speed hands every sound source a new
   * synth; neither can happen while this thread is in the middle of a frame, and doing it from the
   * event thread is what used to hang the emulator on a fast enough double click.
   */
  private final java.util.concurrent.ConcurrentLinkedQueue<Runnable> pending = new java.util.concurrent.ConcurrentLinkedQueue<>();
  private final Module module;
  private final EmulationSession session;
  private final Sound sound;
  private final Settings settings;
  private final Tape tape;
  private final byte[][] screenBytes = new byte[1000][1000];
  private Memory memory1;
  private final PcTraps beforeFetch = new PcTraps();
  private final PcTraps afterInstruction = new PcTraps();
  private int z80_nmi_event;

  @Inject
  public Z80(EventManager eventManager, com.fpetrola.oozx.Memory memory, Display display, Ula ula, Machine machine, Keyboard keyboard, SpectrumZ80Clock zxClock, Input input, PeripheralBusDelegate peripherals, UiDisplay uiDisplay, Timer timer, Module module, EmulationSession session, Sound sound, Settings settings, Tape tape, IO io, UserInterface userInterface) {
    this.eventManager = eventManager;
    this.memory = memory;
    this.display = display;
    this.ula = ula;
    this.machine = machine;
    this.keyboard = keyboard;
    this.zxClock = zxClock;
    this.input = input;
    this.peripherals = peripherals;
    this.uiDisplay = uiDisplay;
    this.timer = timer;
    this.module = module;
    this.session = session;
    this.sound = sound;
    this.settings = settings;
    this.tape = tape;
    this.io = io;
    this.userInterface = userInterface;
  }

  @Override
  public void machineWasReset(boolean hard) {
    reset(hard ? 1 : 0);
  }

  public void reset(int hardReset) {
    ooz80.reset();

    State state = ooz80.getState();

    state.getRegister(AF).write(0xffff);
    state.getRegister(AFx).write(0xffff);
    state.getRegister(BC).write(0);
    state.getRegister(DE).write(0);
    state.getRegister(HLx).write(0);
    state.getRegister(BC).write(0);
    state.getRegister(DEx).write(0);
    state.getRegister(HLx).write(0);
    state.getRegister(IX).write(0);
    state.getRegister(IY).write(0);
    state.getRegister(PC).write(0);
    state.getRegister(SP).write(0xffff);


    state.getRegister(I).write(0);
    state.getRegister(R).write(0);

    interruptsEnabledAt = -1;
  }

  public void toSnapshot(Libspectrum.Snap snap) {

  }

  public void fromSnapshot(Libspectrum.Snap snap) {

  }

  @Override
  public SpectrumZ80Clock getClock() {
    return zxClock;
  }

  @Override
  public EmulatorControl getEmulatorCore() {
    return mockCore;
  }

  @Override
  public void rebaseInterruptWindow(int frameLength) {
    if (interruptsEnabledAt >= 0) {
      interruptsEnabledAt -= frameLength;
    }
  }

  public void interrupt() {
    int i = TimingsHandler.interruptLength(machine.current.getBaseTiming());
    if (ooz80.getState().isIff1() && zxClock.getTStates() < i) {
      zxClock.addTStates(7, "interrupt");
      ooz80.interruption();
    }
  }

  private void initNoTest() {

    // The generated core carries its contention inside, so the aspect is not told about its accesses.
    memory1 = new ContendedMemory(memory, ula, display, zxClock, !generatedCore());
    var state = createState(memory1);
    createOOZ80(state);
    phaseProcessor = new FusePhaseProcessor(this);
    ((ContendedMemory) memory1).watchedBy(phaseProcessor);

    uiDisplay.screenMatrix = screenBytes;
    setupExecutionFetcher();
  }

  private void initTest() {
    createMemoryTEst();
    var state = createState(memory1);
    createOOZ80(state);
    uiDisplay.screenMatrix = screenBytes;
    setupMemoryTEst();
    setupExecutionFetcher();
  }

  /** The generated core carries MEMPTR and the contention inside; the OOP core gets them as listeners. */
  private void setupExecutionFetcher() {
    if (ooz80 instanceof GeneratedZ80Cpu)
      return;
    ooz80.getInstructionExecutor().setExecutionListener(phaseProcessor);
    new MemptrUpdateInstructionSpy(ooz80.getState()).addExecutionListeners(ooz80.getInstructionExecutor());
  }

  private void setupMemoryTEst() {
    phaseProcessor = new TestFusePhaseProcessorZ80(this);
    memory1.addMemoryReadListener(new AddStatesMemoryReadListener((TestFusePhaseProcessor) phaseProcessor) {
      protected void doRead(int address, int value, int fetching) {
        memory.readByte(address, ula);
      }
    });
    memory1.addMemoryWriteListener(new AddStatesMemoryWriteListener((TestFusePhaseProcessor) phaseProcessor) {
      protected void doWrite(int address, int value) {
        memory.writeByte(address, (byte) (value & 0xff), ula, display);
      }
    });
  }


  private void createMemoryTEst() {
    memory1 = new AbstractMemory() {
      protected int doRead(final int address) {
        return memory.readByteInternal(address);
      }

      protected void doWrite(final int address, final int value) {
        memory.writeByteInternal2(address, (byte) value);
      }

      public void reset() {
        memory.reset();
      }
    };
  }

  public static boolean generatedCore() {
    return "generated".equals(System.getProperty("oozx.cpu"));
  }

  private GeneratedZ80 generated;

  private void createOOZ80(State state1) {
    if (generated != null)
      ooz80 = new GeneratedZ80Cpu(state1, generated);
    else
      ooz80 = new OOZ80(state1, Helper.getInstructionFetcher(state1, new NullInstructionSpy(), new DefaultInstructionFactory(state1)), new DefaultInstructionExecutor(state1, false));
  }

  private RegisterBank createBank(Memory memory2) {
    if (!generatedCore())
      return new DefaultRegisterBankFactory().createBank();
    generated = new GeneratedZ80(memory2, io) {
      public void contend(int address, int times, int tstates, Contention.Kind kind) {
        phaseProcessor.contend(address, times, tstates, kind);
      }
    };
    return generated;
  }

  private State createState(Memory memory2) {
    var state1 = new State(io, createBank(memory2), memory2) {
      public void enableInterrupt() {
        super.enableInterrupt();
        interruptsEnabledAt = clock.getTStates();
        eventManager.eventAdd(clock.getTStates() + 1, z80_interrupt_event);
      }
    };
    state1.clock = zxClock;
    return state1;
  }

  public void loadSnap(String url) {
    SpectrumState snapshot = SnapshotLoader.readSnapshot(url);
    if (snapshot == null) {
      return;
    }
    loadSnap(snapshot);
    ooz80.getState().clock.setTStates(tStatesOf(url, snapshot));
  }

  /**
   * How far into a frame the machine was when the snapshot was taken.
   * <p>
   * Only a .z80 needs asking twice: its loader here ends by zeroing the count, so the number is
   * recovered by handing the file to libspectrum. Every other format keeps what it read, and
   * handing one of those to the same call was asking libspectrum to read an SZX as a .z80 - the
   * format is named in the call and it was named Z80 whatever the file was. It answered with
   * error 4 and the whole load failed on it, which is how Beach Head II came to be a recording
   * that would not open at all.
   * <p>
   * A probe that fails is not worth failing a load over either: being a frame's worth of
   * T-states out is a cosmetic matter next to not running.
   */
  private int tStatesOf(String url, SpectrumState snapshot) {
    if (!url.toLowerCase().endsWith(".z80")) {
      return snapshot.getTstates();
    }
    try {
      return Z80Loader.getTstates(LibSpectrum.INSTANCE, url);
    } catch (RuntimeException e) {
      return snapshot.getTstates();
    }
  }

  /**
   * Puts a snapshot into the machine, on the machine it was taken on.
   * <p>
   * A snapshot is not just registers and bytes: it names a model, and a 128K one cannot be
   * poured into a 48K map. It has eight banks where a 48K machine has three, and the game goes
   * on paging them - writing a bank number to 0x7FFD and carrying on at 0xC000 expecting to find
   * it there. Flattening banks 5, 2 and 0 into a 48K machine and dropping the rest gets you a
   * screen that looks right and a game that runs off into whatever the one bank it kept happens
   * to hold, a few frames later. So the machine is chosen first and the snapshot loaded into it,
   * bank for bank, with the paging it was saved under put back.
   */
  public void loadSnap(SpectrumState spectrumState) {
    selectMachineFor(spectrumState);

    State state = ooz80.getState();
    RegistersBase registersBase = new RegistersBase(state);
    if (spectrumState.getSpectrumModel().codeModel == com.fpetrola.emulation.helpers.machine.MachineTypes.CodeModel.SPECTRUM48K) {
      SnapshotLoader.setupFromSpectrumState(registersBase, state, spectrumState);
    } else {
      loadPagedRam(spectrumState);
      SnapshotLoader.setZ80State(registersBase, spectrumState.getZ80State());
      state.clock.setTStates(spectrumState.getTstates());
    }
    interruptsEnabledAt = -1;
    updateMemory();
    display.refreshAll();
  }

  /**
   * Becomes the machine the snapshot was taken on, if it is not already it.
   * <p>
   * Only when it differs: selecting a model resets it, and a 48K snapshot arriving at a machine
   * that is already a 48K one has nothing to gain from that. Going through the default first is
   * how a model change is done everywhere else here, so that the new machine starts from a state
   * that is known rather than from the leftovers of the last one.
   */
  private void selectMachineFor(SpectrumState snapshot) {
    MachineTypes wanted = snapshot.getSpectrumModel();
    if (machine.current != null && machine.current.snapshotModel() == wanted) {
      return;
    }
    machine.forSnapshotModel(wanted).ifPresentOrElse(type -> {
      machine.selectDefault();
      machine.select(type);
    }, () -> userInterface.error(UiError.ERROR,
        "this build has no %s, so the snapshot is loaded into the machine already running", wanted));
  }


  /**
   * Copies the snapshot's eight banks into the machine's, then puts back the paging it was saved
   * under - which is the half that matters, because it decides what the game finds at 0xC000 when
   * it resumes, and which of the two screens is the one being shown.
   * <p>
   * The paging goes back through the machine's own port rather than by mapping pages here, so
   * that whatever else a model hangs off that port - the ROM it selects, the shadow screen, the
   * lock bit that a game sets once and relies on - happens the way it does when a game writes it.
   */
  private void loadPagedRam(SpectrumState spectrumState) {
    int[][] ram = memory.getRAM();
    for (int bank = 0; bank < 8 && bank < ram.length; bank++) {
      byte[] page = spectrumState.getMemoryState().getPageRam(bank);
      if (page == null) {
        continue;
      }
      for (int i = 0; i < page.length; i++) {
        ram[bank][i] = page[i] & 0xff;
      }
    }
    io.out(0x7ffd, spectrumState.getPort7ffd() & 0xff);
    if (spectrumState.getSpectrumModel().codeModel == com.fpetrola.emulation.helpers.machine.MachineTypes.CodeModel.SPECTRUMPLUS3) {
      io.out(0x1ffd, spectrumState.getPort1ffd() & 0xff);
    }
    restoreSoundChip(spectrumState);
  }

  /**
   * The AY's sixteen registers, put back the way the paging is: through the chip's own ports.
   * <p>
   * A snapshot is taken mid-tune and carries the chip's state, and this was dropping it. What was
   * restored was a machine playing whatever the last chip to be written had been playing, until
   * the game happened to rewrite each register - which for a held note or an envelope is not soon,
   * and for a recording is never, because the recording replays the writes that came after.
   * <p>
   * Through the ports rather than into the synthesis, so that the register file the machine can
   * read back and the sound coming out of it cannot disagree about what the chip was set to.
   */
  private void restoreSoundChip(SpectrumState snapshot) {
    if (machine.current == null || !machine.current.has(com.fpetrola.oozx.MachineCapability.AY)) {
      return;
    }
    com.fpetrola.emulation.helpers.snapshots.AY8912State chip = snapshot.getAY8912State();
    if (chip == null || chip.getRegAY() == null) {
      return;
    }
    int[] registers = chip.getRegAY();
    for (int register = 0; register < 16 && register < registers.length; register++) {
      io.out(0xfffd, register);
      io.out(0xbffd, registers[register] & 0xff);
    }
    io.out(0xfffd, chip.getAddressLatch() & 0x0f);
  }

  public void start() {
    z80_interrupt_event = eventManager.eventRegister(this::z80_interrupt_event_fn, "Retriggered interrupt");
    z80_nmi_event = eventManager.eventRegister(this::z80_nmi, "Non-maskable interrupt");
    int z80_nmos_iff2_event = eventManager.eventRegister(null, "IFF2 update dummy event");

    module.register(this);

    if (Emulation.noTest)
      initNoTest();
    else
      initTest();

    return;
  }

  public void end() {
  }

  private final java.util.List<Runnable> nmiListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

  /** Eleven T-states: five of the processor's own, and the two pushes the memory counts. */
  private void z80_nmi(long l, int i, Object o) {
    zxClock.addTStates(5, "nmi");
    nmiListeners.forEach(Runnable::run);
    ooz80.nmi();
  }

  @Override
  public void onNmi(Runnable listener) {
    nmiListeners.add(listener);
  }

  @Override
  public void offNmi(Runnable listener) {
    nmiListeners.remove(listener);
  }

  @Override
  public void jump(int address) {
    ooz80.getState().getPc().write(address);
  }

  @Override
  public void rst(int vector) {
    var state = ooz80.getState();
    com.fpetrola.z80.instructions.impl.Push.doPush((state.getPc().read() + 1) & 0xffff, state.getRegisterSP(), state.getMemory());
    state.getPc().write(vector);
  }

  @Override
  public void nmi() {
    eventManager.eventAdd(zxClock.getTStates(), z80_nmi_event);
  }

  @Override
  public PcTraps beforeFetch() {
    return beforeFetch;
  }

  @Override
  public PcTraps afterInstruction() {
    return afterInstruction;
  }

  /** One instruction, and whatever is watching the address it was fetched from is told. */
  public void step() {
    if (beforeFetch.armed() || afterInstruction.armed()) {
      int pc = ooz80.getState().getPc().read();
      beforeFetch.at(pc);
      ooz80.execute();
      afterInstruction.at(pc);
    } else {
      ooz80.execute();
    }
  }

  private void z80_interrupt_event_fn(long l, int i, Object o) {
    interrupt();
  }

  public void doOpcodes() {
    while (zxClock.getTStates() < eventManager.eventNextEvent) {
      while (emulatorPaused) Thread.onSpinWait();
      bridgeCommand.invoke(0, null);
      try {
        step();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    applyWhatWasDeferred();
  }

  /**
   * Does whatever was asked for with {@link #later}, which every loop that advances this machine
   * has to call.
   * <p>
   * It used to happen at the end of doOpcodes and nowhere else, which was the same thing while
   * doOpcodes was the only way a machine ran. A recording drives its machine frame by frame
   * instead, through RzxSession, so while one played nothing was ever applied: changing the
   * speed, changing the machine, plugging a device in - all queued and none of it happening,
   * with no sign that anything had been asked for.
   */
  public void applyWhatWasDeferred() {
    for (Runnable work = pending.poll(); work != null; work = pending.poll()) {
      work.run();
    }
  }

  /** Ask for something to be done on the emulator's own thread, as soon as it is between instructions. */
  public void later(Runnable work) {
    pending.add(work);
  }

  public void updateMemory() {
    Memory memory1 = ooz80.getState().getMemory();

//    int tStates = zxClock.getTStates();
    // readByteInternal is the read with no clock and no contention, which is what this wants.
    for (int i = 0x4000; i < 0x8000; i++)
      memory.writeByteInternal(i, (byte) (memory.readByteInternal(i) & 0xff), display);
  }

  public void changeSpeed(int emulationSpeed) {
    settings.current.emulationSpeed = emulationSpeed;
    zxClock.rebaseTStates(60000);
    timer.changeSpeed(emulationSpeed);
    sound.speedChanged();
  }

}
