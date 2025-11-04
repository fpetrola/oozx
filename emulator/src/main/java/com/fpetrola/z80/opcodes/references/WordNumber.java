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

public class WordNumber {
  private int value;

  int compareTo(Object o) {
    return intValue() - ((WordNumber) o).intValue();
  }

  public static <T> T createValue(int i) {
    return (T) new WordNumber(i);
  }

  public <T extends WordNumber> T plus1() {
    return plus(1);
  }


  public WordNumber(int aValue) {
    this.value = aValue;
  }

  
  public <T extends WordNumber> T plus(int i) {
    return (T) createInstance((value + i));
  }

  public <T extends WordNumber> WordNumber createInstance(int value) {
    return new WordNumber(value & 0xFFFF);
  }

  public <T extends WordNumber> T minus(T i) {
    return (T) createInstance((value - i.intValue()));
  }

  
  public <T extends WordNumber> T minus1() {
    return (T) createInstance((value - 1));
  }

  
  public <T extends WordNumber> T left(int i) {
    return (T) createInstance((value << i));
  }

  
  public <T extends WordNumber> T right(int i) {
    return (T) createInstance((value >>> i));
  }

  
  public <T extends WordNumber> T rightAndAssign(int i) {
    value >>>= i;
    return (T) this;
  }

  
  public <T extends WordNumber> T leftAndAssign(int i) {
    value <<= i;
    return (T) this;
  }

  
  public <T extends WordNumber> T or(int i) {
    return (T) createInstance((value | i));
  }

  public <T extends WordNumber> T xor(int i) {
    return (T) createInstance((value ^ i));
  }

  
  public <T extends WordNumber> T xor(T wordNumber) {
    return xor(wordNumber.intValue() & 0xFFFF);
  }

  
  public <T extends WordNumber> T and(T wordNumber) {
    return and(wordNumber.intValue() & 0xFFFF);
  }

  
  public <T extends WordNumber> T and(int i) {
    return (T) createInstance((value & i));
  }

  
  public <T extends WordNumber> T or(T wordNumber) {
    return or(wordNumber.intValue() & 0xFFFF);
  }

  
  public boolean isNotZero() {
    return value != 0;
  }

  
  public int intValue() {
    return value;
  }

  
  public <T extends WordNumber> T set(T value) {
    this.value = value.intValue();
    return value;
  }

  
  public WordNumber aluOperation2(WordNumber value1, WordNumber value2, String name) {
    return createInstance(value1.intValue());
  }

  
  public WordNumber aluOperation(WordNumber value, String name) {
    return createInstance(value.intValue());
  }

  
  public <T extends WordNumber> T readOperation(T address, T value) {
    return (T) createInstance(value.intValue());
  }

  
  public <T extends WordNumber> List<T> getFirstReadOperation() {
    return (List<T>) List.of(this);
  }

  
  public void increment() {
    value++;
  }

  
  public void decrement() {
    value--;
    value &= 0xffff;
  }

  public String toString() {
    return value + "";
  }
}
