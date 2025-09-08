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

package com.fpetrola.oozx.speccy.ports;

/**
 * What a device answers when a port is read: the byte it puts on the bus, and whether it drove the
 * bus at all. A port nothing answers is left floating, and the machine says what a floating bus
 * reads as.
 * <p>
 * This used to be a byte returned and a one-element array the caller passed in for the second
 * half, which is how it is said in C. There are only 256 bytes a device can drive, so the
 * answers are made once: what a read costs then does not depend on whether the JIT saw through
 * the call that returned it, which at a port read eight times a frame it was measured not to.
 */
public record BusAnswer(int value, boolean driven) {

  /** Nobody answered: all ones, and nothing on the bus. */
  public static final BusAnswer NONE = new BusAnswer(0xff, false);

  private static final BusAnswer[] DRIVEN = new BusAnswer[0x100];

  static {
    for (int value = 0; value < DRIVEN.length; value++) {
      DRIVEN[value] = new BusAnswer(value, true);
    }
  }

  /** That byte, driven onto the bus. */
  public static BusAnswer of(int value) {
    return DRIVEN[value & 0xff];
  }
}
