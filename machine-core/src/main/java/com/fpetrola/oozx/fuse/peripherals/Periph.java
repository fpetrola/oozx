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

package com.fpetrola.oozx.fuse.peripherals;

public class Periph {
  // Enum for peripheral types
  public enum Type {
    UNKNOWN,
    _128_MEMORY,
    AY,
    AY_FULL_DECODE,
    AY_PLUS3,
    AY_TIMEX,
    AY_TIMEX_WITH_JOYSTICK,
    BETA128,
    BETA128_PENTAGON,
    BETA128_PENTAGON_LATE,
    COVOX_DD,
    COVOX_FB,
    DIVIDE,
    DIVMMC,
    PLUSD,
    DIDAKTIK80,
    DISCIPLE,
    FULLER,
    INTERFACE1,
    INTERFACE2,
    KEMPSTON,
    KEMPSTON_LOOSE,
    KEMPSTON_MOUSE,
    MELODIK,
    MULTIFACE_1,
    MULTIFACE_128,
    MULTIFACE_3,
    OPUS,
    PARALLEL_PRINTER,
    PENTAGON1024_MEMORY,
    PLUS3_MEMORY,
    SCLD,
    SE_MEMORY,
    SIMPLEIDE,
    SPECCYBOOT,
    SPECDRUM,
    SPECTRANET,
    TTX2000S,
    ULA,
    ULA_FULL_DECODE,
    UPD765,
    USOURCE,
    ZXATASP,
    ZXCF,
    ZXMMC,
    ZXPRINTER,
    ZXPRINTER_FULL_DECODE;

    private final Class<? extends ZxPeripheral> aClass;

    Type(Class<? extends ZxPeripheral> aClass) {
      this.aClass = aClass;
    }

    Type() {
      this(null);
    }

    public Class<? extends ZxPeripheral> getZxPeripheralClass() {
      return aClass;
    }
  }

  // Enum for peripheral presence
  public enum Present {
    NEVER,
    OPTIONAL,
    ALWAYS
  }
}
