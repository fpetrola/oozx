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

package com.fpetrola.oozx.fuse.modules.z80;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.*;
import com.fpetrola.oozx.fuse.machine.TimingsHandler;
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.oozx.fuse.modules.Timer;
import com.fpetrola.oozx.fuse.modules.tape.Tape;
import com.fpetrola.oozx.fuse.peripherals.*;
import com.fpetrola.oozx.fuse.pokes.PokFile;
import com.fpetrola.oozx.fuse.pokes.PokInstruction;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.AbstractMemory;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.NullInstructionSpy;
import fuse.PhaseProcessorExecutionListener;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;
import fuse.tstates.phases.AfterMR;
import fuse.tstates.phases.BeforeWrite;
import snapshots.SpectrumState;

import javax.swing.*;
import java.awt.event.*;
import java.io.File;

import static com.fpetrola.z80.registers.RegisterName.*;

public class Z80 implements ZxModule {
  public static double emulationSpeed;
  private final EventManager eventManager;
  public final com.fpetrola.oozx.Memory memory;

  public long interruptsEnabledAt;
  public OOZ80 ooz80;
  public LibretroCore.bridge_command bridgeCommand;
  private PhaseProcessor phaseProcessor;

  private IO io;
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
  private final com.fpetrola.oozx.fuse.modules.Timer timer;
  public EmulatorCore mockCore;
  private Runnable changeMachine;
  private final Module module;
  private final Fuse fuse;
  private final Settings settings;
  private final Tape tape;
  private final byte[][] screenBytes = new byte[1000][1000];
  private Memory memory1;

  public Z80(EventManager eventManager, com.fpetrola.oozx.Memory memory, Display display, Ula ula, Machine machine, Keyboard keyboard, SpectrumZ80Clock zxClock, Input input, IPeriph periph, UiDisplay uiDisplay, Timer timer, Module module, Fuse fuse, Settings settings, Tape tape) {
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
    this.fuse = fuse;
    this.settings = settings;
    this.tape = tape;
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

  public void interrupt() {
    int i = TimingsHandler.interruptLength(machine.current.getBaseTiming());
    if (ooz80.getState().isIff1() && zxClock.getTStates() < i) {
      zxClock.addTStates(7, "interrupt");
      ooz80.interruption();
    }
  }

  private void initNoTest() {
    createIO();

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
    createIO();
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
    FuseScreen contentPane = new FuseScreen(screenBytes);
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

  private void createIO() {
    io = new IO() {
      public int in(int port) {
        return periph.readPort(port);
      }

      public void out(int port, int value) {
        periph.writePort(port, (byte) value);
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
    State state = ooz80.getState();

    RegistersBase registersBase = new RegistersBase(ooz80.getState());

    String first = url; //com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
    byte[] bytes = SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);
    if (bytes != null) {
      int tstates = Z80Loader.getTstates(LibSpectrum.INSTANCE, url);
      state.clock.setTStates(tstates);
      interruptsEnabledAt = -1;

      updateMemory();
      display.refreshAll();
    }
  }

  public void loadSnap(SpectrumState spectrumState) {
    State state = ooz80.getState();
    RegistersBase registersBase = new RegistersBase(ooz80.getState());
    SnapshotLoader.setupFromSpectrumState(registersBase, state, spectrumState);
    interruptsEnabledAt = -1;
    updateMemory();
    display.refreshAll();
  }

  public int init(Object o) {
    z80_interrupt_event = eventManager.eventRegister(this::z80_interrupt_event_fn, "Retriggered interrupt");
    int z80_nmi_event = eventManager.eventRegister(this::z80_nmi, "Non-maskable interrupt");
    int z80_nmos_iff2_event = eventManager.eventRegister(null, "IFF2 update dummy event");

    module.register(new Z80ModuleInfo(this));

    if (OOSpectrumConnector.noTest)
      initNoTest();
    else
      initTest();

    return 0;
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

      public void setFilename(String filename) {
        this.filename = filename;
      }

      public String getFilename() {
        return filename;
      }

      public KeyListener getKeyListener() {
        return new SwingKeyboard(fuse.keyboard, fuse.input);
      }

      public void finishEmulation() {
        fuse.alive = false;
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
          fuse.sound.soundEnabled = !(boolean) value;
//          timer.estimateReset();
        } else if (option.equals("pause")) {
          emulatorPaused = (boolean) value;
          notifyPauseStateChange(emulatorPaused);
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
        return !fuse.sound.soundEnabled;
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
            machine.select(fuse.spec48);
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

//    JFrame frame = new JFrame("Fuse ZX Spectrum");
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
    zxClock.addTStates(-zxClock.getTStates() + 60000);
    timer.changeSpeed(emulationSpeed);
    fuse.sound.end();
    fuse.sound.init(false);
  }

}
