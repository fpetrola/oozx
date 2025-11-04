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

import java.util.Arrays;

public class BaseInstructionLoopTest extends TwoZ80Driver {
  public BaseInstructionLoopTest(IDriverConfigurator driverConfigurator) {
    super(driverConfigurator);
  }


  @Override
  protected void setUpMemory() {
    initMem(() -> {
      int[] data = new int[0x10000];
      Arrays.fill(data, 0);
      int base = 3592 * 4;
      base = 14368;
      data[base] = 16;
      data[base + 1] = 8;
      data[base + 2] = 4;
      data[base + 3] = 2;
      data[0] = 1;
      data[1] = 10;
      data[2] = 20;
      data[3] = 30;
      data[0xFFFF] = 1;
      data[1000] = 123;
      data[100] = 0;
      data[101] = 1;
      data[102] = 2;
      data[300] = 20;
      data[301] = 21;
      data[302] = 22;

      return data;
    });
  }
}
