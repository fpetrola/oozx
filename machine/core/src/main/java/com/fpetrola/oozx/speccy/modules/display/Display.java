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
 * Plots the picture as the beam showed it. The screen memory says what to plot and the beam says
 * when: a cell is plotted from the memory as it was when the beam passed it, so a byte written
 * after that is seen next frame. That is what gives a cell more than two colours when a game
 * changes its attribute mid-frame, and what makes a border change start where the beam was.
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


  /** Where each line of the picture is in the bank that holds it: worked out once and its own thing. */
  public final ScreenLayout layout = new ScreenLayout();

  /** Frames into the flash, which turns over every sixteen and comes round every thirty-two. */
  private int framesIntoTheFlash;

  /** The picture as far as the beam has put it on the canvas. */
  public final Painting painting;

  /** Which cells still have to be put on the canvas: a frame plots what changed and not the screen. */
  public final DirtyCells dirty = new DirtyCells(HEIGHT);
  /** What colours an attribute asks for, and which way round the flash is. */
  public final Colouring colouring = new Colouring();

  /** The stripes around the picture, which share nothing with it but the canvas and the beam. */
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
    border = new Border(picture, new Border.BeamAt() {
      public int column() {
        return getBeamPosition().x;
      }

      public int row() {
        return getBeamPosition().y;
      }
    });
    refreshAll();
  }

  /** Everything is to be plotted again: the memory changed with no write, as a snapshot or another machine does. */
  public void refreshAll() {
    beamLineStart = Long.MIN_VALUE;
    dirty.all();
    border.refreshAll();
  }

  /** The picture is about to come from another page: what the beam showed of this one is plotted first. */
  public void screenChanging() {
    plotUpToTheBeam();
    dirty.all();
  }

  /**
   * A byte of the screen is about to change. Its cell is dirty, and if the beam already passed
   * the cell this frame, what it showed is plotted first, from the memory as it still is.
   */
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

  /** Plots what the beam has shown since the last plot, from the memory as it is now. */
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



  /** The line the beam was last found on, and where that line starts, counted from the first. */
  private int beamLine;
  private long beamLineStart = Long.MIN_VALUE;

  public BeamPosition getBeamPosition() {
    long tStates = z80Clock.getTStates();
    if (tStates < machine.get().current.lineStart(0)) {
      beam.x = beam.y = -1;
      return beam;
    }

    // Asked for every byte that changes on the screen, and a screen copy changes one every
    // twenty-one T-states: the line is the one it was a moment ago far more often than not, so
    // the division that finds it is paid once per line rather than once per byte.
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






  /** The frame is over: what the beam had left is plotted, the border is painted, and the flash counts on. */
  public void frame() {
    painting.upTo(WIDTH_COLS, HEIGHT - 1);
    painting.startAgain();
    border.paintTheFrame();
    framesIntoTheFlash = (framesIntoTheFlash + 1) & 31;
    if ((framesIntoTheFlash & 15) == 0) {
      colouring.reversed = framesIntoTheFlash == 16;
      dirtyEveryFlashingCell();
    }
  }

  /** The flash turned over: every cell that flashes has to be plotted again, changed or not. */
  private void dirtyEveryFlashingCell() {
    for (int row = 0; row < HEIGHT_ROWS; row++) {
      for (int column = 0; column < WIDTH_COLS; column++) {
        if (Colouring.flashes((byte) attribute(row * 8, column))) {
          for (int line = row * 8; line < row * 8 + 8; line++) dirty.cell(column, line);
        }
      }
    }
  }

  /** What the ULA fetches for a column of a pixel line, out of the bank it is showing. */
  public int pixels(int line, int column) {
    return banks.shown().bytes[layout.lineStart[line] + column] & 0xff;
  }

  public int attribute(int line, int column) {
    return banks.shown().bytes[layout.attrStart[line] + column] & 0xff;
  }
}
