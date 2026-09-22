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
 */package com.fpetrola.oozx.speccy.devices.tape;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/** A tape deck on every machine: the EAR line is the ULA's to read, the deck is this. */
public class TapeDevices extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(Tape.class);
    // What is in the socket, while this jar is here: the ULA reads the line and the deck drives
    // it. With this jar gone the socket is empty, which is a machine with no lead in the back.
    com.google.inject.multibindings.OptionalBinder
        .newOptionalBinder(binder(), com.fpetrola.oozx.speccy.modules.ula.EarLine.class)
        .setBinding().to(TapeInTheSocket.class);
    // Only these two: the type this comes from was generated from a schema and offers seven, and
    // the deck reads two of them. Declaring the other five would put five controls in front of
    // somebody that change a field nothing ever looks at - the very thing the settings window was
    // full of. Fast loading and loading a tape by itself are real, and are elsewhere: the speed
    // the machine runs at, and the loader that types LOAD.
    com.fpetrola.oozx.config.Settings.mirror(binder(), "tape",
        com.fpetrola.oozx.speccy.modules.tape.TapeSettingsType.class,
        "highSamplingFreq", "invertedEar");
  }
}
