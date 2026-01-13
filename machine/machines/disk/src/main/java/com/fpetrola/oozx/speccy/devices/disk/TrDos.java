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
package com.fpetrola.oozx.speccy.devices.disk;

import java.nio.charset.StandardCharsets;

/** TR-DOS first-track layout: 8 sectors of 16-byte directory entries, then the disk spec
 * in sector 9. */
public final class TrDos {

  public static final class Spec {
    public int firstFreeSector;
    public int firstFreeTrack;
    public int diskType;
    public int fileCount;
    public int freeSectors;
    public int id;
    public byte[] password = new byte[9];
    public int deletedFiles;
    public byte[] label = new byte[8];

    /** Returns null if the bytes given do not form a valid specification sector. */
    public static Spec read(byte[] src, int at) {
      if (src[at] != 0 || (src[at + 231] & 0xff) != 16) {
        return null;
      }
      Spec spec = new Spec();
      spec.firstFreeSector = src[at + 225] & 0xff;
      spec.firstFreeTrack = src[at + 226] & 0xff;
      spec.diskType = src[at + 227] & 0xff;
      spec.fileCount = src[at + 228] & 0xff;
      spec.freeSectors = (src[at + 229] & 0xff) + (src[at + 230] & 0xff) * 0x100;
      spec.id = src[at + 231] & 0xff;
      System.arraycopy(src, at + 234, spec.password, 0, 9);
      spec.deletedFiles = src[at + 244] & 0xff;
      System.arraycopy(src, at + 245, spec.label, 0, 8);
      return spec;
    }

    public void write(byte[] dest, int at) {
      java.util.Arrays.fill(dest, at, at + 256, (byte) 0);
      dest[at + 225] = (byte) firstFreeSector;
      dest[at + 226] = (byte) firstFreeTrack;
      dest[at + 227] = (byte) diskType;
      dest[at + 228] = (byte) fileCount;
      dest[at + 229] = (byte) freeSectors;
      dest[at + 230] = (byte) (freeSectors >> 8);
      dest[at + 231] = (byte) id;
      System.arraycopy(password, 0, dest, at + 234, 9);
      dest[at + 244] = (byte) deletedFiles;
      System.arraycopy(label, 0, dest, at + 245, 8);
    }
  }

  public static final class DirEntry {
    public byte[] filename = new byte[8];
    public int extension;
    public int param1;
    public int param2;
    public int lengthInSectors;
    public int startSector;
    public int startTrack;

    /** Returns null once the directory ends, marked by an entry with an empty name. */
    public static DirEntry read(byte[] src, int at) {
      if (src[at] == 0) {
        return null;
      }
      DirEntry entry = new DirEntry();
      System.arraycopy(src, at, entry.filename, 0, 8);
      entry.extension = src[at + 8] & 0xff;
      entry.param1 = (src[at + 9] & 0xff) + (src[at + 10] & 0xff) * 0x100;
      entry.param2 = (src[at + 11] & 0xff) + (src[at + 12] & 0xff) * 0x100;
      entry.lengthInSectors = src[at + 13] & 0xff;
      entry.startSector = src[at + 14] & 0xff;
      entry.startTrack = src[at + 15] & 0xff;
      return entry;
    }

    public void write(byte[] dest, int at) {
      System.arraycopy(filename, 0, dest, at, 8);
      dest[at + 8] = (byte) extension;
      dest[at + 9] = (byte) param1;
      dest[at + 10] = (byte) (param1 >> 8);
      dest[at + 11] = (byte) param2;
      dest[at + 12] = (byte) (param2 >> 8);
      dest[at + 13] = (byte) lengthInSectors;
      dest[at + 14] = (byte) startSector;
      dest[at + 15] = (byte) startTrack;
    }

    public String name() {
      return new String(filename, StandardCharsets.ISO_8859_1);
    }
  }

  /** Boot-relevant directory summary: presence of a "boot" file and the first BASIC program found. */
  public static final class BootInfo {
    public boolean hasBootFile;
    public int basicFiles;
    public byte[] firstBasicFile = new byte[8];
  }

  /** Reads the 8 directory sectors using the given stride, so both interleaved (TRD/SCL) and
   * sequential ("turbo") sector layouts read correctly. */
  public static BootInfo readFat(byte[] track, int sectorsAt, int sectorStride) {
    BootInfo info = new BootInfo();
    for (int sector = 0; sector < 8; sector++) {
      int at = sectorsAt + sector * sectorStride * 2;
      for (int j = 0; j < 16; j++) {
        DirEntry entry = DirEntry.read(track, at + j * 16);
        if (entry == null) {
          return info;
        }
        if ((entry.filename[0] & 0xff) > 0x01 && entry.extension == 'B') {
          if (!info.hasBootFile && entry.name().equals("boot    ")) {
            info.hasBootFile = true;
          }
          if (info.basicFiles == 0) {
            System.arraycopy(entry.filename, 0, info.firstBasicFile, 0, 8);
          }
          info.basicFiles++;
        }
      }
    }
    return info;
  }
}
