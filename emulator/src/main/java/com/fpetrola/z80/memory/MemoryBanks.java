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

package com.fpetrola.z80.memory;

/**
 * The memory of a 128K machine seen through 64K: RAM banks 5 and 2 always at 4000 and 8000, the bank port 7FFD
 * chooses at C000 and the ROM its bit 4 chooses at 0000. Paging copies the C000 window out to its bank and the
 * chosen one in, so whatever reads the view as plain 64K memory keeps working; banks 5 and 2 live in their fixed
 * windows, so paging one of them at C000 copies from and back to there.
 */
public class MemoryBanks {
  public static final int WINDOW = 0xC000, SIZE = 0x4000;
  private final int[][] banks = new int[8][];
  private final int[][] roms;
  private int port;

  public MemoryBanks(int[][] roms, int[][] ram, int port, int[] view) {
    this.roms = roms;
    for (int bank = 0; bank < 8; bank++)
      banks[bank] = ram[bank] != null ? ram[bank].clone() : new int[SIZE];
    System.arraycopy(banks[5], 0, view, 0x4000, SIZE);
    System.arraycopy(banks[2], 0, view, 0x8000, SIZE);
    this.port = port & 0xff;
    load(view);
  }

  private MemoryBanks(MemoryBanks other) {
    roms = other.roms;
    for (int bank = 0; bank < 8; bank++)
      banks[bank] = other.banks[bank].clone();
    port = other.port;
  }

  public static boolean pages(int port) {
    return (port & 0x8002) == 0;
  }

  public int bank() {
    return port & 7;
  }

  public int port() {
    return port;
  }

  /** What an OUT to 7FFD does, unless an earlier one set the lock bit. */
  public void write(int value, int[] view) {
    if ((port & 0x20) != 0)
      return;
    store(view);
    port = value & 0xff;
    load(view);
  }

  /** What a bank holds now, the paged one taken from the view. */
  public int[] contents(int bank, int[] view) {
    return copyOf(view).banks[bank];
  }

  /** These banks with the contents the view holds now, for a machine that goes on from here with a view of its own. */
  public MemoryBanks copyOf(int[] view) {
    store(view);
    return new MemoryBanks(this);
  }

  private void store(int[] view) {
    System.arraycopy(view, WINDOW, banks[bank()], 0, SIZE);
    if (bank() == 5 || bank() == 2)
      System.arraycopy(view, WINDOW, view, fixedWindow(bank()), SIZE);
  }

  private void load(int[] view) {
    int bank = bank();
    System.arraycopy(bank == 5 || bank == 2 ? view : banks[bank], bank == 5 || bank == 2 ? fixedWindow(bank) : 0, view, WINDOW, SIZE);
    System.arraycopy(roms[port >> 4 & 1], 0, view, 0, SIZE);
  }

  private static int fixedWindow(int bank) {
    return bank == 5 ? 0x4000 : 0x8000;
  }
}
