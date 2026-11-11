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

package com.fpetrola.oozx.speccy.windows;

import java.awt.Rectangle;

/**
 * One of the four borders of a rectangle, and what lies along and across it: where a window rests
 * against a frame on that border, and where the border itself runs.
 */
public enum Edge {
  BOTTOM(Axis.FLAT, true), TOP(Axis.FLAT, false), LEFT(Axis.UPRIGHT, false), RIGHT(Axis.UPRIGHT, true);

  /** Which way a border runs. An upright one is a flat one with the rectangle seen turned over its diagonal. */
  private enum Axis {
    FLAT {
      Rectangle seen(Rectangle r) {
        return r;
      }
    },
    UPRIGHT {
      Rectangle seen(Rectangle r) {
        return new Rectangle(r.y, r.x, r.height, r.width);
      }
    };

    abstract Rectangle seen(Rectangle r);
  }

  private final Axis axis;
  /** Past the far border (below the frame, or right of it) rather than before the near one. */
  private final boolean far;

  Edge(Axis axis, boolean far) {
    this.axis = axis;
    this.far = far;
  }

  /** The top or the bottom, along which a window runs the frame's width. */
  public boolean flat() {
    return axis == Axis.FLAT;
  }

  /** The border of the other rectangle that meets this one. */
  public Edge facing() {
    for (Edge other : values()) {
      if (other.axis == axis && other.far != far) {
        return other;
      }
    }
    throw new IllegalStateException("every edge has one opposite");
  }

  /** Where a rectangle starts along this border, and how far it runs along it. */
  public int along(Rectangle r) {
    return axis.seen(r).x;
  }

  public int run(Rectangle r) {
    return axis.seen(r).width;
  }

  /** Where a rectangle is across this border, and how thick it is that way. */
  public int across(Rectangle r) {
    return axis.seen(r).y;
  }

  public int thickness(Rectangle r) {
    return axis.seen(r).height;
  }

  /** Across this border, where a window that thick rests against the frame, the two overlapping by the seam. */
  public int rest(Rectangle frame, int thick, int seam) {
    return far ? across(frame) + thickness(frame) - seam : across(frame) - thick + seam;
  }

  /** Across this border, the line of the frame's own border that many pixels inward. */
  public int line(Rectangle frame, int inward) {
    return far ? across(frame) + thickness(frame) - 1 - inward : across(frame) + inward;
  }

  /** The rectangle at those places along and across this border, with that run and thickness. */
  public Rectangle laid(int along, int across, int run, int thick) {
    return axis.seen(new Rectangle(along, across, run, thick));
  }

  /** Whether a window lies alongside this border of the frame, overlapping it along the border. */
  public boolean alongside(Rectangle frame, Rectangle window) {
    return along(window) < along(frame) + run(frame) && along(frame) < along(window) + run(window);
  }
}
