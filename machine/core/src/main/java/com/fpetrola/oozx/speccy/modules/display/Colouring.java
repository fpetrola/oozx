/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

  private static byte inkBits(byte attribute) {
    return (byte) ((attribute & 0x07) + ((attribute & 0x40) >> 3));
  }

  private static byte paperBits(byte attribute) {
    return (byte) ((attribute & (0x0f << 3)) >> 3);
  }
}
