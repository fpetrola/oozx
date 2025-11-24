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

    // --------------------------------------------------------------------
    //  byte[] → String (1 byte = 1 carácter Unicode)
    // --------------------------------------------------------------------
    public static String packToUnicodeString(byte[] data) {
        // Reservar capacidad exacta para evitar rehash
        char[] chars = new char[data.length];
        for (int i = 0; i < data.length; i++) {
            // byte sin signo: 0 → U+0080, 255 → U+00FF
            int unsigned = data[i] & 0xFF;
            chars[i] = (char) (unsigned + 0x80);
        }
        return new String(chars);
    }

    // --------------------------------------------------------------------
    //  String → byte[] (recupera el snapshot original)
    // --------------------------------------------------------------------
    public static byte[] unpackFromUnicodeString(String packed) {
        int len = packed.length();
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            int codePoint = packed.charAt(i);           // será entre 0x80 y 0xFF
            data[i] = (byte) (codePoint - 0x80);        // vuelve a 0..255
        }
        return data;
    }

    // --------------------------------------------------------------------
    //  Ejemplo de uso
    // --------------------------------------------------------------------
    public static void main(String[] args) throws Exception {
        // Simulamos cargar un snapshot .sna
        byte[] snapshot = java.nio.file.Files.readAllBytes(
            java.nio.file.Paths.get("ManicMiner.sna"));   // 49179 bytes

        // → String de solo 49179 caracteres
        String jsonSafe = packToUnicodeString(snapshot);
        System.out.println("Longitud del string: " + jsonSafe.length()); // → 49179

        // Guardas en tu JSON así:
        String json = """
            {
              "game": "Manic Miner",
              "level": 15,
              "snapshot": "%s"
            }
            """.formatted(jsonSafe);

        // ... guardas json en archivo, base de datos, etc.

        // ================================================================
        // Más tarde recuperas el snapshot:
        // ================================================================
        String packedFromJson = jsonSafe; // lo sacas del JSON
        byte[] restored = unpackFromUnicodeString(packedFromJson);

        // Verificación
        System.out.println("Iguales? " + java.util.Arrays.equals(snapshot, restored));
        // → true
    }
}