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

import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TransformerInstructionExecutor<T extends WordNumber> implements InstructionExecutor<T> {
  private final Register<T> pc;
  private InstructionExecutor<T> instructionExecutor;
  private boolean noRepeat;

  public TransformerInstructionExecutor(Register<T> pc, InstructionExecutor<T> instructionExecutor, boolean noRepeat, InstructionTransformer<T> instructionTransformer) {
    this.pc = pc;
    this.instructionExecutor = instructionExecutor;
    this.noRepeat = noRepeat;
    this.instructionTransformer = instructionTransformer;
  }

  private InstructionTransformer<T> instructionTransformer;
  private InstructionActionExecutor<T> resetter = new InstructionActionExecutor<>(r -> r.reset());
  public Map<Integer, Instruction<T>> clonedInstructions = new HashMap<>();
  public Map<Integer, Instruction<T>> instructions = new HashMap<>();
  public List<Instruction<T>> executed = new ArrayList<>();

  private Instruction<T> processTargetSource(Instruction<T> instruction, Instruction<T> existentCloned) {
    instructionTransformer.virtualRegisterFactory.getRegisterNameBuilder().setCurrentAddress(getAddressOf(instruction));

    Instruction<T> baseInstruction = DefaultInstructionFetcher.getBaseInstruction(instruction);
    DefaultInstructionFetcher.processToBase(instruction);
    instructionTransformer.setCurrentInstruction(baseInstruction);
    Instruction<T> cloned;
    cloned = instructionTransformer.clone(baseInstruction);
    if (existentCloned == null) {
      clonedInstructions.put(pc.read().intValue(), cloned);
    } else
      cloned = existentCloned;

    instructions.put(pc.read().intValue(), baseInstruction);

    resetter.executeAction(cloned);

    return cloned;
  }

  @Override
  public Instruction<T> execute(Instruction<T> instruction) {
    Instruction<T> existentCloned = clonedInstructions.get(pc.read().intValue());
    Instruction<T> cloned = processTargetSource(instruction, existentCloned);

//    if (pc.read().intValue() == 34480)
//      System.out.print("");
    //System.err.println(pc.read() + ":- " + cloned);

    //if (isConcreteInstruction(cloned) || existentCloned != null)
      instructionExecutor.execute(cloned);

    if (noRepeat && cloned instanceof RepeatingInstruction repeatingInstruction)
      repeatingInstruction.setNextPC(null);

    if (executed.isEmpty() || executed.get(executed.size() - 1) != cloned)
      executed.add(cloned);

    return cloned;
  }

  private boolean isConcreteInstruction(Instruction<T> cloned) {
    boolean[] b = new boolean[]{isConcrete(cloned)};

    InstructionVisitor<WordNumber> instructionVisitor = new InstructionVisitor<>() {
      public void visitingSource(ImmutableOpcodeReference source, TargetSourceInstruction targetSourceInstruction) {
        source.accept(this);
      }

      public void visitingTarget(OpcodeReference target, TargetInstruction targetInstruction) {
        target.accept(this);
      }

      public void visitIndirectMemory8BitReference(IndirectMemory8BitReference indirectMemory8BitReference) {
        b[0] = true;
      }

      public void visitIndirectMemory16BitReference(IndirectMemory16BitReference indirectMemory16BitReference) {
        b[0] = true;
      }

      public void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference<WordNumber> memoryAccessOpcodeReference) {
        b[0] = true;
      }

      public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference<WordNumber> memoryPlusRegister8BitReference) {
        b[0] = true;
      }

      public void visitRepeatingInstruction(RepeatingInstruction tRepeatingInstruction) {
        b[0] = true;
      }

      public void visitBlockInstruction(BlockInstruction blockInstruction) {
        b[0] = true;
      }

      @Override
      public void visitPush(Push push) {
        b[0] = true;
      }

      @Override
      public void visitingPop(Pop pop) {
        b[0] = true;
      }

      @Override
      public boolean visitRegister(Register register) {
        if (register.getName().equals(RegisterName.SP.name()))
          b[0] = true;
        return false;
      }

      @Override
      public void visitEx(Ex ex) {
        ex.getSource().accept(this);
        ex.getTarget().accept(this);
      }
    };
    cloned.accept(instructionVisitor);

    return b[0];
  }

  private boolean isConcrete(Instruction<T> cloned) {
    return Stream.of(ConditionalInstruction.class, RST.class, In.class, Out.class, EI.class, DI.class)
        .anyMatch(c -> c.isAssignableFrom(cloned.getClass()));
  }

  @Override
  public boolean isExecuting(Instruction<T> instruction) {
    return instructionExecutor.isExecuting(instruction);
  }

  private int getAddressOf(Instruction instruction) {
    return pc.read().intValue();
  }

  @Override
  public void reset() {
    clonedInstructions.clear();
    executed.clear();
  }
}
