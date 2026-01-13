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

import com.fpetrola.oozx.speccy.peripherals.PeripheralRegistry;
import com.fpetrola.oozx.speccy.modules.scheduler.Task;
import com.fpetrola.oozx.speccy.modules.scheduler.Scheduler;
import com.fpetrola.oozx.speccy.modules.timer.Timer;
import com.fpetrola.oozx.speccy.modules.display.Display;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.fpetrola.oozx.speccy.modules.memory.Rom;
import com.fpetrola.oozx.speccy.modules.sound.Sound;
import com.fpetrola.oozx.speccy.machine.AbstractSpectrumMachine;
import com.fpetrola.oozx.speccy.machine.MachineTimings;
import com.fpetrola.oozx.speccy.machine.SpectrumMachine;
import com.fpetrola.oozx.speccy.modules.z80.Cpu;
import com.fpetrola.z80.cpu.Z80Clock;

public abstract class Spectrum extends AbstractSpectrumMachine {
  protected final Roms roms;
  protected final MemoryBus memory;
  /** The memories the machine pages into the bus: they are the hardware's, and outlast the model. */
  protected final SpectrumMemory banks;
  protected final Display display;
  protected final PeripheralRegistry peripherals;
  private final Scheduler scheduler;
  private final Cpu cpu;
  protected final Z80Clock z80Clock;

  /**
   * What the ULA does to an access over the picture: the wait at each of the eight T-states of a
   * group, as they are heard, and where the first group starts relative to the first pixel.
   */
  public record Waits(int[] pattern, int from) {
    public static final Waits NONE = new Waits(null, 0);
    public static final Waits SIX_DOWN_TO_NOTHING = new Waits(new int[]{6, 5, 4, 3, 2, 1, 0, 0}, -1);
    public static final Waits ONE_THEN_SEVEN_DOWN_TO_TWO = new Waits(new int[]{1, 0, 7, 6, 5, 4, 3, 2}, -4);
  }

  private Task endOfFrame;

  private long frames;
  private final Timer timer;
  protected Sound sound;

  public Spectrum(MemoryBus memory, SpectrumMemory banks, Display display, Scheduler scheduler, Cpu cpu, Timer timer, PeripheralRegistry peripherals, Sound sound, Roms roms) {
    this.roms = roms;
    this.memory = memory;
    this.banks = banks;
    this.display = display;
    this.scheduler = scheduler;
    this.cpu = cpu;
    this.z80Clock = cpu.getClock();
    this.timer = timer;
    this.peripherals = peripherals;
    this.sound = sound;
  }

  /** This machine's ROM page, whichever bytes stand in for it outside. */
  public void loadRom(int pageNum, int expectedLength) {
    banks.rom(pageNum).fill(roms.of(this, pageNum, expectedLength));
  }

  private final class EndOfFrame extends Task {
    public void run(long due) {
      spectrumFrame();
      cpu.interrupt(getTimings().interruptLength());
    }
  }

  public long frameCount() {
    return frames;
  }

  /** This machine's own end of frame, registered the first time anyone needs it. */
  public Task endOfFrame() {
    if (endOfFrame == null) {
      endOfFrame = scheduler.register(new EndOfFrame());
    }
    return endOfFrame;
  }

  /** This machine's own end of frame: a whole frame of its clock, shown, and the next one due. */
  public void spectrumFrame() {
    frameEnded(getTimings().tstatesPerFrame());
    presentFrame();
    scheduler.schedule(endOfFrame, getTimings().tstatesPerFrame());
  }

  /**
   * A frame ended, after that many T-states: the clock and everything waiting on it go back by
   * them, so the next frame starts where the beam does.
   * <p>
   * The length is the machine's own except while a recording drives it: a recording says where
   * its frames ended and the machine's own count says nothing about them, so ending the frame on
   * that count instead puts the interrupt somewhere different in the picture every frame, and
   * every effect a game times from the interrupt walks up and down the screen.
   */
  public void frameEnded(int frameLength) {
    scheduler.frameEnded(frameLength);
    z80Clock.addTStates(-frameLength);
    frames++;
  }

  /** What a frame of the clock shows: the sound, how fast it is going, and the picture. */
  public void presentFrame() {
    sound.frame();
    timer.estimateSpeed();
    display.frame();
  }

  public boolean portFromUla(int port) {
    return (port & 0x0001) == 0;
  }

  /** When the beam reaches the first pixel, counted from the interrupt: a border's width of the line comes before it. */
  protected int firstPixel() {
    return (int) lineStart(display.BORDER_HEIGHT) + display.BORDER_WIDTH_COLS * 4;
  }

  /** How the ULA holds up an access to memory over the picture. Sinclair's count six down to nothing. */
  protected Waits waits() {
    return Waits.SIX_DOWN_TO_NOTHING;
  }

  /** The same for a cycle with no memory request, which on a Sinclair is no different. */
  protected Waits waitsWithoutMreq() {
    return waits();
  }

  public int contendDelay(long time) {
    return waitAt(time, waits());
  }

  public int contendDelayNoMreq(long time) {
    return waitAt(time, waitsWithoutMreq());
  }

  private int waitAt(long time, Waits waits) {
    if (waits.pattern() == null) return 0;
    long since = time - (firstPixel() + waits.from());
    int line = (int) Math.floorDiv(since, getTimings().tstatesPerLine());
    int into = (int) Math.floorMod(since, getTimings().tstatesPerLine());
    if (line < 0 || line >= display.HEIGHT || into >= getTimings().frame().line().picture()) return 0;
    return waits.pattern()[into % 8];
  }

  /**
   * What a read of a port nobody answers sees: the bus, and over the picture the bus is the
   * ULA's, carrying what it has just fetched - in every group of eight T-states a pixel byte and
   * its attribute for one column and then for the next, and nothing in the other four. Where the
   * bytes sit is the display's; this only asks for them by line and column.
   */
  public int unattachedPort(int port) {
    if (!hasFloatingBus()) return 0xff;
    long since = z80Clock.getTStates() - firstPixel();
    int line = (int) Math.floorDiv(since, getTimings().tstatesPerLine());
    int into = (int) Math.floorMod(since, getTimings().tstatesPerLine());
    if (line < 0 || line >= display.HEIGHT || into >= getTimings().frame().line().picture()) return 0xff;
    int column = (into / 8) * 2;
    return switch (into % 8) {
      case 2 -> display.pixels(line, column);
      case 3 -> display.attribute(line, column);
      case 4 -> display.pixels(line, column + 1);
      case 5 -> display.attribute(line, column + 1);
      default -> 0xff;
    };
  }

  /** The bits of the ULA's port that nothing drives read high after a write that lifted them, and low otherwise. */
  public byte ulaPortIdleValue(byte lastOut) {
    return (byte) ((lastOut & bitsThatLiftTheIdleValue()) != 0 ? 0xff : 0xbf);
  }

  /** Which bits of a write to the port lift its undriven bits: the speaker's, on a 128 and an issue 3. */
  protected int bitsThatLiftTheIdleValue() {
    return 0x10;
  }
}
