/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

import java.util.List;
import java.util.function.Supplier;

public class Fuse {
  public Supplier<SpectrumMachine> spectrumMachineSupplier;
  public SpectrumZ80Clock zxClock;
  public Settings settings;
  public boolean alive;
  private Module module;
  public Memory memory;
  private UiDisplay uiDisplay;
  public Display display;
  public Keyboard keyboard;
  public IPeriph periph;
  public Tape tape;
  public Ula ula;
  public EventManager eventManager;
  public IPeriph ulaPeriph;
  public Joystick joystick;
  public Input input;
  private Sound sound;
  private Timer timer;
  public Z80 z80;
  private StartupManager startupManager;

  public Spectrum spectrum;

  public Machine machine;
  public MachinesPeriph machinesPeriph;
  public Spec48 spec48;
  public Spec128 spec128;
  private Fdd fdd;
  private UPDFdc uPDFdc;
  public SpecPlus3 specPlus3;
  public SpecPlus2 specPlus2;
  public SpecPlus2A specPlus2a;
  public SpecPlus3E specPlus3e;
  public Spec48Ntsc spec48Ntsc;

  public Fuse() {
    this(new SpectrumZ80Clock());
  }

  public Fuse(SpectrumZ80Clock spectrumZ80Clock) {
    zxClock = spectrumZ80Clock;
    spectrumMachineSupplier = () -> getMachine().current;
    settings = new Settings();
    startupManager = new StartupManager();
    alive = true;
    module = new Module();
    memory = new Memory(spectrumMachineSupplier, zxClock, module, settings);
    uiDisplay = new UiDisplay();
    display = new Display(memory, spectrumMachineSupplier, zxClock, memory, uiDisplay);
    keyboard = new Keyboard();
    periph = new Periph(spectrumMachineSupplier, zxClock, settings);
    tape = new Tape(new TapeSettingsType(), zxClock);
    ula = new Ula(memory, display, spectrumMachineSupplier, keyboard, zxClock, periph, module, settings, tape);
    eventManager = new EventManager(spectrumMachineSupplier, zxClock);
    ulaPeriph = new UlaPeriph(ula, zxClock, periph);
    joystick = new Joystick(keyboard, ulaPeriph, module, settings);
    input = new Input(joystick, keyboard, settings);
    sound = new Sound();
    timer = new Timer(eventManager, spectrumMachineSupplier, sound, settings, tape);
    z80 = new Z80(eventManager, memory, display, ula, spectrumMachineSupplier, keyboard, zxClock, input, ulaPeriph, uiDisplay, timer, () -> getMachine(), module, this, settings, tape);
    spectrum = new Spectrum(memory, display, eventManager, z80, zxClock, memory, spectrumMachineSupplier, timer, module);
    machine = new Machine(eventManager, memory, display, ula, zxClock, spectrum, uiDisplay, timer, module, settings);
    machinesPeriph = new MachinesPeriph(ulaPeriph);
    spec48 = new Spec48(memory, display, machine, machinesPeriph, spectrum, ulaPeriph, settings);
    spec128 = new Spec128(memory, display, machinesPeriph, spectrum, spec48, ulaPeriph, machine, settings);
    fdd = new Fdd(settings);
    uPDFdc = new UPDFdc(settings);
    specPlus3 = new SpecPlus3(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph, settings, fdd, uPDFdc);
    specPlus2 = new SpecPlus2(memory, display, machine, machinesPeriph, spectrum, spec48, spec128, ulaPeriph, settings);
    specPlus2a = new SpecPlus2A(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph, specPlus3, settings);
    specPlus3e = new SpecPlus3E(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph, specPlus3, settings);
    spec48Ntsc = new Spec48Ntsc(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph, settings);
  }

  private Machine getMachine() {
    return machine;
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
        new SpectrumStartupModule(spectrum),
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