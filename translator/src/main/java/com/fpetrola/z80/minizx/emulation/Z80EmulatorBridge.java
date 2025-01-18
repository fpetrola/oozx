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

import com.fpetrola.z80.cpu.FetchListener;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.ide.InstructionTableModel;
import com.fpetrola.z80.ide.RoutineHandlingListener;
import com.fpetrola.z80.ide.Z80Debugger;
import com.fpetrola.z80.ide.Z80Emulator;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.instructions.types.RepeatingInstruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.spy.ObservableRegister;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;

import javax.script.*;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.fpetrola.z80.registers.RegisterName.*;

public class Z80EmulatorBridge<T extends WordNumber> extends Z80Emulator {
  private final ObservableRegister<T> pc;
  private boolean enabled;

  private Thread thread;
  private final OOZ80<T> ooz80;
  private final Predicate<Integer> continueEmulation;
  private List<Instruction<T>> instructions = new ArrayList<>();
  private int pause;
  private final RoutineManager routineManager;
  private State<T> state;
  private Instruction<T> fetchedInstruction;
  private NashornScriptEngine engine;
  private Map<String, CompiledScript> scripts = new HashMap<>();
  private TableModel model0;
  private Routine stepOutRoutine;

  public Z80EmulatorBridge(ObservableRegister pc, OOZ80 ooz80, Predicate<Integer> continueEmulation, int pause, RoutineManager routineManager) {
    this.pc = pc;
    this.ooz80 = ooz80;
    this.state = ooz80.getState();
    this.continueEmulation = continueEmulation;
    this.pause = pause;
    this.routineManager = routineManager;
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

      while (continueEmulation.test(pc.read().intValue()) && enabled) {
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
      int pcValue = state.getPc().read().intValue();
      bindings.put("PC", pcValue);
      bindings.put("SP", state.getRegisterSP().read().intValue());
      bindings.put("INSTRUCTION", new ToStringInstructionVisitor<T>().createToString(fetchedInstruction));
//      bindings.put("ROUTINE", routineManager.findRoutineAt(pcValue));

      try {
        CompiledScript o = scripts.get(valueAt);
        if (o == null)
          scripts.put(valueAt, o = engine.compile(valueAt));

        Object eval = o.eval(bindings);
        if (Boolean.TRUE.equals(eval)) {
          stopExecution();
          System.out.println(eval);
          return;
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

  public void stepOut() {
    stopExecution();
    int pcValue = state.getPc().read().intValue();
    stepOutRoutine = routineManager.findRoutineAt(pcValue);
    boolean finished = false;
    int calls = 1;
    int executions = 0;
    do {
      pcValue = state.getPc().read().intValue();
      Routine currentRoutine = routineManager.findRoutineAt(pcValue);
      String toString = new ToStringInstructionVisitor<T>().createToString(fetchedInstruction);
      if (toString.contains("CALL"))
        calls++;

      if (toString.startsWith("RET")) {
        Ret<T> ret = (Ret<T>) fetchedInstruction;
        boolean b = ret.getCondition().conditionMet(ret);
        if (b) {
          if (currentRoutine == stepOutRoutine || executions > 10000) {
            finished = true;
          }
          calls--;
        }
      }
      doExecuteStep();
      executions++;
    } while (!finished);
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
      fetchedInstruction = instruction;
      if (true || !(instruction instanceof RepeatingInstruction<T>)) {
        if (model0 == null) {
          model0 = instructionTable.getModel();
        }

        int addressValue = address.intValue();

        Map<Routine, JComponent> instructionTables = new ConcurrentHashMap<>(Z80Debugger.instructionTables);
        Routine routineAt = routineManager.findRoutineAt(addressValue);
        if (routineAt != null) {
          JComponent jComponent = instructionTables.get(routineAt);

          if (jComponent != null) {
            Runnable runnable = () -> {
              InstructionTableModel model1 = showRoutineInstructions(jComponent);
              model1.process(addressValue, ooz80, instruction, instructionTable);
              DefaultMutableTreeNode defaultMutableTreeNode = Z80Debugger.treeNodes.get(routineAt);
              if (defaultMutableTreeNode != null) {
                JTree routinesTree = Z80Debugger.routinesTree;
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) routinesTree.getModel().getRoot();
                int index = root.getIndex(defaultMutableTreeNode);
                routinesTree.setSelectionRow(index + 1);
                routinesTree.scrollRowToVisible(index + 1);
              }
            };
            SwingUtilities.invokeLater(runnable);
            return;

          }

          SwingUtilities.invokeLater(() -> {
            instructionTable.setModel(model0);
            InstructionTableModel<T> model = (InstructionTableModel) instructionTable.getModel();
            Z80Debugger.setupColumnModel(instructionTable);
            model.process(addressValue, ooz80, instruction, instructionTable);
          });
        }
      }
    };
  }

  private InstructionTableModel showRoutineInstructions(JComponent jComponent) {
    JComponent instructionScrollPane = jComponent;
    instructionScrollPane.putClientProperty("validated2", "false");

    JTable instructionTable1 = (JTable) ((JViewport) instructionScrollPane.getComponent(0)).getComponent(0);
    InstructionTableModel model1 = (InstructionTableModel) instructionTable1.getModel();
    instructionTable.setModel(model1);
    Z80Debugger.setupColumnModel(instructionTable);
    return model1;
  }

  private boolean isRepeating() {
    return fetchedInstruction instanceof RepeatingInstruction<?>;
  }

  public RoutineHandlingListener getRoutineHandlingListener() {
    return new RoutineHandlingListener() {
      public void routineAdded(Routine routine) {
        JComponent mainPanel = Z80Debugger.addInstructionTable(routine);
      }

      public void routineRemoved(Routine routine) {
      }
    };
  }

  public TreeSelectionListener getTreeListener() {
    return e -> {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) e
          .getPath().getLastPathComponent();

      Map<Routine, JComponent> instructionTables = new ConcurrentHashMap<>(Z80Debugger.instructionTables);
      JComponent jComponent = instructionTables.get(node.getUserObject());

      if (jComponent != null)
        showRoutineInstructions(jComponent);
    };
  }
}
