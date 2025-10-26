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

import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.function.Supplier;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

@SuppressWarnings("unchecked")
public class MockedMemory<T extends WordNumber> extends AbstractMemory<T> {
  protected T[] data = (T[]) new WordNumber[0x10000];

  public MockedMemory(boolean canDisable1) {
    super();
  }

  public void init(Supplier<T[]> supplier) {
    data = supplier.get();
  }

  @Override
  protected T doRead(T address) {
    int i = address.intValue();
    return i >= 0 ? data[i] : createValue(0);
  }

  @Override
  protected void doWrite(int address, T value) {
    data[address] = value;
  }

  @Override
  public void reset() {
    for (int i = 0; i < data.length; i++) {
      doWrite(i, createValue(0));
    }
//    Arrays.fill(data, WordNumber.createValue(0));
  }

  @Override
  public T[] getData() {
    return data;
  }
}
