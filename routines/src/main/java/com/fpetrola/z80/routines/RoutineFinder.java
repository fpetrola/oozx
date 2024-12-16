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

package com.fpetrola.z80.routines;

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.blocks.references.BlockRelation;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.IPopReturnAddress;
import com.fpetrola.z80.se.ReturnAddressWordNumber;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;

import static com.fpetrola.z80.registers.RegisterName.SP;

@SuppressWarnings("ALL")
public class RoutineFinder<T extends WordNumber> {
  private Instruction lastInstruction;
  private Routine currentRoutine;
  private RoutineManager routineManager;
  private int lastPc;

  public RoutineFinder(RoutineManager routineManager) {
    this.routineManager = routineManager;
  }

  public void checkBeforeExecution(Instruction<T> instruction, int pcValue, State<T> state) {
    if (instruction instanceof Ld<T> ld && ld.getTarget() instanceof Register<?> register && register.getName().equals(SP.name())) {
      int value = ld.getSource().read().intValue();

      int sp = state.getRegisterSP().read().intValue();

      while (sp != value) {
        T t = Memory.read16Bits(state.getMemory(), WordNumber.createValue(sp));
        if (t instanceof ReturnAddressWordNumber returnAddressWordNumber) {
          int returnAddress = t.intValue();
          int popAddress = pcValue;
          IPopReturnAddress<WordNumber> simulatedPopReturnAddress = new SimulatedPopReturnAddress(returnAddress, popAddress + 3);
          processPopInstruction(pcValue, simulatedPopReturnAddress);
          System.out.println("pop instruction: " + Helper.formatAddress(returnAddress));
        }
        sp += 2;
      }

      System.out.println("back to: " + currentRoutine);
      System.out.println("");
    }
  }

  public void checkExecution(Instruction<T> instruction, int pcValue, State<T> state) {
    try {
      updateCallers(instruction, pcValue);

      if (currentRoutine == null)
        createOrUpdateCurrentRoutine(pcValue, instruction.getLength());

      if (lastInstruction instanceof Call) {
        processCallInstruction(instruction);
      }

      if (instruction instanceof IPopReturnAddress popReturnAddress && popReturnAddress.getReturnAddress() != null) {
        processPopInstruction(pcValue, popReturnAddress);
      } else {
        currentRoutine.addInstructionAt(instruction, pcValue);
        if (instruction instanceof Ret ret) {
          processRetInstruction(ret);
        }
      }
    } finally {
      routineManager.optimizeAll();
      lastInstruction = instruction;
      lastPc = pcValue;
    }
  }

  private void processCallInstruction(Instruction instruction) {
    WordNumber nextPC = ((ConditionalInstruction) lastInstruction).getNextPC();
    if (nextPC != null) {
//      System.out.printf("CALL: %H%n", nextPC.intValue());
      createOrUpdateCurrentRoutine(nextPC.intValue(), instruction.getLength());
    }
  }

  private void processRetInstruction(Ret ret) {
    if (ret.getNextPC() != null) {
      this.currentRoutine = routineManager.findRoutineAt(ret.getNextPC().intValue() - 1);
    }
  }

  private void processPopInstruction(int pcValue, IPopReturnAddress popReturnAddress) {
    ReturnAddressWordNumber returnAddress1 = popReturnAddress.getReturnAddress();
    Routine returnRoutine = routineManager.findRoutineAt(returnAddress1.pc);
    if (returnRoutine != null) {
      if (popReturnAddress.getPreviousPc() != -1) {
        currentRoutine.getVirtualPop().put(popReturnAddress.getPreviousPc(), popReturnAddress.getPopAddress());
      }
      returnRoutine.addReturnPoint(returnAddress1.pc, popReturnAddress.getPopAddress());
      this.currentRoutine = returnRoutine;
    }
  }

  private Routine createOrUpdateCurrentRoutine(int startAddress, int length) {
    Block lastCurrentRoutine = null;
    if (currentRoutine != null)
      lastCurrentRoutine = routineManager.blocksManager.findBlockAt(currentRoutine.getStartAddress());
    currentRoutine = routineManager.findRoutineAt(startAddress);

    if (currentRoutine != null) {
      if (currentRoutine.getEntryPoint() != startAddress) {
        Routine newRoutine = currentRoutine.split(startAddress);
        currentRoutine = newRoutine;
      } else {
//        System.out.println("eswrg43346346");
      }
    } else {
      currentRoutine = routineManager.createRoutine(startAddress, length);
    }

    if (lastCurrentRoutine != null) {
      BlockRelation blockRelation = BlockRelation.createBlockRelation(lastCurrentRoutine.getRangeHandler().getStartAddress(), startAddress);
      lastCurrentRoutine.getReferencesHandler().addBlockRelation(blockRelation);
    }

    return currentRoutine;
  }

  private void updateCallers(Instruction instruction, int pcValue) {
    if (instruction instanceof ConditionalInstruction<?, ?> conditionalInstruction) {
      if (conditionalInstruction.getNextPC() != null)
        if (instruction instanceof Call) {
          routineManager.callers2.put(conditionalInstruction.getNextPC().intValue(), pcValue);
        } else if (!(instruction instanceof Ret<?>)) {
          routineManager.callers.put(conditionalInstruction.getNextPC().intValue(), pcValue);
          routineManager.callees.put(pcValue, conditionalInstruction.getNextPC().intValue());
        }
    }
  }

  public RoutineManager getRoutineManager() {
    return routineManager;
  }

  private class SimulatedPopReturnAddress implements IPopReturnAddress<WordNumber> {
    private final int returnAddress;
    private final int popAddress;

    public SimulatedPopReturnAddress(int returnAddress, int popAddress) {
      this.returnAddress = returnAddress;
      this.popAddress = popAddress;
    }

    public ReturnAddressWordNumber getReturnAddress() {
      return new ReturnAddressWordNumber(returnAddress, returnAddress - 3);
    }

    public int getPreviousPc() {
      return lastPc;
    }

    public int getPopAddress() {
      return popAddress;
    }
  }
}
