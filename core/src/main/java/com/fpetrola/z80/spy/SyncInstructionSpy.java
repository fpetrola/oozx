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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.cpu.Z80Cpu;
import com.fpetrola.z80.instructions.types.Instruction;

public class SyncInstructionSpy extends NullInstructionSpy {
  private OOZ80 secondZ80;

  @Override
  public void setSecondZ80(Z80Cpu secondZ80) {
    this.secondZ80 = (OOZ80) secondZ80;
  }

  @Override
  public void beforeExecution(Instruction opcode) {
    secondZ80.execute();
  }
}
