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

import java.util.Arrays;
import java.util.List;

public class IntegerWordNumber implements WordNumber {
  private int value;

  public IntegerWordNumber(int aValue) {
    this.value = aValue;
  }

  @Override
  public <T extends WordNumber> T plus(int i) {
    return (T) createInstance((value + i));
  }

  public IntegerWordNumber createInstance(int value) {
    return new IntegerWordNumber(value & 0xFFFF);
  }

  @Override
  public <T extends WordNumber> T minus1() {
    return (T) createInstance((value - 1));
  }

  @Override
  public <T extends WordNumber> T left(int i) {
    return (T) createInstance((value << i));
  }

  @Override
  public <T extends WordNumber> T right(int i) {
    return (T) createInstance((value >>> i));
  }

  @Override
  public <T extends WordNumber> T or(int i) {
    return (T) createInstance((value | i));
  }

  public <T extends WordNumber> T xor(int i) {
    return (T) createInstance((value ^ i));
  }

  @Override
  public <T extends WordNumber> T xor(T wordNumber) {
    return xor(wordNumber.intValue() & 0xFFFF);
  }

  @Override
  public <T extends WordNumber> T and(T wordNumber) {
    return and(wordNumber.intValue() & 0xFFFF);
  }

  @Override
  public <T extends WordNumber> T and(int i) {
    return (T) createInstance((value & i));
  }

  @Override
  public <T extends WordNumber> T or(T wordNumber) {
    return or(wordNumber.intValue() & 0xFFFF);
  }

  @Override
  public boolean isNotZero() {
    return value != 0;
  }

  @Override
  public int intValue() {
    return value;
  }

  @Override
  public <T extends WordNumber> T set(T value) {
    this.value = value.intValue();
    return value;
  }

  @Override
  public WordNumber aluOperation2(WordNumber value1, WordNumber value2, String name) {
    return createInstance(value1.intValue());
  }

  @Override
  public WordNumber aluOperation(WordNumber value, String name) {
    return createInstance(value.intValue());
  }

  @Override
  public <T extends WordNumber> T readOperation(T address, T value) {
    return (T) createInstance(value.intValue());
  }

  @Override
  public <T extends WordNumber> List<T> getFirstReadOperation() {
    return (List<T>) List.of(this);
  }

  public String toString() {
    return value + "";
  }
}
