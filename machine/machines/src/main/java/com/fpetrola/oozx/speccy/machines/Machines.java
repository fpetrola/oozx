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

package com.fpetrola.oozx.speccy.machines;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.modules.machine.DefaultMachine;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * The machine this build carries, which is the 48K.
 * <p>
 * One has to be here: the core is the chassis and cannot start without a machine, and what a
 * Spectrum is to whoever asks for one is the 48K. Every other machine - the 128, Amstrad's, the
 * clones, the remakes - arrives in a jar and adds itself to this the way a board does.
 * <p>
 * Not every machine there is. Anything that is not Sinclair's arrives in a jar and adds itself
 * to these the way a board does - the clones, the Timexes, Chloe's two - so a build that nobody
 * added anything to is a Spectrum and not a museum.
 */
public class Machines extends AbstractModule implements Extension {
  protected void configure() {
    Multibinder<Spectrum> models = Multibinder.newSetBinder(binder(), Spectrum.class);
    models.addBinding().to(Spec48.class);
    // Which machine that is, is a setting: the file names one and this build has it, or nothing
    // named one and a Spectrum is a 48K. Bound rather than read as a machine starts, so what a new
    // machine becomes is answered the one way everything else here is answered.
    bind(Spectrum.class).annotatedWith(DefaultMachine.class).toProvider(TheOneChosen.class);
  }

  /**
   * The machine named in the file under {@code machine.model}, which is what the settings write
   * when nobody is being configured in particular, and the 48K when nothing names one.
   * <p>
   * The instance comes from the models bound above rather than being one of its own: switching
   * machines compares the one asked for against those, and a second 48K is not among them.
   */
  static class TheOneChosen implements com.google.inject.Provider<Spectrum> {
    @com.google.inject.Inject
    private java.util.Set<Spectrum> models;
    @com.google.inject.Inject
    private com.fpetrola.oozx.config.Configuration configuration;
    @com.google.inject.Inject
    private Spec48 whatASpectrumIs;

    public Spectrum get() {
      Object named = configuration.valueOf("machine", "model", String.class);
      return models.stream().filter(model -> model.getName().equals(named)).findFirst()
          .orElse(whatASpectrumIs);
    }
  }
}
