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

package com.fpetrola.z80.opcodes.references;

import java.util.List;

public interface WordNumber {
  static <T> T createValue(int i) {
    return (T) new IntegerWordNumber(i);
  }

  <T extends WordNumber> T plus(int i);

  default <T extends WordNumber> T plus1() {
    return plus(1);
  }

  <T extends WordNumber> T minus1();

  <T extends WordNumber> T left(int i);

  <T extends WordNumber> T right(int i);

  <T extends WordNumber> T or(int i);

  <T extends WordNumber> T xor(T wordNumber);

  <T extends WordNumber> T and(T wordNumber);

  <T extends WordNumber> T and(int i);

  <T extends WordNumber> T or(T wordNumber);

  boolean isNotZero();

  int intValue();

  <T extends WordNumber> T set(T value);

  WordNumber aluOperation2(WordNumber value1, WordNumber value2, String name);

  WordNumber aluOperation(WordNumber address, String name);

  <T extends WordNumber> T readOperation(T address, T value);

  <T extends WordNumber> List<T> getFirstReadOperation();

  default <T extends WordNumber> T createInstance(int value) {
    return WordNumber.createValue(value);
  }
}
