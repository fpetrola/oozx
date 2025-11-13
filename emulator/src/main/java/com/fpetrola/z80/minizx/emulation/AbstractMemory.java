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
import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.memory.MemoryReadListener;
import com.fpetrola.z80.memory.MemoryWriteListener;

public abstract class AbstractMemory implements Memory {
  protected final CollectionHandler<MemoryWriteListener> memoryWriteListener = new CollectionHandler<>();
  protected final CollectionHandler<MemoryReadListener> memoryReadListener = new CollectionHandler<>();
  protected boolean readOnly;

  public AbstractMemory() {
  }

  public int read(int address, int fetching) {
    int value = doRead(address);
    memoryReadListener.forAll(l -> l.readingMemoryAt(address, value, fetching));

    return value;
  }

  protected abstract int doRead(int address);

  @Override
  public void write(final int address, final int value) {
    memoryWriteListener.forAll(l -> l.writtingMemoryAt(address, value));
    doWrite(address, value);
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
    this.memoryWriteListener.add(memoryWriteListener);
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
  }

  @Override
  public void addMemoryReadListener(MemoryReadListener memoryReadListener) {
    this.memoryReadListener.add(memoryReadListener);
  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener memoryReadListener) {

  }

  public void enableReadyOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public void disableReadListener() { //FIXME: para que era???
    memoryReadListener.disable();
  }

  public void enableReadListener() {
    memoryReadListener.enable();
  }

  public void disableWriteListener() {
    memoryWriteListener.disable();
  }

  public void enableWriteListener() {
    memoryWriteListener.enable();
  }

  public boolean isReadListenersDisabled() {
    return !memoryReadListener.isEnabled();
  }

  protected abstract void doWrite(int address, int value);
}
