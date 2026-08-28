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

import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.z80.helpers.Helper;

@Singleton
public class UPDFdc {

@Inject
  public UPDFdc(Settings settings) {
    this.settings = settings;
  }

  // Enums
  public enum UpdType {
    UPD765A,
    UPD765B
  }

  public enum UpdClock {
    UPD_CLOCK_4MHZ,
    UPD_CLOCK_8MHZ
  }

  public enum UpdScan {
    UPD_SCAN_EQ,
    UPD_SCAN_LO,
    UPD_SCAN_HI
  }

  public enum UpdCmdId {
    UPD_CMD_READ_DATA,
    UPD_CMD_READ_DIAG,
    UPD_CMD_WRITE_DATA,
    UPD_CMD_WRITE_ID,
    UPD_CMD_SCAN,
    UPD_CMD_READ_ID,
    UPD_CMD_RECALIBRATE,
    UPD_CMD_SENSE_INT,
    UPD_CMD_SPECIFY,
    UPD_CMD_SENSE_DRIVE,
    UPD_CMD_VERSION,
    UPD_CMD_SEEK,
    UPD_CMD_INVALID
  }

  public enum UpdIntrq {
    UPD_INTRQ_NONE,
    UPD_INTRQ_RESULT,
    UPD_INTRQ_EXE,
    UPD_INTRQ_READY,
    UPD_INTRQ_SEEK
  }

  public enum UpdFdcState {
    UPD_FDC_STATE_CMD,
    UPD_FDC_STATE_EXE,
    UPD_FDC_STATE_RES
  }

  public enum UpdFdcAmType {
    UPD_FDC_AM_NONE,
    UPD_FDC_AM_ID
  }

  // Command structure
  public class UPDCmd {
    UpdCmdId id;         // Command ID
    byte mask;           // && mask
    byte value;          // == value
    int cmdLength;       // Command length
    int resLength;       // Result length

    public UPDCmd(UpdCmdId id, byte mask, byte value, int cmdLength, int resLength) {
      this.id = id;
      this.mask = mask;
      this.value = value;
      this.cmdLength = cmdLength;
      this.resLength = resLength;
    }
  }

  // FDC structure
  Fdd currentDrive;               // Current drive
  public Fdd[] drive = new Fdd[4];       // UPD765 controls 4 drives
  private Settings settings;

  {
    Helper.fillArrayWith(drive, () -> new Fdd(settings));
  }


  UpdType type;                   // UPD765A or UPD765B
  UpdClock clock;                 // Clock rate (4/8 MHz)
  int stpRate;                    // Stepping rate (ms)
  int hutTime;                    // Head unload time (ms)
  int hldTime;                    // Head load time (ms)
  int nonDma;                     // Operating mode
  int firstRw;                    // First sector always read/write even if EOT < R
  int spinCycles;                 // Spin cycles
  int direction;                  // 0 = spindlewards, 1 = rimwards (Fdd.FDD_DIR_*)
  UpdIntrq intrq;                 // Last INTRQ status
  int datarq;                     // DRQ line status
  UpdFdcState state;              // FDC state
  int idTrack;                    // ID track
  int idHead;                     // ID head
  int idSector;                   // ID sector
  int idLength;                   // Sector length code (0, 1, 2, 3)
  int sectorLength;               // Sector length from length code
  int ddam;                       // Read a deleted data mark
  int rev;                        // Revolution counter
  int headLoad;                   // Head state
  int readId;                     // Searching an IDAM
  UpdFdcAmType idMark;            // ID mark type
  int lastSectorRead;             // For Speedlock 'random' sector hack
  public int speedlock;                  // For Speedlock hack, -1 to disable
  int dataOffset;                 // State during transfer
  int cycle;                      // Read/write cycle number
  int delData;                    // READ/WRITE deleted data
  int mt;                         // Multitrack operations
  int mf;                         // MFM mode
  int sk;                         // Skip deleted/not deleted data
  int hd;                         // Physical head address
  int us;                         // Unit select (0-3)
  int[] pcn = new int[4];         // Present cylinder numbers
  int[] ncn = new int[4];         // New cylinder numbers
  int[] rec = new int[4];         // Recalibrate store PCNs
  int[] seek = new int[4];        // Seek status for 4 drives
  int[] seekAge = new int[4];     // Order of overlapped seeks for 4 drives
  int rlen;                       // Expected record length
  UpdScan scan;                   // SCAN type: eq/lo/hi
  UPDCmd cmd;                     // Current command
  byte commandRegister;           // Command register
  byte[] dataRegister = new byte[9]; // Data registers
  byte mainStatus;                // Main status register
  byte[] statusRegister = new byte[4]; // Status registers
  byte[] senseIntRes = new byte[2]; // Result bytes for SENSE INTERRUPT
  int crc;                        // CRC value
  public Runnable setIntrq;              // Set INTRQ callback
  public Runnable resetIntrq;            // Reset INTRQ callback
  public Runnable setDatarq;             // Set DATARQ callback
  public Runnable resetDatarq;           // Reset DATARQ callback

  // Initialize FDC events
  public void initEvents() {
    // Placeholder for event initialization
//        Event.init();
  }

  // Allocate an FDC
  public UPDFdc allocFdc(UpdType type, UpdClock clock) {
    UPDFdc fdc = new UPDFdc(settings);
    fdc.type = type;
    fdc.clock = clock;
    return fdc;
  }

  // Master reset the FDC
  public void masterReset(UPDFdc fdc) {
    // Placeholder for master reset logic
    fdc.state = UpdFdcState.UPD_FDC_STATE_CMD;
    fdc.intrq = UpdIntrq.UPD_INTRQ_NONE;
    fdc.datarq = 0;
    fdc.cmd = null;
    fdc.commandRegister = 0;
    for (int i = 0; i < fdc.dataRegister.length; i++) {
      fdc.dataRegister[i] = 0;
    }
    for (int i = 0; i < fdc.statusRegister.length; i++) {
      fdc.statusRegister[i] = 0;
    }
    for (int i = 0; i < fdc.senseIntRes.length; i++) {
      fdc.senseIntRes[i] = 0;
    }
  }

  // Read FDC status
  public byte readStatus(UPDFdc fdc) {
    // Placeholder for reading status
    return fdc.mainStatus;
  }

  // Read FDC data
  public byte readData(UPDFdc fdc) {
    // Placeholder for reading data
    return fdc.dataRegister[0];
  }

  // Write FDC data
  public void writeData(UPDFdc fdc, byte b) {
    // Placeholder for writing data
    fdc.dataRegister[0] = b;
    if (fdc.state == UpdFdcState.UPD_FDC_STATE_CMD) {
      // Process command byte
    } else if (fdc.state == UpdFdcState.UPD_FDC_STATE_EXE) {
      // Handle execution phase
    }
  }

}