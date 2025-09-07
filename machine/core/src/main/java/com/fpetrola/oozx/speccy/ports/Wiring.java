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
 * Which of the sixteen address lines a device is wired to watch, and what it needs to see on them.
 * It is not the device's: the same chip is soldered differently in different machines, and a
 * Kempston is only on 0x1f because of how it was put in.
 */
@FunctionalInterface
public interface Wiring {
  static Wiring lines(int mask, int value) {
    return port -> (port & mask) == value;
  }

  boolean answers(int port);
}
