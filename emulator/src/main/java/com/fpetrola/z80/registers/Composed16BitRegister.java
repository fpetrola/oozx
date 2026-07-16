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
import com.fpetrola.z80.spy.RegisterWriteListener;

public class Composed16BitRegister<T extends WordNumber, R extends Register<T>> extends ObservableRegister<T> implements RegisterPair<T> {
  protected final R high;
  protected final R low;

  private Composed16BitRegister(String h, String l) {
    super(h + l);
    high = (R) new Plain8BitRegister(h);
    low = (R) new Plain8BitRegister(l);
  }

  public Composed16BitRegister(String name, String h, String l) {
    this(h, l);
  }

  public Composed16BitRegister(String name, Register h, Register l) {
    super(name);
    high = (R) h;
    low = (R) l;
  }

  public Composed16BitRegister(RegisterName name, Register h, Register l) {
    this(name.name(), h, l);
  }

  public T read() {
    T or = combine(high.read(), low.read());
    reading(or);
    return or;
  }

  private T combine(T hValue, T lValue) {
    return (hValue.left(8)).or(lValue);
  }

  public void write(T value) {
    writing(value);
    if (listening) {
      update8Bits(value.right(8), getHigh());
      update8Bits(value.and(0xFF), getLow());
    } else {
      this.high.write(value.right(8));
      this.low.write(value.and(0xFF));
    }
  }

  private void update8Bits(T value, R register) {
    ObservableRegister<T> Register8Bits = (ObservableRegister<T>) register;
    boolean listening = Register8Bits.isListening();
    Register8Bits.setListening(false);
    Register8Bits.write(value);
    Register8Bits.setListening(listening);
  }

  public R getHigh() {
    return this.high;
  }

  @Override
  public R getLow() {
    return this.low;
  }

  public void increment() {
    incrementing(read());
    T plus = low.read().plus(1);
    low.write(plus);
    if (plus.intValue() < 0x100)
      return;
    low.write(WordNumber.createValue(0));
    T plus1 = high.read().plus(1);
    high.write(plus1);
    if (plus1.intValue() < 0x100)
      return;
    high.write(WordNumber.createValue(0));
  }

  public void decrement() {
    decrementing(read());

    T lowValue = low.read();
    if (lowValue.isNotZero()) {
      T minus = lowValue.minus1();
      low.write(minus);
    } else {
      low.write(WordNumber.createValue(0xff));
      T highValue = high.read();
      if (highValue.isNotZero()) {
        T minus1 = highValue.minus1();
        high.write(minus1);
        return;
      }
      high.write(WordNumber.createValue(0xff));
    }
  }

  public int getLength() {
    return 0;
  }

  public void addRegisterWriteListener(RegisterWriteListener<T> memoryWriteListener) {
    super.addRegisterWriteListener(memoryWriteListener);
    ObservableRegister<T> high1 = (ObservableRegister<T>) getHigh();
    high1.addRegisterWriteListener((value, isIncrement) -> {
      memoryWriteListener.writingRegister(combine(value, low.read()), isIncrement);
    });
    ObservableRegister<T> low1 = (ObservableRegister<T>) getLow();
    low1.addRegisterWriteListener((value, isIncrement) -> {
      memoryWriteListener.writingRegister(combine(high.read(), value), isIncrement);
    });
  }

  @Override
  public void setListening(boolean state) {
    super.setListening(state);
    ObservableRegister<T> low1 = (ObservableRegister<T>) getLow();
    ObservableRegister<T> high1 = (ObservableRegister<T>) getHigh();
    low1.setListening(state);
    high1.setListening(state);
  }
}
