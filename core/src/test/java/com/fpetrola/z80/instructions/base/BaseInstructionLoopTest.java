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

import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.Arrays;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class BaseInstructionLoopTest<T extends WordNumber> extends TwoZ80Test<T> {
  @Override
  protected void setUpMemory() {
    initMem(() -> {
      WordNumber[] data = new WordNumber[0x10000];
      Arrays.fill(data, createValue(0));
      int base = 3592 * 4;
      base = 14368;
      data[base] = createValue(16);
      data[base + 1] = createValue(8);
      data[base + 2] = createValue(4);
      data[base + 3] = createValue(2);
      data[0] = createValue(1);
      data[1] = createValue(10);
      data[2] = createValue(20);
      data[3] = createValue(30);
      data[0xFFFF] = createValue(1);
      data[1000] = createValue(123);
      data[100] = createValue(0);
      data[101] = createValue(1);
      data[102] = createValue(2);
      data[300] = createValue(20);
      data[301] = createValue(21);
      data[302] = createValue(22);

      return (T[]) data;
    });
  }
}
