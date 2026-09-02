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
package com.fpetrola.oozx.speccy.devices.ide;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * One ATA channel with a master and a slave, each an HDF file: the eight task-file registers,
 * the commands the firmware of the day used - read and write sectors by CHS or LBA, identify,
 * and the ones that only have to say yes - and a 512-byte buffer the data register hands over
 * a byte at a time, high byte after low on a 16-bit bus or the low byte alone on an 8-bit one.
 * What is written waits in memory until it is committed to the file, which is what Fuse and
 * libspectrum do.
 * <p>
 * An HDF file is "RS-IDE", 0x1a, a version, a flag byte whose bit 0 says only the low byte of
 * each word is stored, the offset of the first sector, and from 0x16 the drive's IDENTIFY block.
 */
public class IdeChannel {

  public enum Register { DATA, ERROR_FEATURE, SECTOR_COUNT, SECTOR, CYLINDER_LOW, CYLINDER_HIGH, HEAD_DRIVE, COMMAND_STATUS }

  public static final int MASTER = 0;
  public static final int SLAVE = 1;
  public static final int SECTOR = 512;
  public static final int STATUS_ERR = 0x01;
  public static final int STATUS_DRQ = 0x08;
  public static final int STATUS_DSC = 0x10;
  public static final int STATUS_DRDY = 0x40;
  private static final int ERROR_ABRT = 0x04;
  private static final int ERROR_IDNF = 0x10;
  private static final int HEAD_LBA = 0x40;
  private static final byte[] SIGNATURE = "RS-IDE".getBytes(StandardCharsets.US_ASCII);
  private static final int IDENTIFY_OFFSET = 0x16;

  private enum Phase { IDLE, READ, WRITE }

  /** A drive: its file, its shape, and the sectors written since the last commit. */
  public static final class Drive {
    private RandomAccessFile file;
    private String filename;
    private int dataOffset;
    private boolean halfSectors;
    private byte[] identify;
    private int cylinders, heads, sectors;
    private long totalSectors;
    private final Map<Long, byte[]> changed = new HashMap<>();

    public boolean present() {
      return file != null;
    }

    public String filename() {
      return filename;
    }

    public boolean dirty() {
      return !changed.isEmpty();
    }

    public long sectors() {
      return totalSectors;
    }

    private void open(File image) throws IOException {
      RandomAccessFile opened = new RandomAccessFile(image, "r");
      byte[] header = new byte[IDENTIFY_OFFSET + SECTOR];
      int got = opened.read(header);
      if (got < 0x0b || !Arrays.equals(Arrays.copyOf(header, SIGNATURE.length), SIGNATURE)) {
        opened.close();
        throw new IOException(image.getName() + " is not an HDF hard disk image");
      }
      file = opened;
      filename = image.getPath();
      halfSectors = (header[8] & 0x01) != 0;
      dataOffset = header[9] & 0xff | (header[10] & 0xff) << 8;
      totalSectors = (opened.length() - dataOffset) / (halfSectors ? SECTOR / 2 : SECTOR);
      identify = dataOffset >= header.length ? Arrays.copyOfRange(header, IDENTIFY_OFFSET, header.length) : null;
      if (identify != null && word(identify, 1) != 0 && word(identify, 3) != 0 && word(identify, 6) != 0) {
        cylinders = word(identify, 1);
        heads = word(identify, 3);
        sectors = word(identify, 6);
      } else {
        heads = 16;
        sectors = 63;
        cylinders = (int) Math.max(1, totalSectors / (heads * sectors));
        identify = identifyFor(this);
      }
      changed.clear();
    }

    private void close() {
      try {
        if (file != null) file.close();
      } catch (IOException ignored) {
      }
      file = null;
      filename = null;
      changed.clear();
    }

    private byte[] read(long lba) throws IOException {
      byte[] sector = changed.get(lba);
      if (sector != null) {
        return sector.clone();
      }
      sector = new byte[SECTOR];
      if (halfSectors) {
        byte[] half = new byte[SECTOR / 2];
        file.seek(dataOffset + lba * half.length);
        file.readFully(half);
        for (int i = 0; i < half.length; i++) {
          sector[2 * i] = half[i];
          sector[2 * i + 1] = (byte) 0xff;
        }
      } else {
        file.seek(dataOffset + lba * SECTOR);
        file.readFully(sector);
      }
      return sector;
    }

    private void write(long lba, byte[] sector) {
      changed.put(lba, sector.clone());
    }

    /** Writes the changed sectors into that file - the one it came from, or a copy of it. */
    public void commit(File into) throws IOException {
      File source = new File(filename);
      if (!into.getAbsoluteFile().equals(source.getAbsoluteFile())) {
        Files.copy(source.toPath(), into.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
      try (RandomAccessFile out = new RandomAccessFile(into, "rw")) {
        for (Map.Entry<Long, byte[]> sector : changed.entrySet()) {
          byte[] data = sector.getValue();
          if (halfSectors) {
            byte[] half = new byte[SECTOR / 2];
            for (int i = 0; i < half.length; i++) {
              half[i] = data[2 * i];
            }
            data = half;
          }
          out.seek(dataOffset + sector.getKey() * data.length);
          out.write(data);
        }
      }
      changed.clear();
    }
  }

  private final boolean sixteenBit;
  private final Drive[] drives = {new Drive(), new Drive()};
  private final int[] registers = new int[8];
  private final byte[] buffer = new byte[SECTOR];
  private int position;
  private boolean highByte;
  private Phase phase = Phase.IDLE;
  private long lba;
  private int remaining;

  public IdeChannel(boolean sixteenBit) {
    this.sixteenBit = sixteenBit;
    reset();
  }

  public Drive drive(int unit) {
    return drives[unit];
  }

  public void insert(int unit, File image) throws IOException {
    drives[unit].close();
    drives[unit].open(image);
  }

  public void eject(int unit) {
    drives[unit].close();
  }

  public void reset() {
    Arrays.fill(registers, 0);
    registers[Register.ERROR_FEATURE.ordinal()] = 0x01;
    registers[Register.SECTOR_COUNT.ordinal()] = 0x01;
    registers[Register.SECTOR.ordinal()] = 0x01;
    registers[Register.COMMAND_STATUS.ordinal()] = STATUS_DRDY | STATUS_DSC;
    phase = Phase.IDLE;
    position = 0;
    highByte = false;
  }

  private Drive selected() {
    return drives[(registers[Register.HEAD_DRIVE.ordinal()] & 0x10) != 0 ? SLAVE : MASTER];
  }

  public int read(Register register) {
    if (!selected().present()) {
      return register == Register.COMMAND_STATUS ? 0x00 : 0xff;
    }
    if (register == Register.DATA) {
      return dataRead();
    }
    return registers[register.ordinal()];
  }

  public void write(Register register, int value) {
    value &= 0xff;
    switch (register) {
      case DATA -> dataWrite(value);
      case COMMAND_STATUS -> {
        if (selected().present()) execute(value);
      }
      default -> registers[register.ordinal()] = value;
    }
  }

  private int dataRead() {
    if (phase != Phase.READ) {
      return 0xff;
    }
    int value;
    if (sixteenBit) {
      value = buffer[position + (highByte ? 1 : 0)] & 0xff;
      highByte = !highByte;
      if (highByte) {
        return value;
      }
    } else {
      value = buffer[position] & 0xff;
    }
    position += 2;
    if (position >= SECTOR) {
      sectorDone();
    }
    return value;
  }

  private void dataWrite(int value) {
    if (phase != Phase.WRITE) {
      return;
    }
    if (sixteenBit) {
      buffer[position + (highByte ? 1 : 0)] = (byte) value;
      highByte = !highByte;
      if (highByte) {
        return;
      }
    } else {
      buffer[position] = (byte) value;
      buffer[position + 1] = (byte) 0xff;
    }
    position += 2;
    if (position >= SECTOR) {
      sectorDone();
    }
  }

  /** One sector has crossed the data register: the next one, or the command is over. */
  private void sectorDone() {
    Drive drive = selected();
    if (phase == Phase.WRITE) {
      drive.write(lba, buffer);
    }
    position = 0;
    highByte = false;
    lba++;
    remaining--;
    if (remaining <= 0 || lba >= drive.sectors()) {
      finish();
      return;
    }
    if (phase == Phase.READ) {
      try {
        System.arraycopy(drive.read(lba), 0, buffer, 0, SECTOR);
      } catch (IOException cannot) {
        fail(ERROR_IDNF);
      }
    }
  }

  private void finish() {
    phase = Phase.IDLE;
    registers[Register.COMMAND_STATUS.ordinal()] = STATUS_DRDY | STATUS_DSC;
  }

  private void fail(int error) {
    phase = Phase.IDLE;
    registers[Register.ERROR_FEATURE.ordinal()] = error;
    registers[Register.COMMAND_STATUS.ordinal()] = STATUS_DRDY | STATUS_DSC | STATUS_ERR;
  }

  private void ready(Phase what) {
    phase = what;
    position = 0;
    highByte = false;
    registers[Register.ERROR_FEATURE.ordinal()] = 0;
    registers[Register.COMMAND_STATUS.ordinal()] = STATUS_DRDY | STATUS_DSC | STATUS_DRQ;
  }

  /** Where the task file points: LBA if the head register says so, else cylinder, head and sector. */
  private long address(Drive drive) {
    int head = registers[Register.HEAD_DRIVE.ordinal()];
    int sector = registers[Register.SECTOR.ordinal()];
    int cylinder = registers[Register.CYLINDER_LOW.ordinal()] | registers[Register.CYLINDER_HIGH.ordinal()] << 8;
    if ((head & HEAD_LBA) != 0) {
      return sector | (long) cylinder << 8 | (long) (head & 0x0f) << 24;
    }
    return ((long) cylinder * drive.heads + (head & 0x0f)) * drive.sectors + sector - 1;
  }

  private void execute(int command) {
    Drive drive = selected();
    remaining = registers[Register.SECTOR_COUNT.ordinal()] == 0 ? 256 : registers[Register.SECTOR_COUNT.ordinal()];
    switch (command) {
      case 0x20, 0x21 -> {
        lba = address(drive);
        if (lba < 0 || lba >= drive.sectors()) {
          fail(ERROR_IDNF);
          return;
        }
        try {
          System.arraycopy(drive.read(lba), 0, buffer, 0, SECTOR);
          ready(Phase.READ);
        } catch (IOException cannot) {
          fail(ERROR_IDNF);
        }
      }
      case 0x30, 0x31 -> {
        lba = address(drive);
        if (lba < 0 || lba >= drive.sectors()) {
          fail(ERROR_IDNF);
          return;
        }
        ready(Phase.WRITE);
      }
      case 0xec -> {
        System.arraycopy(drive.identify, 0, buffer, 0, SECTOR);
        remaining = 1;
        ready(Phase.READ);
      }
      case 0x91 -> {
        drive.heads = (registers[Register.HEAD_DRIVE.ordinal()] & 0x0f) + 1;
        drive.sectors = Math.max(1, registers[Register.SECTOR_COUNT.ordinal()]);
        finish();
      }
      case 0x90 -> {
        registers[Register.ERROR_FEATURE.ordinal()] = 0x01;
        finish();
      }
      default -> {
        if (command >= 0x10 && command <= 0x1f || command >= 0x70 && command <= 0x7f
            || command == 0x40 || command == 0x41 || command == 0xe7 || command == 0xef) {
          finish();
        } else {
          fail(ERROR_ABRT);
        }
      }
    }
  }

  private static int word(byte[] block, int index) {
    return block[2 * index] & 0xff | (block[2 * index + 1] & 0xff) << 8;
  }

  private static void word(byte[] block, int index, int value) {
    block[2 * index] = (byte) value;
    block[2 * index + 1] = (byte) (value >> 8);
  }

  /** ATA strings go two characters to a word, the first in the high byte. */
  private static void text(byte[] block, int index, int words, String text) {
    byte[] padded = String.format("%-" + 2 * words + "s", text).getBytes(StandardCharsets.US_ASCII);
    for (int i = 0; i < words; i++) {
      block[2 * (index + i)] = padded[2 * i + 1];
      block[2 * (index + i) + 1] = padded[2 * i];
    }
  }

  /** The IDENTIFY block for a drive whose file has none. */
  private static byte[] identifyFor(Drive drive) {
    byte[] block = new byte[SECTOR];
    word(block, 0, 0x0040);
    word(block, 1, drive.cylinders);
    word(block, 3, drive.heads);
    word(block, 6, drive.sectors);
    text(block, 10, 10, "0");
    text(block, 23, 4, "1.0");
    text(block, 27, 20, "OOZX HDF drive");
    word(block, 49, 0x0200);
    word(block, 53, 0x0001);
    word(block, 54, drive.cylinders);
    word(block, 55, drive.heads);
    word(block, 56, drive.sectors);
    long chs = (long) drive.cylinders * drive.heads * drive.sectors;
    word(block, 57, (int) chs);
    word(block, 58, (int) (chs >> 16));
    word(block, 60, (int) drive.totalSectors);
    word(block, 61, (int) (drive.totalSectors >> 16));
    return block;
  }

  /** A new, empty image of that many sectors, with the header the current tools write. */
  public static void createHdf(File file, int sectors) throws IOException {
    byte[] header = new byte[IDENTIFY_OFFSET + SECTOR];
    System.arraycopy(SIGNATURE, 0, header, 0, SIGNATURE.length);
    header[6] = 0x1a;
    header[7] = 0x11;
    header[9] = (byte) header.length;
    header[10] = (byte) (header.length >> 8);
    try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
      out.setLength(0);
      out.write(header);
      out.setLength(header.length + (long) sectors * SECTOR);
    }
  }
}
