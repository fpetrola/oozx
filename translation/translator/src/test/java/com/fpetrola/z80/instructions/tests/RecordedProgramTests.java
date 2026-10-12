package com.fpetrola.z80.instructions.tests;

import com.fpetrola.z80.bytecode.BytecodeGeneration;
import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.routines.CodeVersions;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.bytecode.examples.RemoteZ80Translator;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.base.ToStringInstructionVisitor;
import com.fpetrola.z80.cpu.DefaultInstructionFetcher;
import com.fpetrola.z80.cpu.ReadOnlyIOImplementation;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.minizx.MiniZXSound;
import com.fpetrola.z80.tstates.UncontendedTiming;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.se.actions.ChangeStackCallAddressAction;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.transformations.StackAnalyzer;
import io.exemplary.guice.Modules;
import io.exemplary.guice.TestRunner;
import jakarta.inject.Inject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.inject.Guice;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SuppressWarnings("ALL")
@RunWith(TestRunner.class)
@Modules(RoutinesModule.class)
public class RecordedProgramTests {
  private static final int START = 0x8000, STACK = 0xFF00;
  private RealCodeBytecodeCreationBase base;
  private final Set<Integer> ignoredMemory = new java.util.HashSet<>();
  private int interruptEvery, start = START, stack = STACK, maxSteps = 1000;
  private boolean banked;
  private boolean fallsBackToTheEmulator;
  private RoutineManager routineManager;
  private StackAnalyzer stackAnalyzer;
  private CodeVersions versions;
  private State emulated;

  @Inject
  public RecordedProgramTests(RoutinesDriverConfigurator configurator) {
    base = configurator.getRealCodeBytecodeCreationBase();
  }

  @Before
  public void setUp() {
    Helper.hex = true;
    System.setProperty("minizx.headless", "true");
    SpectrumApplication.io = new DefaultMiniZXIO();
  }

  @After
  public void tearDown() {
    Helper.hex = false;
  }

  private String translate(int[]... chunks) {
    return translateWithBlock(null, chunks);
  }

  private String translateFrom(int start, int[]... chunks) {
    this.start = start;
    return translate(chunks);
  }

  /** Starts at {@code start} with the stack where the game has it there. */
  private String translateFrom(int start, int stack, int[]... chunks) {
    this.stack = stack;
    return translateFrom(start, chunks);
  }

  private static int[] memoryOf(int[]... chunks) {
    int[] memory = new int[0x10000];
    for (int[] chunk : chunks)
      for (int i = 1; i < chunk.length; i++)
        memory[chunk[0] + i - 1] = chunk[i];
    return memory;
  }

  private String translateWithBlock(int[] block, int[]... chunks) {
    int[] memory = memoryOf(chunks);
    String image = RemoteZ80Translator.emulateProgram(base, memory, start, stack);
    base.exploreRecording(RemoteZ80Translator.footprint(stackAnalyzer -> machine(EmulatedMiniZX.ofProgram(memory, start, stack, 1000, stackAnalyzer).interruptingEvery(interruptEvery)), start), start);
    stackAnalyzer = base.getStackAnalyzer();
    routineManager = base.getRoutineManager();
    versions = stackAnalyzer.codeVersions;
    if (block != null) {
      RemoteZ80Translator.recordBlockContents(EmulatedMiniZX.ofProgram(memory, start, stack, 1000, null), start, versions);
      base.translateCodeVariants(block[0], block[1], 0xE000, versions);
    }
    String java = base.generateAndDecompile(image, routineManager.getRoutines(), ".", "Program", base.symbolicExecutionAdapter);
    assertRunsLikeTheEmulator(BytecodeGeneration.translatedProgram("Program", read(Path.of("Program.class"))), memory);
    assertRunsLikeTheEmulator(compiled(java), memory);
    return java;
  }

  private EmulatedMiniZX machine(EmulatedMiniZX emulator) {
    return banked ? emulator.banked() : emulator;
  }

  private static MiniZX compiled(String java) {
    try {
      Path dir = Files.createTempDirectory("recorded-program");
      Path source = Files.writeString(dir.resolve("Program.java"), java);
      Assert.assertEquals("the decompiled source compiles", 0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-cp", System.getProperty("java.class.path"), "-d", dir.toString(), source.toString()));
      byte[] bytecode = read(dir.resolve("Program.class"));
      Files.delete(dir.resolve("Program.class"));
      Files.delete(source);
      Files.delete(dir);
      return BytecodeGeneration.translatedProgram("Program", bytecode);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static byte[] read(Path path) {
    try {
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private String routines() {
    return routineManager.getRoutines().stream().map(Routine::getEntryPoint).sorted().map(Integer::toHexString).map(String::toUpperCase).collect(Collectors.joining(" "));
  }

  private String instructionAt(int address) {
    Instruction instruction = routineManager.getInstructionAt(address);
    Class<?> type = instruction == null ? null : instruction.getClass();
    while (type != null && type.getSimpleName().isEmpty())
      type = type.getSuperclass();
    return type == null ? null : type.getSimpleName();
  }

  private static class Finished extends RuntimeException {
  }

  private void savesStackPointerAt(int address) {
    ignoresMemory(address, address + 1);
  }

  private void ignoresMemory(int from, int to) {
    IntStream.rangeClosed(from, to).forEach(ignoredMemory::add);
  }

  private void assertRunsLikeTheEmulator(MiniZX program, int[] memory) {
    EmulatedMiniZX emulator = machine(EmulatedMiniZX.ofProgram(memory, start, stack, 0, null).timed());
    emulator.start();
    State z80 = emulator.ooz80.getState(), translated = EmulatedMiniZX.createOOZ80(new DefaultMiniZXIO()).getState();
    program.loadState(z80);
    int[] previous = {-1}, steps = {0}, nextInterrupt = {interruptEvery}, fetched = {0}, lastR = {z80.getRegisterR().read()};
    program.setInterruptionCondition(fetches -> {
      if (previous[0] >= 0) {
        emulator.ooz80.execute();
        for (int k = 0; k < 0x10000 && z80.getPc().read() == previous[0] && program.PC != previous[0]; k++)
          emulator.ooz80.execute();
      }
      fetched[0] += z80.getRegisterR().read() - lastR[0] & 0x7f;
      lastR[0] = z80.getRegisterR().read();
      if (fetches != fetched[0] && previous[0] >= 0)
        throw new IllegalStateException("after $%04X: fetches java=%d z80=%d".formatted(previous[0], fetches, fetched[0]));
      program.storeRegisters(translated);
      translated.getPc().write(program.PC);
      if (!registers(z80).equals(registers(translated)))
        throw new IllegalStateException("after $%04X\n z80  %s\n java %s".formatted(previous[0], registers(z80), registers(translated)));
      if ((program.tstates & 0xFFFFF) != z80.clock.getTStates())
        throw new IllegalStateException("after $%04X: T-states java=%d z80=%d".formatted(previous[0], program.tstates & 0xFFFFF, z80.clock.getTStates()));
      if (steps[0] % 50 == 0) {
        long nested = Arrays.stream(Thread.currentThread().getStackTrace()).filter(f -> f.getMethodName().startsWith("$")).count();
        int stacked = (stack - z80.getRegisterSP().read() & 0xffff) / 2;
        if (nested > stacked + 8)
          throw new IllegalStateException("after $%04X: %d translated routines nested in Java for %d words on the Z80 stack: a jump became a call".formatted(previous[0], nested, stacked));
      }
      if (z80.getMemory().getData()[z80.getPc().read()] == 0x76 || ++steps[0] == maxSteps)
        throw new Finished();
      previous[0] = program.PC;
      if (interruptEvery > 0 && fetches >= nextInterrupt[0] && z80.isIff1()) {
        nextInterrupt[0] = fetches + interruptEvery;
        emulator.ooz80.interruption();
        previous[0] = -1;
        return true;
      }
      return false;
    });
    try {
      program.run(start);
    } catch (Finished finished) {
    }
    emulated = z80;
    Assert.assertEquals("the recorded path falls back to the emulator", fallsBackToTheEmulator, mutantExecutorOf(program) != null);
    int[] expected = z80.getMemory().getData();
    Assert.assertEquals("", IntStream.range(0, 0x10000).filter(a -> (a < stack - 0x100 || a >= stack + 0x10) && !ignoredMemory.contains(a) && program.mem[a] != expected[a]).limit(8).mapToObj(a -> "%04X z80 %02X java %02X ".formatted(a, expected[a], program.mem[a])).collect(Collectors.joining()));
  }

  private static Object mutantExecutorOf(MiniZX program) {
    try {
      Field field = SpectrumApplication.class.getDeclaredField("mutantExecutor");
      field.setAccessible(true);
      return field.get(program);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private int endValue(String register) {
    return emulated.getRegister(RegisterName.valueOf(register)).read();
  }

  private static String registers(State state) {
    return "PC=%04X".formatted(state.getPc().read()) + Stream.of("AF", "BC", "DE", "HL", "AFx", "BCx", "DEx", "HLx", "IX", "IY", "R")
        .map(name -> " %s=%04X".formatted(name, state.getRegister(RegisterName.valueOf(name)).read() & (name.startsWith("AF") ? 0xffd7 : 0xffff)))
        .collect(Collectors.joining());
  }

  /** Z80 code as its listing, one instruction a line: address, bytes, and what they say, which must be what the decoder reads in them; the routines at {@code returning} only return. */
  private static int[][] listing(String text, int... returning) {
    List<int[]> chunks = new ArrayList<>();
    for (int address : returning)
      chunks.add(at(address, 0xC9));
    int[] image = new int[0x10000];
    for (String line : text.strip().split("\n")) {
      String[] columns = line.strip().split("\t+| {2,}");
      int address = Integer.parseInt(columns[0], 16);
      int[] bytes = Arrays.stream(columns[1].split(" ")).mapToInt(b -> Integer.parseInt(b, 16)).toArray();
      for (int i = 0; i < bytes.length; i++)
        image[address + i & 0xffff] = bytes[i];
      chunks.add(at(address, bytes));
    }
    State reading = new State(new ReadOnlyIOImplementation(null), new Memory() {
      public int read(int address, int fetching) {
        return image[address & 0xffff];
      }

      public void write(int address, int value) {
      }

      public void reset() {
      }
    });
    DefaultInstructionFetcher decoder = new DefaultInstructionFetcher(reading, false, false);
    for (String line : text.strip().split("\n")) {
      String[] columns = line.strip().split("\t+| {2,}");
      reading.getPc().write(Integer.parseInt(columns[0], 16));
      Assert.assertEquals(line, columns[2], new ToStringInstructionVisitor().createToString(decoder.fetchNextInstruction()).strip());
    }
    return chunks.toArray(int[][]::new);
  }

  /** The code and data from C000 up run in {@code bank}: the program at 8000 pages it, copies each piece there from A000 up, calls {@code entry} and pages bank 0 back. */
  private int[][] inBank(int bank, int entry, int[][] chunks) {
    banked = true;
    List<int[]> program = new ArrayList<>(List.of(0x01, 0xFD, 0x7F, 0x3E, 0x10 | bank, 0xED, 0x79).stream().map(b -> new int[]{b}).toList());
    List<int[]> pieces = new ArrayList<>();
    int source = 0xA000;
    for (int[] chunk : chunks) {
      if (chunk[0] < 0xC000) {
        pieces.add(chunk);
        continue;
      }
      int length = chunk.length - 1;
      program.add(new int[]{0x21, source & 0xff, source >> 8, 0x11, chunk[0] & 0xff, chunk[0] >> 8, 0x01, length & 0xff, length >> 8, 0xED, 0xB0});
      int[] copy = chunk.clone();
      copy[0] = source;
      pieces.add(copy);
      source += length;
    }
    program.add(new int[]{0xCD, entry & 0xff, entry >> 8, 0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0x18, 0xFE});
    pieces.add(at(START, program.stream().flatMapToInt(Arrays::stream).toArray()));
    return pieces.toArray(int[][]::new);
  }

  /** The code of a listing with data around it. */
  private static int[][] with(int[][] code, int[]... data) {
    return Stream.concat(Arrays.stream(code), Arrays.stream(data)).toArray(int[][]::new);
  }

  /** The translation as a reader follows it: without the clock calls every instruction makes, the memory image and the empty routines, with addresses in hex. */
  private static String withoutTiming(String java) {
    String code = java.replaceAll("(?s)   public String getProgramBytes\\(\\) \\{.*?\\n   \\}\\n\\n", "").lines()
        .filter(line -> !line.contains("this.pc(") && !line.contains("this.tstates(")).collect(Collectors.joining("\n", "", "\n"))
        .replaceAll("\n   public void \\$\\w+\\(\\) \\{\n   \\}\n", "");
    code = Pattern.compile("'\\\\u([0-9a-f]{4})'").matcher(code).replaceAll(m -> "0x" + m.group(1).toUpperCase());
    code = Pattern.compile("new int\\[\\]\\{([\\d, ]+)\\}").matcher(code)
        .replaceAll(m -> "new int[]{" + Arrays.stream(m.group(1).split(", ")).map(n -> "0x%04X".formatted(Integer.parseInt(n))).collect(Collectors.joining(", ")) + "}");
    return Pattern.compile("((?:unwind|isNextPC|setNextAddress)\\()(\\d+)(?=[,)])").matcher(code)
        .replaceAll(m -> m.group(1) + "0x%04X".formatted(Integer.parseInt(m.group(2))));
  }

  private static int[] at(int address, int... bytes) {
    int[] chunk = new int[bytes.length + 1];
    chunk[0] = address;
    System.arraycopy(bytes, 0, chunk, 1, bytes.length);
    return chunk;
  }

  @Test
  public void greatEscape691AGoesBackToTheMainLoopDroppingTheCallThatGotThere() {
    // Great Escape 691A: LD SP,FFFF / JP 9D7B from deep inside the main loop's calls; 9D9F's call is still pending
    ignoresMemory(0xFF00, 0xFFFF);
    String java = translateFrom(0x691A, listing("""
        691A  31 FF FF  LD SP, 0xFFFF
        691D  C3 7B 9D  JP 0x9D7B
        68F2  18 26  JR 0x691A
        9D7B  CD CF 9D  CALL 0x9DCF
        9D7E  CD E5 9D  CALL 0x9DE5
        9D81  CD 48 7D  CALL 0x7D48
        9D84  CD 07 9E  CALL 0x9E07
        9D87  CD 21 9F  CALL 0x9F21
        9D8A  CD 98 BB  CALL 0xBB98
        9D8D  CD A0 C6  CALL 0xC6A0
        9D90  CD 92 C8  CALL 0xC892
        9D93  CD 7E C4  CALL 0xC47E
        9D96  CD 1C C4  CALL 0xC41C
        9D99  CD 9E DB  CALL 0xDB9E
        9D9C  CD 9E A0  CALL 0xA09E
        9D9F  CD CE B5  CALL 0xB5CE
        B5CE  C3 F2 68  JP 0x68F2
        """, 0x9DCF, 0x9DE5, 0x7D48, 0x9E07, 0x9F21, 0xBB98, 0xC6A0, 0xC892, 0xC47E, 0xC41C, 0xDB9E, 0xA09E));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $691A() {
              int[] var1;
              do {
                 if(!this.isNextPC(0x691D)) {
                    this.SP(0xFFFF);
                 }

                 this.$9DCF();
                 this.$9DE5();
                 this.$7D48();
                 this.$9E07();
                 this.$9F21();
                 this.$BB98();
                 this.$C6A0();
                 this.$C892();
                 this.$C47E();
                 this.$C41C();
                 this.$DB9E();
                 this.$A09E();
                 this.$B5CE();
                 if(!this.unwinding()) {
                    this.$9DCF();
                    return;
                 }

                 var1 = new int[]{0x691A, 0x691D};
              } while(this.isOwnAddress(var1));

           }

           public void $B5CE() {
              this.SP(0xFFFF);
              this.unwind(0x691D);
           }

           public void $691D() {
              this.setNextAddress(0x691D);
              this.$691A();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void renegade840CGoesBackToTheMainLoopDroppingTheCallThatGotThere() {
    // Renegade 840C: LD SP,BDFF restarts the game loop; the CALL 883E at 8464 is still pending (883E stands for the way back to 8400, the loop at 8412 ends the scene)
    ignoresMemory(0xBD00, 0xBDFF);
    String java = translateFrom(0x8457, 0xBDFF, listing("""
        8457  CD 99 9D  CALL 0x9D99
        845A  21 F1 88  LD HL, 0x88F1
        845D  22 6C 89  LD (0x896C), HL
        8460  FD 21 00 BF  LD IY, 0xBF00
        8464  CD 3E 88  CALL 0x883E
        883E  C3 00 84  JP 0x8400
        8400  22 55 84  LD (0x8455), HL
        8403  CD BD 9C  CALL 0x9CBD
        8406  21 66 89  LD HL, 0x8966
        8409  22 F3 F4  LD (0xF4F3), HL
        840C  31 FF BD  LD SP, 0xBDFF
        840F  CD 09 9D  CALL 0x9D09
        8412  18 FE  JR 0x8412
        """, 0x9D99, 0x9CBD, 0x9D09));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $840F() {
              this.$9D09();

              while(true) {
              }
           }

           public void $8457() {
              this.$9D99();
              this.HL(0x88F1);
              int var1 = this.HL();
              this.wMem16(0x896C, var1, 0x845D);
              this.IY(0xBF00);
              this.$883E();
              if(!this.unwinding()) {
                 this.$883E();
                 if(!this.unwinding()) {
                    ;
                 }
              }
           }

           public void $883E() {
              int var1 = this.HL();
              this.wMem16(0x8455, var1, 0x8400);
              this.$9CBD();
              this.HL(0x8966);
              int var2 = this.HL();
              this.wMem16(0xF4F3, var2, 0x8409);
              this.SP(0xBDFF);
              this.unwind(0x840F);
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void wallyEE9CGoesBackToTheMainLoopDroppingTheCallThatGotThere() {
    // Everyone's a Wally EE9C: LD SP,FC00 / JP 8194 back to the main loop; the CALL EABF at 8197 is still pending (EABF stands for the way to EE90, the loop at 819D ends the scene)
    ignoresMemory(0xFB00, 0xFC00);
    ignoresMemory(0x0000, 0x0046);
    String java = translateFrom(0x8194, 0xFC00, listing("""
        8194  CD 55 EB  CALL 0xEB55
        8197  CD BF EA  CALL 0xEABF
        819A  CD 06 AC  CALL 0xAC06
        EABF  C3 90 EE  JP 0xEE90
        EE90  FD 36 0A 98  LD (IY+0A), 0x98
        EE94  FD 36 46 00  LD (IY+46), 0x00
        EE98  FD 36 00 00  LD (IY+00), 0x00
        EE9C  31 00 FC  LD SP, 0xFC00
        EE9F  C3 94 81  JP 0x8194
        819D  18 FE  JR 0x819D
        """, 0xEB55, 0xAC06));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8194() {
              int[] var1;
              do {
                 if(this.isNextPC(0xEE9F)) {
                 }

                 this.$EB55();
                 this.$EABF();
                 if(!this.unwinding()) {
                    this.$AC06();

                    while(true) {
                    }
                 }

                 var1 = new int[]{0xEE9F};
              } while(this.isOwnAddress(var1));

           }

           public void $EABF() {
              int var1 = this.IY() + 10;
              this.wMem(var1, 152, 0xEE90);
              int var2 = this.IY() + 70;
              this.wMem(var2, 0, 0xEE94);
              int var3 = this.IY() + 0;
              this.wMem(var3, 0, 0xEE98);
              this.SP(0xFC00);
              this.unwind(0xEE9F);
           }

           public void $EE9F() {
              this.setNextAddress(0xEE9F);
              this.$8194();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void rainbowIslands7C13LeavesTheLoadersStackForItsOwn() {
    // Rainbow Islands 7C13: at start, SP goes from the loader's stack (5F38) to the game's (FDFD), then the code is copied into place; nothing is pending (the loop at 7419 ends the scene)
    ignoresMemory(0x5B00, 0x5F50);
    ignoresMemory(0xFD00, 0xFDFF);
    String java = translateFrom(0x7C12, 0x5F38, listing("""
        7C12  F3  DI
        7C13  31 FD FD  LD SP, 0xFDFD
        7C16  21 C2 77  LD HL, 0x77C2
        7C19  11 00 5B  LD DE, 0x5B00
        7C1C  01 50 04  LD BC, 0x0450
        7C1F  ED B0  LDIR
        7C21  C3 19 74  JP 0x7419
        7419  18 FE  JR 0x7419
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $7C12() {
              this.di();
              this.SP(0xFDFD);
              this.HL(30658);
              this.DE(23296);
              this.BC(1104);
              this.ldir(31775);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void headOverHeels70E6GoesBackToTheMainLoopDroppingTheCallThatGotThere() {
    // Head over Heels 70E6: LD SP,FFF4 / JP 7064 restarts the main loop; the CALL 93B0 at 7074 is still pending (93B0 stands for the way to 70E6, the loop at 7077 ends the scene)
    ignoresMemory(0xFF00, 0xFFFF);
    String java = translateFrom(0x7064, 0xFFF4, listing("""
        7064  AF  XOR A, A
        7065  32 3D 70  LD (0x703D), A
        7068  CD 91 7B  CALL 0x7B91
        706B  CD F1 70  CALL 0x70F1
        706E  CD 3F 71  CALL 0x713F
        7071  CD 95 70  CALL 0x7095
        7074  CD B0 93  CALL 0x93B0
        93B0  C3 E6 70  JP 0x70E6
        70E6  31 F4 FF  LD SP, 0xFFF4
        70E9  C3 64 70  JP 0x7064
        7077  18 FE  JR 0x7077
        """, 0x7B91, 0x70F1, 0x713F, 0x7095));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $7064() {
              int[] var5;
              do {
                 if(this.isNextPC(0x70E9)) {
                 }

                 int var1 = this.A();
                 int var2 = this.A();
                 int var3 = this.alu("xor", var2, var1);
                 this.A(var3);
                 int var4 = this.A();
                 this.wMem(28733, var4, 28773);
                 this.$7B91();
                 this.$70F1();
                 this.$713F();
                 this.$7095();
                 this.$93B0();
                 if(!this.unwinding()) {
                    while(true) {
                    }
                 }

                 var5 = new int[]{0x70E9};
              } while(this.isOwnAddress(var5));

           }

           public void $93B0() {
              this.SP(0xFFF4);
              this.unwind(0x70E9);
           }

           public void $70E9() {
              this.setNextAddress(0x70E9);
              this.$7064();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void renegade128At8011GoesBackToTheMainLoopDroppingTheCallThatGotThere() {
    // Renegade 128 8011: LD SP,BDFF restarts the game; the CALL 844F at 8075 is still pending (844F stands for the way to 8000, the loop at 8014 ends the scene)
    ignoresMemory(0xBD00, 0xBDFF);
    String java = translateFrom(0x8068, 0xBDFF, listing("""
        8068  CD 54 9B  CALL 0x9B54
        806B  21 FD 84  LD HL, 0x84FD
        806E  22 78 85  LD (0x8578), HL
        8071  FD 21 00 BF  LD IY, 0xBF00
        8075  CD 4F 84  CALL 0x844F
        844F  C3 00 80  JP 0x8000
        8000  22 66 80  LD (0x8066), HL
        8003  3E 03  LD A, 0x03
        8005  CD 58 9E  CALL 0x9E58
        8008  CD 78 9A  CALL 0x9A78
        800B  21 72 85  LD HL, 0x8572
        800E  22 53 7D  LD (0x7D53), HL
        8011  31 FF BD  LD SP, 0xBDFF
        8014  18 FE  JR 0x8014
        """, 0x9B54, 0x9E58, 0x9A78));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8014() {
              while(true) {
              }
           }

           public void $8068() {
              this.$9B54();
              this.HL(0x84FD);
              int var1 = this.HL();
              this.wMem16(0x8578, var1, 0x806E);
              this.IY(0xBF00);
              this.$844F();
              if(!this.unwinding()) {
                 this.$844F();
                 if(!this.unwinding()) {
                    ;
                 }
              }
           }

           public void $844F() {
              int var1 = this.HL();
              this.wMem16(0x8066, var1, 0x8000);
              this.A(3);
              this.$9E58();
              this.$9A78();
              this.HL(0x8572);
              int var2 = this.HL();
              this.wMem16(32083, var2, 0x800E);
              this.SP(0xBDFF);
              this.unwind(0x8014);
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void renegade128At7C46PagesBankZeroAndDropsTheDataAndTheCallBelow() {
    // Renegade 128 7C46: OUT (7FFD) pages bank 0, then LD SP,BDFF drops what the start left: the word PUSH BC put at 7C7A and the CALL 7C87 at 7C7B (7C87 and 9FDD stand for the ways back to 7C3E: the game starts over from more than one place; the game also pushes the return of the next CALL, which a POP takes later)
    ignoresMemory(0xBD00, 0xBDFF);
    String java = translateFrom(0x7C77, 0xBDFF, listing("""
        7C77  01 EE 02  LD BC, 0x02EE
        7C7A  C5  PUSH BC
        7C7B  CD 87 7C  CALL 0x7C87
        7C87  C3 3E 7C  JP 0x7C3E
        7C3E  AF  XOR A, A
        7C3F  D3 FE  OUT 1, A
        7C41  01 FD 7F  LD BC, 0x7FFD
        7C44  ED 79  OUT BC, A
        7C46  31 FF BD  LD SP, 0xBDFF
        7C49  CD DD 9F  CALL 0x9FDD
        9FDD  C3 3E 7C  JP 0x7C3E
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public int runJumps(int var1) {
              int var2 = var1;

              while(true) {
                 var2 = this.pagedCopy(var2);
                 switch(var2) {
                 case 31806:
                    var2 = this.$7C3E();
                    if(!this.unwinding()) {
                       break;
                    }

                    return var2;
                 case 31814:
                    var2 = this.$7C46();
                    if(!this.unwinding()) {
                       break;
                    }

                    return var2;
                 case 31863:
                    var2 = this.$7C77();
                    if(!this.unwinding()) {
                       break;
                    }

                    return var2;
                 case 31879:
                    var2 = this.$7C87();
                    if(this.unwinding()) {
                       return var2;
                    }
                    break;
                 default:
                    return var2;
                 }
              }
           }

           public int $7C3E() {
              int[] var8;
              do {
                 int var1 = this.A();
                 int var2 = this.A();
                 int var3 = this.alu("xor", var2, var1);
                 this.A(var3);
                 int var4 = this.A() << 8 | 254;
                 int var5 = this.A();
                 this.out(var4, var5);
                 this.BC(32765);
                 int var6 = this.BC();
                 int var7 = this.A();
                 this.out(var6, var7);
                 this.SP(0xBDFF);
                 this.unwind(0x7C49);
                 var8 = new int[]{0x7C3E};
              } while(this.isOwnAddress(var8));

              return -1;
           }

           public int $7C46() {
              int[] var1;
              do {
                 if(!this.isNextPC(0x7C49)) {
                    this.SP(0xBDFF);
                 }

                 this.$9FDD();
                 if(!this.unwinding()) {
                    return 31863;
                 }

                 var1 = new int[]{0x7C49};
              } while(this.isOwnAddress(var1));

              return -1;
           }

           public int $7C77() {
              this.BC(750);
              int var1 = this.BC();
              this.push(var1);
              this.runJumps(31879);
              if(this.unwinding()) {
                 return -1;
              } else {
                 return 31879;
              }
           }

           public int $7C87() {
              return 31806;
           }

           public void $9FDD() {
              this.runJumps(31806);
              if(!this.unwinding()) {
                 ;
              }
           }

           public void $7C49() {
              this.setNextAddress(0x7C49);
              this.runJumps(31814);
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void renegadeF28CCopiesThenGoesBackToTheMainLoopDroppingTheCallThatGotThere() {
    // Renegade F28C: after an LDIR, LD SP,BDFF restarts the game; the CALL F460 at F377 is still pending (F460 and 9E49 stand for the ways back to F280: the game starts over from more than one place; the game also pushes the return of the next CALL, which a POP takes later)
    ignoresMemory(0xBD00, 0xBDFF);
    String java = translateFrom(0xF36E, 0xBDFF, listing("""
        F36E  ED 53 F3 F4  LD (0xF4F3), DE
        F372  06 03  LD B, 0x03
        F374  D9  EXX
        F375  2E 05  LD L, 0x05
        F377  CD 60 F4  CALL 0xF460
        F460  C3 80 F2  JP 0xF280
        F280  F3  DI
        F281  11 60 5B  LD DE, 0x5B60
        F284  01 E0 01  LD BC, 0x01E0
        F287  21 00 FD  LD HL, 0xFD00
        F28A  ED B0  LDIR
        F28C  31 FF BD  LD SP, 0xBDFF
        F28F  3E 47  LD A, 0x47
        F291  CD 49 9E  CALL 0x9E49
        9E49  C3 80 F2  JP 0xF280
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public int runJumps(int var1) {
              int var2 = var1;

              while(true) {
                 var2 = this.pagedCopy(var2);
                 switch(var2) {
                 case 62080:
                    var2 = this.$F280();
                    if(!this.unwinding()) {
                       break;
                    }

                    return var2;
                 case 62092:
                    var2 = this.$F28C();
                    if(!this.unwinding()) {
                       break;
                    }

                    return var2;
                 case 62318:
                    var2 = this.$F36E();
                    if(!this.unwinding()) {
                       break;
                    }

                    return var2;
                 case 62560:
                    var2 = this.$F460();
                    if(this.unwinding()) {
                       return var2;
                    }
                    break;
                 default:
                    return var2;
                 }
              }
           }

           public void $9E49() {
              this.runJumps(0xF280);
              if(!this.unwinding()) {
                 ;
              }
           }

           public int $F280() {
              int[] var1;
              do {
                 this.di();
                 this.DE(23392);
                 this.BC(480);
                 this.HL(0xFD00);
                 this.ldir(0xF28A);
                 this.SP(0xBDFF);
                 this.unwind(0xF28F);
                 var1 = new int[]{0xF280};
              } while(this.isOwnAddress(var1));

              return -1;
           }

           public int $F28C() {
              int[] var1;
              do {
                 if(!this.isNextPC(0xF28F)) {
                    this.SP(0xBDFF);
                 }

                 this.A(71);
                 this.$9E49();
                 if(!this.unwinding()) {
                    return 0xF36E;
                 }

                 var1 = new int[]{0xF28F};
              } while(this.isOwnAddress(var1));

              return -1;
           }

           public int $F36E() {
              int var1 = this.DE();
              this.wMem16(0xF4F3, var1, 0xF36E);
              this.B(3);
              this.exx();
              this.L(5);
              this.runJumps(0xF460);
              if(this.unwinding()) {
                 return -1;
              } else {
                 return 0xF460;
              }
           }

           public int $F460() {
              return 0xF280;
           }

           public void $F28F() {
              this.setNextAddress(0xF28F);
              this.runJumps(0xF28C);
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void targetRenegade5DB4ProtectsACallBySavingAndRestoringSP() {
    // Target: Renegade 5DB4: LD (F6F6),SP / CALL C51A / LD SP,(F6F6) guards against a routine that leaves the stack untidy; only forks and the exploration run it (here behind the JP NZ the recording never takes; FA93 ends the scene). In the game C51A is bank 3's code, run untranslated, so the CALL there really pushes its return
    ignoresMemory(0xBF00, 0xBFC1);
    ignoresMemory(0xF6F6, 0xF6F7);
    String java = translateFrom(0x8000, 0xBFB8, listing("""
        8000  AF  XOR A, A
        8001  C2 B0 5D  JP NZ, 0x5DB0
        8004  18 FE  JR 0x8004
        5DB0  2A C0 BF  LD HL, (0xBFC0)
        5DB3  E5  PUSH HL
        5DB4  ED 73 F6 F6  LD (0xF6F6), SP
        5DB8  CD 1A C5  CALL 0xC51A
        5DBB  ED 7B F6 F6  LD SP, (0xF6F6)
        5DBF  E1  POP HL
        5DC0  22 C0 BF  LD (0xBFC0), HL
        5DC3  C3 93 FA  JP 0xFA93
        FA93  18 FE  JR 0xFA93
        """, 0xC51A));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int var1 = this.A();
              int var2 = this.A();
              int var3 = this.alu("xor", var2, var1);
              this.A(var3);
              if(!this.flag(64, true)) {
                 while(true) {
                 }
              }

              int var4 = this.mem16(0xBFC0, 23984);
              this.HL(var4);
              int var5 = this.HL();
              this.push(var5);
              int var6 = this.SP();
              this.wMem16(0xF6F6, var6, 23988);
              this.$C51A();
              int var7 = this.mem16(0xF6F6, 23995);
              this.SP(var7);
              int var8 = this.pop();
              this.HL(var8);
              int var9 = this.HL();
              this.wMem16(0xBFC0, var9, 24000);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void renegade128At9792DrawsASpriteWithTheStackAsItsPointer() {
    // Renegade 128 978D-97B0: saves SP at 70B3, points SP at the sprite (LD SP,HL), takes mask and pixels with POP HL, skips to the next row with ADD HL,SP / LD SP,HL and restores SP from 70B3 (the code at 8000 only sets up two rows of two bytes over FDxx)
    savesStackPointerAt(0x70B3);
    String java = translate(listing("""
        8000  D9  EXX
        8001  01 02 02  LD BC, 0x0202
        8004  11 00 20  LD DE, 0x2000
        8007  D9  EXX
        8008  21 00 90  LD HL, 0x9000
        800B  3E 02  LD A, 0x02
        800D  CD 8A 97  CALL 0x978A
        8010  18 FE  JR 0x8010
        978A  4F  LD C, A
        978B  16 FD  LD D, 0xFD
        978D  ED 73 B3 70  LD (0x70B3), SP
        9791  F3  DI
        9792  F9  LD SP, HL
        9793  D9  EXX
        9794  7B  LD A, E
        9795  D9  EXX
        9796  5F  LD E, A
        9797  41  LD B, C
        9798  E1  POP HL
        9799  1A  LD A, (DE)
        979A  A5  AND A, L
        979B  B4  OR A, H
        979C  12  LD (DE), A
        979D  1C  INC E
        979E  10 F8  DJNZ 0x9798
        97A0  D9  EXX
        97A1  69  LD L, C
        97A2  26 00  LD H, 0x00
        97A4  39  ADD HL, SP
        97A5  F9  LD SP, HL
        97A6  7B  LD A, E
        97A7  82  ADD A, D
        97A8  5F  LD E, A
        97A9  10 EA  DJNZ 0x9795
        97AB  ED 7B B3 70  LD SP, (0x70B3)
        97AF  FB  EI
        97B0  C9  RET
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.exx();
              this.BC(514);
              this.DE(8192);
              this.exx();
              this.HL(0x9000);
              this.A(2);
              this.$978A();

              while(true) {
              }
           }

           public void $978A() {
              int var1 = this.A();
              this.C(var1);
              this.D(253);
              int var2 = this.SP();
              this.wMem16(28851, var2, 0x978D);
              this.di();
              int var3 = this.HL();
              this.SP(var3);
              this.exx();
              int var4 = this.E();
              this.A(var4);

              while(true) {
                 this.exx();
                 int var5 = this.A();
                 this.E(var5);
                 int var6 = this.C();
                 this.B(var6);

                 while(true) {
                    int var7 = this.pop();
                    this.HL(var7);
                    int var8 = this.DE();
                    int var9 = this.mem(var8, 0x9799);
                    this.A(var9);
                    int var10 = this.L();
                    int var11 = this.A();
                    int var12 = this.alu("and", var11, var10);
                    this.A(var12);
                    int var13 = this.H();
                    int var14 = this.A();
                    int var15 = this.alu("or", var14, var13);
                    this.A(var15);
                    int var16 = this.A();
                    int var17 = this.DE();
                    this.wMem(var17, var16, 0x979C);
                    int var18 = this.E();
                    int var19 = this.alu("inc", var18);
                    this.E(var19);
                    int var20 = this.B() - 1 & 255;
                    this.B(var20);
                    if(this.B() == 0) {
                       this.exx();
                       int var21 = this.C();
                       this.L(var21);
                       this.H(0);
                       int var22 = this.SP();
                       int var23 = this.HL();
                       int var24 = this.alu("add16", var23, var22);
                       this.HL(var24);
                       int var25 = this.HL();
                       this.SP(var25);
                       int var26 = this.E();
                       this.A(var26);
                       int var27 = this.D();
                       int var28 = this.A();
                       int var29 = this.alu("add", var28, var27);
                       this.A(var29);
                       int var30 = this.A();
                       this.E(var30);
                       int var31 = this.B() - 1 & 255;
                       this.B(var31);
                       if(this.B() == 0) {
                          int var32 = this.mem16(28851, 0x97AB);
                          this.SP(var32);
                          this.ei();
                          return;
                       }

                       break;
                    }

                 }
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void zynaps8D60SwitchesBetweenTwoStacksThatSaveEachOther() {
    // Zynaps 8D60/8D69: each stack saves its SP and takes the other's (8018 holds the main one, 801A the coroutine's, which starts at 9000 with its own stack at 6FFE); the code at 8000 and 9000 only takes turns
    savesStackPointerAt(0x8018);
    savesStackPointerAt(0x801A);
    ignoresMemory(0x6F00, 0x6FFF);
    String java = translate(with(listing("""
        8000  0C  INC C
        8001  CD 60 8D  CALL 0x8D60
        8004  18 FA  JR 0x8000
        8D60  ED 73 18 80  LD (0x8018), SP
        8D64  ED 7B 1A 80  LD SP, (0x801A)
        8D68  C9  RET
        8D69  ED 73 1A 80  LD (0x801A), SP
        8D6D  ED 7B 18 80  LD SP, (0x8018)
        8D71  C9  RET
        9000  04  INC B
        9001  CD 69 8D  CALL 0x8D69
        9004  18 FA  JR 0x9000
        """), at(0x801A, 0xFE, 0x6F), at(0x6FFE, 0x00, 0x90)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              if(this.isNextPC(0x8004)) {
                 this.setNextAddress(0x8004);
              }

              while(true) {
                 if(!this.isNextPC(0x8004)) {
                    int var1 = this.C();
                    int var2 = this.alu("inc", var1);
                    this.C(var2);
                    this.$8D60();
                 }

              }
           }

           public void $8D60() {
              int var1 = this.SP();
              this.wMem16(0x8018, var1, 0x8D60);
              this.leavingStack();
              int var2 = this.mem16(0x801A, 0x8D64);
              this.SP(var2);
              this.switchStack();
           }

           public void $8D69() {
              int var1 = this.SP();
              this.wMem16(0x801A, var1, 0x8D69);
              this.leavingStack();
              int var2 = this.mem16(0x8018, 0x8D6D);
              this.SP(var2);
              this.switchStack();
           }

           public void $9000() {
              if(this.isNextPC(0x9004)) {
                 this.setNextAddress(0x9004);
              }

              while(true) {
                 if(!this.isNextPC(0x9004)) {
                    int var1 = this.B();
                    int var2 = this.alu("inc", var1);
                    this.B(var2);
                    this.$8D69();
                 }

              }
           }

           public void $8004() {
              this.setNextAddress(0x8004);
              this.$8000();
           }

           public void $9004() {
              this.setNextAddress(0x9004);
              this.$9000();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void batmanDCEBWalksATableOfSpritePointersWithTheStack() {
    // Batman DCEB-DCF4: the table entry (DE') and page (C') make an address, SP goes there, POP HL takes the sprite's address and SP goes to the sprite (the code at 8000 sets up one entry; DD3D, a rewritten JP target in the game, ends the scene)
    String java = translate(with(listing("""
        8000  D9  EXX
        8001  11 00 90  LD DE, 0x9000
        8004  0E 91  LD C, 0x91
        8006  D9  EXX
        8007  C3 EB DC  JP 0xDCEB
        DCEB  D9  EXX
        DCEC  1A  LD A, (DE)
        DCED  6F  LD L, A
        DCEE  61  LD H, C
        DCEF  F9  LD SP, HL
        DCF0  E1  POP HL
        DCF1  F9  LD SP, HL
        DCF2  13  INC DE
        DCF3  D9  EXX
        DCF4  C3 3D DD  JP 0xDD3D
        DD3D  18 FE  JR 0xDD3D
        """), at(0x9000, 0x10), at(0x9110, 0x00, 0x92)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.exx();
              this.DE(0x9000);
              this.C(145);
              this.exx();
              this.exx();
              int var1 = this.DE();
              int var2 = this.mem(var1, 0xDCEC);
              this.A(var2);
              int var3 = this.A();
              this.L(var3);
              int var4 = this.C();
              this.H(var4);
              int var5 = this.HL();
              this.SP(var5);
              int var6 = this.pop();
              this.HL(var6);
              int var7 = this.HL();
              this.SP(var7);
              int var8 = this.DE();
              int var9 = this.inc16(var8);
              this.DE(var9);
              this.exx();

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void emlyn98C2ReadsAWordWithPopAndStepsBackAByte() {
    // Emlyn Hughes 98C2: POP DE / DEC SP takes a byte from the sprite and leaves SP on the next one (the code at 8000 points SP at the sprite and HL at the screen; the loop at 98C5 ends the scene, where the game goes on with JR Z)
    String java = translate(with(listing("""
        8000  F3  DI
        8001  31 00 90  LD SP, 0x9000
        8004  21 00 FD  LD HL, 0xFD00
        8007  C3 C2 98  JP 0x98C2
        98C2  D1  POP DE
        98C3  3B  DEC SP
        98C4  73  LD (HL), E
        98C5  18 FE  JR 0x98C5
        """), at(0x9000, 0x5A, 0xA5)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.di();
              this.SP(0x9000);
              this.HL(0xFD00);
              int var1 = this.pop();
              this.DE(var1);
              int var2 = this.SP();
              int var3 = this.dec16(var2);
              this.SP(var3);
              int var4 = this.E();
              int var5 = this.HL();
              this.wMem(var5, var4, 0x98C4);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void zynaps88DETakesTheCallersWordsAndSkipsOne() {
    // Zynaps 88D8-88E4: pops two words over its own push and one more, then INC SP / INC SP skips the next word before pushing (the code at 8000 pushes three words as the caller does; the loop at 88E5 ends the scene)
    String java = translate(listing("""
        8000  21 11 11  LD HL, 0x1111
        8003  E5  PUSH HL
        8004  21 22 22  LD HL, 0x2222
        8007  E5  PUSH HL
        8008  21 33 33  LD HL, 0x3333
        800B  E5  PUSH HL
        800C  C3 D8 88  JP 0x88D8
        88D8  E5  PUSH HL
        88D9  79  LD A, C
        88DA  E6 03  AND A, 0x03
        88DC  EE 47  XOR A, 0x47
        88DE  E1  POP HL
        88DF  E1  POP HL
        88E0  C1  POP BC
        88E1  33  INC SP
        88E2  33  INC SP
        88E3  F5  PUSH AF
        88E4  C5  PUSH BC
        88E5  18 FE  JR 0x88E5
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.HL(4369);
              int var1 = this.HL();
              this.push(var1);
              this.HL(8738);
              int var2 = this.HL();
              this.push(var2);
              this.HL(13107);
              int var3 = this.HL();
              this.push(var3);
              int var4 = this.HL();
              this.push(var4);
              int var5 = this.C();
              this.A(var5);
              int var6 = this.A();
              int var7 = this.alu("and", var6, 3);
              this.A(var7);
              int var8 = this.A();
              int var9 = this.alu("xor", var8, 71);
              this.A(var9);
              int var10 = this.pop();
              this.HL(var10);
              int var11 = this.pop();
              this.HL(var11);
              int var12 = this.pop();
              this.BC(var12);
              int var13 = this.SP();
              int var14 = this.inc16(var13);
              this.SP(var14);
              int var15 = this.SP();
              int var16 = this.inc16(var15);
              this.SP(var16);
              int var17 = this.AF();
              this.push(var17);
              int var18 = this.BC();
              this.push(var18);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void theRomErrorRestartRestoresTheStackFromErrSp() {
    // ROM 0053-005C: a BASIC error takes its code, stores it in ERR_NR and goes back to the SP saved at ERR_SP (5C3D); only forks get there (here behind the JP NZ the recording never takes; 16C5 ends the scene)
    ignoresMemory(0x0000, 0x0000);
    String java = translate(with(listing("""
        8000  AF  XOR A, A
        8001  C2 53 00  JP NZ, 0x0053
        8004  18 FE  JR 0x8004
        0053  E1  POP HL
        0054  6E  LD L, (HL)
        0055  FD 75 00  LD (IY+00), L
        0058  ED 7B 3D 5C  LD SP, (0x5C3D)
        005C  C3 C5 16  JP 0x16C5
        16C5  18 FE  JR 0x16C5
        """), at(0x5C3D, 0xF0, 0xFE)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int var1 = this.A();
              int var2 = this.A();
              int var3 = this.alu("xor", var2, var1);
              this.A(var3);
              if(!this.flag(64, true)) {
                 while(true) {
                 }
              }

              this.untranslated(83);
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void headOverHeelsC219IsDataAForkRunsAsCode() {
    // Head over Heels C219-C224: data a fork runs as code, with an LD SP,HL whose value means nothing (here behind the JP NZ the recording never takes; 0020 only returns, the loop at C225 ends the scene)
    String java = translate(listing("""
        8000  AF  XOR A, A
        8001  C2 19 C2  JP NZ, 0xC219
        8004  18 FE  JR 0x8004
        C219  67  LD H, A
        C21A  A9  XOR A, C
        C21B  B0  OR A, B
        C21C  6E  LD L, (HL)
        C21D  6C  LD L, H
        C21E  8F  ADC A, A
        C21F  9B  SBC A, E
        C220  E7  RST
        C221  E7  RST
        C222  F9  LD SP, HL
        C223  9F  SBC A, A
        C224  76  HALT
        C225  18 FE  JR 0xC225
        """, 0x0020));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int var1 = this.A();
              int var2 = this.A();
              int var3 = this.alu("xor", var2, var1);
              this.A(var3);
              if(!this.flag(64, true)) {
                 while(true) {
                 }
              }

              int var4 = this.A();
              this.H(var4);
              int var5 = this.C();
              int var6 = this.A();
              int var7 = this.alu("xor", var6, var5);
              this.A(var7);
              int var8 = this.B();
              int var9 = this.A();
              int var10 = this.alu("or", var9, var8);
              this.A(var10);
              int var11 = this.HL();
              int var12 = this.mem(var11, 0xC21C);
              this.L(var12);
              int var13 = this.H();
              this.L(var13);
              int var14 = this.A();
              int var15 = this.A();
              int var16 = this.alu("adc", var15, var14);
              this.A(var16);
              int var17 = this.E();
              int var18 = this.A();
              int var19 = this.alu("sbc", var18, var17);
              this.A(var19);
              this.untranslated(32);
              this.untranslated(32);
              int var20 = this.HL();
              this.SP(var20);
              int var21 = this.A();
              int var22 = this.A();
              int var23 = this.alu("sbc", var22, var21);
              this.A(var23);
              this.halt(0xC224);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void skoolDaze6921GoesBackToTheMainLoopOnlyWhereTheExplorationGoes() {
    // Skool Daze 6921: LD SP,5CFE drops the CALL 6226 at 6773; in the game only the exploration takes that path (here behind the JP NZ the recording never takes; 6226 stands for the way to 691A, the loop at 6925 ends the scene)
    ignoresMemory(0x5C00, 0x5CFF);
    String java = translateFrom(0x8000, 0x5CFE, listing("""
        8000  AF  XOR A, A
        8001  C2 73 67  JP NZ, 0x6773
        8004  18 FE  JR 0x8004
        6773  CD 26 62  CALL 0x6226
        6226  C3 1A 69  JP 0x691A
        691A  CD E0 ED  CALL 0xEDE0
        691D  2A C2 7F  LD HL, (0x7FC2)
        6920  AF  XOR A, A
        6921  31 FE 5C  LD SP, 0x5CFE
        6924  E5  PUSH HL
        6925  18 FE  JR 0x6925
        """, 0xEDE0));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $6226() {
              this.$691A();
              if(!this.unwinding()) {
                 ;
              }
           }

           public void $691A() {
              this.$EDE0();
              int var1 = this.mem16(32706, 26909);
              this.HL(var1);
              int var2 = this.A();
              int var3 = this.A();
              int var4 = this.alu("xor", var3, var2);
              this.A(var4);
              this.SP(23806);
              this.unwind(0x6924);
           }

           public void $6924() {
              int var1 = this.HL();
              this.push(var1);

              while(true) {
              }
           }

           public void $8000() {
              int var1 = this.A();
              int var2 = this.A();
              int var3 = this.alu("xor", var2, var1);
              this.A(var3);
              if(!this.flag(64, true)) {
                 while(true) {
                 }
              }

              this.$6226();
              if(!this.unwinding()) {
                 this.$691A();
                 if(!this.unwinding()) {
                    ;
                 }
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void targetRenegade5B34PagesAndGoesBackToTheMainLoopOnlyWhereTheExplorationGoes() {
    // Target: Renegade 5B34: after paging bank 0, LD SP,BFB8 drops the CALL F3A6 at D79D with the two words pushed before it; in the game only the exploration takes that path (here behind the JP NZ the recording never takes; F3A6 stands for the way to 5B2C, the loops at 5B37 and D7A3 end the scene)
    ignoresMemory(0xBF00, 0xBFB8);
    String java = translateFrom(0x8000, 0xBFB8, listing("""
        8000  AF  XOR A, A
        8001  C2 9A D7  JP NZ, 0xD79A
        8004  18 FE  JR 0x8004
        D79A  FD E5  PUSH IY
        D79C  C5  PUSH BC
        D79D  CD A6 F3  CALL 0xF3A6
        D7A0  C1  POP BC
        D7A1  FD E1  POP IY
        D7A3  18 FE  JR 0xD7A3
        F3A6  C3 2C 5B  JP 0x5B2C
        5B2C  AF  XOR A, A
        5B2D  D3 FE  OUT 1, A
        5B2F  01 FD 7F  LD BC, 0x7FFD
        5B32  ED 79  OUT BC, A
        5B34  31 B8 BF  LD SP, 0xBFB8
        5B37  18 FE  JR 0x5B37
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int[] var6;
              do {
                 if(this.isNextPC(0x5B37)) {
                    while(true) {
                    }
                 }

                 int var1 = this.A();
                 int var2 = this.A();
                 int var3 = this.alu("xor", var2, var1);
                 this.A(var3);
                 if(!this.flag(64, true)) {
                    while(true) {
                    }
                 }

                 int var4 = this.IY();
                 this.push(var4);
                 int var5 = this.BC();
                 this.push(var5);
                 this.$F3A6();
                 if(!this.unwinding()) {
                    int var7 = this.pop();
                    this.BC(var7);
                    int var8 = this.pop();
                    this.IY(var8);

                    while(true) {
                    }
                 }

                 var6 = new int[]{0x5B37};
              } while(this.isOwnAddress(var6));

           }

           public void $F3A6() {
              int var1 = this.A();
              int var2 = this.A();
              int var3 = this.alu("xor", var2, var1);
              this.A(var3);
              int var4 = this.A() << 8 | 254;
              int var5 = this.A();
              this.out(var4, var5);
              this.BC(32765);
              int var6 = this.BC();
              int var7 = this.A();
              this.out(var6, var7);
              this.SP(0xBFB8);
              this.unwind(0x5B37);
           }

           public void $5B37() {
              this.setNextAddress(0x5B37);
              this.$8000();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void batman61BAResetsTheStackOnlyWhereTheExplorationGoes() {
    // Batman 61BA: LD SP,EFEE drops the CALL EBA0 at EC5E; in the game only the exploration takes that path (here behind the JP NZ the recording never takes; EBA0 stands for the way to 61B5, 620B only returns, the loops at 61BD and EC65 end the scene)
    ignoresMemory(0xEF00, 0xEFFF);
    String java = translateFrom(0x8000, 0xEFEA, listing("""
        8000  AF  XOR A, A
        8001  C2 5E EC  JP NZ, 0xEC5E
        8004  18 FE  JR 0x8004
        EC5E  CD A0 EB  CALL 0xEBA0
        EC61  ED 5B 39 EC  LD DE, (0xEC39)
        EC65  18 FE  JR 0xEC65
        EBA0  C3 B5 61  JP 0x61B5
        61B5  23  INC HL
        61B6  BE  CP A, (HL)
        61B7  C4 0B 62  CALL NZ, 0x620B
        61BA  31 EE EF  LD SP, 0xEFEE
        61BD  18 FE  JR 0x61BD
        """, 0x620B));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int[] var4;
              do {
                 if(this.isNextPC(0x61BD)) {
                    while(true) {
                    }
                 }

                 int var1 = this.A();
                 int var2 = this.A();
                 int var3 = this.alu("xor", var2, var1);
                 this.A(var3);
                 if(!this.flag(64, true)) {
                    while(true) {
                    }
                 }

                 this.$EBA0();
                 if(!this.unwinding()) {
                    int var5 = this.mem16(0xEC39, 0xEC61);
                    this.DE(var5);

                    while(true) {
                    }
                 }

                 var4 = new int[]{0x61BD};
              } while(this.isOwnAddress(var4));

           }

           public void $EBA0() {
              int var1 = this.HL();
              int var2 = this.inc16(var1);
              this.HL(var2);
              int var3 = this.HL();
              int var4 = this.mem(var3, 25014);
              int var5 = this.A();
              this.alu("cp", var5, var4);
              if(this.flag(64, true)) {
                 this.$620B();
              }

              this.SP(0xEFEE);
              this.unwind(0x61BD);
           }

           public void $61BD() {
              this.setNextAddress(0x61BD);
              this.$8000();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void headOverHeels7049ResetsTheStackOnlyWhereTheExplorationGoes() {
    // Head over Heels 7049: CALL 7D92 / LD SP,FFF4, code the recording never runs; the exploration drops the CALL 7B0E at 7051 there (here behind the JP NZ the recording never takes; 7B0E stands for the way to 7046, 7D92 only returns, the loops at 704C and 7054 end the scene)
    ignoresMemory(0xFF00, 0xFFFF);
    String java = translateFrom(0x8000, 0xFFF4, listing("""
        8000  AF  XOR A, A
        8001  C2 51 70  JP NZ, 0x7051
        8004  18 FE  JR 0x8004
        7051  CD 0E 7B  CALL 0x7B0E
        7054  18 FE  JR 0x7054
        7B0E  C3 46 70  JP 0x7046
        7046  CD 92 7D  CALL 0x7D92
        7049  31 F4 FF  LD SP, 0xFFF4
        704C  18 FE  JR 0x704C
        """, 0x7D92));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $7B0E() {
              this.$7D92();
              this.SP(0xFFF4);
              this.unwind(0x704C);
           }

           public void $8000() {
              int[] var4;
              do {
                 if(this.isNextPC(0x704C)) {
                    while(true) {
                    }
                 }

                 int var1 = this.A();
                 int var2 = this.A();
                 int var3 = this.alu("xor", var2, var1);
                 this.A(var3);
                 if(!this.flag(64, true)) {
                    while(true) {
                    }
                 }

                 this.$7B0E();
                 if(!this.unwinding()) {
                    while(true) {
                    }
                 }

                 var4 = new int[]{0x704C};
              } while(this.isOwnAddress(var4));

           }

           public void $704C() {
              this.setNextAddress(0x704C);
              this.$8000();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void headOverHeelsB803LoadsSpWithAValueThatIsData() {
    // Head over Heels B803: the exploration reads LD SP,0063 there, bytes that never run as code in the recording (here behind the JP NZ the recording never takes; the loop at B806 ends the scene)
    String java = translateFrom(0x8000, 0xFF00, listing("""
        8000  AF  XOR A, A
        8001  C2 03 B8  JP NZ, 0xB803
        8004  18 FE  JR 0x8004
        B803  31 63 00  LD SP, 0x0063
        B806  18 FE  JR 0xB806
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int var1 = this.A();
              int var2 = this.A();
              int var3 = this.alu("xor", var2, var1);
              this.A(var3);
              if(!this.flag(64, true)) {
                 while(true) {
                 }
              }

              this.SP(99);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void targetRenegadeC3A4ReadsAChannelTableWithTheStackAndRestoresItsSP() {
    // Target: Renegade C3A4-C3F4 (bank 3 in the game, main memory here): saves SP at C487, points SP at the channel table (LD SP,C315), takes two words with POP, jumps to the handler the table names (C31B = F2) which pushes them back and restores SP from C487 (the code at 8000 calls it; the loop at C3FA ends the scene)
    savesStackPointerAt(0xC487);
    String java = translate(with(listing("""
        8000  CD A4 C3  CALL 0xC3A4
        8003  18 FE  JR 0x8003
        C3A4  ED 73 87 C4  LD (0xC487), SP
        C3A8  26 C3  LD H, 0xC3
        C3AA  2E 1A  LD L, 0x1A
        C3AC  35  DEC (HL)
        C3AD  20 49  JR NZ, 0xC3F8
        C3AF  3A 19 C3  LD A, (0xC319)
        C3B2  77  LD (HL), A
        C3B3  31 15 C3  LD SP, 0xC315
        C3B6  D1  POP DE
        C3B7  C1  POP BC
        C3B8  2E 1B  LD L, 0x1B
        C3BA  6E  LD L, (HL)
        C3BB  E9  JP 0x0000
        C3F2  C5  PUSH BC
        C3F3  D5  PUSH DE
        C3F4  ED 7B 87 C4  LD SP, (0xC487)
        C3F8  0E 08  LD C, 0x08
        C3FA  18 FE  JR 0xC3FA
        """), at(0xC315, 0x34, 0x12, 0x78, 0x56, 0x05, 0x01, 0xF2)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.$C3A4();
              if(!this.unwinding()) {
                 ;
              }
           }

           public void $C3A4() {
              int var1 = this.SP();
              this.wMem16(0xC487, var1, 0xC3A4);
              this.H(195);
              this.L(26);
              int var2 = this.HL();
              int var3 = this.mem(var2, 0xC3AC);
              int var4 = this.alu("dec", var3);
              int var5 = this.HL();
              this.wMem(var5, var4, 0xC3AC);
              if(this.flag(64, true)) {
              } else {
                 int var6 = this.mem(0xC319, 0xC3AF);
                 this.A(var6);
                 int var7 = this.A();
                 int var8 = this.HL();
                 this.wMem(var8, var7, 0xC3B2);
                 this.SP(0xC315);
                 int var9 = this.pop();
                 this.DE(var9);
                 int var10 = this.pop();
                 this.BC(var10);
                 this.L(27);
                 int var11 = this.HL();
                 int var12 = this.mem(var11, 0xC3BA);
                 this.L(var12);
                 if(this.HL() != 0xC3F2) {
                    int var16 = this.HL();
                    this.jump(var16);
                    if(this.unwinding()) {
                       return;
                    }

                    return;
                 }

                 int var13 = this.BC();
                 this.push(var13);
                 int var14 = this.DE();
                 this.push(var14);
                 int var15 = this.mem16(0xC487, 0xC3F4);
                 this.SP(var15);
              }

              this.C(8);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void targetRenegadeC461CallsOnASecondStackAndTheCalleeGoesBackThroughIX() {
    // Target: Renegade C44C-C46C (bank 3 in the game, main memory here): restores SP from C487 after another LD SP, takes the channel's own stack from C3A0, and CALL C132 pops its return into IX, takes the channel's word and goes back with JP (IX); then the channel stack is saved and the game's restored (the code at 8000 saves SP at C487 as C3A4 does; the channel stack at 9000 holds B = 2; the loop at C470 ends the scene)
    savesStackPointerAt(0xC487);
    savesStackPointerAt(0xC3A0);
    ignoresMemory(0x8FF0, 0x9001);
    String java = translate(with(listing("""
        8000  ED 73 87 C4  LD (0xC487), SP
        8004  C3 4C C4  JP 0xC44C
        C44C  0E 00  LD C, 0x00
        C44E  ED 7B 87 C4  LD SP, (0xC487)
        C452  2A 20 C3  LD HL, (0xC320)
        C455  CD DB C6  CALL 0xC6DB
        C458  3A A3 C3  LD A, (0xC3A3)
        C45B  B7  OR A, A
        C45C  C2 A8 C5  JP NZ, 0xC5A8
        C45F  16 C3  LD D, 0xC3
        C461  ED 7B A0 C3  LD SP, (0xC3A0)
        C465  CD 32 C1  CALL 0xC132
        C468  ED 73 A0 C3  LD (0xC3A0), SP
        C46C  ED 7B 87 C4  LD SP, (0xC487)
        C470  18 FE  JR 0xC470
        C132  DD E1  POP IX
        C134  C1  POP BC
        C135  10 0F  DJNZ 0xC146
        C146  B7  OR A, A
        C147  C5  PUSH BC
        C148  DD E9  JP 0x0000
        C5A8  18 FE  JR 0xC5A8
        """, 0xC6DB), at(0xC3A0, 0x00, 0x90), at(0x9000, 0x00, 0x02)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int var1 = this.SP();
              this.wMem16(0xC487, var1, 0x8000);
              this.C(0);
              int var2 = this.mem16(0xC487, 0xC44E);
              this.SP(var2);
              int var3 = this.mem16(0xC320, 0xC452);
              this.HL(var3);
              this.$C6DB();
              int var4 = this.mem(0xC3A3, 0xC458);
              this.A(var4);
              int var5 = this.A();
              int var6 = this.A();
              int var7 = this.alu("or", var6, var5);
              this.A(var7);
              if(!this.flag(64, true)) {
                 this.D(195);
                 int var8 = this.mem16(0xC3A0, 0xC461);
                 this.SP(var8);
                 this.push(0xC468);
                 int var9 = this.SP();
                 this.$C132();
                 if(this.unwinding()) {
                    return;
                 }

                 if(this.SP() == var9) {
                    this.pop();
                 }

                 int var10 = this.SP();
                 this.wMem16(0xC3A0, var10, 0xC468);
                 int var11 = this.mem16(0xC487, 0xC46C);
                 this.SP(var11);

                 while(true) {
                 }
              }

              while(true) {
              }
           }

           public void $C132() {
              int var1 = this.pop();
              this.IX(var1);
              int var2 = this.pop();
              this.BC(var2);
              int var3 = this.B() - 1 & 255;
              this.B(var3);
              if(this.B() != 0) {
              } else {
              }

              int var4 = this.A();
              int var5 = this.A();
              int var6 = this.alu("or", var5, var4);
              this.A(var6);
              int var7 = this.BC();
              this.push(var7);
           }
        }
        """, withoutTiming(java));
  }

  @Ignore("open (P5): the copy runs neither the game's virtual POP nor a real push, so POP IX at C132 takes the channel's word 0200 instead of C468; the game gets the virtual POP ($4132 throws, the catch sets IX = C468)")
  @Test
  public void targetRenegadeC461InBankThreeCallsOnASecondStackAndTheCalleeGoesBackThroughIX() {
    // Target: Renegade C44C-C46C as it runs, in bank 3: the translation runs it from a copy below C000, and the return C132 pops into IX must be the original C468 (C400 saves SP at C487 as C3A4 does; the channel stack at 7000 holds B = 2; the RET at C470 ends the scene)
    savesStackPointerAt(0xC487);
    savesStackPointerAt(0xC3A0);
    ignoresMemory(0x6FF0, 0x7001);
    translate(inBank(3, 0xC400, with(listing("""
        C400  ED 73 87 C4  LD (0xC487), SP
        C404  C3 4C C4  JP 0xC44C
        C44C  0E 00  LD C, 0x00
        C44E  ED 7B 87 C4  LD SP, (0xC487)
        C452  2A 20 C3  LD HL, (0xC320)
        C455  CD DB C6  CALL 0xC6DB
        C458  3A A3 C3  LD A, (0xC3A3)
        C45B  B7  OR A, A
        C45C  C2 A8 C5  JP NZ, 0xC5A8
        C45F  16 C3  LD D, 0xC3
        C461  ED 7B A0 C3  LD SP, (0xC3A0)
        C465  CD 32 C1  CALL 0xC132
        C468  ED 73 A0 C3  LD (0xC3A0), SP
        C46C  ED 7B 87 C4  LD SP, (0xC487)
        C470  C9  RET
        C132  DD E1  POP IX
        C134  C1  POP BC
        C135  10 0F  DJNZ 0xC146
        C146  B7  OR A, A
        C147  C5  PUSH BC
        C148  DD E9  JP 0x0000
        C5A8  18 FE  JR 0xC5A8
        """, 0xC6DB), at(0xC3A0, 0x00, 0x70), at(0xC3A3, 0x00), at(0x7000, 0x00, 0x02))));
  }

  @Ignore("open (P5): the continuation a CALL with inline data learns at its original address does not reach the copy, which stops at 'no translated routine at C004'")
  @Test
  public void aCallWithInlineDataInAnotherBanksCodeReturnsPastTheDataInItsCopy() {
    // the shape of Target: Renegade C465/C132 in bank 3 when the callee goes back past the data with JP (HL) instead of a POP into IX
    translate(inBank(3, 0xC000, listing("""
        C000  CD 08 C0  CALL 0xC008
        C003  07  RLCA A
        C004  57  LD D, A
        C005  C9  RET
        C008  E1  POP HL
        C009  7E  LD A, (HL)
        C00A  23  INC HL
        C00B  E9  JP 0x0000
        """)));
    Assert.assertEquals(0x07, endValue("D"));
  }

  @Test
  public void targetRenegadeD8AEDrawsWithTheStackThroughAnUnrolledLoop() {
    // Target: Renegade D8A8-D9A9: saves SP at F6F6, points SP at the sprite (LD SP,HL at D8AE), enters an unrolled loop in its middle, skips rows with ADD HL,DE / LD SP,HL, pushes the drawn area on the list at F6FC and restores SP from F6F6 (the code at 8000 sets up a sprite at 9000, the screen at 4000 and the list at 7000, and pushes the return, HL and DE that the routine pops at the end)
    savesStackPointerAt(0xF6F6);
    ignoresMemory(0x6FF0, 0x7001);
    ignoresMemory(0xF6F2, 0xF6FD);
    String java = translate(with(listing("""
        8000  21 00 70  LD HL, 0x7000
        8003  22 FC F6  LD (0xF6FC), HL
        8006  D9  EXX
        8007  21 00 90  LD HL, 0x9000
        800A  0E 08  LD C, 0x08
        800C  D9  EXX
        800D  01 00 A0  LD BC, 0xA000
        8010  11 00 40  LD DE, 0x4000
        8013  3E 02  LD A, 0x02
        8015  21 1E 80  LD HL, 0x801E
        8018  E5  PUSH HL
        8019  E5  PUSH HL
        801A  D5  PUSH DE
        801B  C3 A8 D8  JP 0xD8A8
        801E  18 FE  JR 0x801E
        D8A8  D9  EXX
        D8A9  F3  DI
        D8AA  ED 73 F6 F6  LD (0xF6F6), SP
        D8AE  F9  LD SP, HL
        D8AF  47  LD B, A
        D8B0  79  LD A, C
        D8B1  90  SUB A, B
        D8B2  4F  LD C, A
        D8B3  80  ADD A, B
        D8B4  ED 43 F2 F6  LD (0xF6F2), BC
        D8B8  D9  EXX
        D8B9  02  LD (BC), A
        D8BA  0B  DEC BC
        D8BB  D2 92 D9  JP NC, 0xD992
        D8BE  D9  EXX
        D8BF  47  LD B, A
        D8C0  0E 00  LD C, 0x00
        D8C2  ED 43 F2 F6  LD (0xF6F2), BC
        D8C6  D9  EXX
        D8C7  C3 92 D9  JP 0xD992
        D8CA  1A  LD A, (DE)
        D8CB  02  LD (BC), A
        D8CC  0B  DEC BC
        D8CD  E1  POP HL
        D8CE  A5  AND A, L
        D8CF  B4  OR A, H
        D8D0  12  LD (DE), A
        D8D1  1C  INC E
        D8D2  1A  LD A, (DE)
        D8D3  02  LD (BC), A
        D8D4  0B  DEC BC
        D8D5  E1  POP HL
        D8D6  A5  AND A, L
        D8D7  B4  OR A, H
        D8D8  12  LD (DE), A
        D8D9  1C  INC E
        D8DA  1A  LD A, (DE)
        D8DB  02  LD (BC), A
        D8DC  0B  DEC BC
        D8DD  E1  POP HL
        D8DE  A5  AND A, L
        D8DF  B4  OR A, H
        D8E0  12  LD (DE), A
        D8E1  1C  INC E
        D8E2  1A  LD A, (DE)
        D8E3  02  LD (BC), A
        D8E4  0B  DEC BC
        D8E5  E1  POP HL
        D8E6  A5  AND A, L
        D8E7  B4  OR A, H
        D8E8  12  LD (DE), A
        D8E9  1C  INC E
        D8EA  1A  LD A, (DE)
        D8EB  02  LD (BC), A
        D8EC  0B  DEC BC
        D8ED  E1  POP HL
        D8EE  A5  AND A, L
        D8EF  B4  OR A, H
        D8F0  12  LD (DE), A
        D8F1  1C  INC E
        D8F2  1A  LD A, (DE)
        D8F3  02  LD (BC), A
        D8F4  0B  DEC BC
        D8F5  E1  POP HL
        D8F6  A5  AND A, L
        D8F7  B4  OR A, H
        D8F8  12  LD (DE), A
        D8F9  14  INC D
        D8FA  D9  EXX
        D8FB  ED 43 F2 F6  LD (0xF6F2), BC
        D8FF  10 20  DJNZ 0xD921
        D901  79  LD A, C
        D902  06 08  LD B, 0x08
        D904  90  SUB A, B
        D905  30 06  JR NC, 0xD90D
        D907  80  ADD A, B
        D908  CA 95 D9  JP Z, 0xD995
        D90B  47  LD B, A
        D90C  AF  XOR A, A
        D90D  4F  LD C, A
        D90E  D9  EXX
        D90F  7B  LD A, E
        D910  C6 20  ADD A, 0x20
        D912  5F  LD E, A
        D913  30 07  JR NC, 0xD91C
        D915  CB 62  BIT 4, D
        D917  28 09  JR Z, 0xD922
        D919  C3 96 D9  JP 0xD996
        D91C  7A  LD A, D
        D91D  D6 08  SUB A, 0x08
        D91F  57  LD D, A
        D920  D9  EXX
        D921  D9  EXX
        D922  C3 50 D9  JP 0xD950
        D925  D9  EXX
        D926  21 00 00  LD HL, 0x0000
        D929  39  ADD HL, SP
        D92A  19  ADD HL, DE
        D92B  F9  LD SP, HL
        D92C  D9  EXX
        D92D  C3 58 D9  JP 0xD958
        D930  1A  LD A, (DE)
        D931  02  LD (BC), A
        D932  0B  DEC BC
        D933  E1  POP HL
        D934  A5  AND A, L
        D935  B4  OR A, H
        D936  12  LD (DE), A
        D937  1D  DEC E
        D938  1A  LD A, (DE)
        D939  02  LD (BC), A
        D93A  0B  DEC BC
        D93B  E1  POP HL
        D93C  A5  AND A, L
        D93D  B4  OR A, H
        D93E  12  LD (DE), A
        D93F  1D  DEC E
        D940  1A  LD A, (DE)
        D941  02  LD (BC), A
        D942  0B  DEC BC
        D943  E1  POP HL
        D944  A5  AND A, L
        D945  B4  OR A, H
        D946  12  LD (DE), A
        D947  1D  DEC E
        D948  1A  LD A, (DE)
        D949  02  LD (BC), A
        D94A  0B  DEC BC
        D94B  E1  POP HL
        D94C  A5  AND A, L
        D94D  B4  OR A, H
        D94E  12  LD (DE), A
        D94F  1D  DEC E
        D950  1A  LD A, (DE)
        D951  02  LD (BC), A
        D952  0B  DEC BC
        D953  E1  POP HL
        D954  A5  AND A, L
        D955  B4  OR A, H
        D956  12  LD (DE), A
        D957  1D  DEC E
        D958  1A  LD A, (DE)
        D959  02  LD (BC), A
        D95A  0B  DEC BC
        D95B  E1  POP HL
        D95C  A5  AND A, L
        D95D  B4  OR A, H
        D95E  12  LD (DE), A
        D95F  14  INC D
        D960  D9  EXX
        D961  ED 43 F2 F6  LD (0xF6F2), BC
        D965  10 23  DJNZ 0xD98A
        D967  79  LD A, C
        D968  06 08  LD B, 0x08
        D96A  90  SUB A, B
        D96B  30 05  JR NC, 0xD972
        D96D  80  ADD A, B
        D96E  28 25  JR Z, 0xD995
        D970  47  LD B, A
        D971  AF  XOR A, A
        D972  4F  LD C, A
        D973  ED 43 F2 F6  LD (0xF6F2), BC
        D977  D9  EXX
        D978  7B  LD A, E
        D979  C6 20  ADD A, 0x20
        D97B  5F  LD E, A
        D97C  30 07  JR NC, 0xD985
        D97E  CB 62  BIT 4, D
        D980  28 09  JR Z, 0xD98B
        D982  C3 96 D9  JP 0xD996
        D985  7A  LD A, D
        D986  D6 08  SUB A, 0x08
        D988  57  LD D, A
        D989  D9  EXX
        D98A  D9  EXX
        D98B  C3 EA D8  JP 0xD8EA
        D98E  D9  EXX
        D98F  19  ADD HL, DE
        D990  F9  LD SP, HL
        D991  D9  EXX
        D992  C3 EA D8  JP 0xD8EA
        D995  D9  EXX
        D996  ED 7B FC F6  LD SP, (0xF6FC)
        D99A  C5  PUSH BC
        D99B  ED 73 FC F6  LD (0xF6FC), SP
        D99F  ED 43 FA F6  LD (0xF6FA), BC
        D9A3  ED 7B F6 F6  LD SP, (0xF6F6)
        D9A7  D1  POP DE
        D9A8  E1  POP HL
        D9A9  C9  RET
        """), at(0xF6FC, 0x00, 0x70)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.HL(28672);
              int var1 = this.HL();
              this.wMem16(0xF6FC, var1, 0x8003);
              this.exx();
              this.HL(0x9000);
              this.C(8);
              this.exx();
              this.BC(0xA000);
              this.DE(16384);
              this.A(2);
              this.HL(0x801E);
              int var2 = this.HL();
              this.push(var2);
              int var3 = this.HL();
              this.push(var3);
              int var4 = this.DE();
              this.push(var4);
              this.exx();
              this.di();
              int var5 = this.SP();
              this.wMem16(0xF6F6, var5, 0xD8AA);
              int var6 = this.HL();
              this.SP(var6);
              int var7 = this.A();
              this.B(var7);
              int var8 = this.C();
              this.A(var8);
              int var9 = this.B();
              int var10 = this.A();
              int var11 = this.alu("sub", var10, var9);
              this.A(var11);
              int var12 = this.A();
              this.C(var12);
              int var13 = this.B();
              int var14 = this.A();
              int var15 = this.alu("add", var14, var13);
              this.A(var15);
              int var16 = this.BC();
              this.wMem16(0xF6F2, var16, 0xD8B4);
              this.exx();
              int var17 = this.A();
              int var18 = this.BC();
              this.wMem(var18, var17, 0xD8B9);
              int var19 = this.BC();
              int var20 = this.dec16(var19);
              this.BC(var20);
              if(!this.flag(1, true)) {
                 this.exx();
                 int var144 = this.A();
                 this.B(var144);
                 this.C(0);
                 int var145 = this.BC();
                 this.wMem16(0xF6F2, var145, 0xD8C2);
                 this.exx();
              }


              while(true) {
                 label76: {
                    label82: {
                       label74: {
                          int var21 = this.DE();
                          int var22 = this.mem(var21, 0xD8EA);
                          this.A(var22);
                          int var23 = this.A();
                          int var24 = this.BC();
                          this.wMem(var24, var23, 0xD8EB);
                          int var25 = this.BC();
                          int var26 = this.dec16(var25);
                          this.BC(var26);
                          int var27 = this.pop();
                          this.HL(var27);
                          int var28 = this.L();
                          int var29 = this.A();
                          int var30 = this.alu("and", var29, var28);
                          this.A(var30);
                          int var31 = this.H();
                          int var32 = this.A();
                          int var33 = this.alu("or", var32, var31);
                          this.A(var33);
                          int var34 = this.A();
                          int var35 = this.DE();
                          this.wMem(var35, var34, 0xD8F0);
                          int var36 = this.E();
                          int var37 = this.alu("inc", var36);
                          this.E(var37);
                          int var38 = this.DE();
                          int var39 = this.mem(var38, 0xD8F2);
                          this.A(var39);
                          int var40 = this.A();
                          int var41 = this.BC();
                          this.wMem(var41, var40, 0xD8F3);
                          int var42 = this.BC();
                          int var43 = this.dec16(var42);
                          this.BC(var43);
                          int var44 = this.pop();
                          this.HL(var44);
                          int var45 = this.L();
                          int var46 = this.A();
                          int var47 = this.alu("and", var46, var45);
                          this.A(var47);
                          int var48 = this.H();
                          int var49 = this.A();
                          int var50 = this.alu("or", var49, var48);
                          this.A(var50);
                          int var51 = this.A();
                          int var52 = this.DE();
                          this.wMem(var52, var51, 0xD8F8);
                          int var53 = this.D();
                          int var54 = this.alu("inc", var53);
                          this.D(var54);
                          this.exx();
                          int var55 = this.BC();
                          this.wMem16(0xF6F2, var55, 0xD8FB);
                          int var56 = this.B() - 1 & 255;
                          this.B(var56);
                          if(this.B() != 0) {
                          } else {
                             int var57 = this.C();
                             this.A(var57);
                             this.B(8);
                             int var58 = this.B();
                             int var59 = this.A();
                             int var60 = this.alu("sub", var59, var58);
                             this.A(var60);
                             if(this.flag(1, true)) {
                             } else {
                                int var61 = this.B();
                                int var62 = this.A();
                                int var63 = this.alu("add", var62, var61);
                                this.A(var63);
                                if(this.flag(64, false)) {
                                   break label82;
                                }

                                int var72 = this.A();
                                this.B(var72);
                                int var73 = this.A();
                                int var74 = this.A();
                                int var75 = this.alu("xor", var74, var73);
                                this.A(var75);
                             }

                             int var76 = this.A();
                             this.C(var76);
                             this.exx();
                             int var77 = this.E();
                             this.A(var77);
                             int var78 = this.A();
                             int var79 = this.alu("add", var78, 32);
                             this.A(var79);
                             int var80 = this.A();
                             this.E(var80);
                             if(!this.flag(1, true)) {
                                int var81 = this.D();
                                this.bit(4, var81);
                                if(!this.flag(64, false)) {
                                   break;
                                }

                                break label74;
                             }

                             int var140 = this.D();
                             this.A(var140);
                             int var141 = this.A();
                             int var142 = this.alu("sub", var141, 8);
                             this.A(var142);
                             int var143 = this.A();
                             this.D(var143);
                             this.exx();
                          }

                          this.exx();
                       }

                       int var82 = this.DE();
                       int var83 = this.mem(var82, 0xD950);
                       this.A(var83);
                       int var84 = this.A();
                       int var85 = this.BC();
                       this.wMem(var85, var84, 0xD951);
                       int var86 = this.BC();
                       int var87 = this.dec16(var86);
                       this.BC(var87);
                       int var88 = this.pop();
                       this.HL(var88);
                       int var89 = this.L();
                       int var90 = this.A();
                       int var91 = this.alu("and", var90, var89);
                       this.A(var91);
                       int var92 = this.H();
                       int var93 = this.A();
                       int var94 = this.alu("or", var93, var92);
                       this.A(var94);
                       int var95 = this.A();
                       int var96 = this.DE();
                       this.wMem(var96, var95, 0xD956);
                       int var97 = this.E();
                       int var98 = this.alu("dec", var97);
                       this.E(var98);
                       int var99 = this.DE();
                       int var100 = this.mem(var99, 0xD958);
                       this.A(var100);
                       int var101 = this.A();
                       int var102 = this.BC();
                       this.wMem(var102, var101, 0xD959);
                       int var103 = this.BC();
                       int var104 = this.dec16(var103);
                       this.BC(var104);
                       int var105 = this.pop();
                       this.HL(var105);
                       int var106 = this.L();
                       int var107 = this.A();
                       int var108 = this.alu("and", var107, var106);
                       this.A(var108);
                       int var109 = this.H();
                       int var110 = this.A();
                       int var111 = this.alu("or", var110, var109);
                       this.A(var111);
                       int var112 = this.A();
                       int var113 = this.DE();
                       this.wMem(var113, var112, 0xD95E);
                       int var114 = this.D();
                       int var115 = this.alu("inc", var114);
                       this.D(var115);
                       this.exx();
                       int var116 = this.BC();
                       this.wMem16(0xF6F2, var116, 0xD961);
                       int var117 = this.B() - 1 & 255;
                       this.B(var117);
                       if(this.B() != 0) {
                       } else {
                          int var118 = this.C();
                          this.A(var118);
                          this.B(8);
                          int var119 = this.B();
                          int var120 = this.A();
                          int var121 = this.alu("sub", var120, var119);
                          this.A(var121);
                          if(this.flag(1, true)) {
                          } else {
                             int var122 = this.B();
                             int var123 = this.A();
                             int var124 = this.alu("add", var123, var122);
                             this.A(var124);
                             if(this.flag(64, false)) {
                                break label82;
                             }

                             int var125 = this.A();
                             this.B(var125);
                             int var126 = this.A();
                             int var127 = this.A();
                             int var128 = this.alu("xor", var127, var126);
                             this.A(var128);
                          }

                          int var129 = this.A();
                          this.C(var129);
                          int var130 = this.BC();
                          this.wMem16(0xF6F2, var130, 0xD973);
                          this.exx();
                          int var131 = this.E();
                          this.A(var131);
                          int var132 = this.A();
                          int var133 = this.alu("add", var132, 32);
                          this.A(var133);
                          int var134 = this.A();
                          this.E(var134);
                          if(!this.flag(1, true)) {
                             int var135 = this.D();
                             this.bit(4, var135);
                             if(!this.flag(64, false)) {
                                break;
                             }

                             break label76;
                          }

                          int var136 = this.D();
                          this.A(var136);
                          int var137 = this.A();
                          int var138 = this.alu("sub", var137, 8);
                          this.A(var138);
                          int var139 = this.A();
                          this.D(var139);
                          this.exx();
                       }

                       this.exx();
                       break label76;
                    }

                    this.exx();
                    break;
                 }

              }

              int var64 = this.mem16(0xF6FC, 0xD996);
              this.SP(var64);
              int var65 = this.BC();
              this.push(var65);
              int var66 = this.SP();
              this.wMem16(0xF6FC, var66, 0xD99B);
              int var67 = this.BC();
              this.wMem16(0xF6FA, var67, 0xD99F);
              int var68 = this.mem16(0xF6F6, 0xD9A3);
              this.SP(var68);
              int var69 = this.pop();
              this.DE(var69);
              int var70 = this.pop();
              this.HL(var70);
              int var71 = this.pop();
              if(var71 == 0x801E) {
                 while(true) {
                 }
              }

              this.jump(var71);
              if(!this.unwinding()) {
                 ;
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void targetRenegadeDA70DrawsWithTheStackAndIX() {
    // Target: Renegade DA60-DB9C: saves SP at F6F6, points SP at the sprite (LD SP,HL at DA70), moves it on with ADD IX,DE / LD SP,IX, pushes the drawn area on the list at F6FC and restores SP from F6F6 (the code at 8000 sets up a sprite at 9000, the screen at 4000 and the list at 7000)
    savesStackPointerAt(0xF6F6);
    ignoresMemory(0x6FF0, 0x7001);
    ignoresMemory(0x70F0, 0x7101);
    ignoresMemory(0xF6F2, 0xF6FD);
    String java = translate(with(listing("""
        8000  21 00 70  LD HL, 0x7000
        8003  22 FC F6  LD (0xF6FC), HL
        8006  21 00 90  LD HL, 0x9000
        8009  11 00 40  LD DE, 0x4000
        800C  01 08 02  LD BC, 0x0208
        800F  CD 60 DA  CALL 0xDA60
        8012  18 FE  JR 0x8012
        DA60  0F  RRCA A
        DA61  47  LD B, A
        DA62  6A  LD L, D
        DA63  26 D0  LD H, 0xD0
        DA65  7E  LD A, (HL)
        DA66  B3  OR A, E
        DA67  2C  INC L
        DA68  66  LD H, (HL)
        DA69  6F  LD L, A
        DA6A  D9  EXX
        DA6B  F3  DI
        DA6C  ED 73 F6 F6  LD (0xF6F6), SP
        DA70  F9  LD SP, HL
        DA71  26 D1  LD H, 0xD1
        DA73  79  LD A, C
        DA74  D9  EXX
        DA75  4F  LD C, A
        DA76  EB  EX DE, HL
        DA77  2A FA F6  LD HL, (0xF6FA)
        DA7A  CB F8  SET 7, B
        DA7C  70  LD (HL), B
        DA7D  2B  DEC HL
        DA7E  73  LD (HL), E
        DA7F  2B  DEC HL
        DA80  72  LD (HL), D
        DA81  2B  DEC HL
        DA82  71  LD (HL), C
        DA83  2B  DEC HL
        DA84  EB  EX DE, HL
        DA85  7C  LD A, H
        DA86  E6 07  AND A, 0x07
        DA88  D6 09  SUB A, 0x09
        DA8A  2F  CPL A
        DA8B  47  LD B, A
        DA8C  79  LD A, C
        DA8D  90  SUB A, B
        DA8E  4F  LD C, A
        DA8F  ED 43 F2 F6  LD (0xF6F2), BC
        DA93  D2 86 DB  JP NC, 0xDB86
        DA96  80  ADD A, B
        DA97  47  LD B, A
        DA98  0E 00  LD C, 0x00
        DA9A  ED 43 F2 F6  LD (0xF6F2), BC
        DA9E  C3 86 DB  JP 0xDB86
        DAA1  7E  LD A, (HL)
        DAA2  12  LD (DE), A
        DAA3  1B  DEC DE
        DAA4  D9  EXX
        DAA5  C1  POP BC
        DAA6  69  LD L, C
        DAA7  A6  AND A, (HL)
        DAA8  68  LD L, B
        DAA9  B6  OR A, (HL)
        DAAA  D9  EXX
        DAAB  77  LD (HL), A
        DAAC  2D  DEC L
        DAAD  7E  LD A, (HL)
        DAAE  12  LD (DE), A
        DAAF  1B  DEC DE
        DAB0  D9  EXX
        DAB1  C1  POP BC
        DAB2  69  LD L, C
        DAB3  A6  AND A, (HL)
        DAB4  68  LD L, B
        DAB5  B6  OR A, (HL)
        DAB6  D9  EXX
        DAB7  77  LD (HL), A
        DAB8  2D  DEC L
        DAB9  7E  LD A, (HL)
        DABA  12  LD (DE), A
        DABB  1B  DEC DE
        DABC  D9  EXX
        DABD  C1  POP BC
        DABE  69  LD L, C
        DABF  A6  AND A, (HL)
        DAC0  68  LD L, B
        DAC1  B6  OR A, (HL)
        DAC2  D9  EXX
        DAC3  77  LD (HL), A
        DAC4  2D  DEC L
        DAC5  7E  LD A, (HL)
        DAC6  12  LD (DE), A
        DAC7  1B  DEC DE
        DAC8  D9  EXX
        DAC9  C1  POP BC
        DACA  69  LD L, C
        DACB  A6  AND A, (HL)
        DACC  68  LD L, B
        DACD  B6  OR A, (HL)
        DACE  D9  EXX
        DACF  77  LD (HL), A
        DAD0  2D  DEC L
        DAD1  7E  LD A, (HL)
        DAD2  12  LD (DE), A
        DAD3  1B  DEC DE
        DAD4  D9  EXX
        DAD5  C1  POP BC
        DAD6  69  LD L, C
        DAD7  A6  AND A, (HL)
        DAD8  68  LD L, B
        DAD9  B6  OR A, (HL)
        DADA  D9  EXX
        DADB  77  LD (HL), A
        DADC  2D  DEC L
        DADD  7E  LD A, (HL)
        DADE  12  LD (DE), A
        DADF  1B  DEC DE
        DAE0  D9  EXX
        DAE1  C1  POP BC
        DAE2  69  LD L, C
        DAE3  A6  AND A, (HL)
        DAE4  68  LD L, B
        DAE5  B6  OR A, (HL)
        DAE6  D9  EXX
        DAE7  77  LD (HL), A
        DAE8  24  INC H
        DAE9  10 1E  DJNZ 0xDB09
        DAEB  79  LD A, C
        DAEC  06 08  LD B, 0x08
        DAEE  90  SUB A, B
        DAEF  30 06  JR NC, 0xDAF7
        DAF1  80  ADD A, B
        DAF2  CA 89 DB  JP Z, 0xDB89
        DAF5  47  LD B, A
        DAF6  AF  XOR A, A
        DAF7  4F  LD C, A
        DAF8  7D  LD A, L
        DAF9  C6 20  ADD A, 0x20
        DAFB  6F  LD L, A
        DAFC  30 07  JR NC, 0xDB05
        DAFE  CB 64  BIT 4, H
        DB00  28 07  JR Z, 0xDB09
        DB02  C3 89 DB  JP 0xDB89
        DB05  7C  LD A, H
        DB06  D6 08  SUB A, 0x08
        DB08  67  LD H, A
        DB09  C3 39 DB  JP 0xDB39
        DB0C  D9  EXX
        DB0D  DD 19  ADD IX, DE
        DB0F  DD F9  LD SP, IX
        DB11  D9  EXX
        DB12  C3 21 DB  JP 0xDB21
        DB15  7E  LD A, (HL)
        DB16  12  LD (DE), A
        DB17  1B  DEC DE
        DB18  D9  EXX
        DB19  C1  POP BC
        DB1A  69  LD L, C
        DB1B  A6  AND A, (HL)
        DB1C  68  LD L, B
        DB1D  B6  OR A, (HL)
        DB1E  D9  EXX
        DB1F  77  LD (HL), A
        DB20  2C  INC L
        DB21  7E  LD A, (HL)
        DB22  12  LD (DE), A
        DB23  1B  DEC DE
        DB24  D9  EXX
        DB25  C1  POP BC
        DB26  69  LD L, C
        DB27  A6  AND A, (HL)
        DB28  68  LD L, B
        DB29  B6  OR A, (HL)
        DB2A  D9  EXX
        DB2B  77  LD (HL), A
        DB2C  2C  INC L
        DB2D  7E  LD A, (HL)
        DB2E  12  LD (DE), A
        DB2F  1B  DEC DE
        DB30  D9  EXX
        DB31  C1  POP BC
        DB32  69  LD L, C
        DB33  A6  AND A, (HL)
        DB34  68  LD L, B
        DB35  B6  OR A, (HL)
        DB36  D9  EXX
        DB37  77  LD (HL), A
        DB38  2C  INC L
        DB39  7E  LD A, (HL)
        DB3A  12  LD (DE), A
        DB3B  1B  DEC DE
        DB3C  D9  EXX
        DB3D  C1  POP BC
        DB3E  69  LD L, C
        DB3F  A6  AND A, (HL)
        DB40  68  LD L, B
        DB41  B6  OR A, (HL)
        DB42  D9  EXX
        DB43  77  LD (HL), A
        DB44  2C  INC L
        DB45  7E  LD A, (HL)
        DB46  12  LD (DE), A
        DB47  1B  DEC DE
        DB48  D9  EXX
        DB49  C1  POP BC
        DB4A  69  LD L, C
        DB4B  A6  AND A, (HL)
        DB4C  68  LD L, B
        DB4D  B6  OR A, (HL)
        DB4E  D9  EXX
        DB4F  77  LD (HL), A
        DB50  2C  INC L
        DB51  7E  LD A, (HL)
        DB52  12  LD (DE), A
        DB53  1B  DEC DE
        DB54  D9  EXX
        DB55  C1  POP BC
        DB56  69  LD L, C
        DB57  A6  AND A, (HL)
        DB58  68  LD L, B
        DB59  B6  OR A, (HL)
        DB5A  D9  EXX
        DB5B  77  LD (HL), A
        DB5C  24  INC H
        DB5D  10 1E  DJNZ 0xDB7D
        DB5F  79  LD A, C
        DB60  06 08  LD B, 0x08
        DB62  90  SUB A, B
        DB63  30 06  JR NC, 0xDB6B
        DB65  80  ADD A, B
        DB66  CA 89 DB  JP Z, 0xDB89
        DB69  47  LD B, A
        DB6A  AF  XOR A, A
        DB6B  4F  LD C, A
        DB6C  7D  LD A, L
        DB6D  C6 20  ADD A, 0x20
        DB6F  6F  LD L, A
        DB70  30 07  JR NC, 0xDB79
        DB72  CB 64  BIT 4, H
        DB74  28 07  JR Z, 0xDB7D
        DB76  C3 89 DB  JP 0xDB89
        DB79  7C  LD A, H
        DB7A  D6 08  SUB A, 0x08
        DB7C  67  LD H, A
        DB7D  C3 C5 DA  JP 0xDAC5
        DB80  D9  EXX
        DB81  DD 19  ADD IX, DE
        DB83  DD F9  LD SP, IX
        DB85  D9  EXX
        DB86  C3 C5 DA  JP 0xDAC5
        DB89  ED 7B FC F6  LD SP, (0xF6FC)
        DB8D  D5  PUSH DE
        DB8E  ED 73 FC F6  LD (0xF6FC), SP
        DB92  ED 7B F6 F6  LD SP, (0xF6F6)
        DB96  ED 53 FA F6  LD (0xF6FA), DE
        DB9A  D1  POP DE
        DB9B  E1  POP HL
        DB9C  C9  RET
        """), at(0xF6FA, 0x00, 0x71)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.HL(28672);
              int var1 = this.HL();
              this.wMem16(0xF6FC, var1, 0x8003);
              this.HL(0x9000);
              this.DE(16384);
              this.BC(520);
              this.$DA60();
              if(!this.unwinding()) {
                 while(true) {
                 }
              }

              int[] var2 = new int[]{0xDB9B};
              if(this.ownAddress(var2) == 0xDB9B) {
                 this.DE(0x8012);
                 int var3 = this.pop();
                 this.HL(var3);
              }
           }

           public void $DA60() {
              int var1 = this.A();
              int var2 = this.alu("rrca", var1);
              this.A(var2);
              int var3 = this.A();
              this.B(var3);
              int var4 = this.D();
              this.L(var4);
              this.H(208);
              int var5 = this.HL();
              int var6 = this.mem(var5, 0xDA65);
              this.A(var6);
              int var7 = this.E();
              int var8 = this.A();
              int var9 = this.alu("or", var8, var7);
              this.A(var9);
              int var10 = this.L();
              int var11 = this.alu("inc", var10);
              this.L(var11);
              int var12 = this.HL();
              int var13 = this.mem(var12, 0xDA68);
              this.H(var13);
              int var14 = this.A();
              this.L(var14);
              this.exx();
              this.di();
              int var15 = this.SP();
              this.wMem16(0xF6F6, var15, 0xDA6C);
              int var16 = this.HL();
              this.SP(var16);
              this.H(209);
              int var17 = this.C();
              this.A(var17);
              this.exx();
              int var18 = this.A();
              this.C(var18);
              this.exHLDE();
              int var19 = this.mem16(0xF6FA, 0xDA77);
              this.HL(var19);
              int var20 = this.B() | 128;
              this.B(var20);
              int var21 = this.B();
              int var22 = this.HL();
              this.wMem(var22, var21, 0xDA7C);
              int var23 = this.HL();
              int var24 = this.dec16(var23);
              this.HL(var24);
              int var25 = this.E();
              int var26 = this.HL();
              this.wMem(var26, var25, 0xDA7E);
              int var27 = this.HL();
              int var28 = this.dec16(var27);
              this.HL(var28);
              int var29 = this.D();
              int var30 = this.HL();
              this.wMem(var30, var29, 0xDA80);
              int var31 = this.HL();
              int var32 = this.dec16(var31);
              this.HL(var32);
              int var33 = this.C();
              int var34 = this.HL();
              this.wMem(var34, var33, 0xDA82);
              int var35 = this.HL();
              int var36 = this.dec16(var35);
              this.HL(var36);
              this.exHLDE();
              int var37 = this.H();
              this.A(var37);
              int var38 = this.A();
              int var39 = this.alu("and", var38, 7);
              this.A(var39);
              int var40 = this.A();
              int var41 = this.alu("sub", var40, 9);
              this.A(var41);
              int var42 = this.A();
              int var43 = this.alu("cpl", var42);
              this.A(var43);
              int var44 = this.A();
              this.B(var44);
              int var45 = this.C();
              this.A(var45);
              int var46 = this.B();
              int var47 = this.A();
              int var48 = this.alu("sub", var47, var46);
              this.A(var48);
              int var49 = this.A();
              this.C(var49);
              int var50 = this.BC();
              this.wMem16(0xF6F2, var50, 0xDA8F);
              if(!this.flag(1, true)) {
                 int var226 = this.B();
                 int var227 = this.A();
                 int var228 = this.alu("add", var227, var226);
                 this.A(var228);
                 int var229 = this.A();
                 this.B(var229);
                 this.C(0);
                 int var230 = this.BC();
                 this.wMem16(0xF6F2, var230, 0xDA9A);
              }


              while(true) {
                 int var51 = this.HL();
                 int var52 = this.mem(var51, 0xDAC5);
                 this.A(var52);
                 int var53 = this.A();
                 int var54 = this.DE();
                 this.wMem(var54, var53, 0xDAC6);
                 int var55 = this.DE();
                 int var56 = this.dec16(var55);
                 this.DE(var56);
                 this.exx();
                 int var57 = this.pop();
                 this.BC(var57);
                 int var58 = this.C();
                 this.L(var58);
                 int var59 = this.HL();
                 int var60 = this.mem(var59, 0xDACB);
                 int var61 = this.A();
                 int var62 = this.alu("and", var61, var60);
                 this.A(var62);
                 int var63 = this.B();
                 this.L(var63);
                 int var64 = this.HL();
                 int var65 = this.mem(var64, 0xDACD);
                 int var66 = this.A();
                 int var67 = this.alu("or", var66, var65);
                 this.A(var67);
                 this.exx();
                 int var68 = this.A();
                 int var69 = this.HL();
                 this.wMem(var69, var68, 0xDACF);
                 int var70 = this.L();
                 int var71 = this.alu("dec", var70);
                 this.L(var71);
                 int var72 = this.HL();
                 int var73 = this.mem(var72, 0xDAD1);
                 this.A(var73);
                 int var74 = this.A();
                 int var75 = this.DE();
                 this.wMem(var75, var74, 0xDAD2);
                 int var76 = this.DE();
                 int var77 = this.dec16(var76);
                 this.DE(var77);
                 this.exx();
                 int var78 = this.pop();
                 this.BC(var78);
                 int var79 = this.C();
                 this.L(var79);
                 int var80 = this.HL();
                 int var81 = this.mem(var80, 0xDAD7);
                 int var82 = this.A();
                 int var83 = this.alu("and", var82, var81);
                 this.A(var83);
                 int var84 = this.B();
                 this.L(var84);
                 int var85 = this.HL();
                 int var86 = this.mem(var85, 0xDAD9);
                 int var87 = this.A();
                 int var88 = this.alu("or", var87, var86);
                 this.A(var88);
                 this.exx();
                 int var89 = this.A();
                 int var90 = this.HL();
                 this.wMem(var90, var89, 0xDADB);
                 int var91 = this.L();
                 int var92 = this.alu("dec", var91);
                 this.L(var92);
                 int var93 = this.HL();
                 int var94 = this.mem(var93, 0xDADD);
                 this.A(var94);
                 int var95 = this.A();
                 int var96 = this.DE();
                 this.wMem(var96, var95, 0xDADE);
                 int var97 = this.DE();
                 int var98 = this.dec16(var97);
                 this.DE(var98);
                 this.exx();
                 int var99 = this.pop();
                 this.BC(var99);
                 int var100 = this.C();
                 this.L(var100);
                 int var101 = this.HL();
                 int var102 = this.mem(var101, 0xDAE3);
                 int var103 = this.A();
                 int var104 = this.alu("and", var103, var102);
                 this.A(var104);
                 int var105 = this.B();
                 this.L(var105);
                 int var106 = this.HL();
                 int var107 = this.mem(var106, 0xDAE5);
                 int var108 = this.A();
                 int var109 = this.alu("or", var108, var107);
                 this.A(var109);
                 this.exx();
                 int var110 = this.A();
                 int var111 = this.HL();
                 this.wMem(var111, var110, 0xDAE7);
                 int var112 = this.H();
                 int var113 = this.alu("inc", var112);
                 this.H(var113);
                 int var114 = this.B() - 1 & 255;
                 this.B(var114);
                 if(this.B() != 0) {
                 } else {
                    int var115 = this.C();
                    this.A(var115);
                    this.B(8);
                    int var116 = this.B();
                    int var117 = this.A();
                    int var118 = this.alu("sub", var117, var116);
                    this.A(var118);
                    if(this.flag(1, true)) {
                    } else {
                       int var119 = this.B();
                       int var120 = this.A();
                       int var121 = this.alu("add", var120, var119);
                       this.A(var121);
                       if(this.flag(64, false)) {
                          break;
                       }

                       int var127 = this.A();
                       this.B(var127);
                       int var128 = this.A();
                       int var129 = this.A();
                       int var130 = this.alu("xor", var129, var128);
                       this.A(var130);
                    }

                    int var131 = this.A();
                    this.C(var131);
                    int var132 = this.L();
                    this.A(var132);
                    int var133 = this.A();
                    int var134 = this.alu("add", var133, 32);
                    this.A(var134);
                    int var135 = this.A();
                    this.L(var135);
                    if(this.flag(1, true)) {
                       int var222 = this.H();
                       this.A(var222);
                       int var223 = this.A();
                       int var224 = this.alu("sub", var223, 8);
                       this.A(var224);
                       int var225 = this.A();
                       this.H(var225);
                    } else {
                       int var136 = this.H();
                       this.bit(4, var136);
                       if(!this.flag(64, false)) {
                          break;
                       }

                    }
                 }

                 int var137 = this.HL();
                 int var138 = this.mem(var137, 0xDB39);
                 this.A(var138);
                 int var139 = this.A();
                 int var140 = this.DE();
                 this.wMem(var140, var139, 0xDB3A);
                 int var141 = this.DE();
                 int var142 = this.dec16(var141);
                 this.DE(var142);
                 this.exx();
                 int var143 = this.pop();
                 this.BC(var143);
                 int var144 = this.C();
                 this.L(var144);
                 int var145 = this.HL();
                 int var146 = this.mem(var145, 0xDB3F);
                 int var147 = this.A();
                 int var148 = this.alu("and", var147, var146);
                 this.A(var148);
                 int var149 = this.B();
                 this.L(var149);
                 int var150 = this.HL();
                 int var151 = this.mem(var150, 0xDB41);
                 int var152 = this.A();
                 int var153 = this.alu("or", var152, var151);
                 this.A(var153);
                 this.exx();
                 int var154 = this.A();
                 int var155 = this.HL();
                 this.wMem(var155, var154, 0xDB43);
                 int var156 = this.L();
                 int var157 = this.alu("inc", var156);
                 this.L(var157);
                 int var158 = this.HL();
                 int var159 = this.mem(var158, 0xDB45);
                 this.A(var159);
                 int var160 = this.A();
                 int var161 = this.DE();
                 this.wMem(var161, var160, 0xDB46);
                 int var162 = this.DE();
                 int var163 = this.dec16(var162);
                 this.DE(var163);
                 this.exx();
                 int var164 = this.pop();
                 this.BC(var164);
                 int var165 = this.C();
                 this.L(var165);
                 int var166 = this.HL();
                 int var167 = this.mem(var166, 0xDB4B);
                 int var168 = this.A();
                 int var169 = this.alu("and", var168, var167);
                 this.A(var169);
                 int var170 = this.B();
                 this.L(var170);
                 int var171 = this.HL();
                 int var172 = this.mem(var171, 0xDB4D);
                 int var173 = this.A();
                 int var174 = this.alu("or", var173, var172);
                 this.A(var174);
                 this.exx();
                 int var175 = this.A();
                 int var176 = this.HL();
                 this.wMem(var176, var175, 0xDB4F);
                 int var177 = this.L();
                 int var178 = this.alu("inc", var177);
                 this.L(var178);
                 int var179 = this.HL();
                 int var180 = this.mem(var179, 0xDB51);
                 this.A(var180);
                 int var181 = this.A();
                 int var182 = this.DE();
                 this.wMem(var182, var181, 0xDB52);
                 int var183 = this.DE();
                 int var184 = this.dec16(var183);
                 this.DE(var184);
                 this.exx();
                 int var185 = this.pop();
                 this.BC(var185);
                 int var186 = this.C();
                 this.L(var186);
                 int var187 = this.HL();
                 int var188 = this.mem(var187, 0xDB57);
                 int var189 = this.A();
                 int var190 = this.alu("and", var189, var188);
                 this.A(var190);
                 int var191 = this.B();
                 this.L(var191);
                 int var192 = this.HL();
                 int var193 = this.mem(var192, 0xDB59);
                 int var194 = this.A();
                 int var195 = this.alu("or", var194, var193);
                 this.A(var195);
                 this.exx();
                 int var196 = this.A();
                 int var197 = this.HL();
                 this.wMem(var197, var196, 0xDB5B);
                 int var198 = this.H();
                 int var199 = this.alu("inc", var198);
                 this.H(var199);
                 int var200 = this.B() - 1 & 255;
                 this.B(var200);
                 if(this.B() != 0) {
                 } else {
                    int var201 = this.C();
                    this.A(var201);
                    this.B(8);
                    int var202 = this.B();
                    int var203 = this.A();
                    int var204 = this.alu("sub", var203, var202);
                    this.A(var204);
                    if(this.flag(1, true)) {
                    } else {
                       int var205 = this.B();
                       int var206 = this.A();
                       int var207 = this.alu("add", var206, var205);
                       this.A(var207);
                       if(this.flag(64, false)) {
                          break;
                       }

                       int var208 = this.A();
                       this.B(var208);
                       int var209 = this.A();
                       int var210 = this.A();
                       int var211 = this.alu("xor", var210, var209);
                       this.A(var211);
                    }

                    int var212 = this.A();
                    this.C(var212);
                    int var213 = this.L();
                    this.A(var213);
                    int var214 = this.A();
                    int var215 = this.alu("add", var214, 32);
                    this.A(var215);
                    int var216 = this.A();
                    this.L(var216);
                    if(this.flag(1, true)) {
                       int var218 = this.H();
                       this.A(var218);
                       int var219 = this.A();
                       int var220 = this.alu("sub", var219, 8);
                       this.A(var220);
                       int var221 = this.A();
                       this.H(var221);
                    } else {
                       int var217 = this.H();
                       this.bit(4, var217);
                       if(!this.flag(64, false)) {
                          break;
                       }

                    }
                 }

              }

              int var122 = this.mem16(0xF6FC, 0xDB89);
              this.SP(var122);
              int var123 = this.DE();
              this.push(var123);
              int var124 = this.SP();
              this.wMem16(0xF6FC, var124, 0xDB8E);
              int var125 = this.mem16(0xF6F6, 0xDB92);
              this.SP(var125);
              int var126 = this.DE();
              this.wMem16(0xF6FA, var126, 0xDB96);
              this.unwind(0xDB9B, "DE");
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void targetRenegadeDC0CRestoresTheDrawnAreasFromTheList() {
    // Target: Renegade DC0C-DC24: saves SP at F6F6, takes the list of drawn areas as its stack (LD SP,(F6FC)) and pops it until a zero word, then restores SP from F6F6 (the code at 8000 leaves one area on the list at 7000)
    savesStackPointerAt(0xF6F6);
    ignoresMemory(0x6FF0, 0x7001);
    ignoresMemory(0xFF82, 0xFF83);
    String java = translate(with(listing("""
        8000  CD 0C DC  CALL 0xDC0C
        8003  18 FE  JR 0x8003
        DC0C  21 00 00  LD HL, 0x0000
        DC0F  22 82 FF  LD (0xFF82), HL
        DC12  ED 73 F6 F6  LD (0xF6F6), SP
        DC16  ED 7B FC F6  LD SP, (0xF6FC)
        DC1A  F1  POP AF
        DC1B  E1  POP HL
        DC1C  7D  LD A, L
        DC1D  B4  OR A, H
        DC1E  20 05  JR NZ, 0xDC25
        DC20  ED 7B F6 F6  LD SP, (0xF6F6)
        DC24  C9  RET
        DC25  7C  LD A, H
        DC26  18 FE  JR 0xDC26
        """), at(0xF6FC, 0xFC, 0x6F), at(0x6FFC, 0x00, 0x00, 0x00, 0x00)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.$DC0C();

              while(true) {
              }
           }

           public void $DC0C() {
              this.HL(0);
              int var1 = this.HL();
              this.wMem16(0xFF82, var1, 0xDC0F);
              int var2 = this.SP();
              this.wMem16(0xF6F6, var2, 0xDC12);
              int var3 = this.mem16(0xF6FC, 0xDC16);
              this.SP(var3);
              int var4 = this.pop();
              this.AF(var4);
              int var5 = this.pop();
              this.HL(var5);
              int var6 = this.L();
              this.A(var6);
              int var7 = this.H();
              int var8 = this.A();
              int var9 = this.alu("or", var8, var7);
              this.A(var9);
              if(this.flag(64, true)) {
                 int var11 = this.H();
                 this.A(var11);

                 while(true) {
                 }
              }

              int var10 = this.mem16(0xF6F6, 0xDC20);
              this.SP(var10);
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void targetRenegadeC7A8ReadsTheSecondChannelTableWithTheStackAndRestoresSP() {
    // Target: Renegade C7A8-C7F4 (bank 3 in the game, main memory here): the second channel does as C3A4 without saving SP again, its handler at C7F2 restores the SP the first channel saved at C487 (the code at 8000 saves SP there as C3A4 does; the loop at C7FA ends the scene)
    savesStackPointerAt(0xC487);
    String java = translate(with(listing("""
        8000  ED 73 87 C4  LD (0xC487), SP
        8004  C3 A8 C7  JP 0xC7A8
        C7A8  26 C7  LD H, 0xC7
        C7AA  2E 1A  LD L, 0x1A
        C7AC  35  DEC (HL)
        C7AD  20 49  JR NZ, 0xC7F8
        C7AF  3A 19 C7  LD A, (0xC719)
        C7B2  77  LD (HL), A
        C7B3  31 15 C7  LD SP, 0xC715
        C7B6  D1  POP DE
        C7B7  C1  POP BC
        C7B8  2E 1B  LD L, 0x1B
        C7BA  6E  LD L, (HL)
        C7BB  E9  JP 0x0000
        C7F2  C5  PUSH BC
        C7F3  D5  PUSH DE
        C7F4  ED 7B 87 C4  LD SP, (0xC487)
        C7F8  0E 0A  LD C, 0x0A
        C7FA  18 FE  JR 0xC7FA
        """), at(0xC715, 0x34, 0x12, 0x78, 0x56, 0x05, 0x01, 0xF2)));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              int var1 = this.SP();
              this.wMem16(0xC487, var1, 0x8000);
              this.H(199);
              this.L(26);
              int var2 = this.HL();
              int var3 = this.mem(var2, 0xC7AC);
              int var4 = this.alu("dec", var3);
              int var5 = this.HL();
              this.wMem(var5, var4, 0xC7AC);
              if(this.flag(64, true)) {
              } else {
                 int var6 = this.mem(0xC719, 0xC7AF);
                 this.A(var6);
                 int var7 = this.A();
                 int var8 = this.HL();
                 this.wMem(var8, var7, 0xC7B2);
                 this.SP(0xC715);
                 int var9 = this.pop();
                 this.DE(var9);
                 int var10 = this.pop();
                 this.BC(var10);
                 this.L(27);
                 int var11 = this.HL();
                 int var12 = this.mem(var11, 0xC7BA);
                 this.L(var12);
                 if(this.HL() != 0xC7F2) {
                    int var16 = this.HL();
                    this.jump(var16);
                    if(this.unwinding()) {
                       return;
                    }

                    return;
                 }

                 int var13 = this.BC();
                 this.push(var13);
                 int var14 = this.DE();
                 this.push(var14);
                 int var15 = this.mem16(0xC487, 0xC7F4);
                 this.SP(var15);
              }

              this.C(10);

              while(true) {
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void renegade128At9701DrawsASpriteThroughAMaskTableWithTheStackAsItsPointer() {
    // Renegade 128 96F0-9720: saves SP at 70B3, points SP at the sprite, takes each word with POP HL and masks the screen through the table at BExx, skips rows with ADD HL,SP / LD SP,HL and jumps to the restore at 97AB (the code at 8000 sets up both register sets for two rows of two bytes over FDxx)
    savesStackPointerAt(0x70B3);
    String java = translate(listing("""
        8000  21 00 90  LD HL, 0x9000
        8003  01 02 02  LD BC, 0x0202
        8006  11 00 20  LD DE, 0x2000
        8009  D9  EXX
        800A  21 00 90  LD HL, 0x9000
        800D  3E 02  LD A, 0x02
        800F  D9  EXX
        8010  CD F0 96  CALL 0x96F0
        8013  18 FE  JR 0x8013
        96F0  D9  EXX
        96F1  7E  LD A, (HL)
        96F2  23  INC HL
        96F3  93  SUB A, E
        96F4  08  EX AF, AFx
        96F5  06 00  LD B, 0x00
        96F7  CB 21  SLA C
        96F9  09  ADD HL, BC
        96FA  16 FD  LD D, 0xFD
        96FC  ED 73 B3 70  LD (0x70B3), SP
        9700  F3  DI
        9701  F9  LD SP, HL
        9702  D9  EXX
        9703  7B  LD A, E
        9704  D9  EXX
        9705  5F  LD E, A
        9706  08  EX AF, AFx
        9707  47  LD B, A
        9708  08  EX AF, AFx
        9709  E1  POP HL
        970A  4C  LD C, H
        970B  26 BE  LD H, 0xBE
        970D  1A  LD A, (DE)
        970E  A6  AND A, (HL)
        970F  69  LD L, C
        9710  B6  OR A, (HL)
        9711  12  LD (DE), A
        9712  1C  INC E
        9713  10 F4  DJNZ 0x9709
        9715  D9  EXX
        9716  69  LD L, C
        9717  26 00  LD H, 0x00
        9719  39  ADD HL, SP
        971A  F9  LD SP, HL
        971B  7B  LD A, E
        971C  92  SUB A, D
        971D  5F  LD E, A
        971E  10 E4  DJNZ 0x9704
        9720  C3 AB 97  JP 0x97AB
        97AB  ED 7B B3 70  LD SP, (0x70B3)
        97AF  FB  EI
        97B0  C9  RET
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.HL(0x9000);
              this.BC(514);
              this.DE(8192);
              this.exx();
              this.HL(0x9000);
              this.A(2);
              this.exx();
              this.$96F0();

              while(true) {
              }
           }

           public void $96F0() {
              this.exx();
              int var1 = this.HL();
              int var2 = this.mem(var1, 0x96F1);
              this.A(var2);
              int var3 = this.HL();
              int var4 = this.inc16(var3);
              this.HL(var4);
              int var5 = this.E();
              int var6 = this.A();
              int var7 = this.alu("sub", var6, var5);
              this.A(var7);
              int var8 = this.AF();
              this.exAF(var8);
              this.B(0);
              int var9 = this.C();
              int var10 = this.alu("sla", var9);
              this.C(var10);
              int var11 = this.BC();
              int var12 = this.HL();
              int var13 = this.alu("add16", var12, var11);
              this.HL(var13);
              this.D(253);
              int var14 = this.SP();
              this.wMem16(28851, var14, 0x96FC);
              this.di();
              int var15 = this.HL();
              this.SP(var15);
              this.exx();
              int var16 = this.E();
              this.A(var16);

              while(true) {
                 this.exx();
                 int var17 = this.A();
                 this.E(var17);
                 int var18 = this.AF();
                 this.exAF(var18);
                 int var19 = this.A();
                 this.B(var19);
                 int var20 = this.AF();
                 this.exAF(var20);

                 while(true) {
                    int var21 = this.pop();
                    this.HL(var21);
                    int var22 = this.H();
                    this.C(var22);
                    this.H(190);
                    int var23 = this.DE();
                    int var24 = this.mem(var23, 0x970D);
                    this.A(var24);
                    int var25 = this.HL();
                    int var26 = this.mem(var25, 0x970E);
                    int var27 = this.A();
                    int var28 = this.alu("and", var27, var26);
                    this.A(var28);
                    int var29 = this.C();
                    this.L(var29);
                    int var30 = this.HL();
                    int var31 = this.mem(var30, 0x9710);
                    int var32 = this.A();
                    int var33 = this.alu("or", var32, var31);
                    this.A(var33);
                    int var34 = this.A();
                    int var35 = this.DE();
                    this.wMem(var35, var34, 0x9711);
                    int var36 = this.E();
                    int var37 = this.alu("inc", var36);
                    this.E(var37);
                    int var38 = this.B() - 1 & 255;
                    this.B(var38);
                    if(this.B() == 0) {
                       this.exx();
                       int var39 = this.C();
                       this.L(var39);
                       this.H(0);
                       int var40 = this.SP();
                       int var41 = this.HL();
                       int var42 = this.alu("add16", var41, var40);
                       this.HL(var42);
                       int var43 = this.HL();
                       this.SP(var43);
                       int var44 = this.E();
                       this.A(var44);
                       int var45 = this.D();
                       int var46 = this.A();
                       int var47 = this.alu("sub", var46, var45);
                       this.A(var47);
                       int var48 = this.A();
                       this.E(var48);
                       int var49 = this.B() - 1 & 255;
                       this.B(var49);
                       if(this.B() == 0) {
                          int var50 = this.mem16(28851, 0x97AB);
                          this.SP(var50);
                          this.ei();
                          return;
                       }

                       break;
                    }

                 }
              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void renegade128At7C18RestartsWhereARewrittenCallGoesOnlyInTheExploration() {
    // Renegade 128 7C18: LD (8483),DE rewrites the target of the CALL at 8482, and where the exploration follows it to 7C18, LD SP,BDFF drops that call (here behind the JP NZ the recording never takes, with DE = 7C18; the loops at 7C1B and 8485 end the scene)
    ignoresMemory(0xBD00, 0xBDFF);
    String java = translateFrom(0x8000, 0xBDFF, listing("""
        8000  11 18 7C  LD DE, 0x7C18
        8003  AF  XOR A, A
        8004  C2 7D 84  JP NZ, 0x847D
        8007  18 FE  JR 0x8007
        847D  ED 53 83 84  LD (0x8483), DE
        8481  E5  PUSH HL
        8482  CD 5E 8F  CALL 0x8F5E
        8485  18 FE  JR 0x8485
        7C18  31 FF BD  LD SP, 0xBDFF
        7C1B  18 FE  JR 0x7C1B
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $7C18() {
              this.SP(0xBDFF);
              this.$7C1B();
           }

           public void $7C1B() {
              while(true) {
              }
           }

           public void $8000() {
              this.DE(31768);
              int var1 = this.A();
              int var2 = this.A();
              int var3 = this.alu("xor", var2, var1);
              this.A(var3);
              if(!this.flag(64, true)) {
                 while(true) {
                 }
              }

              int var4 = this.DE();
              this.wMem16(0x8483, var4, 0x847D);
              int var5 = this.HL();
              this.push(var5);
              int var6 = this.executeMutantCode(0x8482);
              if(!this.unwinding()) {
                 if(var6 == 0x8485) {
                    while(true) {
                    }
                 }

                 if(var6 != -1) {
                    this.goOnFrom(var6);
                    if(this.unwinding()) {
                       return;
                    }
                 }

              }
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void aReturnPoppedIntoARegisterAndPushedBackReturnsToTheCaller() {
    // Target Renegade D780 CALL E058: E0D5 POP HL takes the return; one path goes back to the loop (JP NZ,D794), the other pushes HL again and its RET at E3C7 lands on D783
    translate(listing("""
        8000  CD 00 81  CALL 0x8100
        8003  76  HALT
        8100  04  INC B
        8101  78  LD A, B
        8102  FE 06  CP A, 0x06
        8104  C8  RET Z
        8105  CD 00 82  CALL 0x8200
        8108  0C  INC C
        8109  18 F5  JR 0x8100
        8200  E1  POP HL
        8201  78  LD A, B
        8202  FE 03  CP A, 0x03
        8204  C2 00 81  JP NZ, 0x8100
        8207  C3 00 83  JP 0x8300
        8300  E5  PUSH HL
        8301  C9  RET
        """));
    Assert.assertEquals(1, endValue("BC") & 0xff);
    Assert.assertNotEquals("the code after the CALL is not the callee's", 0x8200, routineManager.findRoutineAt(0x8108).getEntryPoint());
  }

  @Test
  public void aReturnPoppedAndPushedBackInAnotherRoutineReturnsToTheCaller() {
    // Target Renegade E0D5 POP HL takes D783; E0D6 goes on through E1C5 and E32B, and the RET at E3C7 in E355 takes D783 again after a PUSH HL elsewhere
    translate(listing("""
        8000  CD 00 84  CALL 0x8400
        8003  CD 00 81  CALL 0x8100
        8006  76  HALT
        8100  04  INC B
        8101  78  LD A, B
        8102  FE 06  CP A, 0x06
        8104  C8  RET Z
        8105  CD 00 82  CALL 0x8200
        8108  0C  INC C
        8109  18 F5  JR 0x8100
        8200  E1  POP HL
        8201  78  LD A, B
        8202  FE 03  CP A, 0x03
        8204  C2 00 81  JP NZ, 0x8100
        8207  C3 00 83  JP 0x8300
        8300  E5  PUSH HL
        8301  C3 00 84  JP 0x8400
        8400  14  INC D
        8401  C9  RET
        """));
    Assert.assertEquals(1, endValue("BC") & 0xff);
  }

  @Test
  public void aCalleeThatPopsItsReturnAndJumpsBackToTheLoopKeepsTheLoopInOneJavaFrame() {
    // Target: Renegade D75D/D780 CALL E058: E058 sometimes returns, sometimes takes its return with POP HL (E0D5) and goes back into the main loop (JP NZ,D794); the loop must not grow a Java frame for every such turn
    String java = translate(listing("""
        8000  CD 00 81  CALL 0x8100
        8003  76  HALT
        8100  04  INC B
        8101  78  LD A, B
        8102  FE 06  CP A, 0x06
        8104  C8  RET Z
        8105  CD 00 82  CALL 0x8200
        8108  0C  INC C
        8109  18 F5  JR 0x8100
        8200  78  LD A, B
        8201  FE 03  CP A, 0x03
        8203  28 08  JR Z, 0x820D
        8205  E1  POP HL
        8206  14  INC D
        8207  C2 08 81  JP NZ, 0x8108
        820A  C3 00 81  JP 0x8100
        820D  C9  RET
        """));
    Assert.assertEquals("""
        import com.fpetrola.z80.minizx.MiniZX;

        public class Program extends MiniZX {
           public void $8000() {
              this.$8100();
              if(!this.unwinding()) {
                 this.halt(0x8003);
                 this.$8100();
                 if(!this.unwinding()) {
                    ;
                 }
              }
           }

           public void $8100() {
              int[] var12;
              label46:
              do {
                 if(this.isNextPC(0x8108)) {
                    this.setNextAddress(0x8108);
                 }

                 while(true) {
                    while(true) {
                       if(!this.isNextPC(0x8108)) {
                          int var3 = this.B();
                          int var4 = this.alu("inc", var3);
                          this.B(var4);
                          int var5 = this.B();
                          this.A(var5);
                          int var6 = this.A();
                          this.alu("cp", var6, 6);
                          if(this.flag(64, false)) {
                             return;
                          }

                          this.push(0x8108);
                          int var7 = this.SP();
                          this.$8200();
                          if(this.unwinding()) {
                             int[] var10 = new int[]{0x8206};
                             if(this.ownAddress(var10) == 0x8206) {
                                int var13 = this.pop();
                                this.HL(var13);
                                int var14 = this.D();
                                int var15 = this.alu("inc", var14);
                                this.D(var15);
                                if(this.flag(64, true)) {
                                   this.setNextAddress(0x8108);
                                } else {
                                }
                                continue;
                             }

                             int[] var11 = new int[]{0x8108};
                             if(this.ownAddress(var11) == -1) {
                                var12 = new int[]{0x8108};
                                continue label46;
                             }
                          } else {
                             int var8 = this.SP();
                             if(var8 == var7) {
                                this.pop();
                             } else {
                                int var9 = var7 + 2 & 0xFFFF;
                                if(var8 == var9) {
                                   return;
                                }
                             }
                          }
                       }

                       int var1 = this.C();
                       int var2 = this.alu("inc", var1);
                       this.C(var2);
                    }
                 }
              } while(this.isOwnAddress(var12));

           }

           public void $8200() {
              int var1 = this.B();
              this.A(var1);
              int var2 = this.A();
              this.alu("cp", var2, 3);
              if(this.flag(64, false)) {
                 int var3 = this.pop();
                 this.unwind(var3);
              } else {
                 this.unwind(0x8206, "HL");
              }
           }

           public void $8108() {
              this.setNextAddress(0x8108);
              this.$8100();
           }
        }
        """, withoutTiming(java));
  }

  @Test
  public void aContinuationAfterAPoppedReturnThatJumpsBackIntoTheLoopDoesNotNestTheLoop() {
    // Target: Renegade E058/E0D6: after POP HL takes the return of the loop's CALL, the code at E0D6 (here 8300) goes back into the loop at D794 (here 8108) or leaves through E1C5 to a RET that returns to the loop's caller (E3C7 to D783, here 8400 to 8003); the loop calls E058 from more than one place, as at D75D and D780
    String java = translate(listing("""
        8000  CD 00 81  CALL 0x8100
        8003  76  HALT
        8100  04  INC B
        8101  CD 00 82  CALL 0x8200
        8104  0C  INC C
        8105  CD 00 82  CALL 0x8200
        8108  1C  INC E
        8109  18 F5  JR 0x8100
        8200  78  LD A, B
        8201  E6 03  AND A, 0x03
        8203  C8  RET Z
        8204  FE 02  CP A, 0x02
        8206  CA 20 82  JP Z, 0x8220
        8209  E1  POP HL
        820A  14  INC D
        820B  CD 00 85  CALL 0x8500
        820E  7A  LD A, D
        820F  FE 06  CP A, 0x06
        8211  CA 00 84  JP Z, 0x8400
        8214  FE 03  CP A, 0x03
        8216  DA 00 81  JP C, 0x8100
        8219  C2 08 81  JP NZ, 0x8108
        821C  C3 00 86  JP 0x8600
        8220  04  INC B
        8221  C3 00 82  JP 0x8200
        8400  3E 07  LD A, 0x07
        8402  C9  RET
        8500  2C  INC L
        8501  C9  RET
        8600  3E 09  LD A, 0x09
        8602  C9  RET
        """));
    System.err.println("JAVA<<" + withoutTiming(java) + ">>JAVA");
  }

  @Test
  public void targetRenegadeE0D6GoesBackIntoTheMainLoopThatCaughtItInsteadOfCallingItAgain() {
    // Target: Renegade D6E4-D7A0 and E058-E3D0 as they are (the routines they call only return): E058 takes its return with POP HL at E0D5, the main loop's catch runs E0D6, and E0D6 goes back to D794; calling $D6E4 again nested one more loop on every such turn until, at frame 52976, a RET unwound the game out of its loop
    ignoresMemory(0xBF00, 0xBFC0);
    String java = translateFrom(0xD6E4, 0xBFB6, listing("""
        D6E4  31 B8 BF  LD SP, 0xBFB8
        D6E7  21 13 F9  LD HL, 0xF913
        D6EA  34  INC (HL)
        D6EB  3A 14 F9  LD A, (0xF914)
        D6EE  BE  CP A, (HL)
        D6EF  C4 FF 5E  CALL NZ, 0x5EFF
        D6F2  21 40 6A  LD HL, 0x6A40
        D6F5  22 F0 F6  LD (0xF6F0), HL
        D6F8  21 86 68  LD HL, 0x6886
        D6FB  22 17 F9  LD (0xF917), HL
        D6FE  21 00 D2  LD HL, 0xD200
        D701  11 01 D2  LD DE, 0xD201
        D704  01 A3 00  LD BC, 0x00A3
        D707  70  LD (HL), B
        D708  ED B0  LDIR
        D70A  21 0E 2A  LD HL, 0x2A0E
        D70D  22 8E D2  LD (0xD28E), HL
        D710  21 40 60  LD HL, 0x6040
        D713  22 90 D2  LD (0xD290), HL
        D716  3A 03 ED  LD A, (0xED03)
        D719  FE 05  CP A, 0x05
        D71B  20 0B  JR NZ, 0xD728
        D71D  FD 21 8E D2  LD IY, 0xD28E
        D721  FD 36 0A 40  LD (IY+0A), 0x40
        D725  CD 26 E7  CALL 0xE726
        D728  3A 35 F9  LD A, (0xF935)
        D72B  B7  OR A, A
        D72C  28 0D  JR Z, 0xD73B
        D72E  21 04 50  LD HL, 0x5004
        D731  22 0C D2  LD (0xD20C), HL
        D734  FD 21 0A D2  LD IY, 0xD20A
        D738  CD AA DC  CALL 0xDCAA
        D73B  3A 36 F9  LD A, (0xF936)
        D73E  B7  OR A, A
        D73F  28 0D  JR Z, 0xD74E
        D741  21 0A 6C  LD HL, 0x6C0A
        D744  22 22 D2  LD (0xD222), HL
        D747  FD 21 20 D2  LD IY, 0xD220
        D74B  CD AA DC  CALL 0xDCAA
        D74E  CD 65 F5  CALL 0xF565
        D751  CD 1B 5F  CALL 0x5F1B
        D754  21 40 6A  LD HL, 0x6A40
        D757  22 F0 F6  LD (0xF6F0), HL
        D75A  CD 55 E3  CALL 0xE355
        D75D  CD 58 E0  CALL 0xE058
        D760  CD 8B E8  CALL 0xE88B
        D763  21 00 FB  LD HL, 0xFB00
        D766  11 01 FB  LD DE, 0xFB01
        D769  75  LD (HL), L
        D76A  01 00 05  LD BC, 0x0500
        D76D  ED B0  LDIR
        D76F  CD 65 F4  CALL 0xF465
        D772  3E 01  LD A, 0x01
        D774  06 FF  LD B, 0xFF
        D776  CD 68 5D  CALL 0x5D68
        D779  CD 55 E3  CALL 0xE355
        D77C  F3  DI
        D77D  CD D3 5D  CALL 0x5DD3
        D780  CD 58 E0  CALL 0xE058
        D783  CD 06 DD  CALL 0xDD06
        D786  CD AA E7  CALL 0xE7AA
        D789  ED 43 19 D2  LD (0xD219), BC
        D78D  CD A0 E7  CALL 0xE7A0
        D790  ED 43 2F D2  LD (0xD22F), BC
        D794  FD 21 0A D2  LD IY, 0xD20A
        D798  06 06  LD B, 0x06
        D79A  FD E5  PUSH IY
        D79C  C5  PUSH BC
        D79D  CD A6 F3  CALL 0xF3A6
        D7A0  C1  POP BC
        E058  3A AF F7  LD A, (0xF7AF)
        E05B  11 39 D2  LD DE, 0xD239
        E05E  C6 03  ADD A, 0x03
        E060  47  LD B, A
        E061  0E 16  LD C, 0x16
        E063  1A  LD A, (DE)
        E064  B7  OR A, A
        E065  28 06  JR Z, 0xE06D
        E067  7B  LD A, E
        E068  81  ADD A, C
        E069  5F  LD E, A
        E06A  10 F7  DJNZ 0xE063
        E06C  C9  RET
        E06D  1D  DEC E
        E06E  1D  DEC E
        E06F  1D  DEC E
        E070  D5  PUSH DE
        E071  FD E1  POP IY
        E073  2A 17 F9  LD HL, (0xF917)
        E076  7E  LD A, (HL)
        E077  3C  INC A
        E078  CA E9 E2  JP Z, 0xE2E9
        E07B  3C  INC A
        E07C  C2 00 E2  JP NZ, 0xE200
        E07F  21 00 D2  LD HL, 0xD200
        E082  7E  LD A, (HL)
        E083  23  INC HL
        E084  B7  OR A, A
        E085  28 07  JR Z, 0xE08E
        E087  D6 36  SUB A, 0x36
        E089  FE 58  CP A, 0x58
        E08B  30 F5  JR NC, 0xE082
        E08D  C9  RET
        E08E  3E 01  LD A, 0x01
        E090  32 80 68  LD (0x6880), A
        E093  3A 03 ED  LD A, (0xED03)
        E096  D6 03  SUB A, 0x03
        E098  FE 03  CP A, 0x03
        E09A  30 0C  JR NC, 0xE0A8
        E09C  3A 50 F6  LD A, (0xF650)
        E09F  21 E1 D5  LD HL, 0xD5E1
        E0A2  E6 20  AND A, 0x20
        E0A4  C4 55 F4  CALL NZ, 0xF455
        E0A7  AF  XOR A, A
        E0A8  C6 03  ADD A, 0x03
        E0AA  30 0D  JR NC, 0xE0B9
        E0AC  FD 21 0A D2  LD IY, 0xD20A
        E0B0  CD DB F3  CALL 0xF3DB
        E0B3  FD 2E 20  LD IYL, 0x20
        E0B6  CD DB F3  CALL 0xF3DB
        E0B9  2A 0C D2  LD HL, (0xD20C)
        E0BC  ED 5B 83 68  LD DE, (0x6883)
        E0C0  CD D3 DF  CALL 0xDFD3
        E0C3  32 19 D2  LD (0xD219), A
        E0C6  4F  LD C, A
        E0C7  2A 22 D2  LD HL, (0xD222)
        E0CA  7A  LD A, D
        E0CB  C6 08  ADD A, 0x08
        E0CD  57  LD D, A
        E0CE  1D  DEC E
        E0CF  CD D3 DF  CALL 0xDFD3
        E0D2  32 2F D2  LD (0xD22F), A
        E0D5  E1  POP HL
        E0D6  B1  OR A, C
        E0D7  C2 94 D7  JP NZ, 0xD794
        E0DA  CD 9A F6  CALL 0xF69A
        E0DD  21 03 ED  LD HL, 0xED03
        E0E0  7E  LD A, (HL)
        E0E1  34  INC (HL)
        E0E2  FE 03  CP A, 0x03
        E0E4  DA 85 E1  JP C, 0xE185
        E0E7  FE 07  CP A, 0x07
        E0E9  CA C5 E1  JP Z, 0xE1C5
        E0EC  FE 08  CP A, 0x08
        E0EE  20 1A  JR NZ, 0xE10A
        E0F0  06 03  LD B, 0x03
        E0F2  21 42 E1  LD HL, 0xE142
        E0F5  CD 8D F9  CALL 0xF98D
        E0F8  26 58  LD H, 0x58
        E0FA  3E 26  LD A, 0x26
        E0FC  06 13  LD B, 0x13
        E0FE  6F  LD L, A
        E0FF  36 38  LD (HL), 0x38
        E101  2C  INC L
        E102  10 FB  DJNZ 0xE0FF
        E104  C6 20  ADD A, 0x20
        E106  FE 80  CP A, 0x80
        E108  38 F2  JR C, 0xE0FC
        E10A  3A 35 F9  LD A, (0xF935)
        E10D  FD 2E 0A  LD IYL, 0x0A
        E110  11 0A 09  LD DE, 0x090A
        E113  B7  OR A, A
        E114  C4 51 F6  CALL NZ, 0xF651
        E117  FD 2E 20  LD IYL, 0x20
        E11A  3A 36 F9  LD A, (0xF936)
        E11D  11 0A 09  LD DE, 0x090A
        E120  B7  OR A, A
        E121  C4 51 F6  CALL NZ, 0xF651
        E124  3E 03  LD A, 0x03
        E126  06 FF  LD B, 0xFF
        E128  CD 68 5D  CALL 0x5D68
        E12B  CD C6 5D  CALL 0x5DC6
        E12E  3A 03 ED  LD A, (0xED03)
        E131  FE 08  CP A, 0x08
        E133  DA E4 D6  JP C, 0xD6E4
        E136  FE 0B  CP A, 0x0B
        E138  38 A0  JR C, 0xE0DA
        E13A  3E FF  LD A, 0xFF
        E13C  32 84 E1  LD (0xE184), A
        E13F  C3 D6 D6  JP 0xD6D6
        E185  21 01 E0  LD HL, 0xE001
        E188  CD 12 E0  CALL 0xE012
        E18B  FD 21 8E D2  LD IY, 0xD28E
        E18F  CD 09 E7  CALL 0xE709
        E192  FD 36 0A 00  LD (IY+0A), 0x00
        E196  06 11  LD B, 0x11
        E198  C5  PUSH BC
        E199  2A F0 F6  LD HL, (0xF6F0)
        E19C  11 38 00  LD DE, 0x0038
        E19F  19  ADD HL, DE
        E1A0  22 F0 F6  LD (0xF6F0), HL
        E1A3  CD 55 E3  CALL 0xE355
        E1A6  CD 65 F4  CALL 0xF465
        E1A9  C1  POP BC
        E1AA  10 EC  DJNZ 0xE198
        E1AC  21 17 F9  LD HL, 0xF917
        E1AF  34  INC (HL)
        E1B0  3E 4A  LD A, 0x4A
        E1B2  32 80 68  LD (0x6880), A
        E1B5  21 F0 DF  LD HL, 0xDFF0
        E1B8  CD 12 E0  CALL 0xE012
        E1BB  21 83 68  LD HL, 0x6883
        E1BE  7E  LD A, (HL)
        E1BF  EE 1F  XOR A, 0x1F
        E1C1  77  LD (HL), A
        E1C2  C3 83 D7  JP 0xD783
        E1C5  11 00 64  LD DE, 0x6400
        E1C8  21 E4 70  LD HL, 0x70E4
        E1CB  01 00 02  LD BC, 0x0200
        E1CE  ED B0  LDIR
        E1D0  16 68  LD D, 0x68
        E1D2  0E 40  LD C, 0x40
        E1D4  ED B0  LDIR
        E1D6  21 78 6A  LD HL, 0x6A78
        E1D9  22 F0 F6  LD (0xF6F0), HL
        E1DC  3E 08  LD A, 0x08
        E1DE  32 83 68  LD (0x6883), A
        E1E1  3E 48  LD A, 0x48
        E1E3  32 80 68  LD (0x6880), A
        E1E6  21 0D D2  LD HL, 0xD20D
        E1E9  7E  LD A, (HL)
        E1EA  B7  OR A, A
        E1EB  28 02  JR Z, 0xE1EF
        E1ED  36 48  LD (HL), 0x48
        E1EF  2E 23  LD L, 0x23
        E1F1  7E  LD A, (HL)
        E1F2  B7  OR A, A
        E1F3  28 04  JR Z, 0xE1F9
        E1F5  36 4C  LD (HL), 0x4C
        E1F7  2D  DEC L
        E1F8  35  DEC (HL)
        E1F9  21 83 D7  LD HL, 0xD783
        E1FC  E5  PUSH HL
        E1FD  C3 2B E3  JP 0xE32B
        E200  23  INC HL
        E201  CB 76  BIT 6, (HL)
        E203  2B  DEC HL
        E204  28 0F  JR Z, 0xE215
        E206  3A AF F7  LD A, (0xF7AF)
        E209  B7  OR A, A
        E20A  20 09  JR NZ, 0xE215
        E20C  23  INC HL
        E20D  23  INC HL
        E20E  23  INC HL
        E20F  22 17 F9  LD (0xF917), HL
        E212  C3 58 E0  JP 0xE058
        E215  1C  INC E
        E216  1C  INC E
        E217  7E  LD A, (HL)
        E218  12  LD (DE), A
        E219  23  INC HL
        E21A  1C  INC E
        E21B  3E 38  LD A, 0x38
        E21D  CB 7E  BIT 7, (HL)
        E21F  20 1B  JR NZ, 0xE23C
        E221  3A 0D D2  LD A, (0xD20D)
        E224  47  LD B, A
        E225  3A 23 D2  LD A, (0xD223)
        E228  4F  LD C, A
        E229  ED 5F  LDAR A, R
        E22B  1F  RRA A
        E22C  38 03  JR C, 0xE231
        E22E  78  LD A, B
        E22F  41  LD B, C
        E230  4F  LD C, A
        E231  79  LD A, C
        E232  B7  OR A, A
        E233  20 01  JR NZ, 0xE236
        E235  78  LD A, B
        E236  FE 40  CP A, 0x40
        E238  30 02  JR NC, 0xE23C
        E23A  3E 60  LD A, 0x60
        E23C  12  LD (DE), A
        E23D  1C  INC E
        E23E  AF  XOR A, A
        E23F  12  LD (DE), A
        E240  57  LD D, A
        E241  5E  LD E, (HL)
        E242  CB BB  RES 7, E
        E244  CB B3  RES 6, E
        E246  23  INC HL
        E247  7E  LD A, (HL)
        E248  23  INC HL
        E249  22 17 F9  LD (0xF917), HL
        E24C  FD 77 06  LD (IY+06), A
        E24F  FD 73 14  LD (IY+14), E
        E252  21 C7 D6  LD HL, 0xD6C7
        E255  19  ADD HL, DE
        E256  7E  LD A, (HL)
        E257  FD 77 01  LD (IY+01), A
        E25A  21 B9 D6  LD HL, 0xD6B9
        E25D  19  ADD HL, DE
        E25E  3A 84 E1  LD A, (0xE184)
        E261  E6 08  AND A, 0x08
        E263  86  ADD A, (HL)
        E264  FD 77 0E  LD (IY+0E), A
        E267  21 AA F9  LD HL, 0xF9AA
        E26A  19  ADD HL, DE
        E26B  3A 84 E1  LD A, (0xE184)
        E26E  3E 00  LD A, 0x00
        E270  E6 10  AND A, 0x10
        E272  86  ADD A, (HL)
        E273  FD 77 0D  LD (IY+0D), A
        E276  FD 77 0C  LD (IY+0C), A
        E279  21 C4 BF  LD HL, 0xBFC4
        E27C  19  ADD HL, DE
        E27D  3A 84 E1  LD A, (0xE184)
        E280  4F  LD C, A
        E281  ED 5F  LDAR A, R
        E283  A1  AND A, C
        E284  E6 10  AND A, 0x10
        E286  B6  OR A, (HL)
        E287  FD 77 15  LD (IY+15), A
        E28A  FD 36 0A 08  LD (IY+0A), 0x08
        E28E  D5  PUSH DE
        E28F  CD 26 E7  CALL 0xE726
        E292  D1  POP DE
        E293  21 B6 EA  LD HL, 0xEAB6
        E296  7B  LD A, E
        E297  D6 0B  SUB A, 0x0B
        E299  38 3F  JR C, 0xE2DA
        E29B  FD CB 15 A6  RES 4, (IY+15)
        E29F  20 21  JR NZ, 0xE2C2
        E2A1  21 C8 E3  LD HL, 0xE3C8
        E2A4  AF  XOR A, A
        E2A5  CD 00 EC  CALL 0xEC00
        E2A8  FD CB 0A FE  SET 7, (IY+0A)
        E2AC  0E 00  LD C, 0x00
        E2AE  CD 09 E6  CALL 0xE609
        E2B1  FD 77 12  LD (IY+12), A
        E2B4  FD 7D  LD A, IYL
        E2B6  C6 19  ADD A, 0x19
        E2B8  FD 77 13  LD (IY+13), A
        E2BB  FD 36 04 01  LD (IY+04), 0x01
        E2BF  C3 58 E0  JP 0xE058
        E2C2  3D  DEC A
        E2C3  20 0A  JR NZ, 0xE2CF
        E2C5  21 31 E6  LD HL, 0xE631
        E2C8  FD 36 11 06  LD (IY+11), 0x06
        E2CC  AF  XOR A, A
        E2CD  18 0D  JR 0xE2DC
        E2CF  0E 00  LD C, 0x00
        E2D1  CD 09 E6  CALL 0xE609
        E2D4  FD 77 12  LD (IY+12), A
        E2D7  21 4E E5  LD HL, 0xE54E
        E2DA  3E 80  LD A, 0x80
        E2DC  CD 00 EC  CALL 0xEC00
        E2DF  AF  XOR A, A
        E2E0  32 3D F9  LD (0xF93D), A
        E2E3  FD 77 13  LD (IY+13), A
        E2E6  C3 58 E0  JP 0xE058
        E2E9  EB  EX DE, HL
        E2EA  21 3D F9  LD HL, 0xF93D
        E2ED  35  DEC (HL)
        E2EE  20 0A  JR NZ, 0xE2FA
        E2F0  1B  DEC DE
        E2F1  1B  DEC DE
        E2F2  1B  DEC DE
        E2F3  ED 53 17 F9  LD (0xF917), DE
        E2F7  C3 58 E0  JP 0xE058
        E2FA  2A 0C D2  LD HL, (0xD20C)
        E2FD  ED 5B 22 D2  LD DE, (0xD222)
        E301  3A 03 ED  LD A, (0xED03)
        E304  FE 02  CP A, 0x02
        E306  20 0A  JR NZ, 0xE312
        E308  7B  LD A, E
        E309  2F  CPL A
        E30A  C6 20  ADD A, 0x20
        E30C  5F  LD E, A
        E30D  7D  LD A, L
        E30E  2F  CPL A
        E30F  C6 20  ADD A, 0x20
        E311  6F  LD L, A
        E312  0E 1C  LD C, 0x1C
        E314  7A  LD A, D
        E315  B7  OR A, A
        E316  28 0D  JR Z, 0xE325
        E318  7B  LD A, E
        E319  FE 0F  CP A, 0x0F
        E31B  D8  RET C
        E31C  B9  CP A, C
        E31D  38 06  JR C, 0xE325
        E31F  7C  LD A, H
        E320  B7  OR A, A
        E321  28 08  JR Z, 0xE32B
        E323  0E 0F  LD C, 0x0F
        E325  7C  LD A, H
        E326  B7  OR A, A
        E327  C8  RET Z
        E328  7D  LD A, L
        E329  B9  CP A, C
        E32A  D8  RET C
        E32B  2A 17 F9  LD HL, (0xF917)
        E32E  23  INC HL
        E32F  22 17 F9  LD (0xF917), HL
        E332  11 0C 00  LD DE, 0x000C
        E335  2A F0 F6  LD HL, (0xF6F0)
        E338  3A 03 ED  LD A, (0xED03)
        E33B  FE 02  CP A, 0x02
        E33D  20 03  JR NZ, 0xE342
        E33F  11 F4 FF  LD DE, 0xFFF4
        E342  19  ADD HL, DE
        E343  22 F0 F6  LD (0xF6F0), HL
        E346  21 0C D2  LD HL, 0xD20C
        E349  06 07  LD B, 0x07
        E34B  0E 16  LD C, 0x16
        E34D  7E  LD A, (HL)
        E34E  93  SUB A, E
        E34F  77  LD (HL), A
        E350  7D  LD A, L
        E351  81  ADD A, C
        E352  6F  LD L, A
        E353  10 F8  DJNZ 0xE34D
        E355  CD E8 5D  CALL 0x5DE8
        E358  3E 38  LD A, 0x38
        E35A  08  EX AF, AFx
        E35B  11 00 40  LD DE, 0x4000
        E35E  D9  EXX
        E35F  2A F0 F6  LD HL, (0xF6F0)
        E362  ED 5B 81 68  LD DE, (0x6881)
        E366  06 11  LD B, 0x11
        E368  D9  EXX
        E369  06 20  LD B, 0x20
        E36B  D9  EXX
        E36C  7E  LD A, (HL)
        E36D  23  INC HL
        E36E  D9  EXX
        E36F  D5  PUSH DE
        E370  4F  LD C, A
        E371  87  ADD A, A
        E372  6F  LD L, A
        E373  26 19  LD H, 0x19
        E375  29  ADD HL, HL
        E376  29  ADD HL, HL
        E377  7E  LD A, (HL)
        E378  12  LD (DE), A
        E379  14  INC D
        E37A  2C  INC L
        E37B  7E  LD A, (HL)
        E37C  12  LD (DE), A
        E37D  14  INC D
        E37E  2C  INC L
        E37F  7E  LD A, (HL)
        E380  12  LD (DE), A
        E381  14  INC D
        E382  2C  INC L
        E383  7E  LD A, (HL)
        E384  12  LD (DE), A
        E385  14  INC D
        E386  2C  INC L
        E387  7E  LD A, (HL)
        E388  12  LD (DE), A
        E389  14  INC D
        E38A  2C  INC L
        E38B  7E  LD A, (HL)
        E38C  12  LD (DE), A
        E38D  14  INC D
        E38E  2C  INC L
        E38F  7E  LD A, (HL)
        E390  12  LD (DE), A
        E391  14  INC D
        E392  2C  INC L
        E393  7E  LD A, (HL)
        E394  12  LD (DE), A
        E395  21 00 58  LD HL, 0x5800
        E398  CB 7A  BIT 7, D
        E39A  28 02  JR Z, 0xE39E
        E39C  2E 20  LD L, 0x20
        E39E  7A  LD A, D
        E39F  E6 18  AND A, 0x18
        E3A1  0F  RRCA A
        E3A2  0F  RRCA A
        E3A3  0F  RRCA A
        E3A4  57  LD D, A
        E3A5  19  ADD HL, DE
        E3A6  16 68  LD D, 0x68
        E3A8  59  LD E, C
        E3A9  1A  LD A, (DE)
        E3AA  77  LD (HL), A
        E3AB  EE 40  XOR A, 0x40
        E3AD  08  EX AF, AFx
        E3AE  D1  POP DE
        E3AF  1C  INC E
        E3B0  10 B9  DJNZ 0xE36B
        E3B2  08  EX AF, AFx
        E3B3  EE 40  XOR A, 0x40
        E3B5  08  EX AF, AFx
        E3B6  20 04  JR NZ, 0xE3BC
        E3B8  7A  LD A, D
        E3B9  C6 08  ADD A, 0x08
        E3BB  57  LD D, A
        E3BC  CB 7A  BIT 7, D
        E3BE  20 03  JR NZ, 0xE3C3
        E3C0  11 00 C0  LD DE, 0xC000
        E3C3  D9  EXX
        E3C4  19  ADD HL, DE
        E3C5  10 A1  DJNZ 0xE368
        E3C7  C9  RET
        E3C8  FD 7E 02  LD A, (IY+02)
        E3CB  3C  INC A
        E3CC  FD CB 00 7E  BIT 7, (IY+00)
        E3D0  28 02  JR Z, 0xE3D4
        """, 0x5D68, 0x5DC6, 0x5DD3, 0x5DE8, 0x5EFF, 0x5F1B, 0xD6D6, 0xDCAA, 0xDD06, 0xDFD3, 0xE012, 0xE609, 0xE709, 0xE726, 0xE7A0, 0xE7AA, 0xE88B, 0xEC00, 0xF3A6, 0xF3DB, 0xF455, 0xF465, 0xF565, 0xF651, 0xF69A, 0xF98D));
    String continuation = withoutTiming(java.substring(java.indexOf("public void $E0D6()")));
    continuation = continuation.substring(0, continuation.indexOf("\n   }\n"));
    Assert.assertFalse(continuation, continuation.contains("this.$D6E4()") || continuation.contains("this.$D794()"));
    Assert.assertTrue(continuation, continuation.contains("this.unwind(0xD794);"));
    Assert.assertTrue("D794, dispatched at the start of its owner, can be reached by address", withoutTiming(java).contains("public void $D794() {\n      this.setNextAddress(0xD794);"));
  }

  @Test
  public void aCallWhoseTargetIsRewrittenSwitchesOverTheRecordedTargets() {
    // Dizzy E299, Equinox D015
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x23, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0xCD, 0x00, 0x00, 0xC9),
        at(0x8020, 0x06, 0x01, 0xC9, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("8000 8018 8020 8023", routines());
    Assert.assertEquals(CodeVersions.Kind.INSTRUCTION, versions.kinds().get(0x8018));
    Assert.assertEquals(2, versions.instructionVersions(0x8018).size());
  }

  @Test
  public void anOpcodeThatIsRewrittenSwitchesOverTheRecordedOpcodes() {
    // Equinox D035/D0EE INC E <-> INC D, Emlyn 9ACE RLCA <-> RRCA
    String java = translate(
        at(0x8000, 0x3E, 0x1C, 0x32, 0x18, 0x80, 0xCD, 0x18, 0x80, 0x3E, 0x14, 0x32, 0x18, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0x00, 0xC9));
    Assert.assertEquals("8000 8018", routines());
    Assert.assertEquals(CodeVersions.Kind.INSTRUCTION, versions.kinds().get(0x8018));
    Assert.assertEquals(2, versions.instructionVersions(0x8018).size());
  }

  @Test
  public void anOperandThatIsRewrittenIsReadFromMemory() {
    // Equinox 838E CP n, Emlyn AB94
    String java = translate(
        at(0x8000, 0x3E, 0x05, 0x32, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x3E, 0x07, 0x32, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0xFE, 0x00, 0xC9));
    Assert.assertEquals("8000 8018", routines());
    Assert.assertEquals(CodeVersions.Kind.OPERAND, versions.kinds().get(0x8018));
  }

  @Test
  public void aConditionalJumpWhoseTargetIsRewrittenSwitchesOverTheTargets() {
    // Equinox CEC1 JP C,nn
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x1A, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x23, 0x80, 0x22, 0x1A, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0x37, 0xDA, 0x00, 0x00, 0xC9),
        at(0x8020, 0x06, 0x01, 0xC9, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("8000 8018 8020 8023", routines());
    Assert.assertEquals(CodeVersions.Kind.INSTRUCTION, versions.kinds().get(0x8019));
    Assert.assertEquals(2, versions.instructionVersions(0x8019).size());
  }

  @Test
  public void twoPopsOfReturnAddressesUnwindTwoLevels() {
    // JSW 37046/37047: JP Z into POP HL; POP HL when Willy dies
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD, 0xE1, 0xE1, 0x0E, 0x02, 0x76, 0x18, 0xFD),
        at(0x8010, 0xCD, 0x18, 0x80, 0xC9),
        at(0x8018, 0xAF, 0xCA, 0x08, 0x80, 0xC9));
    Assert.assertEquals(Set.of(0x8008), Set.copyOf(stackAnalyzer.poppedCallSites.get(0x8010)));
    Assert.assertEquals(Set.of(0x8009), Set.copyOf(stackAnalyzer.poppedCallSites.get(0x8000)));
  }

  @Test
  public void aRoutineThatSkipsTheDataAfterItsCallReturnsPastItEvenWithDataPushedAndPoppedInBetween() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x2A, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xE1, 0xD5, 0xD1, 0x23, 0xE9));
    Assert.assertEquals(Integer.valueOf(0x8004), stackAnalyzer.callContinuations.get(0x8000));
  }

  @Test
  public void aRoutineThatSkipsTheDataAfterItsCallByPushingTheShiftedReturnReturnsPastIt() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x2A, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xE1, 0xD5, 0xD1, 0x23, 0xE5, 0xC9));
    Assert.assertEquals(Integer.valueOf(0x8004), stackAnalyzer.callContinuations.get(0x8000));
  }

  @Test
  public void aRoutineThatSkipsTheDataAfterItsCallReturnsPastItAfterPeekingAtAParameterBelowTheReturn() {
    translate(
        at(0x8000, 0xD5, 0xCD, 0x10, 0x80, 0x00, 0xD1, 0x76, 0x18, 0xF7),
        at(0x8010, 0xE1, 0xD1, 0xD5, 0x23, 0xE9));
    Assert.assertEquals(Integer.valueOf(0x8005), stackAnalyzer.callContinuations.get(0x8001));
  }

  @Test
  public void aRoutineThatSkipsTheDataAfterItsCallReturnsPastIt() {
    // Emlyn 721D/7218: text after the CALL, ending the routine with JP (HL)
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x2A, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xE1, 0x23, 0xE9));
    Assert.assertEquals("8000 8010", routines());
    Assert.assertEquals(Integer.valueOf(0x8004), stackAnalyzer.callContinuations.get(0x8000));
  }

  @Test
  public void aRetAfterPushingAnAddressJumpsThere() {
    // Dizzy: jump table by return address
    String java = translate(
        at(0x8000, 0x21, 0x08, 0x80, 0xE5, 0xC9),
        at(0x8008, 0x06, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertEquals(Set.of(0x8008), Set.copyOf(stackAnalyzer.pushedValues.get(0x8003)));
    Assert.assertEquals(Set.of(0x8008), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8004)));
  }

  @Test
  public void aCallToAJumpThroughHlCallsTheTarget() {
    // Emlyn menu: CALL 162C, the ROM JP (HL)
    String java = translate(
        at(0x8000, 0x21, 0x08, 0x80, 0xCD, 0x00, 0x4F, 0x76),
        at(0x8008, 0x06, 0x01, 0xC9),
        at(0x4F00, 0xE9));
    Assert.assertEquals("8000 8008", routines());
    Assert.assertEquals(Set.of(0x8008), Set.copyOf(stackAnalyzer.calledThrough.get(0x8003)));
  }

  @Test
  public void aPushedAddressIsAPlantedContinuation() {
    // Emlyn 616E plants 660D
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76),
        at(0x8010, 0x21, 0x18, 0x80, 0xE5, 0xC3, 0x1C, 0x80),
        at(0x8018, 0x06, 0x01, 0xC9),
        at(0x801C, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("8000 8010", routines());
    Assert.assertEquals(Set.of(0x8018), Set.copyOf(stackAnalyzer.pushedValues.get(0x8013)));
    Assert.assertEquals(RoutineManager.Exit.DISPATCH, routineManager.exitOf(0x801E, stackAnalyzer).kind());
  }

  @Test
  public void resettingTheStackReturnsTwoLevels() {
    // Dizzy F877: LD SP,nn; RET
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76),
        at(0x8010, 0xCD, 0x18, 0x80, 0xC9),
        at(0x8018, 0x31, 0xFE, 0xFE, 0xC9));
    Assert.assertEquals(Set.of(0x8003), Set.copyOf(routineManager.nonLocalReturns.get(0x801B)));
    Assert.assertEquals(RoutineManager.Exit.NON_LOCAL, routineManager.exitOf(0x801B, stackAnalyzer).kind());
  }

  @Test
  public void aRewrittenCallWhoseTargetPopsItsReturnAddressReturnsToTheCallerOfTheCall() {
    // Equinox D015 -> D08E: POP AF of the return address, RET one level up
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x28, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0xCD, 0x00, 0x00, 0x06, 0x05, 0xC9),
        at(0x8020, 0xF1, 0x0E, 0x01, 0xC9),
        at(0x8028, 0x16, 0x02, 0xC9));
    Assert.assertEquals(2, versions.instructionVersions(0x8018).size());
    Assert.assertEquals(Set.of(0x8020), Set.copyOf(stackAnalyzer.poppedCallSites.get(0x8018)));
  }

  @Test
  public void aBlockRewrittenWithInstructionsOfOtherLengthsRunsFromACopyPerRecordedShape() {
    // Emlyn 9AF7 line drawer and 9BBF template
    String java = translateWithBlock(new int[]{0x8018, 0x801C},
        at(0x8000, 0x21, 0x3C, 0x3C, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0xC6, 0x05, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0x47, 0x00, 0x00, 0xC9));
    Assert.assertEquals(CodeVersions.Kind.BLOCK, versions.kinds().get(0x8019));
    Assert.assertEquals(List.of(0xE000, 0xE005), routineManager.codeVariants.stream().map(RoutineManager.CodeVariant::relocatedAt).sorted().toList());
  }

  @Test
  public void aCallToAJumpThroughHlInTheGameCallsTheTarget() {
    // like Emlyn's CALL 162C, but with the JP (HL) in the game's own code
    String java = translate(
        at(0x8000, 0x21, 0x08, 0x80, 0xCD, 0x30, 0x80, 0x76),
        at(0x8008, 0x06, 0x01, 0xC9),
        at(0x8030, 0xE9));
    Assert.assertEquals("8000 8008 8030", routines());
    Assert.assertEquals(Set.of(0x8008), Set.copyOf(stackAnalyzer.calledThrough.get(0x8003)));
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
    Assert.assertEquals(levels + 1, routineManager.getRoutines().size());
  }

  @Test
  public void aPlainRoutineCalledWhereARewrittenCallOnceDiscardedItsReturnStillReturnsNormally() {
    // Equinox CFC8: its RET was taken as non-local because D015's callee had popped a return address at the slot below
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x28, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0xCD, 0x30, 0x80, 0x76),
        at(0x8018, 0xCD, 0x00, 0x00, 0xC9),
        at(0x8020, 0xF1, 0xC9),
        at(0x8028, 0xC9),
        at(0x8030, 0x06, 0x01, 0xC9));
    Assert.assertEquals("8000 8018 8020 8021 8028 8030", routines());
    Assert.assertFalse(routineManager.pushedReturnSites.contains(0x8012));
    Assert.assertEquals(RoutineManager.Exit.RETURN, routineManager.exitOf(0x8032, stackAnalyzer).kind());
  }

  @Test
  public void theCodeAfterAPoppedReturnRunsWhenThePoppingTargetComesAfterTheCallIsVersioned() {
    // Equinox D015 -> D08E with the popping target recorded after the CALL already has two versions
    String java = translate(
        at(0x8000, 0x21, 0x28, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0xCD, 0x00, 0x00, 0x06, 0x05, 0xC9),
        at(0x8020, 0xF1, 0x0E, 0x01, 0xC9),
        at(0x8028, 0x16, 0x02, 0xC9));
    Assert.assertEquals(2, versions.instructionVersions(0x8018).size());
    Assert.assertEquals(Set.of(0x8020), Set.copyOf(stackAnalyzer.poppedCallSites.get(0x8018)));
    Assert.assertTrue(routines(), routines().contains("8020"));
  }

  @Test
  public void withoutTheFenceAJumpIntoTheMiddleOfARecordedInstructionIsNotDecoded() {
    // Emlyn FD20: the middle of the CALL C1CD at FD1F, read as CALL 11C1 once the recording stopped fencing the exploration
    String java = translate(at(0x8000, 0xAF, 0xC2, 0x05, 0x80, 0x21, 0x76, 0x00, 0x76, 0x18, 0xFD));
    Assert.assertEquals("Halt", instructionAt(0x8007));
    Assert.assertNotEquals("Halt", instructionAt(0x8005));
  }

  @Test
  public void theContinuationAfterTheDataOfACallRunsEvenWhenItStartsAnotherRoutine() {
    // Emlyn 648E: CALL 721D with text after it; the continuation 64A2 ended in another routine once the fence was gone
    String java = translate(
        at(0x8000, 0xCD, 0x08, 0x80, 0xCD, 0x0C, 0x80, 0x76),
        at(0x8008, 0xCD, 0x20, 0x80, 0x2A, 0x06, 0x01, 0xC9),
        at(0x8020, 0xE1, 0x23, 0xE9));
    Assert.assertEquals(Integer.valueOf(0x800C), stackAnalyzer.callContinuations.get(0x8008));
  }

  @Test
  public void aCallerThatTheRecordingNeverRanContinuesPastItsOwnData() {
    // Emlyn 7218/721D/5D78 print the text after each CALL: an unrecorded caller learns its continuation by forking, not from the recorded caller
    String java = translate(
        at(0x8000, 0xCD, 0x30, 0x80, 0x41, 0xFF, 0xAF, 0x20, 0x08, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xCD, 0x30, 0x80, 0x42, 0x43, 0xFF, 0x0E, 0x02, 0x76, 0x18, 0xFD),
        at(0x8030, 0xE1, 0x7E, 0x23, 0xFE, 0xFF, 0x20, 0xFA, 0xE9));
    Assert.assertEquals(Integer.valueOf(0x8005), stackAnalyzer.callContinuations.get(0x8000));
    Assert.assertEquals(Integer.valueOf(0x8016), stackAnalyzer.callContinuations.get(0x8010));
  }

  @Test
  public void withoutTheFenceARecordedEntryInsideAnotherRecordedInstructionIsDecoded() {
    // the operand of LD A,0AFh is also XOR A, entered by the DJNZ
    String java = translate(at(0x8000, 0x06, 0x02, 0x3E, 0xAF, 0x10, 0xFD, 0x76, 0x18, 0xFD));
    Assert.assertEquals("Ld", instructionAt(0x8002));
    Assert.assertEquals("Xor", instructionAt(0x8003));
  }

  @Test
  public void aCallPatchedAtAFixedAddressSwitchesWithAFallbackEvenIfTheRecordingSawOneTarget() {
    // Equinox 7879: LD (787A),HL at 7CD5 always wrote 7D10 in the recording, with other keys it writes 7C00
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x21, 0x20, 0x80, 0x22, 0x19, 0x80, 0xCD, 0x18, 0x80, 0x76, 0x18, 0xFD),
        at(0x8018, 0xCD, 0x00, 0x00, 0xC9),
        at(0x8020, 0x06, 0x01, 0xC9));
    Assert.assertEquals(CodeVersions.Kind.INSTRUCTION, versions.kinds().get(0x8018));
    Assert.assertEquals(1, versions.instructionVersions(0x8018).size());
  }

  @Test
  public void copyingCodeOntoItselfDoesNotMakeItMutant() {
    // Equinox 9E80 waits with LD HL,0 / LD DE,0 / LDIR, rewriting every byte with its own value
    String java = translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x21, 0x00, 0x80, 0x54, 0x5D, 0x01, 0x30, 0x00, 0xED, 0xB0, 0xCD, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0xCD, 0x28, 0x80, 0xC9),
        at(0x8028, 0x06, 0x01, 0xC9));
    Assert.assertEquals(Map.of(), versions.kinds());
  }

  @Test
  public void aSharedCallWithDataAfterItContinuesPastTheData() {
    // Emlyn 648E without the fence: reached by jumps from several routines, its CALL 721D lost the continuation 64A2
    String java = translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0xCD, 0x28, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0x06, 0x01, 0xC3, 0x30, 0x80),
        at(0x8028, 0x0E, 0x02, 0xC3, 0x30, 0x80),
        at(0x8030, 0xCD, 0x40, 0x80, 0x41, 0xFF, 0x16, 0x03, 0xC9),
        at(0x8040, 0xE1, 0x7E, 0x23, 0xFE, 0xFF, 0x20, 0xFA, 0xE9));
    Assert.assertTrue(routines(), routines().contains("8030"));
    Assert.assertEquals(Integer.valueOf(0x8035), stackAnalyzer.callContinuations.get(0x8030));
  }

  @Test
  public void anOperandPatchedAtAFixedAddressIsReadFromMemoryWhereTheRecordingNeverRanIt() {
    // Equinox 7C08: LD (7C09),A at 7D06 patches LD C,n, which only runs once fuzzing reaches 7C00
    String java = translate(
        at(0x8000, 0x3E, 0x04, 0x32, 0x21, 0x80, 0xAF, 0x20, 0x18, 0x76, 0x18, 0xFD),
        at(0x8020, 0x0E, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertTrue(stackAnalyzer.codeVersions.mutant().contains(0x8021));
  }

  @Test
  public void anOperandPatchedByAStoreThatOnlyAForkRanIsReadFromMemory() {
    // Equinox 7C08: its writers LD (7C09),A at 7D06 and 7E26 never ran in the recording
    String java = translate(
        at(0x8000, 0xAF, 0x20, 0x0D, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3E, 0x04, 0x32, 0x21, 0x80, 0xC3, 0x20, 0x80),
        at(0x8020, 0x0E, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertTrue(stackAnalyzer.codeVersions.mutant().contains(0x8021));
  }

  @Test
  public void aForkThatJumpsThroughARegisterIntoScreenMemoryTeachesNoTarget() {
    // Emlyn 6075: a fork reached CALL 162C with HL=57CB, and without the fence the zeros of the screen became $57CB
    String java = translate(
        at(0x8000, 0x21, 0x20, 0x80, 0xAF, 0x28, 0x02, 0x26, 0x50, 0xE9),
        at(0x8020, 0x06, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertEquals(Set.of(0x8020), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8008)));
  }

  @Test
  public void aRestartIsACallToTheRomThatReturnsToTheNextInstruction() {
    // Monty on the Run E500: its IM 2 handler starts with RST 38 and goes on at E501
    String java = translate(
        at(0x0038, 0xC9),
        at(0x8000, 0xFF, 0x06, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertEquals("38 8000", routines());
  }

  @Test
  public void aRomRoutineIsTranslatedWithoutWanderingIntoRam() {
    // Dizzy jumps to 0000: from there the boot reached the BASIC interpreter, which decoded RAM as code
    String java = translate(
        at(0x0010, 0xAF, 0xC2, 0x00, 0x90, 0xC9),
        at(0x8000, 0xD7, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x9000, 0x06, 0x07, 0xC9));
    Assert.assertEquals("10 8000", routines());
  }

  @Test
  public void anInstructionAtTheLastAddressWrapsAroundTheMemory() {
    // Sir Fred: an instruction at FFFF whose operand is at 0000 broke the instruction cache
    String java = translate(
        at(0x0000, 0x34, 0x12, 0xC9),
        at(0x8000, 0xCD, 0xFF, 0xFF, 0x76, 0x18, 0xFD),
        at(0xFFFF, 0x01));
    Assert.assertTrue(routines(), routines().contains("FFFF"));
    Assert.assertEquals("Ret", instructionAt(0x0002));
  }

  @Test
  public void aRestartGoesOnOnlyWhereTheRecordingSawItReturn() {
    // Dizzy: RST 0 resets and RST 8 or RST 28 carry inline data; the bytes after a restart nobody returned from are not code
    String java = translate(
        at(0x8000, 0xAF, 0x20, 0x0D, 0x76, 0x18, 0xFD),
        at(0x8010, 0xC7, 0x06, 0x07, 0xC9));
    Assert.assertEquals("8000", routines());
    Assert.assertNull(instructionAt(0x8011));
  }

  @Test
  public void aRomRoutineReachedThroughARegisterJumpIsTranslated() {
    // Sir Fred prints with RST 10, and the ROM reaches PRINT-OUT at 09F4 through JP (HL) from the channel table: the target is translated as part of the jumping routine
    String java = translate(
        at(0x0010, 0x21, 0x20, 0x00, 0xE9),
        at(0x0020, 0x06, 0x01, 0xC9),
        at(0x8000, 0xD7, 0x76, 0x18, 0xFD));
    Assert.assertEquals("10 8000", routines());
    Assert.assertNotNull(routineManager.getInstructionAt(0x0020));
  }

  @Test
  public void printingThroughTheRomChannelEntersRst10AndReachesTheChannelsOutputRoutine() {
    // Abu Simbel C431: RST 10 -> 0010 JP 15F2 -> PRINT-A-2: the channel's output routine comes from (CURCHL) through CALL-JUMP at 162C
    translate(
        at(0x0010, 0xC3, 0xF2, 0x15),
        at(0x15F2, 0xD9, 0xE5, 0x2A, 0x51, 0x5C, 0x5E, 0x23, 0x56, 0xEB, 0xCD, 0x2C, 0x16, 0xE1, 0xD9, 0xC9),
        at(0x162C, 0xE9),
        at(0x09F4, 0x0C, 0xC9),
        at(0x5C51, 0xB6, 0x5C),
        at(0x5CB6, 0xF4, 0x09),
        at(0x8000, 0x3E, 0x41, 0xD7, 0x3E, 0x42, 0xD7, 0x06, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertTrue(routines(), routines().startsWith("10 "));
    Assert.assertNotNull(routineManager.findRoutineAt(0x09F4));
  }

  @Test
  public void printingThroughTheRealRomOpensTheChannelAndPrintsThroughRst10() {
    // Abu Simbel C42C: CALL 1601 (CHAN-OPEN) then RST 10 for INK 6 (a control code whose parameter the ROM prints with its own RST 10), the whole 48K ROM in place
    byte[] rom = com.fpetrola.z80.minizx.emulation.MiniZXWithEmulationBase.createROM();
    int[] romChunk = new int[1 + rom.length];
    for (int i = 0; i < rom.length; i++)
      romChunk[i + 1] = rom[i] & 0xff;
    translate(
        romChunk,
        at(0x5C10, 0x01, 0x00, 0x06, 0x00, 0x0B, 0x00, 0x01, 0x00, 0x01, 0x00, 0x06, 0x00, 0x10, 0x00),
        at(0x5C4F, 0xB6, 0x5C),
        at(0x5C51, 0xB6, 0x5C),
        at(0x5CB6, 0xF4, 0x09, 0xA8, 0x10, 0x4B, 0xF4, 0x09, 0xC4, 0x15, 0x53, 0x7B, 0x0F, 0xC4, 0x15, 0x52, 0xF4, 0x09, 0xC4, 0x15, 0x50, 0x80),
        at(0x8000, 0x3E, 0x02, 0xCD, 0x01, 0x16, 0x21, 0x06, 0x00, 0x3E, 0x10, 0xD7, 0x7D, 0xD7, 0x3E, 0x16, 0xD7, 0x3E, 0x01, 0xD7, 0x3E, 0x02, 0xD7, 0x3E, 0x41, 0xD7, 0x3E, 0x17, 0xD7, 0x3E, 0x05, 0xD7, 0x3E, 0x00, 0xD7, 0x06, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertTrue(routines(), (" " + routines() + " ").contains(" 10 "));
    Assert.assertTrue(routines(), routines().contains(" C3B "));
    Assert.assertFalse(routines(), routines().contains(" C3E "));
    Assert.assertEquals(Set.of(0x0A75, 0x0A7A), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x0B23)));
  }

  @Test
  public void anUnrolledLoopReenteredThroughCopiesOfItsEntryPushedByItsOwnRoutineStaysInThatRoutine() {
    // R-Type 88E6: PUSH 8974 as continuation, eight PUSH HL of an entry into the unrolled RL chain, RET; every RET of the chain takes the next copy
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x01, 0x30, 0x80, 0xC5, 0x21, 0x22, 0x80, 0xE5, 0xE5, 0xC9),
        at(0x8020, 0x14, 0x14, 0x1C, 0xC9),
        at(0x8030, 0x0E, 0x09, 0xC9));
    Assert.assertEquals(routineManager.findRoutineAt(0x8013), routineManager.findRoutineAt(0x8023));
  }

  @Test
  public void codeThatRunsInAnotherBankIsTranslatedForThatBankFromACopyInScreenMemory() {
    // Batman The Movie: the music player runs at C000-C8F7 in bank 3, where bank 0 holds the game's own code; Renegade 128's bank 1 (4192 bytes) found no gap below C000 next to the main code
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x06, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x57,
            0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0xCD, 0x00, 0xC0, 0x5F, 0x76, 0x18, 0xFD),
        at(0x8100, 0x3E, 0x05, 0xC3, 0x05, 0xC0, 0xC9),
        at(0xC000, 0x00, 0x3E, 0x07, 0xC9));
    Assert.assertEquals(List.of(3), routineManager.codeVariants.stream().map(RoutineManager.CodeVariant::bank).toList());
    Assert.assertEquals(0x4000, routineManager.codeVariants.get(0).relocatedAt());
    Assert.assertEquals(0x0507, endValue("DE"));
  }

  @Test
  public void codeOfAnotherBankIsCopiedAwayFromCodeOnlyAForkRan() {
    // Head over Heels B889: a fork jumped through a recorded JP into data the recording never ran, and bank 1's code was copied over it
    banked = true;
    translate(
        at(0x8000, 0x21, 0x30, 0x80, 0xAF, 0x28, 0x03, 0x21, 0x00, 0x5B, 0xE9),
        at(0x8030, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x06, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x57,
            0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0xCD, 0x00, 0xC0, 0x5F, 0x76, 0x18, 0xFD),
        at(0x8100, 0x3E, 0x05, 0xC3, 0x05, 0xC0, 0xC9),
        at(0xC000, 0x00, 0x3E, 0x07, 0xC9),
        at(0x5B00, 0x3E, 0x09, 0xC3, 0x30, 0x80));
    RoutineManager.CodeVariant copy = routineManager.codeVariants.get(0);
    Assert.assertTrue(copy.relocated(copy.end()) <= 0x5B00 || copy.relocatedAt() >= 0x5B05);
    Assert.assertEquals(0x0507, endValue("DE"));
  }

  @Test
  public void anAddressTheRunLoopTakesInAnotherBanksCodeRunsThatBanksCopy() {
    // Target: Renegade E3C7: the RET that ends the entry method goes to C300, code of the paged bank, and MiniZX.run invoked $C300 by name
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x06, 0x00, 0xED, 0xB0, 0x21, 0x00, 0xC0, 0x22, 0x00, 0xFF, 0xC9),
        at(0x8100, 0x3E, 0x07, 0x57, 0x76, 0x18, 0xFD),
        at(0xC000, 0x3E, 0x05, 0x57, 0x76, 0x18, 0xFD));
    Assert.assertEquals(List.of(3), routineManager.codeVariants.stream().map(RoutineManager.CodeVariant::bank).toList());
    Assert.assertEquals(0x07, endValue("D"));
  }

  @Test
  public void aReturnAddressPoppedInAnotherBanksCopyIsTheOriginalAddress() {
    // Target: Renegade C132: POP IX takes the return address of the CALL at C465 to read the bytes that follow it
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x0A, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x57,
            0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0xCD, 0x00, 0xC0, 0x5F, 0x76, 0x18, 0xFD),
        at(0x8100, 0xCD, 0x04, 0xC0, 0x09, 0xDD, 0xE1, 0xDD, 0x7E, 0x00, 0xC9),
        at(0xC000, 0x00, 0x3E, 0x07, 0xC9));
    Assert.assertEquals(List.of(3), routineManager.codeVariants.stream().map(RoutineManager.CodeVariant::bank).toList());
    Assert.assertEquals(0x0907, endValue("DE"));
    Assert.assertEquals(0xC003, endValue("IX"));
  }

  @Test
  public void codeAnotherBankRunsWithTheStartingBanksBytesIsTheStartingBanksCode() {
    // Head over Heels FFFF: every bank holds the same interrupt entry, a JR FFF4 whose offset is the ROM's byte at 0000, and bank 1 ran it too, which made bank 1's copy span C000-FFFF
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x06, 0x00, 0xED, 0xB0, 0x21, 0x10, 0x81, 0x11, 0xF4, 0xFF,
            0x01, 0x0C, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0xCD, 0xFF, 0xFF, 0x57, 0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0xCD, 0x00, 0xC0, 0x5F, 0xCD, 0xFF, 0xFF, 0x76, 0x18, 0xFD),
        at(0x8100, 0x3E, 0x05, 0xC3, 0x05, 0xC0, 0xC9),
        at(0x8110, 0x0E, 0x09, 0xC9, 0, 0, 0, 0, 0, 0, 0, 0, 0x18),
        at(0xC000, 0x00, 0x3E, 0x07, 0xC9),
        at(0xFFF4, 0x0E, 0x09, 0xC9, 0, 0, 0, 0, 0, 0, 0, 0, 0x18),
        at(0x0000, 0xF3));
    Assert.assertEquals(0xC006, routineManager.codeVariants.get(0).end());
    Assert.assertEquals(0x0507, endValue("DE"));
    Assert.assertEquals(0x09, endValue("C"));
  }

  @Test
  public void releasingTheCallersStackKeepsTheRecordedCodeInsideItProtected() {
    // Head over Heels FFF4: the stack sits right below the interrupt entry; releasing the caller's stack after SP pointed elsewhere unprotected the recorded code at FFF4-FFFF, and the SE's later writes there made it mutant
    ignoresMemory(0xFFE0, 0xFFF0);
    translate(
        at(0x8000, 0x31, 0xF0, 0xFF, 0xCD, 0xFF, 0xFF, 0xED, 0x73, 0x00, 0x90, 0x31, 0x00, 0xA0, 0xC5, 0xED, 0x7B, 0x00, 0x90, 0xCD, 0xFF, 0xFF, 0x76, 0x18, 0xFD),
        at(0xFFF4, 0x3E, 0x01, 0xC9, 0, 0, 0, 0, 0, 0, 0, 0, 0x18),
        at(0x0000, 0xF3));
    Assert.assertTrue(base.getState().getMemory().isProtected(0xFFF4) && base.getState().getMemory().isProtected(0xFFFF));
    Assert.assertEquals(0x01, endValue("A"));
  }

  @Test
  public void aMutantJumpRunByTheEmulatorContinuesAtItsTargetInsideTheRoutine() {
    // Head over Heels FFFF: the game can store its interrupt entry JR FFF4, so the JR runs in the emulator and lands at FFF4, a label of the same routine with no method of its own
    fallsBackToTheEmulator = true;
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0xAF, 0x28, 0x03, 0x32, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0x18, 0x01, 0x00, 0x06, 0x01, 0xC9));
    Assert.assertTrue(stackAnalyzer.codeVersions.mutant().contains(0x8020));
    Assert.assertEquals(0x01, endValue("B"));
  }

  @Test
  public void comingBackFromTheStackUsedAsStorageAboveAReturnDropsIt() {
    // Rainbow Islands E6CC: after POPping graphics with SP at 5B02, LD SP,FDDB lands on the return of CALL E5E3 at FEE4, above the return of the inner call, and the RET goes back to FEE7
    ignoresMemory(0xFE00, 0xFF00);
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x3E, 0x07, 0x76, 0x18, 0xFD),
        at(0x8010, 0xCD, 0x20, 0x80, 0x3E, 0x09, 0xC9),
        at(0x8020, 0x31, 0x00, 0xA0, 0xE1, 0x31, 0xFE, 0xFE, 0xC9));
    Assert.assertEquals(Set.of(StackAnalyzer.Abandon.INNER_CALL_PENDING), stackAnalyzer.abandoningRets.get(0x8027).stream().map(StackAnalyzer.AbandoningRet::how).collect(Collectors.toSet()));
    Assert.assertEquals(0x07, endValue("A"));
  }

  @Test
  public void aReturnToTheCallersCallerFromAnotherStackAbandonsTheInnerCall() {
    // Rainbow Islands E62D: with SP in a jump table, CALL Z,E74A pushes its return into the table; E6CC reloads SP onto the return of CALL E5E3 and its RET goes back to FEE7
    ignoresMemory(0xFE00, 0xFF00);
    ignoresMemory(0x9000, 0x9040);
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x3E, 0x07, 0x76, 0x18, 0xFD),
        at(0x8010, 0x31, 0x30, 0x90, 0xCD, 0x40, 0x80, 0x06, 0x09, 0xC9),
        at(0x8040, 0x31, 0xFE, 0xFE, 0xC9));
    Assert.assertEquals(Set.of(StackAnalyzer.Abandon.INNER_CALL_PENDING), stackAnalyzer.abandoningRets.get(0x8043).stream().map(StackAnalyzer.AbandoningRet::how).collect(Collectors.toSet()));
    Assert.assertEquals(0x00, endValue("B"));
  }

  @Test
  public void aRepeatingInstructionInAnotherBanksCodeRepeatsAtItsOriginalAddress() {
    // Rainbow Islands D547: bank 1's LDIR runs from its relocated copy, and each repetition reported the copy's address
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x0C, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x76, 0x18, 0xFD),
        at(0x8100, 0x21, 0x00, 0x82, 0x11, 0x00, 0x90, 0x01, 0x03, 0x00, 0xED, 0xB0, 0xC9),
        at(0x8200, 0x01, 0x02, 0x03));
    Assert.assertEquals(List.of(3), routineManager.codeVariants.stream().map(RoutineManager.CodeVariant::bank).toList());
    Assert.assertEquals(0x9003, endValue("DE"));
  }

  @Test
  public void aPushedContinuationInsideAnotherRoutineIsEnteredThere() {
    // Head over Heels 72A8: PUSH 7305 before the shared code whose RET at 72F2 consumes it, 7305 lying inside routine 72BC; entered at 72A0 the same RET returns to the caller
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0xCD, 0x30, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x21, 0x43, 0x80, 0xE5, 0x18, 0x1A),
        at(0x8030, 0x3E, 0x01, 0xC3, 0x40, 0x80),
        at(0x8040, 0x06, 0x02, 0xC9, 0x0C, 0x18, 0xFC));
    Assert.assertTrue(routineManager.externalEntries.contains(0x8043));
    Assert.assertEquals(0x01, endValue("C"));
  }

  @Test
  public void aCalleeJumpingBackIntoItsCallersCodeReturnsThroughTheCallersRet() {
    // Rainbow Islands D44C: CALL CAFB at D470 ends up jumping back to D448, and the RET Z at D44C in the caller's own code returns from that call to D473
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3E, 0x01, 0xA7, 0xC8, 0x3D, 0xCD, 0x30, 0x80, 0x06, 0x05, 0xC9),
        at(0x8030, 0xCD, 0x40, 0x80, 0x0E, 0x07, 0xC9),
        at(0x8040, 0xE1, 0xC3, 0x12, 0x80));
    Assert.assertEquals(0x8012, routineManager.findRoutineAt(0x8012).getEntryPoint());
    Assert.assertEquals(0x0500, endValue("BC"));
  }

  @Test
  public void aReturnPoppedOnlyInAForkedBranchIsNoAbandonmentOfTheRecordedRun() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0xCD, 0x20, 0x80, 0xC9),
        at(0x8020, 0xAF, 0x20, 0x01, 0xC9, 0xE1, 0x18, 0xFE));
    Assert.assertTrue(stackAnalyzer.abandoningRets.get(0x8013).isEmpty());
  }

  @Test
  public void aReturnPoppedDeepInsideACallIsAnAbandonmentOfThatCallsFrameWhateverTheStackDidSince() {
    // Rainbow Islands D44C: the return of CALL CB2B was popped at CF09, with more pushes and pops before the RET Z that closes CALL D470's frame
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3E, 0x01, 0xA7, 0xC8, 0x3D, 0xCD, 0x30, 0x80, 0x06, 0x05, 0xC9),
        at(0x8030, 0xCD, 0x40, 0x80, 0x0E, 0x07, 0xC9),
        at(0x8040, 0xE1, 0xD5, 0xD1, 0xC3, 0x12, 0x80));
    Assert.assertEquals(Set.of(new StackAnalyzer.AbandoningRet(STACK - 4, StackAnalyzer.Abandon.POPPED_RETURN, 0x8015, 0x8030)), Set.copyOf(stackAnalyzer.abandoningRets.get(0x8013)));
    Assert.assertEquals(0x0500, endValue("BC"));
  }

  @Test
  public void aCallBetweenTheStackReloadAndTheReturnToTheCallersCallerKeepsItNonLocal() {
    // Rainbow Islands E729: after LD SP,FDDB at E6CC, calls return before the RET at E729 goes back to FEE7
    ignoresMemory(0xFE00, 0xFF00);
    ignoresMemory(0x9000, 0x9040);
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x3E, 0x07, 0x76, 0x18, 0xFD),
        at(0x8010, 0x31, 0x30, 0x90, 0xCD, 0x40, 0x80, 0x06, 0x09, 0xC9),
        at(0x8040, 0x31, 0xFE, 0xFE, 0xCD, 0x50, 0x80, 0xC9),
        at(0x8050, 0x0E, 0x03, 0xC9));
    Assert.assertEquals(Set.of(StackAnalyzer.Abandon.INNER_CALL_PENDING), stackAnalyzer.abandoningRets.get(0x8046).stream().map(StackAnalyzer.AbandoningRet::how).collect(Collectors.toSet()));
    Assert.assertEquals(0x0003, endValue("BC"));
  }

  @Test
  public void anOperandThatCodeInAnotherBankRewritesIsReadFromThatBank() {
    // Batman The Movie C431: the music player in bank 3 rewrites the operand of its own LD A,n
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x07, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x57,
            0xCD, 0x00, 0xC0, 0x5F, 0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0xCD, 0x00, 0xC0, 0x76, 0x18, 0xFD),
        at(0x8100, 0x3E, 0x05, 0x3C, 0x32, 0x01, 0xC0, 0xC9),
        at(0xC000, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xC9));
    Assert.assertFalse(stackAnalyzer.codeVersions.mutant().contains(0xC001));
    Assert.assertEquals(0x0607, endValue("DE"));
  }

  @Test
  public void aJumpThroughATableInsideCodeOfAnotherBankLandsInThatBanksCode() {
    // Batman The Movie C1E4: the music player in bank 3 jumps through its own table to a handler in bank 3
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x12, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x76, 0x18, 0xFD),
        at(0x8100, 0x2A, 0x10, 0xC0, 0xE9, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x07, 0xC9, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0xC0));
    Assert.assertEquals(0x07, endValue("A"));
  }

  @Test
  public void aJumpOfCodeInAnotherBankIntoWhatTheStartingBankRunsAsOperandsIsKept() {
    // Batman The Movie C282: the music player's jump target lies inside an instruction bank 0 runs at the same address
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x12, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x57,
            0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0xCD, 0x07, 0xC0, 0x5F, 0x76, 0x18, 0xFD),
        at(0x8100, 0x2A, 0x10, 0xC0, 0xE9, 0x00, 0x00, 0x00, 0x00, 0x3E, 0x07, 0xC9, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0xC0),
        at(0xC007, 0x3E, 0x09, 0xC9));
    Assert.assertEquals(0x0709, endValue("DE"));
  }

  @Test
  public void aJumpFromTheStartingBanksCodeIntoAnotherBanksCodeReachesThatBanksCopy() {
    // Rainbow Islands 5C8E: JP (HL) with bank 1 paged lands at CF87, inside the LD HL,nn that bank 0 runs at CF85
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x04, 0x00, 0xED, 0xB0, 0x21, 0x01, 0xC0, 0xCD, 0x30, 0x80, 0x57,
            0x01, 0xFD, 0x7F, 0x3E, 0x10, 0xED, 0x79, 0xCD, 0x00, 0xC0, 0x5F, 0x76, 0x18, 0xFD),
        at(0x8030, 0xE9),
        at(0x8100, 0x00, 0x3E, 0x05, 0xC9),
        at(0xC000, 0x21, 0x3E, 0x07, 0x3E, 0x07, 0xC9));
    Assert.assertTrue(stackAnalyzer.dynamicInvocation.get(0x8030).contains(0xC001));
    Assert.assertEquals(0x0507, endValue("DE"));
  }

  @Test
  public void aJumpFromTheStartingBanksCodeIntoTheMiddleOfAnotherBanksRoutineEntersItThere() {
    // Rainbow Islands 5C8E: JP (HL) with bank 1 paged lands at D6E5, in the middle of bank 1's routine at D6D1
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x13, 0xED, 0x79, 0x21, 0x00, 0x81, 0x11, 0x00, 0xC0, 0x01, 0x04, 0x00, 0xED, 0xB0, 0xCD, 0x00, 0xC0, 0x57, 0x21, 0x02, 0xC0,
            0xCD, 0x30, 0x80, 0x5F, 0x76, 0x18, 0xFD),
        at(0x8030, 0xE9),
        at(0x8100, 0x3E, 0x05, 0x3C, 0xC9));
    Assert.assertEquals(0x0607, endValue("DE"));
    Assert.assertTrue(routineManager.externalEntries.contains(routineManager.codeVariants.get(0).relocated(0xC002)));
  }

  @Test
  public void codeRewrittenSoThatAnOperandBecomesAnOpcodeIsModifiedCode() {
    // Batman The Movie EB27: LD A,(611A) ran there, and later EB28 ran as LD C,A after the region was rewritten
    fallsBackToTheEmulator = true;
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x21, 0x00, 0x81, 0x11, 0x21, 0x80, 0x01, 0x02, 0x00, 0xED, 0xB0, 0x3E, 0x05, 0xCD, 0x21, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0x3A, 0x00, 0x90, 0xC9),
        at(0x8100, 0x4F, 0xC9),
        at(0x9000, 0x42),
        at(0x904F, 0x99));
    Assert.assertTrue(stackAnalyzer.codeVersions.mutant().contains(0x8021));
    Assert.assertEquals(0x05, endValue("C"));
  }

  @Test
  public void port7FFDPagesTheBankAtC000ForTheTranslatedProgramToo() {
    // Renegade 128K pages bank 1 in for its music and bank 0 back: what is read and written at C000 depends on the bank mapped
    banked = true;
    translate(
        at(0x8000, 0x3A, 0x00, 0xC0, 0x01, 0xFD, 0x7F, 0x3E, 0x11, 0xED, 0x79, 0x3A, 0x00, 0xC0, 0x3E, 0x55, 0x32, 0x00, 0xC0, 0x3E, 0x10, 0xED, 0x79,
            0x3A, 0x00, 0xC0, 0x47, 0x3E, 0x11, 0xED, 0x79, 0x3A, 0x00, 0xC0, 0x4F, 0x3E, 0x10, 0xED, 0x79, 0x76, 0x18, 0xFD),
        at(0xC000, 0x42));
    Assert.assertEquals(0x4255, endValue("BC"));
  }

  @Test
  public void aStackResetDroppingTheReturnOfARoutineNoLongerRunningIsNotAReturnToIt() {
    // Renegade 128K 7C46: LD SP,BDFF dropped the return of the patched CALL at 8482, a site that earlier explorations had run too
    ignoresMemory(0xFE00, 0xFF00);
    translate(
        at(0x8000, 0x11, 0x50, 0x80, 0xED, 0x53, 0x0C, 0x80, 0x21, 0x0B, 0x80, 0x00, 0xCD, 0x00, 0x00, 0x11, 0x60, 0x80, 0xED, 0x53, 0x0C, 0x80, 0xE9),
        at(0x8030, 0x76, 0x18, 0xFD),
        at(0x8050, 0xC9),
        at(0x8060, 0x31, 0x00, 0xFF, 0xC3, 0x30, 0x80));
    Assert.assertEquals("8000 8050 8060", routines());
  }

  @Test
  public void anOutToPort7FFDRunByTheEmulatorFallbackPagesTheTranslatedProgramsMemory() {
    // Renegade 128K: code patched by its own stores runs through the emulator fallback, and an OUT there must page the Java side too
    banked = true;
    fallsBackToTheEmulator = true;
    ignoresMemory(0x8F00, 0x9000);
    translate(
        at(0x8000, 0x31, 0x00, 0x90, 0xCD, 0x20, 0x80, 0xCD, 0x40, 0x80, 0x3E, 0x11, 0xCD, 0x20, 0x80, 0x3A, 0x00, 0xC0, 0x76, 0x18, 0xFD),
        at(0x8020, 0x3A, 0x00, 0x00, 0xC9),
        at(0x8040, 0x21, 0x20, 0x80, 0x36, 0xD3, 0x23, 0x36, 0xFD, 0xC9),
        at(0xC000, 0x42));
    Assert.assertEquals(0x00, endValue("A"));
  }

  @Test
  public void aReturnTheRecordingTookAsAReturnIsNoDispatchForWhatAForkMadeItPop() {
    // Target: Renegade D9A9: on a branch the recording never took, the sprite blitter's RET pops the screen addresses it pushed
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3A, 0x02, 0x81, 0xB7, 0x28, 0x04, 0x01, 0x00, 0x70, 0xC5, 0xC9));
    Assert.assertNull(routineManager.findRoutineAt(0x7000));
    Assert.assertEquals(RoutineManager.Exit.RETURN, routineManager.exitOf(0x801A, stackAnalyzer).kind());
  }

  @Test
  public void aDispatchingReturnKeepsTheTargetsTheRecordingSawWhateverAForkMadeItPop() {
    // Target: Renegade D9A9: the recording's callers plant the blitter's return point, a fork pops the screen addresses it pushed
    translate(
        at(0x8000, 0x21, 0x07, 0x80, 0xE5, 0xC3, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0x3A, 0x02, 0x81, 0xB7, 0x28, 0x04, 0x01, 0x00, 0x70, 0xC5, 0xC9));
    Assert.assertEquals(Set.of(0x8007), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x802A)));
    Assert.assertEquals(RoutineManager.Exit.DISPATCH, routineManager.exitOf(0x802A, stackAnalyzer).kind());
  }

  @Test
  public void aPatchedJumpToTheNextAddressGoesOnInTheRoutineThere() {
    // Target: Renegade BFBF: JP BFC2 is a store target, so it runs through the emulator fallback; BFC2 starts another routine
    fallsBackToTheEmulator = true;
    translate(
        at(0x8000, 0xC3, 0x03, 0x80, 0x04, 0xC9),
        at(0x8020, 0x3A, 0x00, 0x90, 0xB7, 0x28, 0x03, 0x32, 0x00, 0x80, 0xCD, 0x03, 0x80, 0x76, 0x18, 0xFD),
        at(STACK, 0x20, 0x80));
    Assert.assertEquals("8000 8003 8020", routines());
    Assert.assertTrue(stackAnalyzer.codeVersions.mutant().contains(0x8000));
  }

  @Test
  public void aJumpThroughARegisterAfterPlantingItsContinuationGoesOnThereWhenTheTargetReturns() {
    // Target: Renegade F428: PUSH HL; LD HL,F42F; PUSH HL; EX DE,HL; JP (HL), and F42F pops the HL pushed first
    translate(
        at(0x8000, 0x21, 0x34, 0x12, 0x11, 0x30, 0x80, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0xE5, 0x21, 0x20, 0x80, 0xE5, 0xEB, 0xE9),
        at(0x8020, 0xE1, 0x7D, 0xC9),
        at(0x8030, 0x04, 0xC9));
    Assert.assertEquals(Set.of(0x8030), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8016)));
    Assert.assertEquals(Set.of(0x8020), Set.copyOf(stackAnalyzer.pushedValues.get(0x8014)));
    Assert.assertFalse(stackAnalyzer.pushesTakenByPops.contains(0x8014));
    Assert.assertEquals(RoutineManager.Exit.SIMULATED_CALL, routineManager.exitOfJump(0x8016, routineManager.findRoutineAt(0x8016), stackAnalyzer));
  }

  @Test
  public void aPlantedContinuationThatATargetPopsAndJumpsToIsPushedForReal() {
    // Target: Renegade EF3D: one of the targets of F42E takes the planted F42F with POP HL and goes there with JP (HL)
    translate(
        at(0x8000, 0x21, 0x34, 0x12, 0x11, 0x30, 0x80, 0xCD, 0x10, 0x80, 0x11, 0x40, 0x80, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0xE5, 0x21, 0x20, 0x80, 0xE5, 0xEB, 0xE9),
        at(0x8020, 0xE1, 0x7D, 0xC9),
        at(0x8030, 0x04, 0xC9),
        at(0x8040, 0xE1, 0x0C, 0xE9));
    Assert.assertTrue(stackAnalyzer.pushesTakenByPops.contains(0x8014));
  }

  @Test
  public void outdAndOutiSendTheByteAtHlAndStepBAndHl() {
    // Batman The Movie C177: LD B,BF; OUTD writes the AY registers from a table
    translate(
        at(0x8000, 0x21, 0x01, 0x90, 0x01, 0xFE, 0x11, 0xED, 0xAB, 0xED, 0xAB, 0xED, 0xA3, 0x76, 0x18, 0xFD),
        at(0x9000, 0x80, 0xFF));
    Assert.assertEquals(0x0EFE, endValue("BC"));
    Assert.assertEquals(0x9000, endValue("HL"));
  }

  @Test
  public void anInterruptArrivingRightAfterEiIsTakenLikeTheEmulatorTakesIt() {
    // Monty on the Run frame 41511: the frame ends at the RET after the ROM ISR's EI; the emulator takes the interrupt there, the Java dropped it
    interruptEvery = 6;
    int[] table = new int[1 + 257];
    table[0] = 0x9000;
    Arrays.fill(table, 1, table.length, 0x80);
    translate(
        at(0x8000, 0x3E, 0x90, 0xED, 0x47, 0xED, 0x5E, 0xFB, 0x3C, 0x76),
        at(0x8080, 0xC3, 0x90, 0x80),
        at(0x8090, 0x0C, 0xFB, 0xED, 0x4D),
        table);
    Assert.assertEquals(0x90, endValue("A"));
  }

  @Test
  public void anUndocumentedIndexedRotationOrBitOpThatAlsoLoadsARegisterIsTranslated() {
    // Batman The Movie: an untaken path decodes DD CB d op with a register copy (RES 0,(IX+0),B); the instruction cache could not clone it
    translate(
        at(0x8000, 0xDD, 0x21, 0x00, 0x90, 0x06, 0x00, 0xDD, 0xCB, 0x00, 0x80, 0x78, 0xDD, 0xCB, 0x01, 0x10, 0x48, 0x76, 0x18, 0xFD),
        at(0x9000, 0xFF, 0x81));
    Assert.assertEquals(0xFE, endValue("A"));
    Assert.assertEquals(0x0202, endValue("BC"));
  }

  @Test
  public void aPrefixedInstructionRunByTheEmulatorFallbackAdvancesRByItsTwoFetches() {
    // Batman The Movie EFEF: the ISR starts with PUSH IX, a mutant site because a store the recording never ran targets it; R and the fetch count come from the emulator's fetch
    fallsBackToTheEmulator = true;
    translate(
        at(0x8000, 0x3E, 0x00, 0xB7, 0x28, 0x03, 0x32, 0x09, 0x80, 0xDD, 0x21, 0x00, 0x90, 0x76, 0x18, 0xFD));
    Assert.assertTrue(stackAnalyzer.codeVersions.mutant().contains(0x8009));
    Assert.assertEquals(5, endValue("R"));
  }

  @Test
  public void aConditionalReturnNotTakenOverPushedDataDispatchesNowhere() {
    // Fairlight EC13/EC14: PUSH HL; RET Z walks a byte stream and returns only at its end marker; every untaken RET Z was recorded as a dispatch to the data pointer
    translate(
        at(0x8000, 0x21, 0x00, 0x90, 0xE5, 0xAF, 0x3C, 0xC8, 0xE1, 0x76, 0x18, 0xFD));
    Assert.assertTrue(stackAnalyzer.dynamicInvocation.get(0x8006).toString(), stackAnalyzer.dynamicInvocation.get(0x8006).isEmpty());
  }

  @Test
  public void aCallIntoTheMiddleOfALoopRoutineEntersItThere() {
    // Fairlight F6BF: CALL FC40 enters the loop routine FC2B-FC47 past its head, and the recording ran that call before any entry at the head
    translate(
        at(0x8000, 0xCD, 0x29, 0x80, 0xCD, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0x3A, 0x00, 0x90, 0xB7, 0xC8, 0x3D, 0x32, 0x00, 0x90, 0x0C, 0x18, 0xF4),
        at(0x9000, 0x02));
    Assert.assertEquals("8000 8020 8029", routines());
  }

  @Test
  public void anInterruptReturnReadByAddressIsPushedByTheTranslatedInterruptToo() {
    // Skool Daze 61AA: the random number walk also reads what the ROM ISR left below SP, laid out after the interrupted PC the Z80 pushed
    interruptEvery = 6;
    int[] table = new int[1 + 257];
    table[0] = 0x9000;
    Arrays.fill(table, 1, table.length, 0x80);
    translate(
        at(0x8000, 0x3E, 0x90, 0xED, 0x47, 0xED, 0x5E, 0xFB, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF3, 0x3A, 0xFF, 0xFE, 0x76, 0x18, 0xFD),
        at(0x8080, 0xF5, 0xF1, 0xED, 0x4D),
        table);
    Assert.assertTrue(stackAnalyzer.layoutCallSites.contains(StackAnalyzer.INTERRUPT));
    Assert.assertEquals(0x80, endValue("A"));
  }

  @Test
  public void aPlantedContinuationIsPushedForRealWhenTheStackIsReadByAddress() {
    // Skool Daze 6AE5: the planted 6AD7 sits between the main loop's 6779 and the handlers' frames that the random number walk reads
    translate(
        at(0x8000, 0xCD, 0x30, 0x80, 0x21, 0x30, 0x80, 0xCD, 0x10, 0x80, 0x3A, 0xFC, 0xFE, 0x76, 0x18, 0xFD),
        at(0x8010, 0x01, 0x17, 0x80, 0xC5, 0xE5, 0xC9, 0x00, 0x3E, 0x05, 0xC9),
        at(0x8030, 0xC9));
    Assert.assertTrue(stackAnalyzer.observesStackLayout());
    Assert.assertEquals(0x17, endValue("A"));
  }

  @Test
  public void aReturnAddressReadByAddressKeepsItsCallPushingIt() {
    // Skool Daze 61AA: the random number generator walks memory with XOR (HL) and reads 5CFD, the high byte of the main loop's return address 6779
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0x3A, 0xFF, 0xFE, 0xC9));
    Assert.assertEquals(Set.of(0x8000), stackAnalyzer.layoutCallSites);
  }

  @Test
  public void dataPushedInsideACallAndReadBackAtItsAddressKeepsTheCallersReturnOnTheStack() {
    // Skool Daze 61AA: the same walk reads data pushed under the main loop's frames, so its position depends on how many returns are stacked
    translate(
        at(0x8000, 0x01, 0x34, 0x12, 0xCD, 0x20, 0x80, 0xDD, 0x21, 0xFC, 0xFE, 0xDD, 0x7E, 0x00, 0x76, 0x18, 0xFD),
        at(0x8020, 0xC5, 0xC1, 0xC9));
    Assert.assertEquals(Set.of(0x8003), stackAnalyzer.layoutCallSites);
  }

  @Test
  public void theStackPointerSavedInsideACallAndReadBackAsDataKeepsTheCallersReturnOnTheStack() {
    // Fairlight F055: LD (FFF6),SP saves SP around a fill routine, and the sprite drawer at E468 later reads FFF6 as graphic data
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x3A, 0xF6, 0xFF, 0x76, 0x18, 0xFD),
        at(0x8020, 0xED, 0x73, 0xF6, 0xFF, 0xED, 0x7B, 0xF6, 0xFF, 0xC9));
    Assert.assertEquals(Set.of(0x8000), stackAnalyzer.layoutCallSites);
  }

  @Test
  public void aCalleeThatPopsItsReturnAndReturnsGoesBackToTheCallersCaller() {
    // Rainbow Islands 6017: CALL 605C reaches the handler through JP (HL); it does POP HL and its RET at 6024 returns to the caller's caller
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x3E, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x21, 0x30, 0x80, 0xCD, 0x20, 0x80, 0x3E, 0x02, 0xC9),
        at(0x8020, 0xE9),
        at(0x8030, 0xE1, 0xC9));
    Assert.assertEquals(Set.of(0x8030), Set.copyOf(stackAnalyzer.poppedCallSites.get(0x8013)));
    Assert.assertEquals(0x01, endValue("A"));
  }

  @Test
  public void aReturnAddressThatAPopInAnotherRoutineMightTakeIsPoppedAfterANormalReturn() {
    // Fairlight FA06: CALL FA9C always returned with RET at E4E0 in the recording, but a path into F7BA's routine pops FA09 there, so the Java pushes it
    translate(
        at(0x8000, 0xCD, 0x40, 0x80, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x21, 0x06, 0x80, 0xE5, 0xCD, 0x20, 0x80, 0xE1, 0xC9),
        at(0x8020, 0x3A, 0x00, 0x90, 0xB7, 0xC8, 0xC3, 0x48, 0x80),
        at(0x8040, 0xF5, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xF1, 0xC9));
    Assert.assertEquals(Set.of(0x8048), Set.copyOf(stackAnalyzer.poppedCallSites.get(0x8014)));
  }

  @Test
  public void aStackResetDropsTheReturnIntoTheExecutionThatMadeTheCall() {
    // Skool Daze 6776: CALL 6AD9 ends in LD SP,5CFE at 6921; the RET at FE51 also dispatches to 67BC, so the SE ran the main loop's 6776 inside 6AD9 too
    translate(
        at(0x8000, 0x31, 0x00, 0xFF, 0x21, 0x10, 0x80, 0xE5, 0xC3, 0x60, 0x80),
        at(0x8010, 0x3A, 0x00, 0x90, 0x3C, 0x32, 0x00, 0x90, 0xFE, 0x03, 0x30, 0x15, 0xCD, 0x40, 0x80, 0x18, 0xE3),
        at(0x8030, 0x76, 0x18, 0xFD),
        at(0x8040, 0x21, 0x50, 0x80, 0xE5, 0xC3, 0x60, 0x80),
        at(0x8050, 0xCD, 0x70, 0x80, 0xC9),
        at(0x8060, 0x00, 0xC9),
        at(0x8070, 0x3A, 0x01, 0x90, 0xB7, 0x20, 0x06, 0x31, 0x00, 0xFF, 0xC3, 0x03, 0x80, 0xC9));
    Assert.assertTrue(base.symbolicExecutionAdapter.routineExecutorHandler.findRoutineExecutionAt(0x8000).getAddressAction(0x801B) instanceof ChangeStackCallAddressAction);
  }

  @Test
  public void aPopOfAReturnAddressPushedBeforeTheRecordingStartedPopsItFromTheStack() {
    // Skool Daze FA69: POP HL takes EDFA, pushed by CALL FA49 at EDF7 before the snapshot (which starts in 673D, called from FA59 inside FA49's loop), so no Java frame owns that call
    translateFrom(0x8050,
        at(0x8010, 0xCD, 0x40, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0x3A, 0x00, 0x90, 0x3C, 0x32, 0x00, 0x90, 0xFE, 0x02, 0x30, 0x06, 0xCD, 0x10, 0x80, 0x18, 0xF0, 0x76, 0x18, 0xFD),
        at(0x8040, 0xCD, 0x50, 0x80, 0x28, 0xFB, 0xE1, 0xC9),
        at(0x8050, 0x3A, 0x01, 0x90, 0xB7, 0xC9),
        at(0x9001, 0x01),
        at(STACK, 0x43, 0x80, 0x13, 0x80, 0x20, 0x80));
    Assert.assertEquals(Set.of(0x8045), Set.copyOf(stackAnalyzer.poppedCallSites.get(0x8010)));
  }

  @Test
  public void aContinuationPlantedUnderARetDispatchTargetRunsWhenTheTargetReturns() {
    // Skool Daze 6251: LD BC,6254; PUSH BC; PUSH DE; RET dispatches to a character's handler, and the handler at 629F is a bare RET that takes 6254
    translate(
        at(0x8000, 0xCD, 0x30, 0x80, 0x21, 0x30, 0x80, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x01, 0x17, 0x80, 0xC5, 0xE5, 0xC9, 0x00, 0x3E, 0x05, 0xC9),
        at(0x8030, 0xC9));
    Assert.assertEquals(Set.of(0x8013), Set.copyOf(stackAnalyzer.dataConsumedBy.get(0x8030)));
    Assert.assertEquals(0x05, endValue("A"));
  }

  @Test
  public void aContinuationPushedForRealUnderARetDispatchTargetResumesWhenTheTargetReturns() {
    // Skool Daze 6251: some handlers POP the continuation 6254 instead of returning to it, so the push is real and the bare RET at 629F must still reach 6254
    translate(
        at(0x8000, 0xCD, 0x30, 0x80, 0x21, 0x30, 0x80, 0xCD, 0x10, 0x80, 0x21, 0x40, 0x80, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x01, 0x17, 0x80, 0xC5, 0xE5, 0xC9, 0x00, 0x3E, 0x05, 0xC9),
        at(0x8030, 0xC9),
        at(0x8040, 0xC1, 0x3C, 0xC9));
    Assert.assertTrue(stackAnalyzer.pushesTakenByPops.contains(0x8013));
    Assert.assertEquals(0x06, endValue("A"));
  }

  @Test
  public void aRoutineThatPushesAndFallsIntoABareRetLeavesThePushForIt() {
    // Skool Daze 629E: PUSH BC falls into 629F RET, which jumps to the handler in BC; 629F is a routine of its own
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x01, 0x30, 0x80, 0xCD, 0x1C, 0x80, 0x76, 0x18, 0xFD),
        at(0x801C, 0x3E, 0x01, 0x00, 0xC5, 0xC9),
        at(0x8030, 0x3E, 0x07, 0xC9));
    Assert.assertEquals(Set.of(0x801F), Set.copyOf(stackAnalyzer.dataConsumedBy.get(0x8020)));
    Assert.assertEquals(0x07, endValue("A"));
  }

  @Test
  public void aStackPointerSavedAndRestoredButNeverReadAsDataKeepsNoReturns() {
    // Fairlight F060: LD SP,(FFF6) restores the saved SP, it does not look at the layout
    ignoresMemory(0xFFF6, 0xFFF7);
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0xED, 0x73, 0xF6, 0xFF, 0xED, 0x7B, 0xF6, 0xFF, 0xC9));
    Assert.assertEquals(Set.of(), stackAnalyzer.layoutCallSites);
  }

  @Test
  public void aSavedStackPointerOverwrittenByAVariableIsNoLongerReadAsTheLayout() {
    // Fairlight E7ED/E840: the recursive fill uses FFF6 as a variable between the SP saves of F055
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x22, 0xF6, 0xFF, 0x3A, 0xF6, 0xFF, 0x76, 0x18, 0xFD),
        at(0x8020, 0xED, 0x73, 0xF6, 0xFF, 0xC9));
    Assert.assertEquals(Set.of(), stackAnalyzer.layoutCallSites);
  }

  @Test
  public void aStackPointerSavedWhileTheStackFillsMemoryDoesNotDependOnTheCalls() {
    // Fairlight E5DC: LD (FFFA),SP after a PUSH fill of the attributes is read back as where the fill ended
    ignoresMemory(0xFFF8, 0xFFF9);
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x2A, 0xFA, 0xFF, 0x76, 0x18, 0xFD),
        at(0x8020, 0xED, 0x73, 0xF8, 0xFF, 0x21, 0x00, 0x5B, 0xF9, 0xD5, 0xD5, 0xED, 0x73, 0xFA, 0xFF, 0xED, 0x7B, 0xF8, 0xFF, 0xC9));
    Assert.assertEquals(Set.of(), stackAnalyzer.layoutCallSites);
  }

  @Test
  public void anOutThroughCGoesToThePortInBCWhateverAHolds() {
    // Renegade 128K 9FF3: OUT (C),A with BC=7FFD; only OUT (n),A puts A on the high byte of the port
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x91, 0xED, 0x79, 0x3A, 0x00, 0xC0, 0x76, 0x18, 0xFD),
        at(0xC000, 0x42));
    Assert.assertEquals(0x00, endValue("A"));
  }

  @Test
  public void poppingAReturnAddressThatWasOnTheStackBeforeTheRecordingIsNotAReturn() {
    // Bruce Lee 9633: POP of a return address from the snapshot's stack, whose CALL is unknown
    String java = translate(
        at(0x8000, 0xE1, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0xFF00, 0x10, 0x80));
    Assert.assertEquals("Ld", instructionAt(0x8001));
    Assert.assertTrue(stackAnalyzer.poppedCallSites.isEmpty());
  }

  @Test
  public void aPopThatTheRecordingOnlyRanOnValuesFromBeforeItStillPopsAReturnOnAnUnrecordedPath() {
    // Manic Miner 8D05: POP HL at the start of the death path; the recording never died, it only ran it on the snapshot's stack
    translate(
        at(0x8000, 0xE1, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xCD, 0x18, 0x80, 0xC9),
        at(0x8018, 0x3A, 0x00, 0x90, 0xB7, 0xC2, 0x00, 0x80, 0xC9),
        at(0xFF00, 0x34, 0x12));
    Assert.assertEquals(Set.of(0x8001), Set.copyOf(routineManager.returnPoints.get(0x8018)));
  }

  @Test
  public void aForkWithTheStackNearAddressZeroStillExplores() {
    // Everyone's a Wally FF02: its snapshot has SP 0000, and the stack analyzer of a fork read the words below SP at negative addresses, which aborted the fork
    int[] memory = memoryOf(at(0x8000, 0x31, 0x04, 0x00, 0xAF, 0x20, 0x2A, 0x76, 0x18, 0xFD), at(0x8030, 0x3E, 0x05, 0x76));
    RemoteZ80Translator.Footprint footprint = RemoteZ80Translator.footprint(stackAnalyzer -> EmulatedMiniZX.ofProgram(memory, START, STACK, 1000, stackAnalyzer), START);
    Assert.assertTrue(footprint.explored().containsKey(0x8030));
  }

  @Test
  public void aFootprintSurvivesBeingSavedAndReadBack() throws Exception {
    // the footprint of a recording is saved to disk so that changes to the exploration or the generator do not replay the RZX
    int[] memory = new int[0x10000];
    int[][] chunks = {at(0x8000, 0xCD, 0x10, 0x80, 0x2A, 0x06, 0x01, 0x76, 0x18, 0xFD), at(0x8010, 0xE1, 0x23, 0xE9)};
    for (int[] chunk : chunks)
      System.arraycopy(chunk, 1, memory, chunk[0], chunk.length - 1);
    RemoteZ80Translator.Footprint footprint = RemoteZ80Translator.footprint(stackAnalyzer -> EmulatedMiniZX.ofProgram(memory, START, STACK, 1000, stackAnalyzer), START);
    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
      out.writeObject(footprint);
    }
    RemoteZ80Translator.Footprint read = (RemoteZ80Translator.Footprint) new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())).readObject();
    Assert.assertEquals(footprint.executed(), read.executed());
    Assert.assertEquals(footprint.learned().callContinuations, read.learned().callContinuations);
    Assert.assertEquals(footprint.learned().shiftedReturns, read.learned().shiftedReturns);
    Assert.assertEquals(footprint.romEntries(), read.romEntries());
  }

  @Test
  public void aRoutineThatUsesTheStackAsADataPointerStillReturnsToItsCaller() {
    // Emlyn: LD (nn),SP / LD SP,data / POPs / LD SP,(nn), the pops are reads of a table, not returns
    savesStackPointerAt(0x9020);
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xED, 0x73, 0x20, 0x90, 0x31, 0x00, 0x90, 0xE1, 0xD1, 0xED, 0x7B, 0x20, 0x90, 0xC9),
        at(0x9000, 0x34, 0x12, 0x78, 0x56));
    Assert.assertEquals("8000 8010", routines());
    Assert.assertTrue(stackAnalyzer.poppedCallSites.isEmpty());
    Assert.assertFalse(stackAnalyzer.callContinuations.containsKey(0x8000));
  }

  @Test
  public void aCallSiteWhoseContinuationDependsOnARegisterIsAJumpTable() {
    // Dizzy: CALL dispatcher, which pops the return address, adds an index and jumps there
    translate(
        at(0x8000, 0x3E, 0x00, 0xCD, 0x20, 0x80, 0x3E, 0x05, 0xC3, 0x02, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8020, 0xE1, 0x5F, 0x16, 0x00, 0x19, 0xE9));
    Assert.assertTrue(stackAnalyzer.jumpTableSites.contains(0x8002));
    Assert.assertEquals(Set.of(0x8005, 0x800A), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8025)));
    Assert.assertFalse(stackAnalyzer.callContinuations.containsKey(0x8002));
  }

  @Test
  public void aJumpIntoTheRomReturnsToTheCallerOfTheRoutine() {
    // a routine that ends with JP into a ROM routine: the ROM's RET returns to the routine's caller
    translate(
        at(0x0020, 0x16, 0x03, 0xC9),
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x0E, 0x02, 0xC3, 0x20, 0x00));
    Assert.assertEquals("20 8000 8010", routines());
  }

  @Test
  public void aCallNotTakenInTheRecordingIsExploredByAFork() {
    translate(
        at(0x8000, 0xAF, 0xC4, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8020, 0x21, 0x34, 0x12, 0xC9));
    Assert.assertEquals("8000 8020", routines());
  }

  @Test
  public void aConditionalReturnTakenInTheRecordingHasItsOtherSideExploredByAFork() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3C, 0xC0, 0x3E, 0x04, 0x32, 0x21, 0x80, 0xC9),
        at(0x8020, 0x0E, 0x01, 0xC9));
    Assert.assertTrue(stackAnalyzer.codeVersions.mutant().contains(0x8021));
  }

  @Test
  public void aForkThatLoopsForeverStopsAtItsBudget() {
    translate(
        at(0x8000, 0xAF, 0xC2, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x18, 0xFE));
    Assert.assertEquals("8000", routines());
  }

  @Test
  public void aRoutineLeavesAPlantedContinuationForTheRoutineItJumpsTo() {
    // Emlyn 616E: pushes 660D and jumps to a routine others call too; that routine's RET lands on the planted address
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x21, 0x18, 0x80, 0xE5, 0xC3, 0x20, 0x80),
        at(0x8018, 0x16, 0x03, 0xC9),
        at(0x8020, 0x0E, 0x02, 0xC9));
    Assert.assertEquals(Set.of(0x8013), Set.copyOf(stackAnalyzer.dataConsumedBy.get(0x8022)));
    Assert.assertEquals(Set.of(0x8013), Set.copyOf(stackAnalyzer.dataOnTopAt.get(0x8014)));
    Assert.assertEquals("8000 8010 8018 8020", routines());
  }

  @Test
  public void twoRoutinesThatJumpToEachOtherRunThroughATrampoline() {
    translate(
        at(0x8000, 0x3E, 0x03, 0xCD, 0x10, 0x80, 0x3E, 0x02, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3D, 0xC8, 0xC3, 0x20, 0x80),
        at(0x8020, 0x0C, 0xC3, 0x10, 0x80));
    Assert.assertEquals(Set.of(0x8010, 0x8020), routineManager.routinesInJumpCycles(address -> Set.copyOf(stackAnalyzer.dynamicInvocation.get(address))).stream().map(Routine::getEntryPoint).collect(Collectors.toSet()));
  }

  @Test
  public void aRoutineEnteredInTheMiddleFromAnotherRoutineIsSplitThere() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x0E, 0x01, 0x16, 0x02, 0xC9),
        at(0x8020, 0x1E, 0x03, 0xC3, 0x12, 0x80));
    Assert.assertEquals("8000 8010 8012 8020", routines());
  }

  @Test
  public void aRomEntryThatIsATrampolineThroughIxIsNotARoutine() {
    translate(
        at(0x0020, 0xDD, 0xE9),
        at(0x8000, 0xDD, 0x21, 0x10, 0x80, 0xCD, 0x20, 0x00, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("8000 8010", routines());
    Assert.assertEquals(Set.of(0x8010), Set.copyOf(stackAnalyzer.calledThrough.get(0x8004)));
  }

  @Test
  public void aReturnAfterPointingTheStackAtATableIsAJump() {
    translate(
        at(0x8000, 0x31, 0x00, 0x90, 0xC9),
        at(0x8010, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x9000, 0x10, 0x80));
    Assert.assertEquals(Set.of(0x8010), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8003)));
  }

  @Test
  public void exchangesBlockInstructionsPortReadsAndSixteenBitDecrementsRunLikeTheEmulator() {
    translate(
        at(0x8000, 0x21, 0x34, 0x12, 0x01, 0x78, 0x56, 0xC5, 0xE3, 0xD1, 0x08, 0x3E, 0x07, 0x08, 0xD9, 0x0B, 0xD9, 0x21, 0x00, 0x90, 0x01, 0x10, 0x00, 0x3E, 0x03, 0xED, 0xB1,
            0x11, 0x0F, 0x90, 0x21, 0x0E, 0x90, 0x01, 0x05, 0x00, 0xED, 0xB8, 0xDB, 0xFE, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x9000, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F));
    Assert.assertEquals("8000", routines());
  }

  @Test
  public void aRoutineTooLargeForOneJavaMethodIsSplit() {
    int[] program = new int[4000 + 6];
    program[0] = START;
    Arrays.fill(program, 1, 4001, 0x3C);
    System.arraycopy(new int[]{0x06, 0x01, 0x76, 0x18, 0xFD}, 0, program, 4001, 5);
    translate(program);
    Assert.assertTrue(routines(), routineManager.getRoutines().size() > 1);
  }

  @Test
  public void theFootprintsOfTwoRecordingsCombineIntoOne() {
    int[] first = new int[0x10000], second = new int[0x10000];
    System.arraycopy(new int[]{0x06, 0x01, 0x76, 0x18, 0xFD}, 0, first, 0x8000, 5);
    System.arraycopy(new int[]{0xC3, 0x10, 0x81}, 0, second, 0x8100, 3);
    System.arraycopy(new int[]{0x0E, 0x02, 0x76, 0x18, 0xFD}, 0, second, 0x8110, 5);
    RemoteZ80Translator.Footprint a = RemoteZ80Translator.footprint(stackAnalyzer -> EmulatedMiniZX.ofProgram(first, 0x8000, STACK, 1000, stackAnalyzer), 0x8000);
    RemoteZ80Translator.Footprint b = RemoteZ80Translator.footprint(stackAnalyzer -> EmulatedMiniZX.ofProgram(second, 0x8100, STACK, 1000, stackAnalyzer), 0x8100);
    Set<Integer> both = new java.util.TreeSet<>(a.codeBytes().keySet());
    both.addAll(b.codeBytes().keySet());
    Assert.assertEquals(both, new java.util.TreeSet<>(RemoteZ80Translator.Footprint.combine(List.of(a, b)).codeBytes().keySet()));
  }

  @Test
  public void aRoutineEnteredInItsMiddleWhoseTailLoopsBackToItsHeadBecomesAJumpCycle() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3C, 0x16, 0x02, 0xFE, 0x03, 0x20, 0xF9, 0xC9),
        at(0x8020, 0x1E, 0x03, 0xC3, 0x11, 0x80));
    Assert.assertEquals("8000 8010 8011 8020", routines());
    Assert.assertEquals(Set.of(0x8010, 0x8011), routineManager.routinesInJumpCycles(address -> Set.copyOf(stackAnalyzer.dynamicInvocation.get(address))).stream().map(Routine::getEntryPoint).collect(Collectors.toSet()));
  }

  @Test
  public void aRoutineEnteredInItsMiddleWhoseHeadJumpsPastTheEntryIsSplitAtBothPlaces() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x0E, 0x01, 0x18, 0x02, 0x16, 0x02, 0x1E, 0x03, 0xC9),
        at(0x8020, 0xC3, 0x14, 0x80));
    Assert.assertEquals("8000 8010 8016 8020", routines());
  }

  @Test
  public void discardingPushedDataByResettingTheStackPointerIsNotAReturn() {
    translate(at(0x8000, 0x21, 0x34, 0x12, 0xE5, 0x31, 0x00, 0xFF, 0x06, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertEquals("8000", routines());
    Assert.assertTrue(routineManager.nonLocalReturns.isEmpty());
  }

  @Test
  public void aRoutineThatPointsTheStackAtATableThroughHlStillReturnsToItsCaller() {
    savesStackPointerAt(0x9020);
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xED, 0x73, 0x20, 0x90, 0x21, 0x00, 0x90, 0xF9, 0xE1, 0xD1, 0xED, 0x7B, 0x20, 0x90, 0xC9),
        at(0x9000, 0x34, 0x12, 0x78, 0x56));
    Assert.assertEquals("8000 8010", routines());
    Assert.assertTrue(stackAnalyzer.poppedCallSites.isEmpty());
  }

  @Test
  public void arithmeticRotatesAndBitOperationsRunLikeTheEmulator() {
    translate(
        at(0x8000, 0x3E, 0x95, 0x27, 0x07, 0x0F, 0x17, 0x1F, 0x2F, 0x37, 0x3F, 0xED, 0x44, 0x06, 0x03, 0xCB, 0x20, 0xCB, 0x28, 0xCB, 0x38, 0x0E, 0x81, 0xCB, 0x01, 0xCB, 0x09,
            0x16, 0x42, 0xCB, 0x12, 0x1E, 0x24, 0xCB, 0x1B, 0xCB, 0x5F, 0xCB, 0xD7, 0xCB, 0x87, 0x80, 0x89, 0x92, 0x9B, 0x26, 0xF0, 0xA4, 0x2E, 0x0F, 0xAD, 0xB0, 0xB9, 0x24, 0x2D,
            0x09, 0xED, 0x5A, 0xED, 0x42, 0x21, 0x00, 0x90, 0xED, 0x6F, 0xED, 0x67, 0x7E, 0x34, 0x35, 0xDD, 0x21, 0x00, 0x90, 0xDD, 0x36, 0x01, 0x05, 0xDD, 0x34, 0x01,
            0xDD, 0xCB, 0x01, 0x46, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x9000, 0x3C, 0x00));
    Assert.assertEquals("8000", routines());
  }

  @Test
  public void programsCombiningTheKnownTricksAtRandomRunLikeTheEmulator() {
    for (int seed = 1; seed <= 24; seed++) {
      Random random = new Random(seed);
      ignoredMemory.clear();
      int[] data = {0x9000};
      List<int[]> chunks = new ArrayList<>();
      int count = 2 + random.nextInt(4);
      int[] main = new int[count * 3 + 3];
      for (int i = 0; i < count; i++) {
        int routine = 0x8100 + i * 0x40;
        System.arraycopy(new int[]{0xCD, lo(routine), hi(routine)}, 0, main, i * 3, 3);
        chunks.addAll(trick(random.nextInt(15), routine, data, random));
      }
      System.arraycopy(new int[]{0x76, 0x18, 0xFD}, 0, main, count * 3, 3);
      chunks.add(0, at(START, main));
      base = Guice.createInjector(new RoutinesModule()).getInstance(RoutinesDriverConfigurator.class).getRealCodeBytecodeCreationBase();
      try {
        translate(chunks.toArray(new int[0][]));
      } catch (AssertionError | RuntimeException e) {
        throw new AssertionError("seed " + seed + ": " + chunks.stream().map(chunk -> "%04X:%s".formatted(chunk[0], Arrays.stream(chunk, 1, chunk.length).mapToObj("%02X"::formatted).collect(Collectors.joining(" ")))).collect(Collectors.joining(" | ")), e);
      }
    }
  }

  private List<int[]> trick(int kind, int base, int[] data, Random random) {
    int sub = base + 0x20, first = base + 0x28, second = base + 0x2B, planted = base + 0x10;
    return switch (kind) {
      case 0 -> List.of(at(base, 0x06, random.nextInt(256), 0x0E, random.nextInt(256), 0xC9));
      case 1 -> List.of(at(base, 0xCD, lo(sub), hi(sub), 0x41, 0x42, 0xFF, 0x06, 0x01, 0xC9), at(sub, 0xE1, 0x7E, 0x23, 0xFE, 0xFF, 0x20, 0xFA, 0xE9));
      case 2 -> List.of(at(base, 0x21, lo(first), hi(first), 0x22, lo(sub + 1), hi(sub + 1), 0xCD, lo(sub), hi(sub), 0x21, lo(second), hi(second), 0x22, lo(sub + 1), hi(sub + 1), 0xCD, lo(sub), hi(sub), 0xC9),
          at(sub, 0xCD, 0x00, 0x00, 0xC9), at(first, 0x06, 0x01, 0xC9), at(second, 0x0E, 0x02, 0xC9));
      case 3 -> List.of(at(base, 0x3E, 0x05, 0x32, lo(sub + 1), hi(sub + 1), 0xCD, lo(sub), hi(sub), 0x3E, 0x07, 0x32, lo(sub + 1), hi(sub + 1), 0xCD, lo(sub), hi(sub), 0xC9), at(sub, 0xFE, 0x00, 0xC9));
      case 4 -> List.of(at(base, 0x3E, random.nextInt(2), 0xFE, 0x01, 0x28, 0x02, 0x06, 0x01, 0x0E, 0x02, 0xC9));
      case 5 -> List.of(at(base, 0x06, 1 + random.nextInt(20), 0x3C, 0x10, 0xFD, 0xC9));
      case 6 -> List.of(at(base, 0x21, lo(planted), hi(planted), 0xE5, 0xC9), at(planted, 0x06, 0x01, 0xC9));
      case 7 -> List.of(at(base, 0x21, lo(sub), hi(sub), 0xE5, 0xC3, lo(planted), hi(planted)), at(planted, 0x0E, 0x02, 0xC9), at(sub, 0x16, 0x03, 0xC9));
      case 8 -> {
        int saved = allocate(data, 2), table = allocate(data, 4);
        savesStackPointerAt(saved);
        yield List.of(at(base, 0xED, 0x73, lo(saved), hi(saved), 0x31, lo(table), hi(table), 0xE1, 0xD1, 0xED, 0x7B, lo(saved), hi(saved), 0xC9), at(table, 0x34, 0x12, 0x78, 0x56));
      }
      case 9 -> List.of(at(base, 0xD7, 0x06, 0x01, 0xC9), at(0x0010, 0xAF, 0xC9));
      case 10 -> List.of(at(base, 0x3E, 0x03, 0xCD, lo(planted), hi(planted), 0x3E, 0x02, 0xCD, lo(sub), hi(sub), 0xC9), at(planted, 0x3D, 0xC8, 0xC3, lo(sub), hi(sub)), at(sub, 0x0C, 0xC3, lo(planted), hi(planted)));
      case 11 -> List.of(at(base, 0x0E, 0x02, 0xC3, 0x20, 0x00), at(0x0020, 0x16, 0x03, 0xC9));
      case 12 -> List.of(at(base, 0xCD, lo(sub), hi(sub), 0x06, 0x09, 0xC9), at(sub, 0x31, lo(STACK - 2), hi(STACK - 2), 0xC9));
      case 13 -> List.of(at(base, 0xAF, 0xC4, lo(sub), hi(sub), 0x3C, 0xC0, 0x0E, 0x02, 0xC9), at(sub, 0x0E, 0x05, 0xC9));
      default -> List.of(at(base, 0x3E, 0x1C, 0x32, lo(sub), hi(sub), 0xCD, lo(sub), hi(sub), 0x3E, 0x14, 0x32, lo(sub), hi(sub), 0xCD, lo(sub), hi(sub), 0xC9), at(sub, 0x00, 0xC9));
    };
  }

  private static int allocate(int[] data, int size) {
    int address = data[0];
    data[0] += size;
    return address;
  }

  private static int lo(int address) {
    return address & 0xff;
  }

  private static int hi(int address) {
    return address >> 8;
  }

  @Test
  public void aLoopEnteredInItsMiddleIsDecompiledAndRunsLikeTheEmulator() {
    // Fernflower empties methods with loops that have two entries; the generator then splits the routine and retries
    translate(at(0x8000, 0x06, 0x05, 0x18, 0x02, 0x3C, 0x0C, 0x10, 0xFC, 0x76, 0x18, 0xFD));
    Assert.assertTrue(routines(), routines().startsWith("8000"));
  }

  @Test
  public void aRomEntryThatIsATrampolineThroughIyIsNotARoutine() {
    translate(
        at(0x0020, 0xFD, 0xE9),
        at(0x8000, 0xFD, 0x21, 0x10, 0x80, 0xCD, 0x20, 0x00, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("8000 8010", routines());
    Assert.assertEquals(Set.of(0x8010), Set.copyOf(stackAnalyzer.calledThrough.get(0x8004)));
  }

  @Test
  public void aForkThatKeepsFindingFreshConditionalCodeStopsAtItsBudget() {
    int[] branches = new int[1 + 700 * 2 + 1];
    branches[0] = 0x8010;
    for (int i = 0; i < 700; i++) {
      branches[1 + i * 2] = 0x28;
      branches[2 + i * 2] = 0x00;
    }
    branches[branches.length - 1] = 0xC9;
    translate(at(0x8000, 0xAF, 0xC2, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD), branches);
    Assert.assertEquals("8000", routines());
  }

  @Test
  public void aConditionalReturnNotTakenInTheRecordingHasItsReturnExploredByAFork() {
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xAF, 0xC0, 0x0E, 0x02, 0xC9));
    Assert.assertEquals("8000 8010", routines());
  }

  @Test
  public void aPushedAddressComputedAfterLoadingItIsNotAPlantedContinuation() {
    // the value is loaded and then incremented before the push, so the RET that lands on it is a jump through the popped value
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x21, 0x17, 0x80, 0x23, 0xE5, 0xC3, 0x20, 0x80),
        at(0x8018, 0x16, 0x03, 0xC9),
        at(0x8020, 0x0E, 0x02, 0xC9));
    Assert.assertEquals(Set.of(0x8014), Set.copyOf(stackAnalyzer.dataConsumedBy.get(0x8022)));
    Assert.assertEquals("Ld", instructionAt(0x8018));
  }

  @Test
  public void interruptModeRefreshAndSearchInstructionsRunLikeTheEmulator() {
    translate(
        at(0x8000, 0xED, 0x56, 0xFB, 0xF3, 0xED, 0x5F, 0xED, 0x57, 0x06, 0x81, 0xCB, 0x30, 0x21, 0x00, 0x90, 0x11, 0x10, 0x90, 0x01, 0x03, 0x00, 0xED, 0xA0, 0xED, 0xA8,
            0x3E, 0x02, 0x01, 0x08, 0x00, 0xED, 0xA1, 0xED, 0xA9, 0x21, 0x07, 0x90, 0xED, 0xB9, 0xEB, 0xDD, 0x21, 0x34, 0x12, 0xDD, 0xE5, 0xDD, 0xE3, 0xDD, 0xE1, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x9000, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09));
    Assert.assertEquals("8000", routines());
  }

  @Test
  public void aJumpThroughHlRightAfterPushingTheNextAddressIsACall() {
    // Emlyn: PUSH of the address right after the JP (HL), so the callee's RET comes back as if it had been called
    translate(
        at(0x8000, 0x21, 0x08, 0x80, 0xE5, 0x21, 0x20, 0x80, 0xE9, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8020, 0x0E, 0x02, 0xC9));
    Assert.assertEquals(RoutineManager.Exit.SIMULATED_CALL, routineManager.exitOfJump(0x8007, routineManager.findRoutineAt(0x8007), stackAnalyzer));
    Assert.assertEquals("8000 8020", routines());
  }

  @Test
  public void aReturnToAnAddressThatWasOnTheStackBeforeTheRecordingContinuesThere() {
    // Dizzy: the recording starts inside the interrupt handler, whose RET goes back to the game through the snapshot's stack
    translate(
        at(0x8000, 0x0E, 0x02, 0xC9),
        at(0x8010, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(STACK, 0x10, 0x80));
    Assert.assertEquals("8000 8010", routines());
  }

  @Test
  public void aProgramImageLongerThanOneStringConstantIsCarriedInChunks() {
    int[] noise = new int[1 + 0x6000];
    noise[0] = 0x9000;
    Random random = new Random(1);
    for (int i = 1; i < noise.length; i++)
      noise[i] = random.nextInt(256);
    translate(at(0x8000, 0x06, 0x01, 0x76, 0x18, 0xFD), noise);
    Assert.assertEquals("8000", routines());
  }

  @Test
  public void aJumpToARomTrampolineGoesOnAtTheTargetOfItsRegister() {
    // Dynamite Dan FFD1: the loader ends with JP 006F, the ROM NMI handler's JP (HL), to reach the game at C800 in HL
    translate(
        at(0x8000, 0x21, 0x50, 0x80, 0xC3, 0x6F, 0x00),
        at(0x8050, 0x3E, 0x07, 0x76, 0x18, 0xFD),
        at(0x006F, 0xE9));
    Assert.assertTrue(stackAnalyzer.dynamicInvocation.get(0x006F).contains(0x8050));
    Assert.assertEquals(0x07, endValue("A"));
  }

  @Test
  public void aCallThroughARomTrampolineThatReallyPushesItsReturnPopsItAfterTheCalleeReturns() {
    // Emlyn Hughes 5FF1: CALL 162C (CALL-JUMP's JP (HL)) pushes its return for real because the stack is read by address, and nothing popped it afterwards
    translate(
        at(0x8000, 0x11, 0x34, 0x12, 0xD5, 0x21, 0x20, 0x80, 0xCD, 0x2C, 0x16, 0xC1, 0x76, 0x18, 0xFD),
        at(0x8020, 0x21, 0x00, 0x00, 0x39, 0x7E, 0xC9),
        at(0x162C, 0xE9));
    Assert.assertTrue(routineManager.pushedReturnSites.contains(0x8007));
    Assert.assertEquals(0x1234, endValue("BC"));
  }

  @Test
  public void theLandingOfAStackResetThatDropsAReturnCanBeEnteredByNumber() {
    // Everyone's a Wally 8185: LD SP,FC00 drops returns and its exception goes on at 8188; when the loader jumped in, no translated caller catches it and MiniZX.run enters 8188 by number
    ignoresMemory(0xFB00, 0xFC00);
    translate(
        at(0x8000, 0x31, 0xFE, 0xFB, 0xCD, 0x50, 0x80, 0x76, 0x18, 0xFD),
        at(0x8050, 0xF3, 0x31, 0x00, 0xFC, 0xCD, 0x70, 0x80, 0x04, 0x18, 0xFA),
        at(0x8070, 0x3C, 0xC9));
    Assert.assertTrue(routineManager.externalEntries.contains(0x8054));
    Assert.assertTrue(endValue("A") > 0);
  }

  @Test
  public void aStackResetDropsAReturnEvenWhenTheWordsAboveItWereWrittenByPlainStores() {
    // Batman 61BA: LD SP from EFE6 to EFEE leaves the return of CALL EC5E behind; EFEA-EFED had been written by a plain store, not by a push
    ignoresMemory(0xFB00, 0xFC08);
    translate(
        at(0x8000, 0x31, 0x00, 0xFC, 0xCD, 0x50, 0x80, 0x76, 0x18, 0xFD),
        at(0x8050, 0xF3, 0x21, 0x00, 0x00, 0x22, 0x00, 0xFC, 0x31, 0x04, 0xFC, 0xCD, 0x70, 0x80, 0x04, 0x18, 0xFA),
        at(0x8070, 0x3C, 0xC9));
    Assert.assertTrue(routineManager.externalEntries.contains(0x805A));
    Assert.assertTrue(endValue("A") > 0);
  }

  @Test
  public void aRecordingStartsAtTheFirstInstructionItRunsInGameRam() {
    // Monty on the Run's snapshot sits at AAC5 in the game; Skool Daze's sits in the ROM interrupt at 0038 and the game goes on at 673F
    Assert.assertEquals(0xAAC5, RemoteZ80Translator.startOf("/home/fernando/detodo/spectrum/montyontherun.rzx"));
    Assert.assertEquals(0x673F, RemoteZ80Translator.startOf("/home/fernando/detodo/spectrum/rzx-top/4549/skooldaze.rzx"));
  }

  @Test
  public void aRecordingIsEmulatedWithTheRomInPlace() {
    // Dynamite Dan II: its snapshot sits in the ROM interrupt routine; with the ROM protected before the snapshot loader filled it, the emulator walked zeros up to the screen
    EmulatedMiniZX emulator = EmulatedMiniZX.ofRecording("/home/fernando/detodo/spectrum/jsw/jsw-full.rzx", 1, null);
    emulator.start();
    Assert.assertEquals(0xF5, emulator.ooz80.getState().getMemory().read(0x0038, 0));
  }

  @Test
  public void theFirstInstructionOfAnInterruptHandlerIsCountedWithTheInterruptBeforeTheFrameCounterIsConsulted() {
    // Emlyn: its recording has frames of 0-3 fetches right after the interrupt; the vector's JP belongs to the interrupt's frame, so a frame cannot end between them
    interruptEvery = 300;
    int[] table = new int[1 + 257];
    table[0] = 0x9000;
    Arrays.fill(table, 1, table.length, 0x80);
    int[][] chunks = {at(0x8000, 0x3E, 0x90, 0xED, 0x47, 0xED, 0x5E, 0xFB, 0x3C, 0x18, 0xFD), at(0x8080, 0xC3, 0x90, 0x80), at(0x8090, 0x0C, 0xFB, 0xC9), table};
    translate(chunks);
    MiniZX program = BytecodeGeneration.translatedProgram("Program", read(Path.of("Program.class")));
    EmulatedMiniZX emulator = EmulatedMiniZX.ofProgram(memoryOf(chunks), start, STACK, 0, null);
    emulator.start();
    program.loadState(emulator.ooz80.getState());
    List<String> consulted = new ArrayList<>();
    program.setInterruptionCondition(fetches -> {
      consulted.add(Integer.toHexString(program.PC) + ":" + fetches);
      if (consulted.size() > 2000)
        throw new Finished();
      return fetches == 300;
    });
    try {
      program.run(start);
    } catch (Finished finished) {
    }
    Assert.assertTrue(consulted.toString(), consulted.contains("8080:302"));
    Assert.assertTrue(consulted.contains("8090:302"));
  }

  @Test
  public void anOutToTheUlaMakesTheBeeperSound() {
    // the speaker bit toggled at the OUTs' T-states comes out of the card as samples at the end of the frames
    Assert.assertTrue(soundOf(at(0x8000, 0x3E, 0x10, 0xD3, 0xFE, 0x3E, 0x00, 0xD3, 0xFE, 0x3C, 0x18, 0xF5)) > 0);
  }

  @Test
  public void aToneProgrammedOnThe128KSoundChipSounds() {
    // Rainbow Islands and Head over Heels play their music on the AY: register at FFFD, value at BFFD; here a tone on channel A at full volume
    Assert.assertTrue(soundOf(at(0x8000, 0x01, 0xFD, 0xFF, 0x3E, 0x00, 0xED, 0x79, 0x06, 0xBF, 0x3E, 0x50, 0xED, 0x79, 0x06, 0xFF, 0x3E, 0x07, 0xED, 0x79, 0x06, 0xBF, 0x3E, 0x3E, 0xED, 0x79,
        0x06, 0xFF, 0x3E, 0x08, 0xED, 0x79, 0x06, 0xBF, 0x3E, 0x0F, 0xED, 0x79, 0x18, 0xFE)) > 0);
  }

  @Test
  public void aProgramThatNeverOutsIsSilent() {
    Assert.assertEquals(0, soundOf(at(0x8000, 0x3E, 0x10, 0x3C, 0x18, 0xFD)));
  }

  private int soundOf(int[]... chunks) {
    translate(chunks);
    MiniZX program = BytecodeGeneration.translatedProgram("Program", read(Path.of("Program.class")));
    EmulatedMiniZX emulator = EmulatedMiniZX.ofProgram(memoryOf(chunks), start, STACK, 0, null);
    emulator.start();
    program.loadState(emulator.ooz80.getState());
    int[] frames = {0}, sounding = {0};
    program.sound = new MiniZXSound(new com.fpetrola.oozx.speccy.modules.sound.SoundCard() {
      public int open(String device, int[] freq, int[] stereo) {
        return 0;
      }

      public void play(int[] samples, int count) {
        frames[0]++;
        sounding[0] += (int) IntStream.range(0, count).filter(i -> samples[i] != 0).count();
      }

      public void close() {
      }
    });
    program.setInterruptionCondition(fetches -> {
      if (frames[0] == 3)
        throw new Finished();
      return fetches % 300 == 0;
    });
    try {
      program.run(start);
    } catch (Finished finished) {
    }
    return sounding[0];
  }

  @Test
  public void everyInstructionCostsTheTStatesTheEmulatorCharges() {
    // taken and untaken branches, a DJNZ loop, a block copy, port accesses, HALT and an interrupt: the beeper's pitch depends on each of them
    interruptEvery = 400;
    translate(
        at(0x0038, 0x0C, 0xFB, 0xC9),
        at(0x8000, 0x3E, 0x05, 0x21, 0x00, 0x90, 0x77, 0x34, 0x28, 0x02, 0x20, 0x00, 0x06, 0x03, 0x10, 0xFE, 0xCD, 0x30, 0x80, 0x11, 0x10, 0x90, 0x01, 0x03, 0x00, 0xED, 0xB0,
            0xE3, 0xDB, 0xFE, 0xD3, 0xFE, 0xFB, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8030, 0xAF, 0xC0, 0xC9));
    Assert.assertArrayEquals(new int[]{8, 13}, UncontendedTiming.costOf(0x10, 0xFE));
    Assert.assertArrayEquals(new int[]{16, 21}, UncontendedTiming.costOf(0xED, 0xB0));
    Assert.assertArrayEquals(new int[]{11, 11}, UncontendedTiming.costOf(0xD3, 0xFE));
  }

  @Test
  public void anInterruptHandlerRecordedBetweenInstructionsIsTranslatedAndInterruptsTheJavaCode() {
    interruptEvery = 300;
    translate(
        at(0x0038, 0x0C, 0xFB, 0xC9),
        at(0x8000, 0xED, 0x56, 0xFB, 0x3C, 0x18, 0xFD));
    Assert.assertEquals("38 8000", routines());
  }

  @Test
  public void anInterruptVectorInRamIsTranslatedAndTheInterruptEntersThroughIt() {
    // R-Type: IM 2, the vector 8383 holds JP BE23 and nothing else reaches the handler
    interruptEvery = 300;
    int[] table = new int[1 + 257];
    table[0] = 0x9000;
    Arrays.fill(table, 1, table.length, 0x80);
    translate(
        at(0x8000, 0x3E, 0x90, 0xED, 0x47, 0xED, 0x5E, 0xFB, 0x3C, 0x18, 0xFD),
        at(0x8080, 0xC3, 0x90, 0x80),
        at(0x8090, 0x0C, 0xFB, 0xC9),
        table);
    Assert.assertTrue(routines(), routines().contains("8080"));
  }

  @Test
  public void aConditionalJumpIntoTheScreenIsNotExploredAsCode() {
    // the symbolic execution used to walk the zeros of the screen as NOPs up to the game's code, in sixteen routines
    translate(at(0x8000, 0xAF, 0xC2, 0x00, 0x50, 0x06, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertEquals("8000", routines());
  }

  @Test
  public void aRoutineTooLargeForOneJavaMethodIsSplitAtItsBlocks() {
    int[] first = new int[1 + 2000 + 3], second = new int[1 + 2000 + 5];
    first[0] = START;
    Arrays.fill(first, 1, 2001, 0x3C);
    System.arraycopy(new int[]{0xC3, 0x00, 0x88}, 0, first, 2001, 3);
    second[0] = 0x8800;
    Arrays.fill(second, 1, 2001, 0x3C);
    System.arraycopy(new int[]{0x06, 0x01, 0x76, 0x18, 0xFD}, 0, second, 2001, 5);
    translate(first, second);
    Assert.assertTrue(routines(), routineManager.getRoutines().size() >= 2 && routines().contains("8800"));
  }

  @Test
  public void aRecursiveRoutineIsTranslatedAsARecursiveMethod() {
    translate(
        at(0x8000, 0x3E, 0x03, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3D, 0xC8, 0xCD, 0x10, 0x80, 0xC9));
    Assert.assertEquals("8000 8010", routines());
  }

  @Test
  public void aStoreIntoTheRomDoesNotMakeRomCodeMutant() {
    // Dynamite Dan II: a store through HL lands on the ROM BEEPER, which can never change
    translate(
        at(0x0020, 0x06, 0x01, 0xC9),
        at(0x8000, 0xCD, 0x20, 0x00, 0x21, 0x21, 0x00, 0x36, 0x05, 0xCD, 0x20, 0x00, 0x76, 0x18, 0xFD));
    Assert.assertEquals(Set.of(), versions.modifiedBytes());
  }

  @Test
  public void aDispatchingRetThatOnceConsumedAStaleStackValueStillDispatches() {
    // The Great Escape AB30: the first time, the RET consumed a value left on the stack before the recording,
    // which looks like a return of unknown origin; every other time it jumps to the handler its routine pushed
    translateFrom(0x8020,
        at(0x8000, 0x3E, 0x01, 0xCD, 0x20, 0x80, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x0C, 0xC9),
        at(0x8020, 0x21, 0x10, 0x80, 0xA7, 0x28, 0x03, 0xE5, 0x18, 0x02, 0x3B, 0x3B, 0xC9),
        at(STACK - 2, 0x10, 0x80, 0x00, 0x80));
    Assert.assertEquals(Set.of(-1), Set.copyOf(stackAnalyzer.returnsConsumedBy.get(0x802B)));
    Assert.assertEquals(Set.of(0x8026), Set.copyOf(stackAnalyzer.dataConsumedBy.get(0x802B)));
  }

  @Test
  public void twoStacksThatHandControlToEachOtherRunAsCoroutines() {
    // Zynaps 8D60/8D69: LD (a),SP; LD SP,(b); RET resumes the other stack where it last gave control away
    ignoresMemory(0x9018, 0x901F);
    translate(
        at(0x8000, 0x21, 0x40, 0x80, 0x22, 0x1E, 0x90, 0x21, 0x1E, 0x90, 0x22, 0x1A, 0x90, 0xCD, 0x20, 0x80, 0x04, 0xCD, 0x20, 0x80, 0x04, 0x76, 0x18, 0xFD),
        at(0x8020, 0xED, 0x73, 0x18, 0x90, 0xED, 0x7B, 0x1A, 0x90, 0xC9, 0xED, 0x73, 0x1A, 0x90, 0xED, 0x7B, 0x18, 0x90, 0xC9),
        at(0x8040, 0x0C, 0xCD, 0x29, 0x80, 0x14, 0xCD, 0x29, 0x80, 0x18, 0xF6));
    Assert.assertEquals(Set.of(0x8040, 0x8044), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8028)));
    Assert.assertEquals(Set.of(0x800F), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8031)));
  }

  @Test
  public void aStackRebuiltFromScratchStartsANewCoroutineInsteadOfResumingTheParkedOne() {
    // Zynaps 8D35: LD HL,FDFB; LD (801A),HL; LD HL,8D72; LD (FDFB),HL restarts the coroutine at a level change; the body yields with one push, so the rebuilt stack sits where the parked one left
    ignoresMemory(0x9018, 0x901F);
    translate(
        at(0x8000, 0x21, 0x80, 0x80, 0x22, 0x1E, 0x90, 0x21, 0x1E, 0x90, 0x22, 0x1A, 0x90, 0xCD, 0x60, 0x80, 0x04, 0xCD, 0x60, 0x80, 0x04,
            0x21, 0x80, 0x80, 0x22, 0x1E, 0x90, 0x21, 0x1E, 0x90, 0x22, 0x1A, 0x90, 0xCD, 0x60, 0x80, 0x04, 0xCD, 0x60, 0x80, 0x04, 0x76, 0x18, 0xFD),
        at(0x8060, 0xED, 0x73, 0x18, 0x90, 0xED, 0x7B, 0x1A, 0x90, 0xC9, 0xED, 0x73, 0x1A, 0x90, 0xED, 0x7B, 0x18, 0x90, 0xC9),
        at(0x8080, 0x0C, 0xE5, 0xCD, 0x69, 0x80, 0xE1, 0x14, 0xE5, 0xCD, 0x69, 0x80, 0xE1, 0x1C, 0xE5, 0xCD, 0x69, 0x80, 0xE1, 0x18, 0xEC));
    Assert.assertEquals(Set.of(0x8080, 0x8085), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8068)));
  }

  @Test
  public void aStackRestoredToPopRegistersBeforeItsReturnIsNoCoroutine() {
    // Target: Renegade D996-D9A9: the blitter keeps a background buffer as a second stack (LD SP,(F6FC); PUSH BC; LD (F6FC),SP), then LD SP,(F6F6); POP DE; POP HL; RET
    ignoresMemory(0x8F00, 0x9300);
    translate(
        at(0x8000, 0x31, 0x00, 0x90, 0x21, 0x00, 0x92, 0x22, 0x02, 0x91, 0x21, 0x00, 0x70, 0x11, 0x00, 0x60, 0xCD, 0x20, 0x80, 0xCD, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0xE5, 0xD5, 0xED, 0x73, 0x00, 0x91, 0xED, 0x7B, 0x02, 0x91, 0xC5, 0xED, 0x73, 0x02, 0x91, 0xED, 0x7B, 0x00, 0x91, 0xD1, 0xE1, 0xC9));
    Assert.assertTrue(stackAnalyzer.stackSwitches.toString(), stackAnalyzer.stackSwitches.isEmpty());
  }

  @Test
  public void aStackEnteredToCallOnItIsNoCoroutineThatTheReturnOnTheOtherStackResumes() {
    // Target: Renegade C860-C86B, C6EE: each music channel has its own stack (LD SP,(C7A0); CALL C132; LD (C7A0),SP; LD SP,(C487)), then the player returns
    ignoresMemory(0x8F00, 0x9200);
    translate(
        at(0x8000, 0x31, 0x00, 0x90, 0x21, 0x00, 0x91, 0x22, 0x02, 0x91, 0xCD, 0x20, 0x80, 0xCD, 0x20, 0x80, 0x76, 0x18, 0xFD),
        at(0x8020, 0xED, 0x73, 0x04, 0x91, 0xED, 0x7B, 0x02, 0x91, 0xCD, 0x40, 0x80, 0xED, 0x73, 0x02, 0x91, 0xED, 0x7B, 0x04, 0x91, 0xC3, 0x50, 0x80),
        at(0x8040, 0x04, 0xC9),
        at(0x8050, 0x0C, 0xC9));
    Assert.assertTrue(stackAnalyzer.stackSwitches.toString(), stackAnalyzer.stackSwitches.isEmpty());
  }

  @Test
  public void aStackPointerSavedAndRepointedNearItsStackDropsNoReturns() {
    // Target: Renegade F474: LD (F6F6),SP; LD SP,HL with HL=C000, just above the stack, to POP a buffer into registers
    ignoresMemory(0x8F00, 0x9200);
    translate(
        at(0x8000, 0x31, 0x00, 0x90, 0xCD, 0x10, 0x80, 0x76, 0x18, 0xFD),
        at(0x8010, 0x21, 0x40, 0x90, 0xED, 0x73, 0x00, 0x91, 0xF9, 0xF1, 0xD1, 0xED, 0x7B, 0x00, 0x91, 0xC9),
        at(0x9040, 0x34, 0x12, 0x78, 0x56));
    Assert.assertEquals(Set.of(0x8003), Set.copyOf(stackAnalyzer.returnsConsumedBy.get(0x801E)));
    Assert.assertEquals(0x5678, endValue("DE"));
  }

  @Test
  public void aStackSwitchIsLearnedFromTheRecordingNotFromTheExplorationOfUntakenPaths() {
    // R-Type FAEF: LD (FBC9),SP; LD SP,F87A reads a table; the exploration reached an unrelated RET before the restore and took the pair for a coroutine
    ignoresMemory(0x9000, 0x9001);
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xED, 0x73, 0x00, 0x90, 0x31, 0x00, 0x91, 0xE1, 0x3A, 0x00, 0x92, 0xB7, 0x28, 0x02, 0xC9, 0x00, 0xED, 0x7B, 0x00, 0x90, 0xC9),
        at(0x9100, 0x34, 0x12, 0x03, 0x80));
    Assert.assertTrue(stackAnalyzer.stackSwitches.toString(), stackAnalyzer.stackSwitches.isEmpty());
  }

  @Test
  public void aTwoBytePopOfTheReturnAddressContinuesAfterBothBytes() {
    // R-Type FA08: POP IX takes the return address, PUSHes write a table through the stack, JP (IX) goes back; the byte after the pop's prefix is not an instruction
    ignoresMemory(0x9000, 0x9001);
    translate(
        at(0x8000, 0xED, 0x73, 0x00, 0x90, 0x31, 0x00, 0x91, 0xCD, 0x20, 0x80, 0xED, 0x7B, 0x00, 0x90, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8020, 0xDD, 0xE1, 0x11, 0x34, 0x12, 0xD5, 0xDD, 0xE9));
    Assert.assertNull(routineManager.getInstructionAt(0x8021));
  }

  @Test
  public void aReturnThroughAStackPointerRestoredFromWhereItWasSavedLeavesTheRoutineNotTheStack() {
    // Zynaps 8846/8880: LD (8881),SP patches the operand of LD SP,nn at 8880; its RET drops the frames pushed since, within the same stack
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0xED, 0x73, 0x21, 0x80, 0xCD, 0x30, 0x80, 0x0E, 0x09, 0xC9),
        at(0x8020, 0x31, 0x00, 0x00, 0xC9),
        at(0x8030, 0x16, 0x02, 0xC3, 0x20, 0x80));
    Assert.assertTrue(stackAnalyzer.stackSwitches.isEmpty());
    Assert.assertEquals(RoutineManager.Exit.NON_LOCAL, routineManager.exitOf(0x8023, stackAnalyzer).kind());
  }

  @Test
  public void aReturnAfterPointingTheStackAtARecordThroughARegisterIsAJump() {
    // Zynaps 873B: LD SP,IY; RET continues at the code address stored in the record IY points to
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x0E, 0x02, 0xC3, 0x03, 0x80),
        at(0x8020, 0xFD, 0x21, 0x00, 0x90, 0xFD, 0xF9, 0xC9),
        at(0x9000, 0x10, 0x80));
    Assert.assertEquals(Set.of(0x8010), Set.copyOf(stackAnalyzer.dynamicInvocation.get(0x8026)));
  }

  @Test
  public void aContinuationPlantedBeforeAJumpThroughARegisterRunsAfterTheRoutineJumpedTo() {
    // Zynaps AAE5: LD DE,AAFF; PUSH DE; JP (HL); the handler's RET lands on AAFF
    translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8010, 0x21, 0x40, 0x80, 0x11, 0x30, 0x80, 0xD5, 0xE9),
        at(0x8030, 0x16, 0x03, 0xC9),
        at(0x8040, 0x0E, 0x02, 0xC9));
    Assert.assertEquals(Set.of(0x8016), Set.copyOf(stackAnalyzer.dataConsumedBy.get(0x8042)));
  }

  @Test
  public void aPatchThatTurnsOneInstructionIntoTwoShorterOnesReachesTheSecondOne() {
    // R-Type 73C5/73D8: BF24-BF25 alternate between LD A,02 and EX AF,AF'; LD (HL),A; the second byte is an instruction only in one mode
    fallsBackToTheEmulator = true;
    translate(
        at(0x8000, 0x21, 0x00, 0x90, 0xCD, 0x30, 0x80, 0xCD, 0x20, 0x80, 0xCD, 0x40, 0x80, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8020, 0x3E, 0x02, 0xC9),
        at(0x8030, 0x21, 0x20, 0x80, 0x36, 0x3E, 0x23, 0x36, 0x02, 0xC9),
        at(0x8040, 0x21, 0x20, 0x80, 0x36, 0x08, 0x23, 0x36, 0x77, 0xC9));
    Assert.assertNotNull(routineManager.getInstructionAt(0x8021));
  }

  @Test
  public void aJumpThroughARegisterIntoTheMiddleOfItsOwnRoutineGoesToThatLabel() {
    // ROM BEEPER 03C1: LD IX,03D1; ADD IX,BC; ... JP (IX) lands on 03D1..03D4 of the same loop; Dynamite Dan II calls it
    translate(
        at(0x8000, 0x01, 0x03, 0x00, 0xCD, 0x10, 0x03, 0x01, 0x00, 0x00, 0xCD, 0x10, 0x03, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x0310, 0xDD, 0x21, 0x20, 0x03, 0xDD, 0x09, 0x16, 0x02, 0xDD, 0xE9),
        at(0x0320, 0x00, 0x00, 0x00, 0x14, 0x15, 0x20, 0xFD, 0xC9));
    Assert.assertEquals("310 8000", routines());
  }

  @Test
  public void aReturnPatchedIntoANopFallsThroughToTheNextRoutine() {
    // Zynaps AAED: DI; RET of each handler becomes NOP; NOP to chain it with the next one
    translate(
        at(0x8000, 0x3E, 0xC9, 0x32, 0x23, 0x80, 0xCD, 0x20, 0x80, 0x3E, 0x00, 0x32, 0x23, 0x80, 0xCD, 0x20, 0x80, 0xCD, 0x24, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8020, 0x0C, 0x16, 0x02, 0xC9, 0x1E, 0x03, 0xC9));
    Assert.assertEquals(2, versions.instructionVersions(0x8023).size());
    Assert.assertEquals("8000 8020 8024", routines());
  }

  @Test
  public void aRoutineReachedThroughADispatchIsNotReenteredWhileItIsStillRunningAbove() {
    // Zynaps: A43E calls A321, whose dispatch had reached AD79; exploring from AD79 kept re-entering A321 to settle AD79's pending branch
    translate(
        at(0x8000, 0xCD, 0x20, 0x80, 0x06, 0x01, 0x76, 0x18, 0xFD),
        at(0x8020, 0xCD, 0x30, 0x80, 0xC9),
        at(0x8030, 0x3A, 0x70, 0x80, 0xFE, 0x01, 0x28, 0x01, 0xC9, 0x21, 0x40, 0x80, 0xE5, 0x21, 0x50, 0x80, 0xE9, 0xC9),
        at(0x8050, 0x3A, 0x71, 0x80, 0xFE, 0x01, 0x28, 0x08, 0x3E, 0x01, 0x32, 0x71, 0x80, 0xCD, 0x20, 0x80, 0xC9),
        at(0x8070, 0x01, 0x00));
    Assert.assertEquals(0, base.symbolicExecutionAdapter.abandonedExplorations);
  }
}
