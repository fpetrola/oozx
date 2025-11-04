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

import static com.fpetrola.z80.opcodes.references.WordNumber.createValue;

public class Composed16BitRegister<T extends WordNumber, R extends Register<T>> implements RegisterPair<T> {
  protected final R high;
  protected final R low;
  private String name;

  private Composed16BitRegister(String h, String l) {
    high = (R) new Plain8BitRegister(h);
    low = (R) new Plain8BitRegister(l);
  }

  public Composed16BitRegister(String name, String h, String l) {
    this(h, l);
    this.name = name;
  }

  public Composed16BitRegister(String name, Register h, Register l) {
    high = (R) h;
    low = (R) l;
    this.name = name;
  }

  public Composed16BitRegister(RegisterName name, Register h, Register l) {
    this(name.name(), h, l);
  }

  public T read() {
    WordNumber wordNumber = high.read();
    WordNumber number = ((WordNumber) WordNumber.<WordNumber>createValue((wordNumber.value << 8) & 0xFFFF));
    int i = low.read().value & 0xFFFF;
    return (T) WordNumber.<WordNumber>createValue((number.value | i) & 0xFFFF);
  }

  public void write(T value) {
    this.high.write((T) WordNumber.<WordNumber>createValue((value.value >>> 8) & 0xFFFF));
    this.low.write((T) WordNumber.<WordNumber>createValue((value.value & 0xFF) & 0xFFFF));
  }

  public R getHigh() {
    return this.high;
  }

  @Override
  public R getLow() {
    return this.low;
  }

  @Override
  public String toString() {
    return name == null ? high.toString() + low.toString() : name;
  }

  public void increment() {
    low.increment();
    if (low.read().value < 0x100)
      return;
    low.write(createValue(0));
    high.read().value++;
    if (high.read().value < 0x100)
      return;
    high.write(createValue(0));
  }

  public void decrement() {
    T lowValue = low.read();
    if (lowValue.value != 0) {
      lowValue.value--;
      lowValue.value &= 0xffff;
    } else {
      low.write(createValue(0xff));
      T highValue = high.read();
      if (highValue.value != 0) {
        highValue.value--;
        highValue.value &= 0xffff;
        return;
      }
      high.write(createValue(0xff));
    }
  }

  public int getLength() {
    return 0;
  }

  public RegisterPair<T> clone() throws CloneNotSupportedException {
    return this;
  }

  public String getName() {
    return name;
  }
}
