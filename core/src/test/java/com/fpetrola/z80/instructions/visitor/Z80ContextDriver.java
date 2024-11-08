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

package com.fpetrola.z80.instructions.visitor;

import com.fpetrola.z80.se.Z80InstructionDriver;
import com.fpetrola.z80.opcodes.references.*;
import com.fpetrola.z80.registers.Register;
import com.fpetrola.z80.registers.RegisterName;
import com.fpetrola.z80.registers.RegisterPair;

public interface Z80ContextDriver<T extends WordNumber> extends Z80InstructionDriver<T> {

  Register<T> r(RegisterName registerName);

  RegisterPair<T> rp(RegisterName registerName);

  Register<T> f();

  Register<T> pc();

  OpcodeReference iRR(Register<T> memoryReader);

  OpcodeReference iRRn(Register<T> register, int plus);

  ImmutableOpcodeReference c(int value);

  OpcodeReference iiRR(Register<T> memoryWriter);

  OpcodeReference iinn(int delta);

  Condition nz();
  BNotZeroCondition bnz();
  Condition z();

  Condition nc();
  Condition c();

  Condition t();

  ImmutableOpcodeReference nn(int delta);
}
