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
    WordNumber wordNumber1 = memory.read(address, 0);
    T and = (T) (WordNumber) new WordNumber((wordNumber1.value & 0xff) & 0xFFFF);
    WordNumber wordNumber = memory.read((T) (WordNumber) new WordNumber((address.value + 1) & 0xFFFF), 0);
    WordNumber number = ((WordNumber) (WordNumber) new WordNumber((wordNumber.value << 8) & 0xFFFF));
    int i = and.value & 0xFFFF;
    return (T) (WordNumber) new WordNumber((number.value | i) & 0xFFFF);
  }

  static <T extends WordNumber> void write16Bits(Memory<T> memory, T value, T address) {
    memory.write((T) (WordNumber) new WordNumber((address.value + 1) & 0xFFFF), ((T) (WordNumber) new WordNumber((value.value >>> 8) & 0xFFFF)));
    memory.write(address, (T) (WordNumber) new WordNumber((value.value & 0xFF) & 0xFFFF));
  }

  static <T extends WordNumber> void write16BitsR(Memory<T> memory, T value, T address) {
    memory.write(address, (T) (WordNumber) new WordNumber((value.value & 0xFF) & 0xFFFF));
    memory.write((T) (WordNumber) new WordNumber((address.value + 1) & 0xFFFF), ((T) (WordNumber) new WordNumber((value.value >>> 8) & 0xFFFF)));
  }

  T read(T address, int fetching);

  void write(T address, T value);

  default boolean compare() {
    return false;
  }

  default void update() {
  }

  default void addMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener){};

  default void removeMemoryWriteListener(MemoryWriteListener<T> memoryWriteListener){};

  void reset();

  default void addMemoryReadListener(MemoryReadListener<T> memoryReadListener) {
  }

  default void removeMemoryReadListener(MemoryReadListener<T> memoryReadListener) {
  }

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

  default void copyFrom(Memory<T> memory) {
    T[] data = memory.getData();
    for (int i = 0; i < data.length; i++) {
      T d = data[i];
      getData()[i] = d;
    }
  }

  default boolean isReadListenersDisabled() {
    return false;
  }
}
