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

import com.fpetrola.z80.bytecode.decompile.SimpleBytecodeProvider;
import com.fpetrola.z80.bytecode.decompile.SimpleResultSaverFor;
import com.fpetrola.z80.bytecode.generators.StateBytecodeGenerator;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.minizx.MiniZX;
import com.fpetrola.z80.minizx.SpectrumApplication;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.hypherionmc.jarmanager.JarManager;
import org.apache.commons.io.FileUtils;
import org.cojen.maker.ClassMaker2;
import org.cojen.maker.MethodMaker;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import soot.Main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public interface BytecodeGeneration {
  default <T extends WordNumber> String getDecompiledSource(String className, String targetFolder, State state, boolean translation) {
    try {
      StateBytecodeGenerator bytecodeGenerator = getBytecodeGenerator(className, state, translation);
      byte[] bytecode = bytecodeGenerator.getBytecode();
      String classFile = className + ".class";
      File source = new File(targetFolder + "/" + classFile);
      FileUtils.writeByteArrayToFile(source, bytecode);

//      bytecode = optimize(className, "target/translation/", source, bytecode);
      return decompile(bytecode, source);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] optimize(String className, String targetFolder, File source, byte[] bytecode) throws IOException {
    String path = BytecodeGeneration.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    int i1 = path.indexOf("nested:");
    boolean isExecutingFatJar = i1 != -1;
    if (isExecutingFatJar) {
      int i = path.indexOf(".jar");
      path = path.substring(i1 + "nested:".length(), i + ".jar".length());
      JarManager.getInstance().unpackJar(new File(path), new File("target/jar-content"));
    }

    if (!targetFolder.equals(".")) {
      FileUtils.writeByteArrayToFile(source, bytecode);
      String s = isExecutingFatJar ? "target/jar-content/BOOT-INF/classes/rt.jar:target/jar-content/BOOT-INF/classes:" : "target/classes/rt.jar:target/classes:";
      String pack = "com.fpetrola.z80.minizx.";
      pack = "";
      String[] args = {"-via-shimple", "-allow-phantom-refs", "-d", targetFolder, "-cp", s + targetFolder, "-O", pack + className};
      Main.main(args);
      bytecode = InterpreterUtil.getBytes(source);
    }

//    Scene scene = Scene.v();
//
//    SootClass sootClass = scene.loadClassAndSupport(className);
//    sootClass.setApplicationClass();
//
//    // Convertir la clase a formato intermedio (Jimple)
//    for (SootMethod method : sootClass.getMethods()) {
//      if (!method.isConcrete()) continue;
//
//      // Obtener la representación en Jimple del método
//      Body body = method.getActiveBody();
//
//      // Generar el CFG usando UnitGraph
//      UnitGraph cfg = new BriefUnitGraph(body);
//
//      // Mostrar información sobre el CFG
//      System.out.println("CFG for method: " + method.getName());
//      for (Unit unit : cfg) {
//        System.out.println(unit);
//      }
//    }
    return bytecode;
  }

  RoutineManager getRoutineManager();

  private void createMainMethod(ClassMaker2 classMaker) {
    MethodMaker mainMethod = classMaker.addMethod(void.class, "main", String[].class);
    mainMethod.public_();
//    Variable jetSetWilly = mainMethod.new_("JetSetWilly");
//    jetSetWilly.invoke("$34762");
//    mainMethod.return_();
  }

  default String decompile(byte[] bytecode, File source) {
    SimpleResultSaverFor saver = new SimpleResultSaverFor();
    HashMap<String, Object> customProperties = createCustomProperties();

    Fernflower fernflower = new Fernflower(new SimpleBytecodeProvider(bytecode), saver, customProperties, new PrintStreamLogger(new PrintStream(new ByteArrayOutputStream())));
    fernflower.getStructContext().addSpace(source, true);
    fernflower.decompileContext();
    return saver.getContent();
  }

  private HashMap<String, Object> createCustomProperties() {
    HashMap<String, Object> customProperties = new HashMap<>();
    customProperties.put("lit", "1");
    customProperties.put("asc", "1");


//    customProperties.put("rbr", "0");
//    customProperties.put("rsy", "0");
//    customProperties.put("bto", "0");
//
////    customProperties.put("nns", "0");
////    customProperties.put("uto", "1");
////    customProperties.put("ump", "1");
////
//    customProperties.put("rer", "0");
//
//    customProperties.put("inn", "0");
////    customProperties.put("bsm", "0");
////    customProperties.put("iib", "0");
////    customProperties.put("iec", "1");
////    customProperties.put("log", IFernflowerLogger.Severity.TRACE.name());
////    customProperties.put("mpm", "0");
////    customProperties.put("ind", "   ");
////    customProperties.put("ban", "");
////    customProperties.put("__unit_test_mode__", "0");
////    customProperties.put("__dump_original_lines__", "1");
////    customProperties.put("jvn", "1");
////    customProperties.put("sef", "0");
////    customProperties.put("dcl", "1");

    return customProperties;
  }

  String generateAndDecompile();

  String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className1);

  default void translateToJava(String className, String startMethod, State state, boolean translation) {
    try {
      boolean useFields = true;
      StateBytecodeGenerator bytecodeGenerator = getBytecodeGenerator(className, state, translation);
      Class<?> finish = bytecodeGenerator.getNewClass();
      Object o = finish.getConstructors()[0].newInstance();
      if (useFields) {
        Method method = o.getClass().getMethod(startMethod);
        method.invoke(o);
      } else {
        Method method = o.getClass().getMethod(startMethod, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class);
        method.invoke(o, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
      }
      writeClassFile(className, state, translation);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private StateBytecodeGenerator getBytecodeGenerator(String className, State state, boolean translation) {
    return new StateBytecodeGenerator(className, this.getRoutineManager(), state, translation, MiniZX.class, SpectrumApplication.class);
  }

  private void writeClassFile(String className, State state, boolean translation) throws IOException {
    StateBytecodeGenerator bytecodeGenerator = getBytecodeGenerator(className, state, translation);
    byte[] bytecode = bytecodeGenerator.getBytecode();
    String classFile = className + ".class";
    File source = new File(classFile);
    FileUtils.writeByteArrayToFile(source, bytecode);
  }
}
