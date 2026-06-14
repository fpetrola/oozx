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

package com.fpetrola.oozx.speccy.devices.interface1;

import com.fpetrola.oozx.speccy.ports.Wired;
import com.fpetrola.oozx.speccy.machine.Roms;
import com.fpetrola.oozx.speccy.ports.BusAnswer;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.machine.RomNotLoadedException;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.modules.z80.PcTraps;
import com.fpetrola.oozx.speccy.peripherals.PluggablePeripheral;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface 1: an 8K shadow ROM mirrored twice in the bottom 16K, paged in at 0x0008/0x1708 and
 * out after its own RET at 0x0700; up to 8 Microdrives chained on a motor shift register; one
 * ULA behind 3 ports decoded by bits 3-4 only (0xe7 head data, 0xef control/status, 0xf7
 * RS232/ZX-Net bit-serial). Entirely port-driven, with no clock of its own.
 */
@Singleton
public class Interface1Peripheral extends PluggablePeripheral {
  private final Roms roms;

  public static final int ROM_SIZE = 0x2000;
  public static final int DRIVES = 8;
  private static final int PORT_MASK = 0x0018;
  private static final int PORT_DATA = 0x0000;
  private static final int PORT_CONTROL = 0x0008;
  private static final int PORT_COMMS = 0x0010;
  private static final int[] HOOKS = {0x0008, 0x1708};
  private static final int LEAVING = 0x0700;

  private final MemoryBus memory;
  private final Cpu cpu;
  private int microdriveSectors;
  private boolean randomMicrodriveLength;
  private boolean rs232Handshake;
  private boolean rawNetwork;
  private final Rom rom;
  /** Two mirrored mappings of the 8K ROM across the bottom 16K. */
  private final MappedMemory[] held;
  private final Microdrive[] drives = new Microdrive[DRIVES];
  private final Rs232 rs232;
  private final ZxNet net = new ZxNet();
  private final List<PcTraps.Watch> watches = new ArrayList<>();

  private SpectrumMachine on;
  private boolean available;
  private boolean paged;
  private boolean commsClock;
  private boolean commsData;

  @Inject
  public Interface1Peripheral(MemoryBus memory, Cpu cpu, Roms roms) {
    super(List.of());
    this.roms = roms;
    this.memory = memory;
    this.cpu = cpu;
    rom = new Rom(ROM_SIZE);
    held = new MappedMemory[]{new MappedMemory(0x0000, rom), new MappedMemory(0x2000, rom)};
    rs232 = new Rs232(this);
    for (int i = 0; i < DRIVES; i++) {
      drives[i] = new Microdrive();
    }
    ports(port(PORT_DATA), port(PORT_CONTROL), port(PORT_COMMS));
  }

  private Wired port(int which) {
    return Wired.at(PORT_MASK, which, new DefaultPortHandler(true, true) {
      public BusAnswer read(int port) {
        return BusAnswer.of(switch (which) {
          case PORT_DATA -> dataIn();
          case PORT_CONTROL -> statusIn();
          default -> commsIn();
        });
      }

      public void write(int port, byte value) {
        switch (which) {
          case PORT_DATA -> dataOut(value & 0xff);
          case PORT_CONTROL -> controlOut(value & 0xff);
          default -> commsOut(value & 0xff);
        }
      }
    });
  }

  /** All running drives' bytes combine by AND on the shared bus, matching the real wiring. */
  private int dataIn() {
    int data = 0xff;
    for (Microdrive drive : drives) {
      data &= drive.read();
    }
    return data;
  }

  private void dataOut(int value) {
    for (Microdrive drive : drives) {
      drive.write(value);
    }
  }

  /** Control/status byte: bit 0 write-protect, bits 1-2 sync/gap, bit 3 DTR, bit 4 busy (unused). */
  private int statusIn() {
    int status = 0xff;
    for (Microdrive drive : drives) {
      status &= drive.status();
    }
    rs232.poll();
    if (rs232.dtr == 0) {
      status &= 0xf7;
    }
    status &= 0xef;
    restart();
    return status;
  }

  /** Control write: bit 0 data, bit 1 clock (falling edge shifts motors, data low starts drive 1), bit 4 CTS. */
  private void controlOut(int value) {
    if ((value & 0x02) == 0 && commsClock) {
      for (int m = DRIVES - 1; m > 0; m--) {
        drives[m].motorOn = drives[m - 1].motorOn;
      }
      drives[0].motorOn = (value & 0x01) == 0;
    }
    if ((value & 0x01) != 0 && !commsData) {
      rs232.restartFraming();
    }
    commsData = (value & 0x01) != 0;
    commsClock = (value & 0x02) != 0;
    rs232.cts((value & 0x10) != 0 ? 1 : 0);
    restart();
  }

  /** Comms read: bit 7 RS232 receive line, bit 0 ZX Net wire. */
  private int commsIn() {
    int comms = 0xff;
    if (rs232.lineIn() == 0) {
      comms &= 0x7f;
    }
    if (net.lineIn() == 0) {
      comms &= 0xfe;
    }
    restart();
    return comms;
  }

  /** Comms write: bit 0 routed to RS232 when the data line is high, to ZX Net when low. */
  private void commsOut(int value) {
    if (commsData) {
      rs232.lineOut(value);
    } else {
      net.lineOut(value);
    }
    restart();
  }

  private void restart() {
    for (Microdrive drive : drives) {
      drive.restart();
    }
  }

  @Override
  public boolean hasHardReset() {
    return true;
  }

  /** Sinclair models whose edge connector exposes /ROMCS, needed to fit an Interface 1. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.pagesThrough1ffd() && !machine.fullyDecodesPorts();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    for (int hook : HOOKS) {
      watches.add(cpu.beforeFetch().watch(hook, pc -> page()));
    }
    watches.add(cpu.afterInstruction().watch(LEAVING, pc -> unpage()));
  }

  @Override
  public void deactivate() {
    watches.forEach(PcTraps.Watch::off);
    watches.clear();
    unpage();
    available = false;
    on = null;
  }

  @Override
  public void machineWasReset(boolean hard) {
    paged = false;
    available = false;
    if (on == null) {
      return;
    }
    try {
      rom.fill(roms.of(this, ROM_SIZE));
    } catch (RomNotLoadedException missing) {
      return;
    }
    unpage();
    rs232.reset();
    net.reset();
    commsClock = commsData = false;
    for (Microdrive drive : drives) {
      drive.reset();
    }
    available = true;
  }

  private void page() {
    if (!available) {
      return;
    }
    paged = true;
    memory.plug(held);
  }

  private void unpage() {
    paged = false;
    memory.unplug(held);
  }


  public boolean isAvailable() {
    return available;
  }

  public boolean isPaged() {
    return paged;
  }

  public boolean motorOn(int which) {
    return drives[which].motorOn;
  }

  public boolean inserted(int which) {
    return drives[which].inserted;
  }

  public boolean writeProtected(int which) {
    return drives[which].writeProtected;
  }

  public boolean modified(int which) {
    return drives[which].modified;
  }

  public String cartridgeName(int which) {
    return drives[which].filename;
  }

  public int sectors(int which) {
    return drives[which].sectors();
  }

  public void insert(int which, File cartridge) throws IOException {
    drives[which].insert(cartridge);
  }

  /** A blank Microdrive cartridge sized per configuration, or the historical default length. */
  public void insertBlank(int which) {
    int sectors = randomMicrodriveLength ? Microdrive.randomLength()
        : Math.max(Microdrive.MIN_SECTORS, Math.min(Microdrive.MAX_SECTORS, microdriveSectors));
    drives[which].insertBlank(sectors);
  }

  public void eject(int which) {
    drives[which].eject();
  }

  public void save(int which, File file) throws IOException {
    drives[which].save(file);
  }

  public void writeProtect(int which, boolean on) {
    drives[which].writeProtect(on);
  }

  public Rs232 rs232() {
    return rs232;
  }

  public ZxNet net() {
    return net;
  }

  public int microdriveSectors() {
    return microdriveSectors;
  }

  public void setMicrodriveSectors(int microdriveSectors) {
    this.microdriveSectors = microdriveSectors;
  }

  public boolean randomMicrodriveLength() {
    return randomMicrodriveLength;
  }

  public void setRandomMicrodriveLength(boolean randomMicrodriveLength) {
    this.randomMicrodriveLength = randomMicrodriveLength;
  }

  public boolean rs232Handshake() {
    return rs232Handshake;
  }

  public void setRs232Handshake(boolean rs232Handshake) {
    this.rs232Handshake = rs232Handshake;
  }

  public boolean rawNetwork() {
    return rawNetwork;
  }

  public void setRawNetwork(boolean rawNetwork) {
    this.rawNetwork = rawNetwork;
  }
}
