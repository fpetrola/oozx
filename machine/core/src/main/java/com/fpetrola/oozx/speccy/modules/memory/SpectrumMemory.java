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
 * The memories a Spectrum has inside it: sixteen kilobytes each, made once and kept, whichever
 * model is switched on. They outlast the model, which is why they are not the machine's - the
 * eight machines are configurations that take turns over one set of chips - and the bus does not
 * hold them either, because where a memory answers is a different question from what memories
 * there are.
 */
@Singleton
public class SpectrumMemory {
  public static final int SPECTRUM_RAM_PAGES = 65; // 1040 KB for Pentagon 1024
  public static final int SPECTRUM_ROM_PAGES = 4;

  private final Ram[] ram = new Ram[SPECTRUM_RAM_PAGES];
  private final Rom[] rom = new Rom[SPECTRUM_ROM_PAGES];
  private Ram shown;

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

  /** The bank the picture comes from, told here by the machine's paging. */
  public Ram shown() {
    return shown;
  }

  /** Which bank the picture comes from now, and who to tell when a byte of it changes. */
  public void show(Ram bank, IntConsumer screen) {
    if (shown != null) {
      shown.shownTo = null;
    }
    shown = bank;
    bank.shownTo = screen;
  }
}
