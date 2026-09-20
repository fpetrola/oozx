/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
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

package com.fpetrola.oozx.speccy.modules.keyboard;

import com.google.inject.Singleton;

/**
 * 8 half rows of 5 bits, low while a key is held; a port read ANDs together every half row its address selects.
 * The 256 possible results are precomputed on press/release, so a game's read (up to 8 times a frame) is one lookup.
 */
@Singleton
public class KeyMatrix {
  private static final byte ALL_UP = (byte) 0xFF;

  private final byte[] halfRows = new byte[SpectrumKey.HALF_ROWS];
  private final byte[] byHighByte = new byte[256];

  public KeyMatrix() {
    releaseAll();
  }

  public void press(SpectrumKey key) {
    halfRows[key.halfRow()] &= ~key.bit();
    decode();
  }

  public void release(SpectrumKey key) {
    halfRows[key.halfRow()] |= key.bit();
    decode();
  }

  public void releaseAll() {
    java.util.Arrays.fill(halfRows, ALL_UP);
    decode();
  }

  /** Whether that key is held, which is what anything showing the keyboard asks. */
  public boolean isDown(SpectrumKey key) {
    return (halfRows[key.halfRow()] & key.bit()) == 0;
  }

  public byte read(int high) {
    return byHighByte[high & 0xff];
  }

  private void decode() {
    for (int high = 0; high < byHighByte.length; high++) {
      byte reads = ALL_UP;
      for (int row = 0; row < halfRows.length; row++) {
        if ((high & (1 << row)) == 0) {
          reads &= halfRows[row];
        }
      }
      byHighByte[high] = reads;
    }
  }
}
