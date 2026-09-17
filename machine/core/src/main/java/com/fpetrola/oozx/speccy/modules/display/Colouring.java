/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
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
 * and a flash bit that swaps ink/paper while reversed. Flash phase is global (all cells flash in sync),
 * so it lives here instead of being passed per cell.
 * <p>
 * Both answers for all 256 bytes are worked out whenever the rule or the flash phase changes, which
 * is a few times a second, so that painting a cell is a look-up and not a call.
 */
public final class Colouring {
  /**
   * How a byte says which two colours a cell is drawn in. The Sinclair way is one of these, and a
   * machine whose colours come out of a byte differently gives its own rather than being asked for.
   * <p>
   * Asked once per attribute when it is given and whenever the flash phase turns, never while
   * painting: what it answers for all 256 bytes is kept in {@link #twoColours}.
   */
  public interface Reading {
    byte ink(byte attribute, boolean reversed);

    byte paper(byte attribute, boolean reversed);
  }

  /** Three bits each way, a bit that lifts the ink into the bright eight, and a bit that swaps them. */
  public static final Reading SINCLAIR = new Reading() {
    public byte ink(byte attribute, boolean reversed) {
      return flashes(attribute) && reversed ? paperBits(attribute) : inkBits(attribute);
    }

    public byte paper(byte attribute, boolean reversed) {
      return flashes(attribute) && reversed ? inkBits(attribute) : paperBits(attribute);
    }
  };

  private Reading reading = SINCLAIR;
  private boolean reversed;

  /** Ink in the low byte and paper in the high one, for every byte an attribute can be. */
  private final short[] twoColours = new short[256];

  public Colouring() {
    fillIn();
  }

  public void reading(Reading another) {
    reading = another == null ? SINCLAIR : another;
    fillIn();
  }

  /** Whether the flashing cells are showing their colours the other way round at the moment. */
  public boolean reversed() {
    return reversed;
  }

  public void reversed(boolean theOtherWayRound) {
    reversed = theOtherWayRound;
    fillIn();
  }

  private void fillIn() {
    for (int byteItCouldBe = 0; byteItCouldBe < twoColours.length; byteItCouldBe++) {
      byte attribute = (byte) byteItCouldBe;
      twoColours[byteItCouldBe] = (short) (((reading.paper(attribute, reversed) & 0xff) << 8)
          | (reading.ink(attribute, reversed) & 0xff));
    }
  }

  public byte ink(byte attribute) {
    return (byte) twoColours[attribute & 0xff];
  }

  public byte paper(byte attribute) {
    return (byte) (twoColours[attribute & 0xff] >> 8);
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
