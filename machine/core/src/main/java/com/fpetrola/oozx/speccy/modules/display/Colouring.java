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
