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
