package com.fpetrola.oozx;// Rectangle.java: Routines for managing the set of screen area rectangles updated since the last display
// Copyright (c) 1999-2015 Philip Kendall, Thomas Harte, Witold Filipczyk, Fredrick Meunier
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Author contact information:
// E-mail: philip-fuse@shadowmagic.org.uk

import java.util.*;

// Assuming ported dependencies:
// - Libspectrum (for renew-like functionality, if needed)
// - Settings (SettingsInfo, current)
// - Ui (for UI-related functionality, if needed)
// - Fuse (for any emulator-specific functionality, if needed)

public class Rectangle {

  // Rectangles modified on the last line to be displayed
  private static List<Rect> active = new ArrayList<>();

  // Rectangles not modified on the last line to be displayed
  public static List<Rect> inactive = new ArrayList<>();

  // Add the rectangle { x, y, w, 1 } to the list of rectangles to be redrawn
  public static void add(int y, int x, int w) {
    // Check through all 'active' rectangles to see if we can extend one
    for (Rect rect : active) {
      if (rect.x == x && rect.getW() == w) {
        rect.setH(rect.getH() + 1);
        return;
      }
    }

    active.add(new Rect(x, y, w, 1));
  }

  // Utility methods for max and min
  private static int max(int a, int b) {
    return Math.max(a, b);
  }

  private static int min(int a, int b) {
    return Math.min(a, b);
  }

  // Compare and merge rectangles with the inactive list
  private static boolean compareAndMergeRectangles(Rect source) {
    for (Rect inactiveRect : inactive) {
      if (inactiveRect.x == source.x && inactiveRect.getW() == source.getW()) {
        if (inactiveRect.y == source.y && inactiveRect.getH() == source.getH()) {
          return true;
        }
        if ((inactiveRect.y < source.y &&
            source.y < (inactiveRect.y + inactiveRect.getH() + 1)) ||
            (source.y < inactiveRect.y &&
                inactiveRect.y < (source.y + source.getH() + 1))) {
          // Rectangles overlap or touch in the y dimension, merge
          inactiveRect.setH(max(inactiveRect.y + inactiveRect.getH(), source.y + source.getH()) -
              min(inactiveRect.y, source.y));
          inactiveRect.y = min(inactiveRect.y, source.y);
          return true;
        }
      }
      if (inactiveRect.y == source.y && inactiveRect.getH() == source.getH()) {
        if ((inactiveRect.x < source.x &&
            source.x < (inactiveRect.x + inactiveRect.getW() + 1)) ||
            (source.x < inactiveRect.x &&
                inactiveRect.x < (source.x + source.getW() + 1))) {
          // Rectangles overlap or touch in the x dimension, merge
          inactiveRect.setW(max(inactiveRect.x + inactiveRect.getW(), source.x + source.getW()) -
              min(inactiveRect.x, source.x));
          inactiveRect.x = min(inactiveRect.x, source.x);
          return true;
        }
      }
      // Note: Overlaps offset by both x and y are not handled, as per original comment
    }
    return false;
  }

  // Move all rectangles not updated on this line to the inactive list
  public static void endLine(int y) {
    for (Iterator<Rect> iterator = active.iterator(); iterator.hasNext(); ) {
      Rect rect = iterator.next();
      if (rect == null) continue;

      // Skip if this rectangle was updated this line
      if (rect.y + rect.getH() == y + 1) {
        continue;
      }

      // Check for merge with inactive list if frame skip is enabled
      if (Settings.current.frameRate > 1 && compareAndMergeRectangles(rect)) {
        iterator.remove();
        rect.setH(0); // Mark as done
        continue;
      }

      inactive.add(rect);
      rect.setH(0); // Mark as done
    }

    active.removeIf(r -> r.getH() == 0);
  }
}