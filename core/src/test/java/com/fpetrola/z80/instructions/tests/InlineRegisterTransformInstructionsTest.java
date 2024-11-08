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

import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.blocks.BlockType;
import com.fpetrola.z80.blocks.BlocksManager;
import com.fpetrola.z80.blocks.CodeBlockType;
import com.fpetrola.z80.blocks.ranges.RangeHandler;
import com.fpetrola.z80.blocks.references.ReferencesHandler;
import com.fpetrola.z80.instructions.impl.*;
import com.fpetrola.z80.instructions.visitor.ManualBytecodeGenerationTest;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.transformations.RegisterTransformerInstructionSpy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static com.fpetrola.z80.registers.RegisterName.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ALL")
@Ignore
public class InlineRegisterTransformInstructionsTest<T extends WordNumber> extends ManualBytecodeGenerationTest<T> {

  private BlocksManager blocksManager;

  @Before
  public void setUp() {
    super.setUp();
    blocksManager = RegisterTransformerInstructionSpy.blocksManager;
    blocksManager.clear();
  }

  @Test
  public void testJRNZSimpleLoop() {
    add(new Ld(r(F), c(20), f()));
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(A), c(1), f()));
    add(new Ld(r(D), c(2), f()));
    add(new Ld(r(H), c(7), f()));

    add(new Inc(r(H), f()));
    add(new Ld(mm(c(memPosition)), r(H), f()));
    add(new Add(r(D), r(H), f()));
    add(new Xor(r(A), r(D), f()));
    add(new Add(r(A), r(B), f()));
    add(new Ld(mm(c(memPosition + 2)), r(A), f()));
    add(new Ld(mm(c(memPosition + 1)), r(D), f()));
    add(new Dec(r(B), f()));
    add(new JR(c(-6), nz(), r(PC)));
    add(new Ld(mm(c(memPosition + 100)), r(H), f()));

    step(27);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.F = 20;
              super.B = 3;
              super.A = 1;
              super.D = 2;
              super.H = 7;
              int var1 = super.H + 1 & 255;
              super.H = var1;
              this.wMem(1000, super.H, 6);
              int var2 = super.D + super.H & 255;
              super.D = var2;
        
              do {
                 int var3 = super.A ^ super.D;
                 super.A = var3;
                 int var4 = super.A + super.B & 255;
                 super.A = var4;
                 this.wMem(1002, super.A, 10);
                 this.wMem(1001, super.D, 11);
                 int var5 = super.B - 1 & 255;
                 super.B = var5;
              } while(super.B != 0);
        
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void createPlainExecution() {
    setUpMemory();
    add(new Ld(r(F), c(0), f()));
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(DE), c(520), f()));
    add(new Ld(r(A), c(0), f()));
    add(new Ld(r(C), c(0), f()));

    add(new Ld(r(H), c(7), f()));
    add(new Ld(r(L), r(A), f()));
    add(new Add(r(D), r(A), f()));
    add(new Add(r(E), r(A), f()));

    add(new Add(r(C), r(B), f()));
    add(new Add(r(C), r(B), f()));
    add(new Add(r(C), r(B), f()));
    add(new Ld(r(A), iRR(r(B)), f()));
    add(new Inc(r(A), f()));
    add(new Ld(mm(c(memPosition + 2)), r(A), f()));
    add(new Ld(iRR(r(E)), r(D), f()));
    add(new Inc(r(H), f()));
    add(new Inc(r(D), f()));
    add(new Ld(r(F), c(1), f()));
    add(new DJNZ(c(-8), bnz(), r(PC)));
    //add(new Ret(t(), r(SP), mem(), r(PC)));

    step(30);
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.F = 0;
              super.B = 3;
              this.DE(520);
              super.A = 0;
              super.C = 0;
              super.H = 7;
              super.L = super.A;
              int var1 = super.D + super.A & 255;
              super.D = var1;
              int var2 = super.E + super.A & 255;
              super.E = var2;
              int var3 = super.C + super.B & 255;
              super.C = var3;
              int var4 = super.C + super.B & 255;
              super.C = var4;
              int var5 = super.C + super.B & 255;
              super.C = var5;
        
              do {
                 int var6 = this.mem(super.B, 12);
                 super.A = var6;
                 int var7 = super.A + 1 & 255;
                 super.A = var7;
                 this.wMem(1002, super.A, 14);
                 this.wMem(super.E, super.D, 15);
                 int var8 = super.H + 1 & 255;
                 super.H = var8;
                 int var9 = super.D + 1 & 255;
                 super.D = var9;
                 super.F = 1;
                 int var10 = super.B - 1 & 255;
                 super.B = var10;
              } while(super.B != 0);
        
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void createPlainExecution2() {
    setUpMemory();
    add(new Ld(r(F), c(0), f()));
    add(new Ld(r(D), c(100), f()));
    add(new Ld(r(A), iRR(r(D)), f()));
    add(new Inc(r(A), f()));
    add(new Ld(r(B), r(A), f()));
    add(new Ld(mm(c(memPosition + 2)), r(A), f()));
    add(new Ld(mm(c(memPosition + 3)), r(B), f()));

    int endAddress = 7;
    step(endAddress);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.F = 0;
              super.D = 100;
              int var1 = this.mem(super.D, 2);
              super.A = var1;
              int var2 = super.A + 1 & 255;
              super.A = var2;
              super.B = super.A;
              this.wMem(1002, super.A, 5);
              this.wMem(1003, super.B, 6);
           }
        }
        """, generateAndDecompile());
  }


  @Test
  public void incrementDBeforeDJNZ() {
    setUpMemory();
    add(new Ld(r(F), c(0), f()));
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(D), c(4), f()));

    add(new Ld(iRR(r(B)), r(D), f()));
    add(new Inc(r(D), f()));
    add(new DJNZ(c(-3), bnz(), r(PC)));

    step(10);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.F = 0;
              super.B = 3;
              super.D = 4;
        
              do {
                 this.wMem(super.B, super.D, 3);
                 int var1 = super.D + 1 & 255;
                 super.D = var1;
                 int var2 = super.B - 1 & 255;
                 super.B = var2;
              } while(super.B != 0);
        
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void bug2() {
    setUpMemory();
    add(new Ld(r(F), c(0), f()));
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(C), c(0), f()));
    add(new Ld(r(A), c(10), f()));

    add(new Ld(r(C), r(B), f()));
    add(new Ld(r(A), r(C), f()));
    add(new Inc(r(A), f()));
    add(new DJNZ(c(-4), bnz(), r(PC)));
    add(new Ld(r(D), r(C), f()));

    step(17);
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.F = 0;
              super.B = 3;
              super.C = 0;
              super.A = 10;
        
              do {
                 super.C = super.B;
                 super.A = super.C;
                 int var1 = super.A + 1 & 255;
                 super.A = var1;
                 int var2 = super.B - 1 & 255;
                 super.B = var2;
              } while(super.B != 0);
        
              super.D = super.C;
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void firstLoop() {
    setUpMemory();
    add(new Ld(r(F), c(0), f()));
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(C), c(0), f()));
    add(new Ld(r(A), c(4), f()));

    add(new Ld(r(DE), c(520), f()));
    add(new Ld(r(H), c(7), f()));
    add(new Ld(r(L), r(A), f()));

    add(new Add16(r(HL), r(HL), f()));
    add(new Add16(r(HL), r(HL), f()));
    add(new Add16(r(HL), r(HL), f()));
    add(new Ld(r(B), c(3), f()));

    add(new Ld(r(A), iRR(r(HL)), f()));
    add(new Ld(iRR(r(DE)), r(A), f()));
    add(new Inc16(r(HL)));
    add(new Inc(r(D), f()));
    add(new DJNZ(c(-5), bnz(), r(PC)));
//    add(new Inc(r(A), f()));
//    add(new Ld(iRR(r(HL)), r(A), f()));
//    add(new JP(c(2), t(), r(PC)));


    step(26);
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.F = 0;
              super.B = 3;
              super.C = 0;
              super.A = 4;
              this.DE(520);
              super.H = 7;
              super.L = super.A;
              int var1 = this.HL() * 2 & '\\uffff';
              this.HL(var1);
              int var2 = this.HL() * 2 & '\\uffff';
              this.HL(var2);
              int var3 = this.HL() * 2 & '\\uffff';
              this.HL(var3);
              super.B = 3;
        
              do {
                 int var4 = this.HL();
                 int var5 = this.mem(var4, 11);
                 super.A = var5;
                 int var6 = this.DE();
                 this.wMem(var6, super.A, 12);
                 int var7 = this.HL() + 1 & '\\uffff';
                 this.HL(var7);
                 int var8 = super.D + 1 & 255;
                 super.D = var8;
                 int var9 = super.B - 1 & 255;
                 super.B = var9;
              } while(super.B != 0);
        
           }
        }
        """, generateAndDecompile());
  }


  @Test
  public void bug3() {
    setUpMemory();
    add(new Ld(r(HL), c(62525), f()));
    add(new Add16(r(HL), c(3), f()));
    add(new Ld(r(A), iRR(r(HL)), f()));

    add(new Inc(r(HL), f()));
    add(new Ld(r(H), iRR(r(HL)), f()));
    add(new Ld(mm(c(100)), r(H), f()));

    step(4);
    step(1);
    step(1);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              int HL = 0;
              int H = 0;
              HL = (char)62525;
              HL = HL + 3 & '\\uffff';
              this.mem(HL, 2);
              HL = HL + 1 & 255;
              H = this.mem(HL, 4);
              this.wMem(100, H, 5);
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void bugIX() {
    setUpMemory();
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(IX), c(33024), f()));
    add(new Ld(r(A), iRRn(r(IX), 4), f()));
    add(new Ld(r(C), c(100), f()));
    add(new Add16(r(IX), c(3), f()));

    add(new DJNZ(c(-4), bnz(), r(PC)));
    add(new Ld(r(C), iRRn(r(A), 4), f()));

    step(5);
    step(1);
    step(9);


    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 3;
              this.IX(33024);
        
              do {
                 int var1 = this.IX() + 4;
                 int var2 = this.mem(var1, 2);
                 super.A = var2;
                 super.C = 100;
                 int var3 = this.IX() + 3 & '\\uffff';
                 this.IX(var3);
                 int var4 = super.B - 1 & 255;
                 super.B = var4;
              } while(super.B != 0);
        
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void bugIXAsTarget() {
    setUpMemory();
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(IX), c(33024), f()));
    add(new Ld(r(A), c(100), f()));
    add(new Ld(iRRn(r(IX), 4), r(A), f()));
    add(new Add16(r(IX), c(3), f()));

    add(new DJNZ(c(-4), bnz(), r(PC)));
    add(new Add16(r(IX), c(3), f()));

    step(5);
    step(1);
    step(6);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 3;
              this.IX(33024);
        
              do {
                 super.A = 100;
                 int var1 = this.IX() + 4;
                 this.wMem(var1, super.A, 3);
                 int var2 = this.IX() + 3 & '\\uffff';
                 this.IX(var2);
                 int var3 = super.B - 1 & 255;
                 super.B = var3;
              } while(super.B != 0);
        
           }
        }
        """, generateAndDecompile());
  }


  @Test
  public void bugForwardJumps() {
    setUpMemory();
    int djnzLine = 12;

    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(IX), c(1000), f()));
    add(new Ld(r(A), c(100), f()));

    add(new Cp(r(B), c(3), f()));
    add(new JR(c(4), z(), r(PC)));
    add(new Cp(r(B), c(2), f()));
    add(new JR(c(4), z(), r(PC)));
    add(new Ld(iRRn(r(IX), 1), r(A), f()));
    add(new JR(c(3), t(), r(PC)));
    add(new Ld(iRRn(r(IX), 3), r(A), f()));
    add(new JR(c(1), t(), r(PC)));
    add(new Ld(iRRn(r(IX), 2), r(A), f()));

    add(new DJNZ(c(-10), bnz(), r(PC)));
    add(new Add16(r(IX), c(20), f()));

    step(21);
    step(1);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 3;
              this.IX(1000);
              super.A = 100;
        
              do {
                 if (super.B != 3) {
                    if (super.B != 2) {
                       int var5 = this.IX() + 1;
                       this.wMem(var5, super.A, 7);
                    } else {
                       int var4 = this.IX() + 2;
                       this.wMem(var4, super.A, 11);
                    }
                 } else {
                    int var1 = this.IX() + 3;
                    this.wMem(var1, super.A, 9);
                 }
        
                 int var2 = super.B - 1 & 255;
                 super.B = var2;
              } while(super.B != 0);
        
              int var3 = this.IX() + 20 & '\\uffff';
              this.IX(var3);
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void bugForward2Jumps() {
    setUpMemory();
    int djnzLine = 8;

    add(new Ld(r(B), c(2), f()));
    add(new Ld(r(IX), c(1000), f()));
    add(new Ld(r(A), c(100), f()));

    add(new Cp(r(B), c(2), f()));
    add(new JR(c(2), z(), r(PC)));
    add(new Ld(iRRn(r(IX), 1), r(A), f()));
    add(new JP(c(djnzLine), t(), r(PC)));
    add(new Ld(iRRn(r(IX), 2), r(A), f()));

    add(new DJNZ(c(-6), bnz(), r(PC)));
    add(new Add16(r(IX), c(20), f()));

    step(12);
    step(1);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 2;
              this.IX(1000);
              super.A = 100;
        
              do {
                 if (super.B != 2) {
                    int var4 = this.IX() + 1;
                    this.wMem(var4, super.A, 5);
                 } else {
                    int var1 = this.IX() + 2;
                    this.wMem(var1, super.A, 7);
                 }
        
                 int var2 = super.B - 1 & 255;
                 super.B = var2;
              } while(super.B != 0);
        
              int var3 = this.IX() + 20 & '\\uffff';
              this.IX(var3);
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testJRNZMoreSimpleLoop() {
    add(new Ld(r(B), c(3), f()));

    add(new Ld(mm(c(memPosition)), r(B), f()));
    add(new Dec(r(B), f()));
    add(new JR(c(-3), nz(), r(PC)));
    add(new Ld(mm(c(memPosition + 100)), r(B), f()));

    step(11);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 3;
        
              do {
                 this.wMem(1000, super.B, 1);
                 int var1 = super.B - 1 & 255;
                 super.B = var1;
              } while(super.B != 0);
        
              this.wMem(1100, super.B, 4);
           }
        }
        """, generateAndDecompile());
  }


  @Test
  public void forwardReference2Jumps() {
    setUpMemory();
    int djnzLine = 8;

    add(new Ld(r(B), c(2), f()));
    add(new Ld(r(IX), c(1000), f()));
    add(new Ld(r(A), c(1), f()));

    add(new Cp(r(B), c(2), f()));
    add(new JR(c(2), z(), r(PC)));
    add(new Ld(r(A), c(10), f()));
    add(new JR(c(1), t(), r(PC)));
    add(new Ld(r(A), c(20), f()));

    add(new Ld(iRRn(r(IX), 1), r(A), f()));
    add(new DJNZ(c(-7), bnz(), r(PC)));
    add(new Add16(r(IX), c(13), f()));
    add(new Ld(iRRn(r(IX), 1), r(B), f()));

    step(16);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 2;
              this.IX(1000);
              super.A = 1;
        
              do {
                 if (super.B != 2) {
                    super.A = 10;
                 } else {
                    super.A = 20;
                 }
        
                 int var1 = this.IX() + 1;
                 this.wMem(var1, super.A, 8);
                 int var2 = super.B - 1 & 255;
                 super.B = var2;
              } while(super.B != 0);
        
              int var3 = this.IX() + 13 & '\\uffff';
              this.IX(var3);
              int var4 = this.IX() + 1;
              this.wMem(var4, super.B, 11);
           }
        }
        """, generateAndDecompile());

    //testBlocks();
  }

  public void testBlocks() {
    List<Block> blocks = blocksManager.getBlocks();

    assertNotNull(blocks, "Blocks list should not be null");
    assertFalse(blocks.isEmpty(), "Blocks list should not be empty");
    assertEquals(5, blocks.size(), "Blocks list size does not match");

    // Obtener instancias de los bloques al principio
    Block block0 = blocks.get(0);
    Block block1 = blocks.get(1);
    Block block2 = blocks.get(2);
    Block block3 = blocks.get(3);
    Block block4 = blocks.get(4);

    assertBlockAttributes(block0, 0, 4, "WHOLE_MEMORY", true, List.of(block1, block2), List.of(block2));
    assertBlockAttributes(block1, 5, 6, null, true, List.of(block2), List.of(block0));
    assertBlockAttributes(block2, 7, 9, null, true, List.of(block0, block3), List.of(block0, block1));
    assertBlockAttributes(block3, 10, 12, null, false, List.of(), List.of(block2));

    RangeHandler rangeHandler = block4.getRangeHandler();
    assertNotNull(rangeHandler, "RangeHandler should not be null");
    assertEquals(13, rangeHandler.getStartAddress(), "RangeHandler start address does not match");
    assertEquals(0xFFFF, rangeHandler.getEndAddress(), "RangeHandler end address does not match");
  }

  private void assertBlockAttributes(Block block, int expectedStartAddress, int expectedEndAddress, String expectedCallType, boolean expectedCompleted, List<Block> expectedNextBlocks, List<Block> expectedPreviousBlocks) {
    assertNotNull(block, "Block should not be null");

    // Verificar ReferencesHandler
    ReferencesHandler referencesHandler = block.getReferencesHandler();
    assertNotNull(referencesHandler, "ReferencesHandler should not be null");

    // Verificar RangeHandler
    RangeHandler rangeHandler = block.getRangeHandler();
    assertNotNull(rangeHandler, "RangeHandler should not be null");
    assertEquals(expectedStartAddress, rangeHandler.getStartAddress(), "RangeHandler start address does not match");
    assertEquals(expectedEndAddress, rangeHandler.getEndAddress(), "RangeHandler end address does not match");

    // Verificar CallType
    String callType = block.getCallType();
    if (expectedCallType != null) {
      assertEquals(expectedCallType, callType, "CallType does not match");
    } else {
      assertNull(callType, "CallType should be null");
    }

    // Verificar BlocksManager
    BlocksManager blocksManager = block.getBlocksManager();
    assertNotNull(blocksManager, "BlocksManager should not be null");

    // Verificar BlockType
    BlockType blockType = block.getBlockType();
    assertNotNull(blockType, "BlockType should not be null");

    // Verificar si el bloque está completado
    boolean completed = block.isCompleted();
    assertEquals(expectedCompleted, completed, "Block completion status does not match");

    // Verificar nextBlocks
    List<Block> nextBlocks = ((CodeBlockType) block.getBlockType()).getNextBlocks();
    assertNotNull(nextBlocks, "NextBlocks should not be null");
    assertEquals(expectedNextBlocks.size(), nextBlocks.size(), "NextBlocks size does not match");
    assertTrue(nextBlocks.containsAll(expectedNextBlocks), "NextBlocks does not contain all expected blocks");

    // Verificar previousBlocks
    List<Block> previousBlocks = ((CodeBlockType) block.getBlockType()).getPreviousBlocks();
    assertNotNull(previousBlocks, "PreviousBlocks should not be null");
    assertEquals(expectedPreviousBlocks.size(), previousBlocks.size(), "PreviousBlocks size does not match");
    assertTrue(previousBlocks.containsAll(expectedPreviousBlocks), "PreviousBlocks does not contain all expected blocks");
  }


  @Test
  public void bugIXB() {
    setUpMemory();
    add(new Ld(r(B), c(3), f()));
    add(new Ld(r(D), c(300), f()));
    add(new Ld(r(A), iRRn(r(D), 4), f()));
    add(new Add(r(C), c(2), f()));
    add(new Add(r(D), c(3), f()));

    add(new DJNZ(c(-4), bnz(), r(PC)));
    add(new Add(r(C), r(A), f()));

    step(6);
    step(9);


    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 3;
              super.D = 300;
        
              do {
                 int var1 = super.D + 4;
                 int var2 = this.mem(var1, 2);
                 super.A = var2;
                 int var3 = super.C + 2 & 255;
                 super.C = var3;
                 int var4 = super.D + 3 & 255;
                 super.D = var4;
                 int var5 = super.B - 1 & 255;
                 super.B = var5;
              } while(super.B != 0);
        
              int var6 = super.C + super.A & 255;
              super.C = var6;
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testLdOr() {
    add(new Ld(r(HL), c(100), f()));
    add(new Or(r(A), iRR(r(HL)), f()));
    add(new Ld(iRR(r(HL)), r(A), f()));

    step(3);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              int HL = 0;
              int A = 0;
              HL = 100;
              int var3 = this.mem(HL, 1);
              A |= var3;
              this.wMem(HL, A, 2);
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testBugSet() {
    add(new Ld(r(IX), c(253), f()));
    add(new Ld(iRRn(r(IX), 0), c(1), f()));
    add(new Ld(iRRn(r(IX), 1), c(2), f()));
    add(new Ld(iRRn(r(IX), 2), c(3), f()));

    add(new Ld(r(HL), c(200), f()));
    add(new Inc(r(HL), f()));

    add(new Ld(r(H), c(0), f()));
    add(new Ld(r(A), c(10), f()));
    add(new Ld(r(L), c(253), f()));

    add(new SET(iRR(r(HL)), 6, f()));
    add(new Sub(r(A), r(L), f()));
    add(new Or(r(A), c(0x2C), f()));
    add(new JR(c(-3), nz(), r(PC)));

    add(new Ld(r(HL), c(29), f()));

    step(6);
    step(13);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              this.IX(253);
              int var1 = this.IX();
              this.wMem(var1, 1, 1);
              int var2 = this.IX() + 1;
              this.wMem(var2, 2, 2);
              int var3 = this.IX() + 2;
              this.wMem(var3, 3, 3);
              this.HL(200);
              int var4 = this.HL() + 1 & 255;
              this.HL(var4);
              super.H = 0;
              super.A = 10;
              super.L = 253;
              int var5 = this.HL();
              int var6 = this.mem(var5, 9) | 64;
              int var7 = this.HL();
              this.wMem(var7, var6, 9);
        
              do {
                 int var8 = super.A - super.L & 255;
                 super.A = var8;
                 int var9 = super.A | 44;
                 super.A = var9;
              } while(super.A << 1 != 0);
        
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testBug2() {
    add(new Ld(r(IX), c(253), f()));
    add(new Inc(iRRn(r(IX), 3), f()));

    step(2);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              this.IX(253);
              int var1 = this.IX() + 3;
              int var2 = this.mem(var1, 1) + 1 & 255;
              this.wMem(var1, var2, 1);
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testBug2b() {
    add(new Ld(r(IX), c(253), f()));
    add(new Cp(r(A), iRRn(r(IX), 5), f()));
    add(new JR(c(-2), z(), r(PC)));

    step(3);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              this.IX(253);
        
              int var2;
              do {
                 int var1 = this.IX() + 5;
                 var2 = this.mem(var1, 1);
              } while(super.A == var2);
        
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testBugBit() {
    add(new Ld(r(HL), c(253), f()));
    add(new Ld(r(HL), c(253), f()));
    add(new Ld(r(HL), c(253), f()));
    add(new Ld(r(HL), c(253), f()));
    add(new BIT(iRR(r(HL)), 7, f()));
    add(new JR(c(-5), z(), r(PC)));

    step(6);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              this.HL(253);
        
              int var1;
              do {
                 this.HL(253);
                 this.HL(253);
                 this.HL(253);
                 var1 = this.HL();
              } while((this.mem(var1, 4) & 128) == 0);
        
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testDoubleJRBug() {
    add(new Ld(r(IX), c(100), f()));
    add(new Ld(r(A), c(253), f()));
    add(new Cp(r(A), iRRn(r(IX), 5), f()));
    add(new JR(c(-2), z(), r(PC)));
    add(new JR(c(-3), c(), r(PC)));
    add(new Ld(r(A), c(20), f()));

    step(6);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              this.IX(100);
              super.A = 253;
        
              while(true) {
                 int var1 = this.IX() + 5;
                 int var2 = this.mem(var1, 2);
                 if (super.A != var2) {
                    if (super.A >= var2) {
                       super.A = 20;
                       return;
                    }
                 }
              }
           }
        }
        """, generateAndDecompile());
  }

  @Test
  public void testInBug() {
    IO io = new IO() {
      public Object in(Object port) {
        return null;
      }

      public void out(Object port, Object value) {
      }
    };
    add(new Ld(r(A), c(3), f()));
    add(new Ld(r(IX), c(100), f()));
    add(new Or(r(A), r(A), f()));
    add(new JP(c(9), z(), r(PC)));
    add(new Ld(r(BC), c(31), f()));
    add(new In(r(A), r(BC), r(A), r(BC), f(), r(SP), io));
    add(new And(r(A), c(3), f()));
    add(new CPL(r(A), f()));
    add(new And(r(A), r(E), f()));
    add(new Ld(r(E), r(A), f()));
    add(new Ld(r(C), c(0), f()));
    add(new Ld(r(A), r(E), f()));
    add(new And(r(A), c(42), f()));

    step(13);

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public int[] $0(int AF, int BC, int DE, int HL, int IX, int IY, int A, int F, int B, int C, int D, int E, int H, int L, int IXH, int IXL, int IYH, int IYL) {
              A = 3;
              int var19 = AF & 255;
              int var20 = A << 8;
              AF = var19 | var20;
              IX = 100;
              IXL = IX & 255;
              IXH = IX >> 8 & 255;
              A |= A;
              int var21 = AF & 255;
              int var22 = A << 8;
              AF = var21 | var22;
              if (A << 1 != 0) {
                 BC = 31;
                 C = BC & 255;
                 B = BC >> 8 & 255;
                 int var47 = this.in(BC);
                 int var27 = AF & 255;
                 int var28 = var47 << 8;
                 AF = var27 | var28;
                 int var48 = var47 & 3;
                 int var29 = AF & 255;
                 int var30 = var48 << 8;
                 AF = var29 | var30;
                 int var49 = ~var48;
                 int var31 = AF & 255;
                 int var32 = var49 << 8;
                 AF = var31 | var32;
                 A = var49 & E;
                 int var33 = AF & 255;
                 int var34 = A << 8;
                 AF = var33 | var34;
              }
        
              DE = DE & '\\uff00' | A;
              C = 0;
              BC = BC & '\\uff00' | C;
              int var23 = AF & 255;
              int var24 = A << 8;
              AF = var23 | var24;
              int var50 = A & 42;
              int var25 = AF & 255;
              int var26 = var50 << 8;
              AF = var25 | var26;
              return this.result(AF, BC, DE, HL, IX, IY, var50, F, B, C, D, A, H, L, IXL, IXH, IYL, IYH);
           }
        }
        """, generateAndDecompile());
  }
}
