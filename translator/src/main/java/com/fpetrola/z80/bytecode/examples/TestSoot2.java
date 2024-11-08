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
import soot.baf.toolkits.base.LoadStoreOptimizer;
import soot.jimple.Stmt;
import soot.options.Options;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class TestSoot2 extends BodyTransformer {

  public static void main(String[] args) {

    String classesDir = "/home/fernando/detodo/desarrollo/m/zx/zx/zx3/ZX3D/target/classes";
    String mainClass = "com.fpetrola.z80.bytecode.tests.JetSetWilly";
    String javapath = System.getProperty("java.class.path");
    String[] split = javapath.split(":");
    Options.v().set_prepend_classpath(true);

    //ConstantInitializerToTagTransformer constantInitializerToTagTransformer = new ConstantInitializerToTagTransformer();

    Transformer constantInitializerToTagTransformer = LoadStoreOptimizer.v();
    PackManager.v().getPack("jtp").add(new Transform("jtp.TestSoot", constantInitializerToTagTransformer));

    ArrayList<String> objects = new ArrayList<>();
    objects.add(classesDir);
    objects.addAll(Arrays.asList(split));
    Options.v().set_process_dir(objects);
    Options.v().set_whole_program(false);
    Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
    Options.v().setPhaseOption("jj", "use-original-names:true");
    Options.v().set_keep_line_number(true);
    Options.v().set_allow_phantom_refs(true);

    SootClass appclass = Scene.v().loadClassAndSupport(mainClass);
    Scene.v().setMainClass(appclass);
    Scene.v().loadNecessaryClasses();

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