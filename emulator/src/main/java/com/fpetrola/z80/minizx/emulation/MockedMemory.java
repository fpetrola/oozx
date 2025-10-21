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

import com.fpetrola.z80.helpers.CollectionHandler;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.memory.MemoryWriteListener;
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.opcodes.references.WordNumber;

import java.util.function.Supplier;

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

@SuppressWarnings("unchecked")
public class MockedMemory<T extends WordNumber> implements Memory<T> {
  protected T[] data = (T[]) new WordNumber[0x10000];
  private final CollectionHandler<MemoryWriteListener<T>> memoryWriteListener = new CollectionHandler<>();
  protected CollectionHandler<MemoryReadListener<T>> memoryReadListener = new CollectionHandler<>();
  private boolean readOnly;
  private boolean canDisable;

  public MockedMemory(boolean canDisable1) {
    this.canDisable = false;
  }

  public void init(Supplier<T[]> supplier) {
    data = supplier.get();
  }

  public T read(T address, int fetching) {
    T value = doRead(address);
    memoryReadListener.forAll(l -> l.readingMemoryAt(address, value, fetching));

    return value;
  }

  private T doRead(T address) {
    int i = address.intValue();
    return i >= 0 ? data[i] : createValue(0);
  }

  @Override
  public void write(T address, T value) {
    if (!readOnly) {
      memoryWriteListener.forAll(l -> l.writtingMemoryAt(address, value));
      data[address.intValue()] = value.and(0xff);
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
      data[i] = createValue(0);
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
    if (canDisable) memoryReadListener.disable();
  }

  @Override
  public void enableReadListener() {
    if (canDisable) memoryReadListener.enable();
  }

  @Override
  public void disableWriteListener() {
    if (canDisable) memoryWriteListener.disable();
  }

  @Override
  public void enableWriteListener() {
    if (canDisable) memoryWriteListener.enable();
  }

  @Override
  public void canDisable(boolean canDisable) {
    this.canDisable = canDisable;
  }

  @Override
  public boolean canDisable() {
    return canDisable;
  }

  @Override
  public boolean isReadListenersDisabled() {
    return !memoryReadListener.isEnabled();
  }
}
