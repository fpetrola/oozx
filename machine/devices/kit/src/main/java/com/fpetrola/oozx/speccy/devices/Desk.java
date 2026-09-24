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

  /**
   * A game as the desk opens it: which file to load, on which machine, and what the catalogue
   * called it. The window that asks knows far more about it than this; this is the part the
   * desk needs to open it and to be able to come back to it.
   */
  record Game(String file, String machine, String id, String title) {
  }

  Desk NOBODY = new Desk() {
    public File choose(String what) {
      return null;
    }

    public void openMachineFor(File file, MachineFrame asking) {
    }

    public void open(Game game, Runnable whenDone) {
      whenDone.run();
    }

    public void play(String url, String label) {
    }

    public void keep(Game game) {
    }

    public void showDetails(Game game) {
    }

    public EmulatorWindow show(com.fpetrola.oozx.Speccy machine, String title, MachineFrame asking) {
      return null;
    }

    public void openRecording(MachineFrame asking) {
    }

    public void keepRecording(String url, String entry, String title) {
    }

    public java.util.List<String> machines() {
      return java.util.List.of();
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

  /**
   * Opens a game: fetched if it has to be, on a machine of its own.
   *
   * @param whenDone run on the event thread once it is up or the attempt failed, so that whoever
   *                 asked can stop saying it is working on it
   */
  void open(Game game, Runnable whenDone);

  /** Plays a recording from wherever it lives, which is a machine driven by it. */
  void play(String url, String label);

  /**
   * A window for a machine that already exists, clipped to the one that asked.
   * <p>
   * Everything else here starts from a file and gets a machine built; a recording arrives with
   * its own, already loaded to the frame it was saved at, and it only wants a picture.
   *
   * @return the window the machine is shown on, or null where there is no desk to show it
   */
  EmulatorWindow show(com.fpetrola.oozx.Speccy machine, String title, MachineFrame asking);

  /**
   * Asks for a recording and puts it into the window that asked, or into a free one when none
   * did. Choosing it, fetching it and unpacking the archive it came in are the desk's work: what
   * arrives here is one file that can be played.
   */
  void openRecording(MachineFrame asking);

  /**
   * Keeps a recording to come back to. The address alone comes back to an archive rather than to
   * the recording that was being watched, so which file inside it was playing goes too.
   */
  void keepRecording(String url, String entry, String title);

  /** Keeps a game to come back to, which is what the desk's favourites are. */
  void keep(Game game);

  /** Shows what the catalogue knows about it, the same page the machine's own button opens. */
  void showDetails(Game game);

  /** The machines this build can open a game on, for offering them beside it. */
  java.util.List<String> machines();

  /** Los ajustes de esa maquina, pegados a su ventana. */
  default void openSettingsFor(EmulatorWindow machine) {
  }

  /** Como se ve la pantalla de esa maquina: escalado, television, color. */
  default void openScreenSettings(EmulatorWindow machine) {
  }

  /** Un ajuste de pantalla que quedo elegido, para las maquinas que se abran despues. */
  default void rememberScreen(String setting, String value) {
  }

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
