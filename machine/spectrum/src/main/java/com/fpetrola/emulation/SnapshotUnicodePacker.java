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

package com.fpetrola.emulation;

import java.nio.charset.StandardCharsets;

public class SnapshotUnicodePacker {

    public static String packToUnicodeString(byte[] data) {
        char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            int unsigned = data[i] & 0xFF;
            chars[i] = (char) (unsigned + 0x80);
        }
        return new String(chars);
    }

    public static byte[] unpackFromUnicodeString(String packed) {
        int len = packed.length();
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            int codePoint = packed.charAt(i);
            data[i] = (byte) (codePoint - 0x80);
        }
        return data;
    }

    public static void main(String[] args) throws Exception {
        byte[] snapshot = java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get("ManicMiner.sna"));

        String jsonSafe = packToUnicodeString(snapshot);
        System.out.println("Longitud del string: " + jsonSafe.length());

        String json = """
            {
              "game": "Manic Miner",
              "level": 15,
              "snapshot": "%s"
            }
            """.formatted(jsonSafe);

        String packedFromJson = jsonSafe;
        byte[] restored = unpackFromUnicodeString(packedFromJson);

        System.out.println("Iguales? " + java.util.Arrays.equals(snapshot, restored));
    }
}