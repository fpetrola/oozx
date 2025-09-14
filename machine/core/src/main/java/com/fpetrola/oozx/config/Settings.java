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
 * Everything the file holds about the machine, and the two directions it moves in.
 * <p>
 * The machine does not know this exists. Its parts hold their own values - a {@link Speed}, the
 * sound's {@link Sound.Output} - and this is what puts the file's values into them after the
 * machine is built, and takes them back out before the file is written. What shipped is the
 * config.json on the classpath, which every file starts from; a field here is null only for a
 * key that neither names.
 * <p>
 * The sections keep the names and keys the file always had, so a file written before this is read
 * the same.
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

  /**
   * A device's share of this, bound by its own module: a device arrives in a jar the file never
   * heard of, so what it holds is filled and gathered by a part it brings along.
   */
  public interface Part {
    void into();

    void from();
  }

  /**
   * A device's properties the file keeps under a name: set from it after the device is built, read
   * back into it before it is written. What a module binds instead of writing that out.
   */
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

  /** The sections as the file has them, each the same instance the file will write. */
  public static Settings load(Configuration configuration) {
    return new Settings(configuration);
  }

  /**
   * The file's values into the machine's parts, and from then on the parts' values into the file
   * whenever it is saved. The parts are reached through the injector because they are what the
   * machine was built from; nothing here needs to know which module holds which.
   */
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

  /** The machine is gone: what it held no longer goes into the file. */
  public void letGo() {
    configuration.notBeforeSave(gather);
  }

  /** The machine's values back into the sections, which is what the file then writes. */
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
