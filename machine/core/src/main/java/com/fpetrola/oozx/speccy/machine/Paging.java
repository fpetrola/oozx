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
package com.fpetrola.oozx.speccy.machine;

/**
 * The 128's paging as the machine holds it: the byte last written to each of its two ports, and
 * nothing else, since which ROM, page and screen are in is only a reading of those bytes. A 48K
 * holds one too and never writes it, so it reads as the 48K's one map.
 * <p>
 * A program's write goes through the lock; a latch does not. The latch is for what is not a
 * program writing the port: the Beta putting the 48 ROM at the bottom.
 */
public final class Paging {
  /** The +3's all-RAM maps, chosen by bits 1 and 2 of 0x1ffd. */
  private static final int[][] ALL_RAM = {{0, 1, 2, 3}, {4, 5, 6, 7}, {4, 5, 6, 3}, {4, 7, 6, 3}};

  private byte port7ffd;
  private byte port1ffd;

  public byte port7ffd() {
    return port7ffd;
  }

  public byte port1ffd() {
    return port1ffd;
  }

  /** A program writing the port: refused while locked. Answers whether it took. */
  public boolean write7ffd(byte b) {
    if (locked()) return false;
    latch7ffd(b);
    return true;
  }

  public boolean write1ffd(byte b) {
    if (locked()) return false;
    port1ffd = b;
    return true;
  }

  public void latch7ffd(byte b) {
    port7ffd = b;
  }

  public void reset() {
    port7ffd = 0;
    port1ffd = 0;
  }

  /** Which RAM page is in a 16K slot. Slot 0 has one only in special mode; otherwise it is a ROM. */
  public int page(int slot) {
    if (special()) return ALL_RAM[(port1ffd >> 1) & 3][slot];
    return slot == 3 ? port7ffd & 0x07 : slot == 1 ? 5 : 2;
  }

  public int screen() {
    return (port7ffd & 0x08) != 0 ? 7 : 5;
  }

  /** Which ROM is at the bottom: the 128's bit, and above it the +3's, which a 128 never sets. */
  public int rom() {
    return (port7ffd & 0x10) >> 4 | (port1ffd & 0x04) >> 1;
  }

  /** Bit 5 of the 128's port: once set, the ports are deaf until a reset. */
  public boolean locked() {
    return (port7ffd & 0x20) != 0;
  }

  /** The +3's all-RAM configurations, bit 0 of its own port. */
  public boolean special() {
    return (port1ffd & 0x01) != 0;
  }
}
