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
import java.util.Random;
import java.util.function.Supplier;

public class Fuse {

  // What name were we called under?
  public static String progname;

  // A flag to say when we want to exit the emulator
  public static boolean exiting;

  // Is Spectrum emulation currently paused, and if so, how many times?
  public static int emulationPaused;

  // The creator information we'll store in file formats that support this
  public static LibspectrumCreator creator;
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

    public void setRAM(byte[][] RAM) {

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

  // The various types of file we may want to run on startup
  static class StartFiles {
    String diskPlus3;
    String diskOpus;
    String diskPlusd;
    String diskBeta;
    String diskDidaktik80;
    String diskDisciple;
    String dock;
    String if2;
    String playback;
    String recording;
    String snapshot;
    String tape;
    String simpleideMaster;
    String simpleideSlave;
    String zxataspMaster;
    String zxataspSlave;
    String zxcf;
    String divideMaster;
    String divideSlave;
    String divmmc;
    String zxmmc;
    String[] mdr = new String[8];
  }

  public static void main(String[] args) {
    if (fuseInit(args) != 0) {
      System.err.println(progname + ": error initialising -- giving up!");
      throw new RuntimeException("1");
    }

    while (!exiting) {
      z80.doOpcodes();
      eventManager.eventDoEvents();
    }

    fuseEnd();
  }

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

  private static void extracted(StartupModule e, List<StartupModule> moduleList) {
    moduleList.add(e);
  }

  public static int fuseInit(String[] args) {
    int error, firstArg;
    String startScaler;
    StartFiles startFiles = new StartFiles();

    // Seed random number generator
    new Random().setSeed(System.currentTimeMillis());

    progname = args.length > 0 ? args[0] : "fuse";

//        Libspectrum.errorFunction = Ui::libspectrumError;

    // Wii-specific display init (assume handled by Display)
//        if (Display.init(args) != 0) return 1;

    firstArg = Settings.init(args);
    if (firstArg < 0) return 1;

    startScaler = Settings.current.startScalerMode;

    String[] argv = args;
    if (runStartupManager() != 0) return 1;

    Settings.current.startMachine = "48";
    error = machine.selectId(Settings.current.startMachine);
    if (error != 0) return error;

    error = Scaler.selectId(startScaler);
    if (error != 0) return error;

    if (setupStartFiles(startFiles) != 0) return 1;
    if (parseNonoptionArgs(args, firstArg, startFiles) != 0) return 1;

    if (Ui.mousePresent) Ui.mouseGrabbed = Ui.mouseGrab(true);

    emulationPaused = 0;
    Movie.init();

    return 0;
  }

  private static int parseNonoptionArgs(String[] args, int firstArg, StartFiles startFiles) {
    return 0;
  }

  private static int setupStartFiles(StartFiles startFiles) {
    startFiles.diskPlus3 = Settings.current.plus3diskFile;
    startFiles.diskOpus = Settings.current.opusdiskFile;
    startFiles.diskPlusd = Settings.current.plusddiskFile;
    startFiles.diskDidaktik80 = Settings.current.didaktik80diskFile;
    startFiles.diskDisciple = Settings.current.disciplediskFile;
    startFiles.diskBeta = Settings.current.betadiskFile;
    startFiles.dock = Settings.current.dckFile;
    startFiles.if2 = Settings.current.if2File;
    startFiles.playback = Settings.current.playbackFile;
    startFiles.recording = Settings.current.recordFile;
    startFiles.snapshot = Settings.current.snapshot;
    startFiles.tape = Settings.current.tapeFile;

    startFiles.simpleideMaster = Settings.current.simpleideMasterFile;
    startFiles.simpleideSlave = Settings.current.simpleideSlaveFile;
    startFiles.zxataspMaster = Settings.current.zxataspMasterFile;
    startFiles.zxataspSlave = Settings.current.zxataspSlaveFile;
    startFiles.zxcf = Settings.current.zxcfPriFile;
    startFiles.divideMaster = Settings.current.divideMasterFile;
    startFiles.divideSlave = Settings.current.divideSlaveFile;
    startFiles.divmmc = Settings.current.divmmcFile;
    startFiles.zxmmc = Settings.current.zxmmcFile;

    startFiles.mdr[0] = Settings.current.mdrFile;
    startFiles.mdr[1] = Settings.current.mdrFile2;
    startFiles.mdr[2] = Settings.current.mdrFile3;
    startFiles.mdr[3] = Settings.current.mdrFile4;
    startFiles.mdr[4] = Settings.current.mdrFile5;
    startFiles.mdr[5] = Settings.current.mdrFile6;
    startFiles.mdr[6] = Settings.current.mdrFile7;
    startFiles.mdr[7] = Settings.current.mdrFile8;

    return 0;
  }

  private static int fuseEnd() {
    Movie.stop();
    StartupManager.runEnd();
    periph.end();
    Ui.end();
//        UiMedia.driveEnd();
    Module.end();
//        Pokemem.end();
//        Svg.captureEnd();
    Libspectrum.end();
    return 0;
  }

  public static void fuseAbort() {
    fuseEnd();
    System.exit(1);
  }

}