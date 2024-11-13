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

import com.fpetrola.z80.spy.ObservableRegister;
import com.fpetrola.z80.spy.RegisterWriteListener;

public class Composed16BitRegister<R extends Register> extends ObservableRegister implements RegisterPair {
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

    public int read() {
        int or = high.read() << 8 | low.read();
        reading(or);
        return or;
    }

    private int combine(int hValue, int lValue) {
        return (hValue << 8) | lValue;
    }

    public void write(int value) {
        writing(value);
        if (listening) {
            update8Bits(value >> 8, getHigh());
            update8Bits(value & 0xFF, getLow());
        } else {
            this.high.write(value >> 8);
            this.low.write(value & 0xFF);
        }
    }

    private void update8Bits(int value, R register) {
        ObservableRegister Register8Bits = (ObservableRegister) register;
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
            low.write((lowValue - 1) & 0xffff);
        } else {
            low.write(0xff);
            int highValue = high.read();
            if (highValue != 0) {
                high.write((highValue - 1) & 0xffff);
            } else
                high.write(0xff);
        }
    }

    public int getLength() {
        return 0;
    }

    public void addRegisterWriteListener(RegisterWriteListener memoryWriteListener) {
        super.addRegisterWriteListener(memoryWriteListener);
        ObservableRegister high1 = (ObservableRegister) getHigh();
        high1.addRegisterWriteListener((value, isIncrement) -> {
            memoryWriteListener.writingRegister(combine(value, low.read()), isIncrement);
        });
        ObservableRegister low1 = (ObservableRegister) getLow();
        low1.addRegisterWriteListener((value, isIncrement) -> {
            memoryWriteListener.writingRegister(combine(high.read(), value), isIncrement);
        });
    }

    @Override
    public void setListening(boolean state) {
        super.setListening(state);
        ObservableRegister low1 = (ObservableRegister) getLow();
        ObservableRegister high1 = (ObservableRegister) getHigh();
        low1.setListening(state);
        high1.setListening(state);
    }
}
