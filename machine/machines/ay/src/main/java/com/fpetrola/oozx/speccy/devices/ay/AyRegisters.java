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

package com.fpetrola.oozx.speccy.devices.ay;

/**
 * The sixteen registers of an AY-3-8912 and which of them is being spoken to.
 * <p>
 * Shared by the chip's two ports, because the chip shares it: one port says which register, the
 * other sends it a value. Kept here rather than inside the synthesis because a program can read
 * these back, and several do it to find out whether there is a chip at all - write a value, read
 * it, and play beeper music if it does not come back.
 */
class AyRegisters {

  /** Unused bits read back as zero, so they are dropped on the way in. */
  private static final int[] MASK = {
      0xff, 0x0f, 0xff, 0x0f, 0xff, 0x0f, 0x1f, 0xff,
      0x1f, 0x1f, 0x1f, 0xff, 0xff, 0x0f, 0xff, 0xff
  };

  private static final int MIXER = 7;
  private static final int PORT_A = 14;
  private static final int PORT_B = 15;
  /** What the outside world holds on the port; serial output is always allowed. */
  private static final int PORT_INPUT = 0xbf;

  private final int[] values = new int[16];
  private int current;

  public int current() {
    return current;
  }

  public void select(int register) {
    current = register & 0x0f;
  }

  public void write(int value) {
    values[current] = value & MASK[current];
  }

  /**
   * What the register port answers.
   * <p>
   * The chip's I/O ports read straight from the pins when set to input, and the register value
   * ANDed with the pins when set to output, which is why those two are not simply the byte that
   * was written. The 8912 has only the one I/O port, so the other reads as all ones.
   */
  public int read() {
    if (current == PORT_A) {
      return (values[MIXER] & 0x40) != 0 ? PORT_INPUT & values[PORT_A] : PORT_INPUT;
    }
    if (current == PORT_B && (values[MIXER] & 0x80) == 0) {
      return 0xff;
    }
    return values[current] & MASK[current];
  }

  public void reset() {
    current = 0;
    java.util.Arrays.fill(values, 0);
  }
}
