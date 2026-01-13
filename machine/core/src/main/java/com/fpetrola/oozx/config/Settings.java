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

package com.fpetrola.oozx.config;

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import java.util.Set;

/**
 * Copies config values into machine parts ({@link Speed}, {@link Sound.Output}, etc.) after the machine is built,
 * and back out before the file is written; the machine itself has no reference to this class.
 * A field is null only when neither the file nor the shipped config.json set it.
 */
public final class Settings {
  @Section("speed")
  public static class SpeedSection {
    public Integer emulation;
    public Boolean fastLoading;
  }

  @Section("memory")
  public static class MemorySection {
    public Boolean writableRoms;
  }

  @Section("machine")
  public static class MachineSection {
    public Boolean lateTimings;
    public Boolean issue2;
  }

  @Section("sound")
  public static class SoundSection {
    public Boolean enabled;
    public String device;
    public Boolean whileLoading;
  }

  /** Lets a device's own module contribute its settings sync, since Settings can't know about jars it never imports. */
  public interface Part {
    void into();

    void from();
  }

  /** Binds a {@link Part} that syncs the named properties of {@code device} with the config file, so a module needn't write that out itself. */
  public static void mirror(Binder binder, String name, Class<?> device, String... properties) {
    Provider<Configuration> configuration = binder.getProvider(Configuration.class);
    Provider<?> held = binder.getProvider(device);
    Multibinder.newSetBinder(binder, Part.class).addBinding().toInstance(new Part() {
      public void into() {
        configuration.get().fill(name, held.get(), properties);
      }

      public void from() {
        configuration.get().put(name, held.get(), properties);
      }
    });
  }

  private static final Key<Set<Part>> PARTS = Key.get(new TypeLiteral<Set<Part>>() {});

  private final Configuration configuration;
  private final SpeedSection speed;
  private final MemorySection memory;
  private final MachineSection machine;
  private final SoundSection sound;

  private Settings(Configuration configuration) {
    this.configuration = configuration;
    speed = configuration.of(SpeedSection.class);
    memory = configuration.of(MemorySection.class);
    machine = configuration.of(MachineSection.class);
    sound = configuration.of(SoundSection.class);
  }

  public static Settings load(Configuration configuration) {
    return new Settings(configuration);
  }

  /** Applies file values to the machine's parts (via the injector, so this needs no knowledge of which module holds which) and registers {@link #from} to run on every later save. */
  public void into(Injector injector) {
    Speed s = injector.getInstance(Speed.class);
    if (speed.emulation != null) s.emulation = speed.emulation;
    if (speed.fastLoading != null) s.fastLoading = speed.fastLoading;

    Rom.Protection protection = injector.getInstance(Rom.Protection.class);
    if (memory.writableRoms != null) protection.writableRoms = memory.writableRoms;

    Machine.Unit unit = injector.getInstance(Machine.Unit.class);
    if (machine.lateTimings != null) unit.lateTimings = machine.lateTimings;
    if (machine.issue2 != null) unit.issue2 = machine.issue2;

    Sound.Output output = injector.getInstance(Sound.Output.class);
    if (sound.enabled != null) output.enabled = sound.enabled;
    if (sound.device != null) output.device = sound.device;
    if (sound.whileLoading != null) output.whileLoading = sound.whileLoading;

    injector.getInstance(PARTS).forEach(Part::into);

    gather = () -> from(injector);
    configuration.beforeSave(gather);
  }

  private Runnable gather;

  public void letGo() {
    configuration.notBeforeSave(gather);
  }

  public void from(Injector injector) {
    Speed s = injector.getInstance(Speed.class);
    speed.emulation = s.emulation;
    speed.fastLoading = s.fastLoading;
    memory.writableRoms = injector.getInstance(Rom.Protection.class).writableRoms;
    Machine.Unit unit = injector.getInstance(Machine.Unit.class);
    machine.lateTimings = unit.lateTimings;
    machine.issue2 = unit.issue2;
    Sound.Output output = injector.getInstance(Sound.Output.class);
    sound.enabled = output.enabled;
    sound.device = output.device;
    sound.whileLoading = output.whileLoading;
    injector.getInstance(PARTS).forEach(Part::from);
  }


}
