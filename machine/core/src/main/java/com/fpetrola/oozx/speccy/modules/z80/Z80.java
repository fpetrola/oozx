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
import com.fpetrola.z80.spy.NullInstructionSpy;
import com.fpetrola.oozx.fuse.modules.z80.TestFusePhaseProcessor;
import fuse.PhaseProcessorExecutionListener;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;
import fuse.tstates.phases.AfterMR;
import fuse.tstates.phases.BeforeWrite;
import com.fpetrola.emulation.helpers.snapshots.SpectrumState;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

import static com.fpetrola.z80.registers.RegisterName.*;

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
  private final IPeriph periph;
  private final UiDisplay uiDisplay;
  private volatile boolean emulatorPaused;
  private final Timer timer;
  public EmulatorCore mockCore;
  private Runnable changeMachine;
  private final Module module;
  private final EmulationSession session;
  private final Sound sound;
  private final Settings settings;
  private final Tape tape;
  private final byte[][] screenBytes = new byte[1000][1000];
  private Memory memory1;

  @Inject
  public Z80(EventManager eventManager, com.fpetrola.oozx.Memory memory, Display display, Ula ula, Machine machine, Keyboard keyboard, SpectrumZ80Clock zxClock, Input input, PeriphDelegate periph, UiDisplay uiDisplay, Timer timer, Module module, EmulationSession session, Sound sound, Settings settings, Tape tape, IO io, UserInterface userInterface) {
    this.eventManager = eventManager;
    this.memory = memory;
    this.display = display;
    this.ula = ula;
    this.machine = machine;
    this.keyboard = keyboard;
    this.zxClock = zxClock;
    this.input = input;
    this.periph = periph;
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
  public EmulatorCore getEmulatorCore() {
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

    memory1 = new Memory() {
      private boolean disabled;
      private final AfterMR afterMR = new AfterMR();
      private final BeforeWrite phase = new BeforeWrite();

      public int read(int address, int fetching) {
        int value = memory.readByteInternal(address);
        if (!disabled) {
          memory.readByte(address, ula);

          zxClock.addTStates(fetching == 1 ? 4 : 3);
          phaseProcessor.setAddress(address);
          phaseProcessor.readCount++;
          phaseProcessor.processPhase(afterMR);
        }
        return value;
      }

      public void write(int address, int value) {
        if (!disabled) {
          if (!phaseProcessor.state.isIntLine()) {
            phaseProcessor.processPhase(phase);
          }
          memory.writeByte(address, (byte) (value & 0xff), ula, display);
          phaseProcessor.writeCount++;
          zxClock.addTStates(3);
        }
        memory.writeByteInternal2(address, (byte) value);
      }

      public void reset() {
        memory.reset();
      }

      public void disableReadListener() {
        disabled = true;
      }

      public void disableWriteListener() {
        disabled = true;
      }

      public void enableReadListener() {
        disabled = false;
      }

      public void enableWriteListener() {
        disabled = false;
      }
    };
    var state = createState(memory1);
    createOOZ80(state);
    createScreenNoTest();
    phaseProcessor = new FusePhaseProcessor(this);

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

  private void setupExecutionFetcher() {
    ooz80.getInstructionExecutor().setExecutionListener(new PhaseProcessorExecutionListener(phaseProcessor));
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    instructionFetcher.tPhaseProcessor = phaseProcessor;
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

  private void createScreenNoTest() {
    //      JFrame screen1 = MiniZX.createScreen(io.miniZXKeyboard, EmulatedMiniZX.getMemFunction(ooz80));
//      JFrame screen = createScreen(io.miniZXKeyboard, zxScreenComponent);
//    audio = new Audio(new AY8912Type());
//    audio.open(MachineTypes.SPECTRUM48K, new AY8912(), false, 32000);
    SpeccyScreen contentPane = new SpeccyScreen(screenBytes);
    contentPane.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        contentPane.requestFocus();
      }
    });

//    contentPane.addKeyListener(new KeyAdapter() {
//      public void keyTyped(KeyEvent e) {
//        super.keyTyped(e);
//      }
//    });
    JFrame screen = createScreen(null, contentPane);
//      new SwingKeyboard(screen, keyboard, input);
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

  private void createOOZ80(State state1) {
    ooz80 = new OOZ80(state1, Helper.getInstructionFetcher(state1, new NullInstructionSpy(), new DefaultInstructionFactory(state1)), new DefaultInstructionExecutor(state1, false));
  }

  private State createState(Memory memory2) {
    var state1 = new State(io, new DefaultRegisterBankFactory().createBank(), memory2) {
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
    Class<?> wanted = switch (snapshot.getSpectrumModel()) {
      case SPECTRUM128K -> Spec128.class;
      case SPECTRUMPLUS2 -> SpecPlus2.class;
      case SPECTRUMPLUS2A -> SpecPlus2A.class;
      case SPECTRUMPLUS3 -> SpecPlus3.class;
      default -> Spec48.class;
    };
    if (machine.current != null && machine.current.getClass() == wanted) {
      announce(wanted);
      return;
    }
    machine.getMachineTypes().stream().filter(m -> m.getClass() == wanted).findFirst()
        .ifPresentOrElse(type -> {
          machine.selectDefault();
          machine.select(type);
          announce(wanted);
        }, () -> userInterface.error(UiError.ERROR,
            "this build has no %s, so the snapshot is loaded into the machine already running",
            wanted.getSimpleName()));
  }

  /**
   * Tells the emulator which machine it has become, so the window agrees with the machine.
   * <p>
   * Choosing a machine for a snapshot changed the machine and told nobody, so the indicator went
   * on naming whatever it had been started as - a 128K game running under a label saying 48K.
   * Said even when nothing changed, because the label may be left over from the game before.
   */
  private void announce(Class<?> machineClass) {
    if (mockCore == null) {
      return;
    }
    String name = machineClass == Spec128.class ? "Spectrum 128K"
        : machineClass == SpecPlus2.class ? "Spectrum Plus 2"
        : machineClass == SpecPlus2A.class ? "Spectrum Plus 2"
        : machineClass == SpecPlus3.class ? "Spectrum Plus 3"
        : "Spectrum 48K";
    if (!name.equals(mockCore.getCurrentModel())) {
      mockCore.setMachineModel(name);
    }
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
  }

  public void start() {
    z80_interrupt_event = eventManager.eventRegister(this::z80_interrupt_event_fn, "Retriggered interrupt");
    int z80_nmi_event = eventManager.eventRegister(this::z80_nmi, "Non-maskable interrupt");
    int z80_nmos_iff2_event = eventManager.eventRegister(null, "IFF2 update dummy event");

    module.register(new Z80ModuleInfo(this));

    if (OOSpectrumConnector.noTest)
      initNoTest();
    else
      initTest();

    return;
  }

  public void end() {
  }

  private void z80_nmi(long l, int i, Object o) {
    System.out.println("Z80 NMI");
  }

  private void z80_interrupt_event_fn(long l, int i, Object o) {
    interrupt();
  }

  public void doOpcodes() {
    while (zxClock.getTStates() < eventManager.eventNextEvent) {
      while (emulatorPaused) Thread.onSpinWait();
      bridgeCommand.invoke(0, null);
      try {
        ooz80.execute();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (changeMachine != null) {
      changeMachine.run();
      changeMachine = null;
    }
  }

  public void updateMemory() {
    Memory memory1 = ooz80.getState().getMemory();

//    int tStates = zxClock.getTStates();
    memory1.disableReadListener();

    for (int i = 0x4000; i < 0x8000; i++) {
      int datum = memory1.read(i, 0);
      memory.writeByteInternal(i, (byte) (datum & 0xff), display);
    }

//    zxClock.setTStates(tStates);
    // zxClock.addTStates(-zxClock.getTStates());
    memory1.enableReadListener();
  }

  public JFrame createScreen(KeyListener keyListener, JComponent contentPane) {
    mockCore = new MockEmulatorCore(contentPane) {
      private String filename;
      private boolean turbo = settings.current.emulationSpeed != 100;

      /**
       * Asked of the machine rather than remembered.
       * <p>
       * A copy of the answer has to be kept in step by everything that changes the machine, and
       * it was not: choosing a machine for a snapshot updated it, choosing one for a 128K tape
       * did not, and the indicator went on naming whatever the emulator had started as. There is
       * no keeping a copy honest; there is only not keeping one.
       */
      @Override
      public String getCurrentModel() {
        return machine.current == null ? super.getCurrentModel() : machine.current.getName();
      }


      public void applyMod(PokFile.PokeMod mod) {
        PokInstruction parsedInstruction = mod.getParsedInstruction();
        parsedInstruction.apply(new PokInstruction.EmulatorMemoryWriter() {
          public void writeMemory(int bank, int address, int value) {
            ooz80.getState().getMemory().write(address, value);
          }

          public int readMemory(int bank, int address) {
            return ooz80.getState().getMemory().read(address);
          }
        });
      }

      public void revertMod(PokFile.PokeMod mod) {
        PokInstruction parsedInstruction = mod.getParsedInstruction();
        parsedInstruction.revert(new PokInstruction.EmulatorMemoryWriter() {
          public void writeMemory(int bank, int address, int value) {
            ooz80.getState().getMemory().write(address, value);
          }

          public int readMemory(int bank, int address) {
            return ooz80.getState().getMemory().read(address);
          }
        });
      }

      public void setFilename(String filename) {
        this.filename = filename;
      }

      public String getFilename() {
        return filename;
      }

      public KeyListener getKeyListener() {
        return new SwingKeyboard(keyboard, input, userInterface);
      }

      public void finishEmulation() {
        session.finish();
      }

      public boolean isPaused() {
        return emulatorPaused;
      }

      public void pauseEmulation() {
        emulatorPaused = !emulatorPaused;
        notifyPauseStateChange(emulatorPaused);
      }

      @Override
      public void resumeEmulation() {
        emulatorPaused = false;
        notifyPauseStateChange(emulatorPaused);
      }

      public void setGeneralOption(String option, Object value) {
        if (option.equals("turbo")) {
          turbo = (boolean) value;
          int emulationSpeed = turbo ? 15000 : 100;
          changeSpeed1(emulationSpeed);
//          timer.estimateReset();
        } else if (option.equals("mute")) {
          sound.soundEnabled = !(boolean) value;
//          timer.estimateReset();
        } else if (option.equals("pause")) {
          emulatorPaused = (boolean) value;
          notifyPauseStateChange(emulatorPaused);
        } else {
          // Anything this one has no opinion on goes to the core it overrides, which is where
          // the options that are about the panel rather than the machine are answered. Without
          // this the override swallowed them: an option nobody handled and nobody reported.
          super.setGeneralOption(option, value);
        }
      }

      public void changeSpeed1(int emulationSpeed) {
        changeSpeed(emulationSpeed);
        notifyTurboModeChange(turbo);
        notifyEmulationSpeedChange(Z80.emulationSpeed);
      }

      @Override
      public double getEmulationSpeed() {
        return settings.current.emulationSpeed;
      }

      @Override
      public boolean isTurboMode() {
        return turbo;
      }

      public boolean isMuted() {
        return !sound.soundEnabled;
      }

      public State getState() {
        return ooz80.getState();
      }

      public RegistersGetter getRegistersGetter() {
        return new RegistersBase(ooz80.getState());
      }
    };
    mockCore.addEmulatorListener(new EmulatorListener() {
      @Override
      public void onEmulationStateChanged(String state) {
        SwingUtilities.invokeLater(() -> {
          if (state.equals("Reset")) {
            tape.insert(new File("/tmp/zxinfo_extracted/SOLARINV.TAP"));
            tape.play(true);
          }
        });
      }

      @Override
      public void onError(String message) {

      }

      @Override
      public void onEmulationSpeedChanged(double speed) {

      }

      @Override
      public void onModelChanged(String model) {
        changeMachine = () -> {
          machine.getMachineTypes().stream().filter(m -> m.getName().equals(model)).forEach(type -> {
            machine.selectDefault();
            machine.select(type);
          });
        };
      }

      @Override
      public void onPauseStateChanged(boolean paused) {

      }

      @Override
      public void onTurboModeChanged(boolean turbo) {

      }

      @Override
      public void onTapeStatusChanged(String status) {

      }
    });

    // Create first emulator
//    zxSpectrumDesktopApp.createNewEmulator(mockCore);

//    ZXSpectrumEmulatorUI ui = new ZXSpectrumEmulatorUI(mockCore);
//    ui.setVisible(true);
//    ui.addKeyListener(keyListener);

//    JFrame frame = new JFrame("Speccy ZX Spectrum");
//    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//    frame.setContentPane(contentPane);
//    frame.setLocationRelativeTo(null);
//    frame.pack();
//    frame.setVisible(true);
//    frame.addKeyListener(keyListener);

    return null;
  }

  public void changeSpeed(int emulationSpeed) {
    settings.current.emulationSpeed = emulationSpeed;
    zxClock.rebaseTStates(60000);
    timer.changeSpeed(emulationSpeed);
    sound.end();
    sound.init(false);
  }

}
