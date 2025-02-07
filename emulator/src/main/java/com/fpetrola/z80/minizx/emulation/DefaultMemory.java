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

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.ArrayList;
import java.util.List;

public class DefaultMemory<T extends WordNumber> implements Memory<T> {
  protected T[] data = (T[]) new WordNumber[0x10000];
  private List<MemoryWriteListener> memoryWriteListener = new ArrayList<>();
  private boolean readOnly;
  protected List<MemoryReadListener> memoryReadListener = new ArrayList<>();
  private List<MemoryReadListener> lastMemoryReadListener;
  private List<MemoryWriteListener> lastMemoryWriteListener;
  private boolean canDisable;

  public DefaultMemory(boolean canDisable1) {
    this.canDisable = false;
  }

  public T read(T address, int fetching) {
    T value = doRead(address);
    boolean b = true || fetching == 1 || address.intValue() < 0;
    if (memoryReadListener != null && b) {
      for (int i = 0, memoryReadListenerSize = memoryReadListener.size(); i < memoryReadListenerSize; i++) {
        MemoryReadListener l = memoryReadListener.get(i);
        l.readingMemoryAt(address, value, 0, fetching);
      }
    }

    return value;
  }

  public T read(T address, int delta, int fetching) {
    T value = doRead(address);
    if (memoryReadListener != null) {
      for (int i = 0, memoryReadListenerSize = memoryReadListener.size(); i < memoryReadListenerSize; i++) {
        MemoryReadListener l = memoryReadListener.get(i);
        l.readingMemoryAt(address, value, delta, fetching);
      }
    }

    return value;
  }

  protected T doRead(T address) {
    T value = WordNumber.createValue(0);
    if (address.intValue() >= 0) {
      T datum = data[address.intValue()];
      if (datum == null) {
      } else {
        value = datum;
      }
    }
    return value;
  }

  @Override
  public void write(T address, T value) {
    if (!readOnly) {
      if (memoryWriteListener != null) {
        for (int i = 0, memoryWriteListenerSize = memoryWriteListener.size(); i < memoryWriteListenerSize; i++) {
          MemoryWriteListener<T> l = memoryWriteListener.get(i);
          T o = l.writtingMemoryAt(address, value);
          if (o != value)
            value= o;
        }
      }
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
  public void addMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener) {
    this.memoryWriteListener.add(memoryWriteListener);
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener) {
  }

  @Override
  public void reset() {
    for (int i = 0; i < data.length; i++) {
      data[i] = WordNumber.createValue(0);
    }
//    Arrays.fill(data, WordNumber.createValue(0));
  }

  @Override
  public void addMemoryReadListener(MemoryReadListener<T> memoryReadListener) {
    this.memoryReadListener.add(memoryReadListener);
  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener<T> memoryReadListener) {

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
    if (canDisable) {
      if (memoryReadListener != null) {
        lastMemoryReadListener = memoryReadListener;
        memoryReadListener = null;
      }
    }
  }

  @Override
  public void enableReadListener() {
    if (canDisable) {
      memoryReadListener = lastMemoryReadListener;
    }
  }

  @Override
  public void disableWriteListener() {
    if (canDisable) {
      lastMemoryWriteListener = memoryWriteListener;
      memoryWriteListener = null;
    }
  }

  @Override
  public void enableWriteListener() {
    if (canDisable) {
      memoryWriteListener = lastMemoryWriteListener;
    }
  }

  @Override
  public void canDisable(boolean canDisable) {
    this.canDisable = canDisable;
  }

  @Override
  public boolean canDisable() {
    return canDisable;
  }
}
