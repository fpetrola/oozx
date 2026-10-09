package com.fpetrola.z80.instructions.tests;

import com.fpetrola.z80.bytecode.BytecodeGeneration;
import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.routines.CodeVersions;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.bytecode.examples.RemoteZ80Translator;
import com.fpetrola.z80.helpers.Helper;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.minizx.MiniZXSound;
import com.fpetrola.z80.tstates.UncontendedTiming;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.registers.RegisterName;
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
  private int interruptEvery, start = START;
  private boolean banked;
  private boolean fallsBackToTheEmulator;
  private RoutineManager routineManager;
  private StackAnalyzer stackAnalyzer;
  private CodeVersions versions;

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

  private static int[] memoryOf(int[]... chunks) {
    int[] memory = new int[0x10000];
    for (int[] chunk : chunks)
      for (int i = 1; i < chunk.length; i++)
        memory[chunk[0] + i - 1] = chunk[i];
    return memory;
  }

  private String translateWithBlock(int[] block, int[]... chunks) {
    int[] memory = memoryOf(chunks);
    String image = RemoteZ80Translator.emulateProgram(base, memory, start, STACK);
    base.exploreRecording(RemoteZ80Translator.footprint(stackAnalyzer -> machine(EmulatedMiniZX.ofProgram(memory, start, STACK, 1000, stackAnalyzer).interruptingEvery(interruptEvery)), start), start);
    stackAnalyzer = base.getStackAnalyzer();
    routineManager = base.getRoutineManager();
    versions = stackAnalyzer.codeVersions;
    if (block != null) {
      RemoteZ80Translator.recordBlockContents(EmulatedMiniZX.ofProgram(memory, start, STACK, 1000, null), start, versions);
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
    EmulatedMiniZX emulator = machine(EmulatedMiniZX.ofProgram(memory, start, STACK, 0, null).timed());
    emulator.start();
    State z80 = emulator.ooz80.getState(), translated = EmulatedMiniZX.createOOZ80(new DefaultMiniZXIO()).getState();
    program.loadState(z80);
    int[] previous = {-1}, steps = {0}, nextInterrupt = {interruptEvery};
    program.setInterruptionCondition(fetches -> {
      if (previous[0] >= 0) {
        emulator.ooz80.execute();
        for (int k = 0; k < 0x10000 && z80.getPc().read() == previous[0] && program.PC != previous[0]; k++)
          emulator.ooz80.execute();
      }
      program.storeRegisters(translated);
      translated.getPc().write(program.PC);
      if (!registers(z80).equals(registers(translated)))
        throw new IllegalStateException("after $%04X\n z80  %s\n java %s".formatted(previous[0], registers(z80), registers(translated)));
      if ((program.tstates & 0xFFFFF) != z80.clock.getTStates())
        throw new IllegalStateException("after $%04X: T-states java=%d z80=%d".formatted(previous[0], program.tstates & 0xFFFFF, z80.clock.getTStates()));
      if (z80.getMemory().getData()[z80.getPc().read()] == 0x76 || ++steps[0] == 1000)
        throw new Finished();
      previous[0] = program.PC;
      if (interruptEvery > 0 && fetches >= nextInterrupt[0] && program.acceptsInterrupt()) {
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
    Assert.assertEquals("the recorded path falls back to the emulator", fallsBackToTheEmulator, mutantExecutorOf(program) != null);
    int[] expected = z80.getMemory().getData();
    Assert.assertEquals("", IntStream.range(0, 0x10000).filter(a -> (a < STACK - 0x100 || a >= STACK + 0x10) && !ignoredMemory.contains(a) && program.mem[a] != expected[a]).limit(8).mapToObj(a -> "%04X z80 %02X java %02X ".formatted(a, expected[a], program.mem[a])).collect(Collectors.joining()));
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

  private static String registers(State state) {
    return "PC=%04X".formatted(state.getPc().read()) + Stream.of("AF", "BC", "DE", "HL", "AFx", "BCx", "DEx", "HLx", "IX", "IY", "R")
        .map(name -> " %s=%04X".formatted(name, state.getRegister(RegisterName.valueOf(name)).read() & (name.startsWith("AF") ? 0xffd7 : 0xffff)))
        .collect(Collectors.joining());
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
  }

  @Test
  public void resettingTheStackReturnsTwoLevels() {
    // Dizzy F877: LD SP,nn; RET
    String java = translate(
        at(0x8000, 0xCD, 0x10, 0x80, 0x76),
        at(0x8010, 0xCD, 0x18, 0x80, 0xC9),
        at(0x8018, 0x31, 0xFE, 0xFE, 0xC9));
    Assert.assertEquals(Set.of(0x8003), Set.copyOf(routineManager.nonLocalReturns.get(0x801B)));
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
    Assert.assertFalse(routineManager.nonLocalReturns.containsKey(0x8032));
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
    Assert.assertTrue(base.symbolicExecutionAdapter.getMutantAddress().contains(0x8021));
  }

  @Test
  public void anOperandPatchedByAStoreThatOnlyAForkRanIsReadFromMemory() {
    // Equinox 7C08: its writers LD (7C09),A at 7D06 and 7E26 never ran in the recording
    String java = translate(
        at(0x8000, 0xAF, 0x20, 0x0D, 0x76, 0x18, 0xFD),
        at(0x8010, 0x3E, 0x04, 0x32, 0x21, 0x80, 0xC3, 0x20, 0x80),
        at(0x8020, 0x0E, 0x01, 0x76, 0x18, 0xFD));
    Assert.assertTrue(base.symbolicExecutionAdapter.getMutantAddress().contains(0x8021));
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
  public void port7FFDPagesTheBankAtC000ForTheTranslatedProgramToo() {
    // Renegade 128K pages bank 1 in for its music and bank 0 back: what is read and written at C000 depends on the bank mapped
    banked = true;
    translate(
        at(0x8000, 0x3A, 0x00, 0xC0, 0x01, 0xFD, 0x7F, 0x3E, 0x11, 0xED, 0x79, 0x3A, 0x00, 0xC0, 0x3E, 0x55, 0x32, 0x00, 0xC0, 0x3E, 0x10, 0xED, 0x79,
            0x3A, 0x00, 0xC0, 0x47, 0x3E, 0x11, 0xED, 0x79, 0x3A, 0x00, 0xC0, 0x4F, 0x3E, 0x10, 0xED, 0x79, 0x76, 0x18, 0xFD),
        at(0xC000, 0x42));
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
  }

  @Test
  public void anOutThroughCGoesToThePortInBCWhateverAHolds() {
    // Renegade 128K 9FF3: OUT (C),A with BC=7FFD; only OUT (n),A puts A on the high byte of the port
    banked = true;
    translate(
        at(0x8000, 0x01, 0xFD, 0x7F, 0x3E, 0x91, 0xED, 0x79, 0x3A, 0x00, 0xC0, 0x76, 0x18, 0xFD),
        at(0xC000, 0x42));
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
    Assert.assertTrue(base.symbolicExecutionAdapter.getMutantAddress().contains(0x8021));
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
    Assert.assertTrue(stackAnalyzer.getSimulatedCallsPcs().contains(0x8007));
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
    Assert.assertTrue(routineManager.nonLocalReturns.toString(), routineManager.nonLocalReturns.containsKey(0x8023));
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
