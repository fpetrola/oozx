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

package com.fpetrola.oozx.speccy.devices.interface1;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * ZX Net over bit 0 of the comms port, backed by a shared interop file in one of two formats:
 * raw (one byte holding the wire's current state) or interpreted (the actual packet bytes, each
 * starting with a station number). Framing here matches what the ROM's SEND-SC/WT-SC-E routines
 * put out and expect back.
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

  /** Produces the net wire's current bit value for a comms-port read. */
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

  /** Consumes a written net-wire bit, active only while the data line selects the network. */
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
