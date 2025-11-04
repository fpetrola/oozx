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
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;
import java.util.function.Consumer;

//TODO: remember reset
public class VirtualRegisterFactory {
  private final InstructionExecutor instructionExecutor;
  private final RegisterNameBuilder registerNameBuilder;
  private final BlocksManager blocksManager;
  private final ArrayListValuedHashMap<Register, VirtualRegister> virtualRegisters = new ArrayListValuedHashMap<>();
  public Map<Register, VirtualRegister> lastVirtualRegisters = new HashMap<>();
  public Map<Register, Integer> lastValues = new HashMap<>();
  public Map<Register, VirtualRegisterVersionHandler> versionHandlers = new HashMap<>();
  private final List<Runnable> actions = new ArrayList<>();

  public VirtualRegisterFactory(InstructionExecutor instructionExecutor, RegisterNameBuilder registerNameBuilder, BlocksManager blocksManager) {
    this.instructionExecutor = instructionExecutor;
    this.registerNameBuilder = registerNameBuilder;
    this.blocksManager = blocksManager;
  }

  public Register createVirtualRegister(Instruction instruction, Register register, VirtualFetcher virtualFetcher, Instruction currentInstruction1) {
    if (register.getName().equals("I") || register.getName().equals("R") || register.getName().equals("SP") || register.getName().equals("PC"))
      return register;
    else if (register instanceof RegisterPair registerPair)
      return create16VirtualRegister(instruction, registerPair, virtualFetcher);
    else
      return createVirtual8BitsRegister(register, instruction, virtualFetcher, currentInstruction1);
  }

  private IVirtual8BitsRegister createVirtual8BitsRegister(Register register, Instruction targetInstruction, VirtualFetcher virtualFetcher, Instruction currentInstruction1) {
    Consumer<Integer> dataConsumer = (v) -> lastValues.put(register, v);
    return (IVirtual8BitsRegister) buildVirtualRegister(targetInstruction, register, (virtualRegisterName, previousVersion, currentAddress, versionHandler) -> new Virtual8BitsRegister(currentAddress, instructionExecutor, virtualRegisterName, targetInstruction, (IVirtual8BitsRegister) previousVersion, virtualFetcher, dataConsumer, versionHandler, blocksManager, currentInstruction1));
  }

  private VirtualRegister create16VirtualRegister(Instruction targetInstruction, RegisterPair registerPair, VirtualFetcher virtualFetcher) {
    IVirtual8BitsRegister virtualH = createVirtual8BitsRegister(registerPair.getHigh(), targetInstruction, virtualFetcher, targetInstruction);
    IVirtual8BitsRegister virtualL = createVirtual8BitsRegister(registerPair.getLow(), targetInstruction, virtualFetcher, targetInstruction);
    return buildVirtualRegister(targetInstruction, registerPair, (virtualRegisterName, supplier, currentAddress, versionHandler) -> new VirtualComposed16BitRegister(currentAddress, virtualRegisterName, virtualH, virtualL, versionHandler, true, blocksManager));
  }

  private VirtualRegister buildVirtualRegister(Instruction targetInstruction, Register register, VirtualRegisterBuilder registerBuilder) {
    VirtualRegister previousVersion = lastVirtualRegisters.get(register);
    VirtualRegisterVersionHandler versionHandler = getVersionHandlerFor(register);

    boolean registerAssignment = targetInstruction instanceof Ld ld && ld.getTarget() == register;
    registerAssignment = false;
    VirtualRegister previousVersion1;
    if (previousVersion == null) {
      previousVersion1 = new InitialVirtualRegister(register, versionHandler, blocksManager);
      previousVersion1.getVersionHandler().addVersion(previousVersion1);
    } else if (registerAssignment) {
      previousVersion1 = null;
    } else {
      previousVersion1 = previousVersion;
    }

    VirtualRegister virtualRegister = registerBuilder.build(
        registerNameBuilder.createVirtualRegisterName(register),
        previousVersion1,
        registerNameBuilder.getCurrentAddress(),
        versionHandler);

    Optional<VirtualRegister> found = Optional.empty();
    for (VirtualRegister r : virtualRegisters.get(register)) {
      if (virtualRegister.getName().startsWith(r.getName() + "%")) {
        found = Optional.of(r);
        break;
      }
    }

    VirtualRegisterVersionHandler finalVersionHandler = versionHandler;
    VirtualRegister result = found.orElseGet(() -> {
      finalVersionHandler.addVersion(virtualRegister);
      virtualRegisters.put(register, virtualRegister);
      return virtualRegister;
    });

    if (result != virtualRegister && result instanceof IVirtual8BitsRegister multiEntryRegister) {
      IVirtual8BitsRegister currentPreviousVersion = ((IVirtual8BitsRegister) virtualRegister).getCurrentPreviousVersion();
      if (currentPreviousVersion != null) {
        // currentPreviousVersion.read();  //FIXME: revisar esto cuando ejecuta simbolico
        multiEntryRegister.addPreviousVersion(currentPreviousVersion);
      }
    }

    actions.add(() -> lastVirtualRegisters.put(register, result));
    return result;
  }

  private VirtualRegisterVersionHandler getVersionHandlerFor(Register register) {
    VirtualRegisterVersionHandler versionHandler = versionHandlers.get(register);
    if (versionHandler == null)
      versionHandlers.put(register, versionHandler = new VirtualRegisterVersionHandler());
    return versionHandler;
  }

  public RegisterNameBuilder getRegisterNameBuilder() {
    return registerNameBuilder;
  }

  public void initTransaction() {
    actions.clear();
  }

  public void endTransaction() {
    actions.forEach(a -> a.run());
  }

  public interface VirtualRegisterBuilder {
    VirtualRegister build(String virtualRegisterName, VirtualRegister previousVersion, int currentAddress, VirtualRegisterVersionHandler versionHandler);
  }

  public void reset() {
    virtualRegisters.clear();
    lastVirtualRegisters.clear();
    lastValues.clear();
    versionHandlers.clear();
    actions.clear();
    registerNameBuilder.reset();
  }

}




















