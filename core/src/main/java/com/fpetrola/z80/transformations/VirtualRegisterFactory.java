/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.instructions.impl.Ld;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterPair;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.*;
import java.util.function.Consumer;

public class VirtualRegisterFactory<T extends WordNumber> {
  private final InstructionExecutor<T> instructionExecutor;
  private final RegisterNameBuilder registerNameBuilder;
  private final ArrayListValuedHashMap<Register<T>, VirtualRegister<T>> virtualRegisters = new ArrayListValuedHashMap<>();
  public Map<Register<T>, VirtualRegister<T>> lastVirtualRegisters = new HashMap<>();
  public Map<Register<T>, T> lastValues = new HashMap<>();
  public Map<Register<T>, VirtualRegisterVersionHandler> versionHandlers = new HashMap<>();
  private List<Runnable> actions = new ArrayList<>();

  public VirtualRegisterFactory(InstructionExecutor<T> instructionExecutor, RegisterNameBuilder registerNameBuilder) {
    this.instructionExecutor = instructionExecutor;
    this.registerNameBuilder = registerNameBuilder;
  }

  public Register<T> createVirtualRegister(Instruction<T> instruction, Register<T> register, VirtualFetcher<T> virtualFetcher) {
    if (register.getName().equals("I") || register.getName().equals("R") || register.getName().equals("SP") || register.getName().equals("PC"))
      return register;
    else if (register instanceof RegisterPair<T> registerPair)
      return create16VirtualRegister(instruction, registerPair, virtualFetcher);
    else
      return createVirtual8BitsRegister(register, instruction, virtualFetcher);
  }

  private IVirtual8BitsRegister<T> createVirtual8BitsRegister(Register<T> register, Instruction<T> targetInstruction, VirtualFetcher<T> virtualFetcher) {
    Consumer<T> dataConsumer = (v) -> lastValues.put(register, v);
    return (IVirtual8BitsRegister<T>) buildVirtualRegister(targetInstruction, register, (virtualRegisterName, previousVersion, currentAddress, versionHandler) -> new Virtual8BitsRegister<>(currentAddress, instructionExecutor, virtualRegisterName, targetInstruction, (IVirtual8BitsRegister<T>) previousVersion, virtualFetcher, dataConsumer, versionHandler));
  }

  private VirtualRegister<T> create16VirtualRegister(Instruction<T> targetInstruction, RegisterPair<T> registerPair, VirtualFetcher<T> virtualFetcher) {
    IVirtual8BitsRegister<T> virtualH = createVirtual8BitsRegister(registerPair.getHigh(), targetInstruction, virtualFetcher);
    IVirtual8BitsRegister<T> virtualL = createVirtual8BitsRegister(registerPair.getLow(), targetInstruction, virtualFetcher);
    return buildVirtualRegister(targetInstruction, registerPair, (virtualRegisterName, supplier, currentAddress, versionHandler) -> new VirtualComposed16BitRegister<>(currentAddress, virtualRegisterName, virtualH, virtualL, versionHandler, true));
  }

  private VirtualRegister<T> buildVirtualRegister(Instruction<T> targetInstruction, Register<T> register, VirtualRegisterBuilder<T> registerBuilder) {
    VirtualRegister<T> previousVersion = lastVirtualRegisters.get(register);
    VirtualRegisterVersionHandler versionHandler = getVersionHandlerFor(register);

    boolean registerAssignment = targetInstruction instanceof Ld<T> ld && ld.getTarget() == register;
    registerAssignment= false;
    VirtualRegister<T> previousVersion1;
    if (previousVersion == null) {
      previousVersion1 = new InitialVirtualRegister(register, versionHandler);
      previousVersion1.getVersionHandler().addVersion(previousVersion1);
    } else if (registerAssignment) {
      previousVersion1 = null;
    } else {
      previousVersion1 = previousVersion;
    }

    VirtualRegister<T> virtualRegister = registerBuilder.build(
        registerNameBuilder.createVirtualRegisterName(register),
        previousVersion1,
        registerNameBuilder.getCurrentAddress(),
        versionHandler);

    Optional<VirtualRegister<T>> found = Optional.empty();
    for (VirtualRegister<T> r : virtualRegisters.get(register)) {
      if (virtualRegister.getName().startsWith(r.getName() + "%")) {
        found = Optional.of(r);
        break;
      }
    }

    VirtualRegisterVersionHandler finalVersionHandler = versionHandler;
    VirtualRegister<T> result = found.orElseGet(() -> {
      finalVersionHandler.addVersion(virtualRegister);
      virtualRegisters.put(register, virtualRegister);
      return virtualRegister;
    });

    if (result != virtualRegister && result instanceof IVirtual8BitsRegister<T> multiEntryRegister) {
      IVirtual8BitsRegister<T> currentPreviousVersion = ((IVirtual8BitsRegister<T>) virtualRegister).getCurrentPreviousVersion();
      if (currentPreviousVersion != null) {
       // currentPreviousVersion.read();  //FIXME: revisar esto cuando ejecuta simbolico
        multiEntryRegister.addPreviousVersion(currentPreviousVersion);
      }
    }

    actions.add(() -> lastVirtualRegisters.put(register, result));
    return result;
  }

  private VirtualRegisterVersionHandler getVersionHandlerFor(Register<T> register) {
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

  public interface VirtualRegisterBuilder<T extends WordNumber> {
    VirtualRegister<T> build(String virtualRegisterName, VirtualRegister<T> previousVersion, int currentAddress, VirtualRegisterVersionHandler versionHandler);
  }

}




















