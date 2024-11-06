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

import com.fpetrola.z80.instructions.*;
import com.fpetrola.z80.instructions.base.Instruction;
import com.fpetrola.z80.bytecode.MockedIO;
import com.fpetrola.z80.instructions.base.TransformInstructionsTest;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.Virtual8BitsRegister;
import org.junit.Test;

import java.util.List;

import static com.fpetrola.z80.registers.RegisterName.*;
import static java.util.stream.IntStream.rangeClosed;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("ALL")
public class ConditionalsTransformInstructionsTest<T extends WordNumber> extends TransformInstructionsTest<T> {
  @Test
  public void testIncJPInfiniteLoop() {
    add(new Ld(f(), c(20), f()));
    add(new Ld(r(H), c(7), f()));
    add(new Inc(r(H), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));
    add(new JP(c(2), t(), r(PC)));

    step(4);
    assertEquals(8, readMemAt(memPosition));
    step(1);
    assertEquals(2, r(PC).read().intValue());
    step(1);
    step(1);
    assertEquals(9, readMemAt(memPosition));
  }


  @Test
  public void testJRNZSimpleLoop() {
    add(new Ld(f(), c(20), f()));
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(H), c(7), f()));

    add(new Inc(r(H), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));
    add(new Dec(r(B), f()));
    add(new JR(c(-4), nz(), r(PC)));
    add(new Ld(mm(c(memPosition + 1)), r(H), f()));

    step(3);

    rangeClosed(0, 2).forEach(i -> {
      assertEquals(3, r(PC).read().intValue());
      step();
      step();
      assertEquals(8 + i, readMemAt(memPosition));
      step(2);
    });

    step();
    assertEquals(10, readMemAt(memPosition + 1));
    assertEquals(8, r(PC).read().intValue());


    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();

//    test3Equals(executedInstructions, 2, 6, 12);
//    test3Equals(executedInstructions, 0, 7, 13);
//    test3Equals(executedInstructions, 5, 9, 15);
//
//    assertEquals(Ld.class, executedInstructions.get(18).getClass());

//    ByteCodeGenerator byteCodeGenerator = new ByteCodeGenerator((address) -> currentContext.getTransformedInstructionAt(address), 0, (address) -> true, 8, currentContext.pc());
//    byteCodeGenerator.generate(() -> ClassMaker.beginExternal("JSW").public_(), "JSW.class");
  }

  private void test3Equals(List executedInstructions, int index, int index1, int index2) {
    assertEquals(executedInstructions.get(index), executedInstructions.get(index1));
    assertEquals(executedInstructions.get(index1), executedInstructions.get(index2));
  }

  @Test
  public void testDjnzSimpleLoop() {
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(H), c(7), f()));
    add(new Inc(r(H), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));
    add(new DJNZ(c(-3), bnz(), r(PC)));

    step(4);
    assertEquals(8, readMemAt(memPosition));

    rangeClosed(1, 2).forEach(i -> {
      step();
      assertEquals(2, r(PC).read().intValue());
      step();
      step();
      assertEquals(8 + i, readMemAt(memPosition));
    });

    step();
    assertEquals(5, r(PC).read().intValue());

    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();
  }

  @Test
  public void testDjnzSimpleLoopIncHL() {
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(HL), c(7), f()));

    add(new Inc16(r(HL)));
    add(new Ld(iRR(r(HL)), r(B), f()));
    add(new DJNZ(c(-3), bnz(), r(PC)));

    step(4);
    assertEquals(3, readMemAt(8));

    rangeClosed(1, 2).forEach(i -> {
      step();
      assertEquals(2, r(PC).read().intValue());
      step();
      step();
      assertEquals(3 - i, readMemAt(8 + i));
    });

    step();
    assertEquals(5, r(PC).read().intValue());

    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();
  }


  @Test
  public void testJRNZSimpleLoopJumpingToBegining() {
    add(new Ld(f(), c(20), f()));
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(H), c(7), f()));

    add(new Inc(r(H), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));
    add(new Dec(r(B), f()));
    add(new JR(c(-4), nz(), r(PC)));

    add(new JP(c(1), t(), r(PC)));

    step(3);

    Runnable assertLoop = () -> rangeClosed(0, 2).forEach(i -> {
      assertEquals(3, r(PC).read().intValue());
      step(2);
      assertEquals(8 + i, readMemAt(memPosition));
      step();
      step();
    });
    assertLoop.run();

    step();
    assertEquals(1, r(PC).read().intValue());
    step(2);

    assertLoop.run();

    assertEquals(7, r(PC).read().intValue());
    step();
    assertEquals(1, r(PC).read().intValue());
    step();

    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();
    assertEquals(2, r(PC).read().intValue());
  }


  @Test
  public void stackOverflowBug() {
    setUpMemory();
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(D), c(4), f()));
    add(new Ld(r(E), c(8), f()));

    add(new Add(r(D), r(A), f()));
    add(new Add(r(E), r(A), f()));
    add(new Ld(iRR(r(E)), r(A), f()));
    add(new Inc(r(D), f()));
    add(new DJNZ(c(-5), bnz(), r(PC)));

    step(9);
    step(2);
    step(1);

    List<Instruction<T>> executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();
  }


  @Test
  public void bug2() {
    setUpMemory();
    add(new Ld(r(D), c(2), f()));
    add(new Ld(r(C), c(0), f()));
    add(new Ld(r(A), c(7), f()));

    add(new Ld(r(B), c(2), f()));

    add(new Ld(r(C), r(B), f()));
    add(new Add(r(A), r(C), f()));
    add(new Inc(r(A), f()));
    add(new DJNZ(c(-4), bnz(), r(PC)));
    add(new Dec(r(D), f()));
    add(new JR(c(-7), nz(), r(PC)));
    add(new Ld(r(H), r(A), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));

    step(4);

    Runnable assertExternalLoop = () -> {
      rangeClosed(1, 2).forEach(i -> {
        assertEquals(4, r(PC).read().intValue());
        step(4);
      });
      assertEquals(8, r(PC).read().intValue());
      step(2);
    };

    assertExternalLoop.run();
    assertEquals(3, r(PC).read().intValue());
    step();

    assertExternalLoop.run();

    assertEquals(10, r(PC).read().intValue());
    step(2);

    assertEquals(17, readMemAt(1000));

    List<Instruction<T>> executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();

    Virtual8BitsRegister target = (Virtual8BitsRegister) ((Ld) executedInstructions.get(3)).getTarget();
    assertNull(target.lastVersionRead);
  }

  @Test
  public void testDjnzSimpleLoopIncHL2() {
    add(new Ld(r(A), c(0), f()));
    add(new Ld(r(H), c(2), f()));
    add(new Ld(r(L), c(255), f()));
    add(new Ld(iRR(r(HL)), c(2), f()));
    add(new Dec16(r(HL)));
    add(new Cp(r(A), r(H), f()));
    add(new JR(c(-4), nz(), r(PC)));

    step(3);

    rangeClosed(1, 512).forEach(i -> {
      assertEquals(3, r(PC).read().intValue());
      step(4);
    });

    assertEquals(7, r(PC).read().intValue());

    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();
  }

  @Test
  public void testLddr() {
    setUpMemory();
    add(new Ld(r(A), c(0), f()));
    add(new Ld(r(DE), c(102), f()));
    add(new Ld(r(BC), c(3), f()));
    add(new Ld(r(HL), c(302), f()));
    add(new Lddr(r(PC), rp(BC), new Ldd(r(DE), rp(BC), r(HL), f(), mem(), new MockedIO())));

    step(4);
    step(1);

    assertEquals(22, readMemAt(102));

    step(1);

    assertEquals(21, readMemAt(101));

    step(1);

    assertEquals(22, readMemAt(102));

    assertEquals(5, r(PC).read().intValue());

    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();
  }


  @Test
  public void testLdir() {
    setUpMemory();
    add(new Ld(r(A), c(0), f()));
    add(new Ld(r(DE), c(100), f()));
    add(new Ld(r(BC), c(3), f()));
    add(new Ld(r(HL), c(300), f()));
    add(new Ldir(r(PC), rp(BC), new Ldd(r(DE), rp(BC), r(HL), f(), mem(), new MockedIO())));

    step(4);
    step(1);

    assertEquals(20, readMemAt(100));

    step(1);

    assertEquals(21, readMemAt(101));

    step(1);

    assertEquals(22, readMemAt(102));

    assertEquals(5, r(PC).read().intValue());

    List executedInstructions = getRegisterTransformerInstructionSpy().getExecutedInstructions();
    executedInstructions.size();
  }
}
