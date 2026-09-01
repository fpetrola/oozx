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

public interface PortHandler {
  byte read(int port, byte[] attached);

  int getMask();

  int getValue();

  boolean isReader();

  boolean isWriter();

  void write(int port, byte value);

  /** Whether this port wants to hear reads of it that it did not answer. Asked when it is plugged in. */
  default boolean listensToBusReads() {
    return false;
  }

  /** A read of this port, with whatever the bus was left holding - nothing here answered it. */
  default void busRead(int port, byte onTheBus) {
  }
}
