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
import com.fpetrola.z80.instructions.Call;
import com.fpetrola.z80.instructions.Ret;
import com.fpetrola.z80.instructions.ReturnAddressWordNumber;
import com.fpetrola.z80.instructions.base.ConditionalInstruction;
import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.bytecode.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.opcodes.references.WordNumber;

@SuppressWarnings("ALL")
public class RoutineFinder {
  private Instruction lastInstruction;

  private Routine currentRoutine;
  private RoutineManager routineManager;
  private int lastPc;

  public RoutineFinder(RoutineManager routineManager) {
    this.routineManager = routineManager;
  }

  public void checkExecution(Instruction instruction, int pcValue) {

    try {
      if (pcValue == 37310)
        System.out.printf("");
      if (currentRoutine == null)
        createOrUpdateCurrentRoutine(pcValue, instruction.getLength());

      if (lastInstruction instanceof Call) {
        WordNumber nextPC = ((ConditionalInstruction) lastInstruction).getNextPC();
        if (nextPC != null)
          createOrUpdateCurrentRoutine(nextPC.intValue(), instruction.getLength());
      }

      if (instruction instanceof SymbolicExecutionAdapter.PopReturnAddress popReturnAddress) {
        if (popReturnAddress.returnAddress0 != null) {
          ReturnAddressWordNumber returnAddress = popReturnAddress.getReturnAddress();
          if (returnAddress != null) {
            if (popReturnAddress.previousPc != -1)
              currentRoutine.virtualPop.put(popReturnAddress.previousPc, popReturnAddress.popAddress);
            Routine returnRoutine = routineManager.findRoutineAt(returnAddress.pc);
            returnRoutine.addReturnPoint(returnAddress.pc, pcValue + 1);
            this.currentRoutine = returnRoutine;
          }
        } else
          currentRoutine.addInstructionAt(instruction, pcValue);
      } else {
        currentRoutine.addInstructionAt(instruction, pcValue);

        if (instruction instanceof Ret ret) {
          if (ret.getNextPC() != null) {
            Routine routineAt = routineManager.findRoutineAt(pcValue);
            if (currentRoutine != routineAt) {
              currentRoutine.addInnerRoutine(routineAt);
            }
            currentRoutine.finish();
            this.currentRoutine = routineManager.findRoutineAt(ret.getNextPC().intValue() - 1);
          }
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      routineManager.optimizeAll();
      lastInstruction = instruction;
      lastPc = pcValue;
    }
  }

  private Routine createOrUpdateCurrentRoutine(int startAddress, int length) {
    Block lastCurrentRoutine = null;
    if (currentRoutine != null)
      lastCurrentRoutine = routineManager.blocksManager.findBlockAt(currentRoutine.getStartAddress());
    currentRoutine = routineManager.findRoutineAt(startAddress);

    if (currentRoutine != null) {
      if (currentRoutine.getStartAddress() < startAddress) {
        Routine newRoutine = currentRoutine.split(startAddress);
        currentRoutine = newRoutine;
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

}
//Block lastCurrentRoutine = blocksManager.findBlockAt(lastPc);
//    if (lastCurrentRoutine != null)
//    lastCurrentRoutine.getReferencesHandler().addBlockRelation(BlockRelation.createBlockRelation(lastCurrentRoutine.getRangeHandler().getStartAddress(), startAddress));
