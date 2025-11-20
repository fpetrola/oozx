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
