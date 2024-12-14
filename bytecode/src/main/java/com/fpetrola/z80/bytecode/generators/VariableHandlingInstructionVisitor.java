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
import com.fpetrola.z80.instructions.impl.LdAR;
import com.fpetrola.z80.instructions.types.BitOperation;
import com.fpetrola.z80.instructions.types.DefaultTargetFlagInstruction;
import com.fpetrola.z80.instructions.types.TargetInstruction;
import com.fpetrola.z80.instructions.types.TargetSourceInstruction;
import com.fpetrola.z80.opcodes.references.ImmutableOpcodeReference;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import org.cojen.maker.Variable;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.fpetrola.z80.bytecode.generators.RoutineBytecodeGenerator.getRealVariable;

public class VariableHandlingInstructionVisitor implements InstructionVisitor<WordNumber, WordNumber> {
  private final BiConsumer<Object, Variable> variableAction;
  protected Object sourceVariable;
  protected Variable targetVariable;
  private final RoutineBytecodeGenerator routineByteCodeGenerator;

  public VariableHandlingInstructionVisitor(BiConsumer<Object, Variable> variableAction, RoutineBytecodeGenerator routineByteCodeGenerator1) {
    this.variableAction = variableAction;
    routineByteCodeGenerator = routineByteCodeGenerator1;
  }

  @Override
  public boolean visitLdAR(LdAR tLdAR) {
    return true;
  }

  public void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {
    OpcodeReferenceVisitor instructionVisitor = new OpcodeReferenceVisitor(true, routineByteCodeGenerator);
    target.accept(instructionVisitor);
    targetVariable = (Variable) instructionVisitor.getResult();
  }

  public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
    OpcodeReferenceVisitor opcodeReferenceVisitor = new OpcodeReferenceVisitor(false, routineByteCodeGenerator);
    source.accept(opcodeReferenceVisitor);
    sourceVariable = opcodeReferenceVisitor.getResult();

    int i = routineByteCodeGenerator.context.pc.read().intValue();
    Set<Integer> mutantAddress = (Set<Integer>) routineByteCodeGenerator.context.symbolicExecutionAdapter.getMutantAddress();
    Optional<Integer> mutantCode = mutantAddress.stream()
        .filter(m -> m >= i && m < routineByteCodeGenerator.currentInstruction.getLength() + i).findFirst();
    if (mutantCode.isPresent()) {
      routineByteCodeGenerator.mm.invoke("executeMutantCode", mutantCode.get());
//      sourceVariable = routineByteCodeGenerator.getField("mem").aget(mutantCode.get());
    }
  }

  public void visitingFlag(Register<WordNumber> flag, DefaultTargetFlagInstruction targetSourceInstruction) {
  }

  public void visitingTargetSourceInstruction(TargetSourceInstruction targetSourceInstruction) {
    createResult();
  }

  @Override
  public void visitingBitOperation(BitOperation tBitOperation) {
    variableAction.accept(sourceVariable, targetVariable);
  }

  private void createResult() {
    if (targetVariable instanceof Variable) {
      Variable variable = targetVariable;
      variableAction.accept(getRealVariable(sourceVariable), variable);
      createResult(variable);
    }
  }

  protected void createResult(Variable variable) {

  }

  public void visitingTargetInstruction(TargetInstruction targetInstruction) {
    createResult();
  }
}
