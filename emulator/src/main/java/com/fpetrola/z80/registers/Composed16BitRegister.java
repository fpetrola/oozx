/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

public class Composed16BitRegister<R extends Register> implements RegisterPair {
  protected final Plain8BitRegister high;
  protected final Plain8BitRegister low;
  private String name;

  private Composed16BitRegister(String h, String l) {
    high = new Plain8BitRegister(h);
    low = new Plain8BitRegister(l);
  }

  public Composed16BitRegister(String name, String h, String l) {
    this(h, l);
    this.name = name;
  }

  public Composed16BitRegister(String name, Register h, Register l) {
    high = (Plain8BitRegister) h;
    low = (Plain8BitRegister) l;
    this.name = name;
  }

  public Composed16BitRegister(RegisterName name, Register h, Register l) {
    this(name.name(), h, l);
  }

  public int read() {
    return high.data << 8 | low.data;
  }

  public void write(final int value) {
    this.high.data = value >>> 8;
    this.low.data = value & 0xFF;
  }

  public R getHigh() {
    return (R) this.high;
  }

  @Override
  public R getLow() {
    return (R) this.low;
  }

  @Override
  public String toString() {
    return name == null ? high.toString() + low.toString() : name;
  }

  public void increment() {
    if (++low.data < 0x100)
      return;
    low.data = 0;
    if (++high.data < 0x100)
      return;
    high.data = 0;
  }

  public void decrement() {
    if (--low.data >= 0)
      return;
    low.data = 0xff;

    if (--high.data >= 0)
      return;
    high.data = 0xff;
  }

  public int getLength() {
    return 0;
  }

  public String getName() {
    return name;
  }
}
