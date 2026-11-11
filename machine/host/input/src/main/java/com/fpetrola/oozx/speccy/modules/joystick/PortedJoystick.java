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

package com.fpetrola.oozx.speccy.modules.joystick;

/**
 * A joystick with a port of its own: pushing a direction moves a bit of the byte that port reads.
 * <p>
 * A Kempston reads zero at rest and a push raises its bit; a Fuller reads all ones and a push
 * pulls one down. Same wiring, opposite sense, which is the whole of what tells them apart.
 */
public final class PortedJoystick implements JoystickKind {
  private final byte[] bits;
  private final byte atRest;
  private final boolean pushRaises;
  private byte reads;
  /** How long after a read it still counts as in use: a few frames. */
  private static final long IN_USE_NANOS = 250_000_000L;
  private volatile long lastRead = System.nanoTime() - IN_USE_NANOS;

  public static PortedJoystick kempston() {
    return new PortedJoystick(new byte[]{0x02, 0x01, 0x08, 0x04, 0x10}, (byte) 0x00, true);
  }

  public static PortedJoystick timex() {
    return new PortedJoystick(TIMEX_BITS, (byte) 0x00, true);
  }

  public static PortedJoystick fuller() {
    return new PortedJoystick(TIMEX_BITS, (byte) 0xff, false);
  }

  /** The bits a Timex reads, which a Fuller uses too. */
  private static final byte[] TIMEX_BITS = {0x04, 0x08, 0x01, 0x02, (byte) 0x80};

  private PortedJoystick(byte[] bits, byte atRest, boolean pushRaises) {
    this.bits = bits;
    this.atRest = atRest;
    this.pushRaises = pushRaises;
    reads = atRest;
  }

  public void push(Direction direction, boolean pushed) {
    byte bit = bits[direction.ordinal()];
    if (pushed == pushRaises) {
      reads |= bit;
    } else {
      reads &= ~bit;
    }
  }

  public byte reads() {
    return reads;
  }

  public byte read() {
    lastRead = System.nanoTime();
    return reads;
  }

  public boolean readLately() {
    return System.nanoTime() - lastRead < IN_USE_NANOS;
  }
}
