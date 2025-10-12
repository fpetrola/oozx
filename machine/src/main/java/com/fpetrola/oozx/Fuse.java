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

import com.fpetrola.oozx.fuse.modules.Joystick;
import com.fpetrola.oozx.fuse.modules.*;
import com.fpetrola.oozx.fuse.modules.Keyboard;
import com.fpetrola.oozx.fuse.peripherals.Periph;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class Fuse {

  private static final String FUSE_COPYRIGHT = "";
  private static final String VERSION = "";
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
  public static Display display = new Display(memory, fuseMachineInfoSupplier, tStatesHolder, ramHolder);
  public static Keyboard keyboard = new Keyboard();
  public static Ula ula = new Ula(memory, display, fuseMachineInfoSupplier, keyboard);
  public static EventManager eventManager = new EventManager(fuseMachineInfoSupplier, tStatesHolder);
  public static Joystick joystick = new Joystick(keyboard);
  public static Z80 z80 = new Z80();
  public static Spectrum spectrum = new Spectrum(memory, display, eventManager, z80, tStatesHolder, ramHolder, fuseMachineInfoSupplier);
  public static Machine machine = new Machine(eventManager, memory, display, ula, tStatesHolder, spectrum);
  public static MachinesPeriph machinesPeriph = new MachinesPeriph(machine);

  public static void abort() {

  }

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

  private static final String LIBSPECTRUM_MIN_VERSION = "0.5.0";

  public static void main(String[] args) {
    int r;

    // Windows-specific error mode setting (simplified, assume handled by environment)
    // Wii-specific fatInitDefault (assume handled by environment)

    if (fuseInit(args) != 0) {
      System.err.println(progname + ": error initialising -- giving up!");
      throw new RuntimeException("1");
    }

    if (Settings.current.showHelp || Settings.current.showVersion) {
      throw new RuntimeException("help");
    }

    if (Settings.current.unittests) {
      r = Unittests.run();
    } else {
      while (!exiting) {
        z80.doOpcodes();
        eventManager.eventDoEvents();
      }
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
        new MachineStartupModule(machine),
        new MachinesPeriphStartupModule(machinesPeriph),
        new MemoryStartupModule(memory, ramHolder),
        new SpectrumStartupModule(spectrum),
        new UlaStartupModule(ula),
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

    if (Settings.current.showVersion) {
      fuseShowVersion();
      return 0;
    } else if (Settings.current.showHelp) {
      fuseShowHelp();
      return 0;
    }

    startScaler = Settings.current.startScalerMode;

    fuseShowCopyright();

    String[] argv = args;
    if (runStartupManager() != 0) return 1;

    Settings.current.startMachine = "48";
    error = machine.selectId(Settings.current.startMachine);
    if (error != 0) return error;

    error = Scaler.selectId(startScaler);
    if (error != 0) return error;

    if (setupStartFiles(startFiles) != 0) return 1;
    if (parseNonoptionArgs(args, firstArg, startFiles) != 0) return 1;
    if (doStartFiles(startFiles) != 0) return 1;

    if (Ui.mousePresent) Ui.mouseGrabbed = Ui.mouseGrab(true);

    emulationPaused = 0;
    Movie.init();

    return 0;
  }

  private static int parseNonoptionArgs(String[] args, int firstArg, StartFiles startFiles) {
    return 0;
  }

  private static void fuseShowCopyright() {
    System.out.println("\n");
    fuseShowVersion();
    System.out.printf(
        FUSE_COPYRIGHT + "; see the file\n" +
            "'AUTHORS' for more details.\n" +
            "\n" +
            "For help, please mail <fuse-emulator-devel@lists.sf.net> or use\n" +
            "the forums at <http://sourceforge.net/p/fuse-emulator/discussion/>.\n" +
            "\n" +
            "This program is distributed in the hope that it will be useful,\n" +
            "but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
            "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
            "GNU General Public License for more details.\n\n");
  }

  private static void fuseShowVersion() {
    System.out.println("The Free Unix Spectrum Emulator (Fuse) version " + VERSION + ".");
  }

  private static void fuseShowHelp() {
    System.out.println("\n");
    fuseShowVersion();
    System.out.println(
        "\nAvailable command-line options:\n\n" +
            "Boolean options (use `--no-<option>' to turn off):\n\n" +
            "--auto-load            Automatically load tape files when opened.\n" +
            "--compress-rzx         Write RZX files out compressed.\n" +
            "--issue2               Emulate an Issue 2 Spectrum.\n" +
            "--kempston             Emulate the Kempston joystick on QAOP<space>.\n" +
            "--loading-sound        Emulate the sound of tapes loading.\n" +
            "--sound                Produce sound.\n" +
            "--sound-force-8bit     Generate 8-bit sound even if 16-bit is available.\n" +
            "--slt                  Turn SLT traps on.\n" +
            "--traps                Turn tape traps on.\n\n" +
            "Other options:\n\n" +
            "--help                 This information.\n" +
            "--machine <type>       Which machine should be emulated?\n" +
            "--playback <filename>  Play back RZX file <filename>.\n" +
            "--record <filename>    Record to RZX file <filename>.\n" +
            "--separation <type>    Use ACB/ABC stereo for the AY-3-8912 sound chip.\n" +
            "--snapshot <filename>  Load snapshot <filename>.\n" +
            "--speed <percentage>   How fast should emulation run?\n" +
            "--fb-mode <mode>       Which mode should be used for FB?\n" +
            "--tape <filename>      Open tape file <filename>.\n" +
            "--version              Print version number and exit.\n" +
            "\n" +
            "For help, please mail <fuse-emulator-devel@lists.sf.net> or use\n" +
            "the forums at <http://sourceforge.net/p/fuse-emulator/discussion/>.\n" +
            "For complete documentation, see the manual page of Fuse.\n\n");
  }

  public static int fuseEmulationPause() {
    if (emulationPaused++ > 0) return 0;

    if (Rzx.recording && Rzx.competitionMode) {
      Ui.error(UiError.INFO, "Stopping competition mode RZX recording");
      int error = Rzx.stopRecording();
      if (error != 0) return error;
    }

    Sound.pause();
    return 0;
  }

  public static int fuseEmulationUnpause() {
    if (--emulationPaused > 0) return 0;

    Sound.unpause();

    int error = Timer.estimateReset();
    if (error != 0) return error;

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

//    private static int parseNonoptionArgs(String[] args, int firstArg, StartFiles startFiles) {
//        for (int i = firstArg; i < args.length; i++) {
//            String filename = args[i];
//
//            UtilsFile file = new UtilsFile();
//            int error = Utils.readFile(filename, file);
//            if (error != 0) return error;
//
//            LibspectrumIdType type = new LibspectrumIdType();
//            LibspectrumClassType classType = new LibspectrumClassType();
//            error = Libspectrum.identifyFileWithClass(type, classType, filename, file.buffer, file.length);
//            if (error != 0) {
//                Utils.closeFile(file);
//                return error;
//            }
//
//            switch (classType.getValue()) {
//                case LibspectrumClassType.CARTRIDGE_TIMEX:
//                    startFiles.dock = filename;
//                    break;
//
//                case LibspectrumClassType.CARTRIDGE_IF2:
//                    startFiles.if2 = filename;
//                    break;
//
//                case LibspectrumClassType.HARDDISK:
//                    if (Settings.current.zxcfActive) {
//                        startFiles.zxcf = filename;
//                    } else if (Settings.current.zxataspActive) {
//                        startFiles.zxataspMaster = filename;
//                    } else if (Settings.current.simpleideActive) {
//                        startFiles.simpleideMaster = filename;
//                    } else if (Settings.current.divideEnabled) {
//                        startFiles.divideMaster = filename;
//                    } else if (Settings.current.divmmcEnabled) {
//                        startFiles.divmmc = filename;
//                    } else if (Settings.current.zxmmcEnabled) {
//                        startFiles.zxmmc = filename;
//                    } else {
//                        Settings.current.zxcfActive = true;
//                        startFiles.zxcf = filename;
//                    }
//                    break;
//
//                case LibspectrumClassType.DISK_PLUS3:
//                    startFiles.diskPlus3 = filename;
//                    break;
//
//                case LibspectrumClassType.DISK_OPUS:
//                    startFiles.diskOpus = filename;
//                    break;
//
//                case LibspectrumClassType.DISK_DIDAKTIK:
//                    startFiles.diskDidaktik80 = filename;
//                    break;
//
//                case LibspectrumClassType.DISK_PLUSD:
//                    if (Periph.isActive(PeriphType.DISCIPLE)) {
//                        startFiles.diskDisciple = filename;
//                    } else {
//                        startFiles.diskPlusd = filename;
//                    }
//                    break;
//
//                case LibspectrumClassType.DISK_TRDOS:
//                    startFiles.diskBeta = filename;
//                    break;
//
//                case LibspectrumClassType.DISK_GENERIC:
//                    if ((Machine.current.capabilities & LibspectrumMachineCapability.PLUS3_DISK) != 0) {
//                        startFiles.diskPlus3 = filename;
//                    } else if ((Machine.current.capabilities & LibspectrumMachineCapability.TRDOS_DISK) != 0) {
//                        startFiles.diskBeta = filename;
//                    } else {
//                        if (Periph.isActive(PeriphType.BETA128)) {
//                            startFiles.diskBeta = filename;
//                        } else if (Periph.isActive(PeriphType.PLUSD)) {
//                            startFiles.diskPlusd = filename;
//                        } else if (Periph.isActive(PeriphType.DIDAKTIK80)) {
//                            startFiles.diskDidaktik80 = filename;
//                        } else if (Periph.isActive(PeriphType.DISCIPLE)) {
//                            startFiles.diskDisciple = filename;
//                        } else if (Periph.isActive(PeriphType.OPUS)) {
//                            startFiles.diskOpus = filename;
//                        }
//                    }
//                    break;
//
//                case LibspectrumClassType.RECORDING:
//                    startFiles.playback = filename;
//                    break;
//
//                case LibspectrumClassType.SNAPSHOT:
//                    startFiles.snapshot = filename;
//                    break;
//
//                case LibspectrumClassType.MICRODRIVE:
//                    for (int j = 0; j < 8; j++) {
//                        if (startFiles.mdr[j] == null) {
//                            startFiles.mdr[j] = filename;
//                            break;
//                        }
//                    }
//                    break;
//
//                case LibspectrumClassType.TAPE:
//                    startFiles.tape = filename;
//                    break;
//
//                case LibspectrumClassType.AUXILIARY:
//                    if (type.getValue() == LibspectrumIdType.AUX_POK) {
//                        Pokemem.setPokfile(filename);
//                    }
//                    break;
//
//                case LibspectrumClassType.UNKNOWN:
//                    Ui.error(UiError.WARNING, "couldn't identify '%s'; ignoring it", filename);
//                    break;
//
//                default:
//                    Ui.error(UiError.ERROR, "parse_nonoption_args: unknown file class %d", classType.getValue());
//                    break;
//            }
//
//            Utils.closeFile(file);
//        }
//
//        return 0;
//    }

  private static int doStartFiles(StartFiles startFiles) {
    int autoload = startFiles.snapshot == null ? Tape.canAutoload() : 0;

    if (startFiles.playback != null && startFiles.recording != null) {
      Ui.error(UiError.WARNING, "can't do both input playback and recording; recording disabled");
      startFiles.recording = null;
    }

    if (startFiles.diskPlus3 != null && startFiles.diskBeta != null) {
      Ui.error(UiError.WARNING, "can't use +3 and TR-DOS disks simultaneously; +3 disk ignored");
      startFiles.diskPlus3 = null;
    }

    if ((startFiles.diskPlus3 != null || startFiles.diskBeta != null) && startFiles.dock != null) {
      Ui.error(UiError.WARNING, "can't use disks and the dock simultaneously; dock cartridge ignored");
      startFiles.dock = null;
    }

    if ((startFiles.diskPlus3 != null || startFiles.diskBeta != null) && startFiles.if2 != null) {
      Ui.error(UiError.WARNING, "can't use disks and the Interface 2 simultaneously; cartridge ignored");
      startFiles.if2 = null;
    }

    int error;
    if (startFiles.diskPlus3 != null) {
      error = Utils.openFile(startFiles.diskPlus3, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.diskPlusd != null) {
      error = Utils.openFile(startFiles.diskPlusd, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.diskDidaktik80 != null) {
      error = Utils.openFile(startFiles.diskDidaktik80, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.diskDisciple != null) {
      error = Utils.openFile(startFiles.diskDisciple, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.diskOpus != null) {
      error = Utils.openFile(startFiles.diskOpus, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.diskBeta != null) {
      error = Utils.openFile(startFiles.diskBeta, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.dock != null) {
      error = Utils.openFile(startFiles.dock, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.if2 != null) {
      error = Utils.openFile(startFiles.if2, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.snapshot != null) {
      error = Utils.openFile(startFiles.snapshot, autoload, null);
      if (error != 0) return error;
    }

    if (startFiles.tape != null) {
      error = Utils.openFile(startFiles.tape, autoload, null);
      if (error != 0) return error;
    }

    for (int i = 0; i < 8; i++) {
      if (startFiles.mdr[i] != null) {
        error = Utils.openFile(startFiles.mdr[i], autoload, null);
        if (error != 0) return error;
      }
    }

//        if (startFiles.simpleideMaster != null) {
//            error = Simpleide.insert(startFiles.simpleideMaster, LibspectrumIdeType.MASTER);
//            Simpleide.reset(0);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.simpleideSlave != null) {
//            error = Simpleide.insert(startFiles.simpleideSlave, LibspectrumIdeType.SLAVE);
//            Simpleide.reset(0);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.zxataspMaster != null) {
//            error = Zxatasp.insert(startFiles.zxataspMaster, LibspectrumIdeType.MASTER);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.zxataspSlave != null) {
//            error = Zxatasp.insert(startFiles.zxataspSlave, LibspectrumIdeType.SLAVE);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.zxcf != null) {
//            error = Zxcf.insert(startFiles.zxcf);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.divideMaster != null) {
//            error = Divide.insert(startFiles.divideMaster, LibspectrumIdeType.MASTER);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.divideSlave != null) {
//            error = Divide.insert(startFiles.divideSlave, LibspectrumIdeType.SLAVE);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.divmmc != null) {
//            error = Divmmc.insert(startFiles.divmmc);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.zxmmc != null) {
//            error = Zxmmc.insert(startFiles.zxmmc);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.playback != null) {
//            boolean checkSnapshot = startFiles.snapshot == null;
//            error = Rzx.startPlayback(startFiles.playback, checkSnapshot);
//            if (error != 0) return error;
//        }
//
//        if (startFiles.recording != null) {
//            error = Rzx.startRecording(startFiles.recording, Settings.current.embedSnapshot);
//            if (error != 0) return error;
//        }

    return 0;
  }

  private static int fuseEnd() {
    Movie.stop();
    StartupManager.runEnd();
    Periph.end();
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