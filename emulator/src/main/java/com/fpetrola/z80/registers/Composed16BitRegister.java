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

public class Composed16BitRegister< R extends Register> implements RegisterPair {
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

  public int read() {
    Integer wordNumber = high.read();
    Integer number = (wordNumber << 8) & 0xFFFF;
    int i = low.read() & 0xFFFF;
    return (number | i) & 0xFFFF;
  }

  public void write(int value) {
    this.high.write((value >>> 8) & 0xFFFF);
    this.low.write((value & 0xFF) & 0xFFFF);
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
    if (low.read() < 0x100)
      return;
    low.write(0);
    high.increment();
    if (high.read() < 0x100)
      return;
    high.write(0);
  }

  public void decrement() {
    int lowValue = low.read();
    if (lowValue != 0) {
      lowValue--;
      lowValue &= 0xffff;
      low.write(lowValue);
    } else {
      low.write(0xff);
      int highValue = high.read();
      if (highValue != 0) {
        highValue--;
        highValue &= 0xffff;
        high.write(highValue);
        return;
      }
      high.write(0xff);
    }
  }

  public int getLength() {
    return 0;
  }

  public RegisterPair clone() throws CloneNotSupportedException {
    return this;
  }

  public String getName() {
    return name;
  }
}
