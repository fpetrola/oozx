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

package com.fpetrola.z80.jspeccy;

import com.fpetrola.z80.mmu.Memory;

public class ReadOnlyMemoryImplementation<T> implements Memory<T> {
  protected Memory<T> memory;

  public ReadOnlyMemoryImplementation(Memory memory) {
    this.memory = memory;
  }

  public T read(T address) {
    return memory.read(address);
  }

  public void write(T address, T value) {
  }

  public boolean compare() {
    return memory.compare();
  }

  public void update() {
  }

  public void addMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
  }

  @Override
  public void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener) {

  }

  public Memory getMemory() {
    return this;
  }

  @Override
  public void reset() {

  }

  @Override
  public void addMemoryReadListener(MemoryReadListener memoryReadListener) {

  }

  @Override
  public void removeMemoryReadListener(MemoryReadListener memoryReadListener) {

  }
}