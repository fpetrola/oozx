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

import java.util.*;
import java.util.function.Function;

// Assuming ported dependencies:
// - Libspectrum (with Machine, MachineCapabilities, Timings, Creator)
// - Display (with SCREEN_HEIGHT, BORDER_HEIGHT, etc.)
// - Peripherals (Ay, Covox, Specdrum)
// - Spectrum (with Raminfo, UnattachedPortFn, etc.)
// Use long for libspectrum_dword, int for libspectrum_byte/word
// Functional interface for unattached_port_fn

public class Machine {
  private static EventManager eventManager= Fuse.eventManager;

  public static List<FuseMachineInfo> machineTypes = new ArrayList<>(); // All available machines

  public static FuseMachineInfo current; // The currently selected machine
  private static Display display= Fuse.display;

  //  private static void reg1() {
//    StartupManagerModule[] dependencies = {
//        StartupManagerModule.MEMORY,
//        StartupManagerModule.SETUID
//    };
//    StartupManager.register(StartupManagerModule.MACHINE, dependencies,
//        Machine::initMachines, null, Machine::end);
//  }

  static int initMachines(Object context) {
    int error;

//        error = addMachine(Spec16::init);
//        if (error != 0) return error;
    error = addMachine(Spec48::init);
    if (error != 0) return error;
//        error = addMachine(Spec48Ntsc::init);
//        if (error != 0) return error;
    error = addMachine(Spec128::init);
    if (error != 0) return error;
//        error = addMachine(SpecPlus2::init);
//        if (error != 0) return error;
//        error = addMachine(SpecPlus2a::init);
//        if (error != 0) return error;
        error = addMachine(SpecPlus3::init);
        if (error != 0) return error;
//        error = addMachine(SpecPlus3e::init);
//        if (error != 0) return error;
//        error = addMachine(Tc2048::init);
//        if (error != 0) return error;
//        error = addMachine(Tc2068::init);
//        if (error != 0) return error;
//        error = addMachine(Ts2068::init);
//        if (error != 0) return error;
//        error = addMachine(Pentagon::init);
//        if (error != 0) return error;
//        error = addMachine(Pentagon512::init);
//        if (error != 0) return error;
//        error = addMachine(Pentagon1024::init);
//        if (error != 0) return error;
//        error = addMachine(Scorpion::init);
//        if (error != 0) return error;
//        error = addMachine(SpecSe::init);
//        if (error != 0) return error;

    return 0;
  }

  private static int addMachine(Function<FuseMachineInfo, Integer> initFunction) {
    FuseMachineInfo machine = new FuseMachineInfo();

    int error = initFunction.apply(machine);
    if (error != 0) return error;

    machineTypes.add(machine);

    setConstTimings(machine);
//    machine.timings.tstatesPerFrame = 69888;
//    machine.timings.tstatesPerLine= 224;

    machine.capabilities = Libspectrum.machineCapabilities(machine.machine);

    return 0;
  }

  public static int select(int type) {
    int i;
    int error;

    Rzx.stopRecording();
    Rzx.stopPlayback(true);

    Movie.stop();

    for (i = 0; i < machineTypes.size(); i++) {
      if (machineTypes.get(i).machine.ordinal() == type) {
        int location = i;
        error = selectMachine(machineTypes.get(i));

        if (error == 0) return 0;

        if (type != Libspectrum.Machine._48K.ordinal()) {
          error = select(Libspectrum.Machine._48K.ordinal());
        }

        if (error != 0) {
          Ui.error(UiError.ERROR, "can't select 48K machine. Giving up.");
          Fuse.abort();
        } else {
          Ui.error(UiError.INFO, "selecting 48K machine");
          return 0;
        }

        return 0;
      }
    }

    Ui.error(UiError.ERROR, "machine type %d unknown", type);
    return 1;
  }

  public static int selectId(String id) {
    int i;
    int error;

    for (i = 0; i < machineTypes.size(); i++) {
      if (machineTypes.get(i).id.equals(id)) {
        error = selectMachine(machineTypes.get(i));
        if (error != 0) return error;
        return 0;
      }
    }

    Ui.error(UiError.ERROR, "Machine id '%s' unknown", id);
    return 1;
  }

  public static String getId(int type) {
    for (int i = 0; i < machineTypes.size(); i++) {
      if (machineTypes.get(i).machine.ordinal() == type) return machineTypes.get(i).id;
    }
    return null;
  }

  private static int selectMachine(FuseMachineInfo machine) {
    int width, height;
    int capabilities;

    current = machine;

    Settings.setString(Settings.current.startMachine, machine.id);

    Spectrum.tstates = 0;

    eventManager.reset();
//        EventManager.eventAdd(0, Timer.event);
    eventManager.eventAdd(machine.timings.tstatesPerFrame, Spectrum.spectrumFrameEvent);

    Sound.end();

    if (UiDisplay.end() != 0) return 1;

    capabilities = Libspectrum.machineCapabilities(machine.machine);

    if ((capabilities & Libspectrum.MachineCapability.TIMEX_VIDEO) != 0) {
      width = display.SCREEN_WIDTH;
      height = 2 * display.SCREEN_HEIGHT;
    } else {
      width = display.ASPECT_WIDTH;
      height = display.SCREEN_HEIGHT;
    }

    if (UiDisplay.init(width, height) != 0) return 1;

    Sound.init(Settings.current.soundDevice);

    machine.reset.run();
    Machine.reset(false);
//        if (error != 0) return error;

//        Ui.menuActivate(UiMenuItem.MEDIA_CARTRIDGE_DOCK_EJECT, 0);
//
//        Ui.widgetsReset();

    return 0;
  }

  public static int loadRomBankFromBuffer(MemoryPage[] bankMap, int pageNum, byte[] buffer, int length, boolean custom) {
    int offset = 0;
    byte[] data = new byte[length];
    System.arraycopy(buffer, 0, data, 0, length);

    for (MemoryPage page : Arrays.asList(bankMap).subList(pageNum * Memory.PAGES_IN_16K, pageNum * Memory.PAGES_IN_16K + length / Memory.PAGE_SIZE)) {
      page.offset = offset;
      page.pageNum = pageNum;
      page.setPage(Arrays.copyOfRange(data, offset, offset + Memory.PAGE_SIZE));
      page.writable = false;
      page.saveToSnapshot = custom;
      offset += Memory.PAGE_SIZE;
    }

    return 0;
  }

  private static int loadRomBankFromFile(MemoryPage[] bankMap, int pageNum, String filename, int expectedLength, boolean custom) {
    Utils.File rom = new Utils.File();
    int error = Utils.readAuxiliaryFile(filename, rom, Utils.AuxiliaryType.ROM);
    if (error == -1) {
      Ui.error(UiError.ERROR, "couldn't find ROM '%s'", filename);
      return 1;
    }
    if (error != 0) return error;
    rom.buffer= new byte[0x4000];
    rom.length= rom.buffer.length;

    if (rom.length != expectedLength) {
      Ui.error(UiError.ERROR, "ROM '%s' is %d bytes long; expected %d bytes", filename, rom.length, expectedLength);
      Utils.closeFile(rom);
      return 1;
    }

    error = loadRomBankFromBuffer(bankMap, pageNum, rom.buffer, rom.length, custom);

    Utils.closeFile(rom);

    return error;
  }

  public static int loadRomBank(MemoryPage[] bankMap, int pageNum, String filename, String fallback, int expectedLength) {
    boolean custom = fallback != null && !filename.equals(fallback);
    int retval = loadRomBankFromFile(bankMap, pageNum, filename, expectedLength, custom);
    if (retval != 0 && fallback != null && custom) {
      retval = loadRomBankFromFile(bankMap, pageNum, fallback, expectedLength, false);
    }
    return retval;
  }

  public static int loadRom(int pageNum, String filename, String fallback, int expectedLength) {
    return loadRomBank(Memory.mapRom, pageNum, filename, fallback, expectedLength);
  }

  public static int reset(boolean hardReset) {
//        Pokemem.clear();
//
//        Sound.ayReset();
//
//        Tape.stop();

    Memory.poolFree();
//    current = new FuseMachineInfo();
//    current.reset = () -> {
//    };
//    current.ram = new Spectrum.RamInfo();
//    current.ram.romcs = false;
//    current.memoryMap = () -> {
//    };
    setVariableTimings(current);

    Memory.reset();

    current.reset.run();
//        if (error != 0) return error;

    Module.reset(hardReset ? 1 : 0);

    current.memoryMap.run();
//        if (error != 0) return error;

    for (int i = 0; i < (int) current.timings.tstatesPerFrame; i++) {
      Ula.contention[i] = (byte) current.ramInfo.contendDelay.apply(i);
      Ula.contentionNoMreq[i] = (byte) current.ramInfo.contendDelayNoMreq.apply(i);
    }

//        Ui.menuDiskUpdate();

    display.refreshAll();

    return 0;
  }

  private static void setConstTimings(FuseMachineInfo machine) {
    machine.timings.processorSpeed = Timings.processorSpeed(machine.machine);
    machine.timings.leftBorder = Timings.leftBorder(machine.machine);
    machine.timings.horizontalScreen = Timings.horizontalScreen(machine.machine);
    machine.timings.rightBorder = Timings.rightBorder(machine.machine);
    machine.timings.tstatesPerLine = Timings.tstatesPerLine(machine.machine);
    machine.timings.interruptLength = Timings.interruptLength(machine.machine);
    machine.timings.tstatesPerFrame = Timings.tstatesPerFrame(machine.machine);
  }

  private static void setVariableTimings(FuseMachineInfo machine) {
    machine.lineTimes[0] = Timings.topLeftPixel(machine.machine) -
        display.BORDER_HEIGHT * machine.timings.tstatesPerLine -
        4 * display.BORDER_WIDTH_COLS;

    if (Settings.current.lateTimings) machine.lineTimes[0]++;

    for (int y = 1; y < display.SCREEN_HEIGHT + 1; y++) {
      machine.lineTimes[y] = machine.lineTimes[y - 1] + machine.timings.tstatesPerLine;
    }
  }

  static void end() {
    for (int i = 0; i < machineTypes.size(); i++) {
      if (machineTypes.get(i).shutdown != null) machineTypes.get(i).shutdown.run();
    }

    machineTypes = null;
  }
}