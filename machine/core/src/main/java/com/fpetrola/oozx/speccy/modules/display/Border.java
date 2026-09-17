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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.fpetrola.oozx.speccy.modules.display.Display.BORDER_HEIGHT;
import static com.fpetrola.oozx.speccy.modules.display.Display.BORDER_WIDTH_COLS;
import static com.fpetrola.oozx.speccy.modules.display.Display.HEIGHT;
import static com.fpetrola.oozx.speccy.modules.display.Display.SCREEN_HEIGHT;
import static com.fpetrola.oozx.speccy.modules.display.Display.SCREEN_WIDTH_COLS;
import static com.fpetrola.oozx.speccy.modules.display.Display.WIDTH_COLS;

/**
 * The single-colour border, changed by port writes at arbitrary beam positions (stripe effects).
 * Records each change and the beam position when it happened, then paints the bands between changes at frame end.
 */
public final class Border {
  private final Picture picture;
  private final BeamAt beam;
  private byte colour;
  private byte lastPlotted;

  public interface BeamAt {
    int column();

    int row();
  }

  private final Colouring colouring;

  public Border(Picture picture, Colouring colouring, BeamAt beam) {
    this.colouring = colouring;
    this.picture = picture;
    this.beam = beam;
    addSentinel();
  }
  private static class Change {
    int x, y, colour;
  }

  private final List<Change> changes = new ArrayList<>();

  /** Count of live entries in {@link #changes}; the tail is last frame's objects, kept to avoid reallocating. */
  private int used;

  private void addChange(int x, int y, int colour) {
    if (used == changes.size()) {
      changes.add(new Change());
    }
    Change change = changes.get(used++);
    change.x = x;
    change.y = y;
    change.colour = colour;
  }

  private void addSentinel() {
    addChange(0, 0, colour);
  }

  private void pushChange(int colour) {
    if (beam.row() >= SCREEN_HEIGHT) return;
    addChange(Math.max(0, Math.min(beam.column(), SCREEN_WIDTH_COLS)), Math.max(0, beam.row()), colour);
  }

  public void becomes(int wanted) {
    colour = (byte) wanted;
    if (colour != lastPlotted) {
      pushChange(colour);
      lastPlotted = colour;
    }
  }

  /**
   * The border is a cell with nothing but paper in it, so which colour that is gets asked the way
   * a cell asks: a machine that reads a byte's paper differently borders itself differently too,
   * and here that question is already answered.
   */
  public void paint(int y, int start, int end, int colour) {
    byte paper = colouring.paper((byte) ((colour & 0x07) << 3));
    for (; start < end; start++) {
      picture.fillColumn(start, y, paper);
    }
  }

  private void paintRow(int y, int start, int end, int colour) {
    boolean whole = start == 0 && end == SCREEN_WIDTH_COLS;
    if (whole && rowPlotted[y] == colour) return;
    rowPlotted[y] = whole ? colour : -1;
    if (y < BORDER_HEIGHT || y >= BORDER_HEIGHT + HEIGHT) {
      paint(y, start, end, colour);
      return;
    }
    if (start < BORDER_WIDTH_COLS) {
      paint(y, start, Math.min(end, BORDER_WIDTH_COLS), colour);
    }
    if (end > BORDER_WIDTH_COLS + WIDTH_COLS) {
      paint(y, Math.max(start, BORDER_WIDTH_COLS + WIDTH_COLS), end, colour);
    }
  }

  private void doChange(Change first, Change second) {
    if (first.x != 0) {
      if (first.x != SCREEN_WIDTH_COLS) {
        paintRow(first.y, first.x, SCREEN_WIDTH_COLS, first.colour);
      }
      if (first.y < SCREEN_HEIGHT - 1) first.y++;
    }
    for (; first.y < second.y; first.y++) {
      paintRow(first.y, 0, SCREEN_WIDTH_COLS, first.colour);
    }
    if (second.x != 0) {
      paintRow(first.y, 0, second.x, first.colour);
    }
  }

  public void paintTheFrame() {
    addChange(SCREEN_WIDTH_COLS, SCREEN_HEIGHT - 1, 0);
    if (picture.active) {
      for (int pos = 0; pos < used - 1; pos++) {
        doChange(changes.get(pos), changes.get(pos + 1));
      }
    }
    used = 0;
    addSentinel();
  }
  /** -1 means the row was last painted in parts, so it can't be skipped by a whole-row colour match. */
  private final int[] rowPlotted = new int[SCREEN_HEIGHT];

  public void refreshAll() {
    Arrays.fill(rowPlotted, -1);
  }
}
