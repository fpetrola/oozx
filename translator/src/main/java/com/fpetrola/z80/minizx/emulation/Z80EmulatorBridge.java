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
import com.fpetrola.z80.cpu.FetchListener;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.ide.Z80Emulator;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.RegisterSpy;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

import static com.fpetrola.z80.helpers.Helper.formatAddress;
import static com.fpetrola.z80.registers.RegisterName.*;

class Z80EmulatorBridge<T extends WordNumber> extends Z80Emulator {
  private final RegisterSpy<T> pc;
  private boolean enabled;

  private Thread thread;
  private final OOZ80<T> ooz80;
  private final int emulateUntil;
  private List<Instruction<T>> instructions = new ArrayList<>();
  private int pause;
  private QueueExecutor queueExecutor;
  private State<T> state;
  private Vector<Integer> addressToRow = new Vector<>();
  private double startTime = System.currentTimeMillis();


  public Z80EmulatorBridge(RegisterSpy<T> pc, OOZ80<T> ooz80, int emulateUntil, int pause, DefaultInstructionFetcher alternativeInstructionFetcher) {
    this.pc = pc;
    this.ooz80 = ooz80;
    this.state = ooz80.getState();
    this.emulateUntil = emulateUntil;
    this.pause = pause;
    thread = createThread();
    for (int i = 0; i < 0xFFFF; i++) {
      instructions.add(null);
    }

    queueExecutor = new QueueExecutor();
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
    enabled = false;
    do {
      doExecuteStep();
    } while (isRepeating());
  }

  public void continueExecution() {
    enabled = true;
    thread.start();
  }

  public void stopExecution() {
//    pc.write(createValue(initialPcValue));
    enabled = false;
    thread = createThread();
  }

  public String[] getInstructions() {
    return instructions.stream().map(i -> i != null ? getString(i) : "-").toList().toArray(new String[0]);
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

  public FetchListener<T> getRegisterWriteListener() {
    return (address, instruction) -> {
      int addressValue = address.intValue();
      boolean addressIsPresent = addressToRow.contains(addressValue);
      if (!addressIsPresent) {
        int rowNumber = Collections.binarySearch(addressToRow, addressValue);
        int rowNumber1 = -(rowNumber + 1);

        if (rowNumber1 > addressToRow.size() - 1)
          addressToRow.add(addressValue);
        else
          addressToRow.add(rowNumber1, addressValue);

        Runnable runnable = () -> {
          DefaultTableModel model = (DefaultTableModel) instructionTable.getModel();
          String string = getString(instruction);
          model.insertRow(rowNumber1, new Object[]{false, formatAddress(addressValue), string});
        };

        queueExecutor.threadSafeQueue.add(runnable);
      }

      long currentTime = System.currentTimeMillis();
      if (currentTime - startTime > 50) {
        queueExecutor.threadSafeQueue.add(() -> {
          SwingUtilities.invokeLater(() -> updateSelectedRow(addressValue));
        });
        startTime = System.currentTimeMillis();
      }
    };
  }

  private String getString(Instruction<T> instruction) {
    ToStringInstructionVisitor visitor = new ToStringInstructionVisitor();
    return visitor.createToString(instruction);
  }

  private void updateSelectedRow(int j) {
    int index0 = addressToRow.indexOf(j);
    instructionTable.setRowSelectionInterval(index0, index0);
    instructionTable.scrollRectToVisible(new Rectangle(instructionTable.getCellRect(index0, 0, true)));
  }

  private boolean isRepeating() {
    DefaultInstructionFetcher instructionFetcher = (DefaultInstructionFetcher) ooz80.getInstructionFetcher();
    return instructionFetcher.instruction2 instanceof RepeatingInstruction<?>;
  }

}
