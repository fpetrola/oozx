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

package com.fpetrola.oozx;

/**
 * What a machine's hardware can do, named as in Fuse's libspectrum_machine_capability so the
 * devices still to be migrated keep the same vocabulary.
 */
public enum MachineCapability {
  AY,
  MEMORY_128,
  PLUS3_MEMORY,
  PLUS3_DISK,
  TIMEX_MEMORY,
  TIMEX_VIDEO,
  TRDOS_DISK,
  /** T[SC]2068-style cartridge port. */
  TIMEX_DOCK,
  SINCLAIR_JOYSTICK,
  KEMPSTON_JOYSTICK,
  SCORP_MEMORY,
  /** M1 cycles always start on even tstate counts. */
  EVEN_M1,
  SE_MEMORY,
  NTSC,
  PENT512_MEMORY,
  PENT1024_MEMORY
}
