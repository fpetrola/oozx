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
import com.fpetrola.oozx.speccy.machine.Pentagon512;
import com.fpetrola.oozx.speccy.machine.Pentagon1024;
import com.fpetrola.oozx.speccy.machine.Pentagon1024MemoryPeripheral;
import com.fpetrola.oozx.speccy.machine.Spec16;
import com.fpetrola.oozx.speccy.machine.Spec128;
import com.fpetrola.oozx.speccy.machine.Spec48;
import com.fpetrola.oozx.speccy.machine.SpecSe;
import com.fpetrola.oozx.speccy.machine.Tc2048;
import com.fpetrola.oozx.speccy.machine.Tc2068;
import com.fpetrola.oozx.speccy.machine.Chloe140Se;
import com.fpetrola.oozx.speccy.machine.Chloe280Se;
import com.fpetrola.oozx.speccy.machine.Chrome;
import com.fpetrola.oozx.speccy.machine.CzSpectrum;
import com.fpetrola.oozx.speccy.machine.Inves;
import com.fpetrola.oozx.speccy.machine.CzSpectrumPlus;
import com.fpetrola.oozx.speccy.machine.Scorpion;
import com.fpetrola.oozx.speccy.machine.Tk90x;
import com.fpetrola.oozx.speccy.machine.Tk95;
import com.fpetrola.oozx.speccy.machine.Ts2068;
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
      "Spectrum 16K", "Spectrum 48K", "Spectrum 128K", "Spectrum Plus 3", "Spectrum Plus 2", "Spectrum Plus 2A",
      "Sinclair Spectrum 48K (NTSC)", "Amstrad Spectrum +3e", "Pentagon", "Pentagon 512K", "Timex TC2048", "Timex TC2068", "Timex TS2068", "Spectrum SE", "Scorpion ZS 256",
      "Microdigital TK90X", "Microdigital TK95", "Czerweny CZ Spectrum", "Czerweny CZ Spectrum Plus", "Chloe 140SE", "Chloe 280SE", "Inves Spectrum+", "Chrome", "Pentagon 1024K");

  protected void configure() {
    Multibinder.newSetBinder(binder(), com.fpetrola.oozx.speccy.peripherals.Peripheral.class)
        .addBinding().to(Pentagon1024MemoryPeripheral.class);
    Multibinder.newSetBinder(binder(), com.fpetrola.oozx.speccy.peripherals.Peripheral.class)
        .addBinding().to(com.fpetrola.oozx.speccy.machine.ChloeUla2Peripheral.class);
    Multibinder.newSetBinder(binder(), com.fpetrola.oozx.speccy.peripherals.Peripheral.class)
        .addBinding().to(com.fpetrola.oozx.speccy.machine.InvesInterruptFault.class);
    Multibinder<Spectrum> models = Multibinder.newSetBinder(binder(), Spectrum.class);
    models.addBinding().to(Spec16.class);
    models.addBinding().to(Spec48.class);
    models.addBinding().to(Spec128.class);
    models.addBinding().to(SpecPlus3.class);
    models.addBinding().to(SpecPlus2.class);
    models.addBinding().to(SpecPlus2A.class);
    models.addBinding().to(Spec48Ntsc.class);
    models.addBinding().to(SpecPlus3E.class);
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
    models.addBinding().to(Chloe140Se.class);
    models.addBinding().to(Chloe280Se.class);
    models.addBinding().to(Inves.class);
    models.addBinding().to(Chrome.class);
    bind(Spectrum.class).annotatedWith(DefaultMachine.class).to(Spec48.class);
  }
}
