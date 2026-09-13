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

package com.fpetrola.oozx.speccy.devices.scld;

import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.ScreenLayout;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The one byte that decides what a Timex machine shows, written to and read back from port 0xff.
 * <p>
 * Its bottom three bits pick between the two display files and between colour by cell and colour
 * by line; the three above them are the pair of colours a hi-res picture is drawn in. A machine
 * with no such chip answers that port from the bus instead, which is why this is a device.
 */
@Singleton
public class ScldPortHandler extends DefaultPortHandler {
  /** The second display file rather than the first. */
  public static final int SECOND_FILE = 0x01;
  /** Colour a line at a time, taken from the other file. */
  public static final int COLOUR_PER_LINE = 0x02;
  /** Five hundred and twelve pixels across, in one pair of colours. */
  public static final int HI_RES = 0x04;

  private final Display display;
  private byte register;

  @Inject
  public ScldPortHandler(Display display) {
    super(true, true);
    this.display = display;
  }

  @Override
  public BusAnswer read(int port) {
    return BusAnswer.of(register & 0xff);
  }

  @Override
  public void write(int port, byte value) {
    if (value == register) return;
    display.screenChanging();
    register = value;
    ScreenLayout layout = display.layout;
    layout.file = (value & SECOND_FILE) != 0 ? ScreenLayout.SECOND_FILE : 0;
    layout.colourPerLine = (value & COLOUR_PER_LINE) != 0;
    display.refreshAll();
  }

  public byte register() {
    return register;
  }

  public void reset() {
    write(0, (byte) 0);
  }
}
