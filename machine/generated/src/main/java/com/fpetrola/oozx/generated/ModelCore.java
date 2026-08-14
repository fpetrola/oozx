/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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
package com.fpetrola.oozx.generated;

import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OopCore;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.RegisterBank;
import com.fpetrola.z80.registers.UnrolledRegisterBank;

/**
 * The core of a machine built to be read by the generator, not to run: the bank the generated core
 * will be, and the contention counted inside, so that the memory the Z80 builds is the one the
 * generated core inlines.
 */
public class ModelCore extends OopCore {
  public RegisterBank bank(Memory memory, IO io) {
    return new UnrolledRegisterBank();
  }

  public boolean countsItsOwnContention() {
    return true;
  }
}
