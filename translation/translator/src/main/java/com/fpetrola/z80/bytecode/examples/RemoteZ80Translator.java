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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
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

  public record Footprint(Map<Integer, int[]> codeBytes, Map<Integer, int[]> explored, Set<Integer> modifiedCode, StackAnalyzer learned, Set<Integer> returnAddressesOnStack, int[] finalMemory) {
    public Map<Integer, Integer> executed() {
      Map<Integer, Integer> lengths = new HashMap<>();
      code().forEach((address, bytes) -> lengths.put(address, bytes.length));
      return lengths;
    }

    private Map<Integer, int[]> code() {
      Set<Integer> recordedInteriors = new HashSet<>();
      codeBytes.forEach((address, bytes) -> {
        for (int i = 1; i < bytes.length; i++)
          recordedInteriors.add(address + i & 0xffff);
      });
      Map<Integer, int[]> code = new HashMap<>(codeBytes);
      explored.forEach((address, bytes) -> {
        if (!recordedInteriors.contains(address) && java.util.stream.IntStream.range(1, bytes.length).noneMatch(i -> codeBytes.containsKey(address + i & 0xffff)))
          code.putIfAbsent(address, bytes);
      });
      return code;
    }

    public static Footprint combine(List<Footprint> footprints) {
      Map<Integer, int[]> codeBytes = new HashMap<>(), explored = new HashMap<>();
      Set<Integer> modifiedCode = new HashSet<>(), returnAddressesOnStack = new HashSet<>();
      StackAnalyzer learned = new StackAnalyzer(null);
      footprints.forEach(footprint -> {
        footprint.codeBytes.forEach(codeBytes::putIfAbsent);
        footprint.explored.forEach(explored::putIfAbsent);
        modifiedCode.addAll(footprint.modifiedCode);
        returnAddressesOnStack.addAll(footprint.returnAddressesOnStack);
        learned.learnFrom(footprint.learned);
      });
      return new Footprint(codeBytes, explored, modifiedCode, learned, returnAddressesOnStack, footprints.get(footprints.size() - 1).finalMemory);
    }

    public void install(Memory memory, int stackPointer) {
      for (int address = 0x4000; address < 0x10000; address++)
        if (address < stackPointer || address >= stackPointer + 128)
          memory.write(address, finalMemory[address]);
      code().forEach((address, bytes) -> {
        for (int i = 0; i < bytes.length; i++) {
          int at = address + i & 0xffff;
          memory.write(at, bytes[i]);
          if (!modifiedCode.contains(at))
            memory.protect(at, at + 1);
        }
      });
    }
  }

  public static Footprint footprint(String rzxFile, int from) {
    Map<Integer, int[]> codeBytes = new HashMap<>();
    Set<Integer> modifiedCode = new HashSet<>(), returnAddressesOnStack = new HashSet<>();
    boolean[] started = {false};
    StackAnalyzer stackAnalyzer = new StackAnalyzer(null);
    EmulatedMiniZX[] emulator = {null};
    Map<Integer, int[]> explored = new HashMap<>();
    Forks forks = new Forks(codeBytes, new HashSet<>(), explored, new HashMap<>(), new HashSet<>());
    ConditionalInstruction<?>[] pending = {null};
    int[] pendingAddress = {-1};
    try {
      emulator[0] = EmulatedMiniZX.ofRecording(rzxFile, -1, stackAnalyzer).listening(new FetchListener() {
        public void instructionFetchedAt(int address, Instruction instruction) {
          boolean starting = !started[0] && address == from;
          started[0] |= starting;
          StackAnalyzer.collecting = started[0];
          if (!started[0])
            return;
          if (pending[0] != null)
            forks.exploreUntakenBranch(emulator[0].ooz80, pending[0], pendingAddress[0], address, stackAnalyzer, BRANCH_BUDGET);
          if (pendingAddress[0] != -1 && address != (pendingAddress[0] + codeBytes.get(pendingAddress[0]).length & 0xffff))
            forks.landings().add(address);
          boolean conditional = isUntakenBranchCandidate(instruction);
          pending[0] = conditional ? (ConditionalInstruction<?>) instruction : null;
          pendingAddress[0] = address;
          int[] memory = emulator[0].ooz80.getState().getMemory().getData();
          for (int slot = emulator[0].ooz80.getState().getRegisterSP().read(); starting && slot < 0x10000 - 1 && returnAddressesOnStack.size() < 10; slot += 2)
            returnAddressesOnStack.add(memory[slot] | memory[slot + 1] << 8);
          int[] bytes = new int[instruction.getLength()];
          for (int i = 0; i < bytes.length; i++)
            bytes[i] = memory[address + i & 0xffff];
          int[] seen = codeBytes.putIfAbsent(address, bytes);
          for (int i = 0; seen != null && i < Math.max(seen.length, bytes.length); i++)
            if (i >= seen.length || i >= bytes.length || seen[i] != bytes[i])
              modifiedCode.add(address + i & 0xffff);
          if (instruction instanceof Call && (modifiedCode.contains(address + 1 & 0xffff) || modifiedCode.contains(address + 2 & 0xffff))) {
            stackAnalyzer.calledThrough.put(address, seen[1] | seen[2] << 8);
            stackAnalyzer.calledThrough.put(address, bytes[1] | bytes[2] << 8);
          }
        }
      });
      emulator[0].start();
    } catch (RuntimeException finished) {
      if (!"rzx finished".equals(finished.getMessage()))
        throw finished;
    }
    StackAnalyzer.collecting = false;
    returnAddressesOnStack.retainAll(codeBytes.keySet());
    return new Footprint(codeBytes, explored, modifiedCode, stackAnalyzer, returnAddressesOnStack, emulator[0].ooz80.getState().getMemory().getData().clone());
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
          if (step > 0 && (depth == 0 || depth >= 0x8000) && (codeBytes.containsKey(pc) || known.contains(pc)))
            break;
          if (pc == 0 || pc >= 0x4000 && pc < 0x5B00 && !codeBytes.containsKey(pc))
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
          if (executed instanceof JP jump && jump.getPositionOpcodeReference() instanceof Register && codeBytes.containsKey(forkState.getPc().read()) && !landings.contains(forkState.getPc().read()))
            return;
          if (executed instanceof Halt || executed instanceof Ret && (forkState.getRegisterSP().read() - startSp & 0xffff) > 0 && (forkState.getRegisterSP().read() - startSp & 0xffff) < 0x8000)
            break;
        }
      } catch (RuntimeException deadEnd) {
      }
      unfinished.removeAll(own);
      learned.learnFrom(analyzer);
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