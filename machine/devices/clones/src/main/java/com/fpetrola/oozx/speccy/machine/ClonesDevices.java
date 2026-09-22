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
 * Every machine that is not Sinclair's or Amstrad's: the Russian ones, the Timexes, the ones
 * built in Brazil, Argentina and Spain, and the modern remakes.
 * <p>
 * They are a jar rather than part of the emulator because that is what they are to somebody
 * using it: a 48K is the machine, and a Scorpion is something you go and get. What the build
 * carries is Sinclair's line; these add themselves to it the way a board does, and the two
 * parts each of them needs that are not machines - the Pentagon 1024's paging and the Inves's
 * fault - come along in the same jar.
 */
public class ClonesDevices extends AbstractModule implements Extension {

  protected void configure() {
    Multibinder<Peripheral> parts = Multibinder.newSetBinder(binder(), Peripheral.class);
    parts.addBinding().to(Pentagon1024MemoryPeripheral.class);
    parts.addBinding().to(InvesInterruptFault.class);

    Multibinder<Spectrum> models = Multibinder.newSetBinder(binder(), Spectrum.class);
    models.addBinding().to(Pentagon.class);
    models.addBinding().to(Pentagon512.class);
    models.addBinding().to(Pentagon1024.class);
    models.addBinding().to(Tc2048.class);
    models.addBinding().to(Tc2068.class);
    models.addBinding().to(Ts2068.class);
    models.addBinding().to(SpecSe.class);
    models.addBinding().to(Scorpion.class);
    models.addBinding().to(Tk90x.class);
    models.addBinding().to(Tk95.class);
    models.addBinding().to(CzSpectrum.class);
    models.addBinding().to(CzSpectrumPlus.class);
    models.addBinding().to(Inves.class);
    models.addBinding().to(Chrome.class);
  }
}
