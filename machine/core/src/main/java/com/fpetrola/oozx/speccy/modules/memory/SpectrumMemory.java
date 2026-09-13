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


package com.fpetrola.oozx.speccy.modules.memory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.function.IntConsumer;

/**
 * The 16K RAM and ROM banks physically present, created once and outliving any single machine model:
 * models are just configurations that page the same chips differently, so neither a Machine nor a MemoryBus owns them.
 */
@Singleton
public class SpectrumMemory {
  /** 1040 KB, enough for a Pentagon 1024. */
  public static final int SPECTRUM_RAM_PAGES = 65;
  public static final int SPECTRUM_ROM_PAGES = 4;

  private final Ram[] ram = new Ram[SPECTRUM_RAM_PAGES];
  private final Rom[] rom = new Rom[SPECTRUM_ROM_PAGES];
  private Ram shown;
  private Rom absent;

  @Inject
  public SpectrumMemory(Rom.Protection protection) {
    for (int page = 0; page < SPECTRUM_RAM_PAGES; page++) {
      ram[page] = new Ram(0x4000);
      ram[page].pageNum = page;
    }
    for (int page = 0; page < SPECTRUM_ROM_PAGES; page++) {
      rom[page] = new Rom(0x4000, protection);
      rom[page].pageNum = page;
    }
  }

  public Ram ram(int page) {
    return ram[page];
  }

  public Rom rom(int page) {
    return rom[page];
  }

  /**
   * The sixteen K a machine has nothing in: every address reads as 0xff and a write goes nowhere,
   * which is what a bus with no chip on it does. One of these is enough, since it has no state.
   */
  public Rom absent() {
    if (absent == null) {
      absent = new Rom(0x4000, null);
      java.util.Arrays.fill(absent.bytes, (byte) 0xff);
    }
    return absent;
  }

  public Ram shown() {
    return shown;
  }

  /** Switches which bank is displayed and where its screen-byte writes get reported. */
  public void show(Ram bank, IntConsumer screen) {
    if (shown != null) {
      shown.shownTo = null;
    }
    shown = bank;
    bank.shownTo = screen;
  }
}
