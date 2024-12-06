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

package com.fpetrola.z80.memory;

import com.fpetrola.z80.opcodes.references.WordNumber;

public interface Memory<T> {

  static <T extends WordNumber> T read16Bits(Memory<T> memory, T address) {
    T and = memory.read(address, 0).and(0xff);
    return memory.read(address.plus1(), 0).left(8).or(and);
  }

  static <T extends WordNumber> void write16Bits(Memory<T> memory, T value, T address) {
    memory.write(address.plus1(), (value.right(8)));
    memory.write(address, value.and(0xFF));
  }

  static <T extends WordNumber> void write16BitsR(Memory<T> memory, T value, T address) {
    memory.write(address, value.and(0xFF));
    memory.write(address.plus1(), (value.right(8)));
  }

  T read(T address, int fetching);

  default T read(T address, int delta, int fetching) {
    return read(address, fetching);
  }

  void write(T address, T value);

  boolean compare();

  void update();

  void addMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener);

  void removeMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener);

  void reset();

  void addMemoryReadListener(MemoryReadListener<T> memoryReadListener);

  void removeMemoryReadListener(MemoryReadListener<T> memoryReadListener);

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

  default void canDisable(boolean canDisable) {
  }

  default boolean canDisable() {
    return false;
  }
}
