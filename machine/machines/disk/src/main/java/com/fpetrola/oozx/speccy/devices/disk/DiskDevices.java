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

package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.fpetrola.oozx.config.Settings;
import com.fpetrola.oozx.Extension;

/** The floppy controller a +3 has. */
public class DiskDevices extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder<Peripheral> devices = Multibinder.newSetBinder(binder(), Peripheral.class);
    devices.addBinding().to(Upd765Peripheral.class);
    devices.addBinding().to(Beta128Peripheral.class);
    Settings.mirror(binder(), "floppy", Fdd.Limits.class, "fortyTrackMax", "eightyTrackMax");
    Settings.mirror(binder(), "beta128", Beta128Peripheral.TrDos.class, "bootOn48k", "autoBoot");
    Settings.mirror(binder(), "machine.plus3", Upd765Peripheral.class, "detectSpeedlock", "driveA", "driveB");
  }
}
