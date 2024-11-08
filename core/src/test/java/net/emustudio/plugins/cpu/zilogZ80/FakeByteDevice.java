/*
 *
 *  * This file is part of emuStudio.
 *  *
 *  * Copyright (C) 2006-2023  Peter Jakubčo
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
