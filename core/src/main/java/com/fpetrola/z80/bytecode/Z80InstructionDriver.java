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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.minizx.emulation.MockedMemory;
import com.fpetrola.z80.mmu.State;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.RegisterTransformerInstructionSpy;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static org.junit.Assert.assertNotNull;

public interface Z80InstructionDriver<T extends WordNumber> {
  int add(Instruction<T> instruction);

  State<T> getState();

  void step();

  MockedMemory<T> mem();

  MockedMemory<T> initMem(Supplier<T[]> supplier);

  Instruction getInstructionAt(int i);

  Instruction<T> getTransformedInstructionAt(int i);

  default int readMemAt(int i) {
    T read = mem().read(createValue(i));
    assertNotNull(read);
    return read.intValue();
  }

  default void step(int i) {
    IntStream.range(0, i).forEach(i2 -> step());
  }

  default long countExecutedInstructionsOfType(Class<? extends Instruction> instructionType) {
    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    return executedInstructions.stream().filter(i -> instructionType.isAssignableFrom(i.getClass())).count();
  }

  RegisterTransformerInstructionSpy getRegisterTransformerInstructionSpy();
}
