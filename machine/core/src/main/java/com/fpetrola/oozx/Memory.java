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

package com.fpetrola.oozx;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.fuse.machine.RamInfo;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.Ula;
import com.fpetrola.oozx.fuse.modules.ZxModule;
import com.fpetrola.z80.helpers.Helper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class Memory extends DefaultRAMHolder implements ZxModule {

  // Constants for memory page sizes
  public final int PAGE_SIZE_LOGARITHM = 11;
  public final int PAGE_SIZE = 1 << PAGE_SIZE_LOGARITHM;
  public final int PAGE_SIZE_MASK = PAGE_SIZE - 1;
  public final int PAGES_IN_64K = 1 << (16 - PAGE_SIZE_LOGARITHM);
  public final int PAGES_IN_16K = 1 << (14 - PAGE_SIZE_LOGARITHM);
  public final int PAGES_IN_8K = 1 << (13 - PAGE_SIZE_LOGARITHM);
  public final int PAGES_IN_4K = 1 << (12 - PAGE_SIZE_LOGARITHM);
  public final int PAGES_IN_2K = 1 << (11 - PAGE_SIZE_LOGARITHM);
  public final int PAGES_IN_14K = PAGES_IN_16K - PAGES_IN_2K;
  public final int PAGES_IN_12K = PAGES_IN_16K - PAGES_IN_4K;

  // Maximum number of 16KB RAM and ROM pages
  public static final int SPECTRUM_RAM_PAGES = 65; // 1040 KB for Pentagon 1024
  public final int SPECTRUM_ROM_PAGES = 4;

  // Memory sources
  public List<String> memorySources;
  public int sourceRom;
  public int sourceRam;
  public int sourceDock;
  public int sourceExrom;
  public int sourceAny;
  public int sourceNone;

  // Memory mappings
  public final MemoryPage[] mapRead = new MemoryPage[PAGES_IN_64K];
  public final MemoryPage[] mapWrite = new MemoryPage[PAGES_IN_64K];
  public final MemoryPage[] mapRam = new MemoryPage[SPECTRUM_RAM_PAGES * PAGES_IN_16K];
  public final MemoryPage[] mapRom = new MemoryPage[SPECTRUM_ROM_PAGES * PAGES_IN_16K];
  private final SpectrumZ80Clock zxClock;
  private final Module module;
  private final Settings settings;

@Inject
  public Memory(SpectrumZ80Clock zxClock, Module module, Settings settings) {
    this.zxClock = zxClock;
    this.module = module;
    this.settings = settings;
    Helper.fillArrayWith(mapRead, MemoryPage::new);
    Helper.fillArrayWith(mapWrite, MemoryPage::new);
  }

  public int init(Object initContext) {
    memorySources = new ArrayList<>();

    sourceRom = sourceRegister("ROM");
    sourceRam = sourceRegister("RAM");
    sourceDock = sourceRegister("Timex Dock");
    sourceExrom = sourceRegister("Timex EXROM");
    sourceAny = sourceRegister("Absolute address");
    sourceNone = sourceRegister("None");

    pool = new ArrayList<>();

    for (int i = 0; i < SPECTRUM_ROM_PAGES; i++) {
      for (int j = 0; j < PAGES_IN_16K; j++) {
        MemoryPage page = mapRom[i * PAGES_IN_16K + j] = new MemoryPage();
        page.setWritable(false);
        page.contended = false;
        page.source = sourceRom;
      }
    }

    for (int i = 0; i < SPECTRUM_RAM_PAGES; i++) {
      for (int j = 0; j < PAGES_IN_16K; j++) {
        MemoryPage page = mapRam[i * PAGES_IN_16K + j] = new MemoryPage();
        page.offset = j * PAGE_SIZE;
        page.setPage(getRAM(), i);
        page.setPageNum(i);
        page.source = sourceRam;
        page.setWritable(true);
      }
    }

    return 0;
  }

  public void end() {
    if (pool != null) {
      pool.clear();
      pool = null;
    }

    if (memorySources != null) {
      memorySources.clear();
      memorySources = null;
    }
  }

  // Memory pool for allocated memory
  public class MemoryPoolEntry {
    public boolean persistent;
    public byte[] memory;

    MemoryPoolEntry(boolean persistent, byte[] memory) {
      this.persistent = persistent;
      this.memory = memory;
    }
  }

  public List<MemoryPoolEntry> pool = new ObjectArrayList<>();

  // Current screen and mask
  public int currentScreen;
  public int screenMask;

  // Register a new memory source
  public int sourceRegister(String description) {
    memorySources.add(description);
    return memorySources.size() - 1;
  }

  // Get the description for a given source
  public String sourceDescription(int source) {
    return memorySources.get(source);
  }

  // Find the source for a given description
  public int sourceFind(String description) {
    for (int i = 0; i < memorySources.size(); i++) {
      if (description.equalsIgnoreCase(memorySources.get(i))) {
        return i;
      }
    }
    return -1;
  }

  // Allocate memory from the pool
  public byte[] poolAllocate(int length) {
    return poolAllocatePersistent(length, false);
  }

  // Allocate persistent memory from the pool
  public byte[] poolAllocatePersistent(int length, boolean persistent) {
    byte[] memory = new byte[length];
    MemoryPoolEntry entry = new MemoryPoolEntry(persistent, memory);
    pool.add(0, entry); // Prepend to mimic GSList behavior
    return memory;
  }

  // Free non-persistent memory in the pool
  public void poolFree() {
    pool.removeIf(entry -> !entry.persistent);
  }

  // Set contention for 16K of RAM
  public void ramSet16kContention(int pageNum, boolean contended) {
    for (int i = 0; i < PAGES_IN_16K; i++) {
      mapRam[pageNum * PAGES_IN_16K + i].contended = contended;
    }
  }

  // Map 16K of memory
  public void map16k(int address, MemoryPage[] source, int pageNum) {
    map16kReadWrite(address, source, pageNum, true, true);
  }

  // Map 16K of memory for reading, writing, or both
  public void map16kReadWrite(int address, MemoryPage[] source, int pageNum, boolean mapRead, boolean mapWrite) {
    map8kReadWrite(address, source, pageNum * 2, mapRead, mapWrite);
    map8kReadWrite(address + 0x2000, source, pageNum * 2 + 1, mapRead, mapWrite);
  }

  // Map 8K of memory
  public void map8k(int address, MemoryPage[] source, int pageNum) {
    map8kReadWrite(address, source, pageNum, true, true);
  }

  // Map 8K of memory for reading, writing, or both
  public void map8kReadWrite(int address, MemoryPage[] source, int pageNum, boolean mapRead, boolean mapWrite) {
    map4kReadWrite(address, source, pageNum * 2, mapRead, mapWrite);
    map4kReadWrite(address + 0x1000, source, pageNum * 2 + 1, mapRead, mapWrite);
  }

  // Map 4K of memory for reading, writing, or both
  public void map4kReadWrite(int address, MemoryPage[] source, int pageNum, boolean mapRead, boolean mapWrite) {
    map2kReadWrite(address, source, pageNum * 2, mapRead, mapWrite);
    map2kReadWrite(address + 0x0800, source, pageNum * 2 + 1, mapRead, mapWrite);
  }

  // Map 2K of memory for reading, writing, or both
  public void map2kReadWrite(int address, MemoryPage[] source, int pageNum, boolean isMapRead, boolean isMapWrite) {
    for (int i = 0; i < PAGES_IN_2K; i++) {
      int pageOffset = (address >>> PAGE_SIZE_LOGARITHM) + i;
      MemoryPage page = source[pageNum * PAGES_IN_2K + i];
      if (isMapRead) mapRead[pageOffset] = page;
      if (isMapWrite) mapWrite[pageOffset] = page;
    }
  }

  // Map one page of memory
  public void mapPage(MemoryPage[] source, int pageNum) {
    mapRead[pageNum] = mapWrite[pageNum] = source[pageNum];
  }

  // Page in 16K from /ROMCS
  public void mapRomcsFull(MemoryPage[] source) {
    map16k(0x0000, source, 0);
  }

  // Page in 8K from /ROMCS
  public void mapRomcs8k(int address, MemoryPage[] source) {
    map8k(address, source, 0);
  }

  // Page in 4K from /ROMCS
  public void mapRomcs4k(int address, MemoryPage[] source) {
    map4kReadWrite(address, source, 0, true, true);
  }

  // Page in 2K from /ROMCS
  public void mapRomcs2k(int address, MemoryPage[] source) {
    map2kReadWrite(address, source, 0, true, true);
  }

  // Read a byte from memory
  public void readByte(int address, Ula ula) {
    if (mapRead[address >>> PAGE_SIZE_LOGARITHM].contended) {
      zxClock.addTStates(ula.contention[zxClock.getTStates()], "ula readbyte");
    }
//      tStatesHolder.tstates += 3;
//    mapping.getPage().get(address & PAGE_SIZE_MASK);
  }

  public int readByteInternal(final int address) {
    final MemoryPage mapping = mapRead[address >>> PAGE_SIZE_LOGARITHM];
    return mapping.get(address & PAGE_SIZE_MASK);
  }

  // Write a byte to memory
  public void writeByte(final int address, final byte b, final Ula ula, final Display display) {
    MemoryPage memoryPage = mapWrite[address >>> PAGE_SIZE_LOGARITHM];
    if (memoryPage.contended) {
      zxClock.addTStates(ula.contention[zxClock.getTStates()], "ula writebyte");
    }
    writeByteInternal(address, b, display, memoryPage);
  }

  private void writeByteInternal(final int address, final byte value, final Display display, final MemoryPage memoryPage) {
    if (memoryPage.isWritable() || (memoryPage.source != sourceNone && settings.current.writableRoms)) {
      final int addressMasked = address & PAGE_SIZE_MASK;
      displayDirtySinclair(addressMasked, value, display, memoryPage);
      memoryPage.set(addressMasked, value);
    }
  }

  // Write a byte to memory (internal)
  public void writeByteInternal(int address, byte b, Display display) {
    writeByteInternal(address, b, display, mapWrite[address >>> PAGE_SIZE_LOGARITHM]);
  }

  // Handle dirty display for Sinclair mode
  public void displayDirtySinclair(final int address, final byte value, final Display display, final MemoryPage mapping) {
    final int offset2 = address + mapping.offset;

    if (mapping.source == sourceRam && mapping.getPageNum() == currentScreen && (offset2 & screenMask) < 0x1b00 && mapping.get(address) != value) {
      display.dirtySinclair(offset2);
    }
  }

  public void writeByteInternal2(int address, byte b) {
    MemoryPage mapping = mapWrite[address >>> PAGE_SIZE_LOGARITHM];
    if (mapping.isWritable() || (mapping.source != sourceNone && settings.current.writableRoms)) {
      mapping.set(address & PAGE_SIZE_MASK, b);
    }
  }

  // Map ROMCS
  public void romcsMap(RamInfo ramInfo) {
    if (!ramInfo.romcs) return;
    module.romcs();
  }

  // Load memory from snapshot

  // Check if custom ROMs are loaded
  public boolean customRom() {
    for (int i = 0; i < SPECTRUM_ROM_PAGES * PAGES_IN_16K; i++) {
      if (mapRom[i].saveToSnapshot) return true;
    }
    return false;
  }

  // Reset custom ROM flags
  public void reset() {
    for (int i = 0; i < SPECTRUM_ROM_PAGES * PAGES_IN_16K; i++) {
      mapRom[i] = new MemoryPage();
      mapRom[i].saveToSnapshot = false;
    }
  }

  // Save ROMs to snapshot
  public void romToSnapshot(Libspectrum.Snap snap) {
    if (!customRom()) return;

    Libspectrum.snapSetCustomRom(snap, true);

    byte[] currentRom = null;
    int currentPageNum = -1;
    int currentRomNum = 0;
    int romLength = 0;

    for (int i = 0; i < SPECTRUM_ROM_PAGES * PAGES_IN_16K; i++) {
      if (mapRom[i].getPage() != null) {
        if (currentPageNum != mapRom[i].getPageNum()) {
          if (currentRom != null) {
            Libspectrum.snapSetRoms(snap, currentRomNum, currentRom);
            Libspectrum.snapSetRomLength(snap, currentRomNum, romLength);
            currentRomNum++;
            currentRom = null;
          }

          romLength = PAGE_SIZE;
          currentRom = new byte[romLength];
          System.arraycopy(mapRom[i].getPage(), 0, currentRom, 0, PAGE_SIZE);
          currentPageNum = mapRom[i].getPageNum();
        } else {
          byte[] newRom = new byte[romLength + PAGE_SIZE];
          System.arraycopy(currentRom, 0, newRom, 0, romLength);
          System.arraycopy(mapRom[i].getPage(), 0, newRom, romLength, PAGE_SIZE);
          currentRom = newRom;
          romLength += PAGE_SIZE;
        }
      }
    }

    if (currentRom != null) {
      Libspectrum.snapSetRoms(snap, currentRomNum, currentRom);
      Libspectrum.snapSetRomLength(snap, currentRomNum, romLength);
      currentRomNum++;
    }

    Libspectrum.snapSetCustomRomPages(snap, currentRomNum);
  }
}