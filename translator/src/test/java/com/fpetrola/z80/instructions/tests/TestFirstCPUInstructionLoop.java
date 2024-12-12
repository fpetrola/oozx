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

package com.fpetrola.z80.instructions.tests;

import com.fpetrola.z80.base.BaseInstructionLoopTest;
import com.fpetrola.z80.base.PlainDriverConfigurator;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.google.inject.Inject;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;
import static com.fpetrola.z80.registers.RegisterName.*;
import static org.junit.Assert.assertEquals;

@RunWith(TestRunner.class)
@Modules(TransformationsTestBaseModule.class)
public class TestFirstCPUInstructionLoop<T extends WordNumber> extends BaseInstructionLoopTest<T> {

  @Inject
  public TestFirstCPUInstructionLoop(PlainDriverConfigurator tDriverConfigurator) {
    super(tDriverConfigurator);
  }

  @Test
  public void testPlainPath() {
    useSecond();

    createPlainExecution();

    assertLoopSetup();

    assertLoopNumber(0, 16);
    assertLoopNumber(1, 8);
    assertLoopNumber(2, 4);
    assertEquals(11, r(PC).read().intValue());
    step();
    assertEquals(257, r(PC).read().intValue());
  }

  @Test
  public void testPlainPath2() {
    useSecond();
    createPlainExecution();

    step();

    Instruction instructionAt = getInstructionAt(0);
    System.out.println(instructionAt);
    step();
    step();
    step();
  }

  private void createPlainExecution() {
    setUpMemory();
    r(DE).write(createValue(520));

    add(new Ld(r(H), c(7), f()));
    add(new Ld(r(L), r(A), f()));
    // add(new SET(state, l, 7, 0));
    add(new Add16(r(HL), r(HL), f()));
    add(new Add16(r(HL), r(HL), f()));
    add(new Add16(r(HL), r(HL), f()));
    add(new Ld(r(B), c(3), f()));

    add(new Ld(r(A), iRR(r(HL)), f()));
    add(new Ld(iiRR(r(DE)), r(A), f()));
    add(new Inc16(r(HL)));
    add(new Inc(r(D), f()));
    add(new DJNZ(c(-5), bnz(), r(PC)));
    add(new Ret(t(), r(SP), mem(), r(PC)));
  }

  private void assertLoopNumber(int increment, int memoryValue) {
    assertEquals(6, r(PC).read().intValue());
    step();
    assertEquals(memoryValue, r(A).read().intValue());
    step();
    assertEquals(memoryValue, mem().read(r(DE).read(), 0).intValue());
    step();
    assertEquals(14369 + increment, r(HL).read().intValue());
    step();
    assertEquals(3 + increment, r(D).read().intValue());
    assertEquals(10, r(PC).read().intValue());
    step();
  }

  private void assertLoopSetup() {
    r(A).write(createValue(4));
    step();
    assertEquals(7, r(H).read().intValue());
    step();
    assertEquals(4, r(L).read().intValue());
    step();
    assertEquals(3592, r(HL).read().intValue());
    step();
    assertEquals(3592 * 2, r(HL).read().intValue());
    step();
    assertEquals(3592 * 4, r(HL).read().intValue());
    step();
    assertEquals(3, r(B).read().intValue());
  }


  private void assertCompositeLoop(Register<T> vr1, Register<T> counter, int bValue, int memoryReadValue, int indexValue, int dValue, int readAddress, Register<T> vr2A) {
    step();

    assertEquals(memoryReadValue, mem().read(createValue(readAddress), 0).intValue());
    assertEquals(indexValue, vr1.read().intValue());

    step();
    assertEquals(indexValue + 1, vr1.read().intValue());
    assertEquals(dValue, vr2A.read().intValue());

    step();
    assertEquals(dValue + 1, vr2A.read().intValue());

    step();
    assertEquals(bValue - 1, counter.read().intValue());
  }
}
