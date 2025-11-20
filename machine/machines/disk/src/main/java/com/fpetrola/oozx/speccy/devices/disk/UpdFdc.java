/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.function.LongSupplier;

/**
 * NEC uPD765 floppy controller (the +3's): its two host registers on one side, the raw track
 * bytes, marks and CRCs on the other, across four drives. Timed events, as in {@link WdFdc}.
 */
public final class UpdFdc {

  public enum Type { UPD765A, UPD765B }

  /** 4MHz doubles every SPECIFY timing value; 8MHz uses them as given. */
  public enum Rate { MHZ4, MHZ8 }

  private enum Scan { EQ, LO, HI }

  /**
   * Declaration order matters: code elsewhere compares a command's ordinal against READ_ID,
   * RECALIBRATE, SENSE_INT and WRITE_DATA to group commands by behaviour.
   */
  private enum CmdId {
    READ_DATA, READ_DIAG, WRITE_DATA, WRITE_ID, SCAN, READ_ID,
    RECALIBRATE, SENSE_INT, SPECIFY, SENSE_DRIVE, VERSION, SEEK, INVALID
  }

  public enum Intrq { NONE, RESULT, EXE, READY, SEEK }

  private enum State { CMD, EXE, RES }

  private enum AmType { NONE, ID }

  private record Cmd(CmdId id, int mask, int value, int cmdLength, int resLength) {
  }

  /** The command byte is tested against these masks in order; the first match wins. */
  private static final Cmd[] COMMANDS = {
      new Cmd(CmdId.READ_DATA, 0x1f, 0x06, 0x08, 0x07),
      new Cmd(CmdId.READ_DATA, 0x1f, 0x0c, 0x08, 0x07),   // reads a sector marked deleted
      new Cmd(CmdId.READ_DIAG, 0x9f, 0x02, 0x08, 0x07),
      new Cmd(CmdId.RECALIBRATE, 0xff, 0x07, 0x01, 0x00),
      new Cmd(CmdId.SEEK, 0xff, 0x0f, 0x02, 0x00),
      new Cmd(CmdId.WRITE_DATA, 0x3f, 0x05, 0x08, 0x07),
      new Cmd(CmdId.WRITE_DATA, 0x3f, 0x09, 0x08, 0x07),  // writes a sector marked deleted
      new Cmd(CmdId.WRITE_ID, 0xbf, 0x0d, 0x05, 0x07),
      new Cmd(CmdId.SCAN, 0x1f, 0x11, 0x08, 0x07),
      new Cmd(CmdId.SCAN, 0x1f, 0x19, 0x08, 0x07),        // SCAN LOW OR EQUAL
      new Cmd(CmdId.SCAN, 0x1f, 0x1d, 0x08, 0x07),        // SCAN HIGH OR EQUAL
      new Cmd(CmdId.READ_ID, 0xbf, 0x0a, 0x01, 0x07),
      new Cmd(CmdId.SENSE_INT, 0xff, 0x08, 0x00, 0x02),
      new Cmd(CmdId.SPECIFY, 0xff, 0x03, 0x02, 0x00),
      new Cmd(CmdId.SENSE_DRIVE, 0xff, 0x04, 0x01, 0x01),
      new Cmd(CmdId.VERSION, 0x1f, 0x10, 0x00, 0x01),
      new Cmd(CmdId.INVALID, 0x00, 0x00, 0x00, 0x01),
  };

  private static final int MAX_SIZE_CODE = 8;

  private static final int MAIN_BUSY = 0x10;
  private static final int MAIN_EXECUTION = 0x20;
  private static final int MAIN_DATADIR = 0x40;
  /** Bit 6 set: host reads the data register. */
  private static final int MAIN_DATA_READ = 0x40;
  /** Bit 6 clear: host writes the data register. */
  private static final int MAIN_DATA_WRITE = 0x00;
  private static final int MAIN_DATAREQ = 0x80;

  private static final int ST0_NOT_READY = 0x08;
  private static final int ST0_EQUIP_CHECK = 0x10;
  private static final int ST0_SEEK_END = 0x20;
  /** ST0 bits 6-7 = 01: command ended abnormally. */
  private static final int ST0_INT_ABNORM = 0x40;
  /** ST0 bits 6-7 = 11: a drive's ready line changed. */
  private static final int ST0_INT_READY = 0xc0;

  private static final int ST1_MISSING_AM = 0x01;
  private static final int ST1_NOT_WRITEABLE = 0x02;
  private static final int ST1_NO_DATA = 0x04;
  private static final int ST1_OVERRUN = 0x10;
  private static final int ST1_CRC_ERROR = 0x20;
  private static final int ST1_EOF_CYLINDER = 0x80;

  private static final int ST2_MISSING_DM = 0x01;
  private static final int ST2_BAD_CYLINDER = 0x02;
  private static final int ST2_SCAN_NOT_SAT = 0x04;
  private static final int ST2_SCAN_HIT = 0x08;
  private static final int ST2_WRONG_CYLINDER = 0x10;
  private static final int ST2_DATA_ERROR = 0x20;
  private static final int ST2_CONTROL_MARK = 0x40;

  private static final int ST3_TR00 = 0x10;
  private static final int ST3_READY = 0x20;
  private static final int ST3_WRPROT = 0x40;

  private final Scheduler scheduler;
  private final Z80Clock clock;
  private final LongSupplier processorSpeed;
  private final Task fdcEvent;
  private final Task headEvent;
  private final Task timeoutEvent;

  public Fdd currentDrive;
  /** Four logical units; the +3 exposes only two physical ones, doubled via US0. */
  public final Fdd[] drive = new Fdd[4];

  private final Type type;
  private final Rate rate;

  private int stpRate;
  private int hutTime;
  private int hldTime;
  private boolean nonDma;
  /** Guarantees at least one sector transfers even if EOT is already below R. */
  private boolean firstRw;

  private Intrq intrq = Intrq.NONE;
  private State state = State.CMD;

  private int idTrack;
  private int idHead;
  private int idSector;
  /** N: 0=128, 1=256, 2=512, 3=1024 bytes per sector. */
  private int idLength;
  private int sectorLength;
  private boolean ddam;
  /** Revolutions left before giving up an ID or data search. */
  private int rev;
  private boolean headLoaded;
  private boolean readingId;
  private AmType idMark = AmType.NONE;

  /** Speedlock detector state: last sector re-read and how often; -1 turns it off. */
  private int lastSectorRead;
  public int speedlock;

  private int dataOffset;
  private int cycle;
  private boolean delData;
  private boolean mt;
  private boolean mf;
  private boolean sk;
  private int hd;
  private int us;
  private final int[] pcn = new int[4];
  private final int[] ncn = new int[4];
  private final int[] rec = new int[4];
  private final int[] seek = new int[4];
  private final int[] seekAge = new int[4];
  private int rlen;
  private Scan scan = Scan.EQ;

  private Cmd cmd = COMMANDS[COMMANDS.length - 1];

  private int commandRegister;
  private final int[] dataRegister = new int[9];
  private int mainStatus;
  private final int[] statusRegister = new int[4];
  private final int[] senseIntRes = new int[2];
  private int crc;

  public UpdFdc(Type type, Rate rate, Scheduler scheduler, Z80Clock clock, LongSupplier processorSpeed) {
    this.type = type;
    this.rate = rate;
    this.scheduler = scheduler;
    this.clock = clock;
    this.processorSpeed = processorSpeed;
    fdcEvent = scheduler.register(new FdcEvent());
    headEvent = scheduler.register(new HeadEvent());
    timeoutEvent = scheduler.register(new TimeoutEvent());
    speedlock = 0;
    masterReset();
  }

  private long now() {
    return clock.getTStates();
  }

  private long ms(long millis) {
    return processorSpeed.getAsLong() * millis / 1000;
  }

  private void after(long tstates, Task task) {
    scheduler.schedule(task, now() + tstates);
  }

  public Intrq intrq() {
    return intrq;
  }

  public void masterReset() {
    currentDrive = drive[0];

    // The +3 wires drives 2 and 3 as 0 and 1 on the same US0 pin, so selection must go by
    // the drive object, not by array index.
    for (Fdd d : drive) {
      if (d != null) {
        d.select(d == currentDrive);
      }
    }

    mainStatus = MAIN_DATAREQ;
    for (int i = 0; i < 4; i++) {
      statusRegister[i] = pcn[i] = seek[i] = seekAge[i] = 0;
    }
    stpRate = 16;
    hutTime = 240;
    hldTime = 254;
    nonDma = true;
    headLoaded = false;
    intrq = Intrq.NONE;
    state = State.CMD;
    cycle = 0;
    lastSectorRead = 0;
    readingId = false;
    // A disabled Speedlock hack (-1) stays disabled across a reset.
    if (speedlock != -1) {
      speedlock = 0;
    }
  }

  private void cmdIdentify() {
    Cmd found = COMMANDS[COMMANDS.length - 1];
    for (Cmd candidate : COMMANDS) {
      if (candidate.id() == CmdId.INVALID || (commandRegister & candidate.mask()) == candidate.value()) {
        found = candidate;
        break;
      }
    }
    mt = (commandRegister & 0x80) != 0;
    mf = (commandRegister & 0x40) != 0;
    sk = (commandRegister & 0x20) != 0;
    cmd = found;
  }

  private void crcPreset() {
    crc = 0xffff;
  }

  private void crcAdd(Fdd d) {
    crc = Crc.fdc(crc, d.data & 0xff);
  }

  /** Reads the next ID field. 0 = found, 1 = found with bad CRC, 2 = none found. */
  private int readId() {
    Fdd d = currentDrive;
    statusRegister[1] &= ~(ST1_CRC_ERROR | ST1_MISSING_AM | ST1_NO_DATA);
    idMark = AmType.NONE;
    int at = rev;
    while (at == rev && d.ready) {
      d.readData();
      if (d.index) rev--;
      crcPreset();
      if (mf) {                      // MFM
        if (d.data == 0xffa1) {
          crcAdd(d);
          d.readData();
          crcAdd(d);
          if (d.index) rev--;
          if (d.data != 0xffa1) continue;
          d.readData();
          crcAdd(d);
          if (d.index) rev--;
          if (d.data != 0xffa1) continue;
        } else {                          // 0xa1 needs its clock bit set to be a real mark
          continue;
        }
      }
      d.readData();
      if (d.index) rev--;
      if (mf) {
        if (d.data != 0x00fe) continue;
      } else {                            // FM
        if (d.data != 0xfffe) continue;
      }
      crcAdd(d);
      d.readData();
      crcAdd(d);
      if (d.index) rev--;
      idTrack = d.data;
      d.readData();
      crcAdd(d);
      if (d.index) rev--;
      idHead = d.data;
      d.readData();
      crcAdd(d);
      if (d.index) rev--;
      idSector = d.data;
      d.readData();
      crcAdd(d);
      if (d.index) rev--;
      idLength = Math.min(d.data, MAX_SIZE_CODE);
      sectorLength = 0x80 << idLength;
      d.readData();
      crcAdd(d);
      if (d.index) rev--;
      d.readData();
      crcAdd(d);
      if (d.index) rev--;

      idMark = AmType.ID;
      if (crc != 0x0000) {
        statusRegister[1] |= ST1_CRC_ERROR | ST1_NO_DATA;
        return 1;
      }
      return 0;
    }
    if (!d.ready) rev = 0;
    statusRegister[1] |= ST1_MISSING_AM | ST1_NO_DATA;
    return 2;
  }

  /** 0 if the data mark was found, 1 otherwise; also sets {@code ddam}. */
  private int readDatamark() {
    Fdd d = currentDrive;
    int i;
    if (mf) {                        // MFM
      for (i = 40; i > 0; i--) {
        d.readData();
        if (d.data == 0x4e) continue;     // still in the gap
        if (d.data == 0x00) break;        // sync zeros found
        statusRegister[2] |= ST2_MISSING_DM;
        return 1;
      }
      for (; i > 0; i--) {
        crcPreset();
        d.readData();
        crcAdd(d);
        if (d.data == 0x00) continue;
        if (d.data == 0xffa1) break;      // sync mark found
        statusRegister[2] |= ST2_MISSING_DM;
        return 1;
      }
      for (i = d.data == 0xffa1 ? 2 : 3; i > 0; i--) {
        d.readData();
        crcAdd(d);
        if (d.data != 0xffa1) {
          statusRegister[2] |= ST2_MISSING_DM;
          return 1;
        }
      }
      d.readData();
      crcAdd(d);
      if (d.data < 0x00f8 || d.data > 0x00fb) {
        statusRegister[2] |= ST2_MISSING_DM;
        return 1;
      }
      ddam = d.data != 0x00fb;
      return 0;
    }
    for (i = 30; i > 0; i--) {            // FM
      d.readData();
      if (d.data == 0xff) continue;
      if (d.data == 0x00) break;
      statusRegister[2] |= ST2_MISSING_DM;
      return 1;
    }
    for (; i > 0; i--) {
      crcPreset();
      d.readData();
      crcAdd(d);
      if (d.data == 0x00) continue;
      if (d.data >= 0xfff8 && d.data <= 0xfffb) break;
      statusRegister[2] |= ST2_MISSING_DM;
      return 1;
    }
    if (i == 0) {
      d.readData();
      crcAdd(d);
      if (d.data < 0xfff8 || d.data > 0xfffb) {
        statusRegister[2] |= ST2_MISSING_DM;
        return 1;
      }
    }
    ddam = d.data != 0x00fb;
    return 0;
  }

  /** Looks for the requested sector's ID. 0 ok, 1 bad CRC, 2 no ID seen, 3 wrong ID. */
  private int seekId() {
    statusRegister[2] &= ~(ST2_WRONG_CYLINDER | ST2_BAD_CYLINDER);
    int r = readId();
    if (r != 0) return r;

    if (idTrack != dataRegister[1]) {
      statusRegister[2] |= ST2_WRONG_CYLINDER;
      if (idTrack == 0xff) {
        statusRegister[2] |= ST2_BAD_CYLINDER;
      }
      return 3;
    }
    if (idSector == dataRegister[3] && idHead == dataRegister[2]) {
      if (idLength != dataRegister[4]) {
        statusRegister[1] |= ST1_NO_DATA;
        return 3;
      }
      return 0;
    }
    statusRegister[1] |= ST1_NO_DATA;
    return 3;
  }

  private void cmdResult() {
    cycle = cmd.resLength();
    mainStatus &= ~MAIN_EXECUTION;
    mainStatus |= MAIN_DATAREQ;
    if (cycle > 0) {
      state = State.RES;
      intrq = Intrq.RESULT;
      mainStatus |= MAIN_DATA_READ;
    } else {
      state = State.CMD;
      mainStatus &= ~MAIN_DATADIR;
      mainStatus &= ~MAIN_BUSY;
    }
    scheduler.cancel(timeoutEvent);
    if (headLoaded && cmd.id().ordinal() <= CmdId.READ_ID.ordinal()) {
      after(ms(hutTime), headEvent);
    }
  }

  private void seekStep(boolean start) {
    int i;
    if (start) {
      i = us;
      if ((mainStatus & (1 << i)) != 0) return;     // that drive already seeking
      // Bit stays set until a Sense Interrupt reads the result.
      mainStatus |= 1 << i;
    } else {
      i = 0;
      for (int j = 1; j < 4; j++) {
        if (seekAge[j] > seekAge[i]) i = j;
      }
      if (seek[i] == 0 || seek[i] >= 4) return;
    }

    Fdd d = drive[i];

    if (pcn[i] == ncn[i] && seek[i] == 2 && !d.tr00) {   // 77 steps and still not at track 0
      seek[i] = 5;
      seekAge[i] = 0;
      intrq = Intrq.SEEK;
      statusRegister[0] |= ST0_EQUIP_CHECK;
      mainStatus &= ~(1 << i);
      return;
    }

    if (pcn[i] == ncn[i] || (seek[i] == 2 && d.tr00)) {  // target cylinder reached
      if (seek[i] == 2) pcn[i] = 0;
      seek[i] = 4;
      seekAge[i] = 0;
      intrq = Intrq.SEEK;
      mainStatus &= ~(1 << i);
      return;
    }

    if (!d.ready) {
      if (seek[i] == 2) pcn[i] = rec[i] - (77 - pcn[i]);
      seek[i] = 6;
      seekAge[i] = 0;
      intrq = Intrq.READY;
      mainStatus &= ~(1 << i);
      return;
    }

    if (pcn[i] != ncn[i]) {
      d.step(pcn[i] > ncn[i] ? Fdd.Dir.OUT : Fdd.Dir.IN);
      pcn[i] += pcn[i] > ncn[i] ? -1 : 1;
      for (int j = 0; j < 4; j++) {
        if (seekAge[j] > 0) seekAge[j]++;
      }
      seekAge[i] = 1;
      after(ms(stpRate), fdcEvent);
    }
  }

  /** Elapsed head travel since {@code from}, in twentieths of a revolution. */
  private long sinceStartOfSearch(int from) {
    Disk disk = currentDrive.disk;
    return disk.cBpt != 0 ? (long) (disk.i - from) * 200 / disk.cBpt : 200;
  }

  private int searchStart() {
    Disk disk = currentDrive.disk;
    return disk.i >= disk.cBpt ? 0 : disk.i;
  }

  private void startReadId() {
    if (!readingId) {
      rev = 2;
      readingId = true;
    }
    if (rev != 0) {
      int from = searchStart();
      if (readId() != 2) rev = 0;
      long waited = sinceStartOfSearch(from);
      if (waited > 0) {
        after(ms(waited), fdcEvent);
        return;
      }
    }
    readingId = false;
    if (idMark != AmType.NONE) {
      dataRegister[1] = idTrack;
      dataRegister[2] = idHead;
      dataRegister[3] = idSector;
      dataRegister[4] = idLength;
    }
    if (idMark != AmType.ID || (statusRegister[1] & ST1_CRC_ERROR) != 0) {
      statusRegister[0] |= ST0_INT_ABNORM;
    }
    intrq = Intrq.RESULT;
    cmdResult();
  }

  private void startReadDiag() {
    if (!readingId) {
      rev = 2;
      readingId = true;
    }
    if (rev != 0) {
      int from = searchStart();
      if (readId() != 2) rev = 0;
      long waited = sinceStartOfSearch(from);
      if (waited > 0) {
        after(ms(waited), fdcEvent);
        return;
      }
    }
    readingId = false;
    if (idMark == AmType.NONE) {
      statusRegister[0] |= ST0_INT_ABNORM;
      statusRegister[1] |= ST1_EOF_CYLINDER;
      abortReadDiag();
      return;
    }
    if (idTrack != dataRegister[1] || idSector != dataRegister[3] || dataRegister[2] != idHead) {
      statusRegister[1] |= ST1_NO_DATA;
    }
    if (idTrack != dataRegister[1]) {
      statusRegister[2] |= ST2_WRONG_CYLINDER;
      if (idTrack == 0xff) {
        statusRegister[2] |= ST2_BAD_CYLINDER;
      }
    }
    if (readDatamark() > 0) {
      statusRegister[0] |= ST0_INT_ABNORM;
      abortReadDiag();
      return;
    }
    mainStatus |= MAIN_DATAREQ | MAIN_DATA_READ;
    dataOffset = 0;
    scheduler.cancel(timeoutEvent);
    after(twoRevolutions(), timeoutEvent);
  }

  private void abortReadDiag() {
    state = State.RES;
    cycle = cmd.resLength();
    mainStatus &= ~MAIN_EXECUTION;
    intrq = Intrq.RESULT;
    cmdResult();
  }

  /** Timeout for the host to service a data byte: two disk revolutions. */
  private long twoRevolutions() {
    return processorSpeed.getAsLong() * 4 / 10;
  }

  /** Gap time between formatted sectors: a tenth of a revolution. */
  private long tenthOfRevolution() {
    return processorSpeed.getAsLong() * 2 / 100;
  }

  private void startReadData() {
    while (true) {
      if (firstRw || readingId || dataRegister[5] > dataRegister[3]) {
        if (!readingId) {
          if (!firstRw) {
            dataRegister[3] = (dataRegister[3] + 1) & 0xff;
          }
          firstRw = false;
          rev = 2;
          readingId = true;
        }
        while (rev != 0) {
          int from = searchStart();
          if (seekId() == 0) {
            rev = 0;
          } else {
            idMark = AmType.NONE;
          }
          long waited = sinceStartOfSearch(from);
          if (waited > 0) {
            after(ms(waited), fdcEvent);
            return;
          }
        }
        readingId = false;
        if (idMark == AmType.NONE) {
          statusRegister[0] |= ST0_INT_ABNORM;
          abortReadData();
          return;
        }
        if (readDatamark() > 0) {
          statusRegister[0] |= ST0_INT_ABNORM;
          abortReadData();
          return;
        }
        if (ddam != delData) {
          statusRegister[2] |= ST2_CONTROL_MARK;
          if (sk) {
            dataRegister[3] = (dataRegister[3] + 1) & 0xff;
            continue;                     // sk set: pass over a deleted sector
          }
        }
      } else {
        if (mt) {
          dataRegister[1] = (dataRegister[1] + 1) & 0xff;
          dataRegister[3] = 1;
          continue;                       // mt set: carries on into the next cylinder
        }
        abortReadData();
        return;
      }
      break;
    }
    mainStatus |= MAIN_DATAREQ;
    mainStatus |= cmd.id() != CmdId.SCAN ? MAIN_DATA_READ : MAIN_DATA_WRITE;
    dataOffset = 0;
    scheduler.cancel(timeoutEvent);
    after(twoRevolutions(), timeoutEvent);
  }

  /** Ends a read; sets end-of-cylinder once the last (EOT) sector is read whole with no TC. */
  private void abortReadData() {
    state = State.RES;
    cycle = cmd.resLength();
    if (statusRegister[0] == 0 && statusRegister[1] == 0) {
      statusRegister[0] |= ST0_INT_ABNORM;
      statusRegister[1] |= ST1_EOF_CYLINDER;
    }
    if ((statusRegister[0] & (ST0_INT_ABNORM | ST0_INT_READY)) == 0) {
      dataRegister[1] = (dataRegister[1] + 1) & 0xff;
      dataRegister[3] = 1;
    }
    mainStatus &= ~MAIN_EXECUTION;
    intrq = Intrq.RESULT;
    cmdResult();
  }

  private void startWriteData() {
    Fdd d = currentDrive;
    while (true) {
      if (firstRw || readingId || dataRegister[5] > dataRegister[3]) {
        if (!readingId) {
          if (!firstRw) {
            dataRegister[3] = (dataRegister[3] + 1) & 0xff;
          }
          firstRw = false;
          rev = 2;
          readingId = true;
        }
        while (rev != 0) {
          int from = searchStart();
          if (seekId() == 0) {
            rev = 0;
          } else {
            idMark = AmType.NONE;
          }
          long waited = sinceStartOfSearch(from);
          if (waited > 0) {
            after(ms(waited), fdcEvent);
            return;
          }
        }
        readingId = false;
        if (idMark == AmType.NONE) {
          statusRegister[0] |= ST0_INT_ABNORM;
          abortWriteData();
          return;
        }

        for (int i = 11; i > 0; i--) {    // gap3 is 22 bytes in MFM, 11 in FM
          d.readData();
        }
        if (mf) {
          for (int i = 11; i > 0; i--) {
            d.readData();
          }
        }
        for (int i = mf ? 12 : 6; i > 0; i--) {
          d.writeData(0x00);
        }
        crcPreset();
        if (mf) {
          for (int i = 3; i > 0; i--) {   // sync mark is three of these
            d.writeData(0xffa1);
            crcAdd(d);
          }
        }
        d.writeData((delData ? 0x00f8 : 0x00fb) | (mf ? 0x0000 : 0xff00));
        crcAdd(d);
      } else {
        dataRegister[1] = (dataRegister[1] + 1) & 0xff;
        dataRegister[3] = 1;
        if (mt) continue;
        abortWriteData();
        return;
      }
      break;
    }
    mainStatus |= MAIN_DATAREQ | MAIN_DATA_WRITE;
    dataOffset = 0;
    scheduler.cancel(timeoutEvent);
    after(twoRevolutions(), timeoutEvent);
  }

  private void abortWriteData() {
    state = State.RES;
    cycle = cmd.resLength();
    statusRegister[0] |= ST0_INT_ABNORM;
    statusRegister[1] |= ST1_EOF_CYLINDER;
    mainStatus &= ~MAIN_EXECUTION;
    intrq = Intrq.RESULT;
    cmdResult();
  }

  /** Writes the index address mark and the gap that opens a freshly formatted track. */
  private void startWriteId() {
    Fdd d = currentDrive;
    for (int i = 40; i > 0; i--) {
      d.writeData(mf ? 0x4e : 0xff);
    }
    if (mf) {
      for (int i = 40; i > 0; i--) {
        d.writeData(0x4e);
      }
    }
    for (int i = mf ? 12 : 6; i > 0; i--) {
      d.writeData(0x00);
    }
    crcPreset();
    if (mf) {
      for (int i = 3; i > 0; i--) {       // index mark's sync, three of these
        d.writeData(0xffc2);
      }
    }
    d.writeData(0x00fc | (mf ? 0x0000 : 0xff00));

    for (int i = 26; i > 0; i--) {       // gap1, right after the index mark
      d.writeData(mf ? 0x4e : 0xff);
    }
    if (mf) {
      for (int i = 24; i > 0; i--) {
        d.writeData(0x4e);
      }
    }

    mainStatus |= MAIN_DATAREQ | MAIN_DATA_WRITE;
    dataOffset = 0;
    after(tenthOfRevolution(), timeoutEvent);
  }

  private void loadHead() {
    scheduler.cancel(headEvent);
    if (headLoaded) {
      switch (cmd.id()) {
        case READ_DATA, SCAN -> startReadData();
        case READ_ID -> startReadId();
        case READ_DIAG -> {
          currentDrive.waitIndexHole();   // READ TRACK always begins at the index
          startReadDiag();
        }
        case WRITE_DATA -> startWriteData();
        case WRITE_ID -> {
          currentDrive.waitIndexHole();   // FORMAT begins at the index too
          startWriteId();
        }
        default -> { }
      }
    } else {
      currentDrive.headLoad(true);
      headLoaded = true;
      after(ms(hldTime), fdcEvent);
    }
  }

  private final class TimeoutEvent extends Task {
    public void run(long due) {
      statusRegister[0] |= ST0_INT_ABNORM;
      statusRegister[1] |= ST1_OVERRUN;
      cmdResult();
    }
  }

  private final class HeadEvent extends Task {
    public void run(long due) {
      currentDrive.headLoad(false);
      headLoaded = false;
    }
  }

  private final class FdcEvent extends Task {
    public void run(long due) {
      if (readingId) {
        switch (cmd.id()) {
          case READ_DATA, SCAN -> startReadData();
          case READ_ID -> startReadId();
          case READ_DIAG -> startReadDiag();
          case WRITE_DATA -> startWriteData();
          default -> { }
        }
      } else if ((mainStatus & 0x03) != 0) {          // bits 0-1: drives still seeking
        seekStep(false);
      } else {
        switch (cmd.id()) {
          case READ_DATA, SCAN -> startReadData();
          case READ_ID -> startReadId();
          case READ_DIAG -> {
            currentDrive.waitIndexHole();
            startReadDiag();
          }
          case WRITE_DATA -> startWriteData();
          case WRITE_ID -> {
            currentDrive.waitIndexHole();
            startWriteId();
          }
          default -> { }
        }
      }
    }
  }

  public int readStatus() {
    return mainStatus & 0xff;
  }

  public int readData() {
    Fdd d = currentDrive;

    if ((mainStatus & MAIN_DATAREQ) == 0 || (mainStatus & MAIN_DATA_READ) == 0) {
      return 0xff;
    }

    if (state == State.EXE) {                         // still transferring a READ DATA/DIAG
      dataOffset++;
      d.readData();
      crcAdd(d);

      // Speedlock loaders reread one sector hunting for weak bits; vary the byte returned
      // each time unless the drive already models weak reads itself.
      if (speedlock > 0 && !d.doReadWeak) {
        if (dataOffset < 64 && d.data != 0xe5) {
          speedlock = 2;                              // the variant with byte 0xe5 removed
        } else if ((speedlock > 1 || dataOffset < 64) && dataOffset % 29 == 0) {
          d.data ^= dataOffset;
          crcAdd(d);
        }
      }

      int r = d.data & 0xff;
      if (dataOffset == rlen) {                       // rlen is the transfer length, may be under a full sector
        while (dataOffset < sectorLength) {
          d.readData();
          crcAdd(d);
          dataOffset++;
        }
      }
      if ((cmd.id() == CmdId.READ_DIAG || cmd.id() == CmdId.READ_DATA) && dataOffset == sectorLength) {
        d.readData();
        crcAdd(d);
        d.readData();
        crcAdd(d);
        if (crc != 0x0000) {
          statusRegister[2] |= ST2_DATA_ERROR;
          statusRegister[1] |= ST1_CRC_ERROR;
          if (cmd.id() == CmdId.READ_DATA) {          // READ DIAG keeps going past a CRC error
            statusRegister[0] |= ST0_INT_ABNORM;
            cmdResult();
            return r;
          }
        }
        if (cmd.id() == CmdId.READ_DATA) {
          if (ddam != delData) {                      // deletion mark does not match what was requested
            if (dataRegister[5] > dataRegister[3]) {
              statusRegister[0] |= ST0_INT_ABNORM;
            }
            cmdResult();
            return r;
          }
          rev = 2;
          mainStatus &= ~MAIN_DATAREQ;
          startReadData();
        } else {                                      // READ_DIAG branch
          dataRegister[3] = (dataRegister[3] + 1) & 0xff;
          dataRegister[5] = (dataRegister[5] - 1) & 0xff;
          if (dataRegister[5] == 0) {
            cmdResult();
            return r;
          }
          mainStatus &= ~MAIN_DATAREQ;
          startReadDiag();
        }
      }
      return r;
    }

    if (state != State.RES) {
      return 0xff;
    }

    int r;
    if (cmd.id() == CmdId.SENSE_DRIVE) {
      r = statusRegister[3];
    } else if (cmd.id() == CmdId.SENSE_INT) {
      r = senseIntRes[cmd.resLength() - cycle];
    } else if (cmd.resLength() - cycle < 3) {
      r = statusRegister[cmd.resLength() - cycle];
    } else {
      r = dataRegister[cmd.resLength() - cycle - 2];
    }
    cycle--;
    if (cycle == 0) {
      state = State.CMD;
      mainStatus |= MAIN_DATAREQ;
      mainStatus &= ~MAIN_DATADIR;
      mainStatus &= ~MAIN_BUSY;
      if (intrq.ordinal() < Intrq.READY.ordinal()) {
        intrq = Intrq.NONE;
      }
    }
    return r & 0xff;
  }

  public void writeData(int data) {
    data &= 0xff;
    boolean terminated = false;
    Fdd d;

    if ((mainStatus & MAIN_DATAREQ) == 0 || (mainStatus & MAIN_DATA_READ) != 0) {
      return;
    }

    if ((mainStatus & MAIN_BUSY) != 0 && state == State.EXE) {   // still transferring a WRITE/FORMAT/SCAN
      d = currentDrive;
      if (cmd.id() == CmdId.WRITE_ID) {                          // WRITE_ID is the FORMAT TRACK command
        dataRegister[dataOffset + 5] = data;
        dataOffset++;
        if (dataOffset == 4) {                                   // cylinder, head, sector, size all in
          scheduler.cancel(timeoutEvent);

          for (int i = mf ? 12 : 6; i > 0; i--) {
            d.writeData(0x00);
          }
          crcPreset();
          if (mf) {
            for (int i = 3; i > 0; i--) {
              d.writeData(0xffa1);
              crcAdd(d);
            }
          }
          d.writeData(0x00fe | (mf ? 0x0000 : 0xff00));          // 0xfe: ID address mark
          crcAdd(d);
          for (int i = 0; i < 4; i++) {
            d.writeData(dataRegister[i + 5]);
            crcAdd(d);
          }
          d.writeData(crc >> 8);
          d.writeData(crc & 0xff);

          for (int i = 11; i > 0; i--) {
            d.writeData(mf ? 0x4e : 0xff);
          }
          if (mf) {
            for (int i = 11; i > 0; i--) {
              d.writeData(0x4e);
            }
          }
          for (int i = mf ? 12 : 6; i > 0; i--) {
            d.writeData(0x00);
          }
          crcPreset();
          if (mf) {
            for (int i = 3; i > 0; i--) {
              d.writeData(0xffa1);
              crcAdd(d);
            }
          }
          d.writeData(0x00fb | (mf ? 0x0000 : 0xff00));          // 0xfb: data address mark
          crcAdd(d);

          for (int i = rlen; i > 0; i--) {
            d.writeData(dataRegister[4]);                        // filler byte given by the FORMAT command
            crcAdd(d);
          }
          d.writeData(crc >> 8);
          d.writeData(crc & 0xff);

          for (int i = dataRegister[3]; i > 0; i--) {
            d.writeData(mf ? 0x4e : 0xff);
          }
          dataOffset = 0;
          dataRegister[2] = (dataRegister[2] - 1) & 0xff;        // sectors-per-track countdown
        }
        if (dataRegister[2] == 0) {                              // sector countdown reached zero
          while (!d.index) {                                     // fill out to the next index pulse
            d.writeData(mf ? 0x4e : 0xff);
          }
          state = State.RES;
          cycle = cmd.resLength();
          mainStatus &= ~MAIN_EXECUTION;
          intrq = Intrq.RESULT;
          cmdResult();
          return;
        }
        after(tenthOfRevolution(), timeoutEvent);
        return;
      } else if (cmd.id() == CmdId.WRITE_DATA) {
        dataOffset++;
        d.writeData(data);
        crcAdd(d);

        if (dataOffset == rlen) {                                // host sent fewer than a full sector
          d.data = 0x00;
          while (dataOffset < sectorLength) {                    // pad out with zeros
            d.readData();
            crcAdd(d);
            dataOffset++;
          }
        }
        if (dataOffset == sectorLength) {
          d.writeData(crc >> 8);
          d.writeData(crc & 0xff);
          mainStatus &= ~MAIN_DATAREQ;
          startWriteData();
        }
        return;
      } else {                                                   // only SCAN reaches here
        dataOffset++;
        d.readData();
        crcAdd(d);
        if (dataOffset == 1 && d.data == data) {
          statusRegister[2] |= ST2_SCAN_HIT;
        }
        if (d.data != data) {
          statusRegister[2] &= ~ST2_SCAN_HIT;
        }
        if ((scan == Scan.EQ && d.data != data)
            || (scan == Scan.LO && d.data > data)
            || (scan == Scan.HI && d.data < data)) {
          statusRegister[2] |= ST2_SCAN_NOT_SAT;
        }
        if (dataOffset == sectorLength) {
          d.readData();
          crcAdd(d);
          d.readData();
          crcAdd(d);
          if (crc != 0x0000) {
            statusRegister[2] |= ST2_DATA_ERROR;
            statusRegister[1] |= ST1_CRC_ERROR;
          }
          dataRegister[3] = (dataRegister[3] + dataRegister[7]) & 0xff;
          if (ddam != delData) {
            if (dataRegister[5] >= dataRegister[3]) {
              statusRegister[0] |= ST0_INT_ABNORM;
            }
            cmdResult();
            return;
          }
          if ((statusRegister[2] & ST2_SCAN_HIT) != 0 || (statusRegister[2] & ST2_SCAN_NOT_SAT) == 0) {
            cmdResult();
            return;
          }
          rev = 2;
          mainStatus &= ~MAIN_DATAREQ;
          startReadData();
        }
        return;
      }
    }

    if (cycle == 0) {
      commandRegister = data;
      cmdIdentify();
      mainStatus |= MAIN_BUSY;
      // Some clones require a Sense Interrupt after every seek even with none pending;
      // the uPD765 must reject it there, or certain loaders desync.
      if (intrq == Intrq.NONE && cmd.id() == CmdId.SENSE_INT) {
        commandRegister = 0x00;
        cmdIdentify();
      }
    } else {
      dataRegister[cycle - 1] = data;
    }

    if (cycle >= cmd.cmdLength()) {                              // full command received
      state = State.EXE;
      mainStatus &= ~MAIN_DATAREQ;
      if (nonDma) {                                         // DMA mode is not modelled
        mainStatus |= MAIN_EXECUTION;
      }

      if (cmd.id() != CmdId.SENSE_INT && cmd.id() != CmdId.SPECIFY
          && cmd.id() != CmdId.VERSION && cmd.id() != CmdId.INVALID) {
        us = dataRegister[0] & 0x03;
        if (currentDrive != drive[us]) {
          if (currentDrive != null) currentDrive.select(false);
          currentDrive = drive[us];
          if (currentDrive != null) currentDrive.select(true);
        }
        hd = (dataRegister[0] & 0x04) >> 2;
        currentDrive.setHead(hd);

        if (cmd.id() == CmdId.READ_DATA || cmd.id() == CmdId.WRITE_DATA) {
          delData = (commandRegister & 0x08) != 0;
          sk = (dataRegister[0] & 0x20) != 0;
        }
      }

      // Busy is cleared once the seek command itself is read; up to four drives can then be
      // seeking together, one step issued per drive per round.
      if (cmd.id() == CmdId.RECALIBRATE || cmd.id() == CmdId.SEEK || cmd.id() == CmdId.SPECIFY) {
        mainStatus &= ~MAIN_BUSY;
      }

      if (cmd.id().ordinal() < CmdId.SENSE_INT.ordinal()) {
        if (cmd.id().ordinal() < CmdId.RECALIBRATE.ordinal()) {
          statusRegister[0] = statusRegister[1] = statusRegister[2] = 0x00;
        }
        statusRegister[0] = us + (hd << 2);
      }

      d = currentDrive;
      switch (cmd.id()) {
        case INVALID:
          statusRegister[0] = 0x80;
          break;
        case VERSION:
          statusRegister[0] = type == Type.UPD765B ? 0x90 : 0x80;
          break;
        case SPECIFY:
          stpRate = 0x10 - (dataRegister[0] >> 4);
          hutTime = (dataRegister[0] & 0x0f) << 4;
          if (hutTime == 0) hutTime = 128;
          hldTime = dataRegister[1] & 0xfe;
          if (hldTime == 0) hldTime = 256;
          nonDma = (dataRegister[1] & 0x01) != 0;
          // A 4MHz clock (mini-floppy) doubles every SPECIFY interval.
          if (rate == Rate.MHZ4) {
            stpRate *= 2;
            hutTime *= 2;
            hldTime *= 2;
          }
          state = State.CMD;                                     // SPECIFY has no result bytes
          break;
        case SENSE_DRIVE:
          statusRegister[3] = us + (hd << 2);
          // On the +3, the double-sided line and write-protect share one signal.
          statusRegister[3] |= d.wrprot ? ST3_WRPROT : 0;
          statusRegister[3] |= d.tr00 ? ST3_TR00 : 0;
          statusRegister[3] |= d.ready ? ST3_READY : 0;
          break;
        case SENSE_INT:
          for (int i = 0; i < 4; i++) {
            if (seek[i] >= 4) {
              statusRegister[0] &= ~0xc0;                        // clears the abnormal/ready bits
              statusRegister[0] |= ST0_SEEK_END;
              if (seek[i] == 5) {
                statusRegister[0] |= ST0_INT_ABNORM;
              } else if (seek[i] == 6) {
                statusRegister[0] |= ST0_INT_READY | ST0_NOT_READY;
              }
              seek[i] = seekAge[i] = 0;
              senseIntRes[0] = statusRegister[0] & 0xfb;         // bit 2 (head) is forced to 0
              senseIntRes[1] = pcn[i];
              break;                                            // reports only the first pending drive
            }
          }
          if (seek[0] < 4 && seek[1] < 4 && seek[2] < 4 && seek[3] < 4) {
            intrq = Intrq.NONE;
          }
          break;
        case RECALIBRATE:
          if ((mainStatus & (1 << us)) != 0) break;              // that drive is already seeking
          rec[us] = pcn[us];
          pcn[us] = 77;
          dataRegister[1] = 0x00;                                // RECALIBRATE always targets track 0
          ncn[us] = dataRegister[1];
          seek[us] = 2;
          seekStep(true);
          break;
        case SEEK:
          if ((mainStatus & (1 << us)) != 0) break;
          ncn[us] = dataRegister[1];
          seek[us] = 1;
          seekStep(true);
          break;
        case READ_ID:
          loadHead();
          return;
        case READ_DATA:
          // Tracks repeats of the same read, the signature of a Speedlock weak-sector check.
          if (speedlock != -1 && !d.doReadWeak) {
            int asked = (dataRegister[2] & 0x01) + (dataRegister[1] << 1) + (dataRegister[3] << 8);
            if (dataRegister[3] == dataRegister[5] && asked == 0x200) {
              if (asked == lastSectorRead) {
                speedlock++;
              } else {
                speedlock = 0;
                lastSectorRead = asked;
              }
            } else {
              lastSectorRead = speedlock = 0;
            }
          }
          rlen = 0x80 << Math.min(dataRegister[4], MAX_SIZE_CODE);
          if (dataRegister[4] == 0 && dataRegister[7] < 128) {
            rlen = dataRegister[7];
          }
          firstRw = true;                                           // forces the first ID search
          loadHead();
          return;
        case READ_DIAG:                                          // READ_DIAG is the READ TRACK command
          rlen = 0x80 << Math.min(dataRegister[4], MAX_SIZE_CODE);
          if (dataRegister[4] == 0 && dataRegister[7] < 128) {
            rlen = dataRegister[7];
          }
          loadHead();
          return;
        case WRITE_DATA:
          if (d.wrprot) {
            statusRegister[1] |= ST1_NOT_WRITEABLE;
            statusRegister[0] |= ST0_INT_ABNORM;
            terminated = true;
            break;
          }
          rlen = 0x80 << Math.min(dataRegister[4], MAX_SIZE_CODE);
          if (dataRegister[4] == 0 && dataRegister[7] < 128) {
            rlen = dataRegister[7];
          }
          firstRw = true;                                           // forces the first ID search
          loadHead();
          return;
        case WRITE_ID:                                           // WRITE_ID is the FORMAT TRACK command
          if (d.wrprot) {
            statusRegister[1] |= ST1_NOT_WRITEABLE;
            statusRegister[0] |= ST0_INT_ABNORM;
            terminated = true;
            break;
          }
          rlen = 0x80 << Math.min(dataRegister[1], MAX_SIZE_CODE);
          loadHead();
          return;
        case SCAN:
          int kind = (commandRegister & 0x0c) >> 2;
          scan = kind == 0 ? Scan.EQ : kind == 0x03 ? Scan.HI : Scan.LO;
          rlen = 0x80 << Math.min(dataRegister[4], MAX_SIZE_CODE);
          firstRw = true;
          if (dataRegister[4] == 0 && dataRegister[7] < 128) {
            rlen = dataRegister[7];
          }
          loadHead();
          return;
        default:
          break;
      }

      if (cmd.id().ordinal() < CmdId.READ_ID.ordinal() && !terminated) {   // this command has data to transfer
        mainStatus |= MAIN_DATAREQ;
        if (cmd.id().ordinal() < CmdId.WRITE_DATA.ordinal()) {
          mainStatus |= MAIN_DATA_READ;
        }
      } else {
        cmdResult();
      }
    } else {
      cycle++;
    }
  }
}
