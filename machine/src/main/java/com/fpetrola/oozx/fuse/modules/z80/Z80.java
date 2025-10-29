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

package com.fpetrola.oozx.fuse.modules.z80;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.*;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.machine.TimingsHandler;
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.oozx.fuse.modules.Timer;
import com.fpetrola.oozx.fuse.peripherals.*;
import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.emulation.AbstractMemory;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.NullInstructionSpy;
import fuse.PhaseProcessorExecutionListener;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.function.Supplier;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.*;

public class Z80 implements ZxModule {
  public static double emulationSpeed;
  private EventManager eventManager;
  public com.fpetrola.oozx.Memory memory;

  public long interruptsEnabledAt;
  public OOZ80<WordNumber> ooz80;
  public LibretroCore.bridge_command bridgeCommand;
  private PhaseProcessor<WordNumber> phaseProcessor;

  private MiniZXIO io;
  private int z80_interrupt_event;
  //   ZXScreenComponent<WordNumber> zxScreenComponent = new ZXScreenComponent<>();
//   MemoryWriteListener<WordNumber> writeListener = zxScreenComponent.getWriteListener();
  private boolean init;
  public Audio audio;
  private Display display;
  public Ula ula;
  private Supplier<SpectrumMachine> machineSupplier;
  private Keyboard keyboard;
  public SpectrumZ80Clock zxClock;
  private Input input;
  private IPeriph periph;
  private UiDisplay uiDisplay;
  private volatile boolean emulatorPaused;
  private com.fpetrola.oozx.fuse.modules.Timer timer;
  public EmulatorCore mockCore;
  private Supplier<Machine> machine;
  private Runnable changeMachine;
  private Module module;
  private Fuse fuse;
  private Settings settings;

  public Z80(EventManager eventManager, com.fpetrola.oozx.Memory memory, Display display, Ula ula, Supplier<SpectrumMachine> machineSupplier, Keyboard keyboard, SpectrumZ80Clock zxClock, Input input, IPeriph periph, UiDisplay uiDisplay, Timer timer, Supplier<Machine> machine, Module module, Fuse fuse, Settings settings) {
    this.eventManager = eventManager;
    this.memory = memory;
    this.display = display;
    this.ula = ula;
    this.machineSupplier = machineSupplier;
    this.keyboard = keyboard;
    this.zxClock = zxClock;
    this.input = input;
    this.periph = periph;
    this.uiDisplay = uiDisplay;
    this.timer = timer;
    this.machine = machine;
    this.module = module;
    this.fuse = fuse;
    this.settings = settings;
  }

  public void reset(int hardReset) {
    ooz80.reset();

    State<WordNumber> state = ooz80.getState();

    state.getRegister(AF).write(createValue(0xffff));
    state.getRegister(AFx).write(createValue(0xffff));
    state.getRegister(BC).write(createValue(0));
    state.getRegister(DE).write(createValue(0));
    state.getRegister(HLx).write(createValue(0));
    state.getRegister(BC).write(createValue(0));
    state.getRegister(DEx).write(createValue(0));
    state.getRegister(HLx).write(createValue(0));
    state.getRegister(IX).write(createValue(0));
    state.getRegister(IY).write(createValue(0));
    state.getRegister(PC).write(createValue(0));
    state.getRegister(SP).write(createValue(0xffff));


    state.getRegister(I).write(createValue(0));
    state.getRegister(R).write(createValue(0));

    interruptsEnabledAt = -1;
  }

  public void toSnapshot(Libspectrum.Snap snap) {

  }

  public void fromSnapshot(Libspectrum.Snap snap) {

  }

  public void interrupt() {
    int i = TimingsHandler.interruptLength(machineSupplier.get().getBaseTiming());
    if (ooz80.getState().isIff1() && zxClock.getTStates() < i) {
      zxClock.addTStates(7, "interrupt");
      ooz80.interruption();
    }
  }

  public <T extends WordNumber> OOZ80<T> createOOZ80(MiniZXIO io) {
    Memory<T> memory1 = new AbstractMemory<T>() {
      protected T doRead(T address) {
        byte b = memory.readByteInternal(address.intValue());
        return createValue(b & 0xff);
      }

      protected void doWrite(int address, T value) {
        memory.writeByteInternal2(address, (byte) value.intValue());
      }

      public void reset() {
        memory.reset();
      }
    };
    var state = new State<T>(io, new DefaultRegisterBankFactory<T>().createBank(), memory1) {
      public void enableInterrupt() {
        super.enableInterrupt();
        interruptsEnabledAt = clock.getTStates();
        eventManager.eventAdd(clock.getTStates() + 1, z80_interrupt_event);
      }
    };

    state.clock = zxClock;
    io.setPc(state.getPc());
    return new OOZ80(state, Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory<T>(state)), new DefaultInstructionExecutor(state, false));
  }


  private void init2() {
    io = new MiniZXIO() {
      public synchronized WordNumber in(WordNumber port) {
        return createValue(periph.readPort(port.intValue()));
      }

      public void out(WordNumber port, WordNumber value) {
        periph.writePort(port.intValue(), (byte) value.intValue());
      }
    };
    ooz80 = createOOZ80(io);
    zxClock.setPc(ooz80.getState().getPc());

    byte[][] bytes = new byte[1000][1000];
    if (OOSpectrumConnector.noTest) {
//      JFrame screen1 = MiniZX.createScreen(io.miniZXKeyboard, EmulatedMiniZX.getMemFunction(ooz80));
//      JFrame screen = createScreen(io.miniZXKeyboard, zxScreenComponent);

      audio = new Audio(new AY8912Type());
      audio.open(MachineTypes.SPECTRUM48K, new AY8912(), false, 32000);

      FuseScreen contentPane = new FuseScreen(bytes);
      JFrame screen = createScreen(io.miniZXKeyboard, contentPane);
//      new SwingKeyboard(screen, keyboard, input);
    }
    uiDisplay.screenMatrix = bytes;
//    Keyboard0.keyboard = io.miniZXKeyboard;

    setupMemory();
  }

  public void loadSnap(String url) {
    State<?> state = ooz80.getState();

    RegistersBase registersBase = new RegistersBase<>(ooz80.getState());

    String first = url; //com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);
    Z80Loader.LibSpectrum lib = Z80Loader.LibSpectrum.INSTANCE;
    Z80Loader.libspectrum_snap snap = Z80Loader.getLibspectrumSnap(lib, url);
    state.clock.setTStates(lib.libspectrum_snap_tstates(snap));
    interruptsEnabledAt = -1;

    updateMemory();
    display.refreshAll();
  }

  private void setupMemory() {
    State<?> state = ooz80.getState();

    Memory<WordNumber> memory1 = (Memory<WordNumber>) state.getMemory();

    phaseProcessor = new FusePhaseProcessor(this);
    DefaultInstructionFetcher<WordNumber> instructionFetcher = (DefaultInstructionFetcher<WordNumber>) ooz80.getInstructionFetcher();
    instructionFetcher.tPhaseProcessor = phaseProcessor;

    ooz80.getInstructionExecutor().addExecutionListener(new PhaseProcessorExecutionListener<>(phaseProcessor));

    memory1.addMemoryReadListener(new AddStatesMemoryReadListener<>(phaseProcessor) {
      protected void doRead(WordNumber address, WordNumber value, int fetching) {
        memory.readByte(address.intValue(), ula);
      }
    });
    memory1.addMemoryWriteListener(new AddStatesMemoryWriteListener<>(phaseProcessor) {
      protected void doWrite(WordNumber address, WordNumber value) {
        memory.writeByte(address.intValue(), (byte) (value.intValue() & 0xff), ula);
      }

      protected void doEnd(WordNumber address, WordNumber value) {
        memory.writeByteInternal(address.intValue(), (byte) (value.intValue() & 0xff), display);
      }
    });
  }

  public int init(Object o) {
    z80_interrupt_event = eventManager.eventRegister(this::z80_interrupt_event_fn, "Retriggered interrupt");
    int z80_nmi_event = eventManager.eventRegister(this::z80_nmi, "Non-maskable interrupt");
    int z80_nmos_iff2_event = eventManager.eventRegister(null, "IFF2 update dummy event");

    module.register(new Z80ModuleInfo(this));

    init2();

    return 0;
  }

  @Override
  public void end() {

  }

  private void z80_nmi(long l, int i, Object o) {

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
    Memory<WordNumber> memory1 = ooz80.getState().getMemory();

    memory1.canDisable(true);
    memory1.disableReadListener();

    for (int i = 0x4000; i < 0x8000; i++) {
      WordNumber datum = memory1.read(createValue(i), 0);
      memory.writeByteInternal(i, datum != null ? (byte) (datum.intValue() & 0xff) : 0, display);
    }

    memory1.enableReadListener();
    memory1.canDisable(false);
  }

  public JFrame createScreen(KeyListener keyListener, JComponent contentPane) {
    mockCore = new MockEmulatorCore(contentPane) {
      private boolean turbo;

      public KeyListener getKeyListener() {
        return new SwingKeyboard(fuse.keyboard, fuse.input);
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
          turbo = !turbo;
          int emulationSpeed = (boolean) value ? 15000 : 100;
          settings.current.emulationSpeed = emulationSpeed;
          timer.addEvent();
          notifyTurboModeChange(turbo);
          notifyEmulationSpeedChange(Z80.emulationSpeed);
//          timer.estimateReset();
        }
      }

      @Override
      public double getEmulationSpeed() {
        return settings.current.emulationSpeed;
      }

      @Override
      public boolean isTurboMode() {
        return turbo;
      }
    };
    mockCore.addEmulatorListener(new EmulatorListener() {
      @Override
      public void onEmulationStateChanged(String state) {

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
          Machine machine1 = machine.get();
          List<SpectrumMachine> machineTypes = machine1.getMachineTypes();

          machineTypes.stream().filter(m -> m.getName().equals(model)).forEach(m -> {
            machine1.select(m);
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

}
