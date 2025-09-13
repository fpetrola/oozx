/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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