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

import com.fpetrola.oozx.speccy.Input;
import com.fpetrola.oozx.speccy.modules.Sound;
import com.fpetrola.oozx.speccy.machine.*;
import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.modules.*;
import com.fpetrola.oozx.speccy.modules.Keyboard;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.z80.Z80;
import com.fpetrola.oozx.speccy.peripherals.PeripheralBus;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.fpetrola.oozx.speccy.peripherals.Peripheral;

@Singleton
public class Speccy {
  public final SpectrumZ80Clock zxClock;
  public final EmulationSession session;
  public final Settings settings;
  public final Memory memory;
  public final Display display;
  public final Keyboard keyboard;
  public final PeripheralBus peripherals;
  public final Tape tape;
  public final Sound sound;
  public final Ula ula;
  public final EventManager eventManager;
  public final PeripheralBusDelegate contendedBus;
  public final Joystick joystick;
  public final Input input;
  public final Machine machine;
  private final java.util.Set<Peripheral> devices;
  public final Z80 z80;
  public final UiDisplay uiDisplay;
  public final Spec48 spec48;
  public final Spec128 spec128;
  public final SpecPlus3 specPlus3;
  public final SpecPlus2 specPlus2;
  public final SpecPlus2A specPlus2a;
  public final SpecPlus3E specPlus3e;
  public final Spec48Ntsc spec48Ntsc;
  public final Pentagon pentagon;
  private final java.util.Set<Spectrum> models;
  private final Spectrum defaultMachine;

  private final Module module;
  private final Timer timer;

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
    return Guice.createInjector(Modules.override(new EmulatorModule(clock)).with(overrides))
        .getInstance(Speccy.class);
  }

  @Inject
  public Speccy(SpectrumZ80Clock zxClock, EmulationSession session, Settings settings, Memory memory,
              Display display, Keyboard keyboard, PeripheralBus peripherals, Tape tape, Sound sound, Ula ula,
              EventManager eventManager, PeripheralBusDelegate contendedBus, Joystick joystick, Input input, Machine machine, Z80 z80, UiDisplay uiDisplay,
              Spec48 spec48, Spec128 spec128, SpecPlus3 specPlus3, SpecPlus2 specPlus2,
              SpecPlus2A specPlus2a, SpecPlus3E specPlus3e, Spec48Ntsc spec48Ntsc, Pentagon pentagon,
              java.util.Set<Spectrum> models, java.util.Set<Peripheral> devices,
              @DefaultMachine Spectrum defaultMachine,
              Module module, Timer timer) {
    this.zxClock = zxClock;
    this.session = session;
    this.settings = settings;
    this.memory = memory;
    this.display = display;
    this.keyboard = keyboard;
    this.peripherals = peripherals;
    this.tape = tape;
    this.sound = sound;
    this.ula = ula;
    this.eventManager = eventManager;
    this.contendedBus = contendedBus;
    this.joystick = joystick;
    this.input = input;
    this.machine = machine;
    this.devices = devices;
    this.z80 = z80;
    this.uiDisplay = uiDisplay;
    this.spec48 = spec48;
    this.spec128 = spec128;
    this.specPlus3 = specPlus3;
    this.specPlus2 = specPlus2;
    this.specPlus2a = specPlus2a;
    this.specPlus3e = specPlus3e;
    this.spec48Ntsc = spec48Ntsc;
    this.pentagon = pentagon;
    this.models = models;
    this.defaultMachine = defaultMachine;
    this.module = module;
    this.timer = timer;

    machine.addMachineChangeListeners(sound, display, timer, peripherals, ula, eventManager);
  }

  public boolean isAlive() {
    return session.isAlive();
  }

  /**
   * Brings the parts up.
   * <p>
   * A startup manager used to do this, resolving an order the modules declared between them. The
   * declarations are gone - every one of them said "this other thing has to have allocated its
   * collection first", which is what C says instead of building an object whole - so what is left
   * is a list of things to start, written here in the order it happens to be written in, and no
   * order is required of it.
   */
  public void init() {
    // The models this build has, told to the Machine here rather than in its constructor: it
    // cannot take them there, because a model reaches the Z80 and the Z80 reaches back, which is
    // the construction cycle this refactor started by breaking. The composition root can hold
    // both ends of a cycle; neither end can hold the other.
    models.forEach(machine::addMachine);
    machine.setDefaultMachine(defaultMachine);

    display.start();
    joystick.start();
    keyboard.start();

    // Whatever devices the build turned out to have. Each says which machines it fits, so they can
    // all be registered here without anything knowing what any of them is.
    devices.forEach(peripherals::register);

    spec48.start();
    timer.start();
    ula.start();
    z80.start();

    machine.selectDefault();
  }

  /** In the reverse of the order they were started, as the manager did it. */
  public void end() {
    timer.end();
    machine.end();
    keyboard.end();
    joystick.end();
    peripherals.end();
  }
}
