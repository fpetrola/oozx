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

import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.HashMap;
import java.util.Map;

public final class MemorySpy<T extends WordNumber> implements Memory<T> {
  private Memory<T> memory;

  public Map<Integer, Integer> map = new HashMap<Integer, Integer>();

  public MemorySpy() {
  }

  public MemorySpy(Memory<T> memory) {
    this.memory = memory;
  }

  @Override
  public T[] getData() {
    return memory.getData();
  }

  public void write(T address, T value) {
    int key = address.intValue() & 0xFFFF;
    Integer times = map.get(key);
    if (times != null)
      times = times + 1;
    else
      times = 0;

    map.put(key, times);
    memory.write(address, value);
  }

  public T read(T address, int fetching) {
    T value = memory.read(address, 0);
    return value;
  }

  public int getAddressModificationsCounter(int address) {
    Integer integer = map.get(address & 0xFFFF);
    return integer != null ? integer : 0;
  }

  public boolean compare() {
    return memory.compare();
  }

  @Override
  public void update() {
    memory.update();
  }

  @Override
  public void addMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
    memory.addMemoryWriteListener(memoryWriteListener);
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
    memory.removeMemoryWriteListener(memoryWriteListener);
  }

  public void setMemory(Memory memory) {
    this.memory = memory;
  }

  public Memory getMemory() {
    return memory;
  }

  @Override
  public void reset() {
    memory.reset();
  }

  @Override
  public void addMemoryReadListener(MemoryReadListener memoryReadListener) {
    memory.addMemoryReadListener(memoryReadListener);
  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener memoryReadListener) {
    memory.removeMemoryReadListener(memoryReadListener);
  }
}