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

import com.fpetrola.z80.jspeccy.MemoryReadListener;
import com.fpetrola.z80.jspeccy.MemoryWriteListener;
import com.fpetrola.z80.mmu.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.function.Supplier;

public class MockedMemory<T extends WordNumber> implements Memory<T> {
  protected T[] data = (T[]) new WordNumber[0x100000];
  private MemoryWriteListener memoryWriteListener;
  private boolean readOnly;
  private MemoryReadListener memoryReadListener;
  private MemoryReadListener lastMemoryReadListener;
  private MemoryWriteListener lastMemoryWriteListener;

  public MockedMemory() {
  }

  public void init(Supplier<T[]> supplier) {
    data = supplier.get();
  }

  @Override
  public T read(T address) {
    if (memoryReadListener != null)
      memoryReadListener.readingMemoryAt(address, WordNumber.createValue(0));

    T datum = data[address.intValue()];
    if (datum == null)
      return WordNumber.createValue(0);
    else
      return datum.and(0xFF);
  }

  @Override
  public void write(T address, T value) {
    if (!readOnly) {
      if (memoryWriteListener != null)
        memoryWriteListener.writtingMemoryAt(address, value);
//      if (address.intValue() == 23548)
//        System.out.println("");
      data[address.intValue()] = value;
    }
  }

  @Override
  public boolean compare() {
    return false;
  }

  @Override
  public void update() {

  }

  @Override
  public void addMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
    this.memoryWriteListener = memoryWriteListener;
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
  }

  @Override
  public void reset() {

  }

  @Override
  public void addMemoryReadListener(MemoryReadListener memoryReadListener) {
    this.memoryReadListener = memoryReadListener;
  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener memoryReadListener) {

  }

  public void enableReadyOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  @Override
  public T[] getData() {
    return data;
  }

  @Override
  public void disableReadListener() { //FIXME: para que era???
    lastMemoryReadListener = memoryReadListener;
    memoryReadListener = null;
  }

  @Override
  public void enableReadListener() {
    memoryReadListener = lastMemoryReadListener;
  }

  @Override
  public void disableWriteListener() {
    lastMemoryWriteListener = memoryWriteListener;
    memoryWriteListener = null;
  }

  @Override
  public void enableWriteListener() {
    memoryWriteListener = lastMemoryWriteListener;
  }
}
