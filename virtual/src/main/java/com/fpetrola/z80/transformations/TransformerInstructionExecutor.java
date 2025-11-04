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

import com.fpetrola.z80.cpu.InstructionExecutor;
import com.fpetrola.z80.base.InstructionVisitor;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.*;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.spy.AbstractInstructionSpy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class TransformerInstructionExecutor implements InstructionExecutor {
  private final Register pc;
  public final InstructionExecutor instructionExecutor;
  private final boolean noRepeat;

  public TransformerInstructionExecutor(Register pc, InstructionExecutor instructionExecutor, boolean noRepeat, InstructionTransformer instructionTransformer) {
    this.pc = pc;
    this.instructionExecutor = instructionExecutor;
    this.noRepeat = noRepeat;
    this.instructionTransformer = instructionTransformer;
  }

  private final InstructionTransformer instructionTransformer;
  private final InstructionActionExecutor resetter = new InstructionActionExecutor(r -> r.reset());
  private Map<java.lang.Integer, Instruction> clonedInstructions = new HashMap<>();
  public Map<java.lang.Integer, Instruction> instructions = new HashMap<>();
  public List<Instruction> executed = new ArrayList<>();

  @Override
  public Instruction getInstructionAt(int address) {
    return clonedInstructions.get(address);
  }

  private Instruction processTargetSource(Instruction instruction, Instruction existentCloned) {
    instructionTransformer.virtualRegisterFactory.getRegisterNameBuilder().setCurrentAddress(getAddressOf(instruction));

    Instruction baseInstruction = AbstractInstructionSpy.processToBase(instruction);
    AbstractInstructionSpy.processToBase(instruction);
    instructionTransformer.setCurrentInstruction(baseInstruction);
    Instruction cloned;
    cloned = instructionTransformer.clone(baseInstruction);
    if (existentCloned == null) {
      clonedInstructions.put(pc.read(), cloned);
    } else
      cloned = existentCloned;

    instructions.put(pc.read(), baseInstruction);

    resetter.executeAction(cloned);

    return cloned;
  }

  @Override
  public Instruction execute(Instruction instruction) {
    Instruction existentCloned = clonedInstructions.get(pc.read());
    Instruction cloned = processTargetSource(instruction, existentCloned);

//    if (pc.read().intValue() == 34480)
//      System.out.print("");
    //System.err.println(pc.read() + ":- " + cloned);

    //if (isConcreteInstruction(cloned) || existentCloned != null)
      instructionExecutor.execute(cloned);

    if (noRepeat && cloned instanceof RepeatingInstruction repeatingInstruction)
      repeatingInstruction.setNextPC(-1);

    if (executed.isEmpty() || executed.get(executed.size() - 1) != cloned)
      executed.add(cloned);

    return cloned;
  }

  private boolean isConcreteInstruction(Instruction cloned) {
    boolean[] b = new boolean[]{isConcrete(cloned)};

    InstructionVisitor<?> instructionVisitor = new InstructionVisitor<Integer>() {
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

      public void visitMemoryAccessOpcodeReference(MemoryAccessOpcodeReference memoryAccessOpcodeReference) {
        b[0] = true;
      }

      public void visitMemoryPlusRegister8BitReference(MemoryPlusRegister8BitReference memoryPlusRegister8BitReference) {
        b[0] = true;
      }

      public boolean visitRepeatingInstruction(RepeatingInstruction tRepeatingInstruction) {
        b[0] = true;
        return false;
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

  private boolean isConcrete(Instruction cloned) {
    return Stream.of(ConditionalInstruction.class, RST.class, In.class, Out.class, EI.class, DI.class)
        .anyMatch(c -> c.isAssignableFrom(cloned.getClass()));
  }

  @Override
  public boolean isExecuting(Instruction instruction) {
    return instructionExecutor.isExecuting(instruction);
  }

  private int getAddressOf(Instruction instruction) {
    return pc.read();
  }

  @Override
  public void reset() {
    clonedInstructions.clear();
    executed.clear();
    instructionExecutor.reset();
    instructionTransformer.virtualRegisterFactory.reset();
  }
}
