package com.fpetrola.z80.instructions.tests;

import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.bytecode.examples.RemoteZ80Translator;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.StackAnalyzer;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import jakarta.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.stream.Stream;

@SuppressWarnings("ALL")
@RunWith(TestRunner.class)
@Modules(RoutinesModule.class)
public class RecordedProgramTests {
  private static final int START = 0x8000, STACK = 0xFF00;
  private final RealCodeBytecodeCreationBase base;

  @Inject
  public RecordedProgramTests(RoutinesDriverConfigurator configurator) {
    base = configurator.getRealCodeBytecodeCreationBase();
  }

  @Before
  public void setUp() {
    Helper.hex = true;
  }

  @After
  public void tearDown() {
    Helper.hex = false;
  }

  private String translate(int[]... chunks) {
    return translateWithBlock(null, chunks);
  }

  private String translateWithBlock(int[] block, int[]... chunks) {
    int[] memory = new int[0x10000];
    for (int[] chunk : chunks)
      for (int i = 1; i < chunk.length; i++)
        memory[chunk[0] + i - 1] = chunk[i];
    RemoteZ80Translator.emulateProgram(base, memory, START, STACK);
    base.exploreRecording(RemoteZ80Translator.footprint(stackAnalyzer -> EmulatedMiniZX.ofProgram(memory, START, STACK, 1000, stackAnalyzer), START), START);
    StackAnalyzer stackAnalyzer = base.getStackAnalyzer();
    RoutineManager routineManager = base.getRoutineManager();
    if (block != null) {
      RemoteZ80Translator.recordBlockContents(EmulatedMiniZX.ofProgram(memory, START, STACK, 1000, null), START, stackAnalyzer.codeVersions);
      base.translateCodeVariants(block[0], block[1], 0xE000, stackAnalyzer.codeVersions);
    }
    return base.generateAndDecompile("", routineManager.getRoutines(), ".", "Program", base.symbolicExecutionAdapter);
  }

  private static int[] at(int address, int... bytes) {
    int[] chunk = new int[bytes.length + 1];
    chunk[0] = address;
    System.arraycopy(bytes, 0, chunk, 1, bytes.length);
    return chunk;
  }

  @Test
  public void aCallWhoseTargetIsRewrittenSwitchesOverTheRecordedTargets() {
    // Dizzy E299, Equinox D015
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x23, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76),
        at(0x8018, 0xCD, 0x00, 0x00, 0xC9),
        at(0x8020, 0x06, 0x01, 0xC9, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.HL('\\u8020');
              int var1 = this.HL();
              this.wMem16('\\u8019', var1, '\\u8003');
              this.$8018();
              this.HL('\\u8023');
              int var2 = this.HL();
              this.wMem16('\\u8019', var2, '\\u800c');
              this.$8018();
              this.halt('\\u8012');
              this.untranslated('\\u8013');
           }

           public void $8018() {
              int var1 = this.codeHash('\\u8018', 3);
              if(var1 == 227916) {
                 this.$8020();
              } else if(var1 == 228009) {
                 this.$8023();
              } else {
                 int var2 = this.executeMutantCode('\\u8018');
                 if(var2 != '\\u801b') {
                    this.jump(var2);
                    return;
                 }
              }

           }

           public void $8020() {
              super.B = 1;
           }

           public void $8023() {
              super.C = 2;
           }
        }
        """, java);
  }

  @Test
  public void anOpcodeThatIsRewrittenSwitchesOverTheRecordedOpcodes() {
    // Equinox D035/D0EE INC E <-> INC D, Emlyn 9ACE RLCA <-> RRCA
    String java = translate(
        at(0x8000, 0x3E, 0x1C, 0x32, 0x18, 0x80, 0xCD, 0x18, 0x80, 0x3E, 0x14, 0x32, 0x18, 0x80, 0xCD, 0x18, 0x80, 0x76),
        at(0x8018, 0x00, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              super.A = 28;
              this.wMem('\\u8018', super.A, '\\u8002');
              this.$8018();
              super.A = 20;
              this.wMem('\\u8018', super.A, '\\u800a');
              this.$8018();
              this.halt('\\u8010');
              this.untranslated('\\u8011');
           }

           public void $8018() {
              int var1 = this.codeHash('\\u8018', 1);
              if(var1 == 59) {
                 int var4 = this.alu("inc", super.E);
                 super.E = var4;
              } else if(var1 == 51) {
                 int var3 = this.alu("inc", super.D);
                 super.D = var3;
              } else {
                 int var2 = this.executeMutantCode('\\u8018');
                 if(var2 != '\\u8019') {
                    this.jump(var2);
                    return;
                 }
              }

           }
        }
        """, java);
  }

  @Test
  public void anOperandThatIsRewrittenIsReadFromMemory() {
    // Equinox 838E CP n, Emlyn AB94
    String java = translate(
        at(0x8000, 0x3E, 0x05, 0x32, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x3E, 0x07, 0x32, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76),
        at(0x8018, 0xFE, 0x00, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              super.A = 5;
              this.wMem('\\u8019', super.A, '\\u8002');
              this.$8018();
              super.A = 7;
              this.wMem('\\u8019', super.A, '\\u800a');
              this.$8018();
              this.halt('\\u8010');
              this.untranslated('\\u8011');
           }

           public void $8018() {
              int var1 = this.mem('\\u8019', '\\u8018');
              this.alu("cp", super.A, var1);
           }
        }
        """, java);
  }

  @Test
  public void aConditionalJumpWhoseTargetIsRewrittenSwitchesOverTheTargets() {
    // Equinox CEC1 JP C,nn
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x1A, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x23, 0x80, 0x22, 0x1A, 0x80, 0xCD, 0x18, 0x80, 0x76),
        at(0x8018, 0x37, 0xDA, 0x00, 0x00, 0xC9),
        at(0x8020, 0x06, 0x01, 0xC9, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.HL('\\u8020');
              int var1 = this.HL();
              this.wMem16('\\u801a', var1, '\\u8003');
              this.$8018();
              this.HL('\\u8023');
              int var2 = this.HL();
              this.wMem16('\\u801a', var2, '\\u800c');
              this.$8018();
              this.halt('\\u8012');
              this.untranslated('\\u8013');
           }

           public void $8018() {
              this.alu("scf", super.A);
              int var1 = this.codeHash('\\u8019', 3);
              if(var1 == 240409) {
                 if(this.flag(1, false)) {
                    this.$8020();
                    return;
                 }
              } else if(var1 == 240502) {
                 if(this.flag(1, false)) {
                    this.$8023();
                    return;
                 }
              } else {
                 int var2 = this.executeMutantCode('\\u8019');
                 if(var2 != '\\u801c') {
                    this.jump(var2);
                    return;
                 }
              }

           }

           public void $8020() {
              super.B = 1;
           }

           public void $8023() {
              super.C = 2;
           }
        }
        """, java);
  }

  @Test
  public void twoPopsOfReturnAddressesUnwindTwoLevels() {
    // JSW 37046/37047: JP Z into POP HL; POP HL when Willy dies
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0xE1, 0xE1, 0x0E, 0x02, 0x76),
        at(0x8010, 0xCD, 0x18, 0x80, 0xC9),
        at(0x8018, 0xAF, 0xCA, 0x06, 0x80, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        import com.fpetrola.z80.minizx.StackException;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              try {
                 this.$8010();
              } catch (StackException var2) {
                 if(var2.getNextPC() != '\\u8008') {
                    throw var2;
                 }

                 this.HL('\\u8003');
              }

              super.C = 2;
              this.halt('\\u800a');
              this.untranslated('\\u800b');
           }

           public void $8010() {
              try {
                 this.$8018();
              } catch (StackException var2) {
                 if(var2.getNextPC() == '\\u8007') {
                    this.HL('\\u8013');
                    throw new StackException('\\u8008');
                 } else {
                    throw var2;
                 }
              }
           }

           public void $8018() {
              int var1 = this.alu("xor", super.A, super.A);
              super.A = var1;
              if(this.flag(64, false)) {
                 throw new StackException('\\u8007');
              }
           }
        }
        """, java);
  }

  @Test
  public void aRoutineThatSkipsTheDataAfterItsCallReturnsPastIt() {
    // Emlyn 721D/7218: text after the CALL, ending the routine with JP (HL)
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x2A, 0x06, 0x01, 0x76),
        at(0x8010, 0xE1, 0x23, 0xE9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.push('\\u8003');
              this.$8010();
              super.B = 1;
              this.halt('\\u8006');
              this.untranslated('\\u8007');
           }

           public void $8010() {
              int var1 = this.pop();
              this.HL(var1);
              int var2 = this.HL();
              int var3 = this.inc16(var2);
              this.HL(var3);
           }
        }
        """, java);
  }

  @Test
  public void aRetAfterPushingAnAddressJumpsThere() {
    // Dizzy: jump table by return address
    String java = translate(
        at(0x8000, 0x21, 0x08, 0x80, 0xE5, 0xC9),
        at(0x8008, 0x06, 0x01, 0x76));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.HL('\\u8008');
              int var1 = this.HL();
              this.push(var1);
              int var2 = this.pop();
              if(var2 != '\\u8008') {
                 this.jump(var2);
              } else {
                 super.B = 1;
                 this.halt('\\u800a');
                 this.untranslated('\\u800b');
              }
           }
        }
        """, java);
  }

  @Test
  public void aCallToAJumpThroughHlCallsTheTarget() {
    // Emlyn menu: CALL 162C, the ROM JP (HL)
    String java = translate(
        at(0x8000, 0x21, 0x08, 0x80, 0xCD, 0x00, 0x4F, 0x76),
        at(0x8008, 0x06, 0x01, 0xC9),
        at(0x4F00, 0xE9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.HL('\\u8008');
              int var1 = this.HL();
              if(var1 == '\\u8008') {
                 this.$8008();
              } else {
                 this.jump(var1);
              }

              this.halt('\\u8006');
              this.$8008();
           }

           public void $8008() {
              super.B = 1;
           }
        }
        """, java);
  }

  @Test
  public void aPushedAddressIsAPlantedContinuation() {
    // Emlyn 616E plants 660D
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76),
        at(0x8010, 0x21, 0x18, 0x80, 0xE5, 0xC3, 0x1C, 0x80),
        at(0x8018, 0x06, 0x01, 0xC9),
        at(0x801C, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.$8010();
              this.halt('\\u8003');
              this.$8010();
           }

           public void $8010() {
              this.HL('\\u8018');
              int var1 = this.HL();
              this.push(var1);
              super.C = 2;
              int var2 = this.pop();
              if(var2 != '\\u8018') {
                 this.jump(var2);
              } else {
                 super.B = 1;
              }
           }
        }
        """, java);
  }

  @Test
  public void resettingTheStackReturnsTwoLevels() {
    // Dizzy F877: LD SP,nn; RET
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76),
        at(0x8010, 0xCD, 0x18, 0x80, 0xC9),
        at(0x8018, 0x31, 0xFE, 0xFE, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        import com.fpetrola.z80.minizx.StackException;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              try {
                 this.push('\\u8003');
                 this.$8010();
                 this.pop();
              } catch (StackException var2) {
                 if(var2.getNextPC() != '\\u8003') {
                    throw var2;
                 }
              }

              this.halt('\\u8003');
              this.$8010();
           }

           public void $8010() {
              this.$8018();
           }

           public void $8018() {
              this.SP('\\ufefe');
              this.$801B();
           }

           public void $801B() {
              while(true) {
                 try {
                    if(!this.isNextPC('\\u801b')) {
                       ;
                    }

                    int var1 = this.pop();
                    throw new StackException(var1);
                 } catch (StackException var4) {
                    int[] var3 = new int[]{'\\u801b'};
                    if(!this.isOwnAddress(var4, var3)) {
                       throw var4;
                    }
                 }
              }
           }
        }
        """, java);
  }

  @Test
  public void aRewrittenCallWhoseTargetPopsItsReturnAddressReturnsToTheCallerOfTheCall() {
    // Equinox D015 -> D08E: POP AF of the return address, RET one level up
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x28, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76),
        at(0x8018, 0xCD, 0x00, 0x00, 0x06, 0x05, 0xC9),
        at(0x8020, 0xF1, 0x0E, 0x01, 0xC9),
        at(0x8028, 0x16, 0x02, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;
        import com.fpetrola.z80.minizx.StackException;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.HL('\\u8020');
              int var1 = this.HL();
              this.wMem16('\\u8019', var1, '\\u8003');

              try {
                 this.push('\\u8009');
                 this.$8018();
                 this.pop();
              } catch (StackException var6) {
                 if(var6.getNextPC() != '\\u8009') {
                    throw var6;
                 }
              }

              this.HL('\\u8028');
              int var3 = this.HL();
              this.wMem16('\\u8019', var3, '\\u800c');

              try {
                 this.push('\\u8012');
                 this.$8018();
                 this.pop();
              } catch (StackException var5) {
                 if(var5.getNextPC() != '\\u8012') {
                    throw var5;
                 }
              }

              this.halt('\\u8012');
              this.untranslated('\\u8013');
           }

           public void $8018() {
              try {
                 int var2 = this.codeHash('\\u8018', 3);
                 if(var2 == 227916) {
                    this.$8020();
                 } else if(var2 == 228164) {
                    this.$8028();
                 } else {
                    int var3 = this.executeMutantCode('\\u8018');
                    if(var3 != '\\u801b') {
                       this.jump(var3);
                       return;
                    }
                 }
              } catch (StackException var5) {
                 if(var5.getNextPC() == '\\u8021') {
                    this.AF('\\u801b');
                    this.$8021();
                    return;
                 }

                 throw var5;
              }

              super.B = 5;
              int var4 = this.pop();
              throw new StackException(var4);
           }

           public void $8020() {
              throw new StackException('\\u8021');
           }

           public void $8021() {
              super.C = 1;
           }

           public void $8028() {
              super.D = 2;
           }
        }
        """, java);
  }

  @Test
  public void aBlockRewrittenWithInstructionsOfOtherLengthsRunsFromACopyPerRecordedShape() {
    // Emlyn 9AF7 line drawer and 9BBF template
    String java = translateWithBlock(new int[]{0x8018, 0x801C},
        at(0x8000, 0x21, 0x3C, 0x3C, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0xC6, 0x05, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76),
        at(0x8018, 0x47, 0x00, 0x00, 0xC9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.HL(15420);
              int var1 = this.HL();
              this.wMem16('\\u8019', var1, '\\u8003');
              this.$8018();
              this.HL(1478);
              int var2 = this.HL();
              this.wMem16('\\u8019', var2, '\\u800c');
              this.$8018();
              this.halt('\\u8012');
              this.untranslated('\\u8013');
           }

           public void $8018() {
              int var1 = this.codeHash('\\u8019', 2);
              if(var1 == 2881) {
                 this.$E000();
              } else if(var1 == 7104) {
                 this.$E005();
              } else {
                 this.unknownCodeVariant('\\u8018', '\\u8019', 2);
                 super.B = super.A;
                 int var2 = this.alu("inc", super.A);
                 super.A = var2;
                 int var3 = this.alu("inc", super.A);
                 super.A = var3;
              }
           }

           public void $E000() {
              super.B = super.A;
              int var1 = this.alu("inc", super.A);
              super.A = var1;
              int var2 = this.alu("inc", super.A);
              super.A = var2;
           }

           public void $E005() {
              super.B = super.A;
              int var1 = this.alu("add", super.A, 5);
              super.A = var1;
           }
        }
        """, java);
  }

  @Test
  public void aCallToAJumpThroughHlInTheGameCallsTheTarget() {
    // like Emlyn's CALL 162C, but with the JP (HL) in the game's own code
    String java = translate(
        at(0x8000, 0x21, 0x08, 0x80, 0xCD, 0x30, 0x80, 0x76),
        at(0x8008, 0x06, 0x01, 0xC9),
        at(0x8030, 0xE9));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.SpectrumApplication;

        public class Program extends SpectrumApplication {
           public void $0() {
           }

           public void $8000() {
              this.HL('\\u8008');
              this.$8030();
              this.halt('\\u8006');
              this.$8008();
           }

           public void $8008() {
              super.B = 1;
           }

           public void $8030() {
              if(this.HL() == '\\u8008') {
                 this.$8008();
              } else {
                 int var1 = this.HL();
                 this.jump(var1);
              }
           }
        }
        """, java);
  }

  @Test(timeout = 60000)
  public void aDeepChainOfRoutinesThatCallTheNextOneTwiceIsExploredInLinearTime() {
    // without the recording fence the SE reaches many shared routines; isPending must not walk every path
    int levels = 24;
    int[][] chunks = new int[levels + 1][];
    for (int level = 0; level < levels; level++) {
      int next = START + 8 * (level + 1);
      chunks[level] = at(START + 8 * level, 0xCD, next & 0xFF, next >> 8, 0xCD, next & 0xFF, next >> 8, 0xC9);
    }
    chunks[levels] = at(START + 8 * levels, 0xC9);
    String java = translate(chunks);
    Assert.assertEquals(levels + 2, java.split("public void \\$").length - 1);
  }
}
