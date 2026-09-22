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

package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.emulation.helpers.machine.MachineTypes;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.fpetrola.oozx.speccy.modules.memory.MappedMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.Ram;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The Spectrum Investronica built in Spain, which is a 48K in the way a copy of a drawing is the
 * drawing: everything is where it should be and nothing is quite right.
 * <p>
 * It has sixty-four K of memory rather than forty-eight, and the ROM sits over the first sixteen
 * of it: a program reads the ROM there and writes underneath it. Its chip holds nothing up, so
 * there is no contention anywhere. A port nobody answers reads as all ones, because nothing of the
 * picture is left on the bus. What is written to the chip that draws is put through whatever was
 * lying in memory at that same address first. And on every interrupt it accepted it wrote a byte
 * of ones into memory, where the address of the next one is kept - which is a fault, and is why
 * games that put their interrupt table where that byte lands come apart on this machine and
 * nowhere else.
 * <p>
 * Two of its faults are not here: the speaker follows the two bits together rather than the one,
 * and its joystick interface answers on fewer address lines than a Kempston does.
 */
@Singleton
public class Inves extends Spec48 {
  /** Its line is four T-states longer than a Sinclair's, so its picture starts that much later. */
  private static final MachineTimings.Frame FRAME = new MachineTimings.Frame(
      new MachineTimings.Span(24, 128, 24, 52), new MachineTimings.Span(48, 192, 48, 24), 32, 228 * 64);

  private static final MachineTimings TIMINGS = new MachineTimings(3500000, FRAME);

  /** The sixteen K under the ROM, which is a page no 48K has anything in. */
  private static final int UNDER_THE_ROM = 1;

  /** Which page each sixteen K of the address space is, the one under the ROM included. */
  private static final int[] PAGES = {UNDER_THE_ROM, 5, 2, 0};

  @Inject
  public Inves(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals, Machine.Unit unit,
               Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, unit, roms, scheduler, cpu, timer, sound);
  }

  /** The fault is a part of this machine, so it is in while this machine is and not a moment longer. */
  @Override
  public java.util.Set<Class<? extends com.fpetrola.oozx.speccy.peripherals.Peripheral>> onBoard() {
    java.util.Set<Class<? extends com.fpetrola.oozx.speccy.peripherals.Peripheral>> board =
        new java.util.HashSet<>(super.onBoard());
    board.add(InvesInterruptFault.class);
    return board;
  }

  @Override
  public int reset() {
    int result = super.reset();
    for (int page = 0; page < 8; page++) {
      banks.ram(page).contended = false;
    }
    // The ROM reads where it always did; what is written there goes to the memory underneath it,
    // which is what having sixteen K more than a 48K amounts to on this machine.
    memory.plug(new MappedMemory(0x0000, banks.ram(UNDER_THE_ROM), 0, 0x4000, false, true));
    return result;
  }

  /** Whatever is written to the chip that draws goes through what is in memory at that address. */
  @Override
  public byte asWrittenToTheUla(int port, byte value) {
    Ram page = banks.ram(PAGES[(port >>> 14) & 3]);
    return (byte) (value & page.bytes[port & 0x3fff]);
  }

  /** Nothing of the picture is ever on its bus, so a port nobody answers is all ones. */
  @Override
  public boolean hasFloatingBus() {
    return false;
  }

  @Override
  public MachineTimings getTimings() {
    return TIMINGS;
  }

  @Override
  public String shortName() {
    return "Inves";
  }

  @Override
  public MachineTypes snapshotModel() {
    return null;
  }

  @Override
  public String getName() {
    return "Inves Spectrum+";
  }
}
