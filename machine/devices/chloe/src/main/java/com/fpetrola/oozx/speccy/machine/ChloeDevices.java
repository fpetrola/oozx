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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Chloe's two machines, which are machines that arrive rather than machines the emulator was
 * written with: a jar that adds them to the ones there are, the same way a board adds itself.
 * <p>
 * They are here and not in the emulator's own list because they are the only models with ULAplus
 * soldered on, and having them in meant every machine this build made carried a ULAplus whether
 * or not anybody asked for one.
 */
public class ChloeDevices extends AbstractModule implements Extension {

  protected void configure() {
    Multibinder.newSetBinder(binder(), Peripheral.class).addBinding().to(ChloeUla2Peripheral.class);
    Multibinder<Spectrum> models = Multibinder.newSetBinder(binder(), Spectrum.class);
    models.addBinding().to(Chloe140Se.class);
    models.addBinding().to(Chloe280Se.class);
  }
}
