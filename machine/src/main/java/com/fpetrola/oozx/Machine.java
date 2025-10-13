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

import com.fpetrola.oozx.fuse.machine.SpectrumMachine;
import com.fpetrola.oozx.fuse.machine.TimingsHandler;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.modules.Ula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Machine {
  private EventManager eventManager;
  private Memory memory;
  private Display display;
  private Ula ula;

  public List<SpectrumMachine> machineTypes = new ArrayList<>(); // All available machines
  public static SpectrumMachine current; // The currently selected machine
  private TStatesHolder tStatesHolder;
  private final Spectrum spectrum;
  private UiDisplay uiDisplay;

  public Machine(EventManager eventManager, Memory memory, Display display, Ula ula, TStatesHolder tStatesHolder, Spectrum spectrum, UiDisplay uiDisplay) {
    this.eventManager = eventManager;
    this.memory = memory;
    this.display = display;
    this.ula = ula;
    this.tStatesHolder = tStatesHolder;
    this.spectrum = spectrum;
    this.uiDisplay = uiDisplay;
  }

  public void addMachine(SpectrumMachine spectrumMachine) {
    machineTypes.add(spectrumMachine);
    setConstTimings(spectrumMachine);
  }

  public int select(SpectrumMachine type) {
    int i;
    int error;

    Rzx.stopRecording();
    Rzx.stopPlayback(true);

    Movie.stop();

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

  private int selectMachine(SpectrumMachine machine) {
    int width, height;
    int capabilities;

    current = machine;

    Settings.setString(Settings.current.startMachine, machine.getClass().getSimpleName());

    tStatesHolder.setTstates(0);

    eventManager.reset();
//        EventManager.eventAdd(0, Timer.event);
    eventManager.eventAdd(machine.getTimings().tstatesPerFrame, spectrum.spectrumFrameEvent);

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

    Sound.init(Settings.current.soundDevice);

    machine.reset();
    reset(false);
//        if (error != 0) return error;

//        Ui.menuActivate(UiMenuItem.MEDIA_CARTRIDGE_DOCK_EJECT, 0);
//
//        Ui.widgetsReset();

    return 0;
  }

  public int loadRomBankFromBuffer(MemoryPage[] bankMap, int pageNum, byte[] buffer, int length, boolean custom) {
    int offset = 0;
    byte[] data = new byte[length];
    System.arraycopy(buffer, 0, data, 0, length);

    for (MemoryPage page : Arrays.asList(bankMap).subList(pageNum * memory.PAGES_IN_16K, pageNum * memory.PAGES_IN_16K + length / memory.PAGE_SIZE)) {
      page.offset = offset;
      page.pageNum = pageNum;
      page.setPage(Arrays.copyOfRange(data, offset, offset + memory.PAGE_SIZE));
      page.writable = false;
      page.saveToSnapshot = custom;
      offset += memory.PAGE_SIZE;
    }

    return 0;
  }

  private int loadRomBankFromFile(MemoryPage[] bankMap, int pageNum, String filename, int expectedLength, boolean custom) {
    Utils.File rom = new Utils.File();
    int error = Utils.readAuxiliaryFile(filename, rom, Utils.AuxiliaryType.ROM);
    if (error == -1) {
      Ui.error(UiError.ERROR, "couldn't find ROM '%s'", filename);
      return 1;
    }
    if (error != 0) return error;
    rom.buffer = new byte[0x4000];
    rom.length = rom.buffer.length;

    if (rom.length != expectedLength) {
      Ui.error(UiError.ERROR, "ROM '%s' is %d bytes long; expected %d bytes", filename, rom.length, expectedLength);
      Utils.closeFile(rom);
      return 1;
    }

    error = loadRomBankFromBuffer(bankMap, pageNum, rom.buffer, rom.length, custom);

    Utils.closeFile(rom);

    return error;
  }

  public int loadRomBank(MemoryPage[] bankMap, int pageNum, String filename, String fallback, int expectedLength) {
    boolean custom = fallback != null && !filename.equals(fallback);
    int retval = loadRomBankFromFile(bankMap, pageNum, filename, expectedLength, custom);
    if (retval != 0 && fallback != null && custom) {
      retval = loadRomBankFromFile(bankMap, pageNum, fallback, expectedLength, false);
    }
    return retval;
  }

  public int loadRom(int pageNum, String filename, String fallback, int expectedLength) {
    return loadRomBank(memory.mapRom, pageNum, filename, fallback, expectedLength);
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

    Module.reset(hardReset ? 1 : 0);

    current.memoryMap();
//        if (error != 0) return error;

    for (int i = 0; i < (int) current.getTimings().tstatesPerFrame; i++) {
      ula.contention[i] = (byte) current.getRamInfo().contendDelay(i);
      ula.contentionNoMreq[i] = (byte) current.getRamInfo().contendDelayNoMreq(i);
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

    if (Settings.current.lateTimings) machine.getLineTimes()[0]++;

    for (int y = 1; y < display.SCREEN_HEIGHT + 1; y++) {
      machine.getLineTimes()[y] = machine.getLineTimes()[y - 1] + machine.getTimings().tstatesPerLine;
    }
  }

  public void end() {
    for (int i = 0; i < machineTypes.size(); i++) {
      machineTypes.get(i).shutdown();
    }

    machineTypes = null;
  }
}