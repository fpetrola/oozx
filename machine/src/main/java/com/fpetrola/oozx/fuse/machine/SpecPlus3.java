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

package com.fpetrola.oozx.fuse.machine;

import com.fpetrola.oozx.*;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.fuse.modules.Display;
import com.fpetrola.oozx.fuse.modules.EventManager;
import com.fpetrola.oozx.fuse.modules.Timer;
import com.fpetrola.oozx.fuse.modules.z80.Z80;
import com.fpetrola.oozx.fuse.peripherals.*;
import com.fpetrola.z80.helpers.Helper;

public class SpecPlus3 extends Spec128 {
  private UPDFdc specplus3Fdc;
  public Fdd fdd;
  public UPDFdc uPDFdc;

  public SpecPlus3(Memory memory, Display display, MachinesPeriph machinesPeriph, IPeriph periph, Settings settings, Fdd fdd, UPDFdc uPDFdc, EventManager eventManager, Z80 z80, Timer timer, Module module) {
    super(memory, display, machinesPeriph, periph, settings, eventManager, z80, timer, module,  new SpecPlus3RamInfo(8));
    this.fdd = fdd;
    this.uPDFdc = uPDFdc;
    specplus3765Init();
    specplus3MenuItems();
  }

  public int contendDelay(long time) {
    return contendDelay76543210(time);
  }

  public int contendDelayNoMreq(long time) {
    return contendDelayNone(time);
  }

  @Override
  protected void init() {
    periph.register(new Spec128MemoryPeripheral(this));
    periph.register(new SpecPlus3MemoryPeripheral(this));
    periph.register(new Upd765Peripheral(this));
  }

  public Fdd[] specplus3Drives = new Fdd[SpecPlus3Constants.SPECPLUS3_NUM_DRIVES];


  // Check if a port is handled by the ULA
  public boolean portFromUla(int port) {
    // No contended ports
    return false;
  }

  // Initialize the Spectrum +3 machine

  // Update FDC settings
  public void specplus3765UpdateFdd() {
    specplus3Fdc.speedlock = settings.current.plus3DetectSpeedlock ? 0 : -1;
  }

  // Initialize FDC and drives
  public void specplus3765Init() {
    specplus3Fdc = uPDFdc.allocFdc(UPDFdc.UpdType.UPD765A, UPDFdc.UpdClock.UPD_CLOCK_4MHZ);
    // +3 uses US0 pin to select drives: drive 2 := drive 0, drive 3 := drive 1
    specplus3Fdc.drive[0] = specplus3Drives[0];
    specplus3Fdc.drive[1] = specplus3Drives[1];
    specplus3Fdc.drive[2] = specplus3Drives[0];
    specplus3Fdc.drive[3] = specplus3Drives[1];

    for (int i = 0; i < SpecPlus3Constants.SPECPLUS3_NUM_DRIVES; i++) {
      specplus3Drives[i] = new Fdd(settings);
      specplus3Drives[i].disk.flag = Disk.DISK_FLAG_PLUS3_CPC;
    }

    // Built-in drive 1 head 42 track
    Helper.fillArrayWith(FddParams.fddParams, FddParams::new);

    fdd.init(specplus3Drives[0], fdd.FDD_SHUGART, FddParams.fddParams[1], false);
    // Drive geometry autodetect
    fdd.init(specplus3Drives[1], fdd.FDD_SHUGART, null, false);

    specplus3Fdc = new UPDFdc(settings);

    specplus3Fdc.setIntrq = null;
    specplus3Fdc.resetIntrq = null;
    specplus3Fdc.setDatarq = null;
    specplus3Fdc.resetDatarq = null;

    specplus3765UpdateFdd();

//    for (int i = 0; i < SpecPlus3Constants.SPECPLUS3_NUM_DRIVES; i++) {
//      uiDrives[i].fdd = specplus3Drives[i];
//      UIMedia.driveRegister(uiDrives[i]);
//    }
  }

  // Reset FDC and drives
  public void specplus3765Reset() {
    FddParams dt = FddParams.fddParams[Options.enumerateDiskoptionsDrivePlus3aType() + 1]; // +1 => no Disabled
    uPDFdc.masterReset(specplus3Fdc);
    fdd.init(specplus3Drives[0], fdd.FDD_SHUGART, dt, true);

    dt = FddParams.fddParams[Options.enumerateDiskoptionsDrivePlus3bType()];
    fdd.init(specplus3Drives[1], dt.enabled != 0 ? fdd.FDD_SHUGART : FddConstants.FDD_TYPE_NONE, dt, true);
  }

  // Reset the Spectrum +3 machine
  public int reset() {
    int error = loadRom(0, settings.current.romPlus30, settings.defaults.romPlus30, 0x4000);
    if (error != 0) return error;
    error = loadRom(1, settings.current.romPlus31, settings.defaults.romPlus31, 0x4000);
    if (error != 0) return error;
    error = loadRom(2, settings.current.romPlus32, settings.defaults.romPlus32, 0x4000);
    if (error != 0) return error;
    error = loadRom(3, settings.current.romPlus33, settings.defaults.romPlus33, 0x4000);
    if (error != 0) return error;

    error = plus2aCommonReset();
    if (error != 0) return error;

    periph.clear();
    machinesPeriph.machinesPeriphPlus3();

    periph.setPresent(Periph.Type.UPD765, Periph.Present.ALWAYS);

    periph.update();

    specplus3765Reset();
    specplus3MenuItems();

//    spec48.commonDisplaySetup();

    return 0;
  }

  // Common reset for +2A/+3
  public int plus2aCommonReset() {
    RamInfo ramInfo1 = ramInfo;

    ramInfo1.currentPage = 0;
    ramInfo1.currentRom = 0;
    ramInfo1.locked = false;
    ramInfo1.special = false;
    ramInfo1.lastByte = 0;
    ramInfo1.lastByte2 = 0;

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
    boolean b1 = (getCapabilities() & Libspectrum.MachineCapability.PLUS3_DISK) != 0;
    b1 = true;
    if (b1) {
      fdd.motorOn(specplus3Drives[0], (b & 0x08) != 0);
      fdd.motorOn(specplus3Drives[1], (b & 0x08) != 0);
    }

    ramInfo.lastByte2 = b;

    memoryMap();
  }

  public void memoryPort2Write(int port, byte b) {
    if (ramInfo.locked) return;
    memoryPort2WriteInternal(port, b);
  }

  // Map memory for +3
  public void memoryMap() {
    RamInfo currentRamInfo = ramInfo;
    byte lastByte = currentRamInfo.lastByte;
    byte lastByte2 = currentRamInfo.lastByte2;

    int page = lastByte & 0x07;
    int screen = (lastByte & 0x08) != 0 ? 7 : 5;
    int rom = ((lastByte & 0x10) >> 4) | ((lastByte2 & 0x04) >> 1);

    if (memory.currentScreen != screen) {
      display.updateCritical(0, 0);
      display.refreshMainScreen();
//      display.dirtySinclair(0);
//      display.writeIfDirtySinclair(0, 0);
      memory.currentScreen = screen;
    }

    if ((lastByte2 & 0x01) != 0) {
      currentRamInfo.special = true;
      specialMemoryMap((lastByte2 & 0x06) >> 1);
    } else {
      currentRamInfo.special = false;
      normalMemoryMap(rom, page);
    }

    currentRamInfo.currentPage = page;
    currentRamInfo.currentRom = rom;

    memory.romcsMap(ramInfo);

  }

  // Update menu items for +3 drives
  public void specplus3MenuItems() {
//    UIMedia.driveUpdateMenus(uiDrives[SpecPlus3Constants.SPECPLUS3_DRIVE_A], UIMediaConstants.UI_MEDIA_DRIVE_UPDATE_ALL);
//    UIMedia.driveUpdateMenus(uiDrives[SpecPlus3Constants.SPECPLUS3_DRIVE_B], UIMediaConstants.UI_MEDIA_DRIVE_UPDATE_ALL);
  }

  // Read FDC status
  public byte fdcStatus(int port, byte[] attached) {
    attached[0] = (byte) 0xFF; // TODO: check this
    return uPDFdc.readStatus(specplus3Fdc);
  }

  // Read FDC data
  public byte fdcRead(int port, byte[] attached) {
    attached[0] = (byte) 0xFF; // TODO: check this
    byte b = uPDFdc.readData(specplus3Fdc);
    b = -1;
    return b;
  }

  // Write FDC data
  public void fdcWrite(int port, byte data) {
    uPDFdc.writeData(specplus3Fdc, data);
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
  public Fdd getFdd(int which) {
    return specplus3Drives[which];
  }

  // Check if drive is available
  private boolean uiDriveIsAvailable() {
    return (getCapabilities() & Libspectrum.MachineCapability.PLUS3_DISK) != 0;
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
  public void shutdown() {
  }

  public String getName() {
    return "Spectrum Plus 3";
  }

  public int unattachedPort(int port) {
    return spectrumUnattachedPortNone();
  }

  public TimingsHandler.Timings getBaseTiming() {
    return new TimingsHandler.Timings(3546900, 1773400, TimingsHandler.AMSTRAD_ASIC);
  }

  private static class SpecPlus3RamInfo extends RamInfo {
    public SpecPlus3RamInfo(int validPages) {
      this.validPages = validPages;
    }
  }
}


