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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.bytecode.generators.StateBytecodeGenerator;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.minizx.emulation.GameData;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public interface BytecodeGeneration {
  Pattern UNDECOMPILED_METHOD = Pattern.compile("(?:void|int) \\$([0-9A-F]+)\\(\\) \\{\\s*// \\$FF: Couldn't be decompiled");

  default String getDecompiledSource(String className, String targetFolder, State state, boolean translation, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory, GameData gameData) {
    for (int attempt = 0; ; attempt++) {
      StateBytecodeGenerator bytecodeGenerator = getBytecodeGenerator(className, state, translation, symbolicExecutionAdapter, base64Memory, gameData);
      Decompiler decompiler = new Decompiler();
      bytecodeGenerator.getBytecode().forEach((key, value) -> decompiler.addClass(value, createFile(key, targetFolder, value)));
      if (Boolean.getBoolean("translation.skipDecompile"))
        return "";
      String source = decompiler.decompile();
      List<Integer> undecompiled = UNDECOMPILED_METHOD.matcher(source).results().map(m -> Integer.parseInt(m.group(1), 16)).toList();
      if (undecompiled.isEmpty() || attempt == 5 || !undecompiled.stream().allMatch(bytecodeGenerator::splitRoutineAt))
        return source.replace("// $FF: Couldn't be decompiled", "throw new IllegalStateException(\"not decompiled\");");
    }
  }

  private File createFile(String className, String targetFolder, byte[] bytecode) {
    try {
      String classFile = className + ".class";
      File source = new File(targetFolder + "/" + classFile);
      FileUtils.writeByteArrayToFile(source, bytecode);
      return source;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  RoutineManager getRoutineManager();

  String generateAndDecompile();

  String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className1, SymbolicExecutionAdapter symbolicExecutionAdapter);

  default MiniZX translatedProgram(String className, State state, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory, GameData gameData) {
    return translatedProgram(className, getBytecodeGenerator(className, state, true, symbolicExecutionAdapter, base64Memory, gameData).getBytecode().get(className));
  }

  static MiniZX translatedProgram(String className, byte[] bytecode) {
    try {
      return (MiniZX) new ClassLoader(MiniZX.class.getClassLoader()) {
        Class<?> define() {
          return defineClass(className, bytecode, 0, bytecode.length);
        }
      }.define().getConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private StateBytecodeGenerator getBytecodeGenerator(String className, State state, boolean translation, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory, GameData gameData) {
    return new StateBytecodeGenerator(className, this.getRoutineManager(), state, translation, MiniZX.class, SpectrumApplication.class, symbolicExecutionAdapter, base64Memory, gameData);
  }
}
