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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An MMC or SD card as the machine talks to it: one byte in and one byte out per access, which
 * is SPI with the clock left out. The host sends six bytes - a command with its top bits 01,
 * four of argument and a checksum - and then clocks 0xff until the card answers; a read is a
 * one-byte answer, a 0xfe token and 512 bytes with two of checksum, a write is the same the
 * other way round with a token of acceptance and a moment of the card being busy.
 * <p>
 * The card is a file of 512-byte sectors, addressed by byte on a card of this size, and what is
 * written waits in memory until it is committed. Written from the MMC and SD specifications:
 * libspectrum's own is not in this tree.
 */
public class MmcCard implements MassStorage {

  public static final int SECTOR = 512;
  private static final int R1_IDLE = 0x01;
  private static final int R1_OK = 0x00;
  private static final int R1_ILLEGAL = 0x04;
  private static final int START_BLOCK = 0xfe;
  private static final int DATA_ACCEPTED = 0x05;

  private enum Phase { COMMAND, RESPONSE, READ, WRITE_TOKEN, WRITE, WRITE_ANSWER }

  private RandomAccessFile file;
  private String filename;
  private long sectors;
  private final Map<Long, byte[]> changed = new HashMap<>();

  private final int[] command = new int[6];
  private int commandLength;
  private final byte[] response = new byte[SECTOR + 4];
  private int responseLength;
  private int responseAt;
  private final byte[] block = new byte[SECTOR];
  private int blockAt;
  private Phase phase = Phase.COMMAND;
  private boolean idle = true;
  private boolean expectingApp;
  private long writingTo;

  @Override
  public boolean present() {
    return file != null;
  }

  @Override
  public String filename() {
    return filename;
  }

  @Override
  public boolean dirty() {
    return !changed.isEmpty();
  }

  @Override
  public long sectors() {
    return sectors;
  }

  public void insert(File image) throws IOException {
    eject();
    file = new RandomAccessFile(image, "r");
    filename = image.getPath();
    sectors = file.length() / SECTOR;
    reset();
  }

  public void eject() {
    try {
      if (file != null) file.close();
    } catch (IOException ignored) {
    }
    file = null;
    filename = null;
    sectors = 0;
    changed.clear();
  }

  @Override
  public void commit(File into) throws IOException {
    File source = new File(filename);
    if (!into.getAbsoluteFile().equals(source.getAbsoluteFile())) {
      Files.copy(source.toPath(), into.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    try (RandomAccessFile out = new RandomAccessFile(into, "rw")) {
      for (Map.Entry<Long, byte[]> sector : changed.entrySet()) {
        out.seek(sector.getKey() * SECTOR);
        out.write(sector.getValue());
      }
    }
    changed.clear();
  }

  /** A new card of that many sectors, all zeros, which the machine can then format. */
  public static void createImage(File image, int sectors) throws IOException {
    try (RandomAccessFile out = new RandomAccessFile(image, "rw")) {
      out.setLength(0);
      out.setLength((long) sectors * SECTOR);
    }
  }

  public void reset() {
    phase = Phase.COMMAND;
    commandLength = 0;
    responseLength = responseAt = 0;
    idle = true;
    expectingApp = false;
  }

  /** What the card puts on the bus for this access; every access clocks a byte both ways. */
  public int read() {
    if (file == null) {
      return 0xff;
    }
    if (responseAt < responseLength) {
      int b = response[responseAt++] & 0xff;
      if (responseAt == responseLength && phase == Phase.READ) {
        phase = Phase.COMMAND;
      }
      return b;
    }
    return 0xff;
  }

  public void write(int value) {
    if (file == null) {
      return;
    }
    value &= 0xff;
    switch (phase) {
      case COMMAND, RESPONSE, READ -> command(value);
      case WRITE_TOKEN -> {
        if (value == START_BLOCK) {
          phase = Phase.WRITE;
          blockAt = 0;
        }
      }
      case WRITE -> {
        if (blockAt < SECTOR) {
          block[blockAt] = (byte) value;
        }
        if (++blockAt == SECTOR + 2) {
          changed.put(writingTo, block.clone());
          answer(DATA_ACCEPTED, 0x00, 0xff);
          phase = Phase.COMMAND;
        }
      }
      default -> {
      }
    }
  }

  /** The six bytes of a command, taken as they come; anything before the first is clocking. */
  private void command(int value) {
    if (commandLength == 0 && (value & 0xc0) != 0x40) {
      return;
    }
    command[commandLength++] = value;
    if (commandLength < command.length) {
      return;
    }
    commandLength = 0;
    execute(command[0] & 0x3f, (long) command[1] << 24 | (long) command[2] << 16 | command[3] << 8 | command[4]);
  }

  private void execute(int which, long argument) {
    boolean app = expectingApp;
    expectingApp = false;
    if (app) {
      switch (which) {
        // The one that ends the card's idle state, which is how a host waits for it to be ready.
        case 41 -> {
          idle = false;
          answer(R1_OK);
        }
        default -> answer(R1_ILLEGAL | status());
      }
      return;
    }
    switch (which) {
      case 0 -> {
        idle = true;
        answer(R1_IDLE);
      }
      case 1 -> {
        idle = false;
        answer(R1_OK);
      }
      // The interface condition: a card of this generation does not know it.
      case 8 -> answer(R1_ILLEGAL | status());
      case 9 -> answerBlock(csd());
      case 10 -> answerBlock(cid());
      case 12, 16, 55 -> {
        expectingApp = which == 55;
        answer(status());
      }
      case 17 -> {
        long sector = argument / SECTOR;
        if (sector >= sectors) {
          answer(R1_ILLEGAL | status());
          return;
        }
        try {
          answerBlock(sectorAt(sector));
        } catch (IOException cannot) {
          answer(R1_ILLEGAL | status());
        }
      }
      case 24 -> {
        writingTo = argument / SECTOR;
        if (writingTo >= sectors) {
          answer(R1_ILLEGAL | status());
          return;
        }
        answer(status());
        phase = Phase.WRITE_TOKEN;
      }
      // The operating conditions register: a card that is not high capacity, so bytes address it.
      case 58 -> answer(status(), 0x00, 0xff, 0x80, 0x00);
      case 59 -> answer(status());
      default -> answer(R1_ILLEGAL | status());
    }
  }

  private int status() {
    return idle ? R1_IDLE : R1_OK;
  }

  private byte[] sectorAt(long sector) throws IOException {
    byte[] data = changed.get(sector);
    if (data != null) {
      return data.clone();
    }
    data = new byte[SECTOR];
    file.seek(sector * SECTOR);
    file.readFully(data);
    return data;
  }

  private void answer(int... bytes) {
    for (int i = 0; i < bytes.length; i++) {
      response[i] = (byte) bytes[i];
    }
    responseLength = bytes.length;
    responseAt = 0;
    phase = Phase.RESPONSE;
  }

  /** The one-byte answer, the token that says the data follows, the data, and two of checksum. */
  private void answerBlock(byte[] data) {
    response[0] = (byte) status();
    response[1] = (byte) START_BLOCK;
    System.arraycopy(data, 0, response, 2, data.length);
    response[data.length + 2] = (byte) 0xff;
    response[data.length + 3] = (byte) 0xff;
    responseLength = data.length + 4;
    responseAt = 0;
    phase = Phase.READ;
  }

  /** The card-specific data: version 1, with the size written the way that version has it. */
  private byte[] csd() {
    byte[] csd = new byte[16];
    Arrays.fill(csd, (byte) 0x00);
    csd[0] = 0x00;
    csd[1] = 0x0e;
    csd[2] = 0x00;
    csd[3] = 0x32;
    csd[4] = 0x5b;
    csd[5] = 0x59;
    // C_SIZE, with a multiplier of 512 blocks of 512 bytes: the size in sectors over 512, less one.
    long size = Math.max(1, sectors / 512) - 1;
    csd[6] = (byte) (0x80 | size >> 10 & 0x03);
    csd[7] = (byte) (size >> 2);
    csd[8] = (byte) (size << 6 & 0xc0 | 0x3f);
    csd[9] = (byte) 0xf9;
    csd[10] = (byte) 0x96;
    csd[11] = (byte) 0x40;
    csd[12] = 0x0f;
    csd[13] = 0x00;
    csd[14] = 0x00;
    csd[15] = 0x01;
    return csd;
  }

  private byte[] cid() {
    byte[] cid = new byte[16];
    cid[0] = 0x01;
    cid[1] = 'O';
    cid[2] = 'O';
    cid[3] = 'Z';
    cid[4] = 'X';
    cid[5] = 'C';
    cid[6] = 'A';
    cid[7] = 'R';
    cid[8] = 'D';
    cid[9] = 0x10;
    cid[15] = 0x01;
    return cid;
  }
}
