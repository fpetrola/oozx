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

package com.fpetrola.z80.ide;

import com.fpetrola.z80.blocks.spy.QueueExecutor;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.minizx.emulation.ToStringInstructionVisitor;
import com.fpetrola.z80.opcodes.references.WordNumber;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Collections;
import java.util.Vector;

import static com.fpetrola.z80.helpers.Helper.formatAddress;

public class InstructionTableModel<T extends WordNumber> extends DefaultTableModel {
  private Vector<Integer> addressToRow = new Vector<>();
  private static QueueExecutor queueExecutor = new QueueExecutor();
  private double startTime = System.currentTimeMillis();

  public InstructionTableModel(Object[][] data, Object[] columnNames) {
    super(data, columnNames);

  }

  public void process(int addressValue, OOZ80<T> ooz80, Instruction<T> instruction, JTable instructionTable) {
    boolean addressIsPresent = addressToRow.contains(addressValue);
    if (!addressIsPresent) {
      int rowNumber = Collections.binarySearch(addressToRow, addressValue);
      int rowNumber1 = -(rowNumber + 1);

      if (rowNumber1 > addressToRow.size() - 1)
        addressToRow.add(addressValue);
      else
        addressToRow.add(rowNumber1, addressValue);

      T[] data = ooz80.getState().getMemory().getData();

      int length = instruction.getLength();
      StringBuilder opcodes = new StringBuilder();
      for (int i = 0; i < length; i++) {
        T datum = data[addressValue + i];
        opcodes.append("%02X ".formatted(datum.intValue()));
      }

      Runnable runnable = () -> {
        String string = getString(instruction);
        Object[] rowData = {false, formatAddress(addressValue), opcodes.toString(), string};
        insertRow(rowNumber1, rowData);
      };

      queueExecutor.threadSafeQueue.add(runnable);
    }

    long currentTime = System.currentTimeMillis();
    if (currentTime - startTime > 50) {
      queueExecutor.threadSafeQueue.add(() -> {
        SwingUtilities.invokeLater(() -> updateSelectedRow(addressValue, instructionTable));
      });
      startTime = System.currentTimeMillis();
    }
  }

  private void updateSelectedRow(int j, JTable instructionTable) {
    int index0 = addressToRow.indexOf(j);
    if (instructionTable.getRowCount() > index0) {
      instructionTable.setRowSelectionInterval(index0, index0);
      instructionTable.scrollRectToVisible(new Rectangle(instructionTable.getCellRect(index0, 0, true)));
    }
  }

  private String getString(Instruction<T> instruction) {
    ToStringInstructionVisitor visitor = new ToStringInstructionVisitor();
    return visitor.createToString(instruction);
  }

  public void setComponent(JTable instructionTable) {
//    this.instructionTable = instructionTable;
  }
}
