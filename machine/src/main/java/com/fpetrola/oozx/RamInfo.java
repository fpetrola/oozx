/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

package com.fpetrola.oozx;

// Structure to hold RAM information
public class RamInfo {
  Spectrum.PortFromUlaFunction portFromUla; // Is this port result supplied by the ULA?
  Spectrum.ContentionDelayFunction contendDelay; // Delay with MREQ active
  Spectrum.ContentionDelayFunction contendDelayNoMreq; // Delay without MREQ
  boolean locked; // Is the memory configuration locked?
  int currentPage; // Current paged memory page
  int currentRom; // Current paged ROM
  byte lastByte; // Last byte sent to the 128K port
  byte lastByte2; // Last byte sent to +3 port
  boolean special; // Is a +3 special config in use?
  boolean romcs; // Is the /ROMCS line low?
  int validPages; // Available RAM pages
}
