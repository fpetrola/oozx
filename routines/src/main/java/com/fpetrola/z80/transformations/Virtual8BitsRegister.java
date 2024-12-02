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
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Plain8BitRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Virtual8BitsRegister<T extends WordNumber> extends Plain8BitRegister<T> implements IVirtual8BitsRegister<T> {
  private final int address;
  private final InstructionExecutor instructionExecutor;
  public Instruction<T> instruction;
  private final VirtualFetcher<T> virtualFetcher;
  private final List<VirtualRegister<T>> previousVersions = new ArrayList<>();
  protected T lastData;
  protected int reads;
  public IVirtual8BitsRegister<T> lastVersionRead;
  private final Consumer<T> dataConsumer;
  private final VirtualRegisterVersionHandler versionHandler;

  @Override
  public BlocksManager getBlocksManager() {
    return blocksManager;
  }

  private final BlocksManager blocksManager;
  private final List<VirtualRegister<T>> dependants = new ArrayList<>();
  private final Scope scope;
  public VirtualComposed16BitRegister<T> virtualComposed16BitRegister;

  private boolean isComposed;

  @Override
  public boolean isInitialized() {
    return instruction instanceof Ld;
  }

  public Virtual8BitsRegister(int address, InstructionExecutor instructionExecutor, String name, Instruction<T> instruction,
                              IVirtual8BitsRegister<T> previousVersion, VirtualFetcher<T> virtualFetcher, Consumer<T> dataConsumer,
                              VirtualRegisterVersionHandler versionHandler, BlocksManager blocksManager) {
    super(name);
    this.address = address;
    this.instructionExecutor = instructionExecutor;
    this.instruction = instruction;
    this.virtualFetcher = virtualFetcher;
    this.dataConsumer = dataConsumer;
    this.versionHandler = versionHandler;
    this.blocksManager = blocksManager;

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

  public IVirtual8BitsRegister<T> getCurrentPreviousVersion() {
    return previousVersions.isEmpty() ? null : (IVirtual8BitsRegister<T>) previousVersions.get(previousVersions.size() - 1);
  }

  public T read() {
    T t = virtualFetcher.readFromVirtual(() -> instructionExecutor.isExecuting(instruction), () -> instructionExecutor.execute(instruction), () -> data, () -> (lastVersionRead = getCurrentPreviousVersion()).readPrevious());
    if (data == t)
      reads++;
    lastData = null;
    data = t;

    dataConsumer.accept(data);

    return t;
  }

  @Override
  public void write(T value) {
    super.write(value);
    lastData = null;
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
    data = null;
    reads = 0;
  }

  @Override
  public List<VirtualRegister<T>> getPreviousVersions() {
    return previousVersions;
  }

  public void addPreviousVersion(IVirtual8BitsRegister previousVersion) {
    previousVersion.addDependant(this);
    previousVersions.remove(previousVersion);
    previousVersions.add(previousVersion);
    previousVersion.saveData();
  }

  @Override
  public void set16BitsRegister(VirtualComposed16BitRegister<T> virtualComposed16BitRegister) {
    if (this.virtualComposed16BitRegister == null)
      this.virtualComposed16BitRegister = virtualComposed16BitRegister;
  }

  @Override
  public List<VirtualRegister<T>> getDependants() {
    return dependants;
  }

  @Override
  public VirtualComposed16BitRegister<T> getVirtualComposed16BitRegister() {
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

  public T readPrevious() {
    Helper.breakInStackOverflow();

//    if (data == null && lastData == null && reads == 0) {
//      for (VirtualRegister<T> v1 : previousVersions) {
//        if (v1 != this) {
//          if (v1 instanceof MyVirtualRegister<T>)
//            return v1.read();
//          else
//            return ((Virtual8BitsRegister<T>) v1).readPrevious();
//        }
//      }
//    }

//    if (instruction instanceof Ld<T> || instruction instanceof In<T>) {
//      TargetSourceInstruction<T, ?> tt = (TargetSourceInstruction) instruction;
//      if ((tt.getTarget() instanceof Register<T>) || tt.getTarget() instanceof IndirectMemory16BitReference<T> indirectMemory16BitReference && indirectMemory16BitReference.target instanceof Register<T>) {
//        T result = lastData != null ? lastData : read();
//        saveData();
//        return result;
//        //instruction.execute();
//        // T value = WordNumber.createValue(ld.getSource().read().intValue());
//        // return data;
//      }
//    }

    T result = lastData != null ? lastData : read();
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

  @Override
  public boolean isComposed2() {


    return virtualComposed16BitRegister != null;
  }

  @Override
  public boolean isComposed() {
    boolean[] isReturnValue2 = {true};

    if (instruction instanceof RepeatingInstruction<T>
        || instruction instanceof BitOperation<T>
//        || instruction instanceof Push<T>
        || instruction instanceof In<T>
        || instruction instanceof Ld<T>
        || instruction instanceof ParameterizedBinaryAluInstruction<T>
        || instruction instanceof ParameterizedUnaryAluInstruction<T>
        || instruction instanceof VirtualAssignmentInstruction<T>)
      return false;

    if (instruction instanceof Push<T>) {
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
