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

package com.fpetrola.z80.bytecode.generators.helpers;

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import org.cojen.maker.ClassMaker;
import org.cojen.maker.MethodMaker;

import java.util.HashMap;
import java.util.Map;

public class BytecodeGenerationContext {
  public RoutineManager routineManager;
  public boolean optimize16Convertion = false;
  public ClassMaker cm;
  public Map<String, MethodMaker> methods;
  public Register<WordNumber> pc;
  public SymbolicExecutionAdapter symbolicExecutionAdapter;
  public boolean syncEnabled;

  public BytecodeGenerationContext(RoutineManager routineManager, ClassMaker classMaker, Register<?> pc1, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    this.routineManager = routineManager;
    this.cm = classMaker;
    this.pc = (Register<WordNumber>) pc1;
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
    this.methods = new HashMap<>();
    this.syncEnabled = true;
  }
}