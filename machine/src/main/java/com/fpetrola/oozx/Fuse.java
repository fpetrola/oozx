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
import com.fpetrola.oozx.fuse.modules.Joystick;
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.oozx.fuse.modules.Keyboard;
import com.fpetrola.oozx.fuse.peripherals.Periph;

import java.util.List;
import java.util.function.Supplier;

public class Fuse {
  public static Supplier<FuseMachineInfo> fuseMachineInfoSupplier = () -> Machine.current;
  public static TStatesHolder tStatesHolder = new TStatesHolder() {
    private long tstates;

    public long getTstates() {
      return tstates;
    }

    public void setTstates(long tstates) {
      this.tstates = tstates;
    }
  };
  private static RAMHolder ramHolder = new RAMHolder() {
    // RAM array: 65 pages of 16KB each (from SPECTRUM_RAM_PAGES)
    private static byte[][] RAM = new byte[memory.SPECTRUM_RAM_PAGES][0x4000];

    public byte[][] getRAM() {
      return RAM;
    }
  };
  public static Memory memory = new Memory(fuseMachineInfoSupplier, tStatesHolder);
  private static UiDisplay uiDisplay = new UiDisplay(tStatesHolder);
  public static Display display = new Display(memory, fuseMachineInfoSupplier, tStatesHolder, ramHolder, uiDisplay);
  public static Keyboard keyboard = new Keyboard();
  public static Ula ula = new Ula(memory, display, fuseMachineInfoSupplier, keyboard, tStatesHolder);
  public static EventManager eventManager = new EventManager(fuseMachineInfoSupplier, tStatesHolder);
  public static Periph periph = new Periph(eventManager, ula, fuseMachineInfoSupplier, tStatesHolder);
  public static Joystick joystick = new Joystick(keyboard, periph);
  public static Input input = new Input(joystick, keyboard);
  public static Z80 z80 = new Z80(eventManager, memory, display, ula, fuseMachineInfoSupplier, keyboard, tStatesHolder, input, periph, uiDisplay);
  public static Spectrum spectrum = new Spectrum(memory, display, eventManager, z80, tStatesHolder, ramHolder, fuseMachineInfoSupplier);
  public static Machine machine = new Machine(eventManager, memory, display, ula, tStatesHolder, spectrum, uiDisplay);
  public static MachinesPeriph machinesPeriph = new MachinesPeriph(periph);
  public static Spec48 spec48 = new Spec48(memory, display, machine, machinesPeriph, spectrum, periph);
  public static Spec128 spec128 = new Spec128(memory, display, fuseMachineInfoSupplier, machinesPeriph, spectrum, spec48, periph);
  public static SpecPlus3 specPlus3 = new SpecPlus3(memory, display, machine, machinesPeriph, spectrum, spec48, periph);

  private static int runStartupManager() {
    StartupManager.init();

    List.of(
        new DisplayStartupModule(display),
        new EventManagerStartupModule(eventManager),
        new JoystickStartupModule(joystick),
        new KeyboardStartupModule(keyboard),
        new LibspectrumStartupModule(),
        new MachineStartupModule(machine, spec48, spec128, specPlus3),
        new MachinesPeriphStartupModule(machine, spec128, specPlus3, periph),
        new MemoryStartupModule(memory, ramHolder, machine, spec128, specPlus3),
        new SpectrumStartupModule(spectrum),
        new UlaStartupModule(ula, periph),
        new Z80StartupModule(z80)
    ).forEach(StartupManager::register);

    return StartupManager.run();
  }

  public static int fuseInit() {
    runStartupManager();
    machine.selectId("48");
    return 0;
  }

  public static int fuseEnd() {
    StartupManager.runEnd();
    periph.end();
    return 0;
  }
}