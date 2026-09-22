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

import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.display.Colouring;
import com.fpetrola.oozx.speccy.modules.display.Painting;
import com.fpetrola.oozx.speccy.modules.display.Picture;
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
    Painting.Line wanted = sixteen ? sixteenColours : display.painting.sinclair;
    if (display.painting.line(wanted) != wanted) display.refreshAll();
  }

  /**
   * How this machine paints while it is showing sixteen colours: four bytes of the column read
   * from the two banks at once, each one a colour for its left pixel and another for its right,
   * so eight pixels are eight colours and nothing in memory is an attribute.
   */
  /** Both halves of the screen are bitmap here, read from two banks, so nothing is an attribute. */
  private final Painting.WithoutAttributes sixteenColours = this::paintSixteenColours;

  private void paintSixteenColours(int y, int bits) {
    byte[] screen = banks.shown().bytes, other = banks.beside().bytes;
    Picture canvas = display.picture();
    int row = (y + Display.BORDER_HEIGHT) * Picture.STRIDE;
    for (; bits != 0; bits &= bits - 1) {
      int x = Integer.numberOfTrailingZeros(bits);
      int at = display.layout.pixelsAt(y, x), above = display.layout.secondByteAt(y, x);
      int pixel = row + (x + Display.BORDER_WIDTH_COLS) * 8;
      plotColours(canvas, pixel, other[at]);
      plotColours(canvas, pixel + 2, screen[at]);
      plotColours(canvas, pixel + 4, other[above]);
      plotColours(canvas, pixel + 6, screen[above]);
    }
  }

  /** A byte that is not a bitmap but two colours: the same bits an attribute puts its ink and paper in. */
  private void plotColours(Picture canvas, int at, byte colours) {
    canvas.pixels[at] = canvas.palette[Colouring.inkBits(colours) & 0xff];
    canvas.pixels[at + 1] = canvas.palette[Colouring.paperBits(colours) & 0xff];
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
