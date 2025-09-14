/*
 * Copyright (c) 2023-2025 Fernando Damian Petrola
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

  public Border(Picture picture, BeamAt beam) {
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

  public void paint(int y, int start, int end, int colour) {
    for (; start < end; start++) {
      picture.plot8(start, y, (byte) 0, (byte) 0, (byte) colour);
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
    for (int pos = 0; pos < used - 1; pos++) {
      doChange(changes.get(pos), changes.get(pos + 1));
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
