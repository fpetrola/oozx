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

package com.fpetrola.oozx;

import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.DecodedMemoryBus;
import com.fpetrola.oozx.speccy.ports.Backplane;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.machine.*;
import com.fpetrola.oozx.speccy.modules.z80.PeripheralIO;
import com.fpetrola.oozx.speccy.ports.PortBus;
import com.fpetrola.oozx.speccy.modules.ports.MachinePortBus;
import com.fpetrola.z80.cpu.Core;
import com.fpetrola.z80.cpu.IO;
import com.fpetrola.z80.cpu.OopCore;
import com.fpetrola.z80.cpu.Z80Clock;
import com.google.inject.Inject;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;
import java.util.ServiceLoader;

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

    // PortBus is the raw bus, which the ULA has to receive: ContendedPortBus wraps the
    // ULA, so handing the ULA the contended one would close a loop. The backplane is what the
    // registry plugs handlers into and the bus decodes, so both see the one.
    bind(PortBus.class).to(MachinePortBus.class);
    bind(Backplane.class).in(Singleton.class);
    // The ROMs come from outside the machine: it asks by page, and this is what knows the files.
    bind(Configuration.class).toInstance(Configuration.shared());
    Configuration.section(binder(), com.fpetrola.oozx.config.RomFiles.class);
    // What the machine itself keeps in the file, said where the parts are bound rather than in a
    // list inside Settings: the same declaration a device makes about itself.
    com.fpetrola.oozx.config.Settings.mirror(binder(), "speed",
        com.fpetrola.oozx.speccy.modules.timer.Speed.class, "emulation", "fastLoading");
    com.fpetrola.oozx.config.Settings.mirror(binder(), "memory",
        com.fpetrola.oozx.speccy.modules.memory.Rom.Protection.class, "writableRoms");
    com.fpetrola.oozx.config.Settings.mirror(binder(), "machine",
        com.fpetrola.oozx.speccy.modules.machine.Machine.Unit.class, "lateTimings", "issue2");
    com.fpetrola.oozx.config.Settings.mirror(binder(), "sound",
        com.fpetrola.oozx.speccy.modules.sound.Sound.Output.class, "enabled", "device", "whileLoading");
    // How loud it is belongs to the sound itself rather than to what it is played on, and it kept
    // no memory of it between runs: a machine opened again came back at a hundred per cent.
    com.fpetrola.oozx.config.Settings.mirror(binder(), "sound.volume",
        com.fpetrola.oozx.speccy.modules.sound.Sound.class, "volume");
    bind(com.fpetrola.oozx.speccy.machine.Roms.class).to(com.fpetrola.oozx.config.RomFiles.class);
    // The bus as defined asks on every access; the one the machine runs on remembers.
    bind(MemoryBus.class).to(DecodedMemoryBus.class);

    // The configuration this emulator brings, a section per part: each one is handed to the part
    // it belongs to and to nothing else.

    // What the processor runs on: the one a machine starts on, and every one it could be asked
    // to run on instead. The OOP core is always both; anything faster arrives on the classpath.
    OptionalBinder.newOptionalBinder(binder(), Core.class).setDefault().to(OopCore.class);
    bind(Core.class).annotatedWith(com.fpetrola.oozx.speccy.modules.z80.Processors.StartsOn.class)
        .toProvider(TheOneChosen.class);
    // The mix plays into silence unless a module brings a card, the way a machine with no chip has no chip.
    // How the processor is wired to this machine. A test that counts its own T-states binds
    // another over it; nothing in the model knows there is another.
    OptionalBinder.newOptionalBinder(binder(), com.fpetrola.oozx.speccy.modules.z80.ProcessorWiring.class)
        .setDefault().to(com.fpetrola.oozx.speccy.modules.z80.ContendedWiring.class);

    OptionalBinder.newOptionalBinder(binder(), com.fpetrola.oozx.speccy.modules.sound.SoundCard.class)
        .setDefault().to(com.fpetrola.oozx.speccy.modules.sound.SilentSoundDevice.class);
    Multibinder.newSetBinder(binder(), Core.class).addBinding().to(OopCore.class);

    // The processor's ports. Bound to a type rather than built inside the Z80, so an RZX
    // recording can replace it to play back, or wrap it to record.
    bind(IO.class).to(PeripheralIO.class);

    // Every model the emulator can be, and separately which one it falls back to. The set says
    // nothing about order, so nothing depends on the order these are listed in.
    // Devices are found, not named: every Extension on the classpath is installed, and each
    // says what it brings. The empty set binder is what lets a build have no devices at all
    // rather than fail to resolve one.
    Multibinder.newSetBinder(binder(), Peripheral.class);
    Multibinder.newSetBinder(binder(), com.fpetrola.oozx.config.Settings.Part.class);
    com.fpetrola.oozx.plugins.Plugins.found(Extension.class).forEach(this::install);

    // The machines themselves arrive as extensions, bound into Set<Spectrum>; none is named here.

  }

  /**
   * The processor a machine starts on: the one the file names under {@code machine.processor}, or
   * whichever this build prefers when nothing names one or this build has no such thing.
   * <p>
   * A binding and not a field somebody sets before building a machine: what a machine starts on is
   * a setting, and a setting is read where everything else a machine is made of is read.
   */
  static class TheOneChosen implements com.google.inject.Provider<Core> {
    @Inject
    private java.util.Set<Core> available;
    @Inject
    private com.fpetrola.oozx.config.Configuration configuration;
    @Inject
    private Core preferred;

    public Core get() {
      Object named = configuration.valueOf("machine", "processor", String.class);
      return available.stream().filter(core -> core.name().equals(named)).findFirst().orElse(preferred);
    }
  }

  /**
   * A module that makes the machine run on this core instead of the one it would start on. Both
   * keys, so that it beats what the file names as well: a machine built to be read by the
   * generator has to be the one asked for, whatever the person chose for their own machines.
   */
  public static com.google.inject.Module core(Class<? extends Core> core) {
    return binder -> {
      OptionalBinder.newOptionalBinder(binder, Core.class).setBinding().to(core);
      binder.bind(Core.class)
          .annotatedWith(com.fpetrola.oozx.speccy.modules.z80.Processors.StartsOn.class).to(core);
    };
  }
}
