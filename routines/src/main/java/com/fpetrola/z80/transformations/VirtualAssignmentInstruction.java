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

package com.fpetrola.z80.transformations;

import com.fpetrola.z80.instructions.base.DummyInstruction;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.function.Supplier;

public class VirtualAssignmentInstruction<T extends WordNumber> extends DummyInstruction<T> {
  private final Virtual8BitsRegister register;
  private final Supplier<IVirtual8BitsRegister<T>> lastRegister;

  public VirtualAssignmentInstruction(Virtual8BitsRegister register, Supplier<IVirtual8BitsRegister<T>> lastRegister) {
    this.register = register;
    this.lastRegister = lastRegister;
  }

  public int execute() {
    IVirtual8BitsRegister<T> tVirtualRegister = lastRegister.get();
    register.write(tVirtualRegister.read());
    register.lastVersionRead= tVirtualRegister;
    return 0;
  }

  public Register getRegister() {
    return register;
  }

  public Supplier<IVirtual8BitsRegister<T>> getLastRegister() {
    return lastRegister;
  }

  public String toString() {
    return "virtual loading: " + register + " <- " + lastRegister.get().toString();
  }
}
