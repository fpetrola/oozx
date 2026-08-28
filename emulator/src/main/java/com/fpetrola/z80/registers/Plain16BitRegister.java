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

import com.fpetrola.z80.spy.ObservableRegister;

public class Plain16BitRegister extends ObservableRegister {
    protected int data;

    public Plain16BitRegister(String name) {
        super(name);
    }

    public Plain16BitRegister(RegisterName name) {
        this(name.name());
    }

    public int read() {
        reading(data);
        return data;
    }

    public void write(int value) {
        int data1 = value & 0xFFFF;
        writing(data1);
        this.data = data1;
    }

    public void increment() {
        incrementing(data);
        data = (data + 1) & 0xFFFF;
    }

    public void decrement() {
        decrementing(data);
        data = (data - 1) & 0xFFFF;
    }

    public int getLength() {
        return 0;
    }
}
