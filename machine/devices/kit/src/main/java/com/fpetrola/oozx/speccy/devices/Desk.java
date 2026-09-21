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

package com.fpetrola.oozx.speccy.devices;

import java.io.File;

/**
 * What a window that was found can ask of the desk it was opened on.
 * <p>
 * A window knows about one machine - the one it is clipped to - and about nothing else. The two
 * things it cannot do by itself are asking whoever is in front for a file and getting a computer
 * built, both of which belong to whatever put the windows on the screen. This is that, named,
 * so a window can ask for them without being handed the application's own methods.
 * <p>
 * There is one, set by whoever runs the desk. With none - the tests, a headless build - asking
 * for a file gets nothing and asking for a machine does nothing, which is what a window with no
 * desk under it should see.
 */
public interface Desk {

  Desk NOBODY = new Desk() {
    public File choose(String what) {
      return null;
    }

    public void openMachineFor(File file, MachineFrame asking) {
    }
  };

  /** A file from whoever is in front, or null if they did not pick one. */
  File choose(String what);

  /**
   * Builds a computer for this file and clips the window that asked onto it.
   * <p>
   * The window stays where it was put and the machine arrives above it: pressing play on a deck
   * is asking for a computer for THAT deck, not for another one holding the same cassette.
   */
  void openMachineFor(File file, MachineFrame asking);

  static Desk theOne() {
    return Where.desk;
  }

  static void isRunBy(Desk desk) {
    Where.desk = desk == null ? NOBODY : desk;
  }

  /** An interface cannot hold a field, and the one there is has to live somewhere. */
  final class Where {
    private static Desk desk = NOBODY;

    private Where() {
    }
  }
}
