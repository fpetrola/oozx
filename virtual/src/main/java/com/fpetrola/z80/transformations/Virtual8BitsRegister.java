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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.instructions.impl.In;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.registers.Plain8BitRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Virtual8BitsRegister extends Plain8BitRegister implements IVirtual8BitsRegister {
  private final int address;
  private final InstructionExecutor instructionExecutor;
  public Instruction instruction;
  private final VirtualFetcher virtualFetcher;
  private final List<VirtualRegister> previousVersions = new ArrayList<>();
  protected int lastData;
  protected int reads;
  public IVirtual8BitsRegister lastVersionRead;
  private final Consumer dataConsumer;
  private final VirtualRegisterVersionHandler versionHandler;

  @Override
  public BlocksManager getBlocksManager() {
    return blocksManager;
  }

  private final BlocksManager blocksManager;
  public Instruction currentInstruction1;
  private final List<VirtualRegister> dependants = new ArrayList<>();
  private final Scope scope;
  public VirtualComposed16BitRegister virtualComposed16BitRegister;

  private boolean isComposed;

  @Override
  public boolean isInitialized() {
    return instruction instanceof Ld;
  }

  public Virtual8BitsRegister(int address, InstructionExecutor instructionExecutor, String name, Instruction instruction,
                              IVirtual8BitsRegister previousVersion, VirtualFetcher virtualFetcher, Consumer dataConsumer,
                              VirtualRegisterVersionHandler versionHandler, BlocksManager blocksManager, Instruction currentInstruction1) {
    super(name);
    this.address = address;
    this.instructionExecutor = instructionExecutor;
    this.instruction = instruction;
    this.virtualFetcher = virtualFetcher;
    this.dataConsumer = dataConsumer;
    this.versionHandler = versionHandler;
    this.blocksManager = blocksManager;
    this.currentInstruction1 = currentInstruction1;

    if (previousVersion != null)
      addPreviousVersion(previousVersion);

    if (instruction == null)
      this.instruction = new VirtualAssignmentInstruction(this, () -> this.getCurrentPreviousVersion());

    scope = new Scope(getRegisterLine(), getRegisterLine());
    scope.include(this);
  }

  @Override
  public boolean usesMultipleVersions() {
    return lastVersionRead != null && previousVersions.size() > 1;
  }

  public IVirtual8BitsRegister getCurrentPreviousVersion() {
    return previousVersions.isEmpty() ? null : (IVirtual8BitsRegister) previousVersions.get(0);
  }

  public int read() {
    int t = virtualFetcher.readFromVirtual(() -> instructionExecutor.isExecuting(instruction), () -> instructionExecutor.execute(instruction), () -> data, () -> (lastVersionRead = getCurrentPreviousVersion()).readPrevious());
    if (data == t)
      reads++;
    lastData = -1;
    data = t;

    dataConsumer.accept(data);

    return t;
  }

  @Override
  public void write(int value) {
    super.write(value);
    lastData = -1;
    dataConsumer.accept(value);
  }

  public void decrement() {
    read();
    super.decrement();
  }

  public void increment() {
    read();
    super.increment();
  }

  public void reset() {
    data = -1;
    reads = 0;
  }

  @Override
  public List<VirtualRegister> getPreviousVersions() {
    return previousVersions;
  }

  public void addPreviousVersion(IVirtual8BitsRegister previousVersion) {
    previousVersion.addDependant(this);
    previousVersions.remove(previousVersion);
    previousVersions.add(previousVersion);
    previousVersion.saveData();
  }

  @Override
  public void set16BitsRegister(VirtualComposed16BitRegister virtualComposed16BitRegister) {
    if (this.virtualComposed16BitRegister == null)
      this.virtualComposed16BitRegister = virtualComposed16BitRegister;
  }

  @Override
  public List<VirtualRegister> getDependants() {
    return dependants;
  }

  @Override
  public VirtualComposed16BitRegister getVirtualComposed16BitRegister() {
    return virtualComposed16BitRegister;
  }

  @Override
  public void addDependant(VirtualRegister virtualRegister) {
    if (!virtualRegister.getName().contains("%") && !dependants.contains(virtualRegister))
      dependants.add(virtualRegister);
//    scope.include(virtualRegister);
  }

  public void saveData() {
    lastData = data;
    // data = null;
  }

  public int readPrevious() {
    Helper.breakInStackOverflow();

//    if (data == null && lastData == null && reads == 0) {
//      for (VirtualRegister v1 : previousVersions) {
//        if (v1 != this) {
//          if (v1 instanceof MyVirtualRegister)
//            return v1.read();
//          else
//            return ((Virtual8BitsRegister) v1).readPrevious();
//        }
//      }
//    }

//    if (instruction instanceof Ld || instruction instanceof In) {
//      TargetSourceInstruction<T, ?> tt = (TargetSourceInstruction) instruction;
//      if ((tt.getTarget() instanceof Register) || tt.getTarget() instanceof IndirectMemory16BitReference indirectMemory16BitReference && indirectMemory16BitReference.target instanceof Register) {
//        int result = lastData != null ? lastData : read();
//        saveData();
//        return result;
//        //instruction.execute();
//        // int value = WordNumber.createValue(ld.getSource().read().intValue());
//        // return data;
//      }
//    }

    int result = lastData != -1 ? lastData : read();
    return result;
  }

  public void accept(InstructionVisitor instructionVisitor) {
    //instruction.accept(instructionVisitor);
    instructionVisitor.visitRegister(this);
  }

  @Override
  public int getAddress() {
    return address;
  }

  @Override
  public Scope getScope() {
    return scope;
  }

  public VirtualRegisterVersionHandler getVersionHandler() {
    return versionHandler;
  }

  public boolean isComposed2() {


    return virtualComposed16BitRegister != null;
  }

  @Override
  public boolean isComposed() {
    boolean[] isReturnValue2 = {true};

    if (instruction instanceof RepeatingInstruction
        || instruction instanceof BitOperation
//        || instruction instanceof Push
        || instruction instanceof In
        || instruction instanceof Ld
        || instruction instanceof ParameterizedBinaryAluInstruction
        || instruction instanceof ParameterizedUnaryAluInstruction
        || instruction instanceof VirtualAssignmentInstruction)
      return false;

    if (instruction instanceof Push) {
      System.out.println("dsgsddgs");
    }

//    instruction.accept(new RegisterFinderInstructionVisitor() {
//      public boolean visitRegister(Register register) {
//        if (isSource) {
//          if (virtualComposed16BitRegister == register)
//            isReturnValue2[0] = isComposed2();
//        }
//        return isReturnValue2[0];
//      }
//    });
    return isReturnValue2[0];
  }

  @Override
  public void setComposed(boolean composed) {
    isComposed = composed;
  }
}
