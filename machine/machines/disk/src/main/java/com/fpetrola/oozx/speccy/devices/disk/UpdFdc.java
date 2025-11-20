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

package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.function.LongSupplier;

/**
 * The NEC uPD765 floppy controller, which is the +3's, as a program sees it through its two
 * registers and as the drive sees it: a byte at a time off the track, address marks and CRCs and
 * all. It drives four units, of which the +3 wires two twice over, and does its own stepping.
 * <p>
 * Everything that takes time on the real chip - a step, a
 * revolution, the head loading, a sector going by - is an event on the machine's clock, the way
 * {@link WdFdc} does it.
 */
public final class UpdFdc {

  public enum Type { UPD765A, UPD765B }

  /** The chip's own clock. At 4MHz every interval the SPECIFY command sets is doubled. */
  public enum Rate { MHZ4, MHZ8 }

  private enum Scan { EQ, LO, HI }

  /**
   * The commands, in the order they are declared, which is load-bearing: the chip decides what a
   * command does to the head, to the status registers and to the result phase by comparing against
   * READ_ID, RECALIBRATE, SENSE_INT and WRITE_DATA, and those comparisons are on this order.
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

  /** Matched in order against the command byte; the first that fits is the command. */
  private static final Cmd[] COMMANDS = {
      new Cmd(CmdId.READ_DATA, 0x1f, 0x06, 0x08, 0x07),
      new Cmd(CmdId.READ_DATA, 0x1f, 0x0c, 0x08, 0x07),   // deleted data
      new Cmd(CmdId.READ_DIAG, 0x9f, 0x02, 0x08, 0x07),
      new Cmd(CmdId.RECALIBRATE, 0xff, 0x07, 0x01, 0x00),
      new Cmd(CmdId.SEEK, 0xff, 0x0f, 0x02, 0x00),
      new Cmd(CmdId.WRITE_DATA, 0x3f, 0x05, 0x08, 0x07),
      new Cmd(CmdId.WRITE_DATA, 0x3f, 0x09, 0x08, 0x07),  // deleted data
      new Cmd(CmdId.WRITE_ID, 0xbf, 0x0d, 0x05, 0x07),
      new Cmd(CmdId.SCAN, 0x1f, 0x11, 0x08, 0x07),
      new Cmd(CmdId.SCAN, 0x1f, 0x19, 0x08, 0x07),        // low or equal
      new Cmd(CmdId.SCAN, 0x1f, 0x1d, 0x08, 0x07),        // high or equal
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
  /** The computer should read. */
  private static final int MAIN_DATA_READ = 0x40;
  /** The computer should write. */
  private static final int MAIN_DATA_WRITE = 0x00;
  private static final int MAIN_DATAREQ = 0x80;

  private static final int ST0_NOT_READY = 0x08;
  private static final int ST0_EQUIP_CHECK = 0x10;
  private static final int ST0_SEEK_END = 0x20;
  /** Abnormal termination. */
  private static final int ST0_INT_ABNORM = 0x40;
  /** The ready signal changed. */
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
  /** The uPD765 controls four drives; the +3 wires two of them twice, on the US0 pin alone. */
  public final Fdd[] drive = new Fdd[4];

  private final Type type;
  private final Rate rate;

  private int stpRate;
  private int hutTime;
  private int hldTime;
  private boolean nonDma;
  /** The first sector is always read or written, even when EOT is below R. */
  private boolean firstRw;

  private Intrq intrq = Intrq.NONE;
  private State state = State.CMD;

  private int idTrack;
  private int idHead;
  private int idSector;
  /** The sector length code: 0, 1, 2, 3. */
  private int idLength;
  private int sectorLength;
  private boolean ddam;
  /** How many revolutions are left to find what is being looked for. */
  private int rev;
  private boolean headLoaded;
  private boolean readingId;
  private AmType idMark = AmType.NONE;

  /** For the Speedlock hack: the sector it keeps asking for, and how many times. -1 disables it. */
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

    // Careful with mirrored drives: the +3 uses only the US0 pin, so drive 2 is drive 0 and
    // drive 3 is drive 1, and selecting by identity rather than by index is what keeps that right.
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
    // The disabled state of the Speedlock hack survives a reset.
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

  /**
   * Reads the next ID field into the id fields. Answers 0 when one was found, 1 when one was found
   * with a CRC error, and 2 when none was.
   */
  private int readId() {
    Fdd d = currentDrive;
    statusRegister[1] &= ~(ST1_CRC_ERROR | ST1_MISSING_AM | ST1_NO_DATA);
    idMark = AmType.NONE;
    int at = rev;
    while (at == rev && d.ready) {
      d.readData();
      if (d.index) rev--;
      crcPreset();
      if (mf) {                      // double density (MFM)
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
        } else {                          // no 0xa1 with a missing clock
          continue;
        }
      }
      d.readData();
      if (d.index) rev--;
      if (mf) {
        if (d.data != 0x00fe) continue;
      } else {                            // single density (FM)
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

  /** Reads the data address mark: 0 when found, 1 when not. */
  private int readDatamark() {
    Fdd d = currentDrive;
    int i;
    if (mf) {                        // double density (MFM)
      for (i = 40; i > 0; i--) {
        d.readData();
        if (d.data == 0x4e) continue;     // read the next one
        if (d.data == 0x00) break;        // go to PLL sync
        statusRegister[2] |= ST2_MISSING_DM;
        return 1;
      }
      for (; i > 0; i--) {
        crcPreset();
        d.readData();
        crcAdd(d);
        if (d.data == 0x00) continue;
        if (d.data == 0xffa1) break;      // got to the a1 mark
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
    for (i = 30; i > 0; i--) {            // single density (FM)
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

  /**
   * Seeks to the ID the command asked for. Answers 0 when found, 1 when the ID has a CRC error,
   * 2 when no ID was found at all and 3 when the one asked for was not there.
   */
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
      if ((mainStatus & (1 << i)) != 0) return;     // already seeking
      // Marked as seeking for the drive; a Sense Interrupt command clears it.
      mainStatus |= 1 << i;
    } else {
      i = 0;
      for (int j = 1; j < 4; j++) {
        if (seekAge[j] > seekAge[i]) i = j;
      }
      if (seek[i] == 0 || seek[i] >= 4) return;
    }

    Fdd d = drive[i];

    if (pcn[i] == ncn[i] && seek[i] == 2 && !d.tr00) {   // recalibrate failed
      seek[i] = 5;
      seekAge[i] = 0;
      intrq = Intrq.SEEK;
      statusRegister[0] |= ST0_EQUIP_CHECK;
      mainStatus &= ~(1 << i);
      return;
    }

    if (pcn[i] == ncn[i] || (seek[i] == 2 && d.tr00)) {  // in the right place
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

  /** How long the head took to get where it is now, as a twentieth of a revolution per byte. */
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

  /** Two revolutions, which is how long the chip waits for the computer to take a byte. */
  private long twoRevolutions() {
    return processorSpeed.getAsLong() * 4 / 10;
  }

  /** A tenth of a revolution, which is what formatting gets between sectors. */
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
            continue;                     // skip the deleted sector and look for the next
          }
        }
      } else {
        if (mt) {
          dataRegister[1] = (dataRegister[1] + 1) & 0xff;
          dataRegister[3] = 1;
          continue;                       // the next track of a multitrack read
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

  /**
   * The end of a read. End of cylinder is set when the sector was read whole, was the one EOT
   * names, and no terminal count arrived - which on the +3 it never does.
   */
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

        for (int i = 11; i > 0; i--) {    // let 11 GAP bytes go by
          d.readData();
        }
        if (mf) {
          for (int i = 11; i > 0; i--) {  // and another 11
            d.readData();
          }
        }
        for (int i = mf ? 12 : 6; i > 0; i--) {
          d.writeData(0x00);
        }
        crcPreset();
        if (mf) {
          for (int i = 3; i > 0; i--) {   // three 0xa1 with a clock mark
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

  /** Formatting: lays down the index mark and the gap that starts a track. */
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
      for (int i = 3; i > 0; i--) {       // three 0xc2 with a clock mark
        d.writeData(0xffc2);
      }
    }
    d.writeData(0x00fc | (mf ? 0x0000 : 0xff00));

    for (int i = 26; i > 0; i--) {       // the post-index gap
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
          currentDrive.waitIndexHole();   // reading a track starts at the index hole
          startReadDiag();
        }
        case WRITE_DATA -> startWriteData();
        case WRITE_ID -> {
          currentDrive.waitIndexHole();   // and so does writing one
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
      } else if ((mainStatus & 0x03) != 0) {          // a seek or a recalibrate is running
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

    if (state == State.EXE) {                         // READ DATA or READ DIAG
      dataOffset++;
      d.readData();
      crcAdd(d);

      // The Speedlock hack: a loader that reads the same sector over and over is looking for the
      // weak bits a protected disk has, so give it something different each time. Left alone when
      // the drive is doing its own weak reads, which would be two answers to the same question.
      if (speedlock > 0 && !d.doReadWeak) {
        if (dataOffset < 64 && d.data != 0xe5) {
          speedlock = 2;                              // the W.E.C Le Mans kind
        } else if ((speedlock > 1 || dataOffset < 64) && dataOffset % 29 == 0) {
          d.data ^= dataOffset;
          crcAdd(d);
        }
      }

      int r = d.data & 0xff;
      if (dataOffset == rlen) {                       // the host is sent rlen bytes and no more
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
          if (cmd.id() == CmdId.READ_DATA) {          // a READ DIAG is not aborted by it
            statusRegister[0] |= ST0_INT_ABNORM;
            cmdResult();
            return r;
          }
        }
        if (cmd.id() == CmdId.READ_DATA) {
          if (ddam != delData) {                      // a sector nobody asked for
            if (dataRegister[5] > dataRegister[3]) {
              statusRegister[0] |= ST0_INT_ABNORM;
            }
            cmdResult();
            return r;
          }
          rev = 2;
          mainStatus &= ~MAIN_DATAREQ;
          startReadData();
        } else {                                      // READ DIAG
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

    if ((mainStatus & MAIN_BUSY) != 0 && state == State.EXE) {   // WRITE, FORMAT or SCAN
      d = currentDrive;
      if (cmd.id() == CmdId.WRITE_ID) {                          // FORMAT
        dataRegister[dataOffset + 5] = data;
        dataOffset++;
        if (dataOffset == 4) {                                   // C, H, R, N: format the track
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
          d.writeData(0x00fe | (mf ? 0x0000 : 0xff00));          // the ID mark
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
          d.writeData(0x00fb | (mf ? 0x0000 : 0xff00));          // the data mark
          crcAdd(d);

          for (int i = rlen; i > 0; i--) {
            d.writeData(dataRegister[4]);                        // the filler byte
            crcAdd(d);
          }
          d.writeData(crc >> 8);
          d.writeData(crc & 0xff);

          for (int i = dataRegister[3]; i > 0; i--) {
            d.writeData(mf ? 0x4e : 0xff);
          }
          dataOffset = 0;
          dataRegister[2] = (dataRegister[2] - 1) & 0xff;        // ready for the next sector
        }
        if (dataRegister[2] == 0) {                              // every sector done
          while (!d.index) {                                     // GAP3, as Intel calls it
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

        if (dataOffset == rlen) {                                // only rlen bytes come from the host
          d.data = 0x00;
          while (dataOffset < sectorLength) {                    // the rest is filled with zeroes
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
      } else {                                                   // SCAN
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

    // ----------- Command phase ---------------
    if (cycle == 0) {
      commandRegister = data;
      cmdIdentify();
      mainStatus |= MAIN_BUSY;
      // A Sense Interrupt with no interrupt outstanding is an invalid command. The i8272 wants one
      // after every seek; the uPD765 must not, or The New Zealand Story does not load.
      if (intrq == Intrq.NONE && cmd.id() == CmdId.SENSE_INT) {
        commandRegister = 0x00;
        cmdIdentify();
      }
    } else {
      dataRegister[cycle - 1] = data;
    }

    if (cycle >= cmd.cmdLength()) {                              // every byte of the command is in
      state = State.EXE;
      mainStatus &= ~MAIN_DATAREQ;
      if (nonDma) {                                         // only non-DMA mode is emulated
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

      // A seek is busy only while its command is being read: up to four drives can be seeking at
      // once, and only one step per drive is issued while they overlap.
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
          // At 4MHz - which is what a mini-floppy runs at - every interval is twice as long.
          if (rate == Rate.MHZ4) {
            stpRate *= 2;
            hutTime *= 2;
            hldTime *= 2;
          }
          state = State.CMD;                                     // no result phase
          break;
        case SENSE_DRIVE:
          statusRegister[3] = us + (hd << 2);
          // The +3's wiring makes the double-sided signal the same as write protect.
          statusRegister[3] |= d.wrprot ? ST3_WRPROT : 0;
          statusRegister[3] |= d.tr00 ? ST3_TR00 : 0;
          statusRegister[3] |= d.ready ? ST3_READY : 0;
          break;
        case SENSE_INT:
          for (int i = 0; i < 4; i++) {
            if (seek[i] >= 4) {
              statusRegister[0] &= ~0xc0;                        // normal termination
              statusRegister[0] |= ST0_SEEK_END;
              if (seek[i] == 5) {
                statusRegister[0] |= ST0_INT_ABNORM;
              } else if (seek[i] == 6) {
                statusRegister[0] |= ST0_INT_READY | ST0_NOT_READY;
              }
              seek[i] = seekAge[i] = 0;
              senseIntRes[0] = statusRegister[0] & 0xfb;         // the head always reads as 0
              senseIntRes[1] = pcn[i];
              break;                                            // one interrupt is enough
            }
          }
          if (seek[0] < 4 && seek[1] < 4 && seek[2] < 4 && seek[3] < 4) {
            intrq = Intrq.NONE;
          }
          break;
        case RECALIBRATE:
          if ((mainStatus & (1 << us)) != 0) break;              // one is already running
          rec[us] = pcn[us];
          pcn[us] = 77;
          dataRegister[1] = 0x00;                                // to track 0
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
          // The Speedlock hack: count how many times the loader asks for the same sector.
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
          firstRw = true;                                           // at least one sector is read
          loadHead();
          return;
        case READ_DIAG:                                          // READ TRACK
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
          firstRw = true;                                           // at least one sector is written
          loadHead();
          return;
        case WRITE_ID:                                           // FORMAT TRACK
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

      if (cmd.id().ordinal() < CmdId.READ_ID.ordinal() && !terminated) {   // there is an execution phase
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
