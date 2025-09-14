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
 * The border, which is one colour at a time and changes wherever the beam happens to be. A program
 * writing the port over and over paints stripes, so what is kept is not a colour but the changes
 * and where each of them caught the beam; at the end of a frame they are painted as the bands
 * between one change and the next.
 * <p>
 * It shares nothing with the picture but the canvas and the beam: the bitmap, the attributes, the
 * dirty cells and the flash are all somebody else's.
 */
public final class Border {
  private final Picture picture;
  private final BeamAt beam;
  private byte colour;
  private byte lastPlotted;

  /** Where the beam is when the port is written; the border does not know how that is worked out. */
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

  /** How many of changes are this frame's; the rest are last frame's, kept to be reused. */
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

  /** The port was written: from here on, and from where the beam is, the border is this. */
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

  /** At the end of a frame, the bands between one change and the next are painted. */
  public void paintTheFrame() {
    addChange(SCREEN_WIDTH_COLS, SCREEN_HEIGHT - 1, 0);
    for (int pos = 0; pos < used - 1; pos++) {
      doChange(changes.get(pos), changes.get(pos + 1));
    }
    used = 0;
    addSentinel();
  }
  /** The colour each row was last painted in whole, or -1 where it was painted in parts. */
  private final int[] rowPlotted = new int[SCREEN_HEIGHT];

  /** Everything is to be painted again. */
  public void refreshAll() {
    Arrays.fill(rowPlotted, -1);
  }
}
