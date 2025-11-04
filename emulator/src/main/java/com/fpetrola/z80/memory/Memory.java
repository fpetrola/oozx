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

public interface Memory {
  static int read16Bits(Memory memory, int address) {
    int wordNumber1 = memory.read(address, 0);
    int wordNumber = memory.read((address + 1) & 0xFFFF, 0);
    return ((wordNumber << 8) | wordNumber1);
  }

  static void write16Bits(Memory memory, int value, int address) {
    memory.write((address + 1) & 0xFFFF, ((value >>> 8)));
    memory.write(address, (value & 0xFF));
  }

  static void write16BitsR(Memory memory, int value, int address) {
    memory.write(address, (value & 0xFF));
    memory.write((address + 1) & 0xFFFF, ((value >>> 8)));
  }

  int read(int address, int fetching);

  void write(int address, int value);

  default boolean compare() {
    return false;
  }

  default void update() {
  }

  default void addMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
  }

  ;

  default void removeMemoryWriteListener(MemoryWriteListener memoryWriteListener) {
  }

  ;

  void reset();

  default void addMemoryReadListener(MemoryReadListener memoryReadListener) {
  }

  default void removeMemoryReadListener(MemoryReadListener memoryReadListener) {
  }

  default int[] getData() {
    return new int[0];
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

  default void copyFrom(Memory memory) {
    int[] data = memory.getData();
    for (int i = 0; i < data.length; i++) {
      int d = data[i];
      getData()[i] = d;
    }
  }

  default boolean isReadListenersDisabled() {
    return false;
  }
}
