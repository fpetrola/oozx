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

package com.fpetrola.z80;

import com.fpetrola.z80.cpu.*;
import com.fpetrola.z80.memory.Memory;
import fuse.tstates.Contention;
import org.junit.jupiter.api.DisplayName;

/** Every ALU instruction against the documented Z80, on the generated core. */
@DisplayName("Every ALU instruction against the documented Z80, generated core")
public class GeneratedAluReferenceTest extends AluReferenceTest {
  protected Z80Cpu processor(IO io, Memory memory) {
    GeneratedZ80 core = new GeneratedZ80(memory, io) {
      public void contend(int address, int times, int tstates, Contention.Kind kind) {
      }
    };
    return new GeneratedZ80Cpu(new State(io, core, memory), core);
  }
}
