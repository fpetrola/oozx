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

package com.fpetrola.oozx.speccy.devices.ulaplus;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.display.Colouring;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

/**
 * Sixty-four colours a program says for itself, instead of the sixteen everybody shares.
 * <p>
 * Two ports: one names a register, the other reads and writes it. Sixty-four of those registers
 * are the colours, and one more says whether the machine is painting in them at all. While it is,
 * a byte is read differently: the two bits that were bright and flash pick one of four tables of
 * sixteen, the ink is the low eight of it and the paper the high eight, and nothing flashes.
 */
@Singleton
public class UlaPlusPeripheral extends AbstractPeripheral
    implements com.fpetrola.oozx.speccy.modules.display.ColoursOfItsOwn, com.fpetrola.oozx.speccy.peripherals.Pluggable {
  /** How many colours this chip has, which is how many registers of them it has. */
  public static final int COLOURS = 64;
  /** Which of the two groups of registers the named one is in: the colours, or the one about them. */
  private static final int MODE_GROUP = 0x40;
  /** The bit of the mode register that says the machine is painting in these colours. */
  private static final int PALETTE_ON = 0x01;

  /** Three bits of colour as eight, evenly spread, which is what the chip does with them. */
  private static final int[] EIGHTH = {0, 36, 73, 109, 146, 182, 219, 255};

  /**
   * A byte's two colours while these are the colours. Flash and bright stopped meaning what they
   * meant: together they name one of four tables, and the byte's two halves index into it.
   */
  static final Colouring.Reading READING = new Colouring.Reading() {
    public byte ink(byte attribute, boolean reversed) {
      return (byte) (table(attribute) + (attribute & 0x07));
    }

    public byte paper(byte attribute, boolean reversed) {
      return (byte) (table(attribute) + ((attribute >> 3) & 0x07) + 8);
    }

    private int table(byte attribute) {
      return ((attribute & 0x80) != 0 ? 32 : 0) + ((attribute & 0x40) != 0 ? 16 : 0);
    }
  };

  private final Display display;
  private final byte[] colours = new byte[COLOURS];
  private int named;
  private byte mode;
  private boolean fitted;

  @Inject
  public UlaPlusPeripheral(Display display) {
    super(List.of());
    this.display = display;
    ports(Wired.at(0xffff, 0xbf3b, new DefaultPortHandler(false, true) {
      @Override
      public void write(int port, byte value) {
        named = value & 0xff;
      }
    }), Wired.at(0xffff, 0xff3b, new DefaultPortHandler(true, true) {
      @Override
      public void write(int port, byte value) {
        written(value);
      }

      @Override
      public BusAnswer read(int port) {
        return BusAnswer.of((named & MODE_GROUP) != 0 ? mode : colours[named & (COLOURS - 1)]);
      }
    }));
  }

  /** A modification to the chip that draws: the machines that come with it, and any one fitted with it. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return fitted || machine.hasOnBoard(UlaPlusPeripheral.class);
  }

  @Override
  public void fitted(boolean modified) {
    fitted = modified;
  }

  /** Fitting it is plugging it in, so that a window clipped onto a machine is what gives it these colours. */
  @Override
  public void plugIn(boolean connected) {
    fitted(connected);
  }

  @Override
  public boolean isPluggedIn() {
    return fitted;
  }

  /** Whether the machine is painting in these colours, for whoever is showing them. */
  public boolean inUse() {
    return (mode & PALETTE_ON) != 0;
  }

  /** What one of the sixty-four is worth, as a colour rather than as the byte it was written as. */
  public int colourOf(int index) {
    return rgb(colours[index & (COLOURS - 1)]);
  }

  /** Which register was named last, which is where the next byte written would go. */
  public int named() {
    return named;
  }

  /** The same thing said the way the other devices somebody switches on and off say it. */
  public void setFitted(boolean modified) {
    fitted(modified);
  }

  private void written(byte value) {
    if ((named & MODE_GROUP) != 0) {
      mode = value;
      painting();
    } else {
      colours[named & (COLOURS - 1)] = value;
      if (painting()) display.picture().colour(named & (COLOURS - 1), rgb(value));
    }
    display.refreshAll();
  }

  /** Whether these are the colours now, putting the sixteen back the moment they are not. */
  private boolean painting() {
    boolean on = (mode & PALETTE_ON) != 0;
    display.colouring.reading(on ? READING : null);
    if (on) {
      for (int colour = 0; colour < COLOURS; colour++) display.picture().colour(colour, rgb(colours[colour]));
    } else {
      display.picture().sinclairColours();
    }
    return on;
  }

  /**
   * A colour as this chip writes it: three bits of green, three of red and two of blue. The blue
   * is short of a bit and the missing one is the other two together, so that nothing is dimmer
   * than it should be and none of the sixty-four is unreachable.
   */
  static int rgb(byte colour) {
    int red = (colour >> 2) & 0x07;
    int green = (colour >> 5) & 0x07;
    int blue = (colour & 0x03) << 1;
    if (blue != 0) blue |= 1;
    return EIGHTH[red] << 16 | EIGHTH[green] << 8 | EIGHTH[blue];
  }

  /**
   * The colours a machine was painting in when somebody wrote it down, put back. Sixty-four of
   * them and whether they were being painted in, which is everything this chip is.
   */
  @Override
  public void asItWas(int[] sixtyFour, boolean painting) {
    if (sixtyFour != null) {
      for (int colour = 0; colour < Math.min(sixtyFour.length, COLOURS); colour++) {
        colours[colour] = (byte) sixtyFour[colour];
      }
    }
    mode = (byte) (painting ? PALETTE_ON : 0);
    painting();
    display.refreshAll();
  }

  /** A machine switched on is a machine painting in the sixteen it was born with. */
  @Override
  public void machineWasReset(boolean hard) {
    named = 0;
    mode = 0;
    java.util.Arrays.fill(colours, (byte) 0);
    painting();
  }

  @Override
  public void deactivate() {
    mode = 0;
    painting();
  }
}
