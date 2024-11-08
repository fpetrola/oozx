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

package com.fpetrola.z80.instructions.base;

import com.fpetrola.z80.bytecode.BytecodeGeneration;
import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import com.fpetrola.z80.routines.RoutineManager;

import java.util.List;

public abstract class ManualBytecodeGenerationTest<T extends WordNumber> extends TransformInstructionsTest<T> implements BytecodeGeneration {
  public String generateAndDecompile() {
    return generateAndDecompile("", RealCodeBytecodeCreationBase.getRoutines(), ".", "JetSetWilly");
  }

  @Override
  public String generateAndDecompile(String base64Memory, List<Routine> routines, String targetFolder, String className1) {
    SymbolicExecutionAdapter.mutantAddress.clear();
    return getDecompiledSource(currentContext.pc(),
        (address) -> currentContext.getTransformedInstructionAt(address),
        "JSW", base64Memory, routines, ".");
  }

  @Override
  public RoutineManager getRoutineManager() {
    return new RoutineManager();
  }
}
