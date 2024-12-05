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
import com.fpetrola.z80.blocks.BlockRoleVisitor;
import com.fpetrola.z80.blocks.references.BlockRelation;
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.se.PopReturnAddress;
import com.fpetrola.z80.se.ReturnAddressWordNumber;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("ALL")
public class RoutineFinder {
  private Instruction lastInstruction;

  private Routine currentRoutine;

  public static ListValuedMap<Integer, Integer> callers = new ArrayListValuedHashMap<>();
  public static ListValuedMap<Integer, Integer> callees = new ArrayListValuedHashMap<>();

  public RoutineManager getRoutineManager() {
    return routineManager;
  }

  private RoutineManager routineManager;
  private int lastPc;

  public RoutineFinder(RoutineManager routineManager) {
    this.routineManager = routineManager;
    callers.clear();
    callees.clear();
  }

  public void checkExecution(Instruction instruction, int pcValue) {

    try {
      if (pcValue == 0xD895)
        System.out.printf("");

      if (instruction instanceof ConditionalInstruction<?, ?> conditionalInstruction) {
        if (!(instruction instanceof Call) && !(instruction instanceof Ret<?>))
          if (conditionalInstruction.getNextPC() != null) {
            callers.put(conditionalInstruction.getNextPC().intValue(), pcValue);
            callees.put(pcValue, conditionalInstruction.getNextPC().intValue());
          }
      }

      if (currentRoutine == null)
        createOrUpdateCurrentRoutine(pcValue, instruction.getLength());

      if (lastInstruction instanceof Call) {
        processCallInstruction(instruction);
      }

      if (instruction instanceof PopReturnAddress popReturnAddress) {
        processPopInstruction(instruction, pcValue, popReturnAddress);
      } else {
        currentRoutine.addInstructionAt(instruction, pcValue);
        if (instruction instanceof Ret ret) {
          processRetInstruction(ret);
        } else if (instruction instanceof ConditionalInstruction<?, ?> conditionalInstruction) {
          WordNumber nextPC = conditionalInstruction.getNextPC();
          if (nextPC != null)
            if (!(conditionalInstruction instanceof Call<?>))
              processNonCalls(pcValue, nextPC);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      routineManager.optimizeAll();
      lastInstruction = instruction;
      lastPc = pcValue;
    }
  }

  private void processNonCalls(int pcValue, WordNumber nextPC) {
    Routine routineAt1 = routineManager.findRoutineAt(pcValue);
    if (routineAt1 != null)
      currentRoutine = routineAt1;

    if (nextPC.intValue() == 0xD895)
      System.out.printf("");
    if (pcValue == 0xD304)
      System.out.println("ddgsdggd");
    int address = nextPC.intValue();
    Routine routineAt = routineManager.findRoutineAt(address);

    if (routineAt != null && routineAt != currentRoutine && !currentRoutine.containsInner(routineAt)) {
      if (routineAt.getParent() != null) {
        routineAt.getParent().removeInnerRoutine(routineAt);
        routineManager.addRoutine(new Routine(routineAt.getBlocks(), routineAt.getEntryPoint(), true));
      } else {
        Block blockOf = routineAt.findBlockOf(address);
        routineAt.removeBlock(blockOf);
        if (blockOf.getRangeHandler().getStartAddress() != address) {
          Block split = blockOf.split(address - 1);
          routineAt.addBlock(blockOf);
          blockOf = split;
        } else {
          if (routineAt.getBlocks().isEmpty())
            routineManager.removeRoutine(routineAt);
          System.out.println("quizas!");
        }
        Routine routine = new Routine(blockOf, address, true);
        routine.getVirtualPop().putAll(routineAt.getVirtualPop());
        routineManager.addRoutine(routine);
        routine.optimizeSplit();
      }
    }
  }

  private void processCallInstruction(Instruction instruction) {
    WordNumber nextPC = ((ConditionalInstruction) lastInstruction).getNextPC();
    if (nextPC != null)
      createOrUpdateCurrentRoutine(nextPC.intValue(), instruction.getLength());
  }

  private void processRetInstruction(Ret ret) {
    if (ret.getNextPC() != null) {
//            Routine routineAt = routineManager.findRoutineAt(pcValue);
//            if (currentRoutine != routineAt && !currentRoutine.contains(routineAt)) {
//              currentRoutine.addInnerRoutine(routineAt);
//            }
//            currentRoutine.finish();
      this.currentRoutine = routineManager.findRoutineAt(ret.getNextPC().intValue() - 1);
    }
  }

  private void processPopInstruction(Instruction instruction, int pcValue, PopReturnAddress popReturnAddress) {
    if (popReturnAddress.returnAddress0 != null) {
      ReturnAddressWordNumber returnAddress = popReturnAddress.getReturnAddress();
      if (returnAddress != null) {
        if (popReturnAddress.previousPc != -1)
          currentRoutine.getVirtualPop().put(popReturnAddress.previousPc, popReturnAddress.popAddress);
        Routine returnRoutine = routineManager.findRoutineAt(returnAddress.pc);
        returnRoutine.addReturnPoint(returnAddress.pc, pcValue + 1);
        this.currentRoutine = returnRoutine;
      }
    } else
      currentRoutine.addInstructionAt(instruction, pcValue);
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
        System.out.println("eswrg43346346");
      }
    } else {
      currentRoutine = routineManager.createRoutine(startAddress, length);
    }

//    System.out.println("chaging routine to: "+ currentRoutine);
    if (lastCurrentRoutine != null) {
      BlockRelation blockRelation = BlockRelation.createBlockRelation(lastCurrentRoutine.getRangeHandler().getStartAddress(), startAddress);
      lastCurrentRoutine.getReferencesHandler().addBlockRelation(blockRelation);
    }

    return currentRoutine;
  }

}
//Block lastCurrentRoutine = blocksManager.findBlockAt(lastPc);
//    if (lastCurrentRoutine != null)
//    lastCurrentRoutine.getReferencesHandler().addBlockRelation(BlockRelation.createBlockRelation(lastCurrentRoutine.getRangeHandler().getStartAddress(), startAddress));
