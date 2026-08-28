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

package com.fpetrola.oozx;

import com.fpetrola.oozx.speccy.machine.*;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PeripheralIO;
import com.fpetrola.oozx.speccy.modules.z80.Z80;
import com.fpetrola.oozx.speccy.peripherals.IPeriph;
import com.fpetrola.oozx.speccy.peripherals.Periph;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * The bindings a Spectrum needs beyond what the constructors already say.
 * <p>
 * Almost the whole graph resolves on its own from the @Inject constructors: this only has to
 * answer the questions those cannot, which are the four places where a dependency is asked for
 * by an interface that has more than one implementation, or none that Guice could guess.
 */
public class EmulatorModule extends AbstractModule {

  private final SpectrumZ80Clock clock;

  public EmulatorModule(SpectrumZ80Clock clock) {
    this.clock = clock;
  }

  @Override
  protected void configure() {
    // The clock comes from outside, so that a caller can supply an instrumented one.
    bind(SpectrumZ80Clock.class).toInstance(clock);
    bind(Z80Clock.class).to(SpectrumZ80Clock.class);

    // Memory is the RAM holder; the two names are the same object seen from two sides.
    bind(RAMHolder.class).to(Memory.class);

    // The two peripheral buses. IPeriph is the raw one; PeriphDelegate is the UlaPeriph that
    // decorates it, and the ULA itself must keep receiving the raw one — UlaPeriph wraps the
    // ULA, so handing the ULA the delegate would close a loop.
    bind(IPeriph.class).to(Periph.class);
    bind(PeriphDelegate.class).to(UlaPeriph.class);

    bind(Cpu.class).to(Z80.class);

    // The processor's ports. Bound to a type rather than built inside the Z80, so an RZX
    // recording can replace it to play back, or wrap it to record.
    bind(IO.class).to(PeripheralIO.class);

    // Every model the emulator can be, and separately which one it falls back to. The set says
    // nothing about order, so nothing depends on the order these are listed in.
    Multibinder<Spectrum> models = Multibinder.newSetBinder(binder(), Spectrum.class);
    models.addBinding().to(Spec48.class);
    models.addBinding().to(Spec128.class);
    models.addBinding().to(SpecPlus3.class);
    models.addBinding().to(SpecPlus2.class);
    models.addBinding().to(SpecPlus2A.class);
    models.addBinding().to(SpecPlus3E.class);
    models.addBinding().to(Spec48Ntsc.class);

    bind(Spectrum.class).annotatedWith(DefaultMachine.class).to(Spec48.class);
  }
}
