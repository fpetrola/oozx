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
