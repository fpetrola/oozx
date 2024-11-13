/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

import java.util.Arrays;

public class MemoryForOpcodes implements Memory {
  private int counter;

  public static int read16Bits(Memory memory, int address) {
    return memory.read16Bits(address);
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
  public void copyFrom(Memory memory) {
    this.memory.copyFrom(memory);
  }

  private final Memory memory;
  protected final int[] cachedData = new int[0x10000];
  /**
   * Journal de las direcciones cacheadas desde el ultimo reset(), para poder
   * invalidar solo esas. Arrancaba en 4 (el largo maximo de una instruccion Z80)
   * asumiendo un reset por instruccion, pero la generacion de bytecode recorre
   * muchas instrucciones sin resetear, asi que crece segun haga falta.
   */
  protected int[] cachedAddresses = new int[8];

  public MemoryForOpcodes(Memory memory, State state) {
    this.memory = memory;
    Arrays.fill(cachedData, -1);
    Arrays.fill(cachedAddresses, 0);
  }

  private int read1(final int address, final int fetching) {
    if (cachedData[address] != -1) {
      return cachedData[address];
    } else {
      int value = memory.read(address, fetching);
      cachedData[address] = value;
      if (counter == cachedAddresses.length)
        cachedAddresses = Arrays.copyOf(cachedAddresses, cachedAddresses.length * 2);
      cachedAddresses[counter++] = address;
      return value;
    }
  }

  public void write(final int address, final int value) {
    memory.write(address, value);
//    cachedValues[address.intValue()] = null;
  }

  public void reset() {
    while (counter > 0) {
      cachedData[cachedAddresses[--counter]] = -1;
    }
  }
}
