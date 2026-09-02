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
 * Ported from Fuse's disk.c. The track's bytes, clock marks, FM marks and weak marks live in one
 * array, laid out per track as Fuse lays them out, so a UDI image is that array and nothing else.
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

  private boolean gapAdd(int gap, int gaptype) {
    Gap g = GAPS[gaptype];
    if (i + g.len[gap] >= cBpt) {
      return true;
    }
    Arrays.fill(data, track + i, track + i + g.len[gap], (byte) g.gap);
    i += g.len[gap];
    return false;
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

  private boolean preindexAdd(int gaptype) {
    Gap g = GAPS[gaptype];
    if (i + preindexLen(gaptype) >= cBpt || gapAdd(0, gaptype)) {
      return true;
    }
    sync(g);
    if (g.mark < 0) {
      setBit(clocks, i, true);
    }
    setTrackByte(i++, 0xfc);
    return false;
  }

  private boolean postindexAdd(int gaptype) {
    return gapAdd(1, gaptype);
  }

  private boolean gap4Add(int gaptype) {
    int len = cBpt - i;
    if (len < 0) {
      return true;
    }
    Arrays.fill(data, track + i, track + cBpt, (byte) GAPS[gaptype].gap);
    i = cBpt;
    return false;
  }

  private boolean idAdd(int h, int t, int s, int l, int gaptype, boolean crcError) {
    Gap g = GAPS[gaptype];
    if (i + g.syncLen + (g.mark >= 0 ? 3 : 0) + 7 >= cBpt) {
      return true;
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
    return gapAdd(2, gaptype);
  }

  private boolean datamarkAdd(boolean ddam, int gaptype) {
    Gap g = GAPS[gaptype];
    if (i + g.len[2] + g.syncLen + (g.mark >= 0 ? 3 : 0) + 1 >= cBpt) {
      return true;
    }
    sync(g);
    if (g.mark < 0) {
      setBit(clocks, i, true);
    }
    setTrackByte(i++, ddam ? 0xf8 : 0xfb);
    return false;
  }

  /**
   * A sector's data, from an image being read (buffer, at from) or from bytes given; padded with
   * autofill when the image runs short, unless autofill is negative.
   */
  private boolean dataAdd(byte[] buffer, int[] from, byte[] given, int len, boolean ddam, int gaptype,
                          boolean crcError, int autofill) {
    Gap g = GAPS[gaptype];
    if (datamarkAdd(ddam, gaptype)) {
      return true;
    }
    int crc = 0xffff;
    if (g.mark >= 0) {
      for (int n = 0; n < 3; n++) crc = Crc.fdc(crc, g.mark);
    }
    crc = Crc.fdc(crc, ddam ? 0xf8 : 0xfb);
    if (len >= 0) {
      if (i + len + 2 >= cBpt) {
        return true;
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
          return true;
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
    return gapAdd(3, gaptype);
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
  private boolean trackgen(byte[] buffer, int[] from, int head, int cylinder, int sectorBase, int sectors,
                           int sectorLength, boolean preindex, int gap, int interleave, int autofill) {
    int slen = calcSectorLen(density != Density.SD && density != Density.SD8, sectorLength, gap);
    i = 0;
    setTrack(head, cylinder);
    if (preindex && preindexAdd(gap)) {
      return true;
    }
    if (postindexAdd(gap)) {
      return true;
    }
    int idx = i;
    int pos = 0, filled = 0;
    for (int s = sectorBase; s < sectorBase + sectors; s++) {
      i = idx + pos * slen;
      if (idAdd(head, cylinder, s, calcLenId(sectorLength), gap, false)) {
        return true;
      }
      if (dataAdd(buffer, from, null, sectorLength, false, gap, false, autofill)) {
        return true;
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
    return gap4Add(gap);
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
          if (trackgen(buffer, from, j, c, 1, sectors, seclen, false, GAP_MGT_PLUSD, NO_INTERLEAVE, NO_AUTOFILL)) {
            throw new DiskException("invalid disk geometry");
          }
        }
      }
    } else {
      for (int c = 0; c < cylinders; c++) {
        for (int j = 0; j < sides; j++) {
          if (trackgen(buffer, from, j, c, type == Type.MGT ? 1 : 0, sectors, seclen, false, GAP_MGT_PLUSD,
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
        String head = new String(buffer, 0, Math.min(8, buffer.length), java.nio.charset.StandardCharsets.US_ASCII);
        if (head.startsWith("MV - CPC") || head.startsWith("EXTENDED")) {
          throw new DiskException("CPC .dsk images are not read yet");
        }
        yield Type.MGT;
      }
      case "trd", "scl", "udi", "fdi", "td0", "sad", "d40", "d80" ->
          throw new DiskException("." + ext + " images are not read yet");
      default -> throw new DiskException("not a disk image this knows: " + name);
    };
  }

  /** A disk from an image file, with the track marks made up as the format needs. */
  public static Disk open(File file) throws IOException {
    Disk disk = openBuffer(file.getName(), Files.readAllBytes(file.toPath()));
    disk.filename = file.getPath();
    disk.wrprot = !file.canWrite();
    return disk;
  }

  public static Disk openBuffer(String name, byte[] buffer) throws DiskException {
    Disk disk = new Disk();
    disk.type = typeOf(name, buffer);
    switch (disk.type) {
      case MGT, IMG, OPD -> disk.openImgMgtOpd(buffer);
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
