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
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.oozx.fuse.peripherals.IPeriph;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.oozx.fuse.startup.*;

import java.util.List;
import java.util.function.Supplier;

public class Fuse {
  public Supplier<SpectrumMachine> spectrumMachineSupplier = () -> Machine.current;
  public SpectrumZ80Clock zxClock = new SpectrumZ80Clock();
  public Memory memory = new Memory(spectrumMachineSupplier, zxClock);
  private UiDisplay uiDisplay = new UiDisplay(zxClock);
  public Display display = new Display(memory, spectrumMachineSupplier, zxClock, memory, uiDisplay);
  public Keyboard keyboard = new Keyboard();
  public IPeriph periph = new Periph(spectrumMachineSupplier, zxClock);
  public Ula ula = new Ula(memory, display, spectrumMachineSupplier, keyboard, zxClock, periph);
  public EventManager eventManager = new EventManager(spectrumMachineSupplier, zxClock);
  public IPeriph ulaPeriph = new UlaPeriph(ula, zxClock, periph);
  public Joystick joystick = new Joystick(keyboard, ulaPeriph);
  public Input input = new Input(joystick, keyboard);
  private Sound sound = new Sound();
  private Timer timer = new Timer(eventManager, spectrumMachineSupplier, sound);
  public Z80 z80 = new Z80(eventManager, memory, display, ula, spectrumMachineSupplier, keyboard, zxClock, input, ulaPeriph, uiDisplay, timer, () -> getMachine());

  private Machine getMachine() {
    return machine;
  }

  public Spectrum spectrum = new Spectrum(memory, display, eventManager, z80, zxClock, memory, spectrumMachineSupplier, timer);
  public Machine machine = new Machine(eventManager, memory, display, ula, zxClock, spectrum, uiDisplay, timer);
  public MachinesPeriph machinesPeriph = new MachinesPeriph(ulaPeriph);
  public Spec48 spec48 = new Spec48(memory, display, machine, machinesPeriph, spectrum, ulaPeriph);
  public Spec128 spec128 = new Spec128(memory, display, machinesPeriph, spectrum, spec48, ulaPeriph, machine);
  public SpecPlus3 specPlus3 = new SpecPlus3(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph);
  public SpecPlus2 specPlus2 = new SpecPlus2(memory, display, machine, machinesPeriph, spectrum, spec48, spec128, ulaPeriph);
  public SpecPlus2A specPlus2a = new SpecPlus2A(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph, specPlus3);
  public SpecPlus3E specPlus3e = new SpecPlus3E(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph, specPlus3);
  public Spec48Ntsc spec48Ntsc = new Spec48Ntsc(memory, display, machine, machinesPeriph, spectrum, spec48, ulaPeriph);

  public void fuseInit() {
    StartupManager.init();

    List.of(
        new DisplayStartupModule(display),
        new EventManagerStartupModule(eventManager),
        new JoystickStartupModule(joystick),
        new KeyboardStartupModule(keyboard),
        new LibspectrumStartupModule(),
        new MachineStartupModule(machine, spec48, spec128, specPlus3, specPlus2, specPlus2a, specPlus3e, spec48Ntsc),
        new MachinesPeriphStartupModule(machine, spec128, specPlus3, periph),
        new MemoryStartupModule(memory, machine, spec128, specPlus3),
        new SpectrumStartupModule(spectrum),
        new TimerStartupModule(timer),
        new UlaStartupModule(ula),
        new Z80StartupModule(z80)
    ).forEach(StartupManager::register);

    StartupManager.run();
    machine.select(specPlus2);
  }

  public void fuseEnd() {
    StartupManager.runEnd();
    periph.end();
  }
}