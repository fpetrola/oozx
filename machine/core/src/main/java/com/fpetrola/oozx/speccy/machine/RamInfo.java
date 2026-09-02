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

public abstract class RamInfo {
  public boolean locked;
  int currentPage;
  int currentRom;
  public byte lastByte;

  /** Which of its ROMs a 128 has at the bottom: 1 is the 48 BASIC, where the Beta's traps live. */
  public int currentRom() {
    return currentRom;
  }

  public byte lastByte2;
  boolean special;
  public boolean romcs;
  protected int validPages;
}
