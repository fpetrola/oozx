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

import com.fpetrola.oozx.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.IntConsumer;

/**
 * The serial side of the Interface 1's ULA: the Spectrum bit-bangs its transmission on bit 0 of
 * the comms port and reads its reception on bit 7, one bit per access, and this is the other
 * end of both wires, framing what comes out into bytes and serialising what goes in.
 * <p>
 * The wire's far end is either the desk's terminal or a pair of files, the way Fuse has it: on
 * those, a 0x00 is an escape - 0x00 0x00 and 0x00 0x01 drop and raise DTR, 0x00 0x02 and
 * 0x00 0x03 report CTS, 0x00 '*' is a real zero. Without handshaking, DTR is simply up while
 * both ends are connected. From Fuse's if1.c.
 */
public final class Rs232 {

  private static final int EMPTY = 0x100;

  private final Settings settings;
  private InputStream rx;
  private OutputStream tx;
  private IntConsumer terminal;
  private final Deque<Integer> typed = new ArrayDeque<>();
  private int cts = 2;
  int dtr;
  private int lineIn;
  private int dataIn;
  private int countIn;
  private int dataOut;
  private int countOut;
  private boolean escape;
  private int buffer = EMPTY;

  Rs232(Settings settings) {
    this.settings = settings;
  }

  void reset() {
    cts = 2;
    escape = false;
  }

  /** The desk's terminal: what the Spectrum sends goes there, and what is typed there comes in. */
  public void terminal(IntConsumer terminal) {
    this.terminal = terminal;
    typed.clear();
    updateDtr();
  }

  public void type(int b) {
    typed.add(b & 0xff);
  }

  public void plugRx(File file) throws IOException {
    unplugRx();
    rx = new FileInputStream(file);
    buffer = EMPTY;
    updateDtr();
  }

  public void plugTx(File file) throws IOException {
    unplugTx();
    tx = new FileOutputStream(file, true);
    updateDtr();
  }

  public void unplugRx() {
    close(rx);
    rx = null;
    updateDtr();
  }

  public void unplugTx() {
    close(tx);
    tx = null;
    dtr = 0;
    updateDtr();
  }

  public boolean rxPlugged() {
    return rx != null;
  }

  public boolean txPlugged() {
    return tx != null;
  }

  private boolean receiving() {
    return rx != null || terminal != null;
  }

  private boolean transmitting() {
    return tx != null || terminal != null;
  }

  private void updateDtr() {
    if (!settings.current.rs232Handshake) {
      dtr = receiving() && transmitting() ? 1 : 0;
    }
  }

  private static void close(AutoCloseable stream) {
    try {
      if (stream != null) stream.close();
    } catch (Exception ignored) {
    }
  }

  /** The status port is where the ROM waits for DTR, so that is where the far end is listened to. */
  void poll() {
    if (buffer > 0xff) {
      int b = fetch();
      if (b >= 0) buffer = b;
    }
  }

  /** One byte from the far end, with the escapes taken out and acted on, or -1. */
  private int fetch() {
    if (!typed.isEmpty()) {
      return typed.poll();
    }
    if (rx == null) {
      return -1;
    }
    try {
      while (rx.available() > 0) {
        int b = rx.read();
        if (b < 0) {
          return -1;
        }
        if (escape) {
          escape = false;
          if (b == '*') return 0;
          if (b == 0x00 && settings.current.rs232Handshake) dtr = 0;
          if (b == 0x01 && settings.current.rs232Handshake) dtr = 1;
        } else if (b == 0x00) {
          escape = true;
        } else {
          return b;
        }
      }
    } catch (IOException gone) {
      unplugRx();
    }
    return -1;
  }

  private boolean readByte() {
    if (buffer <= 0xff) {
      dataIn = buffer;
      buffer = EMPTY;
      return true;
    }
    int b = fetch();
    if (b < 0) {
      return false;
    }
    dataIn = b;
    return true;
  }

  /** Bit 7 of the comms port: the next bit of the byte being received, framed the way the ROM samples it. */
  int lineIn() {
    if (!receiving()) {
      return lineIn;
    }
    if (cts == 0) {
      countIn = 0;
      lineIn = 0;
    } else if (countIn == 0) {
      if (readByte()) countIn++;
      lineIn = 0;
    } else if (countIn < 5) {
      lineIn = 1;
      countIn++;
    } else if (countIn < 13) {
      lineIn = (dataIn & 0x01) != 0 ? 0 : 1;
      dataIn >>= 1;
      countIn++;
    } else {
      countIn = 0;
    }
    return lineIn;
  }

  /** Bit 0 of the comms port written: start bit, eight inverted data bits, stop bits, then the byte is out. */
  void lineOut(int value) {
    if (!transmitting()) {
      return;
    }
    int bit = value & 0x01;
    if (countOut == 0 && bit == 0) {
      countOut++;
    } else if (countOut == 1) {
      countOut = cts != 0 || bit == 0 ? -1 : 2;
    } else if (countOut >= 2 && countOut <= 9) {
      dataOut = dataOut >> 1 | (bit != 0 ? 0 : 0x80);
      countOut++;
    } else if (countOut >= 10 && countOut <= 11) {
      countOut = bit != 0 ? -1 : countOut + 1;
    } else if (countOut == 12) {
      countOut = bit == 0 ? -1 : 13;
    } else if (countOut == 13 && bit != 0) {
      countOut = -1;
    }
    if (countOut == -1) {
      countOut = 13;
      dataOut = '?';
      toWire(0x00);
    }
    if (countOut == 13) {
      send(dataOut);
      countOut = 0;
    }
  }

  private void send(int b) {
    if (terminal != null) {
      terminal.accept(b);
    }
    if (b == 0x00) {
      toWire(0x00);
      b = '*';
    }
    toWire(b);
  }

  private void toWire(int b) {
    if (tx == null) {
      return;
    }
    try {
      tx.write(b);
      tx.flush();
    } catch (IOException gone) {
      unplugTx();
    }
  }

  /** The CTS bit of the control register, which the far end is told about when handshaking. */
  void cts(int bit) {
    if (settings.current.rs232Handshake && tx != null && cts != bit) {
      toWire(0x00);
      toWire(bit != 0 ? 0x03 : 0x02);
    }
    cts = bit;
  }

  /** The comms data line going up starts both directions over. */
  void restartFraming() {
    countOut = dataOut = countIn = dataIn = 0;
  }
}
