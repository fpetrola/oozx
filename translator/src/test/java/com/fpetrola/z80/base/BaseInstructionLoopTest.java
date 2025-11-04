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

package com.fpetrola.z80.base;

import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.Arrays;

public class BaseInstructionLoopTest<T extends WordNumber> extends TwoZ80Driver<T> {
  public BaseInstructionLoopTest(IDriverConfigurator<T> driverConfigurator) {
    super(driverConfigurator);
  }


  @Override
  protected void setUpMemory() {
    initMem(() -> {
      WordNumber[] data = new WordNumber[0x10000];
      Arrays.fill(data, (Object) new WordNumber(0));
      int base = 3592 * 4;
      base = 14368;
      data[base] = (WordNumber) new WordNumber(16);
      data[base + 1] = (WordNumber) new WordNumber(8);
      data[base + 2] = (WordNumber) new WordNumber(4);
      data[base + 3] = (WordNumber) new WordNumber(2);
      data[0] = (WordNumber) new WordNumber(1);
      data[1] = (WordNumber) new WordNumber(10);
      data[2] = (WordNumber) new WordNumber(20);
      data[3] = (WordNumber) new WordNumber(30);
      data[0xFFFF] = (WordNumber) new WordNumber(1);
      data[1000] = (WordNumber) new WordNumber(123);
      data[100] = (WordNumber) new WordNumber(0);
      data[101] = (WordNumber) new WordNumber(1);
      data[102] = (WordNumber) new WordNumber(2);
      data[300] = (WordNumber) new WordNumber(20);
      data[301] = (WordNumber) new WordNumber(21);
      data[302] = (WordNumber) new WordNumber(22);

      return (T[]) data;
    });
  }
}
