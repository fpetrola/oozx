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

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

public class MemoryForOpcodes<T extends WordNumber> implements Memory<T> {
  public static <T1 extends WordNumber> T1 read16Bits(Memory<T1> memory, T1 address) {
    return Memory.read16Bits(memory, address);
  }

  public static <T1 extends WordNumber> void write16Bits(Memory<T1> memory, T1 value, T1 address) {
    Memory.write16Bits(memory, value, address);
  }

  public static <T1 extends WordNumber> void write16BitsR(Memory<T1> memory, T1 value, T1 address) {
    Memory.write16BitsR(memory, value, address);
  }

  @Override
  public T read(T address, int delta, int fetching) {
    return memory.read(address, delta, fetching);
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
  protected T[] cachedValues = (T[]) new WordNumber[0x10000];

  public MemoryForOpcodes(Memory<T> memory) {
    this.memory = memory;
  }

  public T read(T address, int fetching) {
    if (cachedValues[address.intValue()] != null)
      return cachedValues[address.intValue()];
    else {
      T value = memory.read(address, fetching);
      cachedValues[address.intValue()] = value;
      return value;
    }
  }

  public void write(T address, T value) {
    memory.write(address, value);
//    cachedValues[address.intValue()] = null;
  }

  public void reset() {
    cachedValues = (T[]) new WordNumber[0x10000];
  }
}
