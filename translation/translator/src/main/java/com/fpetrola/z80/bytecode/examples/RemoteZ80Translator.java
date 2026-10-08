/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

package com.fpetrola.z80.bytecode.examples;

import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.cpu.RegistersSetter;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.emulation.helpers.snapshots.SnapshotLoader;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.routines.CodeVersions;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import io.korhner.asciimg.image.AsciiImgCache;
import io.korhner.asciimg.image.character_fit_strategy.StructuralSimilarityFitStrategy;
import io.korhner.asciimg.image.converter.AsciiToStringConverter;
import org.apache.commons.text.CaseUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fpetrola.z80.cpu.FetchListener;
import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.instructions.impl.Call;
import com.fpetrola.z80.instructions.impl.Halt;
import com.fpetrola.z80.instructions.impl.JP;
import com.fpetrola.z80.instructions.impl.Ret;
import com.fpetrola.z80.instructions.types.ConditionalInstruction;
import com.fpetrola.z80.minizx.DefaultMiniZXIO;
import com.fpetrola.z80.opcodes.references.ConditionAlwaysTrue;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.transformations.StackAnalyzer;
import com.fpetrola.z80.instructions.types.Instruction;
import com.fpetrola.z80.memory.Memory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static java.net.URI.create;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class RemoteZ80Translator {
  private RealCodeBytecodeCreationBase realCodeBytecodeCreationBase;

//  {
//    InstructionSpy registerTransformerInstructionSpy1 = new NullInstructionSpy();
//    State state1 = new State(new MockedIO(), new SpyRegisterBankFactory(registerTransformerInstructionSpy1).createBank(), registerTransformerInstructionSpy1.wrapMemory(new MockedMemory(true)));
//    final RegisterTransformerInstructionSpy registerTransformerInstructionSpy2 = new RegisterTransformerInstructionSpy(RealCodeBytecodeCreationBase.routineManager);
//    realCodeBytecodeCreationBase = new RealCodeBytecodeCreationBase(registerTransformerInstructionSpy2, new RoutineManager(), state1, new SpyInstructionExecutor(registerTransformerInstructionSpy2));
//  }

  public static void main(String[] args) {
    RemoteZ80Translator remoteZ80Translator = new RemoteZ80Translator();

    String action = "translate";
    String gameName = "jetsetwilly";
    String url = "http://torinak.com/qaop/bin/" + gameName;
    int startRoutineAddress = 34762;
    String screenURL = "https://tcrf.net/images/3/3a/Jet_Set_Willy-ZX_Spectrum-title.png";
    int emulateUntil = -1;


    if (args.length >= 4) {
      action = args[0];
      gameName = args[1];
      url = args[2];
      startRoutineAddress = java.lang.Integer.parseInt(args[3]);
      if (args.length > 4)
        emulateUntil = java.lang.Integer.parseInt(args[4]);
    }

    System.out.println("\n\nTranslating: " + gameName + " " + url + " " + startRoutineAddress + "\n\n");

    remoteZ80Translator.translate(action, gameName, url, startRoutineAddress, screenURL, emulateUntil);
  }

  public static String emulateUntil(RealCodeBytecodeCreationBase realCodeBytecodeCreationBase, int emulateUntil, String url) {
    return emulate(realCodeBytecodeCreationBase, new EmulatedMiniZX(url, 1, false, emulateUntil, false, realCodeBytecodeCreationBase.getStackAnalyzer()));
  }

  public static String emulateRecording(RealCodeBytecodeCreationBase realCodeBytecodeCreationBase, String rzxFile, int frames) {
    return emulate(realCodeBytecodeCreationBase, EmulatedMiniZX.ofRecording(rzxFile, frames, realCodeBytecodeCreationBase.getStackAnalyzer()));
  }

  public record Footprint(Map<Integer, int[]> codeBytes, Map<Integer, int[]> explored, StackAnalyzer learned, Set<Integer> returnAddressesOnStack, int[] finalMemory, CodeVersions versions, Set<Integer> romEntries) implements java.io.Serializable {
    public Set<Integer> modifiedCode() {
      return versions.modifiedBytes();
    }

    public Map<Integer, Integer> executed() {
      Map<Integer, Integer> lengths = new HashMap<>();
      code().forEach((address, bytes) -> lengths.put(address, span(address, bytes)));
      return lengths;
    }

    private Map<Integer, int[]> code() {
      Set<Integer> recordedInteriors = interiors(codeBytes);
      Map<Integer, int[]> code = new HashMap<>(codeBytes);
      explored.forEach((address, bytes) -> {
        if (!recordedInteriors.contains(address) && java.util.stream.IntStream.range(1, bytes.length).noneMatch(i -> codeBytes.containsKey(address + i & 0xffff)))
          code.putIfAbsent(address, bytes);
      });
      return code;
    }

    private Set<Integer> interiors(Map<Integer, int[]> instructions) {
      Set<Integer> interiors = new HashSet<>();
      instructions.forEach((address, bytes) -> {
        for (int i = 1; i < span(address, bytes); i++)
          interiors.add(address + i & 0xffff);
      });
      interiors.removeAll(codeBytes.keySet());
      return interiors;
    }

    private int span(int address, int[] bytes) {
      int data = learned.callContinuations.getOrDefault(address, address + bytes.length) - address - bytes.length & 0xffff;
      return data < 256 && java.util.stream.IntStream.range(bytes.length, bytes.length + data).noneMatch(i -> codeBytes.containsKey(address + i & 0xffff)) ? bytes.length + data : bytes.length;
    }

    private Footprint forgettingJumpsIntoData() {
      Set<Integer> interiors = interiors(code());
      learned.dynamicInvocation.entries().stream().filter(jump -> interiors.contains(jump.getValue())).toList().forEach(jump -> learned.dynamicInvocation.removeMapping(jump.getKey(), jump.getValue()));
      return this;
    }

    public static Footprint combine(List<Footprint> footprints) {
      Map<Integer, int[]> codeBytes = new HashMap<>(), explored = new HashMap<>();
      Set<Integer> returnAddressesOnStack = new HashSet<>(), romEntries = new HashSet<>();
      StackAnalyzer learned = new StackAnalyzer(null);
      CodeVersions versions = new CodeVersions();
      footprints.forEach(footprint -> {
        footprint.codeBytes.forEach((address, bytes) -> recordVersion(codeBytes, versions, address, bytes));
        versions.addAll(footprint.versions);
        footprint.explored.forEach(explored::putIfAbsent);
        returnAddressesOnStack.addAll(footprint.returnAddressesOnStack);
        romEntries.addAll(footprint.romEntries);
        learned.learnFrom(footprint.learned);
      });
      learned.codeVersions = versions;
      return new Footprint(codeBytes, explored, learned, returnAddressesOnStack, footprints.get(footprints.size() - 1).finalMemory, versions, romEntries);
    }

    public void install(Memory memory, int stackPointer) {
      for (int address = 0x4000; address < 0x10000; address++)
        if (address < stackPointer || address >= stackPointer + 128)
          memory.write(address, finalMemory[address]);
      Set<Integer> modified = modifiedCode();
      code().forEach((address, bytes) -> {
        for (int i = 0; i < bytes.length; i++) {
          int at = address + i & 0xffff;
          memory.write(at, bytes[i]);
          if (!modified.contains(at))
            memory.protect(at, at + 1);
        }
      });
    }
  }

  public static Footprint footprint(String rzxFile, int from) {
    try {
      Path saved = Path.of("target", "footprints", footprintKey(rzxFile, from) + ".ser");
      if (Files.exists(saved))
        try (java.io.ObjectInputStream in = new java.io.ObjectInputStream(new java.io.BufferedInputStream(Files.newInputStream(saved)))) {
          return (Footprint) in.readObject();
        }
      Footprint footprint = footprint(stackAnalyzer -> EmulatedMiniZX.ofRecording(rzxFile, -1, stackAnalyzer), from);
      Files.createDirectories(saved.getParent());
      try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(new java.io.BufferedOutputStream(Files.newOutputStream(saved)))) {
        out.writeObject(footprint);
      }
      return footprint;
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static String footprintKey(String rzxFile, int from) throws IOException {
    java.security.MessageDigest digest = sha256();
    digest.update(Files.readAllBytes(Path.of(rzxFile)));
    digest.update(Integer.toString(from).getBytes());
    for (Class<?> type : List.of(com.fpetrola.z80.cpu.OOZ80.class, StackAnalyzer.class, RemoteZ80Translator.class))
      digestCode(digest, Path.of(type.getProtectionDomain().getCodeSource().getLocation().getPath()), type == RemoteZ80Translator.class);
    return java.util.HexFormat.of().formatHex(digest.digest());
  }

  private static void digestCode(java.security.MessageDigest digest, Path location, boolean onlyTheFootprint) throws IOException {
    java.util.function.Predicate<String> part = name -> name.endsWith(".class") && (!onlyTheFootprint || name.contains("RemoteZ80Translator") || name.contains("/minizx/"));
    if (Files.isDirectory(location))
      try (java.util.stream.Stream<Path> files = Files.walk(location)) {
        for (Path file : files.filter(f -> part.test(f.toString())).sorted().toList())
          digest.update(Files.readAllBytes(file));
      }
    else
      try (java.util.jar.JarFile jar = new java.util.jar.JarFile(location.toFile())) {
        for (java.util.jar.JarEntry entry : jar.stream().filter(e -> part.test(e.getName())).sorted(java.util.Comparator.comparing(java.util.jar.JarEntry::getName)).toList())
          digest.update(jar.getInputStream(entry).readAllBytes());
      }
  }

  private static java.security.MessageDigest sha256() {
    try {
      return java.security.MessageDigest.getInstance("SHA-256");
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static String emulateProgram(RealCodeBytecodeCreationBase realCodeBytecodeCreationBase, int[] memory, int entry, int stack) {
    return emulate(realCodeBytecodeCreationBase, EmulatedMiniZX.ofProgram(memory, entry, stack, 0, null));
  }

  public static Footprint footprint(java.util.function.Function<StackAnalyzer, EmulatedMiniZX> emulatorFor, int from) {
    Map<Integer, int[]> codeBytes = new HashMap<>();
    Set<Integer> returnAddressesOnStack = new HashSet<>();
    CodeVersions versions = new CodeVersions();
    boolean[] started = {false};
    StackAnalyzer stackAnalyzer = new StackAnalyzer(null);
    stackAnalyzer.codeVersions = versions;
    EmulatedMiniZX[] emulator = {null};
    Map<Integer, int[]> explored = new HashMap<>();
    Forks forks = new Forks(codeBytes, new HashSet<>(), explored, new HashMap<>(), new HashSet<>());
    ConditionalInstruction<?>[] pending = {null};
    int[] pendingAddress = {-1};
    Set<Integer> patched = new HashSet<>(), romEntries = new HashSet<>();
    Instruction[] previous = {null};
    emulator[0] = emulatorFor.apply(stackAnalyzer).listening(new FetchListener() {
      public void instructionFetchedAt(int address, Instruction instruction) {
        boolean starting = !started[0] && address == from;
        started[0] |= starting;
        StackAnalyzer.collecting = started[0];
        if (!started[0])
          return;
        if (pending[0] != null)
          forks.exploreUntakenBranch(emulator[0].ooz80, pending[0], pendingAddress[0], address, stackAnalyzer, BRANCH_BUDGET);
        if (pendingAddress[0] != -1 && address != (pendingAddress[0] + codeBytes.get(pendingAddress[0]).length & 0xffff)) {
          forks.landings().add(address);
          if (address < 0x4000 && (pendingAddress[0] >= 0x4000 && !(previous[0] instanceof Ret) || previous[0] instanceof JP jump && jump.getPositionOpcodeReference() instanceof Register))
            romEntries.add(address);
        }
        previous[0] = instruction;
        boolean conditional = isUntakenBranchCandidate(instruction);
        pending[0] = conditional ? (ConditionalInstruction<?>) instruction : null;
        pendingAddress[0] = address;
        int[] memory = emulator[0].ooz80.getState().getMemory().getData();
        for (int slot = emulator[0].ooz80.getState().getRegisterSP().read(); starting && slot < 0x10000 - 1 && returnAddressesOnStack.size() < 10; slot += 2)
          returnAddressesOnStack.add(memory[slot] | memory[slot + 1] << 8);
        int[] bytes = new int[instruction.getLength()];
        for (int i = 0; i < bytes.length; i++)
          bytes[i] = memory[address + i & 0xffff];
        recordVersion(codeBytes, versions, address, bytes);
        patched.addAll(CodeVersions.fixedStoreTargets(address, instruction, memory));
      }
    });
    play(emulator[0]);
    StackAnalyzer.collecting = false;
    versions.patched(patched, codeBytes);
    returnAddressesOnStack.retainAll(codeBytes.keySet());
    return new Footprint(codeBytes, explored, stackAnalyzer, returnAddressesOnStack, emulator[0].ooz80.getState().getMemory().getData().clone(), versions, romEntries).forgettingJumpsIntoData();
  }

  private static int[] recordVersion(Map<Integer, int[]> codeBytes, CodeVersions versions, int address, int[] bytes) {
    int[] seen = codeBytes.putIfAbsent(address, bytes);
    if (seen != null && !Arrays.equals(seen, bytes))
      versions.record(address, seen, bytes);
    return seen;
  }

  public static BiFunction<Integer, int[], Instruction> decoder() {
    OOZ80 decoder = EmulatedMiniZX.createOOZ80(new DefaultMiniZXIO());
    int[] memory = decoder.getState().getMemory().getData();
    return (address, bytes) -> {
      for (int i = 0; i < bytes.length; i++)
        memory[address + i & 0xffff] = bytes[i];
      decoder.getState().getPc().write(address);
      return decoder.getInstructionFetcher().fetchNextInstruction();
    };
  }

  public static void recordBlockContents(EmulatedMiniZX emulator, int from, CodeVersions versions) {
    boolean[] started = {false};
    emulator.listening(new FetchListener() {
      public void instructionFetchedAt(int address, Instruction instruction) {
        started[0] |= address == from;
        if (started[0])
          versions.recordBlockContent(address, emulator.ooz80.getState().getMemory().getData());
      }
    });
    play(emulator);
  }

  private static void play(EmulatedMiniZX emulator) {
    try {
      emulator.start();
    } catch (RuntimeException finished) {
      if (!"rzx finished".equals(finished.getMessage()))
        throw finished;
    }
  }

  private static final int BRANCH_BUDGET = 500;

  private record Forks(Map<Integer, int[]> codeBytes, Set<Integer> landings, Map<Integer, int[]> explored, Map<Integer, Integer> forked, Set<Integer> unfinished) {
    private void exploreUntakenBranch(OOZ80 main, ConditionalInstruction<?> branch, int site, int taken, StackAnalyzer learned, int budget) {
      State state = main.getState();
      int fallThrough = site + branch.getLength() & 0xffff, sp = state.getRegisterSP().read();
      int[] memory = state.getMemory().getData();
      int target = branch instanceof Ret ? state.getMemory().read16Bits(taken == fallThrough ? sp : sp - 2 & 0xffff)
          : branch.getLength() == 2 ? site + 2 + (byte) memory[site + 1 & 0xffff] & 0xffff : memory[site + 1 & 0xffff] | memory[site + 2 & 0xffff] << 8;
      int alternative = taken == fallThrough ? target : fallThrough;
      if (taken != fallThrough && taken != target || codeBytes.containsKey(alternative) || explored.containsKey(alternative) && !unfinished.contains(alternative) || forked.getOrDefault(site, 0) >= budget)
        return;
      forked.put(site, budget);
      OOZ80 fork = EmulatedMiniZX.createOOZ80(new DefaultMiniZXIO() {
        public int in(int port) {
          return 0xff;
        }
      });
      State forkState = fork.getState();
      System.arraycopy(state.getMemory().getData(), 0, forkState.getMemory().getData(), 0, 0x10000);
      forkState.takeFrom(state);
      if (branch instanceof Call)
        forkState.getRegisterSP().write(taken == fallThrough ? push(forkState, fallThrough) : sp + 2 & 0xffff);
      else if (branch instanceof Ret)
        forkState.getRegisterSP().write(taken == fallThrough ? sp + 2 & 0xffff : sp - 2 & 0xffff);
      forkState.getPc().write(alternative);
      StackAnalyzer analyzer = new StackAnalyzer(forkState);
      analyzer.knowsWholeStack = false;
      analyzer.addExecutionListener(fork.getInstructionExecutor());
      int startSp = forkState.getRegisterSP().read();
      Set<Integer> known = new HashSet<>(explored.keySet());
      known.removeAll(unfinished);
      Set<Integer> own = new HashSet<>();
      try {
        Instruction executed = null;
        for (int fresh = 0, step = 0; ; step++) {
          if (fresh >= budget && executed instanceof ConditionalInstruction || step == 100 * budget) {
            unfinished.addAll(own);
            own.clear();
            break;
          }
          int pc = forkState.getPc().read(), depth = startSp - forkState.getRegisterSP().read() & 0xffff;
          if (step > 0 && (depth == 0 && !analyzer.returnPoppedBelow(forkState.getRegisterSP().read()) || depth >= 0x8000) && (codeBytes.containsKey(pc) || known.contains(pc)))
            break;
          if (deadEnd(pc))
            break;
          if (own.add(pc) && !codeBytes.containsKey(pc) && !known.contains(pc))
            fresh++;
          int[] bytes = java.util.Arrays.copyOfRange(forkState.getMemory().getData(), pc, Math.min(pc + 4, 0x10000));
          executed = fork.execute(1);
          if (executed == null)
            break;
          explored.put(pc, java.util.Arrays.copyOf(bytes, executed.getLength()));
          if (budget > BRANCH_BUDGET / 32 && isUntakenBranchCandidate(executed))
            exploreUntakenBranch(fork, (ConditionalInstruction<?>) executed, pc, forkState.getPc().read(), analyzer, budget / 2);
          int landing = forkState.getPc().read();
          if (executed instanceof JP jump && jump.getPositionOpcodeReference() instanceof Register && (codeBytes.containsKey(landing) ? !landings.contains(landing) : deadEnd(landing)))
            return;
          if (executed instanceof Halt || executed instanceof Ret && (forkState.getRegisterSP().read() - startSp & 0xffff) > 0 && (forkState.getRegisterSP().read() - startSp & 0xffff) < 0x8000)
            break;
        }
      } catch (RuntimeException deadEnd) {
      }
      unfinished.removeAll(own);
      learned.learnFrom(analyzer);
    }

    private boolean deadEnd(int pc) {
      return pc == 0 || pc >= 0x4000 && pc < 0x5B00 && !codeBytes.containsKey(pc);
    }
  }

  private static boolean isUntakenBranchCandidate(Instruction instruction) {
    return instruction instanceof ConditionalInstruction<?> branch && !(branch.getCondition() instanceof ConditionAlwaysTrue) && (branch instanceof Ret || !(branch.getPositionOpcodeReference() instanceof Register));
  }

  private static int push(State state, int value) {
    int sp = state.getRegisterSP().read() - 2 & 0xffff;
    state.getMemory().write16Bits(value, sp);
    return sp;
  }

  public static String emulateRecordingUntil(RealCodeBytecodeCreationBase realCodeBytecodeCreationBase, String rzxFile, int address) {
    return emulate(realCodeBytecodeCreationBase, EmulatedMiniZX.ofRecording(rzxFile, -1, null).stoppingAt(address));
  }

  private static String emulate(RealCodeBytecodeCreationBase realCodeBytecodeCreationBase, EmulatedMiniZX emulatedMiniZX) {
    emulatedMiniZX.start();

    State state = emulatedMiniZX.ooz80.getState();
    String base64Memory = SnapshotHelper.getBase64Memory(state);
    realCodeBytecodeCreationBase.setProgramImage(((int[]) state.getMemory().getData()).clone());
    realCodeBytecodeCreationBase.getState().getMemory().copyFrom(state.getMemory());
    realCodeBytecodeCreationBase.getState().setRegisters(state);
    return base64Memory;
  }

  private void drawPicture(String url) {
    try {
      File input = getRemoteFile(url, "", "/tmp/" + "screen");

      AsciiImgCache cache = AsciiImgCache.create(new Font("Courier", Font.PLAIN, 2));
      BufferedImage portraitImage = ImageIO.read(input);
      AsciiToStringConverter stringConverter = new AsciiToStringConverter(cache, new StructuralSimilarityFitStrategy());
      System.out.println(stringConverter.convertImage(portraitImage));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void translate(String action, String gameName, String url, int startRoutineAddress, String screeenURL, int emulateUntil) {
    //  drawPicture(screeenURL);
    int firstAddress = startRoutineAddress;
    String base64Memory;
    if (emulateUntil > 0) {
      base64Memory = RemoteZ80Translator.emulateUntil(realCodeBytecodeCreationBase, emulateUntil, url);
    } else {
      File tempFile = getRemoteFile(url, ".z80", "/tmp/" + gameName + ".z80");
      State state = realCodeBytecodeCreationBase.getState();
      SnapshotLoader.setupStateWithSnapshot(getDefaultRegistersSetter(), tempFile.getAbsolutePath(), state);
      firstAddress = realCodeBytecodeCreationBase.getState().getPc().read();
      base64Memory = SnapshotHelper.getBase64Memory(realCodeBytecodeCreationBase.getState());
    }

    stepUntilComplete(firstAddress);

    List<Routine> routines = realCodeBytecodeCreationBase.getRoutines();
    String className = CaseUtils.toCamelCase(gameName, true);

    if (action.equals("translate")) {
      String targetFolder = "target/translation/";
      String sourceCode = generateAndDecompile(base64Memory, routines, targetFolder, className, realCodeBytecodeCreationBase.symbolicExecutionAdapter);

      try {
        String fileName = className + ".java";
        FileWriter fileWriter = new FileWriter(targetFolder + fileName);
        fileWriter.write(improveSource(sourceCode));
        fileWriter.close();
        System.out.println("\n\nWritting java source code to: " + fileName + "\n\n");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else
      translateToJava(gameName, base64Memory, "$" + startRoutineAddress);
  }

  public static String improveSource(String sourceCode) {
    sourceCode = sourceCode.replace("this.", "").replace("super.", "");
    sourceCode = sourceCode.replaceAll("\\(\\(.*\\)this\\).", "");
    sourceCode = StringReplacer.replace(sourceCode, Pattern.compile("('\\\\u([0-9a-f]{4})')"), m -> {
      String group = m.group(2);
      return String.valueOf(java.lang.Integer.parseInt(group, 16));
    });
    return sourceCode;
  }

  private File getRemoteFile(String url, String suffix, String pathname) {
    try {
      File tempFile = new File(pathname);
      // File tempFile = File.createTempFile("zx-" + gameName + "-", suffix);
      Path target = tempFile.toPath();
      Files.copy(create(url).toURL().openStream(), target, REPLACE_EXISTING);
      return tempFile;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void stepUntilComplete(int startAddress) {
    realCodeBytecodeCreationBase.stepUntilComplete(startAddress);
  }

  public String generateAndDecompile() {
    return realCodeBytecodeCreationBase.generateAndDecompile();
  }

  public String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    return realCodeBytecodeCreationBase.generateAndDecompile(base64Memory, routines, targetFolder, className, symbolicExecutionAdapter);
  }

  public void translateToJava(String className, String memoryInBase64, String startMethod) {
    realCodeBytecodeCreationBase.translateToJava(className, memoryInBase64, startMethod);
  }

  public RegistersSetter getDefaultRegistersSetter() {
    return realCodeBytecodeCreationBase.getRegistersSetter();
  }
}