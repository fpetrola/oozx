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
  BusAnswer read(int port);

  boolean isReader();

  boolean isWriter();

  void write(int port, byte value);

  /**
   * Whether this port latches the data lines on any I/O cycle, not looking at /RD or /WR, so a
   * read of it writes back whatever the bus settled to. Asked when it is plugged in.
   */
  default boolean ignoresReadWriteLine() {
    return false;
  }
}
