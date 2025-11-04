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

public class MemoryForOpcodes implements Memory {
  private int counter;

  public static int read16Bits(Memory memory, int address) {
    return Memory.read16Bits(memory, address);
  }

  @Override
  public int read(int address, int fetching) {
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
  public void addMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
    memory.addMemoryWriteListener(memoryWriteListener);
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
    memory.removeMemoryWriteListener(memoryWriteListener);
  }

  @Override
  public void addMemoryReadListener(MemoryReadListener memoryReadListener) {
    memory.addMemoryReadListener(memoryReadListener);
  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener memoryReadListener) {
    memory.removeMemoryReadListener(memoryReadListener);
  }

  @Override
  public int[] getData() {
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
  public void copyFrom(Memory memory) {
    this.memory.copyFrom(memory);
  }

  private final Memory memory;
  private final State state;
  protected Integer[] cachedData = new Integer[0x10000];
  protected int[] cachedAddresses = new int[0x100];

  public MemoryForOpcodes(Memory memory, State state) {
    this.memory = memory;
    this.state = state;
  }

  private int read1(int address, int fetching) {
    int i = address;
    if (cachedData[i] != null) {
      return cachedData[i];
    } else {
      int value = memory.read(address, fetching);
      if (memory.isReadListenersDisabled())
        return value;
      cachedData[i] = value;
      cachedAddresses[counter++] = i;
      return value;
    }
  }

  public void write(int address, int value) {
    memory.write(address, value);
//    cachedValues[address.intValue()] = null;
  }

  public void reset() {
    while (counter > 0) {
      cachedData[cachedAddresses[--counter]] = null;
    }
  }
}
