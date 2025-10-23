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
 * The AY-3-8912's 16 registers plus the currently selected one, shared across its two ports
 * (select and data). Kept separate from synthesis because programs read registers back - often
 * as a presence check (write then read, falling back to beeper music if it fails).
 */
class AyRegisters {

  /** Masks each register to its real bit width, so unused bits always read back as zero. */
  private static final int[] MASK = {
      0xff, 0x0f, 0xff, 0x0f, 0xff, 0x0f, 0x1f, 0xff,
      0x1f, 0x1f, 0x1f, 0xff, 0xff, 0x0f, 0xff, 0xff
  };

  private static final int MIXER = 7;
  private static final int PORT_A = 14;
  private static final int PORT_B = 15;
  /** External pin state per I/O register; writes to it are always accepted. */
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

  /** Reads pin state directly for an input-configured I/O register, or the register ANDed
   * with the pins for output; the 8912 variant has only one I/O port, the other reads 0xff. */
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
