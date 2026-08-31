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
import com.fpetrola.oozx.speccy.Movie;
import com.fpetrola.oozx.speccy.Sound;
import com.fpetrola.oozx.speccy.machine.*;
import com.fpetrola.oozx.speccy.modules.Joystick;
import com.fpetrola.oozx.speccy.modules.*;
import com.fpetrola.oozx.speccy.modules.Keyboard;
import com.fpetrola.oozx.speccy.modules.tape.Tape;
import com.fpetrola.oozx.speccy.modules.tape.TapeSettingsType;
import com.fpetrola.oozx.speccy.modules.z80.Z80;
import com.fpetrola.oozx.speccy.peripherals.IPeriph;
import com.fpetrola.oozx.speccy.peripherals.Periph;
import com.fpetrola.oozx.speccy.startup.*;
import com.google.inject.Guice;
import com.google.inject.util.Modules;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class Speccy {
  public final SpectrumZ80Clock zxClock;
  public final EmulationSession session;
  public final Settings settings;
  public final Memory memory;
  public final Display display;
  public final Keyboard keyboard;
  public final IPeriph periph;
  public final Tape tape;
  public final Sound sound;
  public final Ula ula;
  public final EventManager eventManager;
  public final PeriphDelegate ulaPeriph;
  public final MachinesPeriph machinesPeriph;
  public final Joystick joystick;
  public final Input input;
  public final Machine machine;
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

  private final Module module;
  private final Timer timer;
  private final StartupManager startupManager;
  private final MachineStartupModule machineStartupModule;

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
              Display display, Keyboard keyboard, IPeriph periph, Tape tape, Sound sound, Ula ula,
              EventManager eventManager, PeriphDelegate ulaPeriph, MachinesPeriph machinesPeriph,
              Joystick joystick, Input input, Machine machine, Z80 z80, UiDisplay uiDisplay,
              Spec48 spec48, Spec128 spec128, SpecPlus3 specPlus3, SpecPlus2 specPlus2,
              SpecPlus2A specPlus2a, SpecPlus3E specPlus3e, Spec48Ntsc spec48Ntsc, Pentagon pentagon,
              Module module, Timer timer, StartupManager startupManager,
              MachineStartupModule machineStartupModule) {
    this.zxClock = zxClock;
    this.session = session;
    this.settings = settings;
    this.memory = memory;
    this.display = display;
    this.keyboard = keyboard;
    this.periph = periph;
    this.tape = tape;
    this.sound = sound;
    this.ula = ula;
    this.eventManager = eventManager;
    this.ulaPeriph = ulaPeriph;
    this.machinesPeriph = machinesPeriph;
    this.joystick = joystick;
    this.input = input;
    this.machine = machine;
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
    this.module = module;
    this.timer = timer;
    this.startupManager = startupManager;
    this.machineStartupModule = machineStartupModule;

    machine.addMachineChangeListeners(sound, display, timer, periph, ula, eventManager);
  }

  public boolean isAlive() {
    return session.isAlive();
  }

  public void init() {
    startupManager.init();

    List.of(
        new DisplayStartupModule(display),
        new EventManagerStartupModule(eventManager),
        new JoystickStartupModule(joystick),
        new KeyboardStartupModule(keyboard),
        new LibspectrumStartupModule(),
        machineStartupModule,
        new MachinesPeriphStartupModule(machine, spec128, specPlus3, periph, sound, zxClock),
        new MemoryStartupModule(memory, machine, spec128, specPlus3, module),
        new SpectrumStartupModule(spec48),
        new TimerStartupModule(timer),
        new UlaStartupModule(ula),
        new Z80StartupModule(z80)
    ).forEach(startupManager::register);

    startupManager.run();
    machine.selectDefault();
  }

  public void end() {
    startupManager.runEnd();
    periph.end();
  }
}
