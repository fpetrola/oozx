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

public abstract class RamInfo {
  boolean locked;
  int currentPage;
  int currentRom;
  byte lastByte;
  byte lastByte2;
  boolean special;
  boolean romcs;
  int validPages;

  abstract boolean portFromUla(int port);

  public abstract int contendDelay(long time);

  public abstract int contendDelayNoMreq(long time);
}
