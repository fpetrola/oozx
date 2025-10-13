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

public class MachineCapability {
  public static final int AY = 1 << 0;
  /* AY-3-8192 */
  public static final int _128_MEMORY = 1 << 1;
  /* 128-style memory paging */
  public static final int PLUS3_MEMORY = 1 << 2;
  /* +3-style memory paging */
  public static final int PLUS3_DISK = 1 << 3;
  /* +3-style disk drive */
  public static final int TIMEX_MEMORY = 1 << 4;
  /* TC20[46]8-style memory paging */
  public static final int TIMEX_VIDEO = 1 << 5;
  /* TC20[46]8-style video modes */
  public static final int TRDOS_DISK = 1 << 6;
  /* TRDOS-style disk drive */
  public static final int TIMEX_DOCK = 1 << 7;
  /* T[SC]2068-style cartridge port */
  public static final int SINCLAIR_JOYSTICK = 1 << 8;
  /* Sinclair-style joystick ports */
  public static final int KEMPSTON_JOYSTICK = 1 << 9;
  /* Kempston-style joystick ports */
  public static final int SCORP_MEMORY = 1 << 10;
  /* Scorpion-style memory paging */
  public static final int EVEN_M1 = 1 << 11;
  /* M1 cycles always start on even tstate counts */
  public static final int SE_MEMORY = 1 << 12;
  /* SE-style memory paging */
  public static final int NTSC = 1 << 13;
  /* NTSC display */
  public static final int PENT512_MEMORY = 1 << 14;
  /* Pentagon 512-style memory paging */
  public static final int PENT1024_MEMORY = 1 << 15;
  /* Pentagon 1024-style memory paging */
}
