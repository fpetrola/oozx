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

package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.bytecode.generators.helpers.WriteArrayVariable;
import com.fpetrola.z80.opcodes.references.MemoryAccessOpcodeReference;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.transformations.*;
import org.cojen.maker.Label;
import org.cojen.maker.Variable;

import java.util.*;
import java.util.function.Function;

public class OpcodeReferenceVisitor<T extends WordNumber> implements InstructionVisitor<T> {
  private Object result;
  private boolean isTarget;
  private RoutineBytecodeGenerator routineByteCodeGenerator;

  public void setInitializerFactory(Function initializerFactory) {
    this.initializerFactory = initializerFactory;
  }

  private Function<VirtualRegister<T>, Object> initializerFactory;

  public OpcodeReferenceVisitor(boolean isTarget, RoutineBytecodeGenerator routineByteCodeGenerator) {
    this.isTarget = isTarget;
    this.routineByteCodeGenerator = routineByteCodeGenerator;

    initializerFactory = new Function<>() {
      public Object apply(VirtualRegister<T> virtualRegister) {
        List<VirtualRegister<T>> previousVersions = virtualRegister.getPreviousVersions();
        if (previousVersions.isEmpty()) {
          return virtualRegister.read().intValue();
        } else {
          VirtualRegister<T> firstPrevious = previousVersions.get(0);
          if (!virtualRegister.hasNoPrevious()) {
            if (firstPrevious.isMixRegister()) {
              VirtualComposed16BitRegister<T> virtualComposed16BitRegister = (VirtualComposed16BitRegister) firstPrevious;
              Variable o = (Variable) processValue(initializerFactory, virtualComposed16BitRegister.getHigh());
              Variable o2 = (Variable) processValue(initializerFactory, virtualComposed16BitRegister.getLow());
              return o.shl(8).or(o2);
            } else {
              if (previousVersions.stream().noneMatch(r -> routineByteCodeGenerator.variableExists(r))) {
                for (Map.Entry<String, VirtualRegister> entry : routineByteCodeGenerator.registerByVariable.entrySet()) {
                  if (entry.getValue() instanceof VirtualComposed16BitRegister<?> virtualComposed16BitRegister) {
                    Variable existingVariable = routineByteCodeGenerator.getExistingVariable(virtualComposed16BitRegister);
                    if (virtualComposed16BitRegister.getLow() == firstPrevious) return existingVariable.and(0xFF);
                    else if (virtualComposed16BitRegister.getHigh() == firstPrevious) return existingVariable.shr(8);
                  }
                }
                return routineByteCodeGenerator.initial;
              }
            }
          }

          return processValue(this, firstPrevious);
        }
      }
    };
  }

  @Override
  public void visitOpcodeReference(OpcodeReference opcodeReference) {
    result = ((WordNumber) opcodeReference.read()).intValue();
  }

  public Object getResult() {
    return result;
  }

  public boolean visitRegister(Register register) {
    if (register instanceof VirtualRegister<?> virtualRegister)
      result = processValue(initializerFactory, (VirtualRegister<T>) virtualRegister);
    else
      System.out.println("Cannot virtualize: " + register.getName());
    return true;
  }

  protected Object processValue(Function<VirtualRegister<T>, Object> initializerFactory, VirtualRegister<T> virtualRegister) {
    Variable variable;
    VirtualRegister top = RoutineBytecodeGenerator.getTop(virtualRegister);
    boolean b = !(top instanceof InitialVirtualRegister);
    if (!routineByteCodeGenerator.variableExists(top)) {
      if (!b) {
        Variable[] variable2 = new Variable[1];

        //byteCodeGenerator.createVariable(virtualRegister);
        Runnable runnable = () -> variable2[0] = routineByteCodeGenerator.getVariable(virtualRegister, () -> solveInitializer(initializerFactory, virtualRegister));
//        runnable.run();
        Label branchLabel = routineByteCodeGenerator.getBranchLabel(top.getScope().start);
        Label insert = branchLabel.insert(runnable);
        variable = variable2[0];
      } else
        variable = routineByteCodeGenerator.getVariable(virtualRegister, () -> solveInitializer(initializerFactory, virtualRegister));
    } else {
      variable = routineByteCodeGenerator.getExistingVariable(virtualRegister);
      if (true || b) {
        if (virtualRegister.isInitialized()) {
          Object value = solveInitializer(initializerFactory, virtualRegister);
          if (value != null) variable.set(value);
        }
      }
    }

    return variable;
  }

  private Object solveInitializer(Function<VirtualRegister<T>, Object> initializerFactory, VirtualRegister<T> virtualRegister) {
    Object initializer;
    if (virtualRegister.usesMultipleVersions()) {
      if (!routineByteCodeGenerator.variableExists(virtualRegister)) {
        VirtualRegister<T> parentPreviousVersion = virtualRegister.adjustRegisterScope();
        initializer = initializerFactory.apply(parentPreviousVersion);
        Object finalInitializer = initializer;
        routineByteCodeGenerator.getVariable(virtualRegister, () -> finalInitializer);
        virtualRegister.getPreviousVersions().forEach(p -> routineByteCodeGenerator.commonRegisters.put(p, virtualRegister));
      }

      initializer = routineByteCodeGenerator.getExistingVariable(virtualRegister);
    } else {
      initializer = initializerFactory.apply(virtualRegister);
    }
    return initializer;
  }

  public void visitConstantOpcodeReference(ConstantOpcodeReference<T> constantOpcodeReference) {
    result = constantOpcodeReference.read().intValue();
  }

  public void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference<T> memoryAccessOpcodeReference) {
    Variable memoryField = routineByteCodeGenerator.getField("memory");
    int o = memoryAccessOpcodeReference.getC().read().intValue();
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> o, "");
    else result = getFromMemory(o);
  }

  public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference<T> memoryPlusRegister8BitReference) {
    Register target = (Register) memoryPlusRegister8BitReference.getTarget();
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(isTarget, routineByteCodeGenerator);
    target.accept(opcodeReferenceVisitor);

    Variable variable = (Variable) opcodeReferenceVisitor.getResult();

    byte value = memoryPlusRegister8BitReference.fetchRelative();
    Variable variablePlusDelta = value > 0 ? variable.add(value) : variable;
    if (isTarget)
      result = new WriteArrayVariable(routineByteCodeGenerator, () -> variablePlusDelta, "");
    else {
      result = getFromMemory(variablePlusDelta);
    }
  }


  public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
    Object variable;
    if (indirectMemory8BitReference.getTarget() instanceof Memory16BitReference<?> memory16BitReference) {
      variable = memory16BitReference.read().intValue();
    } else {
      Register target = (Register) indirectMemory8BitReference.getTarget();
      OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
      target.accept(opcodeReferenceVisitor);

      variable = opcodeReferenceVisitor.getResult();
    }
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> variable, "");
    else {
      result = getFromMemory(variable);
    }
  }

  private Variable getFromMemory(Object variable) {
    return routineByteCodeGenerator.getVariableFromMemory(variable, "");
  }


  @Override
  public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
    Object variable;
    if (indirectMemory16BitReference.target instanceof Memory16BitReference<?> memory16BitReference) {
      variable = memory16BitReference.read().intValue();
    } else {
      Register target = (Register) indirectMemory16BitReference.target;
      OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
      target.accept(opcodeReferenceVisitor);

      variable = opcodeReferenceVisitor.getResult();
    }
    if (isTarget) result = new WriteArrayVariable(routineByteCodeGenerator, () -> variable, "16");
    else {
      result = getFromMemory16(variable);
    }
  }

  private Variable getFromMemory16(Object variable) {
    return routineByteCodeGenerator.getVariableFromMemory(variable, "16");
  }

  @Override
  public void visitImmutableOpcodeReference(ImmutableOpcodeReference immutableOpcodeReference) {
    result = ((T) immutableOpcodeReference.read()).intValue();
  }

  public <T> Variable process(VirtualRegister<T> register) {
    register.accept(this);
    return (Variable) getResult();
  }
}
