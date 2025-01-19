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

package com.fpetrola.z80.bytecode.generators;

import com.fpetrola.z80.bytecode.generators.helpers.BytecodeGenerationContext;
import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.ClassMaker2;
import org.cojen.maker.MethodMaker;

import java.util.*;

public class StateBytecodeGenerator {
  private final String className;
  private final RoutineManager routineManager;
  private final State state;
  private final boolean translation;
  private final Class<?> translationSuperClass;
  private final Class<?> executionSuperClass;
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;
  private final String base64Memory;
  private Map<String, byte[]> bytecodes = new HashMap<>();

  public StateBytecodeGenerator(String className, RoutineManager routineManager, State state, boolean translation, Class<?> translationSuperClass, Class<?> executionSuperClass, SymbolicExecutionAdapter symbolicExecutionAdapter, String base64Memory) {
    this.className = className;
    this.routineManager = routineManager;
    this.state = state;
    this.translation = translation;
    this.translationSuperClass = translationSuperClass;
    this.executionSuperClass = executionSuperClass;
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    this.base64Memory = base64Memory;
  }

  private ClassMaker translate() {
    ClassLoader classLoader = StateBytecodeGenerator.class.getClassLoader();
    if (!translation)
      classLoader = ClassLoader.getSystemClassLoader();
//    ClassMaker classMaker = ClassMaker.begin(className, classLoader).public_();

    ClassMaker classMaker = ClassMaker2.beginExternal(className, classLoader).public_();
    if (translation) {
      classMaker.extend(translationSuperClass);
    } else {
      classMaker.extend(executionSuperClass);
    }

    MethodMaker methodMaker = classMaker.addConstructor().public_();
    methodMaker.invokeSuperConstructor();

    //createMainMethod(classMaker);

    if (translation) {
      MethodMaker getProgramBytesMaker = classMaker.addMethod(String.class, "getProgramBytes").public_();
      getProgramBytesMaker.return_(base64Memory);
    }

    BytecodeGenerationContext bytecodeGenerationContext = new BytecodeGenerationContext(routineManager, classMaker, state.getPc(), symbolicExecutionAdapter);
    List<Routine> routines = routineManager.getRoutinesInDepth();

    RoutineBytecodeGenerator routineBytecodeGenerator1 = new RoutineBytecodeGenerator(bytecodeGenerationContext, null);
    routineBytecodeGenerator1.createMethod(0);

    routines.sort(Comparator.comparingInt(Routine::getEntryPoint));

    routines.forEach(routine -> {
      routine.optimize();
      RoutineBytecodeGenerator routineBytecodeGenerator = new RoutineBytecodeGenerator(bytecodeGenerationContext, routine);
      routineBytecodeGenerator.getMethod(routine.getEntryPoint());
    });

    routines.forEach(routine -> {
      System.out.println(routine);
      RoutineBytecodeGenerator routineBytecodeGenerator = new RoutineBytecodeGenerator(bytecodeGenerationContext, routine);
      routineBytecodeGenerator.generate();
    });

    return classMaker;
  }

  public Map<String, byte[]> getBytecode() {
    ClassMaker translate = translate();
    byte[] bytes = translate.finishBytes();
    bytecodes.put(className, bytes);
    return bytecodes;
  }

  public List<Class<?>> getNewClass() {
    ClassMaker translate = translate();
    Class<?> finish1 = translate.finish();
    return Arrays.asList(finish1);
  }
}
