/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
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

import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.Random;
import java.util.function.LongSupplier;
import com.google.inject.Singleton;

/**
 * A single floppy drive: a 300rpm disk under a stepping head, one index hole per revolution,
 * read/written a byte at a time at whatever rotational position it has reached.
 */
public final class Fdd {

  public enum Type { NONE, SHUGART, IBMPC }

  public enum Dir { OUT, IN }

  /** Drive capacity as configured: absent, or a head count and a maximum track count. */
  public enum Kind {
    DISABLED(false, 0, 0), SINGLE_SIDED_40(true, 1, 40), DOUBLE_SIDED_40(true, 2, 40),
    SINGLE_SIDED_80(true, 1, 80), DOUBLE_SIDED_80(true, 2, 80);

    public final boolean enabled;
    public final int heads, cylinders;

    Kind(boolean enabled, int heads, int cylinders) {
      this.enabled = enabled;
      this.heads = heads;
      this.cylinders = cylinders;
    }
  }

  static final int LOAD_FACT = 2;
  static final int HEAD_FACT = 16;
  static final int STEP_FACT = 34;
  static final int MAX_TRACK = 99;
  static final int TRACK_THRESHOLD = 10;

  private final Scheduler scheduler;
  private final Z80Clock clock;
  private final LongSupplier processorSpeed;
  private final Limits floppy;
  private final Task motorSettled;
  private final Task nextIndexPulse;
  private final Random random = new Random();

  public Type type = Type.NONE;
  boolean autoGeom;
  int fddHeads;
  int fddCylinders;
  public boolean tr00;
  public boolean index;
  public boolean wrprot;
  /** Current byte at the head; bit 8 flags a clock-less (weak) write. */
  public int data;
  /** Per-byte flags: bit 0 single density, bit 1 weak/unreadable. */
  public int marks;
  public Disk disk = new Disk();
  public boolean loaded;
  public boolean upsidedown;
  public boolean selected;
  public boolean ready;
  public boolean dskchg;
  public boolean hdout;
  public boolean unreadable;
  boolean doReadWeak;
  int cHead;
  int cCylinder;
  public boolean motoron;
  public boolean loadhead;
  public boolean indexPulse;

  /** One-shot callback fired on the next index pulse. */
  private Runnable waitingForIndex;

  public Fdd(Scheduler scheduler, Z80Clock clock, LongSupplier processorSpeed, Limits floppy) {
    this.scheduler = scheduler;
    this.clock = clock;
    this.processorSpeed = processorSpeed;
    this.floppy = floppy;
    motorSettled = scheduler.register(new MotorSettled());
    nextIndexPulse = scheduler.register(new IndexPulse());
  }

  private long ms(long millis) {
    return processorSpeed.getAsLong() * millis / 1000;
  }

  /** Turning a disk over swaps which head number reaches which side, and a one-sided disk has nothing under the other. */
  private void setData(int fact) {
    int head = upsidedown ? 1 - cHead : cHead;
    if (!loaded) {
      return;
    }
    if (unreadable || (disk.sides == 1 && head == 1) || cCylinder >= disk.cylinders) {
      disk.noTrack();
      return;
    }
    disk.setTrack(head, cCylinder);
    if (fact > 0) {
      // Random landing position: roughly one "fact"-th around the track, +/-10%.
      disk.i += disk.cBpt / fact + disk.cBpt * (random.nextInt(10) + random.nextInt(10) - 9) / fact / 100;
      while (disk.i >= disk.cBpt) {
        disk.i -= disk.cBpt;
      }
    }
    index = disk.i == 0;
  }

  /** Configures drive type and capacity; a reinit preserves whatever disk is already loaded. */
  public void init(Type type, Kind dt, boolean reinit) {
    boolean wasUpsidedown = upsidedown;
    boolean wasLoaded = loaded;
    boolean wasSelected = selected;
    boolean wasReadingWeak = doReadWeak;
    if (dt == null) {
      dt = Kind.DISABLED;
    }
    fddHeads = fddCylinders = cHead = cCylinder = 0;
    upsidedown = unreadable = loaded = autoGeom = selected = false;
    dskchg = hdout = ready = doReadWeak = false;
    index = tr00 = wrprot = type != Type.NONE;
    this.type = type;
    waitingForIndex = null;
    if (dt.heads < 0 || dt.heads > 2 || dt.cylinders < 0 || dt.cylinders > MAX_TRACK) {
      throw new IllegalArgumentException("invalid drive geometry");
    }
    if (dt.heads == 0) {
      autoGeom = true;
    }
    fddHeads = dt.heads;
    fddCylinders = dt.cylinders == 80 ? floppy.eightyTrackMax : floppy.fortyTrackMax;
    if (reinit) {
      selected = wasSelected;
      doReadWeak = wasReadingWeak;
    } else {
      unload();
    }
    if (reinit && wasLoaded) {
      unload();
      load(wasUpsidedown);
    } else {
      disk.data = null;
    }
  }

  public void motorOn(boolean on) {
    if (!loaded || motoron == on) {
      return;
    }
    motoron = on;
    // Modelled on the TEAC FD55: ready only after insertion, spin-up, and two index pulses.
    scheduler.cancel(motorSettled);
    long now = clock.getTStates();
    if (on) {
      scheduler.schedule(motorSettled, now + ms(400));
      if (loaded) {
        scheduler.schedule(nextIndexPulse, now + ms(indexPulse ? 10 : 190));
      }
    } else {
      scheduler.schedule(motorSettled, now + ms(300));
    }
  }

  public void headLoad(boolean load) {
    if (!loaded || loadhead == load) {
      return;
    }
    loadhead = load;
    setData(HEAD_FACT);
  }

  /** On Shugart-interface drives, selecting the drive also engages the head. */
  public void select(boolean select) {
    selected = select;
    if (type == Type.SHUGART) {
      headLoad(selected);
    }
  }

  /** Loads {@link #disk}, optionally flipped, validating its geometry first. */
  public void load(boolean upsidedown) {
    if (type == Type.NONE) {
      throw new IllegalStateException("there is no drive here");
    }
    if (disk.sides < 0 || disk.sides > 2 || disk.cylinders < 0 || disk.cylinders > MAX_TRACK) {
      throw new IllegalArgumentException("invalid disk geometry");
    }
    if (autoGeom) {
      fddHeads = disk.sides;
      fddCylinders = disk.cylinders > floppy.fortyTrackMax
          ? floppy.eightyTrackMax : floppy.fortyTrackMax;
    }
    unreadable = disk.cylinders > fddCylinders + TRACK_THRESHOLD;
    this.upsidedown = upsidedown;
    wrprot = disk.wrprot;
    loaded = true;
    if (type == Type.SHUGART && selected) {
      headLoad(true);
    }
    doReadWeak = disk.haveWeak;
    setData(LOAD_FACT);
    ready = motoron && loaded;
    if (disk.density == Disk.Density.HD) {
      hdout = true;
    }
  }

  public void insert(Disk disk, boolean upsidedown) {
    this.disk = disk;
    load(upsidedown);
  }

  public void unload() {
    ready = loaded = dskchg = hdout = false;
    index = wrprot = true;
    motorOn(false);
    if (type == Type.SHUGART && selected) {
      headLoad(false);
    }
  }

  public void eject() {
    unload();
    disk = new Disk();
  }

  public void setHead(int head) {
    if (fddHeads == 1) {
      return;
    }
    head = head > 0 ? 1 : 0;
    if (cHead == head) {
      return;
    }
    cHead = head;
    setData(0);
  }

  public void step(Dir direction) {
    if (direction == Dir.OUT) {
      if (cCylinder > 0) cCylinder--;
    } else if (cCylinder < fddCylinders - 1) {
      cCylinder++;
    }
    tr00 = cCylinder == 0;
    setData(STEP_FACT);
    if (loaded && selected) {
      dskchg = true;
    }
  }

  public Type type() {
    return type;
  }

  public int heads() {
    return fddHeads;
  }

  public int cylinder() {
    return cCylinder;
  }

  /** Advances the disk one byte and reads or writes it; rotation continues even when idle. */
  private boolean readWriteData(boolean write) {
    if (!selected || !ready || !loadhead || !disk.hasTrack()) {
      if (loaded && motoron) {
        if (disk.i >= disk.cBpt) {
          disk.i = 0;
        }
        if (!write) {
          data = 0x100;
        }
        disk.i++;
        index = disk.i >= disk.cBpt;
      }
      return true;
    }
    if (disk.i >= disk.cBpt) {
      disk.i = 0;
    }
    if (write) {
      if (disk.wrprot) {
        disk.i++;
        index = disk.i >= disk.cBpt;
        return false;
      }
      disk.setTrackByte(disk.i, data & 0xff);
      disk.setBit(disk.clocks, disk.i, (data & 0xff00) != 0);
      disk.setBit(disk.fm, disk.i, (marks & 0x01) != 0);
      // Real drives cannot produce weak/clock-less data on a write, only reproduce it on read.
      disk.setBit(disk.weak, disk.i, false);
      disk.dirty = true;
    } else {
      data = disk.trackByte(disk.i);
      if (disk.clock(disk.i)) {
        data |= 0xff00;
      }
      marks = 0;
      if (disk.fmMark(disk.i)) {
        marks |= 0x01;
      }
      if (disk.weakMark(disk.i)) {
        marks |= 0x02;
        data |= random.nextInt(0xff);
      }
    }
    disk.i++;
    index = disk.i >= disk.cBpt;
    return true;
  }

  public void readData() {
    readWriteData(false);
  }

  /** Writes to the current track position; returns false if the disk is write-protected. */
  public boolean writeData(int data) {
    this.data = data;
    return readWriteData(true);
  }

  public void flip(boolean upsidedown) {
    if (!loaded) {
      return;
    }
    this.upsidedown = upsidedown;
    setData(LOAD_FACT);
  }

  public void writeProtect(boolean wrprot) {
    if (!loaded) {
      return;
    }
    this.wrprot = disk.wrprot = wrprot;
  }

  public void waitIndexHole() {
    if (!selected || !ready) {
      return;
    }
    disk.i = 0;
    index = true;
  }

  /** Registers a one-shot index-pulse callback. */
  public void onIndex(Runnable controller) {
    waitingForIndex = controller;
  }

  private final class MotorSettled extends Task {
    public void run(long due) {
      ready = motoron && loaded;
    }
  }

  private final class IndexPulse extends Task {
    public void run(long due) {
      indexPulse = !indexPulse;
      if (!indexPulse && waitingForIndex != null) {
        Runnable waiting = waitingForIndex;
        waitingForIndex = null;
        waiting.run();
      }
      if (motoron && loaded) {
        scheduler.schedule(this, due + ms(indexPulse ? 10 : 190));
      }
    }
  }

  /** Shared drive-count limits, independent of which controller owns the drives. */
  @Singleton
  public static class Limits {
    public int fortyTrackMax;
    public int eightyTrackMax;

    public int fortyTrackMax() {
      return fortyTrackMax;
    }

    public void setFortyTrackMax(int max) {
      fortyTrackMax = max;
    }

    public int eightyTrackMax() {
      return eightyTrackMax;
    }

    public void setEightyTrackMax(int max) {
      eightyTrackMax = max;
    }
  }
}
