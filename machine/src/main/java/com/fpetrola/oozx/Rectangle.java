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

    // Structure to represent a rectangle
    public static class Rect {
        int x, y; // Top-left corner
        int w, h; // Width and height

        Rect(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    // Rectangles modified on the last line to be displayed
    private static List<Rect> active = new ArrayList<>();
    private static int activeCount = 0;
    private static int activeAllocated = 0;

    // Rectangles not modified on the last line to be displayed
    public static List<Rect> inactive = new ArrayList<>();
    public static int inactiveCount = 0;
    public static int inactiveAllocated = 0;

    // Add the rectangle { x, y, w, 1 } to the list of rectangles to be redrawn
    public static void add(int y, int x, int w) {
        // Check through all 'active' rectangles to see if we can extend one
        for (int i = 0; i < activeCount; i++) {
            Rect rect = active.get(i);
            if (rect.x == x && rect.w == w) {
                rect.h++;
                return;
            }
        }

        // Couldn't find a rectangle to extend, so create a new one
        if (++activeCount > activeAllocated) {
            int newAlloc = activeAllocated == 0 ? 8 : 2 * activeAllocated;
            while (active.size() < newAlloc) {
                active.add(null); // Pre-allocate space
            }
            activeAllocated = newAlloc;
        }

        active.set(activeCount - 1, new Rect(x, y, w, 1));
    }

    // Utility methods for max and min
    private static int max(int a, int b) {
        return a > b ? a : b;
    }

    private static int min(int a, int b) {
        return a < b ? a : b;
    }

    // Compare and merge rectangles with the inactive list
    private static boolean compareAndMergeRectangles(Rect source) {
        for (int z = 0; z < inactiveCount; z++) {
            Rect inactiveRect = inactive.get(z);
            if (inactiveRect.x == source.x && inactiveRect.w == source.w) {
                if (inactiveRect.y == source.y && inactiveRect.h == source.h) {
                    return true;
                }
                if ((inactiveRect.y < source.y &&
                        source.y < (inactiveRect.y + inactiveRect.h + 1)) ||
                        (source.y < inactiveRect.y &&
                                inactiveRect.y < (source.y + source.h + 1))) {
                    // Rectangles overlap or touch in the y dimension, merge
                    inactiveRect.h = max(inactiveRect.y + inactiveRect.h, source.y + source.h) -
                            min(inactiveRect.y, source.y);
                    inactiveRect.y = min(inactiveRect.y, source.y);
                    return true;
                }
            }
            if (inactiveRect.y == source.y && inactiveRect.h == source.h) {
                if ((inactiveRect.x < source.x &&
                        source.x < (inactiveRect.x + inactiveRect.w + 1)) ||
                        (source.x < inactiveRect.x &&
                                inactiveRect.x < (source.x + source.w + 1))) {
                    // Rectangles overlap or touch in the x dimension, merge
                    inactiveRect.w = max(inactiveRect.x + inactiveRect.w, source.x + source.w) -
                            min(inactiveRect.x, source.x);
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
        List<Rect> newActive = new ArrayList<>();
        int newActiveCount = 0;

        for (int i = 0; i < activeCount; i++) {
            Rect rect = active.get(i);
            if (rect == null) continue;

            // Skip if this rectangle was updated this line
            if (rect.y + rect.h == y + 1) {
                newActive.add(rect);
                newActiveCount++;
                continue;
            }

            // Check for merge with inactive list if frame skip is enabled
            if (Settings.current.frameRate > 1 && compareAndMergeRectangles(rect)) {
                rect.h = 0; // Mark as done
                continue;
            }

            // Move to inactive list
            if (++inactiveCount > inactiveAllocated) {
                int newAlloc = inactiveAllocated == 0 ? 8 : 2 * inactiveAllocated;
                while (inactive.size() < newAlloc) {
                    inactive.add(null); // Pre-allocate space
                }
                inactiveAllocated = newAlloc;
            }

            inactive.set(inactiveCount - 1, rect);
            rect.h = 0; // Mark as done
        }

        // Compress the list of active rectangles
        active = newActive;
        activeCount = newActiveCount;
        activeAllocated = active.size();
    }
}