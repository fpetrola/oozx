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
 */package com.fpetrola.oozx.speccy.devices.disk;

import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.AbstractPeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.fpetrola.oozx.speccy.ports.Wired;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

/** The +3's floppy: a uPD765A and two drives, on the +3's ports and with its motor on its paging port. */
@Singleton
public class Upd765Peripheral extends AbstractPeripheral {
  private final UpdFdc controller;
  private final Fdd[] drives = new Fdd[2];
  private SpectrumMachine machine;
  /** Whether the loader's Speedlock protection is looked for and worked around: the emulator's doing, not the machine's. */
  private boolean detectSpeedlock;
  /** What is fitted in each bay: the +3's own drive, and whatever the second is set to - the default is a double-sided eighty-track one. */
  private Fdd.Kind driveA;
  private Fdd.Kind driveB;

  @Inject
  public Upd765Peripheral(Scheduler scheduler, Cpu cpu, Fdd.Limits limits) {
    super(List.of());
    controller = new UpdFdc(UpdFdc.Type.UPD765A, UpdFdc.Rate.MHZ4, scheduler, cpu.getClock(), this::processorSpeed);
    for (int i = 0; i < drives.length; i++) {
      drives[i] = new Fdd(scheduler, cpu.getClock(), this::processorSpeed, limits);
      drives[i].disk.flag = Disk.FLAG_PLUS3_CPC;
    }
    // The +3 uses the US0 pin to select drives: drive 2 is drive 0, drive 3 is drive 1.
    controller.drive[0] = drives[0];
    controller.drive[1] = drives[1];
    controller.drive[2] = drives[0];
    controller.drive[3] = drives[1];
    drives[0].init(Fdd.Type.SHUGART, Fdd.Kind.SINGLE_SIDED_40, false);
    drives[1].init(Fdd.Type.SHUGART, null, false);
    // The drives are wired before the master reset, so that it selects the one the chip starts on.
    controller.masterReset();
    ports(Wired.at(0xf002, 0x3000, new FdcPortHandler(() -> controller)),
        Wired.at(0xf002, 0x2000, new FdcStatusPortHandler(() -> controller)),
        Wired.at(0xf002, 0x1000, new DefaultPortHandler(false, true) {
          /** The motor is bit 3 of the +3's own paging port, next to the printer strobe. */
          public void write(int port, byte value) {
            for (Fdd drive : drives) drive.motorOn((value & 0x08) != 0);
          }
        }));
  }

  private long processorSpeed() {
    return machine.getTimings().processorSpeed();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    this.machine = machine;
    controller.speedlock = detectSpeedlock ? 0 : -1;
  }

  @Override
  public void machineWasReset(boolean hard) {
    controller.masterReset();
    drives[0].init(Fdd.Type.SHUGART, driveA, true);
    drives[1].init(driveB != null && driveB.enabled ? Fdd.Type.SHUGART : Fdd.Type.NONE, driveB, true);
    controller.speedlock = detectSpeedlock ? 0 : -1;
  }

  public Fdd drive(int which) {
    return drives[which];
  }

  public boolean detectSpeedlock() {
    return detectSpeedlock;
  }

  public void setDetectSpeedlock(boolean detect) {
    detectSpeedlock = detect;
  }

  public Fdd.Kind driveA() {
    return driveA;
  }

  public void setDriveA(Fdd.Kind kind) {
    driveA = kind;
  }

  public Fdd.Kind driveB() {
    return driveB;
  }

  public void setDriveB(Fdd.Kind kind) {
    driveB = kind;
  }

}
