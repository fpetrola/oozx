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
import com.fpetrola.oozx.fuse.machine.Spec128;
import com.fpetrola.oozx.fuse.machine.Spec48;
import com.fpetrola.oozx.fuse.machine.SpecPlus3;
import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.modules.Joystick;
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.oozx.fuse.modules.Keyboard;
import com.fpetrola.oozx.fuse.peripherals.Periph;
import com.fpetrola.oozx.fuse.startup.*;

import java.util.List;
import java.util.function.Supplier;

public class Fuse {
  public Supplier<SpectrumMachine> spectrumMachineSupplier = () -> Machine.current;
  public ZxClock zxClock = new DefaultZxClock();
  public Memory memory = new Memory(spectrumMachineSupplier, zxClock);
  private UiDisplay uiDisplay = new UiDisplay(zxClock);
  public Display display = new Display(memory, spectrumMachineSupplier, zxClock, memory, uiDisplay);
  public Keyboard keyboard = new Keyboard();
  public Ula ula = new Ula(memory, display, spectrumMachineSupplier, keyboard, zxClock);
  public EventManager eventManager = new EventManager(spectrumMachineSupplier, zxClock);
  public Periph periph = new Periph(eventManager, ula, spectrumMachineSupplier, zxClock);
  public Joystick joystick = new Joystick(keyboard, periph);
  public Input input = new Input(joystick, keyboard);
  public Z80 z80 = new Z80(eventManager, memory, display, ula, spectrumMachineSupplier, keyboard, zxClock, input, periph, uiDisplay);
  public Spectrum spectrum = new Spectrum(memory, display, eventManager, z80, zxClock, memory, spectrumMachineSupplier);
  public Machine machine = new Machine(eventManager, memory, display, ula, zxClock, spectrum, uiDisplay);
  public MachinesPeriph machinesPeriph = new MachinesPeriph(periph);
  public Spec48 spec48 = new Spec48(memory, display, machine, machinesPeriph, spectrum, periph);
  public Spec128 spec128 = new Spec128(memory, display, machinesPeriph, spectrum, spec48, periph);
  public SpecPlus3 specPlus3 = new SpecPlus3(memory, display, machine, machinesPeriph, spectrum, spec48, periph);

  public void fuseInit() {
    StartupManager.init();

    List.of(
        new DisplayStartupModule(display),
        new EventManagerStartupModule(eventManager),
        new JoystickStartupModule(joystick),
        new KeyboardStartupModule(keyboard),
        new LibspectrumStartupModule(),
        new MachineStartupModule(machine, spec48, spec128, specPlus3),
        new MachinesPeriphStartupModule(machine, spec128, specPlus3, periph),
        new MemoryStartupModule(memory, machine, spec128, specPlus3),
        new SpectrumStartupModule(spectrum),
        new UlaStartupModule(ula, periph),
        new Z80StartupModule(z80)
    ).forEach(StartupManager::register);

    StartupManager.run();
    machine.select(spec48);
  }

  public void fuseEnd() {
    StartupManager.runEnd();
    periph.end();
  }

}