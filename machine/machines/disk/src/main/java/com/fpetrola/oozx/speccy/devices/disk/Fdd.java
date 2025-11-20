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

import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.Random;
import java.util.function.LongSupplier;
import com.google.inject.Singleton;

/**
 * A floppy drive: a disk turning at 300 rpm under a head that steps between tracks, with an
 * index hole going by once a turn. The controller reads and writes it a byte at a time, and where
 * the head is along the track is where the disk happens to have turned to.
 * <p>
 * The drive is an object, not a struct handed to functions.
 */
public final class Fdd {

  public enum Type { NONE, SHUGART, IBMPC }

  public enum Dir { OUT, IN }

  /** What kind of drive was fitted, in the words the option puts it: none, or heads and how many tracks it can reach. */
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
  /** The byte under the head, with bit 8 set when it was written with a missing clock. */
  public int data;
  /** Bit 0: FM. Bit 1: weak. */
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

  /** Whoever is waiting for the index hole, told once when it comes round. */
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

  /*
   * disk.sides   1  2  1  2  1  2  1  2
   * c_head       0  0  1  1  0  0  1  1
   * upside       0  0  0  0  1  1  1  1
   * UNREADABLE   0  0  1  0  1  0  0  0
   */
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
      // Where along the track the head came down: about bpt/fact on, give or take a tenth.
      disk.i += disk.cBpt / fact + disk.cBpt * (random.nextInt(10) + random.nextInt(10) - 9) / fact / 100;
      while (disk.i >= disk.cBpt) {
        disk.i -= disk.cBpt;
      }
    }
    index = disk.i == 0;
  }

  /** Fits this drive: what type, and what it can reach; reinit keeps the disk that is in it. */
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
    // TEAC FD55: READY once the disk is in, turning at speed, and two index pulses have gone by.
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

  /** Selecting a Shugart drive is what puts its head down. */
  public void select(boolean select) {
    selected = select;
    if (type == Type.SHUGART) {
      headLoad(selected);
    }
  }

  /** The disk in {@link #disk} goes in, the right way up or the other. */
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

  /** The next byte off the disk, or into it; the disk turns either way, whether the head reads or not. */
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
      // Weak data cannot be written with ordinary hardware.
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

  /** Puts that byte on the track under the head. Answers false when the disk is write-protected. */
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

  /** Tells the controller when the hole next comes round, once. */
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

  /** The drives themselves, whichever interface turns them. */
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
