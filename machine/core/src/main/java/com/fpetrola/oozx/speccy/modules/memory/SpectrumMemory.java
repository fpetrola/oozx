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
