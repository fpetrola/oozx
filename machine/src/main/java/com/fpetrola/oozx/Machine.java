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

import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.machine.TimingsHandler;
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Machine implements ZxModule {
  private EventManager eventManager;
  private Memory memory;
  private Display display;
  private Ula ula;
  private Module module;
  private Settings settings;

  public List<Spectrum> getMachineTypes() {
    return machineTypes;
  }

  public List<Spectrum> machineTypes = new ArrayList<>(); // All available machines
  public Spectrum current; // The currently selected machine
  private Z80Clock z80Clock;
  private final Spectrum spectrum;
  private UiDisplay uiDisplay;
  private Timer timer;

  public Machine(EventManager eventManager, Memory memory, Display display, Ula ula, Z80Clock z80Clock, Spectrum spectrum, UiDisplay uiDisplay, Timer timer, Module module, Settings settings) {
    this.eventManager = eventManager;
    this.memory = memory;
    this.display = display;
    this.ula = ula;
    this.module = module;
    this.z80Clock = z80Clock;
    this.spectrum = spectrum;
    this.uiDisplay = uiDisplay;
    this.timer = timer;
    this.settings = settings;
  }

  public void addMachine(Spectrum spectrumMachine) {
    machineTypes.add(spectrumMachine);
    setConstTimings(spectrumMachine);
  }

  public int select(SpectrumMachine type) {
    int i;
    int error;

    for (i = 0; i < machineTypes.size(); i++) {
      if (machineTypes.get(i) == type) {
        error = selectMachine(machineTypes.get(i));

        if (error == 0) return 0;

        if (error != 0) {
          Ui.error(UiError.ERROR, "can't select 48K machine. Giving up.");
        } else {
          Ui.error(UiError.INFO, "selecting 48K machine");
          return 0;
        }

        return 0;
      }
    }

    Ui.error(UiError.ERROR, "machine type %d unknown", type.getClass().getName());
    return 1;
  }

  private int selectMachine(Spectrum machine) {
    int width, height;
    int capabilities;

    int spectrumFrameEvent;
    if (current != null && current.spectrumFrameEvent != -1)
      spectrumFrameEvent = current.spectrumFrameEvent;
    else
      spectrumFrameEvent = spectrum.spectrumFrameEvent;

    current = machine;
    current.init();

    settings.setString(settings.current.startMachine, machine.getClass().getSimpleName());

    z80Clock.setTStates(0);

    eventManager.reset();
//        EventManager.eventAdd(0, Timer.event);
    timer.addEvent();

    if (spectrumFrameEvent != 1)
      System.out.println("ehhh!!!1111");
    eventManager.eventAdd(machine.getTimings().tstatesPerFrame, spectrumFrameEvent);

    Sound.end();

    if (uiDisplay.end() != 0) return 1;

    capabilities = Libspectrum.machineCapabilities(machine);

    if ((capabilities & Libspectrum.MachineCapability.TIMEX_VIDEO) != 0) {
      width = display.SCREEN_WIDTH;
      height = 2 * display.SCREEN_HEIGHT;
    } else {
      width = display.ASPECT_WIDTH;
      height = display.SCREEN_HEIGHT;
    }

    if (uiDisplay.init(width, height) != 0) return 1;

    Sound.init(settings.current.soundDevice);

    machine.reset();
    reset(false);
//        if (error != 0) return error;

//        Ui.menuActivate(UiMenuItem.MEDIA_CARTRIDGE_DOCK_EJECT, 0);
//
//        Ui.widgetsReset();

    return 0;
  }

  public int reset(boolean hardReset) {
//        Pokemem.clear();
//
//        Sound.ayReset();
//
//        Tape.stop();

    memory.poolFree();
//    current = new FuseMachineInfo();
//    current.reset = () -> {
//    };
//    current.ram = new tStatesHolder.RamInfo();
//    current.ram.romcs = false;
//    current.memoryMap = () -> {
//    };
    setVariableTimings(current);

    memory.reset();

    current.reset();
//        if (error != 0) return error;

    module.reset(hardReset ? 1 : 0);

    current.memoryMap();
//        if (error != 0) return error;

    for (int i = 0; i < current.getTimings().tstatesPerFrame; i++) {
      ula.contention[i] = (byte) current.contendDelay(i);
      ula.contentionNoMreq[i] = (byte) current.contendDelayNoMreq(i);
    }

//        Ui.menuDiskUpdate();

    display.refreshAll();

    return 0;
  }

  private void setConstTimings(SpectrumMachine machine) {
    TimingsHandler.initMachineTimings(machine.getTimings(), machine);
  }

  private void setVariableTimings(SpectrumMachine machine) {
    machine.getLineTimes()[0] = TimingsHandler.topLeftPixel(machine.getBaseTiming()) -
        display.BORDER_HEIGHT * machine.getTimings().tstatesPerLine -
        4 * display.BORDER_WIDTH_COLS;

    if (settings.current.lateTimings) machine.getLineTimes()[0]++;

    for (int y = 1; y < display.SCREEN_HEIGHT + 1; y++) {
      machine.getLineTimes()[y] = machine.getLineTimes()[y - 1] + machine.getTimings().tstatesPerLine;
    }
  }

  @Override
  public int init(Object initContext) {
    return 0;
  }

  public void end() {
    for (SpectrumMachine machineType : machineTypes)
      machineType.shutdown();

    machineTypes = null;
  }
}