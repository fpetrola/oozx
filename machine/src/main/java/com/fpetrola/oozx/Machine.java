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

@FunctionalInterface
interface UnattachedPortFn {
  int apply();
}

class FuseMachineInfo {
  Spectrum.RamInfo ramInfo = new Spectrum.RamInfo();

  Libspectrum.Machine machine; // libspectrum_machine
  String id; // Used to select from command line
  int capabilities; // Capabilities of this machine

  Runnable reset; // Reset function

  boolean timex; // Timex machine (keyboard emulation/loading sounds etc.)

  MachineTimings timings = new MachineTimings(); // How long do things take to happen?
  long[] lineTimes = new long[Display.SCREEN_HEIGHT + 1]; // Redraw line y this many tstates after interrupt

  Spectrum.RamInfo ram = new Spectrum.RamInfo(); // How do we access memory, and what's currently paged in

  UnattachedPortFn unattachedPort; // What to return if we read from a port which isn't attached to anything

//    Ayinfo ay = new Ayinfo(); // The AY-3-8912 chip
//
//    SpecdrumInfo specdrum = new SpecdrumInfo(); // SpecDrum settings
//
//    CovoxInfo covox = new CovoxInfo(); // Covox settings

  Runnable shutdown; // Shutdown function

  Runnable memoryMap; // Memory map function
}

public class Machine {

  public static List<FuseMachineInfo> machineTypes = new ArrayList<>(); // All available machines

  public static FuseMachineInfo current; // The currently selected machine

  public static void registerStartup() {
    StartupManagerModule[] dependencies = {
        StartupManagerModule.MEMORY,
        StartupManagerModule.SETUID
    };
    StartupManager.register(StartupManagerModule.MACHINE, dependencies,
        Machine::initMachines, null, Machine::end);
  }

  private static int initMachines(Object context) {
    int error;

//        error = addMachine(Spec16::init);
//        if (error != 0) return error;
    error = addMachine(Spec48::init);
    if (error != 0) return error;
//        error = addMachine(Spec48Ntsc::init);
//        if (error != 0) return error;
//        error = addMachine(Spec128::init);
//        if (error != 0) return error;
//        error = addMachine(SpecPlus2::init);
//        if (error != 0) return error;
//        error = addMachine(SpecPlus2a::init);
//        if (error != 0) return error;
//        error = addMachine(SpecPlus3::init);
//        if (error != 0) return error;
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
    machine.timings.tstatesPerFrame = 69888;

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

        if (type != Libspectrum.Machine._48.ordinal()) {
          error = select(Libspectrum.Machine._48.ordinal());
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

    EventManager.reset();
//        EventManager.eventAdd(0, Timer.event);
    EventManager.eventAdd(machine.timings.tstatesPerFrame, Spectrum.spectrumFrameEvent);

    Sound.end();

    if (UiDisplay.end() != 0) return 1;

    capabilities = Libspectrum.machineCapabilities(machine.machine);

    if ((capabilities & Libspectrum.MachineCapability.TIMEX_VIDEO) != 0) {
      width = Display.SCREEN_WIDTH;
      height = 2 * Display.SCREEN_HEIGHT;
    } else {
      width = Display.ASPECT_WIDTH;
      height = Display.SCREEN_HEIGHT;
    }

    if (UiDisplay.init(width, height) != 0) return 1;

    Sound.init(Settings.current.soundDevice);

    machine.reset.run();
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
      page.page = Arrays.copyOfRange(data, offset, offset + Memory.PAGE_SIZE);
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

    current.ram.romcs = false;

    setVariableTimings(current);

    Memory.reset();

    current.reset.run();
//        if (error != 0) return error;

    Module.reset(hardReset ? 1 : 0);

    current.memoryMap.run();
//        if (error != 0) return error;

    for (int i = 0; i < (int) current.timings.tstatesPerFrame; i++) {
      Ula.contention[i] = (byte) current.ram.contendDelay.apply(i);
      Ula.contentionNoMreq[i] = (byte) current.ram.contendDelayNoMreq.apply(i);
    }

//        Ui.menuDiskUpdate();

    Display.refreshAll();

    return 0;
  }

  private static void setConstTimings(FuseMachineInfo machine) {
    machine.timings.processorSpeed = Libspectrum.timingsProcessorSpeed(machine.machine.ordinal());
    machine.timings.leftBorder = Libspectrum.timingsLeftBorder(machine.machine.ordinal());
    machine.timings.horizontalScreen = Libspectrum.timingsHorizontalScreen(machine.machine.ordinal());
    machine.timings.rightBorder = Libspectrum.timingsRightBorder(machine.machine.ordinal());
    machine.timings.tstatesPerLine = Libspectrum.timingsTstatesPerLine(machine.machine.ordinal());
    machine.timings.interruptLength = Libspectrum.timingsInterruptLength(machine.machine.ordinal());
    machine.timings.tstatesPerFrame = Libspectrum.timingsTstatesPerFrame(machine.machine.ordinal());
  }

  private static void setVariableTimings(FuseMachineInfo machine) {
    machine.lineTimes[0] = Libspectrum.timingsTopLeftPixel(machine.machine.ordinal()) -
        Display.BORDER_HEIGHT * machine.timings.tstatesPerLine -
        4 * Display.BORDER_WIDTH_COLS;

    if (Settings.current.lateTimings) machine.lineTimes[0]++;

    for (int y = 1; y < Display.SCREEN_HEIGHT + 1; y++) {
      machine.lineTimes[y] = machine.lineTimes[y - 1] + machine.timings.tstatesPerLine;
    }
  }

  private static void end() {
    for (int i = 0; i < machineTypes.size(); i++) {
      if (machineTypes.get(i).shutdown != null) machineTypes.get(i).shutdown.run();
    }

    machineTypes = null;
  }
}