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

import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.transformations.IVirtual8BitsRegister;
import com.fpetrola.z80.transformations.VirtualComposed16BitRegister;
import com.fpetrola.z80.transformations.VirtualRegister;
import org.cojen.maker.Variable;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.fpetrola.z80.bytecode.generators.RoutineBytecodeGenerator.getRealVariable;
import static com.fpetrola.z80.bytecode.generators.RoutineBytecodeGenerator.getRegisterName;

public class VariableHandlingInstructionVisitor implements InstructionVisitor<WordNumber> {
  protected Function createInitializer;
  private BiConsumer<Object, Variable> variableAction;
  protected Object sourceVariable;
  protected Variable targetVariable;
  private OpcodeReference target;
  private ImmutableOpcodeReference source;
  private RoutineBytecodeGenerator routineByteCodeGenerator;

  public VariableHandlingInstructionVisitor(BiConsumer<Object, Variable> variableAction, RoutineBytecodeGenerator routineByteCodeGenerator1) {
    this.variableAction = variableAction;
    routineByteCodeGenerator = routineByteCodeGenerator1;
  }

  public void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {
    this.target = target;
    OpcodeReferenceVisitor instructionVisitor = new OpcodeReferenceVisitor(true, routineByteCodeGenerator);
    if (createInitializer != null) instructionVisitor.setInitializerFactory(createInitializer);
    target.accept(instructionVisitor);
    targetVariable = (Variable) instructionVisitor.getResult();
  }

  public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
    this.source = source;
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
    if (createInitializer != null) opcodeReferenceVisitor.setInitializerFactory(createInitializer);

    source.accept(opcodeReferenceVisitor);
    sourceVariable = opcodeReferenceVisitor.getResult();

    int i = routineByteCodeGenerator.bytecodeGenerationContext.pc.read().intValue();
    Optional<Integer> mutantCode = SymbolicExecutionAdapter.mutantAddress.stream()
        .filter(m -> m >= i && m <= routineByteCodeGenerator.currentInstruction.getLength() + i).findFirst();
    if (mutantCode.isPresent()) {
      sourceVariable = routineByteCodeGenerator.getField("mem").aget(mutantCode.get());
    }
  }

  public void visitingFlag(Register<WordNumber> flag, DefaultTargetFlagInstruction targetSourceInstruction) {
//    OpcodeReferenceVisitor instructionVisitor = new OpcodeReferenceVisitor(true, byteCodeGenerator);
//    if (createInitializer != null) instructionVisitor.setCreateInitializer(createInitializer);
//    flag.accept(instructionVisitor);
  }

  public void visitingTargetSourceInstruction(TargetSourceInstruction targetSourceInstruction) {
    createResult();
  }

  @Override
  public void visitingBitOperation(BitOperation tBitOperation) {
    variableAction.accept(sourceVariable, (Variable) targetVariable);
  }

  private void createResult() {
    if (targetVariable instanceof Variable variable) {
      variableAction.accept(getRealVariable(sourceVariable), variable);
      Optional<Map.Entry<VirtualRegister<?>, VirtualRegister<?>>> fromCommonRegisters = getFromCommonRegisters(variable, routineByteCodeGenerator);
      VirtualRegister<?> s = fromCommonRegisters.isEmpty() ? null : fromCommonRegisters.get().getValue();

      if (s != null) {
        if (!s.getName().equals(variable.name())) {
          routineByteCodeGenerator.getExistingVariable(s).set(variable);
        }
      } else {
        routineByteCodeGenerator.commonRegisters.entrySet().stream().forEach(e -> {
          if (e.getKey() instanceof VirtualComposed16BitRegister<?> virtualComposed16BitRegister && virtualComposed16BitRegister.isMixRegister()) {
            boolean contains = routineByteCodeGenerator.getExistingVariable(virtualComposed16BitRegister.getLow()) == variable;
            contains |= routineByteCodeGenerator.getExistingVariable(virtualComposed16BitRegister.getHigh()) == variable;
            if (contains) {
              Variable commonHigh = get8BitCommon(virtualComposed16BitRegister.getHigh());
              Variable commonLow = get8BitCommon(virtualComposed16BitRegister.getLow());
              Variable variable1;

              if (commonHigh == null) variable1 = commonLow.and(0xFF);
              else variable1 = commonHigh.shl(8).or(commonLow.and(0xFF));

              routineByteCodeGenerator.getExistingVariable(e.getValue()).set(variable1);
            }
          }
        });
      }
    }
  }

  public static Optional<Map.Entry<VirtualRegister<?>, VirtualRegister<?>>> getFromCommonRegisters(Variable variable, RoutineBytecodeGenerator routineByteCodeGenerator) {
    return routineByteCodeGenerator.commonRegisters.entrySet().stream().filter(e -> getRegisterName(e.getKey()).equals(variable.name())).findFirst();
  }

  private Variable get8BitCommon(IVirtual8BitsRegister<?> virtualRegister) {
    VirtualComposed16BitRegister<?> virtualComposed16BitRegister = virtualRegister.getVirtualComposed16BitRegister();
    if (virtualComposed16BitRegister.isMixRegister()) return routineByteCodeGenerator.getExistingVariable(virtualRegister);
    return routineByteCodeGenerator.getExistingVariable(virtualComposed16BitRegister);
  }

  public void visitingTargetInstruction(TargetInstruction targetInstruction) {
    createResult();
  }
}
