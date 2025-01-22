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

public class MockedMemory<T extends WordNumber> extends DefaultMemory<T> {
  protected T[] cachedValues = (T[]) new WordNumber[0x10000];

  public MockedMemory(boolean canDisable1) {
    super(canDisable1);
  }

  public void init(Supplier<T[]> supplier) {
    data = supplier.get();
  }

  public T read(T address, int fetching) {
    T value = doRead(address);
    T cachedValue = null;
    //FIXME: para que????

    boolean b = fetching == 1 || address.intValue() < 0 || (cachedValue = cachedValues[address.intValue()]) != value;
    b= true;
    if (memoryReadListener != null && b) {
      memoryReadListener.forEach(l -> l.readingMemoryAt(address, value, 0, fetching));
      if (address.intValue() >= 0)
        cachedValues[address.intValue()] = value;
    }

    return value;
  }
}
