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

import com.fpetrola.z80.base.DriverConfigurator;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.base.ManualBytecodeGenerationTest;
import com.fpetrola.z80.opcodes.references.WordNumber;
import org.junit.Assert;
import org.junit.Test;

import static com.fpetrola.z80.registers.RegisterName.*;

@SuppressWarnings("ALL")

public class ManualJSWTest<T extends WordNumber> extends ManualBytecodeGenerationTest<T> {
  public ManualJSWTest() {
    super(new DriverConfigurator<T>());
  }

  @Test
  public void testJSW2() {
    pc().write(WordNumber.createValue(37056));


    int _246 = 102;

    add(new Ld(r(IX), c(0x8100), f()));
    add(new Ld(r(A), iRRn(r(IX), 0), f()));
    add(new Cp(r(A), c(0xFF), f()));
    add(new Ret(z(), r(SP), mem(), r(PC)));
    add(new And(r(A), c(0x3), f()));
    add(new JP(c(_246), z(), r(PC)));
    add(new Cp(r(A), c(0x1), f()));
    add(new JP(c(51), z(), r(PC)));
    add(new Cp(r(A), c(0x2), f()));
    add(new JP(c(81), z(), r(PC)));
    add(new BIT(iRRn(r(IX), 0), 7, f(), r(PC)));
    add(new JR(c(32), z(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 1), f()));
    add(new BIT(r(A), 7, f(), r(PC)));
    add(new JR(c(15), z(), r(PC)));
    add(new Sub(r(A), c(0x2), f()));
    add(new Cp(r(A), c(0x94), f()));
    add(new JR(c(49), nc(), r(PC)));
    add(new Sub(r(A), c(0x2), f()));
    add(new Cp(r(A), c(0x80), f()));
    add(new JR(c(43), nz(), r(PC)));
    add(new Xor(r(A), r(A), f()));
    add(new JR(c(40), t(), r(PC)));
    add(new Add(r(A), c(0x2), f()));
    add(new Cp(r(A), c(0x12), f()));
    add(new JR(c(34), nc(), r(PC)));
    add(new Add(r(A), c(0x2), f()));
    add(new JR(c(30), t(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 1), f()));
    add(new BIT(r(A), 7, f(), r(PC)));
    add(new JR(c(15), nz(), r(PC)));
    add(new Sub(r(A), c(0x2), f()));
    add(new Cp(r(A), c(0x14), f()));
    add(new JR(c(17), nc(), r(PC)));
    add(new Sub(r(A), c(0x2), f()));
    add(new Or(r(A), r(A), f()));
    add(new JR(c(12), nz(), r(PC)));
    add(new Ld(r(A), c(0x80), f()));
    add(new JR(c(8), t(), r(PC)));
    add(new Add(r(A), c(0x2), f()));
    add(new Cp(r(A), c(0x92), f()));
    add(new JR(c(2), nc(), r(PC)));
    add(new Add(r(A), c(0x2), f()));
    add(new Ld(iRRn(r(IX), 1), r(A), f()));
    add(new And(r(A), c(0x7F), f()));
    add(new Cp(r(A), iRRn(r(IX), 7), f()));
    add(new JP(c(_246), nz(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 0), f()));
    add(new Xor(r(A), c(0x80), f()));
    add(new Ld(iRRn(r(IX), 0), r(A), f()));
    add(new JP(c(_246), t(), r(PC)));
    add(new BIT(iRRn(r(IX), 0), 7, f(), r(PC)));
    add(new JR(c(35), nz(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 0), f()));
    add(new Sub(r(A), c(0x20), f()));
    add(new And(r(A), c(0x7F), f()));
    add(new Ld(iRRn(r(IX), 0), r(A), f()));
    add(new Cp(r(A), c(0x60), f()));
    add(new JR(c(111), c(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 2), f()));
    add(new And(r(A), c(0x1F), f()));
    add(new Cp(r(A), iRRn(r(IX), 6), f()));
    add(new JR(c(5), z(), r(PC)));
    add(new Dec(iRRn(r(IX), 2), f()));
    add(new JR(c(96), t(), r(PC)));
    add(new Ld(iRRn(r(IX), 0), c(0x81), f()));
    add(new JR(c(90), t(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 0), f()));
    add(new Add(r(A), c(0x20), f()));
    add(new Or(r(A), c(0x80), f()));
    add(new Ld(iRRn(r(IX), 0), r(A), f()));
    add(new Cp(r(A), c(0xA0), f()));
    add(new JR(c(76), nc(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 2), f()));
    add(new And(r(A), c(0x1F), f()));
    add(new Cp(r(A), iRRn(r(IX), 7), f()));
    add(new JR(c(5), z(), r(PC)));
    add(new Inc(iRRn(r(IX), 2), f()));
    add(new JR(c(61), t(), r(PC)));
    add(new Ld(iRRn(r(IX), 0), c(0x61), f()));
    add(new JR(c(55), t(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 0), f()));
    add(new Xor(r(A), c(0x8), f()));
    add(new Ld(iRRn(r(IX), 0), r(A), f()));
    add(new And(r(A), c(0x18), f()));
    add(new JR(c(8), z(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 0), f()));
    add(new Add(r(A), c(0x20), f()));
    add(new Ld(iRRn(r(IX), 0), r(A), f()));
    add(new Ld(r(A), iRRn(r(IX), 3), f()));
    add(new Add(r(A), iRRn(r(IX), 4), f()));
    add(new Ld(iRRn(r(IX), 3), r(A), f()));
    add(new Cp(r(A), iRRn(r(IX), 7), f()));
    add(new JR(c(13), nc(), r(PC)));
    add(new Cp(r(A), iRRn(r(IX), 6), f()));
    add(new JR(c(2), z(), r(PC)));
    add(new JR(c(14), nc(), r(PC)));
    add(new Ld(r(A), iRRn(r(IX), 6), f()));
    add(new Ld(iRRn(r(IX), 3), r(A), f()));
    add(new Ld(r(A), iRRn(r(IX), 4), f()));
    add(new Neg(r(A), f()));
    add(new Ld(iRRn(r(IX), 4), r(A), f()));
    add(new Ld(r(DE), c(0x8), f()));
    add(new Add16(r(IX), r(DE), f()));
    add(new JP(c(4), t(), r(PC)));

//    step(104);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
        }
        """, generateAndDecompile());
  }
}
