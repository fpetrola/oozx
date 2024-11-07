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

package com.fpetrola.z80.helpers;

import com.fpetrola.z80.memory.Memory;
import com.fpetrola.z80.registers.Register;

public final class Z80Utils {

    /**
     * Return only the 8 least significant bits of an integer
     *
     * @param value input data
     * @return masked data
     */
    public final static int mask8bit(int value) {
        return value & 0xFF;
    }

    /**
     * Return only the 16 least significant bits of an integer
     *
     * @param value input data
     * @return masked data
     */
    public final static int mask16bit(int value) {
        return value & 0xFFFF;
    }

    /**
     * Return the higher 8-bits from a 16-bit data
     *
     * @param value 16-bit data
     * @return 8-bit data
     */
    public final static int high8bits(int value) {
        return (value >> 8);
    }

    /**
     * Join two 8-bit data into a 16-bit data
     *
     * @param high 8-bit data
     * @param low  8-bite data
     * @return 16-bit data
     */
    public final static int compose16bit(int high, int low) {
        return (high << 8) | low;
    }

    /**
     * Read 16-bit from memory. The memory display is LH (little indian)
     *
     * @param address 16-bit address
     * @param memory  8-bit memory adapter
     * @return
     */
    private final static int read16FromMemory(int address, Memory memory) {
//    	 int lsb = memory.read(address) & 0xff;
//         address = (address + 1) & 0xffff;
//         return ((memory.read(address) << 8) & 0xff00 | lsb);
        return 0;
    }

    /**
     * Write 16 bytes of data to memory
     *
     * @param address pointer
     * @param value   16-bit value
     * @param memory  memory adapter
     */
    public final static void write16ToMemory(int address, int value, Memory memory) {
        memory.write(mask16bit(address), Z80Utils.mask8bit(value));
        memory.write(mask16bit(address + 1), Z80Utils.high8bits(value));
    }

    /**
     * Convert the 8-bit value in a range between -128 and +127
     *
     * @param e
     * @return
     */
    public final static int twoCompliment8bit(int e) {
        return (byte) e;
    }

    /**
     * Push 16-bit data into stack
     *
     * @param sp     Stack pointer register
     * @param value
     * @param memory
     */
    public final static void push(Register sp, int value, Memory memory) {
//        final int address = (sp.read() - 2) & 0xFFFF;
//        sp.write(address);
//        write16ToMemory(address, value, memory);
    }

    /**
     * Pop 16-bit data from stack
     *
     * @param sp     Stack pointer register
     * @param memory
     * @param memory
     */
    private final static int pop(Register sp, Memory memory) {
//        final int value = read16FromMemory(sp.read(), memory);
//        sp.write((sp.read() + 2) & 0xFFFF);
//        return value;
        return 0;
    }

    /**
     * Return true if the number of bits on the 8-bit integer is even. False otherwise.
     *
     * @param e 8-bit integer
     * @return true if even parity
     */
    public final static boolean isEvenParity8bit(int e) {

        int result = e;
        result = (result ^ (result >> 1));
        result = (result ^ (result >> 2));
        result = (result ^ (result >> 4));
        return ((result & 0x01) == 0x00);
    }

}
