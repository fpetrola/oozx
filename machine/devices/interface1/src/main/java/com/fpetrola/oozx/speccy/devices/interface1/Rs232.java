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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.IntConsumer;

/**
 * The far end of Interface 1's bit-banged serial link: the Spectrum toggles bit 0 (transmit) and
 * samples bit 7 (receive) of the comms port one bit at a time; this class frames/deframes those
 * bits into bytes. Connects to a live terminal or a pair of files, where 0x00 escapes a control
 * byte (DTR up/down, CTS report, or a literal zero); with no handshaking DTR is simply up
 * whenever both ends are connected.
 */
public final class Rs232 {

  private static final int EMPTY = 0x100;

  private final Interface1Peripheral owner;
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

  Rs232(Interface1Peripheral owner) {
    this.owner = owner;
  }

  void reset() {
    cts = 2;
    escape = false;
  }

  /** Connects a live terminal: outgoing bytes go to it, and it can inject incoming ones. */
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
    if (!owner.rs232Handshake()) {
      dtr = receiving() && transmitting() ? 1 : 0;
    }
  }

  private static void close(AutoCloseable stream) {
    try {
      if (stream != null) stream.close();
    } catch (Exception ignored) {
    }
  }

  /** Called when the status port is read, since that is where the ROM waits on DTR/data. */
  void poll() {
    if (buffer > 0xff) {
      int b = fetch();
      if (b >= 0) buffer = b;
    }
  }

  /** Reads and de-escapes one byte from the far end, applying any control codes; -1 if none. */
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
          if (b == 0x00 && owner.rs232Handshake()) dtr = 0;
          if (b == 0x01 && owner.rs232Handshake()) dtr = 1;
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

  /** Produces the next receive-line bit for comms port bit 7, in the ROM's expected UART framing. */
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

  /** Consumes one written transmit-line bit (comms port bit 0): start, 8 inverted data, stop bits. */
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

  /** Updates CTS from the control register, notifying the far end if handshaking is enabled. */
  void cts(int bit) {
    if (owner.rs232Handshake() && tx != null && cts != bit) {
      toWire(0x00);
      toWire(bit != 0 ? 0x03 : 0x02);
    }
    cts = bit;
  }

  /** Resets both transmit and receive framing state to their start. */
  void restartFraming() {
    countOut = dataOut = countIn = dataIn = 0;
  }
}
