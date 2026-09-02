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
package com.fpetrola.oozx.speccy.devices.disk;

import java.nio.charset.StandardCharsets;

/**
 * What TR-DOS keeps on the first track: the disk's specification in sector 9, and eight sectors
 * of sixteen-byte directory entries before it. Ported from Fuse's trdos.c.
 */
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

    /** Null if these bytes are not a specification sector. */
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

    /** Null at the end of the directory, which is an entry whose name starts with nothing. */
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

  /** What the directory says about booting: whether there is a "boot" file, and the first BASIC program. */
  public static final class BootInfo {
    public boolean hasBootFile;
    public int basicFiles;
    public byte[] firstBasicFile = new byte[8];
  }

  /**
   * Reads the eight directory sectors as they lie on the track - interleaved 1, 9, 2, 10... which
   * is what a TRD or SCL image can say; a "turbo" format with them in order looks the same here.
   */
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
