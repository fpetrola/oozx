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

package com.fpetrola.oozx.speccy.devices.mouse;

import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;

import java.util.function.IntSupplier;

/**
 * One of the mouse's three ports, each of which only ever answers with one number.
 * <p>
 * One class for the three rather than three classes: what distinguishes them is which port they
 * answer on and which number they give back, and both are arguments.
 */
class KempstonMousePortHandler extends DefaultPortHandler {

  private final IntSupplier reading;

  KempstonMousePortHandler(IntSupplier reading) {
    super(true, false);
    this.reading = reading;
  }

  @Override
  public BusAnswer read(int port) {
    return BusAnswer.of(reading.getAsInt());
  }
}
