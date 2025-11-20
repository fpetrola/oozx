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

/** +3 floppy interface: uPD765A plus two drives, on +3-specific ports with motor control on the paging port. */
@Singleton
public class Upd765Peripheral extends AbstractPeripheral {
  private final UpdFdc controller;
  private final Fdd[] drives = new Fdd[2];
  private SpectrumMachine machine;
  /** Enables the Speedlock protection workaround; an emulator-side option, not hardware behaviour. */
  private boolean detectSpeedlock;
  /** Drive configuration per bay; drive A is fixed, drive B defaults to double-sided 80-track. */
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
    // Only the low select bit is wired, so select values 2 and 3 alias to drives 0 and 1.
    controller.drive[0] = drives[0];
    controller.drive[1] = drives[1];
    controller.drive[2] = drives[0];
    controller.drive[3] = drives[1];
    drives[0].init(Fdd.Type.SHUGART, Fdd.Kind.SINGLE_SIDED_40, false);
    drives[1].init(Fdd.Type.SHUGART, null, false);
    // Drives must be connected before masterReset, since reset selects the chip's starting drive.
    controller.masterReset();
    ports(Wired.at(0xf002, 0x3000, new FdcPortHandler(() -> controller)),
        Wired.at(0xf002, 0x2000, new FdcStatusPortHandler(() -> controller)),
        Wired.at(0xf002, 0x1000, new DefaultPortHandler(false, true) {
          /** Motor control uses bit 3 of the +3 paging port (0x1ffd), alongside the printer strobe. */
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
