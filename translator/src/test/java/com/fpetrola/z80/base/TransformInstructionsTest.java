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

package com.fpetrola.z80.base;

import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.RegisterName;
import org.junit.Before;

public class TransformInstructionsTest<T extends WordNumber> extends BaseInstructionLoopTest<T> {
  protected int memPosition = 1000;
  protected int addedInstructions;

  public TransformInstructionsTest(IDriverConfigurator<T> driverConfigurator) {
    super(driverConfigurator);
  }

  @Before
  public void setUp() {
    super.setUp();
    useSecond();
    reset();
    currentContext.r(RegisterName.PC).write(WordNumber.createValue(0));
  }

  @Override
  public void reset() {
    super.reset();
    addedInstructions= 0;
  }

  @Override
  public int add(Instruction<T> instruction) {
    addedInstructions++;
    return super.add(instruction);
  }
}
