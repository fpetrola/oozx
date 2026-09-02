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
package com.fpetrola.oozx.speccy.devices.plusd;

import com.fpetrola.oozx.Machine;
import com.fpetrola.oozx.MachineCapability;
import com.fpetrola.oozx.Memory;
import com.fpetrola.oozx.Module;
import com.fpetrola.oozx.Settings;
import com.fpetrola.oozx.speccy.devices.parallelprinter.ParallelPrinterPeripheral;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.EventManager;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.ports.DefaultPortHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The +D: the WD1770 on ports 0xe3/0xeb/0xf3/0xfb, paged by the ROM's hooks - the error restart,
 * the NMI, and the keyboard scan the ROM calls every interrupt - and by port 0xe7. Ported from
 * Fuse's plusd.c.
 */
@Singleton
public class PlusDPeripheral extends MgtDiskInterface {

  private static final int[] HOOKS = {0x0008, 0x003a, 0x0066, 0x028e};

  @Inject
  public PlusDPeripheral(Memory memory, Module module, Cpu cpu, Settings settings, EventManager events,
                         Machine machine, ParallelPrinterPeripheral printer) {
    super(memory, module, cpu, settings, events, machine, printer);
    ports(fdcRegister(0xe3, FdcRegister.STATUS_COMMAND),
        fdcRegister(0xeb, FdcRegister.TRACK),
        fdcRegister(0xf3, FdcRegister.SECTOR),
        fdcRegister(0xfb, FdcRegister.DATA),
        new DefaultPortHandler(0x00ff, 0x00ef, false, true) {
          public void write(int port, byte value) {
            control(value & 0xff);
          }
        },
        patchPort(0xe7),
        new DefaultPortHandler(0x00ff, 0x00f7, true, true) {
          public byte read(int port, byte[] attached) {
            attached[0] = (byte) 0xff;
            return (byte) (printerAttached() ? 0x7f : 0xff);
          }

          public void write(int port, byte value) {
            printerDataPort(0xf7).write(port, value);
          }
        });
  }

  @Override
  protected int[] hooks() {
    return HOOKS;
  }

  @Override
  public String romName() {
    return settings.current.romPlusd;
  }

  @Override
  protected String defaultRomName() {
    return settings.defaults.romPlusd;
  }

  /** Fuse leaves it active but not paged after a reset: the first hook pages it in. */
  @Override
  protected boolean pagedAtReset() {
    return false;
  }

  @Override
  protected void reset(boolean hard) {
  }

  @Override
  protected void mapPages() {
    memory.mapRomcs8k(0x0000, rom);
    memory.mapRomcs8k(0x2000, ram);
  }

  /** Bits 0-1 which drive (only 2 is the second), bit 7 the side, bit 6 the printer's strobe. */
  @Override
  protected void control(int b) {
    controlRegister = b;
    selectDrive((b & 0x03) == 2 ? 1 : 0, (b & 0x80) != 0 ? 1 : 0);
    strobe((b & 0x40) != 0);
  }

  /** The Sinclair machines with an edge connector that has /ROMCS: not a +2A or +3, not a clone. */
  @Override
  public boolean fitsOn(SpectrumMachine machine) {
    return !machine.has(MachineCapability.PLUS3_MEMORY) && !machine.fullyDecodesPorts();
  }
}
