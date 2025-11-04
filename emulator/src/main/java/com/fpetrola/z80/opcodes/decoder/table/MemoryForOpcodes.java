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

package com.fpetrola.z80.opcodes.decoder.table;

import com.fpetrola.z80.cpu.State;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class MemoryForOpcodes<T extends WordNumber> implements Memory<T> {
  private int counter;

  public static <T1 extends WordNumber> T1 read16Bits(Memory<T1> memory, T1 address) {
    return Memory.read16Bits(memory, address);
  }

  @Override
  public T read(T address, int fetching) {
    return read1(address, fetching);
  }

  @Override
  public boolean compare() {
    return memory.compare();
  }

  @Override
  public void update() {
    memory.update();
  }

  @Override
  public void addMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener) {
    memory.addMemoryWriteListener(memoryWriteListener);
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener) {
    memory.removeMemoryWriteListener(memoryWriteListener);
  }

  @Override
  public void addMemoryReadListener(MemoryReadListener<T> memoryReadListener) {
    memory.addMemoryReadListener(memoryReadListener);
  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener<T> memoryReadListener) {
    memory.removeMemoryReadListener(memoryReadListener);
  }

  @Override
  public T[] getData() {
    return memory.getData();
  }

  @Override
  public void disableReadListener() {
    memory.disableReadListener();
  }

  @Override
  public void enableReadListener() {
    memory.enableReadListener();
  }

  @Override
  public void disableWriteListener() {
    memory.disableWriteListener();
  }

  @Override
  public void enableWriteListener() {
    memory.enableWriteListener();
  }

  @Override
  public void canDisable(boolean canDisable) {
    memory.canDisable(canDisable);
  }

  @Override
  public boolean canDisable() {
    return memory.canDisable();
  }

  @Override
  public void copyFrom(Memory<T> memory) {
    this.memory.copyFrom(memory);
  }

  private final Memory<T> memory;
  private final State<T> state;
  protected WordNumber[] cachedData = new WordNumber[0x10000];
  protected int[] cachedAddresses = new int[0x100];

  public MemoryForOpcodes(Memory<T> memory, State<T> state) {
    this.memory = memory;
    this.state = state;
  }

  private T read1(T address, int fetching) {
    int i = address.value;
    if (cachedData[i] != null) {
      return (T) cachedData[i];
    } else {
      T value = memory.read(address, fetching);
      if (memory.isReadListenersDisabled())
        return value;
      cachedData[i] = value;
      cachedAddresses[counter++] = i;
      return value;
    }
  }

  public void write(T address, T value) {
    memory.write(address, value);
//    cachedValues[address.intValue()] = null;
  }

  public void reset() {
    while (counter > 0) {
      cachedData[cachedAddresses[--counter]] = null;
    }
  }
}
