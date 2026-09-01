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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.MachineCapability;

import java.util.Set;

public interface SpectrumMachine {
  TimingsHandler.Timings getBaseTiming();

  int reset();

  void memoryMap();

  int unattachedPort(int port);

  default void shutdown() {
  }

  RamInfo getRamInfo();

  Set<MachineCapability> getCapabilities();

  default boolean has(MachineCapability capability) {
    return getCapabilities().contains(capability);
  }

  MachineTimings getTimings();

  long[] getLineTimes();

  String getName();

  default void reset(boolean b) {
    reset();
  }

  boolean portFromUla(int port);

  /** What the ULA's own port reads on the bits the keyboard does not drive, after this write to it. */
  default byte ulaPortIdleValue(byte lastOut) {
    return (byte) ((lastOut & 0x10) != 0 ? 0xff : 0xbf);
  }

  /** Whether this port gives the tape and the program a bit each, which a Timex does not. */
  default boolean separatesTapeFromSpeaker() {
    return true;
  }
}