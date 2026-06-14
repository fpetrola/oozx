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

package com.fpetrola.oozx.speccy.devices.interface1;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

/**
 * One Microdrive and its cartridge: a tape loop of sectors, each a 15-byte header then a
 * 528-byte record (its own 15-byte header, 512 data bytes, checksum), preceded by a gap and a
 * 10-zero/two-0xff preamble. One byte per data-port access; gap/sync lines toggle as marks pass.
 * MDR file format: the sectors, then one write-protect byte.
 */
final class Microdrive {

  static final int BLOCK = 543;
  static final int HEAD = 15;
  static final int RECORD = HEAD + 512 + 1;
  static final int MAX_SECTORS = 254;
  static final int MIN_SECTORS = 10;
  private static final int SYNC_OK = 0xff;
  private static final int GAP_READS = 15;

  private byte[] tape = new byte[0];
  private final int[] preamble = new int[2 * 256];
  boolean writeProtected;
  boolean inserted;
  boolean modified;
  boolean motorOn;
  String filename;
  private int head;
  private int transferred;
  private int maxBytes = HEAD;
  private int last = 0xff;
  private int gap = GAP_READS;
  private int sync = GAP_READS;

  void insert(File file) throws IOException {
    byte[] image = Files.readAllBytes(file.toPath());
    if (image.length % BLOCK != 1 || image.length / BLOCK > MAX_SECTORS) {
      throw new IOException(file.getName() + " is not a Microdrive cartridge");
    }
    tape = Arrays.copyOf(image, image.length - 1);
    writeProtected = image[image.length - 1] != 0;
    Arrays.fill(preamble, SYNC_OK);
    filename = file.getPath();
    inserted = true;
    modified = false;
  }

  /** A freshly blanked cartridge: all 0xff, no sync marks, ready for FORMAT to lay down. */
  void insertBlank(int sectors) {
    tape = new byte[sectors * BLOCK];
    Arrays.fill(tape, (byte) 0xff);
    Arrays.fill(preamble, 0);
    writeProtected = false;
    filename = null;
    inserted = true;
    modified = true;
  }

  /** Sector count range matching real cartridge manufacturing variance (171-247). */
  static int randomLength() {
    Random random = new Random();
    return 171 + random.nextInt(20) + random.nextInt(20) + random.nextInt(20) + random.nextInt(20);
  }

  void eject() {
    inserted = false;
    filename = null;
    tape = new byte[0];
  }

  void save(File file) throws IOException {
    byte[] image = Arrays.copyOf(tape, tape.length + 1);
    image[tape.length] = (byte) (writeProtected ? 1 : 0);
    Files.write(file.toPath(), image);
    filename = file.getPath();
    modified = false;
  }

  void writeProtect(boolean on) {
    writeProtected = on;
    modified = true;
  }

  int sectors() {
    return tape.length / BLOCK;
  }

  void reset() {
    head = 0;
    motorOn = false;
    gap = sync = GAP_READS;
    transferred = 0;
  }

  boolean turning() {
    return motorOn && inserted;
  }

  /** Current byte at the head while running; 0xff (neutral for the bus AND) while stopped. */
  int read() {
    if (!turning()) {
      return 0xff;
    }
    if (transferred < maxBytes) {
      last = tape[head] & 0xff;
      step();
    }
    transferred++;
    return last;
  }

  /** Counts preamble bytes; a sync mark is only produced after a complete preamble. */
  void write(int value) {
    if (!turning()) {
      return;
    }
    int block = block();
    if (transferred == 0 && value == 0x00) {
      preamble[block] = 1;
    } else if (transferred > 0 && transferred < 10 && value == 0x00) {
      preamble[block]++;
    } else if (transferred > 9 && transferred < 12 && value == 0xff) {
      preamble[block]++;
    } else if (transferred == 12 && preamble[block] == 12) {
      preamble[block] = SYNC_OK;
    }
    if (transferred > 11 && transferred < maxBytes + 12) {
      tape[head] = (byte) value;
      step();
      modified = true;
    }
    transferred++;
  }

  /** Status byte: bits 1-2 low during a formatted block's post-gap sync, bit 0 low if write-protected. */
  int status() {
    int status = 0xff;
    if (!turning()) {
      return status;
    }
    if (preamble[block()] == SYNC_OK) {
      if (gap > 0) {
        gap--;
      } else {
        status &= 0xf9;
        if (sync > 0) {
          sync--;
        } else {
          gap = sync = GAP_READS;
        }
      }
    }
    if (writeProtected) {
      status &= 0xfe;
    }
    return status;
  }

  /** A control-port access advances the head to the start of the next header/record boundary. */
  void restart() {
    if (tape.length == 0) {
      return;
    }
    while (head % BLOCK != 0 && head % BLOCK != HEAD) {
      step();
    }
    transferred = 0;
    maxBytes = head % BLOCK == 0 ? HEAD : RECORD;
  }

  private int block() {
    return head / BLOCK + (maxBytes == HEAD ? 0 : 256);
  }

  private void step() {
    if (++head >= tape.length) {
      head = 0;
    }
  }
}
