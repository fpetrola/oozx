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

import com.fpetrola.oozx.screen.FuseScreen;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.jspeccy.RegistersBase;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.MiniZXIO;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.opcodes.references.WordNumber;
import fuse.tstates.AddStatesMemoryReadListener;
import fuse.tstates.AddStatesMemoryWriteListener;
import fuse.tstates.PhaseProcessor;

import javax.swing.*;
import java.awt.event.KeyListener;
import java.util.function.Function;

public class Z80 {
  public static long interruptsEnabledAt;
  private static OOZ80<WordNumber> ooz80;

  public static void interrupt() {

  }

  public static void registerStartup() {
    Machine.reset(false);

    MiniZXIO io = new MiniZXIO() {
      public synchronized WordNumber in(WordNumber port) {
        byte b = Periph.readPort(port.intValue());
        return WordNumber.createValue(b);
      }

      public void out(WordNumber port, WordNumber value) {
//        Periph.writePort(port.intValue(), (byte) value.intValue());
      }
    };
    ooz80 = EmulatedMiniZX.createOOZ80(io);
//    MiniZX.createScreen(io.miniZXKeyboard, EmulatedMiniZX.getMemFunction(ooz80));
    byte[][] bytes = new byte[1000][1000];
    createScreen(io.miniZXKeyboard, EmulatedMiniZX.getMemFunction(ooz80), bytes);
    UiDisplay.screenMatrix = bytes;

    Keyboard.keyboard = io.miniZXKeyboard;

    RegistersBase registersBase = new RegistersBase<>(ooz80.getState());

    String first = com.fpetrola.z80.helpers.Helper.getSnapshotFile("file:///home/fernando/dynamitedan1.z80");
    State<?> state = ooz80.getState();
    Memory<WordNumber> memory = (Memory<WordNumber>) state.getMemory();

    PhaseProcessor<WordNumber> phaseProcessor = new PhaseProcessor<>(ooz80);
    memory.addMemoryReadListener(new AddStatesMemoryReadListener<>(phaseProcessor));
    memory.addMemoryWriteListener(new AddStatesMemoryWriteListener<>(phaseProcessor));
    SnapshotLoader.setupStateWithSnapshot(registersBase, first, state);

    updateScreen();

    memory.addMemoryWriteListener((address, value) -> {
      com.fpetrola.oozx.Memory.writeByte(address.intValue(), (byte) value.intValue());
      if (address.intValue() >= 0x4000 && address.intValue() < 0x8000) {
        Spectrum.RAM[0][address.intValue() - 0x4000] = (byte) value.intValue();
      }
    });

    IO<?> io1 = state.getIo();
  }

  public static void doOpcodes() {
    int startTstates = ooz80.getState().tstates;
    int tstates = 0;
    while (tstates < EventManager.eventNextEvent) {
      ooz80.execute();
      tstates += (ooz80.getState().tstates - startTstates);
    }
    Spectrum.tstates += tstates;
  }

  private static void updateScreen() {
    WordNumber[] data = ooz80.getState().getMemory().getData();
    for (int i = 0; i < 0x4000; i++) {
      Spectrum.RAM[0][i] = (byte) data[i + 0x4000].intValue();
    }
  }

  public static void createScreen(KeyListener keyListener, Function<Integer, Integer> memFunction, byte[][] bytes) {
    JFrame frame = new JFrame("Fuse ZX Spectrum");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setContentPane(new FuseScreen(memFunction, bytes));
    frame.setLocationRelativeTo(null);
    frame.pack();
    frame.setVisible(true);
    frame.addKeyListener(keyListener);
  }
}
