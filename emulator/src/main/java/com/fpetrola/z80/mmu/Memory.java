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

package com.fpetrola.z80.mmu;

import com.fpetrola.z80.jspeccy.MemoryReadListener;
import com.fpetrola.z80.jspeccy.MemoryWriteListener;
import com.fpetrola.z80.opcodes.references.WordNumber;

public interface Memory<T> {

  static <T extends WordNumber> T read16Bits(Memory<T> memory, T address) {
    return memory.read(address.plus1()).left(8).or(memory.read(address).and(0xff));
  }

  static <T extends WordNumber> void write16Bits(Memory<T> memory, T value, T address) {
    memory.write(address, value.and(0xFF));
    memory.write(address.plus1(), (value.right(8)));
  }

  T read(T address);

  void write(T address, T value);

  boolean compare();

  void update();

  void addMemoryWriteListener(MemoryWriteListener memoryWriteListener);

  void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener);

  void reset();

  void addMemoryReadListener(MemoryReadListener memoryReadListener);

  void removeMemoryReadListener(MemoryReadListener memoryReadListener);

  default T[] getData() {
    return (T[]) new WordNumber[0];
  }

  default void disableReadListener() {
  }

  default void enableReadListener() {
  }

  default void disableWriteListener() {
  }

  default void enableWriteListener() {
  }
}
