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

package com.fpetrola.oozx;

import com.fpetrola.oozx.fuse.*;
import com.fpetrola.oozx.fuse.bridge.GetTStatesHistory;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.modules.Keyboard;
import com.fpetrola.oozx.fuse.peripherals.*;
import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.instructions.factory.DefaultInstructionFactory;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.emulation.Helper;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.DefaultRegisterBankFactory;
import com.fpetrola.z80.spy.NullInstructionSpy;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;
import fuse.tstates.phases.BeforeWrite;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.util.function.Supplier;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class Z80 {
  private EventManager eventManager;
  private com.fpetrola.oozx.Memory memory;

  public long interruptsEnabledAt;
  public OOZ80<WordNumber> ooz80;
  public LibretroCore.bridge_command bridgeCommand;
  private PhaseProcessor<WordNumber> phaseProcessor;

  private MiniZXIO io;
  private boolean initialized;
  private int z80_interrupt_event;
  //   ZXScreenComponent<WordNumber> zxScreenComponent = new ZXScreenComponent<>();
//   MemoryWriteListener<WordNumber> writeListener = zxScreenComponent.getWriteListener();
  private boolean init;
  public Audio audio;
  private Display display;
  private Ula ula;
  private Supplier<FuseMachineInfo> machine;
  private Keyboard keyboard;
  private TStatesHolder tStatesHolder;
  private Input input;
  private Periph periph;
  private UiDisplay uiDisplay;

  public Z80(EventManager eventManager, com.fpetrola.oozx.Memory memory, Display display, Ula ula, Supplier<FuseMachineInfo> machine, Keyboard keyboard, TStatesHolder tStatesHolder, Input input, Periph periph, UiDisplay uiDisplay) {
    this.eventManager = eventManager;
    this.memory = memory;
    this.display = display;
    this.ula = ula;
    this.machine = machine;
    this.keyboard = keyboard;
    this.tStatesHolder = tStatesHolder;
    this.input = input;
    this.periph = periph;
    this.uiDisplay = uiDisplay;
  }

  public void reset(int i) {
    ooz80.reset();
    String url = "file:///home/fernando/dynamitedan1.z80";
    url = "/home/fernando/detodo/desarrollo/m/zx/roms/aqua.z80";
//    loadSnap(url);
  }

  public void toSnapshot(Libspectrum.Snap snap) {

  }

  public void fromSnapshot(Libspectrum.Snap snap) {

  }

  public void interrupt() {
    ooz80.getState().tstates = tStatesHolder.getTstates();
    int i = Timings.interruptLength(machine.get().machine);
    if (ooz80.getState().isIff1() && ooz80.getState().tstates < i) {
//      if (ooz80.getState().tstates == Z80.interruptsEnabledAt) {
//        EventManager.eventAdd(ooz80.getState().tstates + 1, z80_interrupt_event);
//        return;
//      }

      GetTStatesHistory.addTStateUpdate((byte) 7, "interrupt", (int) ooz80.getState().tstates);
      ooz80.getState().tstates += 7;
      ooz80.interruption();
    }
//    ooz80.getState().tstates = 0;
    tStatesHolder.setTstates(ooz80.getState().tstates);
  }

  //  private  void reg1() {
//    StartupManagerModule[] dependencies = {
//        StartupManagerModule.DEBUGGER,
//        StartupManagerModule.EVENT,
//        StartupManagerModule.SETUID
//    };
//    StartupManager.register(StartupManagerModule.Z80, dependencies, Z80::init, null, null);

  ////    Machine.reset(false);
//  }
  public <T extends WordNumber> OOZ80<T> createOOZ80(MiniZXIO io) {
    var state = new State(io, new DefaultRegisterBankFactory().createBank(), new MockedMemory(true)) {
      public void enableInterrupt() {
        super.enableInterrupt();
        interruptsEnabledAt = tstates;
        eventManager.eventAdd(tstates + 1, z80_interrupt_event);
      }
    };
    io.setPc(state.getPc());
    return new OOZ80(state, Helper.getInstructionFetcher(state, new NullInstructionSpy(), new DefaultInstructionFactory<T>(state)));
  }


  private void init2() {
    io = new MiniZXIO() {
      public synchronized WordNumber in(WordNumber port) {
//        short invoke = LocalLibretroCore.retroInputStateT.invoke(port.intValue(), 0, 0, 0);
        tStatesHolder.setTstates(ooz80.getState().tstates);
        byte b = periph.readPort(port.intValue());
        ooz80.getState().tstates = tStatesHolder.getTstates();
        return createValue(b);
      }

      public void out(WordNumber port, WordNumber value) {
        tStatesHolder.setTstates(ooz80.getState().tstates);
        periph.writePort(port.intValue(), (byte) value.intValue());
        ooz80.getState().tstates = tStatesHolder.getTstates();
      }
    };
    ooz80 = createOOZ80(io);

    byte[][] bytes = new byte[1000][1000];
    if (FuseLibretroConnector.noTest) {
//      JFrame screen1 = MiniZX.createScreen(io.miniZXKeyboard, EmulatedMiniZX.getMemFunction(ooz80));
//      JFrame screen = createScreen(io.miniZXKeyboard, zxScreenComponent);

      updateScreen2();

      audio = new Audio(new AY8912Type());
      audio.open(MachineTypes.SPECTRUM48K, new AY8912(), false, 32000);

      JFrame screen = createScreen(io.miniZXKeyboard, new FuseScreen(bytes));
      new SwingKeyboard(screen, keyboard, input);
    }
    uiDisplay.screenMatrix = bytes;
//    Keyboard0.keyboard = io.miniZXKeyboard;

    setupMemory();
  }

  private void updateScreen2() {
//    for (int i = 0x4000; i <= 0x5FFF; i++) {
//      WordNumber datum = ooz80.getState().getMemory().getData()[i];
//      writeListener.writtingMemoryAt(createValue(i), createValue(datum != null ? datum.intValue() : 0));
//    }
  }

  public void loadSnap(String url) {
    State<?> state = ooz80.getState();

    RegistersBase registersBase = new RegistersBase<>(ooz80.getState());

    String first = url; //com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);
    Z80Loader.LibSpectrum lib = Z80Loader.LibSpectrum.INSTANCE;
    Z80Loader.libspectrum_snap snap = Z80Loader.getLibspectrumSnap(lib, url);
    state.tstates = lib.libspectrum_snap_tstates(snap);
    tStatesHolder.setTstates(state.tstates);
    interruptsEnabledAt = -1;

    updateMemory();
//    updateScreen();
//    updateScreen2();
    display.refreshAll();

    IO<?> io1 = state.getIo();
//    ZXScreenComponent<WordNumber> zxScreenComponent = new ZXScreenComponent<>();
//    MemoryWriteListener<WordNumber> writeListener = zxScreenComponent.getWriteListener();
//    memory.addMemoryWriteListener(writeListener);
//    createScreen(io.miniZXKeyboard, zxScreenComponent);
//    for (int address = 0x4000; address <= 0x8000; address++) {
//      writeListener.writtingMemoryAt(createValue(address), memory.read(createValue(address), 0));
//    }
  }

  private void setupMemory() {
    State<?> state = ooz80.getState();

    Memory<WordNumber> memory1 = (Memory<WordNumber>) state.getMemory();

    phaseProcessor = new PhaseProcessor<>(ooz80) {
      public void addMultipleMc(int x, int time1, int delta, int baseAddress, String description) {
        for (int i = 0; i < x; i++) {
          MemoryPage memoryPage = memory.mapRead[baseAddress >>> memory.PAGE_SIZE_LOGARITHM];
          if (memoryPage != null && memoryPage.contended) {
            if (state.tstates < ula.contentionNoMreq.length) {
              byte tstates = ula.contentionNoMreq[(int) state.tstates];
              if (tstates > 0) {
                GetTStatesHistory.addTStateUpdate(tstates, "ula " + (description != null ? description : "contend_read_no_mreq"), (int) getState().tstates);
                state.tstates += tstates;
              }
            }
          }
          getAddEvent(new Event(time1, "MC", baseAddress + delta, null, description));
        }
        //        tStatesHolder.tstates= state.tstates;
      }

      @Override
      protected void getAddEvent(Event event) {
        if (event.getTime() > 0) {
          String description = getDescription(event);
          GetTStatesHistory.addTStateUpdate((byte) event.getTime(), description, (int) getState().tstates);
        }
        getState().addEvent(event);
      }

      private String getDescription(Event event) {
        if (event.description != null)
          return event.description;

        return switch (event.getType()) {
          case "MR" -> "contend_read";
          case "MW" -> "contend_write";
          case "MC" -> processing ? "contend_read_no_mreq" : "contend_read";
          default -> "unknown";
        };
      }
    };

    memory1.addMemoryReadListener(new AddStatesMemoryReadListener<>(phaseProcessor) {
      protected void processEvent(WordNumber address, WordNumber value, int fetching) {
        if (LocalLibretroCore.noContended || !initialized())
          return;

        processUlaContention(address);
        super.processEvent(address, value, fetching);
        tStatesHolder.setTstates(state.tstates);
      }

      private void processUlaContention(WordNumber address) {
        tStatesHolder.setTstates(state.tstates);
        memory.readByte(address.intValue(), ula);
        state.tstates = tStatesHolder.getTstates();
      }

      protected void addMc(WordNumber address, int time1) {
        super.addMc(address, time1);
      }
    });
    memory1.addMemoryWriteListener(new AddStatesMemoryWriteListener<>(phaseProcessor) {
      public void writtingMemoryAt(WordNumber address, WordNumber value) {
        if (LocalLibretroCore.noContended || !initialized())
          return;

        if (!ooz80.getState().isIntLine()) {
          this.phaseProcessor.processPhase(new BeforeWrite());
          processUlaContention(address, value);
        }

        this.phaseProcessor.addMultipleMc(1, 3, 0, address.intValue(), "writebyte");
        this.phaseProcessor.addMw(address, value);

        int address1 = address.intValue();
        if (true || address1 >= 0x4000 && address1 < 0x5B00) {
          tStatesHolder.setTstates(state.tstates);
          memory.writeByteInternal(address1, (byte) (value.intValue() & 0xff), Fuse.display);
        }
      }

      private void processUlaContention(WordNumber address, WordNumber value) {
        tStatesHolder.setTstates(state.tstates);
        memory.writeByte(address.intValue(), (byte) (value.intValue() & 0xff), ula);
        ooz80.getState().getMemory().getData()[address.intValue()] = value;
        state.tstates = tStatesHolder.getTstates();
      }
    });
  }

  private boolean initialized() {
    return memory.mapRead[0] != null && ula.contention != null;
  }

  public int init(Object o) {
    z80_interrupt_event = eventManager.eventRegister(this::z80_interrupt_event_fn, "Retriggered interrupt");
    int z80_nmi_event = eventManager.eventRegister(this::z80_nmi, "Non-maskable interrupt");
    int z80_nmos_iff2_event = eventManager.eventRegister(null, "IFF2 update dummy event");

    Module.register(new Z80ModuleInfo(this));

//    z80_debugger_variables_init();

    init2();

    initialized = true;

    return 0;
  }

  private void z80_nmi(long l, int i, Object o) {

  }

  private void z80_interrupt_event_fn(long l, int i, Object o) {
    interrupt();
  }

  public void doOpcodes() {
    ooz80.getState().tstates = tStatesHolder.getTstates();
    while (tStatesHolder.getTstates() < eventManager.eventNextEvent) {
      bridgeCommand.invoke(0, null);
      ooz80.getState().tstates = tStatesHolder.getTstates();
      ooz80.getState().tstates2 = tStatesHolder.getTstates();
//      System.out.printf("Event processed, tstates: %d\n", tStatesHolder.tstates);
      phaseProcessor.initialTStates = tStatesHolder.getTstates();
      ooz80.execute();
      tStatesHolder.setTstates(ooz80.getState().tstates);
    }
  }

  public void updateMemory() {
    WordNumber[] data = ooz80.getState().getMemory().getData();
    for (int i = 0x4000; i < 0x8000; i++) {
      WordNumber datum = data[i];
//      int bank = i >>> PAGE_SIZE_LOGARITHM;
//      byte[] mapping = tStatesHolder.RAM[currentScreen];
//      mapping[i - 0x4000] = datum != null ? (byte) (datum.intValue() & 0xff) : 0;
      memory.writeByteInternal(i, datum != null ? (byte) (datum.intValue() & 0xff) : 0, Fuse.display);
    }
  }

  public JFrame createScreen(KeyListener keyListener, JComponent contentPane) {
    EmulatorCore mockCore = new MockEmulatorCore(contentPane);
    ZXSpectrumEmulatorUI ui = new ZXSpectrumEmulatorUI(mockCore);
    ui.setVisible(true);
    ui.addKeyListener(keyListener);

//    JFrame frame = new JFrame("Fuse ZX Spectrum");
//    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//    frame.setContentPane(contentPane);
//    frame.setLocationRelativeTo(null);
//    frame.pack();
//    frame.setVisible(true);
//    frame.addKeyListener(keyListener);

    return ui;
  }
}
