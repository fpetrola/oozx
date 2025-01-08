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

package com.fpetrola.z80.registers;

import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.spy.ObservableRegister;

public class Plain8BitRegister<T extends WordNumber> extends ObservableRegister<T> implements Register<T> {
  protected T data;

  public Plain8BitRegister(String name) {
    super(name);
  }

  public Plain8BitRegister(RegisterName name) {
    this(name.name());
  }

  public T read() {
    reading(data);
    return data;
  }

  public void write(T value) {
    T and = value.and(0xff);
    writing(and);
    this.data = and;
  }


  public void increment() {
    incrementing(data);
    this.data = data.plus(1);
  }

  public void decrement() {
    decrementing(data);
    this.data = data.minus1().and(0xFF);
  }

  public int getLength() {
    return 0;
  }
}
