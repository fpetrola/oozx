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

import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.peripherals.Periph;

public class SpecPlus3 extends FuseMachineInfo implements SpectrumMachine {
  private Memory memory;
  private UPDFdc specplus3Fdc;
  private Display display;
  private Machine machine;
  private MachinesPeriph machinesPeriph;
  private Spectrum spectrum;
  private Spec48 spec48;
  private Periph periph;

  public SpecPlus3(Memory memory, Display display, Machine machine, MachinesPeriph machinesPeriph, Spectrum spectrum, Spec48 spec48, Periph periph) {
    super(display);
    this.memory = memory;
    this.display = display;
    this.machine = machine;
    this.machinesPeriph = machinesPeriph;
    this.spectrum = spectrum;
    this.spec48 = spec48;
    this.periph = periph;
  }
//  public  Fdd[] specplus3Drives = new Fdd[SpecPlus3Constants.SPECPLUS3_NUM_DRIVES];

//  public  UIMediaDriveInfo[] uiDrives = new UIMediaDriveInfo[]{
//      new UIMediaDriveInfo(
//          "+3 Disk A:",
//          UIMediaConstants.UI_MEDIA_CONTROLLER_PLUS3,
//          SpecPlus3Constants.SPECPLUS3_DRIVE_A,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3,
//          UIMediaConstants.UI_MENU_ITEM_INVALID,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3_A_EJECT,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3_A_FLIP_SET,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3_A_WP_SET,
//          SpecPlus3::uiDriveIsAvailable,
//          SpecPlus3::uiDriveGetParamsA,
//          SpecPlus3::uiDriveInserted,
//          SpecPlus3::uiDriveAutoload
//      ),
//      new UIMediaDriveInfo(
//          "+3 Disk B:",
//          UIMediaConstants.UI_MEDIA_CONTROLLER_PLUS3,
//          SpecPlus3Constants.SPECPLUS3_DRIVE_B,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3_B,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3_B_EJECT,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3_B_FLIP_SET,
//          UIMediaConstants.UI_MENU_ITEM_MEDIA_DISK_PLUS3_B_WP_SET,
//          SpecPlus3::uiDriveIsAvailable,
//          SpecPlus3::uiDriveGetParamsB,
//          SpecPlus3::uiDriveInserted,
//          SpecPlus3::uiDriveAutoload
//      )
//  };

  // Check if a port is handled by the ULA
  public boolean portFromUla(int port) {
    // No contended ports
    return false;
  }

  // Initialize the Spectrum +3 machine
  public FuseMachineInfo init() {
    fuseMachineInfo.machine = Libspectrum.Machine.PLUS3;
    fuseMachineInfo.id = "plus3";

    fuseMachineInfo.reset = this::reset;
    fuseMachineInfo.timex = false;
    fuseMachineInfo.ramInfo.portFromUla = this::portFromUla;
    fuseMachineInfo.ramInfo.contendDelay = spectrum::contendDelay76543210;
    fuseMachineInfo.ramInfo.contendDelayNoMreq = spectrum::contendDelayNone;
    fuseMachineInfo.ramInfo.validPages = 8;

//    machine.unattachedPort = Libretro.LIBRETRO ? Spectrum::spectrumUnattachedPortAmstrad : Spectrum::spectrumUnattachedPortNone;
    fuseMachineInfo.unattachedPort = spectrum::spectrumUnattachedPortNone;

    specplus3765Init();
    specplus3MenuItems();

    fuseMachineInfo.shutdown = this::shutdown;
    fuseMachineInfo.memoryMap = this::memoryMap;

    return this;
  }

  // Update FDC settings
  public void specplus3765UpdateFdd() {
    specplus3Fdc.speedlock = Settings.current.plus3DetectSpeedlock ? 0 : -1;
  }

  // Initialize FDC and drives
  public void specplus3765Init() {
//    specplus3Fdc = UPDFdc.allocFdc(UPDFdc.UPD765A, UPDFdc.UPD_CLOCK_4MHZ);
//    // +3 uses US0 pin to select drives: drive 2 := drive 0, drive 3 := drive 1
//    specplus3Fdc.drive[0] = specplus3Drives[0];
//    specplus3Fdc.drive[1] = specplus3Drives[1];
//    specplus3Fdc.drive[2] = specplus3Drives[0];
//    specplus3Fdc.drive[3] = specplus3Drives[1];
//
//    for (int i = 0; i < SpecPlus3Constants.SPECPLUS3_NUM_DRIVES; i++) {
//      specplus3Drives[i] = new Fdd();
//      specplus3Drives[i].disk.flag = Disk.DISK_FLAG_PLUS3_CPC;
//    }
//
//    // Built-in drive 1 head 42 track
//    Fdd.init(specplus3Drives[0], Fdd.FDD_SHUGART, FddParams.fddParams[1], false);
//    // Drive geometry autodetect
//    Fdd.init(specplus3Drives[1], Fdd.FDD_SHUGART, null, false);

    specplus3Fdc = new UPDFdc();

    specplus3Fdc.setIntrq = null;
    specplus3Fdc.resetIntrq = null;
    specplus3Fdc.setDatarq = null;
    specplus3Fdc.resetDatarq = null;

    specplus3765UpdateFdd();

//    for (int i = 0; i < SpecPlus3.Constants.SPECPLUS3_NUM_DRIVES; i++) {
//      uiDrives[i].fdd = specplus3Drives[i];
//      UIMedia.driveRegister(uiDrives[i]);
//    }
  }

  // Reset FDC and drives
  public void specplus3765Reset() {
//    FddParams dt = FddParams.fddParams[Options.enumerateDiskoptionsDrivePlus3aType() + 1]; // +1 => no Disabled
    UPDFdc.masterReset(specplus3Fdc);
//    Fdd.init(specplus3Drives[0], Fdd.FDD_SHUGART, dt, true);

//    dt = FddParams.fddParams[Options.enumerateDiskoptionsDrivePlus3bType()];
//    Fdd.init(specplus3Drives[1], dt.enabled ? Fdd.FDD_SHUGART : Fdd.FDD_TYPE_NONE, dt, true);
  }

  // Reset the Spectrum +3 machine
  public int reset() {
    int error = machine.loadRom(0, Settings.current.romPlus30, Settings.defaults.romPlus30, 0x4000);
    if (error != 0) return error;
    error = machine.loadRom(1, Settings.current.romPlus31, Settings.defaults.romPlus31, 0x4000);
    if (error != 0) return error;
    error = machine.loadRom(2, Settings.current.romPlus32, Settings.defaults.romPlus32, 0x4000);
    if (error != 0) return error;
    error = machine.loadRom(3, Settings.current.romPlus33, Settings.defaults.romPlus33, 0x4000);
    if (error != 0) return error;

    error = plus2aCommonReset();
    if (error != 0) return error;

    periph.clear();
    machinesPeriph.machinesPeriphPlus3();

    periph.setPresent(Periph.Type.UPD765, Periph.Present.ALWAYS);

    periph.update();

    specplus3765Reset();
    specplus3MenuItems();

    spec48.commonDisplaySetup();

    return 0;
  }

  // Common reset for +2A/+3
  public int plus2aCommonReset() {
    FuseMachineInfo machineCurrent = machine.current;

    machineCurrent.ramInfo.currentPage = 0;
    machineCurrent.ramInfo.currentRom = 0;
    machineCurrent.ramInfo.locked = false;
    machineCurrent.ramInfo.special = false;
    machineCurrent.ramInfo.lastByte = 0;
    machineCurrent.ramInfo.lastByte2 = 0;

    memory.currentScreen = 5;
    memory.screenMask = 0xffff;

    // All memory comes from the home bank
    for (int i = 0; i < memory.PAGES_IN_64K; i++) {
      memory.mapRead[i].source = memory.mapWrite[i].source = memory.sourceRam;
    }

    // RAM pages 4, 5, 6, and 7 contended
    for (int i = 0; i < 8; i++) {
      memory.ramSet16kContention(i, i >= 4);
    }

    return normalMemoryMap(0, 0);
  }

  // Normal memory mapping
  private int normalMemoryMap(int rom, int page) {
    memory.map16k(0x0000, memory.mapRom, rom);
    memory.map16k(0x4000, memory.mapRam, 5);
    memory.map16k(0x8000, memory.mapRam, 2);
    memory.map16k(0xc000, memory.mapRam, page);
    return 0;
  }

  // Special memory mapping
  private void specialMemoryMap(int which) {
    switch (which) {
      case 0:
        selectSpecialMap(0, 1, 2, 3);
        break;
      case 1:
        selectSpecialMap(4, 5, 6, 7);
        break;
      case 2:
        selectSpecialMap(4, 5, 6, 3);
        break;
      case 3:
        selectSpecialMap(4, 7, 6, 3);
        break;
      default:
//        UI.error(UIMediaConstants.UI_ERROR_ERROR, "unknown +3 special configuration %d", which);
        throw new RuntimeException("Fuse abort: unknown +3 special configuration");
    }
  }

  // Select special memory mapping
  private void selectSpecialMap(int page1, int page2, int page3, int page4) {
    memory.map16k(0x0000, memory.mapRam, page1);
    memory.map16k(0x4000, memory.mapRam, page2);
    memory.map16k(0x8000, memory.mapRam, page3);
    memory.map16k(0xc000, memory.mapRam, page4);
  }

  // Write to the +3 memory port 2 (0x1FFD)
  public void memoryPort2WriteInternal(int port, byte b) {
    Printer.parallelStrobeWrite(b & 0x10);

//    if ((Machine.current.capabilities & Libspectrum.MachineCapability.PLUS3_DISK) != 0) {
//      Fdd.motorOn(specplus3Drives[0], (b & 0x08) != 0);
//      Fdd.motorOn(specplus3Drives[1], (b & 0x08) != 0);
//    }

    machine.current.ramInfo.lastByte2 = b;

    machine.current.memoryMap.run();
  }

  public void memoryPort2Write(int port, byte b) {
    if (machine.current.ramInfo.locked) return;
    memoryPort2WriteInternal(port, b);
  }

  // Map memory for +3
  public void memoryMap() {
    FuseMachineInfo machineCurrent = machine.current;
    byte lastByte = machineCurrent.ramInfo.lastByte;
    byte lastByte2 = machineCurrent.ramInfo.lastByte2;

    int page = lastByte & 0x07;
    int screen = (lastByte & 0x08) != 0 ? 7 : 5;
    int rom = ((lastByte & 0x10) >> 4) | ((lastByte2 & 0x04) >> 1);

    if (memory.currentScreen != screen) {
      display.dirtySinclair(0);
      display.writeIfDirtySinclair(0, 0);
      memory.currentScreen = screen;
    }

    if ((lastByte2 & 0x01) != 0) {
      machineCurrent.ramInfo.special = true;
      specialMemoryMap((lastByte2 & 0x06) >> 1);
    } else {
      machineCurrent.ramInfo.special = false;
      normalMemoryMap(rom, page);
    }

    machineCurrent.ramInfo.currentPage = page;
    machineCurrent.ramInfo.currentRom = rom;

    memory.romcsMap();

  }

  // Update menu items for +3 drives
  public void specplus3MenuItems() {
//    UIMedia.driveUpdateMenus(uiDrives[SpecPlus3Constants.SPECPLUS3_DRIVE_A], UIMediaConstants.UI_MEDIA_DRIVE_UPDATE_ALL);
//    UIMedia.driveUpdateMenus(uiDrives[SpecPlus3Constants.SPECPLUS3_DRIVE_B], UIMediaConstants.UI_MEDIA_DRIVE_UPDATE_ALL);
  }

  // Read FDC status
  public byte fdcStatus(int port, byte[] attached) {
    attached[0] = (byte) 0xFF; // TODO: check this
    return UPDFdc.readStatus(specplus3Fdc);
  }

  // Read FDC data
  public byte fdcRead(int port, byte[] attached) {
    attached[0] = (byte) 0xFF; // TODO: check this
    return UPDFdc.readData(specplus3Fdc);
  }

  // Write FDC data
  public void fdcWrite(int port, byte data) {
    UPDFdc.writeData(specplus3Fdc, data);
  }

//  // Insert disk into +3 drive
//  public  int diskInsert(int which, String filename, boolean autoload) {
//    if (which >= SpecPlus3Constants.SPECPLUS3_NUM_DRIVES) {
//      UI.error(UIMediaConstants.UI_ERROR_ERROR, "specplus3_disk_insert: unknown drive %d", which);
//      throw new RuntimeException("Fuse abort: unknown drive");
//    }
//    return UIMedia.driveInsert(uiDrives[which], filename, autoload);
//  }

  // Get FDD for a specific drive
//  public  Fdd getFdd(int which) {
//    return specplus3Drives[which];
//  }

  // Check if drive is available
  private boolean uiDriveIsAvailable() {
    return (machine.current.capabilities & Libspectrum.MachineCapability.PLUS3_DISK) != 0;
  }

//  // Get parameters for drive A
//  private  FddParams uiDriveGetParamsA() {
//    return FddParams.fddParams[Options.enumerateDiskoptionsDrivePlus3aType() + 1];
//  }
//
//  // Get parameters for drive B
//  private  FddParams uiDriveGetParamsB() {
//    return FddParams.fddParams[Options.enumerateDiskoptionsDrivePlus3bType()];
//  }
//
//  // Handle drive insertion
//  private  int uiDriveInserted(UIMediaDriveInfo drive, boolean newDisk) {
//    if (newDisk) {
//      Disk.preformat(drive.fdd.disk);
//    }
//    return 0;
//  }
//
//  // Handle drive autoload
//  private  int uiDriveAutoload() {
//    Machine.reset(false);
//    PhantomTypist.activateDisk();
//    return 0;
//  }

  // Shutdown the +3 machine
  public int shutdown() {
    return 0;
  }
}
