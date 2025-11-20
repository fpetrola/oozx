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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;

/**
 * A floppy disk as the drive sees it: every track a stream of bytes with three bits beside each
 * one - whether it was written with a missing clock (an address mark), whether it is FM or MFM,
 * and whether it is weak (copy protection that reads differently every time).
 * <p>
 * An image file is one of many ways of writing that down. The sector formats - MGT, IMG, OPD, TRD -
 * only keep the sectors' contents, so the track is made up around them from the gap table of the
 * system that formatted it; the raw formats keep the track as it was.
 * <p>
 * The track's bytes, clock marks, FM marks and weak marks live in one
 * array, laid out per track, so a UDI image is that array and nothing else.
 */
public class Disk {

  public enum Type { NONE, UDI, FDI, TD0, MGT, IMG, SAD, CPC, ECPC, TRD, SCL, OPD, D40, D80, LOG }

  /** How long a track is, in bytes, which is what the density comes to. */
  public enum Density {
    AUTO(6250), SD8(5208), DD8(10416), SD(3125), DD(6250), DD_PLUS(6500), HD(12500);

    public final int bytesPerTrack;

    Density(int bytesPerTrack) {
      this.bytesPerTrack = bytesPerTrack;
    }
  }

  /** The gaps and marks a system writes between the sectors it formats. */
  public record Gap(int gap, int sync, int syncLen, int mark, int[] len) {
  }

  public static final int GAP_MGT_PLUSD = 0;
  public static final int GAP_TRDOS = 1;
  public static final int GAP_IBM3740 = 2;
  public static final int GAP_IBM34 = 3;
  public static final int GAP_MINIMAL_FM = 4;
  public static final int GAP_MINIMAL_MFM = 5;
  public static final int GAP_4K765_FM = 6;
  public static final int GAP_8K765_MFM = 7;
  public static final int GAP_CUSTOM_FM = 8;
  public static final int GAP_CUSTOM_MFM = 9;

  static final Gap[] GAPS = {
      new Gap(0x4e, 0x00, 12, 0xa1, new int[] {0, 60, 22, 24}),
      new Gap(0x4e, 0x00, 12, 0xa1, new int[] {0, 10, 22, 60}),
      new Gap(0xff, 0x00, 6, -1, new int[] {40, 26, 11, 27}),
      new Gap(0x4e, 0x00, 12, 0xa1, new int[] {80, 50, 22, 54}),
      new Gap(0xff, 0x00, 6, -1, new int[] {0, 16, 11, 10}),
      new Gap(0x4e, 0x00, 12, 0xa1, new int[] {0, 32, 22, 24}),
      new Gap(0xff, 0x00, 6, -1, new int[] {8, 8, 11, 10}),
      new Gap(0x4e, 0x00, 12, 0xa1, new int[] {16, 16, 22, 24}),
      new Gap(0xff, 0x00, 6, -1, new int[] {0, 0, 0, 0}),
      new Gap(0x4e, 0x00, 12, 0xa1, new int[] {0, 0, 0, 0}),
  };

  static final int NO_INTERLEAVE = 1;
  static final int INTERLEAVE_2 = 2;
  static final int INTERLEAVE_OPUS = 13;
  static final int NO_AUTOFILL = -1;

  public static final int FLAG_NONE = 0;
  public static final int FLAG_PLUS3_CPC = 1;
  public static final int FLAG_OPEN_DS = 2;

  public String filename;
  public int sides;
  public int cylinders;
  public int bpt;
  public boolean wrprot;
  public boolean dirty;
  public boolean haveWeak;
  public int flag;
  public Type type = Type.NONE;
  public Density density = Density.AUTO;

  /** Every track, one after the other: a header of four bytes, the bytes, and the three bit planes. */
  public byte[] data;
  int tlen;

  /** The track the head is over: where its bytes start in {@link #data}, and the three planes after them. */
  int track = -1;
  int clocks;
  int fm;
  int weak;
  /** How long this track is, which can differ from {@link #bpt} in a raw image. */
  public int cBpt;
  /** Where in the track the head is. */
  public int i;

  static int clen(int bpt) {
    return bpt / 8 + (bpt % 8 != 0 ? 1 : 0);
  }

  public boolean hasTrack() {
    return track >= 0;
  }

  int trackByte(int at) {
    return data[track + at] & 0xff;
  }

  void setTrackByte(int at, int value) {
    data[track + at] = (byte) value;
  }

  boolean bit(int plane, int at) {
    return (data[plane + at / 8] & (1 << (at % 8))) != 0;
  }

  void setBit(int plane, int at, boolean on) {
    if (on) {
      data[plane + at / 8] |= (byte) (1 << (at % 8));
    } else {
      data[plane + at / 8] &= (byte) ~(1 << (at % 8));
    }
  }

  boolean clock(int at) {
    return bit(clocks, at);
  }

  boolean fmMark(int at) {
    return bit(fm, at);
  }

  boolean weakMark(int at) {
    return bit(weak, at);
  }

  /** Puts the head over a track, by its number among all of them. */
  public void setTrackIdx(int idx) {
    track = 3 + idx * tlen;
    cBpt = (data[track - 3] & 0xff) + 256 * (data[track - 2] & 0xff);
    clocks = track + cBpt;
    fm = clocks + clen(cBpt);
    weak = fm + clen(cBpt);
  }

  public void setTrack(int head, int cylinder) {
    setTrackIdx(sides * cylinder + head);
  }

  /** Off every track: what a drive does when the head is over nothing it can read. */
  public void noTrack() {
    track = -1;
  }

  private int trackType(int idx) {
    return data[3 + idx * tlen - 1] & 0xff;
  }

  private void setTrackType(int idx, int type) {
    data[3 + idx * tlen - 1] = (byte) type;
  }

  private void alloc() throws DiskException {
    if (density != Density.AUTO) {
      bpt = density.bytesPerTrack;
    } else if (bpt > 12500) {
      throw new DiskException("unsupported track length " + bpt);
    } else if (bpt > 10416) {
      density = Density.HD;
      bpt = density.bytesPerTrack;
    } else if (bpt > 6500) {
      density = Density.DD8;
      bpt = density.bytesPerTrack;
    } else if (bpt > 6250) {
      density = Density.DD_PLUS;
      bpt = density.bytesPerTrack;
    } else if (bpt > 5208) {
      density = Density.DD;
      bpt = density.bytesPerTrack;
    } else if (bpt > 3125) {
      density = Density.SD8;
      bpt = density.bytesPerTrack;
    } else if (bpt > 0) {
      density = Density.SD;
      bpt = density.bytesPerTrack;
    }
    if (bpt > 0) {
      tlen = 4 + bpt + 3 * clen(bpt);
    }
    int length = sides * cylinders * tlen;
    if (length == 0) {
      throw new DiskException("invalid disk geometry");
    }
    data = new byte[length];
    updateTrackLengths();
  }

  private void updateTrackLengths() {
    for (int idx = 0; idx < sides * cylinders; idx++) {
      int at = 3 + idx * tlen;
      if ((data[at - 3] & 0xff) + 256 * (data[at - 2] & 0xff) == 0) {
        data[at - 3] = (byte) bpt;
        data[at - 2] = (byte) (bpt >> 8);
      }
    }
  }

  /** An unformatted disk: nothing on it but its shape. */
  public static Disk blank(int sides, int cylinders, Density density, Type type) throws DiskException {
    if (type == Type.NONE || sides < 1 || sides > 2 || cylinders < 35 || cylinders > 83) {
      throw new DiskException("invalid disk geometry");
    }
    Disk disk = new Disk();
    disk.type = type;
    disk.density = density == Density.AUTO ? Density.DD : density;
    disk.sides = sides;
    disk.cylinders = cylinders;
    disk.alloc();
    disk.wrprot = false;
    disk.dirty = true;
    return disk;
  }

  // ---- reading the marks on a track, for the image writers and the geometry guesser

  /** The next ID on the track from where the head is; answers {track, head, sector, length code} or null. */
  private int[] idRead() {
    boolean a1mark = false;
    while (i < cBpt) {
      if (trackByte(i) == 0xa1 && clock(i)) {
        a1mark = true;
      } else if (trackByte(i) == 0xfe && (clock(i) || a1mark)) {
        i++;
        int[] id = {trackByte(i), trackByte(i + 1), trackByte(i + 2), trackByte(i + 3)};
        i += 4;
        i += 2;
        return id;
      } else {
        a1mark = false;
      }
      i++;
    }
    return null;
  }

  /** Whether a data mark follows; answers 0 for a normal one, 1 for a deleted one, -1 for none. */
  private int datamarkRead() {
    boolean a1mark = false;
    while (i < cBpt) {
      int b = trackByte(i);
      if (b == 0xa1 && clock(i)) {
        a1mark = true;
      } else if (b >= 0xf8 && b <= 0xfe && (clock(i) || a1mark)) {
        i++;
        return b == 0xf8 ? 1 : 0;
      } else {
        a1mark = false;
      }
      i++;
    }
    return -1;
  }

  private boolean idSeek(int sector) {
    i = 0;
    int[] id;
    while ((id = idRead()) != null) {
      if (id[2] == sector) {
        return true;
      }
    }
    return false;
  }

  static final int ID_NOTMATCH = 1;
  static final int SECLEN_VARI = 2;
  static final int SPT_VARI = 4;
  static final int SBASE_VARI = 8;
  static final int MFM_VARI = 16;
  static final int DDAM = 32;
  static final int CORRUPT_SECTOR = 64;
  static final int UNFORMATTED_TRACK = 128;
  static final int FM_DATA = 256;
  static final int WEAK_DATA = 512;

  /** What one track is made of: {sectorBase, sectors, seclen, mfm}, and the oddities as flags. */
  private int guessTrackGeom(int head, int cylinder, int[] geom) {
    int r = 0;
    geom[0] = -1;
    geom[1] = 0;
    geom[2] = -1;
    geom[3] = -1;
    setTrack(head, cylinder);
    i = 0;
    int[] id;
    while ((id = idRead()) != null) {
      if (geom[0] == -1) geom[0] = id[2];
      if (geom[2] == -1) geom[2] = id[3];
      if (geom[3] == -1) geom[3] = trackByte(i) == 0x4e ? 1 : 0;
      int del = datamarkRead();
      if (del < 0) r |= CORRUPT_SECTOR;
      if (id[0] != cylinder) r |= ID_NOTMATCH;
      if (id[2] < geom[0]) geom[0] = id[2];
      if (id[3] != geom[2]) {
        r |= SECLEN_VARI;
        if (id[3] > geom[2]) geom[2] = id[3];
      }
      if (del > 0) r |= DDAM;
      geom[1]++;
    }
    return r;
  }

  private void updateTracksMode() {
    for (int idx = 0; idx < cylinders * sides; idx++) {
      setTrackIdx(idx);
      int mfm = 0, fmBits = 0, weakBits = 0;
      for (int j = clen(cBpt) - 1; j >= 0; j--) {
        mfm |= ~data[fm + j] & 0xff;
        fmBits |= data[fm + j] & 0xff;
        weakBits |= data[weak + j] & 0xff;
      }
      int type = 0;
      if (mfm != 0 && fmBits == 0) type = 0x00;
      if (mfm == 0 && fmBits != 0) type = 0x01;
      if (mfm != 0 && fmBits != 0) type = 0x02;
      if (weakBits != 0) {
        type |= 0x80;
        haveWeak = true;
      }
      setTrackType(idx, type);
    }
  }

  /** The whole disk's shape: {sectorBase, sectors, seclen, mfm, unformattedFrom}, with flags for what varies. */
  private int checkDiskGeom(int[] geom) {
    setTrackIdx(0);
    i = 0;
    Arrays.fill(geom, -1);
    int r = 0;
    int[] one = new int[4];
    for (int t = 0; t < cylinders; t++) {
      for (int h = 0; h < sides; h++) {
        int type = trackType(sides * t + h);
        r |= (type & 0x80) != 0 ? WEAK_DATA : 0;
        r |= (type & 0x03) == 0x02 ? MFM_VARI : 0;
        r |= (type & 0x03) == 0x01 ? FM_DATA : 0;
        r |= guessTrackGeom(h, t, one);
        if (geom[0] == -1) geom[0] = one[0];
        if (geom[1] == -1) geom[1] = one[1];
        if (geom[2] == -1) geom[2] = one[2];
        if (geom[3] == -1) geom[3] = one[3];
        if (one[0] == -1) {
          if (geom[4] == -1 && h > 0) geom[4] = -2;
          if (geom[4] == -1) geom[4] = t;
          continue;
        }
        if (geom[4] > -1) geom[4] = -2;
        if (one[0] != geom[0]) {
          r |= SBASE_VARI;
          if (one[0] < geom[0]) geom[0] = one[0];
        }
        if (one[1] != geom[1]) {
          r |= SPT_VARI;
          if (one[1] > geom[1]) geom[1] = one[1];
        }
        if (one[2] != geom[2]) {
          r |= SECLEN_VARI;
          if (one[2] > geom[2]) geom[2] = one[2];
        }
        if (one[3] != geom[3]) {
          r |= MFM_VARI;
          geom[3] = 1;
        }
      }
    }
    if (geom[4] == -2) {
      r |= UNFORMATTED_TRACK;
      geom[4] = -1;
    }
    return r;
  }

  // ---- making a track up around its sectors

  /** Answers whether the gap fitted on what is left of the track. */
  private boolean wroteGap(int gap, int gaptype) {
    Gap g = GAPS[gaptype];
    if (i + g.len[gap] >= cBpt) {
      return false;
    }
    Arrays.fill(data, track + i, track + i + g.len[gap], (byte) g.gap);
    i += g.len[gap];
    return true;
  }

  private int preindexLen(int gaptype) {
    Gap g = GAPS[gaptype];
    return g.len[0] + g.syncLen + (g.mark >= 0 ? 3 : 0) + 1;
  }

  private void sync(Gap g) {
    Arrays.fill(data, track + i, track + i + g.syncLen, (byte) g.sync);
    i += g.syncLen;
    if (g.mark >= 0) {
      for (int n = 0; n < 3; n++) {
        setTrackByte(i, g.mark);
        setBit(clocks, i, true);
        i++;
      }
    }
  }

  private boolean wrotePreindex(int gaptype) {
    Gap g = GAPS[gaptype];
    if (i + preindexLen(gaptype) >= cBpt || !wroteGap(0, gaptype)) {
      return false;
    }
    sync(g);
    if (g.mark < 0) {
      setBit(clocks, i, true);
    }
    setTrackByte(i++, 0xfc);
    return true;
  }

  private boolean wrotePostindex(int gaptype) {
    return wroteGap(1, gaptype);
  }

  private boolean wroteGap4(int gaptype) {
    int len = cBpt - i;
    if (len < 0) {
      return false;
    }
    Arrays.fill(data, track + i, track + cBpt, (byte) GAPS[gaptype].gap);
    i = cBpt;
    return true;
  }

  private boolean wroteId(int h, int t, int s, int l, int gaptype, boolean crcError) {
    Gap g = GAPS[gaptype];
    if (i + g.syncLen + (g.mark >= 0 ? 3 : 0) + 7 >= cBpt) {
      return false;
    }
    int crc = 0xffff;
    sync(g);
    if (g.mark >= 0) {
      for (int n = 0; n < 3; n++) crc = Crc.fdc(crc, g.mark);
    } else {
      setBit(clocks, i, true);
    }
    setTrackByte(i++, 0xfe);
    crc = Crc.fdc(crc, 0xfe);
    for (int b : new int[] {t, h, s, l}) {
      setTrackByte(i++, b);
      crc = Crc.fdc(crc, b);
    }
    setTrackByte(i++, crc >> 8);
    setTrackByte(i++, crcError ? ~crc & 0xff : crc & 0xff);
    return wroteGap(2, gaptype);
  }

  private boolean wroteDatamark(boolean ddam, int gaptype) {
    Gap g = GAPS[gaptype];
    if (i + g.len[2] + g.syncLen + (g.mark >= 0 ? 3 : 0) + 1 >= cBpt) {
      return false;
    }
    sync(g);
    if (g.mark < 0) {
      setBit(clocks, i, true);
    }
    setTrackByte(i++, ddam ? 0xf8 : 0xfb);
    return true;
  }

  /**
   * A sector's data, from an image being read (buffer, at from) or from bytes given; padded with
   * autofill when the image runs short, unless autofill is negative.
   */
  private boolean wroteData(byte[] buffer, int[] from, byte[] given, int len, boolean ddam, int gaptype,
                            boolean crcError, int autofill) {
    return wroteData(buffer, from, given, len, ddam, gaptype, crcError, autofill, null);
  }

  /** {@code dataStart}, when given, is told where on the track the sector's first byte went. */
  private boolean wroteData(byte[] buffer, int[] from, byte[] given, int len, boolean ddam, int gaptype,
                            boolean crcError, int autofill, int[] dataStart) {
    Gap g = GAPS[gaptype];
    if (!wroteDatamark(ddam, gaptype)) {
      return false;
    }
    if (dataStart != null) {
      dataStart[0] = i;
    }
    int crc = 0xffff;
    if (g.mark >= 0) {
      for (int n = 0; n < 3; n++) crc = Crc.fdc(crc, g.mark);
    }
    crc = Crc.fdc(crc, ddam ? 0xf8 : 0xfb);
    if (len >= 0) {
      if (i + len + 2 >= cBpt) {
        return false;
      }
      int length;
      if (buffer == null) {
        System.arraycopy(given, 0, data, track + i, len);
        length = len;
      } else {
        length = Math.min(buffer.length - from[0], len);
        System.arraycopy(buffer, from[0], data, track + i, length);
        from[0] += length;
      }
      if (length < len) {
        if (autofill < 0) {
          return false;
        }
        Arrays.fill(data, track + i + length, track + i + len, (byte) autofill);
      }
      for (int n = 0; n < len; n++) {
        crc = Crc.fdc(crc, trackByte(i));
        i++;
      }
      if (crcError) crc ^= 1;
      setTrackByte(i++, crc >> 8);
      setTrackByte(i++, crc & 0xff);
    }
    return wroteGap(3, gaptype);
  }

  private static int calcSectorLen(boolean mfm, int sectorLength, int gaptype) {
    Gap g = GAPS[gaptype];
    return g.syncLen + (g.mark >= 0 ? 3 : 0) + 7 + g.len[2]
        + g.syncLen + (g.mark >= 0 ? 3 : 0) + 1 + sectorLength + 2 + g.len[3];
  }

  private static int calcLenId(int sectorLength) {
    int id = 0;
    while (sectorLength > 0x80) {
      id++;
      sectorLength >>= 1;
    }
    return id;
  }

  /** Writes a whole track: the sectors from the image, with the gaps and marks of this system between them. */
  private boolean wroteTrack(byte[] buffer, int[] from, int head, int cylinder, int sectorBase, int sectors,
                             int sectorLength, boolean preindex, int gap, int interleave, int autofill) {
    int slen = calcSectorLen(density != Density.SD && density != Density.SD8, sectorLength, gap);
    i = 0;
    setTrack(head, cylinder);
    if (preindex && !wrotePreindex(gap)) {
      return false;
    }
    if (!wrotePostindex(gap)) {
      return false;
    }
    int idx = i;
    int pos = 0, filled = 0;
    for (int s = sectorBase; s < sectorBase + sectors; s++) {
      i = idx + pos * slen;
      if (!wroteId(head, cylinder, s, calcLenId(sectorLength), gap, false)) {
        return false;
      }
      if (!wroteData(buffer, from, null, sectorLength, false, gap, false, autofill)) {
        return false;
      }
      pos += interleave;
      if (pos >= sectors) {
        pos -= sectors;
        if (pos <= filled) {
          pos++;
          filled++;
        }
      }
    }
    i = idx + sectors * slen;
    return wroteGap4(gap);
  }

  // ---- the sector image formats: MGT, IMG, OPD

  private void openImgMgtOpd(byte[] buffer) throws DiskException {
    int sectors, seclen;
    switch (buffer.length) {
      case 2 * 80 * 10 * 512 -> { sides = 2; cylinders = 80; sectors = 10; seclen = 512; }
      case 80 * 10 * 512 -> { sides = 1; cylinders = 80; sectors = 10; seclen = 512; }
      case 40 * 10 * 512 -> { sides = 1; cylinders = 40; sectors = 10; seclen = 512; }
      case 40 * 18 * 256 -> { sides = 1; cylinders = 40; sectors = 18; seclen = 256; }
      case 80 * 18 * 256 -> { sides = 1; cylinders = 80; sectors = 18; seclen = 256; }
      case 2 * 80 * 18 * 256 -> { sides = 2; cylinders = 80; sectors = 18; seclen = 256; }
      default -> throw new DiskException("an MGT, IMG or OPD image is not " + buffer.length + " bytes long");
    }
    density = Density.DD;
    alloc();
    int[] from = {0};
    if (type == Type.IMG) {
      for (int j = 0; j < sides; j++) {
        for (int c = 0; c < cylinders; c++) {
          if (!wroteTrack(buffer, from, j, c, 1, sectors, seclen, false, GAP_MGT_PLUSD, NO_INTERLEAVE, NO_AUTOFILL)) {
            throw new DiskException("invalid disk geometry");
          }
        }
      }
    } else {
      for (int c = 0; c < cylinders; c++) {
        for (int j = 0; j < sides; j++) {
          if (!wroteTrack(buffer, from, j, c, type == Type.MGT ? 1 : 0, sectors, seclen, false, GAP_MGT_PLUSD,
              type == Type.MGT ? NO_INTERLEAVE : INTERLEAVE_OPUS, NO_AUTOFILL)) {
            throw new DiskException("invalid disk geometry");
          }
        }
      }
    }
  }

  private void writeSector(java.io.OutputStream out, int seclen) throws IOException {
    out.write(data, track + i, 0x80 << seclen);
  }

  private void saveTrack(java.io.OutputStream out, int head, int cylinder, int sectorBase, int sectors, int seclen)
      throws IOException {
    setTrack(head, cylinder);
    i = 0;
    for (int s = sectorBase; s < sectorBase + sectors; s++) {
      if (!idSeek(s)) {
        throw new DiskException("sector " + s + " is missing from track " + cylinder);
      }
      if (datamarkRead() >= 0) {
        writeSector(out, seclen);
      }
    }
  }

  private void writeImgMgtOpd(java.io.OutputStream out) throws IOException {
    int[] geom = new int[5];
    int oddities = checkDiskGeom(geom);
    int sbase = geom[0], sectors = geom[1], seclen = geom[2], cyl = geom[4];
    if (oddities != 0
        || (type != Type.OPD && (sbase != 1 || seclen != 2 || sectors != 10))
        || (type == Type.OPD && (sbase != 0 || seclen != 1 || sectors != 18))) {
      throw new DiskException("this disk is not the shape a " + type + " image can hold");
    }
    if (cyl == -1) cyl = cylinders;
    if (cyl != 40 && cyl != 80) {
      throw new DiskException("a " + type + " image holds 40 or 80 tracks, not " + cyl);
    }
    if (type == Type.IMG) {
      for (int j = 0; j < sides; j++) {
        for (int c = 0; c < cyl; c++) saveTrack(out, j, c, 1, sectors, seclen);
      }
    } else {
      for (int c = 0; c < cyl; c++) {
        for (int j = 0; j < sides; j++) saveTrack(out, j, c, type == Type.MGT ? 1 : 0, sectors, seclen);
      }
    }
  }

  // ---- Didaktik: D40 and D80, sectors with a small header of geometry

  private void openD40D80(byte[] buffer) throws DiskException {
    if (buffer.length < 180) {
      throw new DiskException("a Didaktik image is at least 180 bytes long");
    }
    sides = (buffer[0xb1] & 0x10) != 0 ? 2 : 1;
    cylinders = buffer[0xb2] & 0xff;
    int sectors = buffer[0xb3] & 0xff;
    if (cylinders > 83 || sectors > 127) {
      throw new DiskException("invalid disk geometry");
    }
    density = Density.DD;
    alloc();
    int[] from = {0};
    for (int c = 0; c < cylinders; c++) {
      for (int j = 0; j < sides; j++) {
        if (!wroteTrack(buffer, from, j, c, 1, sectors, 512, false, GAP_MGT_PLUSD, NO_INTERLEAVE, NO_AUTOFILL)) {
          throw new DiskException("invalid disk geometry");
        }
      }
    }
  }

  private void writeD40D80(java.io.OutputStream out) throws IOException {
    int[] geom = new int[5];
    if (checkDiskGeom(geom) != 0 || geom[0] != 1) {
      throw new DiskException("this disk is not the shape a Didaktik image can hold");
    }
    int cyl = geom[4] == -1 ? cylinders : geom[4];
    if ((type == Type.D40 && cyl > 43) || (type == Type.D80 && cyl > 83)) {
      throw new DiskException("too many tracks for a " + type + " image");
    }
    for (int c = 0; c < cyl; c++) {
      for (int j = 0; j < sides; j++) saveTrack(out, j, c, 1, geom[1], geom[2]);
    }
  }

  // ---- TR-DOS: TRD sector images and SCL file containers

  private static final byte[] BETA128_BOOT_LOADER = {
      0x00, 0x01, 0x1c, 0x00, (byte) 0xf9, (byte) 0xc0, 0x31, 0x35, 0x36, 0x31, 0x39, 0x0e,
      0x00, 0x00, 0x03, 0x3d, 0x00, 0x3a, (byte) 0xea, 0x3a, (byte) 0xf7, 0x22, 0x20, 0x20,
      0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x22, 0x0d,
  };

  private static final int SECLEN_256 = 1;

  private void openTrd(byte[] buffer) throws DiskException {
    int spec = 8 * 256;
    if (buffer.length < spec + 256 || (buffer[spec + 231] & 0xff) != 0x10
        || (buffer[spec + 227] & 0xff) < 0x16 || (buffer[spec + 227] & 0xff) > 0x19) {
      throw new DiskException("not a TR-DOS disk: no specification sector");
    }
    sides = (buffer[spec + 227] & 0x08) != 0 ? 1 : 2;
    cylinders = (buffer[spec + 227] & 0x01) != 0 ? 40 : 80;
    if (buffer.length > sides * cylinders * 16 * 256) {
      int more = cylinders + 1;
      while (more < 83 && sides * more * 16 * 256 < buffer.length) {
        more++;
      }
      cylinders = more;
    }
    density = Density.DD;
    alloc();
    int[] from = {0};
    for (int c = 0; c < cylinders; c++) {
      for (int j = 0; j < sides; j++) {
        if (!wroteTrack(buffer, from, j, c, 1, 16, 256, false, GAP_TRDOS, INTERLEAVE_2, 0x00)) {
          throw new DiskException("invalid disk geometry");
        }
      }
    }
  }

  private static int trdosSectorAt(int sector, int slen) {
    return GAPS[GAP_TRDOS].len[1] + (sector % 8 * 2 + sector / 8) * slen;
  }

  private static int trdosPreDam() {
    Gap g = GAPS[GAP_TRDOS];
    return g.syncLen + (g.mark >= 0 ? 3 : 0) + 7 + g.len[2];
  }

  private static int trdosPreData() {
    Gap g = GAPS[GAP_TRDOS];
    return trdosPreDam() + g.syncLen + (g.mark >= 0 ? 3 : 0) + 1;
  }

  private boolean insertBasicFile(TrDos.Spec spec, byte[] file) {
    byte[] trailing = {(byte) 0x80, (byte) 0xaa, 0x01, 0x00};
    if (spec.fileCount >= 128) {
      return false;
    }
    int sectors = (file.length + trailing.length + 255) / 256;
    if (spec.freeSectors < sectors) {
      return false;
    }
    int slen = calcSectorLen(density != Density.SD && density != Density.SD8, 256, GAP_TRDOS);
    byte[] head = new byte[256];
    int copied = 0;
    int s = spec.firstFreeSector;
    int t = spec.firstFreeTrack;
    setTrackIdx(t);
    for (int n = 0; n < sectors; n++) {
      Arrays.fill(head, (byte) 0);
      int bytes = 0;
      if (copied < file.length) {
        bytes = Math.min(256, file.length - copied);
        System.arraycopy(file, copied, head, 0, bytes);
        copied += bytes;
      }
      if (copied >= file.length) {
        while (copied - file.length < trailing.length && bytes < 256) {
          head[bytes++] = trailing[copied - file.length];
          copied++;
        }
      }
      i = trdosSectorAt(s, slen) + trdosPreDam();
      wroteData(null, null, head, 256, false, GAP_TRDOS, false, NO_AUTOFILL);
      s = (s + 1) % 16;
      if (s == 0) {
        t++;
        if (t >= cylinders) {
          return false;
        }
        setTrackIdx(t);
      }
    }
    TrDos.DirEntry entry = new TrDos.DirEntry();
    entry.filename = "boot    ".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    entry.extension = 'B';
    entry.param1 = file.length;
    entry.param2 = file.length;
    entry.lengthInSectors = sectors;
    entry.startSector = spec.firstFreeSector;
    entry.startTrack = spec.firstFreeTrack;
    setTrackIdx(0);
    int fatSector = spec.fileCount / 16;
    i = trdosSectorAt(fatSector, slen);
    System.arraycopy(data, track + i + trdosPreData(), head, 0, 256);
    entry.write(head, spec.fileCount % 16 * 16);
    i += trdosPreDam();
    wroteData(null, null, head, 256, false, GAP_TRDOS, false, NO_AUTOFILL);
    spec.fileCount++;
    spec.freeSectors -= sectors;
    spec.firstFreeSector = s;
    spec.firstFreeTrack = t;
    spec.write(head, 0);
    i = trdosSectorAt(8, slen) + trdosPreDam();
    wroteData(null, null, head, 256, false, GAP_TRDOS, false, NO_AUTOFILL);
    return true;
  }

  /**
   * For a TR-DOS disk with no "boot" file when auto-load is on, this writes one that
   * runs the first BASIC program, so RUN "boot" starts the game.
   */
  public void insertTrdosBootLoader() {
    int savedTrack = track, savedI = i, savedCBpt = cBpt, savedClocks = clocks, savedFm = fm, savedWeak = weak;
    try {
      setTrackIdx(0);
      if (!idSeek(9) || datamarkRead() < 0) {
        return;
      }
      TrDos.Spec spec = TrDos.Spec.read(data, track + i);
      if (spec == null || spec.fileCount >= 128 || spec.freeSectors == 0) {
        return;
      }
      int slen = calcSectorLen(density != Density.SD && density != Density.SD8, 256, GAP_TRDOS);
      if (!idSeek(1) || datamarkRead() < 0) {
        return;
      }
      TrDos.BootInfo info = TrDos.readFat(data, track + i, slen);
      if (info.hasBootFile || info.basicFiles < 1) {
        return;
      }
      byte[] loader = BETA128_BOOT_LOADER.clone();
      System.arraycopy(info.firstBasicFile, 0, loader, 22, 8);
      insertBasicFile(spec, loader);
    } finally {
      track = savedTrack;
      i = savedI;
      cBpt = savedCBpt;
      clocks = savedClocks;
      fm = savedFm;
      weak = savedWeak;
    }
  }

  private void openScl(byte[] buffer) throws DiskException {
    sides = 2;
    cylinders = 80;
    density = Density.DD;
    alloc();
    int files = buffer.length > 8 ? buffer[8] & 0xff : 0;
    if (files > 128 || files < 1) {
      throw new DiskException("an SCL file holds between 1 and 128 files, not " + files);
    }
    int[] from = {9};
    setTrackIdx(0);
    i = 0;
    wrotePostindex(GAP_TRDOS);
    int firstSector = i;
    int s = 1;
    int deleted = 0;
    int sectors = 0;
    byte[] head = new byte[256];
    int j = 0;
    int seclen = calcSectorLen(true, 256, GAP_TRDOS);
    for (int n = 0; n < files; n++) {
      if (from[0] + 14 > buffer.length) {
        throw new DiskException("the SCL file ends inside its directory");
      }
      System.arraycopy(buffer, from[0], head, j, 14);
      from[0] += 14;
      head[j + 14] = (byte) (sectors % 16);
      head[j + 15] = (byte) (sectors / 16 + 1);
      sectors += head[j + 13] & 0xff;
      if (head[j] == 0x01) {
        deleted++;
      }
      if (sectors > 16 * 159) {
        throw new DiskException("the SCL file needs more sectors than a disk has");
      }
      j += 16;
      if (j == 256) {
        i = firstSector + ((s - 1) % 8 * 2 + (s - 1) / 8) * seclen;
        wroteId(0, 0, s, SECLEN_256, GAP_TRDOS, false);
        wroteData(null, null, head, 256, false, GAP_TRDOS, false, NO_AUTOFILL);
        Arrays.fill(head, (byte) 0);
        s++;
        j = 0;
      }
    }
    if (j != 0) {
      i = firstSector + ((s - 1) % 8 * 2 + (s - 1) / 8) * seclen;
      wroteId(0, 0, s, SECLEN_256, GAP_TRDOS, false);
      wroteData(null, null, head, 256, false, GAP_TRDOS, false, NO_AUTOFILL);
      s++;
    }
    Arrays.fill(head, (byte) 0);
    for (; s <= 16; s++) {
      i = firstSector + ((s - 1) % 8 * 2 + (s - 1) / 8) * seclen;
      wroteId(0, 0, s, SECLEN_256, GAP_TRDOS, false);
      if (s == 9) {
        head[225] = (byte) (sectors % 16);
        head[226] = (byte) (sectors / 16 + 1);
        head[227] = 0x16;
        head[228] = (byte) files;
        head[229] = (byte) ((2544 - sectors) % 256);
        head[230] = (byte) ((2544 - sectors) / 256);
        head[231] = 0x10;
        Arrays.fill(head, 234, 243, (byte) 32);
        head[244] = (byte) deleted;
        System.arraycopy("OOZX-SCL".getBytes(java.nio.charset.StandardCharsets.US_ASCII), 0, head, 245, 8);
      }
      wroteData(null, null, head, 256, false, GAP_TRDOS, false, NO_AUTOFILL);
      if (s == 9) {
        Arrays.fill(head, (byte) 0);
      }
    }
    wroteGap4(GAP_TRDOS);
    for (int n = 1; n < sides * cylinders; n++) {
      if (!wroteTrack(buffer, from, n % 2, n / 2, 1, 16, 256, false, GAP_TRDOS, INTERLEAVE_2, 0x00)) {
        throw new DiskException("invalid disk geometry");
      }
    }
  }

  private void writeTrd(java.io.OutputStream out) throws IOException {
    int[] geom = new int[5];
    if (checkDiskGeom(geom) != 0 || geom[0] != 1 || geom[2] != 1 || geom[1] != 16) {
      throw new DiskException("this disk is not the shape a TRD image can hold");
    }
    int cyl = geom[4] == -1 ? cylinders : geom[4];
    for (int c = 0; c < cyl; c++) {
      for (int j = 0; j < sides; j++) saveTrack(out, j, c, 1, 16, 1);
    }
  }

  private void writeScl(java.io.OutputStream out) throws IOException {
    int[] geom = new int[5];
    if (checkDiskGeom(geom) != 0 || geom[0] != 1 || geom[2] != 1 || geom[1] != 16) {
      throw new DiskException("this disk is not the shape an SCL file can hold");
    }
    setTrackIdx(0);
    if (!idSeek(9) || datamarkRead() < 0) {
      throw new DiskException("no TR-DOS specification sector");
    }
    int entries = trackByte(i + 228);
    int type = trackByte(i + 227);
    if (entries > 128 || trackByte(i + 231) != 0x10 || type < 0x16 || type > 0x19 || trackByte(i) != 0) {
      throw new DiskException("not a TR-DOS disk");
    }
    long sum = 597;
    out.write("SINCLAIR".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    out.write(entries);
    sum += entries;
    byte[] head = new byte[256];
    int j = 1, k = 0;
    for (int n = 0; n < entries; n++) {
      if (j > 8) {
        throw new DiskException("a TR-DOS directory is eight sectors long");
      }
      if (k == 0 && (!idSeek(j) || datamarkRead() < 0)) {
        throw new DiskException("directory sector " + j + " is missing");
      }
      out.write(data, track + i + k, 14);
      for (int b = 0; b < 14; b++) {
        sum += trackByte(i + k);
        k++;
      }
      k += 2;
      if (k >= 256) {
        j++;
        k = 0;
      }
    }
    j = 1;
    k = 0;
    for (int n = 0; n < entries; n++) {
      setTrackIdx(0);
      if (k == 0) {
        if (!idSeek(j) || datamarkRead() < 0) {
          throw new DiskException("directory sector " + j + " is missing");
        }
        System.arraycopy(data, track + i, head, 0, 256);
      }
      int s = head[k + 14] & 0xff;
      int t = head[k + 15] & 0xff;
      int last = (head[k + 13] & 0xff) + s;
      k += 16;
      if (k == 256) {
        k = 0;
        j++;
      }
      if (t >= sides * cylinders) {
        throw new DiskException("a file starts beyond the disk");
      }
      if (s % 16 == 0) {
        t--;
      }
      setTrackIdx(t);
      for (; s < last; s++) {
        if (s % 16 == 0) {
          t++;
          if (t >= sides * cylinders) {
            throw new DiskException("a file runs beyond the disk");
          }
          setTrackIdx(t);
        }
        if (!idSeek(s % 16 + 1)) {
          throw new DiskException("sector " + (s % 16 + 1) + " of track " + t + " is missing");
        }
        if (datamarkRead() >= 0) {
          writeSector(out, 1);
          for (int b = 0; b < 256; b++) {
            sum += trackByte(i + b);
          }
        }
      }
    }
    out.write((int) (sum & 0xff));
    out.write((int) ((sum >> 8) & 0xff));
    out.write((int) ((sum >> 16) & 0xff));
    out.write((int) ((sum >> 24) & 0xff));
  }


  private static final int CPC_ISSUE_NONE = 0;
  /** One 8K sector carrying 6K or 8K of data. */
  private static final int CPC_ISSUE_1 = 1;
  /** One 8K sector and then ten of 512. */
  private static final int CPC_ISSUE_2 = 2;
  /** Sectors that double: 128, 256, 512 and up to 4096. */
  private static final int CPC_ISSUE_3 = 3;
  /** Track 38 alternating 512 and 256, nine times over. */
  private static final int CPC_ISSUE_5 = 5;

  private static int postindexLen(int gaptype) {
    return GAPS[gaptype].len()[1];
  }

  /**
   * The bytes that differ between the copies an image keeps of one sector are its weak ones: what
   * a protected disk is made of, and what the drive must answer differently every time it is read.
   */
  private void cpcSetWeakRange(int idx, byte[] buffer, int at, int copies, int len) {
    int first = -1, last = -1;
    for (int k = 0; k < len; k++) {
      for (int copy = 0; copy < copies - 1; copy++) {
        if (data[track + idx + k] != buffer[at + copy * len + k]) {
          if (first == -1) first = idx + k;
          last = idx + k;
        }
      }
    }
    if (first == -1) return;
    for (; first <= last; first++) {
      setBit(weak, first, true);
    }
  }

  private int cpcGap(byte[] buffer, int at) {
    return (buffer[at + 0x1b] & 0xff) >= 6
        ? (buffer[at + 0x13] == 2 ? GAP_8K765_MFM : GAP_4K765_FM)
        : (buffer[at + 0x13] == 2 ? GAP_IBM34 : GAP_IBM3740);
  }

  /**
   * A CPC or Extended CPC .dsk, which is the format a +3 disk comes in: a header, then a header
   * and the data for each track. The track is laid down from what the sector headers say - gaps,
   * address marks, CRCs and all - because a track is what the controller reads, not a list of
   * sectors.
   * <p>
   * The corrections for disks whose tracks do not fit what their
   * headers say, which is how most +3 protections work, are applied only when the disk was asked
   * for with {@link #FLAG_PLUS3_CPC}.
   */
  private void openCpc(byte[] buffer, boolean preindex) throws DiskException {
    boolean extended = type == Type.ECPC;
    sides = buffer[0x31] & 0xff;
    cylinders = buffer[0x30] & 0xff;
    if (sides < 1 || sides > 2 || cylinders < 1 || cylinders > 84) {
      throw new DiskException("invalid disk geometry");
    }
    int trlen = extended ? -1 : (buffer[0x32] & 0xff) + 256 * (buffer[0x33] & 0xff);
    int tltbl = 0x34;

    // Every track is MFM unless its header says otherwise. Walking off the end of a lying
    // header here would get away with it in C; this stops, because Java would throw anyway.
    int at = 256;
    for (int t = 0; t < sides * cylinders && at + 0x13 < buffer.length; t++) {
      if (buffer[at + 0x13] == 0) buffer[at + 0x13] = 2;
      at += extended ? (buffer[tltbl + t] & 0xff) * 256 : trlen;
    }

    at = 256;
    int maxBpt = 0;
    for (int t = 0; t < sides * cylinders; t++) {
      if (buffer.length - at >= 13 && text(buffer, at, 13).equals("Offset-Info\r\n")) {
        at = buffer.length;                              // the sector offset block is not read
      }
      if (buffer.length - at <= 0) {                     // the header promised tracks the file has not
        cylinders = t / sides + t % sides;
        break;
      }
      if (extended && buffer[tltbl + t] == 0) continue;  // an unformatted track
      if (buffer.length - at < 256 || !text(buffer, at, 10).equals("Track-Info")) {
        throw new DiskException("no track header at " + at);
      }
      int sectors = buffer[at + 0x15] & 0xff;
      int room = 24 + 8 * sectors;
      room += 0x100 - (room % 0x100);
      if (sectors > 29 && buffer.length - at < room) {
        throw new DiskException("the track says it has " + sectors + " sectors and there is no room for them");
      }
      int gap = cpcGap(buffer, at);
      int cpcFix = CPC_ISSUE_NONE;
      if (t != (buffer[at + 0x10] & 0xff) * sides + (buffer[at + 0x11] & 0xff)) {
        throw new DiskException("the track at " + at + " says it is another one");
      }
      int bpt = postindexLen(gap) + (preindex ? preindexLen(gap) : 0) + (buffer[at + 0x13] == 2 ? 6 : 3);
      int seclen = 0;
      for (int k = 0; k < sectors; k++) {
        int n = buffer[at + 0x1b + 8 * k] & 0xff;
        seclen = extended ? (buffer[at + 0x1e + 8 * k] & 0xff) + 256 * (buffer[at + 0x1f + 8 * k] & 0xff)
            : 0x80 << n;
        int idlen = 0x80 << (n > 7 ? 8 : n);
        if (!extended && ((seclen == 6144 && idlen != 8192) || (idlen > 4096 && seclen != 0) || seclen != idlen)) {
          throw new DiskException("sector " + k + " is not the length its ID says");
        }
        bpt += calcSectorLen(buffer[at + 0x13] == 2, Math.min(seclen, idlen), gap);
        if ((flag & FLAG_PLUS3_CPC) != 0) {
          int first = buffer[at + 0x18] & 0xff;
          if (k == 0 && n == 6) {
            cpcFix = CPC_ISSUE_1;
          } else if (k == 0 && ((buffer[at + 0x18] == 0 && buffer[at + 0x19] == 0
              && buffer[at + 0x1a] == 0 && buffer[at + 0x1b] == 0)
              || (buffer[at + 0x18] == 1 && buffer[at + 0x19] == 1
              && buffer[at + 0x1a] == 1 && buffer[at + 0x1b] == 1))) {
            cpcFix = CPC_ISSUE_3;
          } else if (k == 1 && cpcFix == CPC_ISSUE_1 && n == 2) {
            cpcFix = CPC_ISSUE_2;
          } else if (t == 38 && k == 0 && n == 2) {
            cpcFix = CPC_ISSUE_5;
          } else if (k > 1 && cpcFix == CPC_ISSUE_2 && n != 2) {
            cpcFix = CPC_ISSUE_NONE;
          } else if (k > 0 && cpcFix == CPC_ISSUE_3
              && ((buffer[at + 0x18 + 8 * k] & 0xff) != k + first
              || (buffer[at + 0x19 + 8 * k] & 0xff) != k + first
              || (buffer[at + 0x1a + 8 * k] & 0xff) != k + first
              || (buffer[at + 0x1b + 8 * k] & 0xff) != k + first)) {
            cpcFix = CPC_ISSUE_NONE;
          } else if (k > 10 && cpcFix == CPC_ISSUE_2) {
            cpcFix = CPC_ISSUE_NONE;
          } else if (t == 38 && k > 0 && cpcFix == CPC_ISSUE_5 && n != 2 - (k & 1)) {
            cpcFix = CPC_ISSUE_NONE;
          }
        }
      }
      buffer[at] = (byte) cpcFix;                        // the T of "Track-Info" holds the correction
      int full = 6250 / (buffer[at + 0x13] == 2 ? 1 : 2);
      if (bpt > full && cpcFix == CPC_ISSUE_NONE
          && bpt - sectors * GAPS[gap].len()[3] <= full - sectors * 8) {
        buffer[at + 0x16] = (byte) ((full + sectors * GAPS[gap].len()[3] - bpt) / sectors);
        buffer[at] |= (byte) 0x80;                       // and a gap3 of its own
      }
      if ((buffer[at] & 0xff) == CPC_ISSUE_1 && seclen > 6144) {
        bpt = 6500;
      } else if ((buffer[at] & 0xff) != CPC_ISSUE_NONE) {
        bpt = 6250;
      }
      at += extended ? 256 * (buffer[tltbl + t] & 0xff) : trlen;
      if (bpt > maxBpt) maxBpt = bpt;
    }

    if (maxBpt == 0 && at == 0x100) {                    // a header and nothing else: an empty disk
      maxBpt = 6250;
      if (cylinders == 0) cylinders = 1;
    }
    if (maxBpt == 0) {
      throw new DiskException("the image has no track this can read");
    }
    density = Density.AUTO;
    bpt = maxBpt;
    alloc();

    setTrackIdx(0);
    at = 256;
    for (int t = 0; t < sides * cylinders; t++) {
      if (extended && buffer[tltbl + t] == 0) continue;
      int header = at;
      at += 256;                                         // past the header, to the data
      int gap = cpcGap(buffer, header);
      if ((buffer[header + 0x10] & 0xff) * sides + (buffer[header + 0x11] & 0xff) > t) {
        t = (buffer[header + 0x10] & 0xff) * sides + (buffer[header + 0x11] & 0xff);
      }
      int cpcFix = buffer[header] & 0xff;
      if ((cpcFix & 0x80) != 0) {
        int custom = buffer[header + 0x13] == 2 ? GAP_CUSTOM_MFM : GAP_CUSTOM_FM;
        System.arraycopy(GAPS[gap].len(), 0, GAPS[custom].len(), 0, 3);
        GAPS[custom].len()[3] = buffer[header + 0x16] & 0xff;
        gap = custom;
        cpcFix &= 0x7f;
      }
      setTrackIdx(t);
      if (cBpt == 6500 && cpcFix != CPC_ISSUE_1) {       // only the long tracks are that long
        data[track - 3] = (byte) (6250 % 256);
        data[track - 2] = (byte) (6250 / 256);
        cBpt = 6250;
        setTrackIdx(t);
      }
      i = 0;
      if (preindex) wrotePreindex(gap);
      wrotePostindex(gap);

      int sectors = buffer[header + 0x15] & 0xff;
      for (int k = 0; k < sectors; k++) {
        int n = buffer[header + 0x1b + 8 * k] & 0xff;
        int seclen = extended ? (buffer[header + 0x1e + 8 * k] & 0xff) + 256 * (buffer[header + 0x1f + 8 * k] & 0xff)
            : 0x80 << n;
        int idlen = 0x80 << (n > 7 ? 8 : n);
        boolean idCrcError = (buffer[header + 0x1c + 8 * k] & 0x20) != 0 && (buffer[header + 0x1d + 8 * k] & 0x20) == 0;
        boolean dataCrcError = (buffer[header + 0x1c + 8 * k] & 0x20) != 0 && (buffer[header + 0x1d + 8 * k] & 0x20) != 0;
        boolean ddam = (buffer[header + 0x1d + 8 * k] & 0x40) != 0;
        int[] from = {at};

        if (cpcFix == CPC_ISSUE_2 && k == 0) {
          i = 8;                                         // the dummy sector goes further along
        }
        wroteId(buffer[header + 0x19 + 8 * k] & 0xff, buffer[header + 0x18 + 8 * k] & 0xff,
            buffer[header + 0x1a + 8 * k] & 0xff, n, gap, idCrcError);

        if (cpcFix == CPC_ISSUE_1 && k == 0) {
          wroteData(buffer, from, null, seclen > 6144 ? 6384 : seclen, ddam, gap, dataCrcError, NO_AUTOFILL);
          at = from[0];
          if (seclen > 6144) at += seclen - 6384;
        } else if (cpcFix == CPC_ISSUE_2 && k == 0) {
          wroteDatamark(ddam, gap);
          wroteGap(2, gap);
          at += seclen;
        } else if (cpcFix == CPC_ISSUE_3) {
          wroteData(buffer, from, null, 128, ddam, gap, dataCrcError, NO_AUTOFILL);
          at = from[0] + seclen - 128;
        } else if (cpcFix == CPC_ISSUE_5) {
          if (idlen == 256) {
            wroteData(null, null, Arrays.copyOfRange(buffer, at, at + 512), 512, ddam, gap, dataCrcError, NO_AUTOFILL);
            at += idlen;
          } else {
            wroteData(buffer, from, null, idlen, ddam, gap, dataCrcError, NO_AUTOFILL);
            at = from[0];
          }
        } else {
          int[] dataStart = new int[1];
          if (!wroteData(buffer, from, null, Math.min(seclen, idlen), ddam, gap, dataCrcError, NO_AUTOFILL, dataStart)) {
            at += seclen;                                // it did not fit, so step over it here
          } else {
            at = from[0];
            if (seclen > idlen && seclen % idlen != 0) { // the rest of it was written into the gap
              int saved = i;
              i = dataStart[0] + idlen;
              for (int left = seclen - idlen; left > 0; left--) {
                if (i == cBpt) i = 0;
                data[track + i] = buffer[at];
                i++;
                at++;
              }
              i = saved;
            } else if (seclen > idlen) {                 // the same sector, kept more than once
              cpcSetWeakRange(dataStart[0], buffer, at, seclen / idlen, idlen);
              at += (seclen / idlen - 1) * idlen;
            }
          }
        }
      }
      wroteGap4(gap);
      at = header + (extended ? 256 * (buffer[tltbl + t] & 0xff) : trlen);
    }
  }

  private static String text(byte[] buffer, int at, int length) {
    return new String(buffer, at, length, java.nio.charset.StandardCharsets.US_ASCII);
  }

  // ---- files

  /** Which format a file is, from its name and its size; what this cannot read yet says so. */
  static Type typeOf(String name, byte[] buffer) throws DiskException {
    String lower = name.toLowerCase(Locale.ROOT);
    int dot = lower.lastIndexOf('.');
    String ext = dot < 0 ? "" : lower.substring(dot + 1);
    return switch (ext) {
      case "mgt" -> Type.MGT;
      case "img" -> Type.IMG;
      case "opd", "opu" -> Type.OPD;
      case "dsk" -> {
        String head = text(buffer, 0, Math.min(8, buffer.length));
        if (head.startsWith("EXTENDED")) {
          yield Type.ECPC;
        }
        yield head.startsWith("MV - CPC") ? Type.CPC : Type.MGT;
      }
      case "trd" -> Type.TRD;
      case "scl" -> Type.SCL;
      case "d40", "d80" -> Type.D80;
      case "udi", "fdi", "td0", "sad" ->
          throw new DiskException("." + ext + " images are not read yet");
      default -> throw new DiskException("not a disk image this knows: " + name);
    };
  }

  /** A disk from an image file, with the track marks made up as the format needs. */
  public static Disk open(File file) throws IOException {
    return open(file, FLAG_NONE);
  }

  /** The flag is asked for before the image is read, because a +3 disk is built differently for it. */
  public static Disk open(File file, int flag) throws IOException {
    Disk disk = openBuffer(file.getName(), Files.readAllBytes(file.toPath()), flag);
    disk.filename = file.getPath();
    disk.wrprot = !file.canWrite();
    return disk;
  }

  public static Disk openBuffer(String name, byte[] buffer) throws DiskException {
    return openBuffer(name, buffer, FLAG_NONE);
  }

  public static Disk openBuffer(String name, byte[] buffer, int flag) throws DiskException {
    Disk disk = new Disk();
    disk.flag = flag;
    disk.type = typeOf(name, buffer);
    switch (disk.type) {
      case MGT, IMG, OPD -> disk.openImgMgtOpd(buffer);
      case CPC, ECPC -> disk.openCpc(buffer, false);
      case TRD -> disk.openTrd(buffer);
      case SCL -> disk.openScl(buffer);
      case D80 -> disk.openD40D80(buffer);
      default -> throw new DiskException("cannot open " + name);
    }
    disk.dirty = false;
    disk.updateTracksMode();
    disk.filename = name;
    return disk;
  }

  /** The image, in this disk's format, as bytes: what goes back into the file. */
  public byte[] toImage() throws IOException {
    int savedTrack = track, savedI = i, savedCBpt = cBpt, savedClocks = clocks, savedFm = fm, savedWeak = weak;
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    try {
      updateTracksMode();
      switch (type) {
        case IMG, MGT, OPD -> writeImgMgtOpd(out);
        case TRD -> writeTrd(out);
        case SCL -> writeScl(out);
        case D40, D80 -> writeD40D80(out);
        default -> throw new DiskException("cannot write a " + type + " image yet");
      }
    } finally {
      track = savedTrack;
      i = savedI;
      cBpt = savedCBpt;
      clocks = savedClocks;
      fm = savedFm;
      weak = savedWeak;
    }
    return out.toByteArray();
  }

  public void write(File file) throws IOException {
    if (type == Type.NONE) {
      type = typeOf(file.getName(), new byte[0]);
    }
    Files.write(file.toPath(), toImage());
    filename = file.getPath();
    dirty = false;
  }

  /** Whether there is anything on it: a blank disk has no data at all. */
  public boolean isLoaded() {
    return data != null;
  }
}
