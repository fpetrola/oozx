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

package com.fpetrola.oozx.speccy.devices.interface1;

import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.MemoryPage;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.RomNotLoadedException;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.RomcsDevice;
import com.fpetrola.oozx.speccy.modules.ZxModule;
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
 * The Interface 1: an 8K shadow ROM, seen twice in the bottom 16K, that comes in when the
 * processor reaches 0x0008 or 0x1708 of the machine's and goes out after the RET at 0x0700 of
 * its own; up to eight Microdrives on a shift register of motors; and one ULA behind three
 * ports, decoded by bits 3 and 4 alone - 0xe7 the byte under the head, 0xef control and status,
 * 0xf7 the RS232 and the ZX Net a bit at a time. Nothing in it has a clock: it all moves with
 * the ports. From Fuse's if1.c.
 */
@Singleton
public class Interface1Peripheral extends PluggablePeripheral implements ZxModule, RomcsDevice {

  public static final int ROM_SIZE = 0x2000;
  public static final int DRIVES = 8;
  private static final int PORT_MASK = 0x0018;
  private static final int PORT_DATA = 0x0000;
  private static final int PORT_CONTROL = 0x0008;
  private static final int PORT_COMMS = 0x0010;
  private static final int[] HOOKS = {0x0008, 0x1708};
  private static final int LEAVING = 0x0700;

  private final Memory memory;
  private final Module module;
  private final Cpu cpu;
  private final Settings settings;
  private final MemoryPage[] rom;
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
  public Interface1Peripheral(Memory memory, Module module, Cpu cpu, Settings settings) {
    super(List.of());
    this.memory = memory;
    this.module = module;
    this.cpu = cpu;
    this.settings = settings;
    rom = memory.newRomBank();
    rs232 = new Rs232(settings);
    for (int i = 0; i < DRIVES; i++) {
      drives[i] = new Microdrive();
    }
    ports(port(PORT_DATA), port(PORT_CONTROL), port(PORT_COMMS));
  }

  private DefaultPortHandler port(int which) {
    return new DefaultPortHandler(PORT_MASK, which, true, true) {
      public byte read(int port, byte[] attached) {
        attached[0] = (byte) 0xff;
        return (byte) switch (which) {
          case PORT_DATA -> dataIn();
          case PORT_CONTROL -> statusIn();
          default -> commsIn();
        };
      }

      public void write(int port, byte value) {
        switch (which) {
          case PORT_DATA -> dataOut(value & 0xff);
          case PORT_CONTROL -> controlOut(value & 0xff);
          default -> commsOut(value & 0xff);
        }
      }
    };
  }

  /** Every turning drive puts its byte on the bus at once; that is the hardware's AND. */
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

  /** Bit 0 write protect, bits 1-2 sync and gap, bit 3 DTR, bit 4 busy, which nothing ever raises. */
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

  /** Bit 0 data, bit 1 clock - its fall shifts the motors along, the first one on if data is low - bit 4 CTS. */
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

  /** Bit 7 what the RS232 is receiving, bit 0 the net wire. */
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

  /** Bit 0 goes to the RS232 while the comms data line is up, to the net while it is down. */
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

  /** The Sinclair machines with an edge connector that has /ROMCS. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }

  @Override
  public void activate(SpectrumMachine machine) {
    on = machine;
    module.register(this);
    for (int hook : HOOKS) {
      watches.add(cpu.beforeFetch().watch(hook, pc -> page()));
    }
    watches.add(cpu.afterInstruction().watch(LEAVING, pc -> unpage()));
  }

  @Override
  public void deactivate() {
    module.unregister(this);
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
      memory.loadRomBank(rom, 0, romName(), ROM_SIZE, !romName().equals(settings.defaults.romInterface1));
    } catch (RomNotLoadedException missing) {
      return;
    }
    on.getRamInfo().romcs = false;
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
    on.getRamInfo().romcs = true;
    on.memoryMap();
  }

  private void unpage() {
    paged = false;
    if (on != null) {
      on.getRamInfo().romcs = false;
      on.memoryMap();
    }
  }

  @Override
  public void mapRom() {
    if (paged) {
      memory.mapRomcs8k(0x0000, rom);
      memory.mapRomcs8k(0x2000, rom);
    }
  }

  public String romName() {
    return settings.current.romInterface1;
  }

  public boolean isAvailable() {
    return available;
  }

  public boolean isPaged() {
    return paged && on != null && on.getRamInfo().romcs;
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

  /** A blank cartridge, as long as the settings say or as long as they happened to come. */
  public void insertBlank(int which) {
    int sectors = settings.current.mdrRandomLen ? Microdrive.randomLength()
        : Math.max(Microdrive.MIN_SECTORS, Math.min(Microdrive.MAX_SECTORS, settings.current.mdrLen));
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

  @Override
  public void start() {
  }

  @Override
  public void end() {
  }
}
