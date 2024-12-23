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

import com.fpetrola.z80.blocks.spy.QueueExecutor;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.ide.Z80Emulator;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.RegisterSpy;
import com.fpetrola.z80.spy.RegisterWriteListener;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.*;

class Z80EmulatorBridge<T extends WordNumber> extends Z80Emulator {
  private final RegisterSpy<T> pc;
  private final int initialPcValue;
  private int lastPCValue;
  private boolean enabled;

  private Thread thread;
  private final OOZ80<T> ooz80;
  private final int emulateUntil;
  private List<Instruction<T>> instructions = new ArrayList<>();
  private int pause;
  private QueueExecutor queueExecutor;
  private State<T> state;

  public Z80EmulatorBridge(RegisterSpy<T> pc, OOZ80<T> ooz80, int emulateUntil, int pause) {
    this.pc = pc;
    this.initialPcValue = pc.read().intValue();
    lastPCValue = pc.read().intValue();
    this.ooz80 = ooz80;
    this.state= ooz80.getState();
    this.emulateUntil = emulateUntil;
    this.pause = pause;
    thread = createThread();
    for (int i = 0; i <= 0xFFFF; i++) {
      instructions.add(null);
    }

    queueExecutor = new QueueExecutor();
  }

  private List<Instruction<T>> updateInstructions() {
    RegisterSpy<T> pc = (RegisterSpy<T>) state.getPc();
    pc.listening(false);
    DefaultInstructionFetcher<T> instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    int start = state.getPc().read().intValue();
    int i = 0;
    int nextInstructionIndex = 0;
    while (i <= 0xffff) {
      if (i >= start && i <= start + 5) {
        if (i >= nextInstructionIndex) {
          T value = createValue(i);
          state.getPc().write(value);
          Instruction<T> instruction = instructionFetcher.fetchInstruction(value);
          nextInstructionIndex = i + instruction.getLength();
          instructions.set(i, instruction);
        }
      }
      i++;
    }

    state.getPc().write(createValue(start));
    pc.listening(true);

    return instructions;
  }

  private Thread createThread() {
    return new Thread(() -> {
      System.out.println("starting thread");

      int i = 0;

      while (pc.read().intValue() != emulateUntil && enabled) {
        if ((i++ % (pause * 100)) == 0) {
          ooz80.getState().setINTLine(true);
        } else {
          if (i % pause == 0) {
            doExecuteStep();
          }
        }
      }

      System.out.println("ending thread");
    });
  }

  private void doExecuteStep() {
    ooz80.execute();
  }

  public void step() {
    do {
      doExecuteStep();
    } while (isRepeating());
  }

  public void continueExecution() {
    enabled = true;
    thread.start();
  }

  public void stopExecution() {
    pc.write(createValue(initialPcValue));
    enabled = false;
    thread = createThread();
  }

  public String[] getInstructions() {
    return instructions.stream().map(i -> i != null ? i.toString() : "-").toList().toArray(new String[0]);
  }

  public int getPC() {
    return pc.read().intValue();
  }

  @Override
  public int[] getRegisters() {
    return new int[]{rv(AF), rv(BC), rv(DE), rv(HL), rv(IX), rv(IY), rv(SP), rv(PC)};
  }

  private int rv(RegisterName registerName) {
    return ooz80.getState().getRegister(registerName).read().intValue();
  }

  public RegisterWriteListener<T> getRegisterWriteListener() {
    return (value, increment) -> {
      if (lastPCValue != 0) {
        if (!isRepeating()) {
          updateInstructions();
          int lastPCValue1 = value.intValue();
          Runnable runnable = () -> {
            lastPCValue = lastPCValue1;

            List<String> list = new ArrayList<>();
            for (int j = lastPCValue1; j < lastPCValue1 + 10; j++) {
              Instruction<T> i = instructions.get(j);
              String s = i != null ? i.toString() : "-";
              list.add(s);
              instructionTable.setValueAt(false, j, 0);
              instructionTable.setValueAt(com.fpetrola.z80.helpers.Helper.formatAddress(j), j, 1);
              instructionTable.setValueAt(s, j, 2);
            }


//              updateListener.run();
          };
          SwingUtilities.invokeLater(() -> {
            instructionTable.setRowSelectionInterval(lastPCValue, lastPCValue);
            instructionTable.scrollRectToVisible(new Rectangle(instructionTable.getCellRect(lastPCValue, 0, true)));
          });

          queueExecutor.threadSafeQueue.add(runnable);
//              runnable.run();
        }
      }
    };
  }

  private boolean isRepeating() {
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    return instructionFetcher.instruction2 instanceof RepeatingInstruction<?>;
  }
}
