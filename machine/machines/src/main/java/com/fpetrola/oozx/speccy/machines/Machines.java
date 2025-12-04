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

package com.fpetrola.oozx.speccy.machines;

import com.fpetrola.oozx.Extension;
import com.fpetrola.oozx.speccy.machine.Pentagon;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecPlus2;
import com.fpetrola.oozx.speccy.machine.SpecPlus2A;
import com.fpetrola.oozx.speccy.machine.SpecPlus3;
import com.fpetrola.oozx.speccy.modules.machine.DefaultMachine;
import com.fpetrola.oozx.speccy.machine.Spec48Ntsc;
import com.fpetrola.oozx.speccy.machine.SpecPlus3E;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Every machine the emulator can be, found on the classpath like a device: the core is the
 * chassis and cannot start without one of these. Sinclair's, then the variants a snapshot never
 * names, then the clones.
 */
public class Machines extends AbstractModule implements Extension {
  /**
   * What the machines call themselves, for anything that has to offer them before one has been
   * built - a menu in the browser is drawn long before a machine exists. A copy, and copies go
   * stale, so a test asserts it against the machines themselves.
   */
  public static final java.util.List<String> MODEL_NAMES = java.util.List.of(
      "Spectrum 48K", "Spectrum 128K", "Spectrum Plus 3", "Spectrum Plus 2", "Spectrum Plus 2A",
      "Sinclair Spectrum 48K (NTSC)", "Amstrad Spectrum +3e", "Pentagon");

  protected void configure() {
    Multibinder<Spectrum> models = Multibinder.newSetBinder(binder(), Spectrum.class);
    models.addBinding().to(Spec48.class);
    models.addBinding().to(Spec128.class);
    models.addBinding().to(SpecPlus3.class);
    models.addBinding().to(SpecPlus2.class);
    models.addBinding().to(SpecPlus2A.class);
    models.addBinding().to(Spec48Ntsc.class);
    models.addBinding().to(SpecPlus3E.class);
    models.addBinding().to(Pentagon.class);
    bind(Spectrum.class).annotatedWith(DefaultMachine.class).to(Spec48.class);
  }
}
