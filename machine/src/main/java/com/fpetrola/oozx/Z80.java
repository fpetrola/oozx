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
import com.fpetrola.z80.cpu.Event;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.opcodes.references.WordNumber;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;
import fuse.tstates.phases.BeforeWrite;

import javax.swing.*;
import java.awt.event.KeyListener;

import static com.fpetrola.oozx.Memory.PAGE_SIZE_LOGARITHM;
import static com.fpetrola.oozx.Memory.mapRead;
import static com.fpetrola.z80.opcodes.references.WordNumber.*;

public class Z80 {
  public static long interruptsEnabledAt;
  public static OOZ80<WordNumber> ooz80;
  public static LibretroCore.bridge_command bridgeCommand;
  private static PhaseProcessor<WordNumber> phaseProcessor;

  private static final ModuleInfo z80ModuleInfo = new ModuleInfo(
      Z80::reset, // reset
      null, // romcs
      null, // snapshotEnabled
      Z80::fromSnapshot, // snapshotFrom
      Z80::toSnapshot // snapshotTo
  );

  private static void reset(int i) {
    ooz80.reset();
  }

  private static void toSnapshot(Libspectrum.Snap snap) {

  }

  private static void fromSnapshot(Libspectrum.Snap snap) {

  }

  public static void interrupt() {
    ooz80.getState().tstates = Spectrum.tstates;
    ooz80.interruption();
    ooz80.getState().tstates = 0;
    Spectrum.tstates = ooz80.getState().tstates;
  }

  public static void registerStartup() {
    StartupManagerModule[] dependencies = {
        StartupManagerModule.DEBUGGER,
        StartupManagerModule.EVENT,
        StartupManagerModule.SETUID
    };
    StartupManager.register(StartupManagerModule.Z80, dependencies, Z80::init, null, null);

//    Machine.reset(false);

    MiniZXIO io = new MiniZXIO() {
      public synchronized WordNumber in(WordNumber port) {
        byte b = Periph.readPort(port.intValue());
        return createValue(b);
      }

      public void out(WordNumber port, WordNumber value) {
        Periph.writePortInternal(port.intValue(), (byte) value.intValue());
      }
    };
    ooz80 = EmulatedMiniZX.createOOZ80(io);

//    MiniZX.createScreen(io.miniZXKeyboard, EmulatedMiniZX.getMemFunction(ooz80));
    byte[][] bytes = new byte[1000][1000];
    createScreen(io.miniZXKeyboard, new FuseScreen(EmulatedMiniZX.getMemFunction(ooz80), bytes));
    UiDisplay.screenMatrix = bytes;

    Keyboard.keyboard = io.miniZXKeyboard;

    RegistersBase registersBase = new RegistersBase<>(ooz80.getState());

    String url = "file:///home/fernando/dynamitedan1.z80";
    url = "/home/fernando/detodo/desarrollo/m/zx/roms/aqua.z80";
    String first = url; //com.fpetrola.z80.helpers.Helper.getSnapshotFile(url);
    State<?> state = ooz80.getState();
    Memory<WordNumber> memory = (Memory<WordNumber>) state.getMemory();

    phaseProcessor = new PhaseProcessor<>(ooz80) {
      public void addMultipleMc(int x, int time1, int delta, int baseAddress, String description) {
        for (int i = 0; i < x; i++) {
          if (mapRead[baseAddress >> PAGE_SIZE_LOGARITHM].contended) {
            byte tstates = Ula.contentionNoMreq[(int) state.tstates];
            if (tstates > 0) {
              LocalLibretroCore.getTstatesUpdates().add(new TStateUpdate((int) getState().tstates, tstates, "ula " + (description != null ? description : "contend_read_no_mreq")));
              state.tstates += tstates;
            }
          }
          getAddEvent(new Event(time1, "MC", baseAddress + delta, null, description));
        }
        //        Spectrum.tstates= state.tstates;
      }

      @Override
      protected void getAddEvent(Event event) {
        if (event.getTime() > 0) {
          String description = getDescription(event);
          LocalLibretroCore.getTstatesUpdates().add(new TStateUpdate((int) getState().tstates, event.getTime(), description));
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

    memory.addMemoryReadListener(new AddStatesMemoryReadListener<>(phaseProcessor) {
      protected void processEvent(WordNumber address, WordNumber value, int fetching) {
        if (LocalLibretroCore.noContended)
          return;

        processUlaContention(address);
        super.processEvent(address, value, fetching);
        Spectrum.tstates = state.tstates;
      }

      private void processUlaContention(WordNumber address) {
        Spectrum.tstates = state.tstates;
        com.fpetrola.oozx.Memory.readByte(address.intValue());
        state.tstates = Spectrum.tstates;
      }

      protected void addMc(WordNumber address, int time1) {
        super.addMc(address, time1);
      }
    });
    memory.addMemoryWriteListener(new AddStatesMemoryWriteListener<>(phaseProcessor) {
      public void writtingMemoryAt(WordNumber address, WordNumber value) {
        if (LocalLibretroCore.noContended)
          return;

        this.phaseProcessor.processPhase(new BeforeWrite());
        processUlaContention(address, value);
        this.phaseProcessor.addMultipleMc(1, 3, 0, address.intValue(), "writebyte");
        this.phaseProcessor.addMw(address, value);

        Spectrum.tstates = state.tstates;
      }

      private void processUlaContention(WordNumber address, WordNumber value) {
        Spectrum.tstates = state.tstates;
        com.fpetrola.oozx.Memory.writeByte(address.intValue(), (byte) value.intValue());
        state.tstates = Spectrum.tstates;
      }
    });
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);

    Z80Loader.LibSpectrum lib = Z80Loader.LibSpectrum.INSTANCE;
    Z80Loader.libspectrum_snap snap = Z80Loader.getLibspectrumSnap(lib, url);

    state.tstates = lib.libspectrum_snap_tstates(snap);
    updateScreen();

    IO<?> io1 = state.getIo();
//    ZXScreenComponent<WordNumber> zxScreenComponent = new ZXScreenComponent<>();
//    MemoryWriteListener<WordNumber> writeListener = zxScreenComponent.getWriteListener();
//    memory.addMemoryWriteListener(writeListener);
//    createScreen(io.miniZXKeyboard, zxScreenComponent);
//    for (int address = 0x4000; address <= 0x8000; address++) {
//      writeListener.writtingMemoryAt(createValue(address), memory.read(createValue(address), 0));
//    }
  }

  private static int init(Object o) {
    int z80_interrupt_event = EventManager.eventRegister(Z80::z80_interrupt_event_fn, "Retriggered interrupt");
    int z80_nmi_event = EventManager.eventRegister(Z80::z80_nmi, "Non-maskable interrupt");
    int z80_nmos_iff2_event = EventManager.eventRegister(null, "IFF2 update dummy event");

    Module.register(z80ModuleInfo);

//    z80_debugger_variables_init();
    return 0;
  }

  private static void z80_nmi(long l, int i, Object o) {

  }

  private static void z80_interrupt_event_fn(long l, int i, Object o) {
    ooz80.interruption();
  }

  public static void doOpcodes() {
    ooz80.getState().tstates = Spectrum.tstates;
    while (Spectrum.tstates < EventManager.eventNextEvent) {
      bridgeCommand.invoke(0, null);
      ooz80.getState().tstates = Spectrum.tstates;
      ooz80.getState().tstates2 = Spectrum.tstates;
//      System.out.printf("Event processed, tstates: %d\n", Spectrum.tstates);
      phaseProcessor.initialTStates = Spectrum.tstates;
      ooz80.execute();
      Spectrum.tstates = ooz80.getState().tstates;
    }
    Spectrum.tstates = ooz80.getState().tstates;
  }

  private static void updateScreen() {
    WordNumber[] data = ooz80.getState().getMemory().getData();
    for (int i = 0; i < 0x4000; i++) {
      Spectrum.RAM[0][i] = (byte) data[i + 0x4000].intValue();
    }
  }

  public static void createScreen(KeyListener keyListener, JComponent contentPane) {
    JFrame frame = new JFrame("Fuse ZX Spectrum");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setContentPane(contentPane);
    frame.setLocationRelativeTo(null);
    frame.pack();
    frame.setVisible(true);
    frame.addKeyListener(keyListener);
  }
}
