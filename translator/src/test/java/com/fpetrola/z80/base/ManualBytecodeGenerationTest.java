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

package com.fpetrola.z80.base;

import com.fpetrola.z80.bytecode.BytecodeGeneration;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;

import java.util.List;

public abstract class ManualBytecodeGenerationTest<T extends WordNumber> extends TransformInstructionsTest<T> implements BytecodeGeneration {
  public ManualBytecodeGenerationTest(DriverConfigurator<T> tDriverConfigurator) {
    super(tDriverConfigurator);
  }

  public String generateAndDecompile() {
    return generateAndDecompile("", getRoutineManager().getRoutines(), ".", "JetSetWilly", ((DriverConfigurator)driverConfigurator).symbolicExecutionAdapter);
  }

  @Override
  public String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className1, SymbolicExecutionAdapter symbolicExecutionAdapter) {
    return getDecompiledSource("JSW", ".", currentContext.getState(), !base64Memory.isBlank(), symbolicExecutionAdapter, base64Memory);
  }

  @Override
  public RoutineManager getRoutineManager() {
    return driverConfigurator.getRoutineManager();
  }
}
