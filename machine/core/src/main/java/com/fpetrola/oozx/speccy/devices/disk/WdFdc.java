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

import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.function.LongSupplier;

/**
 * A Western Digital floppy controller - the 1770 and 1772 that drive the motor themselves, and
 * the 1773, 1793 and 2797 that load the head - as a program sees it through its five registers,
 * and as the drive sees it: a byte at a time off the track, address marks and CRCs and all.
 * <p>
 * Ported from Fuse's wd_fdc.c. Everything that takes time on the real chip - a step, a
 * revolution, the motor spinning up, a byte going by - is an event on the machine's clock.
 */
public final class WdFdc {

  public enum Type { WD1773, FD1793, WD1770, WD1772, WD2797 }

  public static final int FLAG_NONE = 0;
  /** The Beta 128 wires HLD to both READY and MOTOR ON. */
  public static final int FLAG_BETA128 = 1;
  /** The Opus Discovery needs DRQ pulsed for every byte. */
  public static final int FLAG_DRQ = 2;
  /** READY comes from the board, not the drive. */
  public static final int FLAG_RDY = 4;
  /** HLT is not wired, so it is always high. */
  public static final int FLAG_NOHLT = 8;

  public static final int SR_MOTORON = 0x80;
  public static final int SR_WRPROT = 0x40;
  public static final int SR_SPINUP = 0x20;
  public static final int SR_RNF = 0x10;
  public static final int SR_CRCERR = 0x08;
  public static final int SR_LOST = 0x04;
  public static final int SR_IDX_DRQ = 0x02;
  public static final int SR_BUSY = 0x01;

  enum State { NONE, SEEK, SEEK_DELAY, VERIFY, READ, WRITE, READTRACK, WRITETRACK, READID }

  enum StatusType { TYPE1, TYPE2 }

  enum AmType { NONE, INDEX, ID, DATA }

  private final EventManager events;
  private final Z80Clock clock;
  private final LongSupplier processorSpeed;
  private final int fdcEvent;
  private final int motorOffEvent;
  private final int timeoutEvent;

  public Fdd currentDrive;
  public final Type type;
  private final int[] rates;
  public Fdd.Dir direction = Fdd.Dir.OUT;
  public boolean dden;
  public boolean intrq;
  public boolean datarq;
  boolean headLoad;
  boolean hlt;
  private final int hltTime;
  private final int flags;
  public boolean extraSignal;
  State state = State.NONE;
  boolean readId;
  StatusType statusType = StatusType.TYPE1;
  AmType idMark = AmType.NONE;
  int idTrack, idHead, idSector, idLength;
  boolean nonIbmLenCode;
  int sectorLength;
  boolean ddam;
  int rev;
  int dataCheckHead;
  boolean dataMultisector;
  int dataOffset;
  public int commandRegister;
  public int statusRegister;
  public int trackRegister;
  public int sectorRegister;
  public int dataRegister;
  int crc;

  /** What the board does with the lines: nothing, an NMI, a flag. Null for a line not wired. */
  public Runnable onIntrq, onIntrqReset, onDatarq, onDatarqReset;

  public WdFdc(Type type, int hltTime, int flags, EventManager events, Z80Clock clock, LongSupplier processorSpeed) {
    this.type = type;
    this.hltTime = hltTime;
    this.flags = flags;
    this.events = events;
    this.clock = clock;
    this.processorSpeed = processorSpeed;
    rates = type == Type.WD1772 ? new int[] {2, 3, 5, 6} : new int[] {6, 12, 20, 30};
    fdcEvent = events.eventRegister(this::event, "WD FDC event");
    motorOffEvent = events.eventRegister(this::event, "WD FDC motor off");
    timeoutEvent = events.eventRegister(this::event, "WD FDC timeout");
    masterReset();
  }

  private boolean headLoading() {
    return type == Type.WD1773 || type == Type.FD1793 || type == Type.WD2797;
  }

  private boolean motorDriving() {
    return type == Type.WD1770 || type == Type.WD1772;
  }

  private boolean has(int flag) {
    return (flags & flag) != 0;
  }

  private long now() {
    return clock.getTStates();
  }

  private long ms(long millis) {
    return processorSpeed.getAsLong() * millis / 1000;
  }

  private long us(long micros) {
    return processorSpeed.getAsLong() * micros / 1000000;
  }

  private void after(long tstates, int event) {
    events.eventAddWithData(now() + tstates, event, this);
  }

  private void cancel(int event) {
    events.eventRemoveType(event);
  }

  public void masterReset() {
    Fdd d = currentDrive;
    direction = Fdd.Dir.OUT;
    headLoad = false;
    if (d != null) {
      if (has(FLAG_BETA128)) d.motorOn(false); else d.headLoad(false);
    }
    readId = false;
    hlt = true;
    if (!has(FLAG_NOHLT) && hltTime > 0) hlt = false;
    intrq = false;
    datarq = false;
    state = State.NONE;
    statusType = StatusType.TYPE1;
    if (d != null) {
      while (!d.tr00) {
        d.step(Fdd.Dir.OUT);
      }
    }
    trackRegister = 0;
    sectorRegister = 0;
    dataRegister = 0;
    statusRegister = SR_LOST;
  }

  private void setIntrq() {
    if (motorDriving() && (statusRegister & SR_MOTORON) != 0) {
      after(ms(2000), motorOffEvent);
    }
    if (headLoading() && headLoad) {
      after(ms(3000), motorOffEvent);
    }
    if (!intrq) {
      intrq = true;
      if (onIntrq != null) onIntrq.run();
    }
  }

  private void resetIntrq() {
    if (intrq) {
      intrq = false;
      if (onIntrqReset != null) onIntrqReset.run();
    }
  }

  private void setDatarq() {
    if (!datarq) {
      statusRegister |= SR_IDX_DRQ;
      datarq = true;
      if (onDatarq != null) onDatarq.run();
    }
  }

  private void resetDatarq() {
    if (datarq) {
      statusRegister &= ~SR_IDX_DRQ;
      datarq = false;
      if (onDatarqReset != null) onDatarqReset.run();
    }
  }

  public void setHlt(boolean hlt) {
    this.hlt = hlt;
  }

  private void crcPreset() {
    crc = 0xffff;
  }

  private void crcAdd(Fdd d) {
    crc = Crc.fdc(crc, d.data & 0xff);
  }

  private boolean diskReady() {
    if (has(FLAG_BETA128)) return headLoad;
    if (has(FLAG_RDY)) return extraSignal;
    return currentDrive.ready;
  }

  /** Reads bytes off the track until an ID mark: 0 found it, 1 did not, 2 found it with a bad CRC. */
  private int readIdMark() {
    Fdd d = currentDrive;
    int i = rev;
    idMark = AmType.NONE;
    if (rev <= 0) {
      return 1;
    }
    while (i == rev) {
      crcPreset();
      if (dden) {
        d.readData();
        if (d.index) rev--;
        crcAdd(d);
        if (d.data == 0xffa1) {
          d.readData(); crcAdd(d);
          if (d.index) rev--;
          if (d.data != 0xffa1) continue;
          d.readData(); crcAdd(d);
          if (d.index) rev--;
          if (d.data != 0xffa1) continue;
        } else {
          continue;
        }
      }
      d.readData(); crcAdd(d);
      if (d.index) rev--;
      if (dden ? d.data != 0x00fe : d.data != 0xfffe) continue;
      d.readData(); crcAdd(d);
      if (d.index) rev--;
      idTrack = d.data;
      d.readData(); crcAdd(d);
      if (d.index) rev--;
      idHead = d.data;
      d.readData(); crcAdd(d);
      if (d.index) rev--;
      idSector = d.data;
      d.readData(); crcAdd(d);
      if (d.index) rev--;
      idLength = d.data;
      if (nonIbmLenCode) {
        sectorLength = 0x80 << ((d.data + 1) & 0x03);
      } else {
        sectorLength = 0x80 << (d.data & 0x03);
      }
      d.readData(); crcAdd(d);
      if (d.index) rev--;
      d.readData(); crcAdd(d);
      if (d.index) rev--;
      idMark = AmType.ID;
      if (crc != 0x0000) {
        statusRegister |= SR_CRCERR;
        return 2;
      }
      statusRegister &= ~SR_CRCERR;
      return 0;
    }
    return 1;
  }

  private boolean readDatamark() {
    Fdd d = currentDrive;
    idMark = AmType.NONE;
    int i;
    if (dden) {
      for (i = 40; i > 0; i--) {
        d.readData();
        if (d.data == 0x4e) continue;
        if (d.data == 0x00) break;
        return false;
      }
      for (; i > 0; i--) {
        crcPreset();
        d.readData(); crcAdd(d);
        if (d.data == 0x00) continue;
        if (d.data == 0xffa1) break;
        return false;
      }
      for (i = d.data == 0xffa1 ? 2 : 3; i > 0; i--) {
        d.readData(); crcAdd(d);
        if (d.data != 0xffa1) return false;
      }
      d.readData(); crcAdd(d);
      if (d.data < 0x00f8 || d.data > 0x00fb) return false;
      ddam = d.data != 0x00fb;
      idMark = AmType.DATA;
      return true;
    }
    for (i = 30; i > 0; i--) {
      d.readData();
      if (d.data == 0xff) continue;
      if (d.data == 0x00) break;
      return false;
    }
    for (; i > 0; i--) {
      crcPreset();
      d.readData(); crcAdd(d);
      if (d.data == 0x00) continue;
      if (d.data >= 0xfff8 && d.data <= 0xfffb) break;
      return false;
    }
    if (i == 0) {
      d.readData(); crcAdd(d);
      if (d.data < 0xfff8 || d.data > 0xfffb) return false;
    }
    ddam = d.data != 0x00fb;
    idMark = AmType.DATA;
    return true;
  }

  public int srRead() {
    Fdd d = currentDrive;
    resetIntrq();
    if (statusType == StatusType.TYPE1) {
      statusRegister &= ~SR_IDX_DRQ;
      if (!d.loaded || d.indexPulse) {
        statusRegister |= SR_IDX_DRQ;
      }
    }
    if (headLoading()) {
      if (diskReady()) statusRegister &= ~SR_MOTORON; else statusRegister |= SR_MOTORON;
    }
    return statusRegister & 0xff;
  }

  /** How long the bytes just read took, as a slice of a revolution, so the next look comes then. */
  private long timeSince(int startedAt) {
    Fdd d = currentDrive;
    int slice = d.disk.cBpt != 0 ? (d.disk.i - startedAt) * 200 / d.disk.cBpt : 200;
    return slice > 0 ? ms(slice) : 0;
  }

  private int startPosition() {
    Fdd d = currentDrive;
    return d.disk.i >= d.disk.cBpt ? 0 : d.disk.i;
  }

  private void seekVerifyReadId() {
    readId = true;
    cancel(fdcEvent);
    if (idMark == AmType.NONE) {
      while (rev != 0) {
        int started = startPosition();
        if (readIdMark() == 0) {
          if (idTrack != trackRegister) {
            statusRegister |= SR_RNF;
          }
        } else {
          idMark = AmType.NONE;
        }
        long wait = timeSince(started);
        if (wait > 0) {
          after(wait, fdcEvent);
          return;
        } else if (idMark != AmType.NONE) {
          break;
        }
      }
      if (idMark == AmType.NONE) {
        statusRegister |= SR_RNF;
      }
    }
    state = State.NONE;
    statusRegister &= ~SR_BUSY;
    setIntrq();
    readId = false;
  }

  private void seekVerify() {
    Fdd d = currentDrive;
    cancel(fdcEvent);
    if (headLoading()) {
      if (!hlt) {
        after(ms(5), fdcEvent);
        return;
      }
      if (headLoad) {
        statusRegister |= SR_SPINUP;
      }
    }
    if (d.tr00) statusRegister |= SR_LOST; else statusRegister &= ~SR_LOST;
    rev = 5;
    idMark = AmType.NONE;
    seekVerifyReadId();
  }

  private void typeI() {
    int b = commandRegister;
    Fdd d = currentDrive;
    boolean stepping = (b & 0x60) != 0;
    if (state == State.SEEK_DELAY) {
      if (stepping) {
        typeIVerify();
        return;
      }
      typeILoop();
      return;
    }
    statusRegister |= SR_SPINUP;
    if (stepping) {
      if ((b & 0x40) != 0) {
        direction = (b & 0x20) != 0 ? Fdd.Dir.OUT : Fdd.Dir.IN;
      }
      if ((b & 0x10) != 0) {
        trackRegister = (trackRegister + (direction == Fdd.Dir.IN ? 1 : -1)) & 0xff;
      }
      typeIStep();
      return;
    }
    if ((b & 0x10) == 0) {
      trackRegister = 0xff;
      dataRegister = 0;
    }
    typeILoop();
  }

  private void typeILoop() {
    if (trackRegister != dataRegister) {
      direction = trackRegister < dataRegister ? Fdd.Dir.IN : Fdd.Dir.OUT;
      trackRegister = (trackRegister + (direction == Fdd.Dir.IN ? 1 : -1)) & 0xff;
      typeIStep();
      return;
    }
    typeIVerify();
  }

  private void typeIStep() {
    Fdd d = currentDrive;
    if (d.tr00 && direction == Fdd.Dir.OUT) {
      trackRegister = 0;
      if ((commandRegister & 0x60) != 0) {
        typeIVerify();
      } else {
        typeILoop();
      }
      return;
    }
    d.step(direction);
    state = State.SEEK_DELAY;
    cancel(fdcEvent);
    after(ms(rates[commandRegister & 0x03]), fdcEvent);
  }

  private void typeIVerify() {
    int b = commandRegister;
    Fdd d = currentDrive;
    if ((b & 0x04) != 0) {
      if (headLoading()) {
        headLoad = true;
        cancel(motorOffEvent);
        if (has(FLAG_BETA128)) d.motorOn(true); else d.headLoad(true);
        cancel(fdcEvent);
        after(ms(15), fdcEvent);
      }
      state = State.VERIFY;
      if (motorDriving() && (statusRegister & SR_MOTORON) == 0) {
        statusRegister |= SR_MOTORON;
        d.motorOn(true);
        cancel(fdcEvent);
        after(ms(1200), fdcEvent);
        return;
      }
      seekVerify();
      return;
    }
    if (d.tr00) statusRegister |= SR_LOST; else statusRegister &= ~SR_LOST;
    state = State.NONE;
    statusRegister &= ~SR_BUSY;
    setIntrq();
  }

  private void typeIISeek() {
    int b = commandRegister;
    Fdd d = currentDrive;
    cancel(fdcEvent);
    if (idMark == AmType.NONE) {
      readId = true;
      while (rev != 0) {
        int started = startPosition();
        if (readIdMark() == 0) {
          if ((dataCheckHead != -1 && dataCheckHead != (idHead != 0 ? 1 : 0))
              || idTrack != trackRegister || idSector != sectorRegister) {
            idMark = AmType.NONE;
          }
        } else {
          idMark = AmType.NONE;
        }
        long wait = timeSince(started);
        if (wait > 0) {
          after(wait, fdcEvent);
          return;
        } else if (idMark != AmType.NONE) {
          break;
        }
      }
    }
    readId = false;
    if (idMark == AmType.NONE) {
      statusRegister |= SR_RNF;
      statusRegister &= ~SR_BUSY;
      state = State.NONE;
      setIntrq();
      return;
    }
    if (state == State.READ) {
      if (idMark == AmType.ID) {
        readDatamark();
      }
      if (idMark == AmType.NONE) {
        statusRegister |= SR_RNF;
        statusRegister &= ~SR_BUSY;
        state = State.NONE;
        setIntrq();
        return;
      }
      if (ddam) {
        statusRegister |= SR_SPINUP;
      }
      dataOffset = 0;
      setDatarq();
    } else {
      ddam = (b & 0x01) != 0;
      for (int i = 11; i > 0; i--) d.readData();
      setDatarq();
      dataOffset = 0;
      if (dden) {
        for (int i = 11; i > 0; i--) d.readData();
      }
      d.data = 0x00;
      for (int i = dden ? 12 : 6; i > 0; i--) d.writeData();
      crcPreset();
      if (dden) {
        d.data = 0xffa1;
        for (int i = 3; i > 0; i--) {
          d.writeData();
          crcAdd(d);
        }
      }
      d.data = (ddam ? 0x00f8 : 0x00fb) | (dden ? 0x0000 : 0xff00);
      d.writeData();
      crcAdd(d);
    }
    cancel(timeoutEvent);
    after(ms(1000), timeoutEvent);
  }

  private void typeII() {
    int b = commandRegister;
    Fdd d = currentDrive;
    cancel(fdcEvent);
    if (headLoading()) {
      if (!diskReady()) {
        statusRegister &= ~SR_BUSY;
        state = State.NONE;
        setIntrq();
        return;
      }
      if (!hlt) {
        after(ms(5), fdcEvent);
        return;
      }
    }
    if (state == State.WRITE) {
      if (d.wrprot) {
        statusRegister |= SR_WRPROT;
        statusRegister &= ~SR_BUSY;
        state = State.NONE;
        setIntrq();
        return;
      }
      statusRegister &= ~SR_WRPROT;
    }
    dataMultisector = (b & 0x10) != 0;
    rev = 5;
    idMark = AmType.NONE;
    typeIISeek();
  }

  private void typeIII() {
    Fdd d = currentDrive;
    cancel(fdcEvent);
    if (!readId && headLoading()) {
      if (!diskReady()) {
        statusRegister &= ~SR_BUSY;
        state = State.NONE;
        setIntrq();
        return;
      }
      if (!hlt) {
        after(ms(5), fdcEvent);
        return;
      }
    }
    if (state == State.WRITETRACK) {
      if (d.wrprot) {
        statusRegister |= SR_WRPROT;
        statusRegister &= ~SR_BUSY;
        state = State.NONE;
        setIntrq();
        return;
      }
      statusRegister &= ~SR_WRPROT;
      dataOffset = 0;
      d.waitIndexHole();
      setDatarq();
    } else if (state == State.READTRACK) {
      d.waitIndexHole();
      setDatarq();
    } else {
      if (!readId) {
        readId = true;
        rev = 5;
        idMark = AmType.NONE;
      }
      if (idMark == AmType.NONE) {
        while (rev != 0) {
          int started = startPosition();
          readIdMark();
          long wait = timeSince(started);
          if (wait > 0) {
            after(wait, fdcEvent);
            return;
          } else if (idMark != AmType.NONE) {
            break;
          }
        }
        if (idMark == AmType.NONE) {
          state = State.NONE;
          statusRegister |= SR_RNF;
          statusRegister &= ~SR_BUSY;
          setIntrq();
          readId = false;
          return;
        }
      }
      readId = false;
      dataOffset = 0;
      setDatarq();
    }
    cancel(timeoutEvent);
    after(ms(400), timeoutEvent);
  }

  private void event(long lastTstates, int event, Object userData) {
    Fdd d = currentDrive;
    if (event == timeoutEvent) {
      if (state == State.READ || state == State.WRITE || state == State.READTRACK
          || state == State.WRITETRACK || state == State.READID) {
        state = State.NONE;
        statusRegister |= SR_LOST;
        statusRegister &= ~SR_BUSY;
        resetDatarq();
        setIntrq();
      }
      return;
    }
    if (event == motorOffEvent) {
      if (motorDriving()) {
        statusRegister &= ~SR_MOTORON;
        d.motorOn(false);
      } else {
        headLoad = false;
        if (has(FLAG_BETA128)) d.motorOn(false); else d.headLoad(false);
      }
      return;
    }
    if (headLoading() && hltTime > 0 && headLoad && !hlt) {
      hlt = true;
    }
    if ((motorDriving() && (statusRegister & SR_MOTORON) != 0 && statusType == StatusType.TYPE1)
        || (headLoading() && (state == State.SEEK || state == State.SEEK_DELAY) && headLoad)) {
      statusRegister |= SR_SPINUP;
    }
    if (readId) {
      if (state == State.VERIFY) {
        seekVerifyReadId();
      } else if ((state == State.READ || state == State.WRITE) && datarq) {
        datarq = false;
        setDatarq();
      } else if (state == State.READ || state == State.WRITE) {
        typeIISeek();
      } else if (state == State.READID) {
        typeIII();
      }
    } else if (state == State.SEEK || state == State.SEEK_DELAY) {
      typeI();
    } else if (state == State.VERIFY) {
      seekVerify();
    } else if ((state == State.READ || state == State.WRITE) && datarq) {
      datarq = false;
      setDatarq();
    } else if (state == State.READ || state == State.WRITE) {
      typeII();
    } else if ((state == State.READTRACK || state == State.READID || state == State.WRITETRACK) && datarq) {
      datarq = false;
      setDatarq();
    } else if (state == State.READTRACK || state == State.READID || state == State.WRITETRACK) {
      typeIII();
    }
  }

  /** Gets the disk turning, or the head down; answers true when the command has to wait for it. */
  private boolean spinup(int b) {
    long delay = 0;
    Fdd d = currentDrive;
    if (state != State.SEEK && (b & 0x04) != 0) {
      delay = 30;
    }
    if (motorDriving()) {
      if ((statusRegister & SR_MOTORON) == 0) {
        statusRegister |= SR_MOTORON;
        d.motorOn(true);
        if ((b & 0x08) == 0) {
          delay += 6 * 200;
        }
      }
    } else {
      cancel(motorOffEvent);
      if (state == State.SEEK) {
        if ((b & 0x08) != 0) {
          headLoad = true;
          if (has(FLAG_BETA128)) d.motorOn(true); else d.headLoad(true);
        } else if ((b & 0x04) == 0) {
          headLoad = false;
          if (!has(FLAG_NOHLT) && hltTime > 0) hlt = false;
          if (has(FLAG_BETA128)) d.motorOn(false); else d.headLoad(false);
        }
        return false;
      }
      headLoad = true;
      if (has(FLAG_BETA128)) d.motorOn(true); else d.headLoad(true);
      if (hltTime > 0) {
        delay += hltTime;
      }
    }
    if (type == Type.WD2797 && (b & 0xc0) == 0xc0 && (b & 0x30) != 0x10) {
      d.setHead((b & 0x02) != 0 ? 1 : 0);
    }
    if (delay != 0) {
      cancel(fdcEvent);
      after(ms(delay), fdcEvent);
      return true;
    }
    return false;
  }

  public void crWrite(int b) {
    b &= 0xff;
    Fdd d = currentDrive;
    resetIntrq();
    if ((b & 0xf0) == 0xd0) {
      cancel(fdcEvent);
      statusRegister &= ~(SR_BUSY | SR_WRPROT | SR_CRCERR | SR_IDX_DRQ);
      state = State.NONE;
      statusType = StatusType.TYPE1;
      resetDatarq();
      if ((b & 0x08) != 0) {
        setIntrq();
      } else if ((b & 0x04) != 0) {
        d.onIndex(this::setIntrq);
      }
      if (d.tr00) statusRegister |= SR_LOST; else statusRegister &= ~SR_LOST;
      spinup(b & 0xf7);
      return;
    }
    if ((statusRegister & SR_BUSY) != 0) {
      return;
    }
    commandRegister = b;
    statusRegister |= SR_BUSY;
    cancel(motorOffEvent);
    if ((b & 0x80) == 0) {
      state = State.SEEK;
      statusType = StatusType.TYPE1;
      statusRegister &= ~(SR_CRCERR | SR_RNF | SR_IDX_DRQ);
      resetDatarq();
      rev = 5;
      if (spinup(b)) return;
      typeI();
    } else if ((b & 0x40) == 0) {
      if (type == Type.WD1773 || type == Type.FD1793) {
        if (!diskReady()) {
          statusRegister &= ~SR_BUSY;
          state = State.NONE;
          setIntrq();
          return;
        }
      }
      if (type == Type.WD1773 && (b & 0x02) != 0) {
        dataCheckHead = (b & 0x08) != 0 ? 1 : 0;
      } else if (type == Type.WD2797) {
        dataCheckHead = (b & 0x02) != 0 ? 1 : 0;
      } else {
        dataCheckHead = -1;
      }
      nonIbmLenCode = type == Type.WD2797 && (b & 0x08) == 0;
      state = (b & 0x20) != 0 ? State.WRITE : State.READ;
      statusType = StatusType.TYPE2;
      statusRegister &= ~(SR_WRPROT | SR_RNF | SR_IDX_DRQ | SR_LOST | SR_SPINUP);
      if (type == Type.WD2797) d.setHead((b & 0x02) != 0 ? 1 : 0);
      rev = 5;
      if (spinup(b)) return;
      typeII();
    } else if ((b & 0x30) != 0x10) {
      if (headLoading()) {
        if (!diskReady()) {
          statusRegister &= ~SR_BUSY;
          state = State.NONE;
          setIntrq();
          return;
        }
      }
      state = (b & 0x20) != 0 ? ((b & 0x10) != 0 ? State.WRITETRACK : State.READTRACK) : State.READID;
      statusType = StatusType.TYPE2;
      statusRegister &= ~(SR_SPINUP | SR_RNF | SR_IDX_DRQ | SR_LOST);
      rev = 5;
      if (spinup(b)) return;
      typeIII();
    }
  }

  public int trRead() {
    return trackRegister;
  }

  public void trWrite(int b) {
    trackRegister = b & 0xff;
  }

  public int secRead() {
    return sectorRegister;
  }

  public void secWrite(int b) {
    sectorRegister = b & 0xff;
  }

  private void finishTransfer(boolean ok) {
    statusRegister &= ~SR_BUSY;
    statusType = StatusType.TYPE2;
    state = State.NONE;
    setIntrq();
    resetDatarq();
  }

  private void nextSector() {
    sectorRegister = (sectorRegister + 1) & 0xff;
    rev = 5;
    resetDatarq();
    after(ms(1000), timeoutEvent);
    after(ms(20), fdcEvent);
  }

  public int drRead() {
    Fdd d = currentDrive;
    if (has(FLAG_DRQ) && (statusRegister & SR_BUSY) != 0) {
      cancel(fdcEvent);
    }
    if (state == State.READ) {
      dataOffset++;
      d.readData();
      crcAdd(d);
      if (d.data > 0xff) {
        statusRegister |= SR_RNF;
        finishTransfer(false);
      } else {
        dataRegister = d.data;
        if (dataOffset == sectorLength) {
          d.readData(); crcAdd(d);
          d.readData(); crcAdd(d);
          cancel(timeoutEvent);
          if (crc == 0x0000 && dataMultisector) {
            nextSector();
          } else {
            if (crc == 0x0000) statusRegister &= ~SR_CRCERR; else statusRegister |= SR_CRCERR;
            finishTransfer(true);
          }
        }
      }
    } else if (state == State.READID) {
      switch (dataOffset) {
        case 0 -> dataRegister = idTrack;
        case 1 -> dataRegister = idHead;
        case 2 -> dataRegister = idSector;
        case 3 -> dataRegister = idLength;
        case 4 -> dataRegister = crc >> 8;
        case 5 -> {
          sectorRegister = idTrack;
          dataRegister = crc & 0xff;
          cancel(timeoutEvent);
          finishTransfer(true);
        }
        default -> { }
      }
      dataOffset++;
    } else if (state == State.READTRACK) {
      d.readData();
      dataRegister = d.data & 0x00ff;
      if (d.index) {
        cancel(timeoutEvent);
        finishTransfer(true);
      }
    }
    if (has(FLAG_DRQ) && (statusRegister & SR_BUSY) != 0) {
      after(us(30), fdcEvent);
    }
    return dataRegister & 0xff;
  }

  public void drWrite(int b) {
    b &= 0xff;
    Fdd d = currentDrive;
    dataRegister = b;
    if (state == State.WRITE) {
      d.data = b;
      dataOffset++;
      d.writeData();
      crcAdd(d);
      if (dataOffset == sectorLength) {
        d.data = crc >> 8;
        d.writeData();
        d.data = crc & 0xff;
        d.writeData();
        d.data = 0xff;
        d.writeData();
        cancel(timeoutEvent);
        if (dataMultisector) {
          nextSector();
        } else {
          finishTransfer(true);
        }
      }
    } else if (state == State.WRITETRACK) {
      d.data = b;
      if (dden) {
        if (b == 0xf7) {
          d.data = crc >> 8;
          d.writeData();
          d.data = crc & 0xff;
        } else if (b == 0xf5) {
          d.data = 0xffa1;
          crc = 0xcdb4;
        } else if (b == 0xf6) {
          d.data = 0xffc2;
        } else {
          crcAdd(d);
        }
      } else {
        if (b == 0xf7) {
          d.data = crc >> 8;
          d.writeData();
          d.data = crc & 0xff;
        } else if (b == 0xfe || (b >= 0xf8 && b <= 0xfb)) {
          crcPreset();
          crcAdd(d);
          d.data |= 0xff00;
        } else if (b == 0xfc) {
          d.data |= 0xff00;
        } else {
          crcAdd(d);
        }
      }
      d.writeData();
      if (d.index) {
        cancel(timeoutEvent);
        statusRegister &= ~SR_BUSY;
        state = State.NONE;
        setIntrq();
        resetDatarq();
      }
    }
    if (has(FLAG_DRQ) && (statusRegister & SR_BUSY) != 0) {
      after(us(30), fdcEvent);
    }
  }
}
