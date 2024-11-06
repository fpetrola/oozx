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

package com.fpetrola.z80.instructions.base;

import com.fpetrola.z80.instructions.*;
import com.fpetrola.z80.opcodes.references.WordNumber;
import org.junit.Test;

import static com.fpetrola.z80.registers.RegisterName.*;
import static org.junit.Assert.*;

@SuppressWarnings("ALL")
public class VirtualVirtual8BitsRegisterTest<T extends WordNumber> extends TransformInstructionsTest<T> {
  @Test
  public void testRegisterAssignmentUsingVirtualRegister() {
    add(new Ld(r(H), c(7), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));

    step();
    assertNotEquals(7, r(H).read().intValue());

    step();
    assertEquals(7, readMemAt(memPosition));
  }

  @Test
  public void testRegisterAssignmentUsingVirtualRegisterTwice() {
    add(new Ld(r(H), c(7), f()));
    add(new Ld(r(B), r(H), f()));
    add(new Ld(mm(c(memPosition)), r(B), f()));

    step();
    assertNotEquals(7, r(H).read().intValue());

    step();
    assertNotEquals(7, r(B).read().intValue());

    step();
    assertEquals(7, readMemAt(memPosition));
  }

  @Test
  public void testRegisterAssignmentWithInc() {
    add(new Ld(f(), c(0), f()));
    add(new Ld(r(H), c(7), f()));
    add(new Inc(r(H), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));

    step(2);
    assertNotEquals(7, r(H).read().intValue());

    step();
    assertNotEquals(7, r(H).read().intValue());
    assertNotEquals(8, r(H).read().intValue());

    step();
    assertEquals(8, readMemAt(memPosition));
  }

  @Test
  public void testRegisterAssignmentWithRlaInc() {
    add(new Ld(f(), c(0), f()));
    add(new Ld(r(A), c(4), f()));
    add(new RLA(r(A), f()));
    add(new Ld(mm(c(memPosition)), r(A), f()));
    add(new Ld(r(B), r(A), f()));
    add(new Inc(r(B), f()));
    add(new Ld(mm(c(memPosition)), r(B), f()));
    add(new RL(r(B), f()));
    add(new Ld(mm(c(memPosition)), r(B), f()));

    step(4);
    assertEquals(8, readMemAt(memPosition));

    step(2);
    step();
    assertEquals(9, readMemAt(memPosition));

    step(2);
    assertEquals(18, readMemAt(memPosition));
  }

  @Test
  public void testRegisterUsedTwice() {
    add(new Ld(f(), c(0), f()));

    add(new Ld(r(A), c(4), f()));
    add(new Ld(r(B), r(A), f()));
    add(new Inc(r(A), f()));
    add(new Ld(r(C), r(A), f()));
    add(new Ld(mm(c(memPosition)), r(B), f()));
    add(new Ld(mm(c(memPosition + 1)), r(C), f()));

    step(5);
    assertNotEquals(4, r(C).read().intValue());
    step();
    assertEquals(4, readMemAt(memPosition));
    step();
    assertEquals(5, readMemAt(memPosition + 1));
  }

  @Test
  public void test8BitRegisterAssignmentReflectedIn16Bits() {
    add(new Ld(r(H), c(1), f()));
    add(new Ld(r(L), c(255), f()));
    add(new Ld(r(C), c(7), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));

    step(4);
    assertEquals(7, readMemAt(255 + 256));
  }

  @Test
  public void test8BitRegisterAssignmentReflectedIn16BitsUpdatingL() {
    add(new Ld(r(H), c(1), f()));
    add(new Ld(r(L), c(255), f()));
    add(new Ld(r(C), c(7), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));
    add(new Ld(r(L), c(2), f()));
    add(new Ld(r(C), c(10), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));

    step(4);
    assertEquals(7, readMemAt(255 + 256));

    step(3);
    assertEquals(10, readMemAt(2 + 256));
  }

  @Test
  public void test8BitRegisterAssignmentReflectedIn16BitsIncHL() {
    add(new Ld(r(H), c(1), f()));
    add(new Ld(r(L), c(2), f()));
    add(new Ld(r(C), c(7), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));
    add(new Inc16(r(HL)));
    add(new Ld(r(C), c(10), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));

    step(4);
    assertEquals(7, readMemAt(256 + 2));

    step();
    step(2);
    assertEquals(7, readMemAt(256 + 2));
    assertEquals(10, readMemAt(256 + 2 + 1));
  }

  @Test
  public void test8BitRegisterAssignmentReflectedIn16BitsIncHLTwice() {
    add(new Ld(r(H), c(1), f()));
    add(new Ld(r(L), c(2), f()));
    add(new Inc16(r(HL)));
    add(new Inc16(r(HL)));
    add(new Ld(r(C), c(10), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));

    step(6);
    assertEquals(10, readMemAt(256 + 2 + 2));
  }

  @Test
  public void test8BitRegisterAssignmentReflectedIn16BitsIncHLAndSetH() {
    add(new Ld(r(H), c(1), f()));
    add(new Ld(r(L), c(4), f()));
    add(new Inc16(r(HL)));
    add(new Ld(r(H), c(2), f()));
    add(new Ld(r(C), c(10), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));

    add(new Ld(r(L), c(8), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));

    add(new Inc16(r(HL)));
    add(new Ld(iRR(r(HL)), r(C), f()));

    step(6);
    assertEquals(10, readMemAt(512 + 4 + 1));

    step(2);
    assertEquals(10, readMemAt(512 + 8));

    step(2);
    assertEquals(10, readMemAt(512 + 8 + 1));
  }

  @Test
  public void test16BitRegisterAssignedToOther() {
    add(new Ld(r(C), c(8), f()));

    add(new Ld(r(D), c(5), f()));
    add(new Ld(r(E), c(5), f()));

    add(new Ld(r(H), c(1), f()));
    add(new Ld(r(L), c(4), f()));

    add(new Ld(r(DE), r(HL), f()));

    add(new Ld(iRR(r(DE)), r(C), f()));
    step();
    step();
    step();
    step();
    step();
    step();
    step();
    assertEquals(8, readMemAt(256 + 4));
  }

  @Test
  public void test16BitRegisterAssignedToOtherWithNoDeInit() {
    add(new Ld(r(H), c(1), f()));
    add(new Ld(r(L), c(4), f()));
    add(new Ld(r(DE), r(HL), f()));
    add(new Ld(r(C), c(8), f()));
    add(new Ld(iRR(r(DE)), r(C), f()));

    step(5);
    assertEquals(8, readMemAt(256 + 4));
  }

  @Test
  public void test16BitRegisterIncrementAfterDirectAssignment() {
    add(new Ld(r(HL), c(257), f()));
    add(new Inc16(r(HL)));
    add(new Ld(r(C), c(8), f()));
    add(new Ld(iRR(r(HL)), r(C), f()));

    step(4);
    assertEquals(8, readMemAt(257 + 1));
  }

  @Test
  public void test16BitRegisterIncrementAfterDirectAssignmentMultiple() {
    add(new Ld(f(), c(0), f()));

    add(new Ld(r(A), c(3), f()));
    add(new Ld(r(HL), c(257), f()));
    add(new Inc16(r(HL)));
    add(new Ld(r(DE), r(HL), f()));
    add(new Ld(r(BC), r(HL), f()));
    add(new Ld(iRR(r(DE)), r(A), f()));

    add(new Ld(r(A), c(8), f()));
    add(new Ld(iRR(r(BC)), r(A), f()));

    add(new Inc16(r(HL)));
    add(new Inc16(r(HL)));
    add(new Inc16(r(HL)));
    add(new Ld(iRR(r(DE)), r(A), f()));

    add(new Inc(r(A), f()));
    add(new Inc16(r(BC)));
    add(new Ld(iRR(r(BC)), r(A), f()));

    add(new Ld(r(BC), r(HL), f()));
    add(new Inc(r(A), f()));
    add(new Ld(iRR(r(BC)), r(A), f()));

    step(7);
    assertEquals(3, readMemAt(257 + 1));

    step(2);
    assertEquals(8, readMemAt(257 + 1));

    step(4);
    assertEquals(8, readMemAt(257 + 1));

    step(3);
    assertEquals(9, readMemAt(257 + 1 + 1));

    step(2);
    step();
    assertEquals(10, readMemAt(257 + 1 + 1 + 1 + 1));

    T read = r(H).read();
    assertNotEquals(7, read.intValue());
  }

  @Test
  public void test16BitRegisterAssignedTwice() {
    add(new Ld(r(A), c(8), f()));
    add(new Ld(r(HL), c(257), f()));
    add(new Inc16(r(HL)));
    add(new Ld(r(HL), c(300), f()));
    add(new Ld(iRR(r(HL)), r(A), f()));

    step(5);
    assertEquals(8, readMemAt(300));
  }


  @Test
  public void testIncremented16BitRegisterUsedTwice() {
    add(new Ld(r(A), c(3), f()));
    add(new Ld(r(HL), c(257), f()));
    add(new Inc16(r(HL)));
    add(new Ld(r(DE), r(HL), f()));
    add(new Ld(r(BC), r(HL), f()));
    add(new Ld(iRR(r(DE)), r(A), f()));

    add(new Ld(r(A), c(8), f()));
    add(new Ld(iRR(r(BC)), r(A), f()));

    step(6);
    assertEquals(3, readMemAt(257 + 1));
    assertEquals(1, countExecutedInstructionsOfType(Inc16.class));

    step(2);
    assertEquals(8, readMemAt(257 + 1));
    assertEquals(1, countExecutedInstructionsOfType(Inc16.class));
  }
}
