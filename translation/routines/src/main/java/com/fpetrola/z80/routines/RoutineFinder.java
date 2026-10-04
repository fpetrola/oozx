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
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.StackListener;
import com.fpetrola.z80.spy.ExecutionListener;
import com.fpetrola.z80.transformations.StackAnalyzer;

import java.util.HashSet;
import java.util.Set;

import static com.fpetrola.z80.registers.RegisterName.SP;

@SuppressWarnings("ALL")
public class RoutineFinder {
  private final StackAnalyzer stackAnalyzer;
  private Instruction lastInstruction;
  private Routine currentRoutine;
  private RoutineManager routineManager;
  private int lastPc;
  private Set<Integer> processedPcs = new HashSet<>();
  private final State state;
  private Integer lastSimulatedCallJump;

  public RoutineFinder(RoutineManager routineManager, StackAnalyzer stackAnalyzer1, State state) {
    this.routineManager = routineManager;
    this.stackAnalyzer = stackAnalyzer1;
    this.state = state;
  }

  public void checkBeforeExecution(Instruction instruction) {
    if (instruction instanceof Ld ld && ld.getTarget() instanceof Register register && register.getName().equals(SP.name())) {
      int value = ld.getSource().read();

//      int sp = state.getRegisterSP().read();
//
//      while (sp != value) {
//        int t = state.getMemory().read16Bits(sp);
//        if (t instanceof ReturnAddressWordNumber returnAddressWordNumber) {
//          int returnAddress = t;
//          int popAddress = pcValue;
//          int popAddress1 = popAddress;
//
//          if (sp + 2 != value)
//            popAddress1 += instruction.getLength();
//
//          IPopReturnAddress<int> simulatedPopReturnAddress = new SimulatedPopReturnAddress(returnAddress, popAddress1);
//          processPopInstruction(pcValue, simulatedPopReturnAddress);
//          System.out.println("pop instruction: " + Helper.formatAddress(returnAddress));
//        }
//        System.out.println("back to: " + currentRoutine);
//        sp += 2;
//      }

//      System.out.println("");
    }
  }

  public void checkExecution(Instruction instruction) {
    int instructionLength = instruction.getLength();
    if (instructionLength > 0) {
      int pcValue = state.getPc().read();

//      System.out.println("PC: %s -> routine: %s".formatted(Helper.formatAddress(pcValue), currentRoutine));

      if (pcValue == 0xB902)
        System.out.print("");
      try {
        routineManager.recordInstruction(pcValue, instruction);
        processedPcs.add(pcValue);

        updateCallers(instruction, pcValue);

        if (currentRoutine == null)
          createOrUpdateCurrentRoutine(pcValue, instruction.getLength());

        if (lastSimulatedCallJump != null) {
          createOrUpdateCurrentRoutine(lastSimulatedCallJump, instruction.getLength());
          lastSimulatedCallJump = null;
        }

        if (lastInstruction instanceof Call) {
          processCallInstruction(instruction);
        }

        boolean listened = this.stackAnalyzer.listenEvents(new StackListener() {
          public boolean returnAddressPopped(int pcValue, int returnAddress, int callAddress) {
            Routine returnRoutine = routineManager.findRoutineAt(callAddress);
            if (lastPc != -1)
              currentRoutine.getVirtualPop().put(lastPc, pcValue);

            returnRoutine.addReturnPoint(callAddress, routineManager.addressAfter(pcValue));
            currentRoutine = returnRoutine;
            return true;
          }

          public boolean jumpUsingRet(int pcValue, Set<Integer> jumpAddresses) {
            if (instruction instanceof Ret ret) {
              int nextPC = ret.getNextPC();
              if (nextPC != -1) {
//                int spValue = state.getRegisterSP().read();
//                var read = state.getMemory().read16Bits(spValue.plus(-2));
                routineManager.callers.put(nextPC, pcValue);
                routineManager.callees.put(pcValue, nextPC);
                currentRoutine.addInstructionAt(instruction, pcValue);
//                if (ret.getNextPC() != -1) {
//                  RoutineFinder.this.currentRoutine = routineManager.findRoutineAt(ret.getNextPC());
//                }
              }
            }
            return true;
          }

          public boolean simulatedCall(int pcValue, int jumpAddress, Set<Integer> jumpAddresses, int returnAddress) {
            if (instruction instanceof JP jp) {
              int nextPC = jp.getNextPC();
              if (nextPC != -1) {
                lastSimulatedCallJump = nextPC;

//                routineManager.callers.put(nextPC, pcValue);
//                routineManager.callees.put(pcValue, nextPC);
              }
            }
            return false;
          }

          @Override
          public boolean beginUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
            return StackListener.super.beginUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
          }

          @Override
          public boolean endUsingStackAsRepository(int pcValue, int newSpAddress, int oldSpAddress) {
            return StackListener.super.endUsingStackAsRepository(pcValue, newSpAddress, oldSpAddress);
          }

          @Override
          public boolean droppingReturnValues(int pcValue, int newSpAddress, int oldSpAddress, StackAnalyzer.Entry lastReturnAddress) {
//            if (lastReturnAddress != null)
//              currentRoutine = routineManager.findRoutineAt(lastReturnAddress.pc());
//
//            if (lastPc != -1)
//              currentRoutine.getVirtualPop().put(lastPc, pcValue);
//
//            returnRoutine.addReturnPoint(callAddress, pcValue + instructionLength);

            Routine returnRoutine = routineManager.findRoutineAt(lastReturnAddress.pc());
            if (lastPc != -1)
              currentRoutine.getVirtualPop().put(lastPc, pcValue);

            returnRoutine.addReturnPointDropped(lastReturnAddress.value(), routineManager.addressAfter(pcValue));
            currentRoutine = returnRoutine;

            return true;
          }
        });

        if (!listened) {
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
  }

  private void processCallInstruction(Instruction instruction) {
    int nextPC = ((ConditionalInstruction) lastInstruction).getNextPC();
    if (nextPC != -1) {
//      System.out.printf("CALL: %H%n", nextPC);
      createOrUpdateCurrentRoutine(nextPC, instruction.getLength());
    }
  }

  private void processRetInstruction(Ret ret) {
    if (ret.getNextPC() != -1) {
      this.currentRoutine = routineManager.findRoutineAt(ret.getNextPC() - 1);
    }
  }

  private Routine createOrUpdateCurrentRoutine(int startAddress, int length) {
    Block lastCurrentRoutineBlock = null;
    if (currentRoutine != null)
      lastCurrentRoutineBlock = routineManager.blocksManager.findBlockAt(currentRoutine.getStartAddress());
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

    if (lastCurrentRoutineBlock != null) {
      int startAddress1 = lastCurrentRoutineBlock.getRangeHandler().getStartAddress();

      if (!lastCurrentRoutineBlock.getReferencesHandler().containsRelation(startAddress1, startAddress)) {
        BlockRelation blockRelation = BlockRelation.createBlockRelation(startAddress1, startAddress);
        lastCurrentRoutineBlock.getReferencesHandler().addBlockRelation(blockRelation);
      }
    }

    return currentRoutine;
  }

  private void updateCallers(Instruction instruction, int pcValue) {
    if (instruction instanceof ConditionalInstruction<?> conditionalInstruction) {
      if (conditionalInstruction.getNextPC() != -1)
        if (instruction instanceof Call) {
          routineManager.callers2.put(conditionalInstruction.getNextPC(), pcValue);
        } else if (!(instruction instanceof Ret)) {
//          routineManager.callees.put(35211, 34762);
//          routineManager.callers.put(34762, 35211);

          routineManager.callers.put(conditionalInstruction.getNextPC(), pcValue);
          routineManager.callees.put(pcValue, conditionalInstruction.getNextPC());
        }
    }
  }

  public RoutineManager getRoutineManager() {
    return routineManager;
  }

  public void reset() {
    lastInstruction = null;
    lastPc = -1;
    currentRoutine = null;
  }

  public  boolean alreadyProcessed(Instruction instruction, int pcValue) {
    return !(instruction instanceof Call) && !(instruction instanceof Ret) && processedPcs.contains(pcValue);
  }

  public void addExecutionListener(InstructionExecutor instructionExecutor) {
    instructionExecutor.setExecutionListener(new ExecutionListener() {
      public void beforeExecution(Instruction instruction) {
        RoutineFinder.this.checkBeforeExecution(instruction);
      }

      public void afterExecution(Instruction instruction) {
        RoutineFinder.this.checkExecution(instruction);
      }
    });
  }

  private class SimulatedPopReturnAddress {
    private final int returnAddress;
    private final int popAddress;

    public SimulatedPopReturnAddress(int returnAddress, int popAddress) {
      this.returnAddress = returnAddress;
      this.popAddress = popAddress;
    }

    public int getPreviousPc() {
      return lastPc;
    }

    public int getPopAddress() {
      return popAddress;
    }
  }
}
