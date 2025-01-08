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

package com.fpetrola.z80.spy;

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.registers.Register;

import java.util.ArrayList;
import java.util.List;

public abstract class ObservableRegister<T extends WordNumber> implements Register<T> {
  protected List<RegisterWriteListener<T>> registerWriteListeners = new ArrayList<>();
  protected List<RegisterReadListener<T>> registerReadListeners = new ArrayList<>();
  private boolean listening = false;

  private String name;

  public ObservableRegister(String name) {
    this.name = name;
  }

  public T reading(T value) {
    if (listening)
      registerReadListeners.forEach(l -> l.readingRegister(value));
    return value;
  }

  public void writing(T value) {
    if (listening)
      registerWriteListeners.forEach(l -> l.writingRegister(value, false));
  }

  public void incrementing(WordNumber value) {
    if (listening)
      registerWriteListeners.forEach(l -> l.writingRegister(value.plus1(), true));
  }

  public void decrementing(T value) {
    if (listening)
      registerWriteListeners.forEach(l -> l.writingRegister(value.minus1(), true));
  }

  public String toString() {
    return name;
  }

  public String getName() {
    return name;
  }

  public Object clone() throws CloneNotSupportedException {
    return this;
  }

  public void addRegisterWriteListener(RegisterWriteListener memoryWriteListener) {
    this.registerWriteListeners.add(memoryWriteListener);
  }

  public void removeRegisterWriteListener(RegisterWriteListener memoryWriteListener) {
    this.registerWriteListeners.remove(memoryWriteListener);
  }

  public void addRegisterReadListener(RegisterReadListener memoryReadListener) {
    this.registerReadListeners.add(memoryReadListener);
  }

  public void removeRegisterReadListener(RegisterReadListener memoryReadListener) {
    this.registerReadListeners.remove(memoryReadListener);
  }

  public void listening(boolean state) {
    listening = state;
  }
}
