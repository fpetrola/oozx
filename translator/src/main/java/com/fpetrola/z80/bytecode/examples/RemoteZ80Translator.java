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

package com.fpetrola.z80.bytecode.examples;

import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.cpu.RegistersSetter;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.minizx.emulation.DefaultEmulator;
import com.fpetrola.z80.minizx.emulation.EmulatedMiniZX;
import com.fpetrola.z80.opcodes.references.WordNumber;
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
import java.util.List;
import java.util.regex.Pattern;

import static java.net.URI.create;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class RemoteZ80Translator<T extends WordNumber> {
  private RealCodeBytecodeCreationBase<T> realCodeBytecodeCreationBase;

//  {
//    InstructionSpy registerTransformerInstructionSpy1 = new NullInstructionSpy();
//    State state1 = new State(new MockedIO(), new SpyRegisterBankFactory(registerTransformerInstructionSpy1).createBank(), registerTransformerInstructionSpy1.wrapMemory(new MockedMemory(true)));
//    final RegisterTransformerInstructionSpy registerTransformerInstructionSpy2 = new RegisterTransformerInstructionSpy(RealCodeBytecodeCreationBase.routineManager);
//    realCodeBytecodeCreationBase = new RealCodeBytecodeCreationBase<T>(registerTransformerInstructionSpy2, new RoutineManager(), state1, new SpyInstructionExecutor(registerTransformerInstructionSpy2));
//  }

  public static void main(String[] args) {
    RemoteZ80Translator<WordNumber> remoteZ80Translator = new RemoteZ80Translator<>();

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
      startRoutineAddress = Integer.parseInt(args[3]);
      if (args.length > 4)
        emulateUntil = Integer.parseInt(args[4]);
    }

    System.out.println("\n\nTranslating: " + gameName + " " + url + " " + startRoutineAddress + "\n\n");

    remoteZ80Translator.translate(action, gameName, url, startRoutineAddress, screenURL, emulateUntil);
  }

  public static <T extends WordNumber> String emulateUntil(RealCodeBytecodeCreationBase<T> realCodeBytecodeCreationBase, int address, String url) {
    EmulatedMiniZX emulatedMiniZX = new EmulatedMiniZX( url, 1, false, address, false,new DefaultEmulator());
    emulatedMiniZX.start();

    State state = emulatedMiniZX.ooz80.getState();
    String base64Memory = SnapshotHelper.getBase64Memory(state);
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
      State<T> state = realCodeBytecodeCreationBase.getState();
      SnapshotLoader.setupStateWithSnapshot(getDefaultRegistersSetter(), tempFile.getAbsolutePath(), state);
      firstAddress = realCodeBytecodeCreationBase.getState().getPc().read().intValue();
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
      return String.valueOf(Integer.parseInt(group, 16));
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

  public RegistersSetter<T> getDefaultRegistersSetter() {
    return realCodeBytecodeCreationBase.getRegistersSetter();
  }
}