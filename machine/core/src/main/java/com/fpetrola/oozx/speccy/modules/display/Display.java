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

package com.fpetrola.oozx.speccy.modules.display;

import com.fpetrola.oozx.speccy.modules.machine.Machine;
import com.google.inject.Provider;
import com.google.common.base.Suppliers;
import java.util.function.Supplier;
import com.fpetrola.oozx.speccy.modules.memory.SpectrumMemory;
import com.fpetrola.oozx.speccy.modules.memory.MemoryBus;
import com.google.inject.Singleton;
import com.google.inject.Inject;

import com.fpetrola.oozx.*;
import com.fpetrola.z80.cpu.Z80Clock;

import java.util.ArrayList;
import java.util.List;

/**
 * Plots a cell from memory as it was when the beam passed over it, not as it is now — so a mid-frame
 * attribute change only takes effect next frame (this is how a game gets more than two colours per cell),
 * and a border change takes effect exactly where the beam was when the port was written.
 */
@Singleton
public class Display {
  private final MemoryBus memory;
  private final SpectrumMemory banks;

  static final int WIDTH_COLS = 32;
  private static final int HEIGHT_ROWS = 24;

  public static final int HEIGHT = HEIGHT_ROWS * 8;

  public static final int BORDER_WIDTH_COLS = 4;
  private static final int BORDER_HEIGHT_COLS = 3;

  public static final int BORDER_HEIGHT = BORDER_HEIGHT_COLS * 8;

  public static final int SCREEN_HEIGHT = HEIGHT + 2 * BORDER_HEIGHT;
  public static final int SCREEN_WIDTH_COLS = WIDTH_COLS + 2 * BORDER_WIDTH_COLS;


  public final ScreenLayout layout = new ScreenLayout();

  /** Flash flips every 16 frames, cycle repeats every 32. */
  private int framesIntoTheFlash;

  public final Painting painting;
  public final DirtyCells dirty = new DirtyCells(HEIGHT);
  public final Colouring colouring = new Colouring();
  public final Border border;
  private final Z80Clock z80Clock;
  private final Picture picture;
  private final BeamPosition beam;
  private final Supplier<Machine> machine;

  @Inject
  public Display(MemoryBus memory, SpectrumMemory banks, Z80Clock z80Clock, Picture picture, Provider<Machine> machine) {
    this.machine = Suppliers.memoize(machine::get);
    this.banks = banks;
    this.memory = memory;
    this.z80Clock = z80Clock;
    this.picture = picture;
    beam = new BeamPosition();
    painting = new Painting(banks, layout, dirty, colouring, picture);
    border = new Border(picture, colouring, new Border.BeamAt() {
      public int column() {
        return getBeamPosition().x;
      }

      public int row() {
        return getBeamPosition().y;
      }
    });
    refreshAll();
  }

  public Picture picture() {
    return picture;
  }

  public void refreshAll() {
    beamLineStart = Long.MIN_VALUE;
    dirty.all();
    border.refreshAll();
  }

  public void screenChanging() {
    plotUpToTheBeam();
    dirty.all();
  }

  /** If the beam already passed this cell this frame, plots its old contents before marking it dirty. */
  public void screenWritten(int offset) {
    int column = layout.columnOf(offset);
    if (offset < ScreenLayout.ATTRIBUTES) {
      dirtyCell(column, layout.lineOf(offset));
    } else {
      int first = layout.attributeRowOf(offset) * 8;
      for (int line = first; line < first + 8; line++) {
        dirtyCell(column, line);
      }
    }
  }

  private void dirtyCell(int x, int y) {
    if (painting.past(x, y)) plotUpToTheBeam();
    dirty.cell(x, y);
  }

  private void plotUpToTheBeam() {
    BeamPosition beam = getBeamPosition();
    int x = beam.x - BORDER_WIDTH_COLS, y = beam.y - BORDER_HEIGHT;
    if (y < 0) {
      x = y = 0;
    } else if (y >= HEIGHT) {
      x = WIDTH_COLS;
      y = HEIGHT - 1;
    } else {
      x = Math.max(0, Math.min(x, WIDTH_COLS));
    }
    painting.upTo(x, y);
  }



  private int beamLine;
  private long beamLineStart = Long.MIN_VALUE;

  public BeamPosition getBeamPosition() {
    long tStates = z80Clock.getTStates();
    if (tStates < machine.get().current.lineStart(0)) {
      beam.x = beam.y = -1;
      return beam;
    }

    // Called once per changed byte (as often as every 21 T-states), but the line rarely changes between calls,
    // so the division is cached and only redone when tStates falls outside the cached line.
    long intoFrame = tStates - machine.get().current.lineStart(0);
    int tstatesPerLine = machine.get().current.getTimings().tstatesPerLine();
    if (intoFrame < beamLineStart || intoFrame >= beamLineStart + tstatesPerLine) {
      beamLine = (int) (intoFrame / tstatesPerLine);
      beamLineStart = (long) beamLine * tstatesPerLine;
    }
    beam.y = beamLine;

    if (beam.y >= 0 && beam.y <= SCREEN_HEIGHT) {
      beam.x = (int) ((tStates - machine.get().current.lineStart(beam.y)) / 4);
    } else {
      beam.x = 0;
    }
    return beam;
  }






  public void frame() {
    painting.upTo(WIDTH_COLS, HEIGHT - 1);
    painting.startAgain();
    border.paintTheFrame();
    framesIntoTheFlash = (framesIntoTheFlash + 1) & 31;
    if ((framesIntoTheFlash & 15) == 0) {
      colouring.reversed(framesIntoTheFlash == 16);
      dirtyEveryFlashingCell();
    }
  }

  private void dirtyEveryFlashingCell() {
    if (!painting.cellsCanFlash()) return;
    for (int row = 0; row < HEIGHT_ROWS; row++) {
      for (int column = 0; column < WIDTH_COLS; column++) {
        if (Colouring.flashes((byte) attribute(row * 8, column))) {
          for (int line = row * 8; line < row * 8 + 8; line++) dirty.cell(column, line);
        }
      }
    }
  }

  public int pixels(int line, int column) {
    return banks.shown().bytes[layout.pixelsAt(line, column)] & 0xff;
  }

  public int attribute(int line, int column) {
    return banks.shown().bytes[layout.colourAt(line, column)] & 0xff;
  }
}
