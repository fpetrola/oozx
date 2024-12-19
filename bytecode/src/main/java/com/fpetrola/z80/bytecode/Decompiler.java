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

package com.fpetrola.z80.bytecode;import com.fpetrola.z80.bytecode.decompile.SimpleBytecodeProvider;
import com.fpetrola.z80.bytecode.decompile.SimpleResultSaverFor;
import com.hypherionmc.jarmanager.JarManager;
import org.apache.commons.io.FileUtils;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import soot.Main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;

public class Decompiler {
  private SimpleResultSaverFor saver;
  private HashMap<String, Object> customProperties;
  private Fernflower fernflower;
  private SimpleBytecodeProvider bytecodeProvider;

  public Decompiler() {
    saver = new SimpleResultSaverFor();
    customProperties = createCustomProperties();
    bytecodeProvider = new SimpleBytecodeProvider();
    fernflower = new Fernflower(bytecodeProvider, saver, customProperties, new PrintStreamLogger(new PrintStream(new ByteArrayOutputStream())));
  }

  private static byte[] optimize(String className, String targetFolder, File source, byte[] bytecode) throws IOException {
    String path = Decompiler.class.getProtectionDomain().getCodeSource().getLocation().getPath();

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

  private static HashMap<String, Object> createCustomProperties() {
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

  public String decompile() {
    fernflower.decompileContext();
    return saver.getContent();
  }

  public void addClass(byte[] bytecode, File source) {
    bytecodeProvider.addBytecode(source.getAbsolutePath(), bytecode);
    fernflower.getStructContext().addSpace(source, true);
  }
}
