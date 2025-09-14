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

package com.fpetrola.oozx.speccy.modules.keyboard;

import com.google.inject.Singleton;

/**
 * The keys as the machine reads them: eight half rows of five bits, a bit low while its key is
 * held, and a read of the ULA port giving back every half row the address selects, ANDed.
 * <p>
 * What each of the 256 high bytes reads is worked out when a key goes down or up, so the read a
 * game makes eight times a frame is one load. That is this class's own business: nothing outside
 * knows there is a table.
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

  /** What the port whose address has that high byte reads. */
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
