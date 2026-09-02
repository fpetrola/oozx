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
import java.io.RandomAccessFile;

/**
 * The ZX Net wire, which is bit 0 of the comms port both ways, on a file that other emulators
 * share: raw, the file is one byte that is the state of the wire; interpreted, it is the bytes
 * that go over it, each packet starting with the station number, framed here from the bits the
 * ROM's SEND-SC and WT-SC-E routines put out and expect back. From Fuse's if1.c.
 */
public final class ZxNet {

  private static final int IDLE_POLLS = 0x0100;
  private static final int SENDING = 0x0200;

  private RandomAccessFile wire;
  private boolean raw;
  private int net;
  private int data;
  private int state;

  public void plug(File file, boolean raw) throws IOException {
    unplug();
    wire = new RandomAccessFile(file, "rw");
    this.raw = raw;
  }

  public void unplug() {
    try {
      if (wire != null) wire.close();
    } catch (IOException ignored) {
    }
    wire = null;
  }

  public boolean plugged() {
    return wire != null;
  }

  void reset() {
    net = 0;
  }

  /** Bit 0 of the comms port read. */
  int lineIn() {
    if (wire == null) {
      return net;
    }
    try {
      if (raw) {
        int b = wire.read();
        if (b >= 0) net = b;
      } else if (state < IDLE_POLLS) {
        state++;
        net = 0;
      } else if (state == IDLE_POLLS) {
        int b = wire.read();
        if (b >= 0) {
          data = b;
          state++;
          net = 1;
        }
      } else if (state == IDLE_POLLS + 1) {
        state++;
        net = 1;
      } else if (state < IDLE_POLLS + 10) {
        state++;
        net = data & 1;
        data >>= 1;
      } else {
        net = 0;
        state = 0;
      }
    } catch (IOException gone) {
      unplug();
    }
    return net;
  }

  /** Bit 0 of the comms port written, while the comms data line selects the net. */
  void lineOut(int value) {
    if (wire == null) {
      return;
    }
    int bit = value & 0x01;
    try {
      if (raw) {
        net = bit != 0 ? 0 : 1;
        wire.seek(0);
        wire.write(net);
      } else {
        if (state >= SENDING && state < SENDING + 8) {
          state++;
          data = data << 1 | (bit != 0 ? 0 : 1);
        } else if (state == SENDING + 8) {
          data &= 0xff;
          state++;
          wire.write(data);
        } else if (state > 192 && state < SENDING && bit == 0) {
          state = SENDING;
        }
        net = bit != 0 ? 0 : 1;
      }
    } catch (IOException gone) {
      unplug();
    }
  }
}
