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


package com.fpetrola.oozx.speccy.modules.display;

/**
 * Decodes an attribute byte: 3 ink bits, 3 paper bits, a bright bit lifting ink into the top 8 colours,
 * and a flash bit that swaps ink/paper while {@link #reversed}. Flash phase is global (all cells flash in sync),
 * so it lives here instead of being passed per cell.
 */
public final class Colouring {
  public boolean reversed;

  public byte ink(byte attribute) {
    return flashes(attribute) && reversed ? paperBits(attribute) : inkBits(attribute);
  }

  public byte paper(byte attribute) {
    return flashes(attribute) && reversed ? inkBits(attribute) : paperBits(attribute);
  }

  public static boolean flashes(byte attribute) {
    return (attribute & 0x80) != 0;
  }

  public static byte inkBits(byte attribute) {
    return (byte) ((attribute & 0x07) + ((attribute & 0x40) >> 3));
  }

  public static byte paperBits(byte attribute) {
    return (byte) ((attribute & (0x0f << 3)) >> 3);
  }
}
