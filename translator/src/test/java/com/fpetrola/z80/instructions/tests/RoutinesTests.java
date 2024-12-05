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

import com.fpetrola.z80.base.ManualBytecodeGenerationTest;
import com.fpetrola.z80.blocks.Block;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import jakarta.inject.Inject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Iterator;
import java.util.List;

import static com.fpetrola.z80.registers.RegisterName.*;

@SuppressWarnings("ALL")
@RunWith(TestRunner.class)
@Modules(RoutinesModule.class)
public class RoutinesTests<T extends WordNumber> extends ManualBytecodeGenerationTest<T> {

  private final RoutinesDriverConfigurator configurator;

  @Inject
  public RoutinesTests(RoutinesDriverConfigurator configurator) {
    super(configurator);
    this.configurator = configurator;
  }

  public SymbolicExecutionAdapter getSymbolicExecutionAdapter() {
    return ((RoutinesDriverConfigurator) driverConfigurator).symbolicExecutionAdapter;
  }

  @Test
  public void callingSimpleRoutine() {
    setUpMemory();
    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Call(t(), c(5)));
        add(Ld(r(B), c(3)));
        add(Ret(t()));
        add(Ld(r(C), c(4)));

        add(Ld(r(D), c(5)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 2;
              this.$5();
              super.B = 3;
           }
        
           public void $5() {
              super.D = 5;
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals(2, routines.size());

    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 3);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 5, 6);
  }

  protected void stepUntilComplete() {
    getSymbolicExecutionAdapter().stepUntilComplete(this, getState(), 0, 0);
  }

  private void assertBlockAddresses(Block block, int start, int end) {
    Assert.assertEquals(start, block.getRangeHandler().getStartAddress());
    Assert.assertEquals(end, block.getRangeHandler().getEndAddress());
  }

  @Test
  public void callingSimpleRoutineWithContinuation() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(B), c(2)));
        add(Call(t(), c(3)));
        add(Ld(r(B), c(3)));

        add(Ld(r(D), r(B)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    List<Routine> routines = getRoutineManager().getRoutinesInDepth();
    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 2;
              this.$3();
              super.B = 3;
              this.$3();
           }
        
           public void $3() {
              super.D = super.B;
           }
        }
        """, resultingJava);


    Assert.assertEquals(2, routines.size());

    Routine routine0 = routines.get(0);
    Assert.assertEquals(1, routine0.getBlocks().size());

    assertBlockAddresses(routine0.getBlocks().get(0), 0, 2);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 3, 4);
  }


  @Test
  public void multipleInnerRoutinesWithContinuation() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(1)));
        add(Call(t(), c(5)));
        add(Ld(r(B), c(2)));
        add(Call(t(), c(7)));
        add(Ld(r(C), c(3)));

        add(Ld(r(D), c(4)));
        add(Ret(t()));

        add(Ld(r(E), c(5)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 1;
              this.$5();
              super.B = 2;
              this.$7();
              super.C = 3;
              this.$5();
           }
        
           public void $7() {
              super.E = 5;
           }
        
           public void $5() {
              super.D = 4;
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutinesInDepth();


    Assert.assertEquals(3, routines.size());
    Routine routine0 = routines.get(0);
    assertBlockAddresses(routine0.getBlocks().get(0), 0, 4);

    Routine routine1 = routines.get(1);
    Routine routine2 = routines.get(2);
    assertBlockAddresses(routine2.getBlocks().get(0), 5, 6);
    assertBlockAddresses(routine1.getBlocks().get(0), 7, 8);
  }

  @Test
  public void interleavedRoutinesTest() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(1)));
        add(Call(t(), c(5)));
        add(Ld(r(B), c(2)));
        add(JP(c(7), t()));
        add(Ld(r(C), c(3)));

        add(Ld(r(D), c(4)));
        add(Ret(t()));

        add(Ld(r(E), c(5)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();
    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 1;
              this.$5();
              super.B = 2;
              super.E = 5;
           }
        
           public void $5() {
              super.D = 4;
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();


    Assert.assertEquals(2, routines.size());
    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 3);
    assertBlockAddresses(routines.get(0).getBlocks().get(1), 7, 8);


    Assert.assertEquals(0, routines.get(0).getInnerRoutines().size());
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 5, 6);
  }


  @Test
  public void recursiveInnerRoutineWithContinuation() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(1)));
        add(Call(t(), c(5)));
        add(Ld(r(B), c(2)));

        add(Ld(r(C), c(3)));
        add(Call(t(), c(5)));
        add(Ld(r(D), c(6)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 1;
              this.$5();
              super.B = 2;
              super.C = 3;
              this.$5();
              this.$5();
           }
        
           public void $5() {
              super.D = 6;
           }
        }
        """, resultingJava);

    List<Routine> routines = getRoutineManager().getRoutinesInDepth();

    Assert.assertEquals(2, routines.size());
    Routine routine0 = routines.get(0);
    Routine routine1 = routines.get(1);
    assertBlockAddresses(routine0.getBlocks().get(0), 0, 4);

    Block subroutineBlock = routine1.getBlocks().get(0);
    assertBlockAddresses(subroutineBlock, 5, 6);

    Assert.assertEquals(subroutineBlock, routine1.getBlocks().get(0));
  }

  @Test
  public void simpleRoutineTest() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(10)));
        add(Ld(r(B), c(20)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();
    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 10;
              super.B = 20;
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();


    Assert.assertEquals(1, routines.size());
    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 2);
  }

  @Test
  public void nonConsecutiveBlocksTest() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(1)));
        add(JP(c(10), t()));
        add(Ld(r(A), c(2)));
        add(Ld(r(A), c(3)));
        add(Call(t(), c(8)));
        add(Ld(r(B), c(2)));
        add(Ret(t()));
        add(Ld(r(C), c(3)));

        add(Ld(r(D), c(4)));
        add(Ret(t()));

        add(Ld(r(E), c(5)));
        add(JP(c(3), t()));
      }
    };

    stepUntilComplete();
    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 1;
              super.E = 5;
              super.A = 3;
              this.$8();
              super.B = 2;
           }
        
           public void $8() {
              super.D = 4;
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();


    Assert.assertEquals(2, routines.size());
    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 1);
    assertBlockAddresses(routines.get(0).getBlocks().get(1), 3, 6);
    assertBlockAddresses(routines.get(0).getBlocks().get(2), 10, 11);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 8, 9);
  }

  @Ignore
  @Test
  public void recursiveRoutineTest() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Dec(r(A)));
        add(Call(nz(), c(1)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 2;
              this.$1();
           }
        
           public void $1() {
              int var1 = super.A - 1 & 255;
              super.A = var1;
              if (super.A != 0) {
                 this.$1();
              }
        
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();
    Assert.assertEquals(2, routines.size());

    Routine routine0 = routines.get(0);
    assertBlockAddresses(routine0.getBlocks().get(0), 0, 0);
    assertBlockAddresses(routine0.getBlocks().get(1), 1, 3);
    Block innerRoutineBlock = routine0.getInnerRoutines().iterator().next().getBlocks().get(0);
    assertBlockAddresses(innerRoutineBlock, 1, 3);

    Assert.assertEquals(innerRoutineBlock, routines.get(1).getBlocks().get(0));

  }

  @Test
  public void multipleCallsAndRetsTest() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {

        add(Ld(r(A), c(50)));
        add(Call(t(), c(6)));
        add(Ld(r(B), c(60)));
        add(Call(t(), c(8)));
        add(Ld(r(C), c(70)));
        add(Ret(t()));


        add(Ld(r(D), c(80)));
        add(Ret(t()));


        add(Ld(r(E), c(90)));
        add(Ret(t()));
      }
    };


    stepUntilComplete();
    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 50;
              this.$6();
              super.B = 60;
              this.$8();
              super.C = 70;
           }
        
           public void $6() {
              super.D = 80;
           }
        
           public void $8() {
              super.E = 90;
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();


    Assert.assertEquals(3, routines.size());
    Routine routine0 = routines.get(0);
    assertBlockAddresses(routine0.getBlocks().get(0), 0, 5);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 6, 7);
    assertBlockAddresses(routines.get(2).getBlocks().get(0), 8, 9);
  }

  @Test
  public void poppingReturnAddress() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Call(t(), c(6)));
        add(Ld(r(C), c(3)));
        add(Ld(r(C), c(4)));
        add(Ld(r(C), c(5))); // 4
        add(Ret(t()));

        add(Ld(r(D), c(4)));  // 6
        add(JP(c(10), t()));
        add(Ld(r(E), c(5)));
        add(Ret(t()));

        add(Pop(r(HL)));  // 10
        add(Ld(r(A), c(6)));
        add(JP(c(4), t()));
      }
    };


    stepUntilComplete();
    String resultingJava = generateAndDecompile();
    List<Routine> routines = getRoutineManager().getRoutines();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 2;
              this.$6();
              if(!this.isNextPC(11)) {
                 super.C = 3;
                 super.C = 4;
              } else {
                 super.A = 6;
              }
        
              super.C = 5;
           }
        
           public void $6() {
              super.D = 4;
              super.nextAddress = 11;
           }
        }
        """, resultingJava);


    Assert.assertEquals(2, routines.size());
    Routine routine0 = routines.get(0);
    Assert.assertEquals(0, routine0.getStartAddress());
    Assert.assertEquals(12, routine0.getEndAddress());

    Routine routine1 = routines.get(1);
    Assert.assertEquals(6, routine1.getStartAddress());
    Assert.assertEquals(7, routine1.getEndAddress());
  }

  @Test
  public void popping2ReturnAddresses() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Call(t(), c(6)));
        add(Ld(r(C), c(3)));
        add(Ld(r(C), c(4)));
        add(Ld(r(C), c(5))); // 4
        add(Ret(t()));

        add(Ld(r(D), c(4)));  // 6
        add(Call(t(), c(11)));
        add(Ret(t()));
        add(Ld(r(E), c(5)));
        add(Ret(t()));

        add(Dec(r(A))); // 11
        add(JP(c(15), nz()));
        add(Ld(r(E), c(8)));
        add(Ret(t()));

        add(Pop(r(HL))); // 15
        add(Pop(r(HL)));
        add(Ld(r(A), c(61)));
        add(Ld(r(B), c(62)));
        add(JP(c(4), t()));
      }
    };


    stepUntilComplete();
    String resultingJava = generateAndDecompile();

    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 2;
              this.$6();
              if(!this.isNextPC(17)) {
                 super.C = 3;
                 super.C = 4;
              } else {
                 super.A = 61;
                 super.B = 62;
              }
        
              super.C = 5;
           }
        
           public void $6() {
              super.D = 4;
              this.$11();
              if(this.isNextPC(16)) {
                 super.nextAddress = 17;
              }
           }
        
           public void $11() {
              int var1 = super.A - 1 & 255;
              super.A = var1;
              if(super.A != 0) {
                 super.nextAddress = 16;
              } else {
                 super.E = 8;
              }
           }
        }
        """, resultingJava);


    Assert.assertEquals(3, routines.size());
    Routine routine0 = routines.get(0);
    Assert.assertEquals(0, routine0.getStartAddress());
    Assert.assertEquals(19, routine0.getEndAddress());

    Routine routine1 = routines.get(1);
    Assert.assertEquals(6, routine1.getStartAddress());
    Assert.assertEquals(8, routine1.getEndAddress());

    Routine routine2 = routines.get(2);
    Assert.assertEquals(11, routine2.getStartAddress());
    Assert.assertEquals(14, routine2.getEndAddress());
  }

  @Test
  public void popping2ReturnAddressesB() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Call(t(), c(7)));
        add(Ld(r(C), c(2)));
        add(Call(t(), c(22)));
        add(Ld(r(C), c(3)));
        add(Ld(r(C), c(5))); // 5
        add(Ret(t()));

        add(Ld(r(D), c(4)));  // 7
        add(Cp(c(3)));
        add(Call(z(), c(13)));
        add(Ret(t()));
        add(Ld(r(E), c(5)));
        add(Ret(t()));

        add(Ld(r(C), c(40))); // 13
        add(JP(c(16), t()));
        add(Ret(t()));

        add(Pop(r(HL))); // 16
        add(Ld(r(E), c(71)));
        add(Pop(r(HL)));
        add(Ld(r(A), c(61)));
        add(Ld(r(B), c(62)));
        add(JP(c(4), t()));

        add(Ld(r(D), c(41)));  // 22
        add(Ld(r(E), c(51)));
        add(JP(c(18), t()));
      }
    };


    stepUntilComplete();
    String resultingJava = generateAndDecompile();

    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              label12: {
                 super.A = 2;
                 this.$7();
                 if(!this.isNextPC(19)) {
                    super.C = 2;
                    this.$22();
                    if(!this.isNextPC(19)) {
                       break label12;
                    }
                 }
        
                 super.A = 61;
                 super.B = 62;
              }
        
              super.C = 3;
              super.C = 5;
           }
        
           public void $7() {
              super.D = 4;
              if(super.A == 3) {
                 this.$13();
                 if(this.isNextPC(17)) {
                    super.E = 71;
                    super.nextAddress = 19;
                    return;
                 }
              }
        
              int var1 = super.A - 3;
              super.F = var1;
           }
        
           public void $13() {
              super.C = 40;
              super.nextAddress = 17;
           }
        
           public void $22() {
              super.D = 41;
              super.E = 51;
              super.nextAddress = 19;
           }
        }
        """, resultingJava);


    Assert.assertEquals(4, routines.size());
    Routine routine0 = routines.get(0);
    Assert.assertEquals(0, routine0.getStartAddress());
    Assert.assertEquals(21, routine0.getEndAddress());

    Routine routine1 = routines.get(1);
    Assert.assertEquals(7, routine1.getStartAddress());
    Assert.assertEquals(17, routine1.getEndAddress());

    Routine routine2 = routines.get(2);
    Assert.assertEquals(13, routine2.getStartAddress());
    Assert.assertEquals(14, routine2.getEndAddress());

    Routine routine3 = routines.get(3);
    Assert.assertEquals(22, routine3.getStartAddress());
    Assert.assertEquals(24, routine3.getEndAddress());
  }

  @Test
  public void poppingReturnAddressWithPendingBranches() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(H), c(1)));
        add(Ld(r(A), c(2)));
        add(Call(t(), c(7)));
        add(Ld(r(C), c(3)));
        add(Ld(r(C), c(4)));
        add(Ld(r(C), c(5))); // 5
        add(Ret(t()));

        add(Ld(r(D), c(4)));  // 7
        add(Cp(c(3)));
        add(JP(c(19), nz()));
        add(Ld(r(H), c(2)));
        add(Ld(r(D), r(H)));  // 11
        add(JP(c(16), t()));

        add(Ld(r(E), c(5)));
        add(Ret(t()));
        add(Ld(r(H), c(3)));

        add(Pop(r(HL)));  // 16
        add(Ld(r(A), c(6)));
        add(JP(c(5), t()));

        add(Ld(r(A), c(61))); // 19
        add(JP(c(11), t()));
      }
    };


    stepUntilComplete();
    String resultingJava = generateAndDecompile();
    List<Routine> routines = getRoutineManager().getRoutines();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.H = 1;
              super.A = 2;
              this.$7();
              if(!this.isNextPC(17)) {
                 super.C = 3;
                 super.C = 4;
              } else {
                 super.A = 6;
              }
        
              super.C = 5;
           }
        
           public void $7() {
              super.D = 4;
              if(super.A == 3) {
                 super.H = 2;
              } else {
                 super.A = 61;
              }
        
              super.D = super.H;
              super.nextAddress = 17;
           }
        }
        """, resultingJava);


    Assert.assertEquals(2, routines.size());
    Routine routine0 = routines.get(0);
    Assert.assertEquals(0, routine0.getStartAddress());
    Assert.assertEquals(18, routine0.getEndAddress());

    Routine routine1 = routines.get(1);
    Assert.assertEquals(7, routine1.getStartAddress());
    Assert.assertEquals(20, routine1.getEndAddress());
  }

  @Test
  public void callingSimpleRoutineWithRetZ() {
    setUpMemory();
    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Call(t(), c(8)));
        add(Ld(r(B), c(3)));

        add(Ld(r(A), c(0)));
        add(Call(t(), c(8)));
        add(Ld(r(B), c(4)));
        add(Ret(t()));

        add(Ld(r(C), c(4)));

        add(Ld(r(D), c(5)));
        add(Or(r(A)));
        add(Ret(z()));
        add(Ld(r(D), c(6)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();

    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 2;
              this.$8();
              super.B = 3;
              super.A = 0;
              this.$8();
              super.B = 4;
           }
        
           public void $8() {
              super.D = 5;
              if(super.A << 1 != 0) {
                 super.D = 6;
              }
           }
        }
        """, resultingJava);


    Assert.assertEquals(2, routines.size());

    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 6);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 8, 12);
  }

  @Test
  public void callingSimpleRoutineWithRetZInLoop() {
    setUpMemory();
    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Call(t(), c(5)));
        add(Ld(r(B), c(3)));
        add(Ret(t()));

        add(Ld(r(C), c(4)));

        add(Ld(r(D), c(5)));
        add(Dec(r(A)));
        add(Ret(z()));
        add(Ld(r(D), c(6)));
        add(JP(c(5), t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();

    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 2;
              this.$5();
              super.B = 3;
           }
        
           public void $5() {
              while(true) {
                 super.D = 5;
                 int var1 = super.A - 1 & 255;
                 super.A = var1;
                 if(super.A == 0) {
                    return;
                 }
        
                 super.D = 6;
              }
           }
        }
        """, resultingJava);


    Assert.assertEquals(2, routines.size());

    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 3);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 5, 9);
  }

  @Test
  public void callingSimpleRoutineInDjnz() {
    setUpMemory();
    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(10)));
        add(Ld(r(B), c(2)));
        add(Push(r(BC)));
        add(Call(t(), c(11)));
        add(Ld(r(B), c(3)));
        add(Inc(r(A)));
        add(DJNZ(bnz(), c(-2)));
        add(Pop(r(BC)));
        add(DJNZ(bnz(), c(-7)));
        add(Ret(t()));
        add(Ld(r(C), c(4)));

        add(Ld(r(D), r(A)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 10;
              super.B = 2;
        
              do {
                 int var1 = this.BC();
                 this.push(var1);
                 this.$11();
                 super.B = 3;
        
                 do {
                    int var2 = super.A + 1 & 255;
                    super.A = var2;
                    int var3 = super.B - 1 & 255;
                    super.B = var3;
                 } while(super.B != 0);
        
                 int var4 = this.pop();
                 this.BC(var4);
                 int var5 = super.B - 1 & 255;
                 super.B = var5;
              } while(super.B != 0);
        
           }
        
           public void $11() {
              super.D = super.A;
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals(2, routines.size());

    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 9);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 11, 12);
  }


  @Test
  public void callingSimpleRoutineWithRetZ2() {
    setUpMemory();
    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(2)));
        add(Call(t(), c(5)));
        add(Ld(r(B), c(3)));
        add(Ret(t()));

        add(Ld(r(C), c(4)));

        add(And(r(A)));
        add(Ret(z()));
        add(Ld(r(D), c(6)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();

    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 2;
              this.$5();
              super.B = 3;
           }
        
           public void $5() {
              if(super.A << 1 != 0) {
                 super.D = 6;
              }
           }
        }
        """, resultingJava);


    Assert.assertEquals(2, routines.size());

    assertBlockAddresses(routines.get(0).getBlocks().get(0), 0, 3);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 5, 8);
  }

  @Test
  public void simpleRoutineWithContinuation() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(B), c(2)));
        add(Call(t(), c(4)));
        add(Ld(r(B), c(3)));
        add(Ret(t()));

        add(Ld(r(D), r(B)));
        add(Ret(nz()));
        add(Ld(r(B), c(4)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 2;
              this.$4();
              super.B = 3;
           }
        
           public void $4() {
              super.D = super.B;
              if(super.F == 0) {
                 super.B = 4;
              }
           }
        }
        """, resultingJava);


    List<Routine> routines = getRoutineManager().getRoutines();


    Assert.assertEquals(2, routines.size());

    Routine routine0 = routines.get(0);
    Assert.assertEquals(1, routine0.getBlocks().size());

    assertBlockAddresses(routine0.getBlocks().get(0), 0, 3);
  }

  @Ignore
  @Test
  public void callingInnerRoutineOfOther() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(B), c(2)));
        add(Call(t(), c(7)));
        add(Ld(r(B), c(3)));
        add(Call(t(), c(5)));
        add(Ret(t()));

        add(Ld(r(A), c(1)));
        add(JR(t(), c(1)));
        add(Ld(r(A), c(2)));
        add(Ld(r(A), c(3)));
        add(Ld(r(A), c(4)));
        add(Ret(t()));
      }
    };

    stepUntilComplete();

    List<Routine> routines = getRoutineManager().getRoutinesInDepth();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.B = 2;
              this.$5();
              super.B = 3;
              this.$7();
           }
        
           public void $5() {
              this.$5();
           }
        
           public void $7() {
           }
        } 
        """, resultingJava);


    Assert.assertEquals(2, routines.size());

    Routine routine0 = routines.get(0);
    Assert.assertEquals(1, routine0.getBlocks().size());

    assertBlockAddresses(routine0.getBlocks().get(0), 0, 2);
    Assert.assertEquals(1, routine0.getInnerRoutines().size());
    Routine innerRoutine = routine0.getInnerRoutines().iterator().next();
    assertBlockAddresses(innerRoutine.getBlocks().get(0), 3, 4);

    Assert.assertEquals(innerRoutine, routines.get(1));
  }


  @Test
  public void callingSharedCodeFromDifferentRoutines() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(1)));
        add(Call(t(), c(6)));
        add(Ld(r(B), c(8)));
        add(Call(t(), c(10)));
        add(Ld(r(C), c(8)));
        add(Ret(t()));

        add(Ld(r(B), c(1)));
        add(JP(c(14), nz()));
        add(Ld(r(B), c(2)));
        add(Ret(t()));

        add(Ld(r(C), c(1)));
        add(JP(c(14), nz()));
        add(Ld(r(C), c(2)));
        add(Ret(t()));

        add(Ld(r(H), c(1)));
        add(Ret(t()));
      }
    };


    stepUntilComplete();
    String resultingJava = generateAndDecompile();

    List<Routine> routines = getRoutineManager().getRoutines();

    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 1;
              this.$6();
              super.B = 8;
              this.$10();
              super.C = 8;
           }
        
           public void $10() {
              super.C = 1;
              if(super.F != 0) {
                 this.$14();
              } else {
                 super.C = 2;
              }
           }
        
           public void $6() {
              super.B = 1;
              if(super.F != 0) {
                 this.$14();
              } else {
                 super.B = 2;
              }
           }
        
           public void $14() {
              super.H = 1;
           }
        }
        """, resultingJava);

    Assert.assertEquals(4, routines.size());
    Routine routine0 = routines.get(0);
    assertBlockAddresses(routine0.getBlocks().get(0), 0, 5);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 6, 9);
    assertBlockAddresses(routines.get(2).getBlocks().get(0), 10, 13);
    assertBlockAddresses(routines.get(3).getBlocks().get(0), 14, 15);
  }

  @Test
  public void callingSharedCodeInTheMiddleOfExistingRoutine() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        add(Ld(r(A), c(1)));
        add(Call(t(), c(6)));
        add(Ld(r(B), c(8)));
        add(Call(t(), c(10)));
        add(Ld(r(C), c(8)));
        add(Ret(t()));

        add(Ld(r(B), c(1)));
        add(Ld(r(C), c(2)));
        add(Ld(r(D), c(3)));
        add(Ret(t()));

        add(Ld(r(C), c(1)));
        add(JP(c(7), nz()));
        add(Ld(r(C), c(2)));
        add(Ret(t()));
      }
    };


    stepUntilComplete();

    List<Routine> routines = getRoutineManager().getRoutines();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $0() {
              super.A = 1;
              this.$6();
              super.B = 8;
              this.$10();
              super.C = 8;
           }
        
           public void $10() {
              super.C = 1;
              if(super.F != 0) {
                 this.$7();
              } else {
                 super.C = 2;
              }
           }
        
           public void $6() {
              super.B = 1;
              this.$7();
           }
        
           public void $7() {
              super.C = 2;
              super.D = 3;
           }
        }
        """, resultingJava);

    Assert.assertEquals(4, routines.size());
    Routine routine0 = routines.get(0);
    assertBlockAddresses(routine0.getBlocks().get(0), 0, 5);
    assertBlockAddresses(routines.get(1).getBlocks().get(0), 6, 6);
    assertBlockAddresses(routines.get(2).getBlocks().get(0), 7, 9);
    assertBlockAddresses(routines.get(3).getBlocks().get(0), 10, 13);
  }


  @Test
  public void callingSharedCodeInTheMiddleOfExistingRoutine2() {
    setUpMemory();

    getSymbolicExecutionAdapter().new SymbolicInstructionFactoryDelegator() {
      {
        int rut1 = 10;
        int rut2 = 12;

        add(Cp(c(1)));
        add(JP(c(6), nz()));
        add(JP(c(8), nz()));
        add(Call(nz(), c(rut2)));
        add(Ld(r(B), c(3)));
        add(Ret(t()));

        add(Ld(r(B), c(1)));
        add(JP(c(rut1), t()));
        add(Ld(r(B), c(2)));
        add(JP(c(rut1), t()));

        add(Ld(r(C), c(1)));
        add(JP(c(3), t()));

        add(Ld(r(C), c(2)));
        add(JP(c(4), t()));
      }
    };


    stepUntilComplete();

    List<Routine> routines = getRoutineManager().getRoutines();

    String resultingJava = generateAndDecompile();
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        
        public class JSW extends SpectrumApplication {
           public void $12() {
              super.C = 2;
              this.$4();
           }
        
           public void $0() {
              if(super.A != 1) {
                 this.$6();
              } else if(super.A != 1) {
                 this.$8();
              } else {
                 this.$3();
              }
           }
        
           public void $4() {
              super.B = 3;
           }
        
           public void $3() {
              if(super.F != 0) {
                 this.$12();
              }
        
              this.$4();
           }
        
           public void $6() {
              super.B = 1;
              this.$10();
           }
        
           public void $8() {
              super.B = 2;
              this.$10();
           }
        
           public void $10() {
              super.C = 1;
              this.$3();
           }
        }
        """, resultingJava);

    Assert.assertEquals(7, routines.size());
    Routine routine0 = routines.get(0);
    assertBlockAddresses(routine0.getBlocks().get(0), 0, 2);
//    assertBlockAddresses(routines.get(1).getBlocks().get(0), 6, 6);
//    assertBlockAddresses(routines.get(2).getBlocks().get(0), 7, 9);
//    assertBlockAddresses(routines.get(3).getBlocks().get(0), 10, 13);
  }


}
