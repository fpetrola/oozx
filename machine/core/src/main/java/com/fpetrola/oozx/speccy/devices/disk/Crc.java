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
package com.fpetrola.oozx.speccy.devices.disk;

/** The CRC a floppy controller puts after every ID and every sector: CRC-16-CCITT, x^16+x^12+x^5+1. */
public final class Crc {

  private static final int[] TABLE = new int[256];

  static {
    for (int i = 0; i < 256; i++) {
      int crc = i << 8;
      for (int bit = 0; bit < 8; bit++) {
        crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
      }
      TABLE[i] = crc & 0xffff;
    }
  }

  private Crc() {
  }

  public static int fdc(int crc, int data) {
    return ((crc << 8) ^ TABLE[((crc >> 8) ^ data) & 0xff]) & 0xffff;
  }
}
