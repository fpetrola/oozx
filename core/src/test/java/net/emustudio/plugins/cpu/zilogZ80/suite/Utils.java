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
package net.emustudio.plugins.cpu.zilogZ80.suite;

import java.util.function.Predicate;
import java.util.zip.CRC32;

public class Utils {

    public static int get8MSBplus8LSB(int value) {
        return ((value & 0xFF00) + (byte) (value & 0xFF)) & 0xFFFF;
    }

    public static Predicate<Integer> predicate8MSBplus8LSB(int minimum) {
        return value -> get8MSBplus8LSB(value) > minimum;
    }

    public static long crc16(final byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return crc.getValue();
    }
}
