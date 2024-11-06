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
package net.emustudio.plugins.cpu.zilogZ80;

import com.fpetrola.z80.opcodes.references.WordNumber;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

public class FakeByteDevice implements Context8080.CpuPortDevice {
    private final int port;
    private final InstructionsTest.MyIO io;
    private byte value;

    public FakeByteDevice(int port, InstructionsTest.MyIO myIO) {
        this.port = port;
        this.io = myIO;
        io.addDevice(port, this);
    }

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        this.value = value;
      //  io.out(WordNumber.createValue(port), WordNumber.createValue(value));
    }

    @Override
    public byte read(int portAddress) {
        return (byte) (value & 0xFF);
    }

    @Override
    public void write(int portAddress, byte value) {
        this.value = value;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "FakeByteDevice";
    }
}
