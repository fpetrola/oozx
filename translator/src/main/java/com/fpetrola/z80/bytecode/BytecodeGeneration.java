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

package com.fpetrola.z80.bytecode;

import com.fpetrola.z80.bytecode.generators.StateBytecodeGenerator;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import org.apache.commons.io.FileUtils;
import org.cojen.maker.ClassMaker2;
import org.cojen.maker.MethodMaker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public interface BytecodeGeneration {
  default <T extends WordNumber> String getDecompiledSource(String className, String targetFolder, State state, boolean translation, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory) {
    try {
      StateBytecodeGenerator bytecodeGenerator = getBytecodeGenerator(className, state, translation, symbolicExecutionAdapter, base64Memory);
      Map<String, byte[]> bytecode = bytecodeGenerator.getBytecode();

      Decompiler decompiler = new Decompiler();

      bytecode.forEach((key, value) -> {
        File source = createFile(key, targetFolder, value);
        //      bytecode = optimize(className, "target/translation/", source, bytecode);
        decompiler.addClass(value, source);
      });

      return decompiler.decompile();
    } catch (Exception e) {
      throw new RuntimeException(e);
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

  private void createMainMethod(ClassMaker2 classMaker) {
    MethodMaker mainMethod = classMaker.addMethod(void.class, "main", String[].class);
    mainMethod.public_();
//    Variable jetSetWilly = mainMethod.new_("JetSetWilly");
//    jetSetWilly.invoke("$34762");
//    mainMethod.return_();
  }


  String generateAndDecompile();

  String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className1, SymbolicExecutionAdapter symbolicExecutionAdapter);

  default void translateToJava(String className, String startMethod, State state, boolean translation, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory) {
    try {
      boolean useFields = true;
      StateBytecodeGenerator bytecodeGenerator = getBytecodeGenerator(className, state, translation, symbolicExecutionAdapter, base64Memory);
      Class<?> finish = bytecodeGenerator.getNewClass().get(0);
      Object o = finish.getConstructors()[0].newInstance();
      if (useFields) {
        Method method = o.getClass().getMethod(startMethod);
        method.invoke(o);
      } else {
        Method method = o.getClass().getMethod(startMethod, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
        method.invoke(o, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
      }
      writeClassFile(className, state, translation, symbolicExecutionAdapter, base64Memory);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private StateBytecodeGenerator getBytecodeGenerator(String className, State state, boolean translation, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory) {
    return new StateBytecodeGenerator(className, this.getRoutineManager(), state, translation, MiniZX.class, SpectrumApplication.class, symbolicExecutionAdapter, base64Memory);
  }

  private void writeClassFile(String className, State state, boolean translation, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory) throws IOException {
    StateBytecodeGenerator bytecodeGenerator = getBytecodeGenerator(className, state, translation, symbolicExecutionAdapter, base64Memory);
    byte[] bytecode = bytecodeGenerator.getBytecode().get(0);
    String classFile = className + ".class";
    File source = new File(classFile);
    FileUtils.writeByteArrayToFile(source, bytecode);
  }
}
