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

package com.fpetrola.z80.minizx.emulation.finders;

import com.fpetrola.z80.cpu.OOZ80;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.minizx.emulation.GameData;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class VariableRangeFinder<T extends WordNumber> {
  public static final int SCREEN_START_ADDRESS = 16384;
  public static final int ATTRIBUTES_START_ADDRESS = 22528;
  public static final int ATTRIBUTES_END_ADDRESS = 23296;
  private final OOZ80<T> ooz80;
  private final GameData gameData;

  public VariableRangeFinder(OOZ80<T> ooz80, GameData gameData) {
    this.ooz80 = ooz80;
    this.gameData = gameData;
  }

  public void init() {
    Memory<T> memory = ooz80.getState().getMemory();

    memory.addMemoryWriteListener(((address, value) -> {
      int addressValue = address.intValue();
      if (addressValue >= ATTRIBUTES_END_ADDRESS && addressValue >= 32768) {
        gameData.processValueOfAddress(addressValue, value.intValue(), ooz80.getState().getTicks());
      }
      return value;
    }));
  }

}
