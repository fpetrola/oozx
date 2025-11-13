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

package com.fpetrola.z80.cpu;

import com.fpetrola.z80.memory.Memory;

public class MemorySetter {
  private final Memory memory;
  private final byte[] rom;
  private final State state;

  public MemorySetter(Memory memory, byte[] rom, State state) {
    this.memory = memory;
    this.rom = rom;
    this.state = state;
  }

  public void setData(byte[] result) {
    memory.disableWriteListener();
    for (int i = 0; i < result.length; i++) {
      int data = ((i < 16384) ? rom[i] : result[i]) & 0xff;
      int value = data;
      state.clock.addTStates(-state.clock.getTStates());
      memory.write(i, (value & 0xff) & 0xFFFF);
    }
    memory.enableWriteListener();
  }
}
