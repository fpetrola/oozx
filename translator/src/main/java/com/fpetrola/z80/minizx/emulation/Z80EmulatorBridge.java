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

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.cpu.FetchListener;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.ide.InstructionTableModel;
import com.fpetrola.z80.ide.Z80Debugger;
import com.fpetrola.z80.ide.Z80Emulator;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.RegisterSpy;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;

import javax.script.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
  private State<T> state;
  private Instruction<T> fetchedInstruction;
  private NashornScriptEngine engine;
  private Map<String, CompiledScript> scripts = new HashMap<>();

  public Z80EmulatorBridge(RegisterSpy<T> pc, OOZ80<T> ooz80, int emulateUntil, int pause) {
    this.pc = pc;
    this.ooz80 = ooz80;
    this.state = ooz80.getState();
    this.emulateUntil = emulateUntil;
    this.pause = pause;
    thread = createThread();
    for (int i = 0; i < 0xFFFF; i++) {
      instructions.add(null);
    }

    ScriptEngineManager factory = new ScriptEngineManager();
    engine = (NashornScriptEngine) factory.getEngineByName("nashorn");
  }

  private Thread createThread() {
    return new Thread(() -> {
      System.out.println("starting thread");

      int i = 0;

      while (pc.read().intValue() != emulateUntil && enabled) {
        if ((i++ % (pause * 10000)) == 0) {
          ooz80.getState().setINTLine(true);
        } else {
          if (i % pause == 0) {
            doExecuteStep();
            ooz80.getState().setINTLine(false);
          }
        }
      }

      System.out.println("ending thread");
    });
  }

  private void doExecuteStep() {
    for (int i = 0; i < breakpointsTableModel.getRowCount(); i++) {
      String valueAt = (String) breakpointsTableModel.getValueAt(i, 2);

      Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
      bindings.put("PC", state.getPc().read().intValue());
      bindings.put("SP", state.getRegisterSP().read().intValue());
      try {
        CompiledScript o = scripts.get(valueAt);
        if (o == null)
          scripts.put(valueAt, o = engine.compile(valueAt));

        Object eval = o.eval(bindings);
        if (Boolean.TRUE.equals(eval)) {
          stopExecution();
          System.out.println(eval);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    ooz80.execute();
  }

  public void step() {
    stopExecution();
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

//    updateSelectedRow(pc.read().intValue());
  }

  public String[] getInstructions() {
    return instructions.stream().map(i -> i != null ? i.toString() : "-").toList().toArray(new String[0]);
  }

  public byte[] getMemory() {
    T[] data = ooz80.getState().getMemory().getData();
    byte[] bytes = new byte[0x10000];

//    for (int i = 0; i < 0x10000; i++) {
//      bytes[i]= (byte) data[i].intValue();
//    }
    return bytes;
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
      if (!(instruction instanceof RepeatingInstruction<T>)) {
        fetchedInstruction = instruction;
        int addressValue = address.intValue();

        Map<String, JComponent> instructionTables = new ConcurrentHashMap<>(Z80Debugger.instructionTables);
        Optional<Map.Entry<String, JComponent>> found = Optional.empty();
        for (Map.Entry<String, JComponent> e : instructionTables.entrySet()) {
          Block block = Z80Debugger.blockManager.findBlockByName(e.getKey());
          if (block != null && block.contains(addressValue)) {
            found = Optional.of(e);
            break;
          }
        }
        if (found.isPresent()) {
          Map.Entry<String, JComponent> entry = found.get();
          JComponent value = entry.getValue();
          JScrollPane instructionScrollPane = (JScrollPane) value;
          instructionScrollPane.putClientProperty("validated2", "false");

          JTable instructionTable1 = (JTable) ((JViewport) instructionScrollPane.getComponent(0)).getComponent(0);
          InstructionTableModel model1 = (InstructionTableModel) instructionTable1.getModel();
          instructionTable.setModel(model1);
          model1.process(addressValue, ooz80, instruction, instructionTable);
          DefaultMutableTreeNode defaultMutableTreeNode = Z80Debugger.treeNodes.get(entry.getKey());
          if (defaultMutableTreeNode != null) {
            Runnable runnable = () -> {
              JTree routinesTree = Z80Debugger.routinesTree;
              DefaultMutableTreeNode root = (DefaultMutableTreeNode) routinesTree.getModel().getRoot();
              int index = root.getIndex(defaultMutableTreeNode);
              routinesTree.setSelectionRow(index);
              routinesTree.scrollRowToVisible(index);
            };

            SwingUtilities.invokeLater(runnable);
          }
        } else {
          InstructionTableModel<T> model = (InstructionTableModel) instructionTable.getModel();
          model.process(addressValue, ooz80, instruction, instructionTable);
        }
      }
    };
  }

  private boolean isRepeating() {
    return fetchedInstruction instanceof RepeatingInstruction<?>;
  }

}
