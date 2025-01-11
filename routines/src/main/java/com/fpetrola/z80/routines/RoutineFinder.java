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
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.IPopReturnAddress;
import com.fpetrola.z80.se.ReturnAddressWordNumber;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.se.StackListener;
import com.fpetrola.z80.spy.ExecutionListener;
import com.fpetrola.z80.transformations.StackAnalyzer;

import java.util.HashSet;
import java.util.Set;

import static com.fpetrola.z80.registers.RegisterName.SP;

@SuppressWarnings("ALL")
public class RoutineFinder<T extends WordNumber> {
  private final StackAnalyzer<T> stackAnalyzer;
  private Instruction<T> lastInstruction;
  private Routine currentRoutine;
  private RoutineManager routineManager;
  private int lastPc;
  private Set<Integer> processedPcs = new HashSet<>();
  private final State<T> state;

  public RoutineFinder(RoutineManager routineManager, StackAnalyzer<T> stackAnalyzer1, State<T> state) {
    this.routineManager = routineManager;
    this.stackAnalyzer = stackAnalyzer1;
    this.state = state;
  }

  public void checkBeforeExecution(Instruction<T> instruction) {
    if (instruction instanceof Ld<T> ld && ld.getTarget() instanceof Register<?> register && register.getName().equals(SP.name())) {
      int value = ld.getSource().read().intValue();

//      int sp = state.getRegisterSP().read().intValue();
//
//      while (sp != value) {
//        T t = Memory.read16Bits(state.getMemory(), WordNumber.createValue(sp));
//        if (t instanceof ReturnAddressWordNumber returnAddressWordNumber) {
//          int returnAddress = t.intValue();
//          int popAddress = pcValue;
//          int popAddress1 = popAddress;
//
//          if (sp + 2 != value)
//            popAddress1 += instruction.getLength();
//
//          IPopReturnAddress<WordNumber> simulatedPopReturnAddress = new SimulatedPopReturnAddress(returnAddress, popAddress1);
//          processPopInstruction(pcValue, simulatedPopReturnAddress);
//          System.out.println("pop instruction: " + Helper.formatAddress(returnAddress));
//        }
//        System.out.println("back to: " + currentRoutine);
//        sp += 2;
//      }

//      System.out.println("");
    }
  }

  public void checkExecution(Instruction<T> instruction) {
    int instructionLength = instruction.getLength();
    if (instructionLength > 0) {
      int pcValue = state.getPc().read().intValue();
      try {
        processedPcs.add(pcValue);

        updateCallers(instruction, pcValue);

        if (currentRoutine == null)
          createOrUpdateCurrentRoutine(pcValue, instruction.getLength());

        if (lastInstruction instanceof JP<T> jp && jp.getPositionOpcodeReference() instanceof Register<T> register) {
          T t = Memory.read16Bits(state.getMemory(), state.getRegisterSP().read());
          if (t.intValue() == lastPc + 1) {
//          boolean syntheticReturnAddress = routineManager.getDataflowService().isSyntheticReturnAddress();
            WordNumber nextPC = register.read();
            if (nextPC != null) {
              createOrUpdateCurrentRoutine(nextPC.intValue(), instruction.getLength());
            }
          }
        }

        if (lastInstruction instanceof Call) {
          processCallInstruction(instruction);
        }
        final boolean[] returnAddressPopped = new boolean[1];

        this.stackAnalyzer.listenEvents(new StackListener() {
          public void returnAddressPopped(ReturnAddressWordNumber returnAddressWordNumber, int pcValue1) {
            if (returnAddressWordNumber != null) {
              returnAddressPopped[0] = true;
              processPopInstruction(pcValue, new IPopReturnAddress() {
                public ReturnAddressWordNumber getReturnAddress() {
                  return returnAddressWordNumber;
                }

                public int getPreviousPc() {
                  return lastPc;
                }

                public int getPopAddress() {
                  return pcValue1;
                }
              });
            }
          }
        });
        if (!returnAddressPopped[0]) {
//        if (instruction instanceof IPopReturnAddress popReturnAddress && popReturnAddress.getReturnAddress() != null) {
//          processPopInstruction(pcValue, popReturnAddress);
//        } else {
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
    int previousPc = lastPc;
    System.out.printf("pop1: %d %d %d %n", popReturnAddress.getPopAddress(), popReturnAddress.getReturnAddress().intValue(), pcValue);
    ReturnAddressWordNumber returnAddress1 = popReturnAddress.getReturnAddress();
    Routine returnRoutine = routineManager.findRoutineAt(returnAddress1.intValue() - 1);
    if (returnRoutine != null) {
      if (previousPc != -1) {
        currentRoutine.getVirtualPop().put(previousPc, popReturnAddress.getPopAddress());
      }
      returnRoutine.addReturnPoint(returnAddress1.pc, pcValue + 1);
      this.currentRoutine = returnRoutine;
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

  public void reset() {
    lastInstruction = null;
    lastPc = -1;
    currentRoutine = null;
  }

  public <T extends WordNumber> boolean alreadyProcessed(Instruction<T> instruction, int pcValue) {
    return !(instruction instanceof Call) && !(instruction instanceof Ret) && processedPcs.contains(pcValue);
  }

  public void addExecutionListener(InstructionExecutor instructionExecutor) {
    instructionExecutor.addExecutionListener(new ExecutionListener<T>() {
      public void beforeExecution(Instruction instruction) {
        RoutineFinder.this.checkBeforeExecution(instruction);
      }

      public void afterExecution(Instruction<T> instruction) {
        RoutineFinder.this.checkExecution(instruction);
      }
    });
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
