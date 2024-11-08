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

import soot.*;
import soot.jimple.Stmt;
import soot.options.Options;

import java.util.Iterator;
import java.util.Map;

import static java.io.File.pathSeparator;

public class TestSoot extends BodyTransformer {
  public static void main(String[] args) {
    String mainclass = "JetSetWilly";

    //set classpath
    String bootpath = System.getProperty("sun.boot.class.path");
    String javapath = System.getProperty("java.class.path");
    String jceDir = "/home/fernando/.sdkman/candidates/java/8.0.282-open/jre" + "/lib/rt.jar";
    String path = jceDir + pathSeparator + javapath + pathSeparator + "/home/fernando/detodo/desarrollo/m/zx/zx/zx3/soot-demo-1/inputs" + pathSeparator + "/home/fernando/detodo/desarrollo/m/zx/zx/zx3/ZX3D/target/classes";
    Scene.v().setSootClassPath(path);

    //add an intra-procedural analysis phase to Soot
    TestSoot analysis = new TestSoot();
    PackManager.v().getPack("jtp").add(new Transform("jtp.TestSoot", analysis));

    //load and set main class
    Options.v().set_app(true);
    SootClass appclass = Scene.v().loadClassAndSupport(mainclass);
   // Scene.v().setMainClass(appclass);
    Scene.v().loadNecessaryClasses();

    //start working
    PackManager.v().runPacks();
  }

  @Override
  protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
    Iterator<Unit> it = b.getUnits().snapshotIterator();
    while (it.hasNext()) {
      Stmt stmt = (Stmt) it.next();
      System.out.println(stmt);
    }
  }
}