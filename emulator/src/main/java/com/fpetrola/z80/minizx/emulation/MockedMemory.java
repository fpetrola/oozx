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

package com.fpetrola.z80.minizx.emulation;

import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class MockedMemory extends AbstractMemory {
  protected Integer[] data =  new Integer[0x10000];

  public MockedMemory(boolean canDisable1) {
    super();
  }

  public void init(Supplier<Integer[]> supplier) {
    data = supplier.get();
  }

  @Override
  protected int doRead(int address) {
    int i = address;
    return i >= 0 ? data[i] : 0;
  }

  @Override
  protected void doWrite(int address, int value) {
    data[address] = (value & 0xff) & 0xFFFF;
  }

  @Override
  public void reset() {
    for (int i = 0; i < data.length; i++) {
      doWrite(i, 0);
    }
//    Arrays.fill(data, WordNumber.createValue(0));
  }

  @Override
  public Integer[] getData() {
    return data;
  }
}
