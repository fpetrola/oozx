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

  public boolean push(Direction direction, boolean pushed) {
    byte bit = bits[direction.ordinal()];
    if (pushed == pushRaises) {
      reads |= bit;
    } else {
      reads &= ~bit;
    }
    return true;
  }

  public byte reads() {
    return reads;
  }
}
