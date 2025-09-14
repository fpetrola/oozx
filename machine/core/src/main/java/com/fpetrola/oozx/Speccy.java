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

import com.fpetrola.oozx.speccy.modules.z80.SpectrumZ80Clock;
import com.fpetrola.oozx.speccy.machine.Spectrum;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.fpetrola.oozx.speccy.modules.display.Picture;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.ula.Ula;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.machine.*;
import com.fpetrola.oozx.speccy.modules.keyboard.KeyMatrix;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.ports.MachinePortBus;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.config.Settings;
import com.fpetrola.oozx.speccy.modules.timer.Speed;
import com.google.inject.util.Modules;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;

@Singleton
public class Speccy {
  public final SpectrumZ80Clock zxClock;
  public final EmulationSession session;
  public final Speed speed;
  public final com.fpetrola.oozx.speccy.modules.z80.MachineLoop loop;
  public final com.fpetrola.oozx.speccy.modules.z80.Processors processors;
  /** The frontend driving this machine, when there is one: the app parks it here and reads it back. */
  public com.fpetrola.oozx.EmulatorControl control;
  /** This machine's configuration, for whoever has to reach a section nothing else hands out. */
  public final com.fpetrola.oozx.config.Configuration configuration;
  /** The file's hold on this machine's values, let go of when the machine ends; null for one built without the file. */
  private Settings settings;
  /** Where the ROMs come from, for whoever lets a person choose one: the machine itself only ever asks for bytes. */
  public final com.fpetrola.oozx.config.RomFiles roms;
  public final MemoryBus memory;
  public final SpectrumMemory banks;
  public final Display display;
  public final KeyMatrix keys;
  public final PeripheralRegistry peripheralRegistry;
  public final MachinePortBus ports;
  public final Sound sound;
  public final Ula ula;
  public final Scheduler scheduler;
  public final Machine machine;
  private final java.util.Set<Peripheral> devices;
  public final Cpu cpu;
  public final Picture picture;

  public final Timer timer;

  /**
   * Builds the object graph and hands back the assembled emulator.
   * <p>
   * This is the only place in the program that touches the container. Everything below it,
   * including this class, receives what it needs through its constructor.
   */
  public static Speccy create() {
    return create(new SpectrumZ80Clock());
  }

  public static Speccy create(SpectrumZ80Clock clock, com.google.inject.Module... overrides) {
    Injector injector = Guice.createInjector(Modules.override(new EmulatorModule(clock)).with(overrides));
    Speccy speccy = injector.getInstance(Speccy.class);
    // The file's values go into the machine here, from outside it: no part of the machine knows
    // there is a file, only that it has a speed, a set of ROMs, and so on.
    speccy.settings = Settings.load(injector.getInstance(Configuration.class));
    speccy.settings.into(injector);
    return speccy;
  }

  @Inject
  public Speccy(SpectrumZ80Clock zxClock, EmulationSession session, MemoryBus memory, SpectrumMemory banks,
                Display display, KeyMatrix keys, PeripheralRegistry peripheralRegistry, MachinePortBus ports, Sound sound, Ula ula,
                Scheduler scheduler, Machine machine, Cpu cpu, Picture picture, Speed speed, com.fpetrola.oozx.config.Configuration configuration, com.fpetrola.oozx.config.RomFiles roms,
                java.util.Set<Peripheral> devices,
                Timer timer, com.fpetrola.oozx.speccy.modules.z80.Processors processors, com.fpetrola.oozx.speccy.modules.z80.MachineLoop loop) {
    this.zxClock = zxClock;
    this.session = session;
    this.speed = speed;
    this.configuration = configuration;
    this.roms = roms;
    this.memory = memory;
    this.banks = banks;
    this.display = display;
    this.keys = keys;
    this.peripheralRegistry = peripheralRegistry;
    this.ports = ports;
    this.sound = sound;
    this.ula = ula;
    this.scheduler = scheduler;
    this.machine = machine;
    this.devices = devices;
    this.cpu = cpu;
    this.picture = picture;
    this.timer = timer;
    this.processors = processors;
    this.loop = loop;

    timer.onSpeed(managed -> { if (control != null) control.notifySpeed((float) managed); });
  }

  public boolean isAlive() {
    return session.isAlive();
  }

  /**
   * Puts together the one thing that cannot be given to a constructor, and switches the machine on.
   * <p>
   * A startup manager used to do this, resolving an order the parts declared between them, and
   * then a list of start() calls in no particular order stood in for it. Neither is here: a part
   * that has what it needs to work is built with it, the Machine included, models and all.
   * <p>
   * The devices are what is left, and they cannot be handed to the registry that holds them,
   * because some of them ask for the Machine and the Machine asks for that registry. Whatever
   * this build turned out to have arrives here instead. Each says which machines it fits, so
   * they can all be registered without anything knowing what any of them is.
   */
  public void init() {
    devices.forEach(peripheralRegistry::register);
    machine.selectDefault();
  }

  /** What has to be let go of: everything else is dropped with this. */
  public void end() {
    timer.end();
    peripheralRegistry.end();
    if (settings != null) settings.letGo();
  }
}
