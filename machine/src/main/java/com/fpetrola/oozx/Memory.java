package com.fpetrola.oozx;// Memory.java: Memory access routines
// Copyright (c) 1999-2016 Philip Kendall
// Copyright (c) 2015 Stuart Brady
// Copyright (c) 2016 Fredrick Meunier
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Author contact information:
// E-mail: philip-fuse@shadowmagic.org.uk

import com.fpetrola.oozx.fuse.GetTStatesHistory;

import java.util.*;

// Assuming ported dependencies:
// - Libspectrum (Snap, Machine, MachineCapability)
// - Display (dirty, dirtyPentagon16Col, dirtySinclair)
// - Fuse (abort)
// - Machine (FuseMachineInfo, current, ram)
// - Module (ModuleInfo, register)
// - Pentagon (pentagon1024MemoryportWrite, pentagon1024V22MemoryportWrite)
// - Spec128 (memoryportWrite)
// - SpecPlus3 (memoryport2WriteInternal)
// - StartupManager (StartupManagerModule, register)
// - Spectrum (RAM, tstates)
// - Ui (UiError, error)
// - Debugger (DebuggerMode, check)
// - Opus (active, read, write)
// - Spectranet (paged, w5100PagedA, w5100PagedB, w5100Read, w5100Write, flashRomWrite)
// - Ttx2000s (paged, sramRead, sramWrite)
// - Ula (contention)
// - Settings (SettingsInfo, current)
// - Utils (safeStrdup)
// - Beta (active)
// - Plusd (available, active)
// - Disciple (available, active)

public class Memory {

  // Constants for memory page sizes
  public static final int PAGE_SIZE_LOGARITHM = 11;
  public static final int PAGE_SIZE = 1 << PAGE_SIZE_LOGARITHM;
  public static final int PAGE_SIZE_MASK = PAGE_SIZE - 1;
  public static final int PAGES_IN_64K = 1 << (16 - PAGE_SIZE_LOGARITHM);
  public static final int PAGES_IN_16K = 1 << (14 - PAGE_SIZE_LOGARITHM);
  public static final int PAGES_IN_8K = 1 << (13 - PAGE_SIZE_LOGARITHM);
  public static final int PAGES_IN_4K = 1 << (12 - PAGE_SIZE_LOGARITHM);
  public static final int PAGES_IN_2K = 1 << (11 - PAGE_SIZE_LOGARITHM);
  public static final int PAGES_IN_14K = PAGES_IN_16K - PAGES_IN_2K;
  public static final int PAGES_IN_12K = PAGES_IN_16K - PAGES_IN_4K;

  // Maximum number of 16KB RAM and ROM pages
  public static final int SPECTRUM_RAM_PAGES = 65; // 1040 KB for Pentagon 1024
  public static final int SPECTRUM_ROM_PAGES = 4;

  // Memory sources
  public static List<String> memorySources;
  public static int sourceRom;
  public static int sourceRam;
  public static int sourceDock;
  public static int sourceExrom;
  public static int sourceAny;
  public static int sourceNone;

  // Memory mappings
  public static MemoryPage[] mapRead = new MemoryPage[PAGES_IN_64K];
  public static MemoryPage[] mapWrite = new MemoryPage[PAGES_IN_64K];
  public static MemoryPage[] mapRam = new MemoryPage[SPECTRUM_RAM_PAGES * PAGES_IN_16K];
  public static MemoryPage[] mapRom = new MemoryPage[SPECTRUM_ROM_PAGES * PAGES_IN_16K];
  private static Display display= Fuse.display;

  // Memory pool for allocated memory
  public static class MemoryPoolEntry {
    boolean persistent;
    byte[] memory;

    MemoryPoolEntry(boolean persistent, byte[] memory) {
      this.persistent = persistent;
      this.memory = memory;
    }
  }

  public static List<MemoryPoolEntry> pool = new ArrayList<>();

  // Current screen and mask
  public static int currentScreen;
  public static int screenMask;

  // Functional interface for dirty display handling
  @FunctionalInterface
  interface MemoryDisplayDirtyFn {
    void apply(int address, byte b);
  }

  public static MemoryDisplayDirtyFn displayDirty;

  // Initialize memory module

  // Clean up memory module

  // Register memory module with startup manager

//  private static void reg1() {
//    StartupManagerModule[] dependencies = {StartupManagerModule.SETUID};
//    StartupManager.register(StartupManagerModule.MEMORY, dependencies, Memory::init, null, Memory::end);
//  }

  // Register a new memory source
  public static int sourceRegister(String description) {
    String copy = description;
    memorySources.add(copy);
    return memorySources.size() - 1;
  }

  // Get the description for a given source
  public static String sourceDescription(int source) {
    return memorySources.get(source);
  }

  // Find the source for a given description
  public static int sourceFind(String description) {
    for (int i = 0; i < memorySources.size(); i++) {
      if (description.equalsIgnoreCase(memorySources.get(i))) {
        return i;
      }
    }
    return -1;
  }

  // Allocate memory from the pool
  public static byte[] poolAllocate(int length) {
    return poolAllocatePersistent(length, false);
  }

  // Allocate persistent memory from the pool
  public static byte[] poolAllocatePersistent(int length, boolean persistent) {
    byte[] memory = new byte[length];
    MemoryPoolEntry entry = new MemoryPoolEntry(persistent, memory);
    pool.add(0, entry); // Prepend to mimic GSList behavior
    return memory;
  }

  // Free non-persistent memory in the pool
  public static void poolFree() {
    pool.removeIf(entry -> !entry.persistent);
  }

  // Set contention for 16K of RAM
  public static void ramSet16kContention(int pageNum, boolean contended) {
    for (int i = 0; i < PAGES_IN_16K; i++) {
      mapRam[pageNum * PAGES_IN_16K + i].contended = contended;
    }
  }

  // Map 16K of memory
  public static void map16k(int address, MemoryPage[] source, int pageNum) {
    map16kReadWrite(address, source, pageNum, true, true);
  }

  // Map 16K of memory for reading, writing, or both
  public static void map16kReadWrite(int address, MemoryPage[] source, int pageNum, boolean mapRead, boolean mapWrite) {
    map8kReadWrite(address, source, pageNum * 2, mapRead, mapWrite);
    map8kReadWrite(address + 0x2000, source, pageNum * 2 + 1, mapRead, mapWrite);
  }

  // Map 8K of memory
  public static void map8k(int address, MemoryPage[] source, int pageNum) {
    map8kReadWrite(address, source, pageNum, true, true);
  }

  // Map 8K of memory for reading, writing, or both
  public static void map8kReadWrite(int address, MemoryPage[] source, int pageNum, boolean mapRead, boolean mapWrite) {
    map4kReadWrite(address, source, pageNum * 2, mapRead, mapWrite);
    map4kReadWrite(address + 0x1000, source, pageNum * 2 + 1, mapRead, mapWrite);
  }

  // Map 4K of memory for reading, writing, or both
  public static void map4kReadWrite(int address, MemoryPage[] source, int pageNum, boolean mapRead, boolean mapWrite) {
    map2kReadWrite(address, source, pageNum * 2, mapRead, mapWrite);
    map2kReadWrite(address + 0x0800, source, pageNum * 2 + 1, mapRead, mapWrite);
  }

  // Map 2K of memory for reading, writing, or both
  public static void map2kReadWrite(int address, MemoryPage[] source, int pageNum, boolean mapRead, boolean mapWrite) {
    for (int i = 0; i < PAGES_IN_2K; i++) {
      int pageOffset = (address >>> PAGE_SIZE_LOGARITHM) + i;
      MemoryPage page = source[pageNum * PAGES_IN_2K + i];
      if (mapRead) Memory.mapRead[pageOffset] = page;
      if (mapWrite) Memory.mapWrite[pageOffset] = page;
    }
  }

  // Map one page of memory
  public static void mapPage(MemoryPage[] source, int pageNum) {
    mapRead[pageNum] = mapWrite[pageNum] = source[pageNum];
  }

  // Page in 16K from /ROMCS
  public static void mapRomcsFull(MemoryPage[] source) {
    map16k(0x0000, source, 0);
  }

  // Page in 8K from /ROMCS
  public static void mapRomcs8k(int address, MemoryPage[] source) {
    map8k(address, source, 0);
  }

  // Page in 4K from /ROMCS
  public static void mapRomcs4k(int address, MemoryPage[] source) {
    map4kReadWrite(address, source, 0, true, true);
  }

  // Page in 2K from /ROMCS
  public static void mapRomcs2k(int address, MemoryPage[] source) {
    map2kReadWrite(address, source, 0, true, true);
  }

  // Read a byte from memory
  public static byte readByte(int address) {
    int bank = address >>> PAGE_SIZE_LOGARITHM;
    MemoryPage mapping = mapRead[bank];

    if (mapping != null) {
      if (mapping != null && mapping.contended) {
        if (Spectrum.tstates < Ula.contention.length) {
          byte tstates = Ula.contention[(int) Spectrum.tstates];
          if (tstates > 0) {
            GetTStatesHistory.addTStateUpdate(tstates, "ula readbyte", (int) Spectrum.tstates);
          }
          Spectrum.tstates += tstates;
        }
      }
//      Spectrum.tstates += 3;

      if (Opus.active && address >= 0x2800 && address < 0x3800) {
        return Opus.read(address);
      }

      if (Spectranet.paged) {
        if (Spectranet.w5100PagedA && address >= 0x1000 && address < 0x2000) {
          return Spectranet.w5100Read(mapping, address);
        }
        if (Spectranet.w5100PagedB && address >= 0x2000 && address < 0x3000) {
          return Spectranet.w5100Read(mapping, address);
        }
      }

      if (Ttx2000s.paged && address >= 0x2000 && address < 0x4000) {
        return Ttx2000s.sramRead(address);
      }

      return mapping.getPage().get(address & PAGE_SIZE_MASK);
    } else return 0;
  }

  // Write a byte to memory
  public static void writeByte(int address, byte b) {
    int bank = address >>> PAGE_SIZE_LOGARITHM;
    MemoryPage mapping = mapWrite[bank];

    if (mapping.contended) {
      byte tstates = Ula.contention[(int) Spectrum.tstates];
      if (tstates > 0) {
        GetTStatesHistory.addTStateUpdate(tstates, "ula writebyte", (int) Spectrum.tstates);
      }
      Spectrum.tstates += tstates;
    }
//      Spectrum.tstates += 3;

//    writeByteInternal(address, b);
  }

  // Handle dirty display for Sinclair mode
  public static void displayDirtySinclair(int address, byte b) {
    int bank = address >>> PAGE_SIZE_LOGARITHM;
    MemoryPage mapping = mapWrite[bank];
    int offset = address & PAGE_SIZE_MASK;
    ArrayPointer memory = mapping.getPage();

    int offset2 = offset + mapping.offset;

    if (mapping.source == sourceRam &&
        mapping.pageNum == currentScreen &&
        (offset2 & screenMask) < 0x1b00 &&
        memory.get(offset) != b) {
      display.dirty.apply(offset2);
    }
  }

  // Write a byte to memory (internal)
  public static void writeByteInternal(int address, byte b) {
    int bank = address >>> PAGE_SIZE_LOGARITHM;
    MemoryPage mapping = mapWrite[bank];

    if (Spectranet.paged) {
      Spectranet.flashRomWrite(address, b);
      if (Spectranet.w5100PagedA && address >= 0x1000 && address < 0x2000) {
        Spectranet.w5100Write(mapping, address, b);
        return;
      }
      if (Spectranet.w5100PagedB && address >= 0x2000 && address < 0x3000) {
        Spectranet.w5100Write(mapping, address, b);
        return;
      }
    }

    if (Ttx2000s.paged && address >= 0x2000 && address < 0x4000) {
      Ttx2000s.sramWrite(address, b);
      return;
    }

    if (Opus.active && address >= 0x2800 && address < 0x3800) {
      Opus.write(address, b);
    } else if (mapping.writable || (mapping.source != sourceNone && Settings.current.writableRoms)) {
      int offset = address & PAGE_SIZE_MASK;
      ArrayPointer memory = mapping.getPage();

      displayDirty.apply(address, b);
      memory.set(offset,  b);
    }
  }

  // Map ROMCS
  public static void romcsMap() {
    if (!Machine.current.ramInfo.romcs) return;
    Module.romcs();
  }

  // Load memory from snapshot

  // Check if custom ROMs are loaded
  public static boolean customRom() {
    for (int i = 0; i < SPECTRUM_ROM_PAGES * PAGES_IN_16K; i++) {
      if (mapRom[i].saveToSnapshot) return true;
    }
    return false;
  }

  // Reset custom ROM flags
  public static void reset() {
    for (int i = 0; i < SPECTRUM_ROM_PAGES * PAGES_IN_16K; i++) {
      mapRom[i] = new MemoryPage();
      mapRom[i].saveToSnapshot = false;
    }
  }

  // Save ROMs to snapshot
  public static void romToSnapshot(Libspectrum.Snap snap) {
    if (!customRom()) return;

    Libspectrum.snapSetCustomRom(snap, true);

    byte[] currentRom = null;
    int currentPageNum = -1;
    int currentRomNum = 0;
    int romLength = 0;

    for (int i = 0; i < SPECTRUM_ROM_PAGES * PAGES_IN_16K; i++) {
      if (mapRom[i].getPage() != null) {
        if (currentPageNum != mapRom[i].pageNum) {
          if (currentRom != null) {
            Libspectrum.snapSetRoms(snap, currentRomNum, currentRom);
            Libspectrum.snapSetRomLength(snap, currentRomNum, romLength);
            currentRomNum++;
            currentRom = null;
          }

          romLength = PAGE_SIZE;
          currentRom = new byte[romLength];
          System.arraycopy(mapRom[i].getPage(), 0, currentRom, 0, PAGE_SIZE);
          currentPageNum = mapRom[i].pageNum;
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

  // Save memory to snapshot

//    // Check if in the right ROM for tape or other traps
//    public static boolean trapCheckRom(TrapType type) {
//        if (Plusd.available && Plusd.active) return false;
//        if (Disciple.available && Disciple.active) return false;
//        if (Opus.active) return false;
//
//        switch (Machine.current.machine) {
////            case _16:
//            case _48:
////            case _48_NTSC:
////            case SE:
////            case TC2048:
//                return true;
//
////            case TC2068:
////            case TS2068:
////                if (type == TrapType.TAPE_ROM) {
////                    return mapRead[0].source == sourceExrom;
////                } else {
////                    return Machine.current.ram.currentRom == 0;
////                }
////
////            case _128:
////            case PLUS2:
////                return Machine.current.ram.currentRom == 1;
////
////            case PLUS2A:
////            case PLUS3:
////            case PLUS3E:
////                return !Machine.current.ram.special && Machine.current.ram.currentRom == 3;
////
////            case _128E:
////                return !Machine.current.ram.special &&
////                        (Machine.current.ram.currentRom == 1 || Machine.current.ram.currentRom == 3);
////
////            case PENT:
////            case PENT512:
////            case PENT1024:
////            case SCORP:
////                return Machine.current.ram.currentRom == 1 && !Beta.active;
//
//            case UNKNOWN:
//            default:
//                Ui.error(UiError.ERROR, "trap_check_rom: unknown machine type %s",
//                        Machine.current.machine.ordinal());
//                Fuse.abort();
//                return false;
//        }
//    }
}

enum TrapType {
  TAPE_ROM,
  _48K_ROM
}

