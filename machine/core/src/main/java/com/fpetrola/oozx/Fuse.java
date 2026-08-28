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

import com.fpetrola.oozx.fuse.Input;
import com.fpetrola.oozx.fuse.Movie;
import com.fpetrola.oozx.fuse.Sound;
import com.fpetrola.oozx.fuse.machine.*;
import com.fpetrola.oozx.fuse.modules.Joystick;
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.oozx.fuse.modules.Keyboard;
import com.fpetrola.oozx.fuse.modules.tape.Tape;
import com.fpetrola.oozx.fuse.modules.tape.TapeSettingsType;
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.oozx.fuse.startup.*;
import com.google.inject.Guice;
import com.google.inject.Injector;

import java.util.List;

public class Fuse {
  public final Sound sound;
  public SpectrumZ80Clock zxClock;
  public Settings settings;
  public final EmulationSession session = new EmulationSession();
  private final Module module;
  public Memory memory;
  public Display display;
  public Keyboard keyboard;
  public IPeriph periph;
  public Tape tape;
  public Ula ula;
  public EventManager eventManager;
  public PeriphDelegate ulaPeriph;
  public Joystick joystick;
  public Input input;
  private final Timer timer;
  public Z80 z80;
  private final StartupManager startupManager;
  private final Injector injector;
  public Machine machine;
  public MachinesPeriph machinesPeriph;
  public Spec48 spec48;
  public Spec128 spec128;
  public SpecPlus3 specPlus3;
  public SpecPlus2 specPlus2;
  public SpecPlus2A specPlus2a;
  public SpecPlus3E specPlus3e;
  public Spec48Ntsc spec48Ntsc;
  public UiDisplay uiDisplay;

  public Fuse() {
    this(new SpectrumZ80Clock());
  }

  public Fuse(SpectrumZ80Clock spectrumZ80Clock) {
    injector = Guice.createInjector(new EmulatorModule(spectrumZ80Clock));

    zxClock = spectrumZ80Clock;
    settings = injector.getInstance(Settings.class);
    startupManager = injector.getInstance(StartupManager.class);
    module = injector.getInstance(Module.class);
    memory = injector.getInstance(Memory.class);
    uiDisplay = injector.getInstance(UiDisplay.class);
    display = injector.getInstance(Display.class);
    keyboard = injector.getInstance(Keyboard.class);
    periph = injector.getInstance(IPeriph.class);
    tape = injector.getInstance(Tape.class);
    sound = injector.getInstance(Sound.class);
    ula = injector.getInstance(Ula.class);
    eventManager = injector.getInstance(EventManager.class);
    ulaPeriph = injector.getInstance(PeriphDelegate.class);
    machinesPeriph = injector.getInstance(MachinesPeriph.class);
    joystick = injector.getInstance(Joystick.class);
    input = injector.getInstance(Input.class);
    timer = injector.getInstance(Timer.class);
    machine = injector.getInstance(Machine.class);
    z80 = injector.getInstance(Z80.class);
    spec48 = injector.getInstance(Spec48.class);

    machine.addMachineChangeListeners(sound, display, timer, periph, ula, eventManager);

    spec128 = injector.getInstance(Spec128.class);
    specPlus3 = injector.getInstance(SpecPlus3.class);
    specPlus2 = injector.getInstance(SpecPlus2.class);
    specPlus2a = injector.getInstance(SpecPlus2A.class);
    specPlus3e = injector.getInstance(SpecPlus3E.class);
    spec48Ntsc = injector.getInstance(Spec48Ntsc.class);
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
        new MachineStartupModule(machine, spec48, spec128, specPlus3, specPlus2, specPlus2a, specPlus3e, spec48Ntsc),
        new MachinesPeriphStartupModule(machine, spec128, specPlus3, periph),
        new MemoryStartupModule(memory, machine, spec128, specPlus3, module),
        new SpectrumStartupModule(spec48),
        new TimerStartupModule(timer),
        new UlaStartupModule(ula),
        new Z80StartupModule(z80)
    ).forEach(startupManager::register);

    startupManager.run();
    machine.select(spec48);
  }

  public void end() {
    startupManager.runEnd();
    periph.end();
  }
}