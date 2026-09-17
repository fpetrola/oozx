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


package com.fpetrola.oozx.speccy.machine;

import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Painting;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A Pentagon with a megabyte, which it reaches by reading four more bits of the port it already
 * writes and by adding a second port of its own.
 * <p>
 * That second port also decides what the first one means: with one of its bits the machine becomes
 * a later revision, where the page is only the three low bits again and the bit that used to be
 * part of it goes back to being the lock a 128 has.
 */
@Singleton
public class Pentagon1024 extends Pentagon512 {
  /** The later revision, in which the port is read as a 128 reads it. */
  private static final int REVISED = 0x04;
  /** RAM at the bottom, where the ROM usually is. */
  private static final int RAM_BELOW = 0x08;
  /** Sixteen colours at once, out of four bytes read from two banks. */
  private static final int SIXTEEN_COLOURS = 0x01;

  private byte second;
  private boolean locked;

  @Inject
  public Pentagon1024(MemoryBus memory, SpectrumMemory banks, Display display, PeripheralRegistry peripherals,
                      Roms roms, Scheduler scheduler, Cpu cpu, Timer timer, Sound sound) {
    super(memory, banks, display, peripherals, roms, scheduler, cpu, timer, sound);
  }

  /** The byte its own port was last written, which says how the other one is read. */
  public byte second() {
    return second;
  }

  public void secondPortWrite(byte b) {
    if (locked) return;
    second = b;
    memoryMap();
  }

  /**
   * Until it is told it is the later revision, the bit a 128 locks itself with is part of the page
   * number here, so there is nothing to refuse and every write goes through.
   */
  @Override
  public void memoryPortWrite(int port, byte b) {
    if (locked) return;
    paging.latch7ffd(b);
    if ((second & REVISED) != 0) locked = (b & 0x20) != 0;
    memoryMap();
  }



  @Override
  protected int pageAt(int slot) {
    if (slot != 3) return super.pageAt(slot);
    int port = paging.port7ffd() & 0xff;
    if ((second & REVISED) != 0) return port & 0x07;
    return (port & 0x07) + ((port & 0xc0) >> 3) + (port & 0x20);
  }

  /**
   * The picture of this mode is built out of the shown bank and the one below it - five with four,
   * seven with six - so the machine says which second bank is being read and how a column is made,
   * and the drawing knows nothing about which machine asked for it.
   */
  @Override
  public void memoryMap() {
    super.memoryMap();
    if ((second & RAM_BELOW) != 0) memory.slot(0x0000, banks.ram(0));
    boolean sixteen = (second & SIXTEEN_COLOURS) != 0;
    banks.alongside(sixteen ? banks.ram(banks.shown().pageNum - 1) : null);
    Painting.Line wanted = sixteen ? display.painting.fourBytesToAColumn : display.painting.sinclair;
    if (display.painting.line(wanted) != wanted) display.refreshAll();
  }

  @Override
  public int reset() {
    second = 0;
    locked = false;
    display.painting.line(null);
    return super.reset();
  }

  @Override
  public java.util.Set<Class<? extends com.fpetrola.oozx.speccy.peripherals.Peripheral>> onBoard() {
    java.util.Set<Class<? extends com.fpetrola.oozx.speccy.peripherals.Peripheral>> board =
        new java.util.HashSet<>(super.onBoard());
    board.add(Pentagon1024MemoryPeripheral.class);
    return board;
  }

  @Override
  public String shortName() {
    return "Pentagon1024";
  }

  @Override
  public String getName() {
    return "Pentagon 1024K";
  }
}
