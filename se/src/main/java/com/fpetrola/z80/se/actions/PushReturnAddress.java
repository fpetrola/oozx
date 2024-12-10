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

package com.fpetrola.z80.se.actions;

import com.fpetrola.z80.instructions.impl.Push;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.OpcodeReference;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.se.SymbolicExecutionAdapter;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class PushReturnAddress<T extends WordNumber> extends Push<T> {
  private final SymbolicExecutionAdapter symbolicExecutionAdapter;

  public PushReturnAddress(SymbolicExecutionAdapter symbolicExecutionAdapter, OpcodeReference target, Register<T> sp, Memory<T> memory) {
    super(target, sp, memory);
    this.symbolicExecutionAdapter = symbolicExecutionAdapter;
  }

  public int execute() {
    doPush(createValue(target.read().intValue()), sp, memory);
    symbolicExecutionAdapter.checkNextSP();

    return 5 + cyclesCost;
  }

  protected String getName() {
    return "Push_";
  }
}
