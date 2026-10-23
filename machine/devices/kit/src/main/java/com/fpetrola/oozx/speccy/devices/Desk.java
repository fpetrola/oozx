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

import com.fpetrola.oozx.speccy.config.OOZxConfiguration.WindowState;

import javax.swing.JInternalFrame;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
 * <p>
 * How a desk keeps its windows from one run to the next is here too, so that any desk, the
 * application's or a test's, keeps them and brings them back the same way.
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
  };

  /** A file from whoever is in front, or null if they did not pick one. */
  default File choose(String what) {
    return null;
  }

  /**
   * Builds a computer for this file and clips the window that asked onto it.
   * <p>
   * The window stays where it was put and the machine arrives above it: pressing play on a deck
   * is asking for a computer for THAT deck, not for another one holding the same cassette.
   */
  default void openMachineFor(File file, MachineFrame asking) {
  }

  /**
   * Opens a game: fetched if it has to be, on a machine of its own.
   *
   * @param whenDone run on the event thread once it is up or the attempt failed, so that whoever
   *                 asked can stop saying it is working on it
   */
  default void open(Game game, Runnable whenDone) {
    whenDone.run();
  }

  /** Plays a recording from wherever it lives, which is a machine driven by it. */
  default void play(String url, String label) {
  }

  /**
   * A window for a machine that already exists, clipped to the one that asked.
   * <p>
   * Everything else here starts from a file and gets a machine built; a recording arrives with
   * its own, already loaded to the frame it was saved at, and it only wants a picture.
   *
   * @return the window the machine is shown on, or null where there is no desk to show it
   */
  default EmulatorWindow show(com.fpetrola.oozx.Speccy machine, String title, MachineFrame asking) {
    return null;
  }

  /**
   * Asks for a recording and puts it into the window that asked, or into a free one when none
   * did. Choosing it, fetching it and unpacking the archive it came in are the desk's work: what
   * arrives here is one file that can be played.
   */
  default void openRecording(MachineFrame asking) {
  }

  /**
   * Keeps a recording to come back to. The address alone comes back to an archive rather than to
   * the recording that was being watched, so which file inside it was playing goes too.
   */
  default void keepRecording(String url, String entry, String title) {
  }

  /** Keeps a game to come back to, which is what the desk's favourites are. */
  default void keep(Game game) {
  }

  /** Shows what the catalogue knows about it, the same page the machine's own button opens. */
  default void showDetails(Game game) {
  }

  /** The machines this build can open a game on, for offering them beside it. */
  default java.util.List<String> machines() {
    return java.util.List.of();
  }

  /** Lo que el escritorio recuerda entre corridas: favoritos, historial, estados guardados. */
  default com.fpetrola.oozx.speccy.config.OOZxConfiguration configuration() {
    return null;
  }

  /** Abre lo que haya en ese camino como se abre desde el menu: una maquina, una grabacion, un zip. */
  default void open(String path) {
  }

  /** Una maquina desde un estado guardado; si ya no esta, desde el archivo de donde salio. */
  default void openSaved(String savedStateId, String file) {
    open(file);
  }

  /** Una grabacion, eligiendo de entrada que parte de un zip, cuando ya se sabe. */
  default void play(String url, String label, String entry) {
    play(url, label);
  }

  /** La maquina detras de esa ventana, o null si no es la de una maquina. */
  default com.fpetrola.oozx.speccy.peripherals.EmulatorCore coreOf(java.awt.Component window) {
    return null;
  }

  /** Pone la ventana en el escritorio, adelante. */
  default void place(javax.swing.JInternalFrame window) {
  }

  /** The equipment clipped onto that machine's window, each with the name of the kind it is. */
  default java.util.Map<MachineFrame, String> equipmentOn(EmulatorWindow machine) {
    return java.util.Map.of();
  }

  /** A new window of the equipment offered under that name, on the desk and clipped onto nothing; null where none is. */
  default MachineFrame equipment(String name) {
    return null;
  }

  /** The name the desk opened that window under, or null where it is not one of those. */
  default String nameOf(JInternalFrame window) {
    return null;
  }

  /** A window of that name, opened as the desk's menu opens it: the one of the desk, or another of equipment; null where none is. */
  default JInternalFrame window(String name) {
    return null;
  }

  /** A window with the machine of that file, opened as any file is; null where none came of it. */
  default EmulatorWindow machineFrom(File file) {
    return null;
  }

  /**
   * Those windows, from the back to the front, written down so that opening them again in that order
   * leaves them stacked as they were: each machine as a file of that folder, and any other window the
   * session knows. What the folder held before and is not kept now goes.
   */
  default List<WindowState> keep(Session session, List<JInternalFrame> windows, File folder) {
    List<WindowState> kept = new ArrayList<>();
    for (JInternalFrame window : windows) {
      WindowState state = keep(session, window, folder, kept.size());
      if (state != null) kept.add(state);
    }
    Set<String> files = kept.stream().map(WindowState::getFilePath).filter(Objects::nonNull).collect(Collectors.toSet());
    File[] before = folder.listFiles();
    if (before != null) Arrays.stream(before).filter(file -> !files.contains(file.getPath())).forEach(File::delete);
    return kept;
  }

  /**
   * That window written down: where it is, maximised or minimised, and what it is: a machine as file n
   * of that folder, written by the session, and any other as the session says. Null where the session
   * does not keep it, or could not.
   */
  default WindowState keep(Session session, JInternalFrame window, File folder, int n) {
    if (session == null) return null;
    try {
      java.awt.Rectangle place = window.getNormalBounds();
      WindowState state = new WindowState(window instanceof EmulatorWindow ? "EMULATOR" : "WINDOW", place.x, place.y, place.width, place.height);
      state.setMaximum(window.isMaximum());
      state.setIcon(window.isIcon());
      if (window instanceof EmulatorWindow machine) {
        File file = new File(folder, n + "." + session.extension());
        byte[] bytes = session.write(machine);
        folder.mkdirs();
        java.nio.file.Files.write(file.toPath(), bytes);
        state.setFilePath(file.getPath());
        return state;
      }
      state.setInside(session.keep(window));
      return state.getInside().isEmpty() ? null : state;
    } catch (Exception | LinkageError cannot) {
      System.err.println("Error guardando la ventana " + window.getTitle() + ": " + cannot);
      return null;
    }
  }

  /** Each window kept, in that order, where it was: a machine from its file as any file opens, any other as the session brings it back. */
  default void bringBack(Session session, List<WindowState> kept) {
    for (WindowState state : kept) {
      try {
        JInternalFrame window = null;
        if ("EMULATOR".equals(state.getType())) {
          File file = new File(state.getFilePath());
          EmulatorWindow machine = file.isFile() ? machineFrom(file) : null;
          if (machine != null && session != null) session.bringBack(java.nio.file.Files.readAllBytes(file.toPath()), machine);
          if (machine instanceof JInternalFrame frame) window = frame;
        } else if (session != null && state.getInside() != null) {
          window = session.bringBack(state.getInside());
          if (window != null) place(window);
        }
        if (window == null) continue;
        if (state.getWidth() > 0) window.setBounds(state.getX(), state.getY(), state.getWidth(), state.getHeight());
        window.setMaximum(state.isMaximum());
        window.setIcon(state.isIcon());
      } catch (Exception | LinkageError cannot) {
        System.err.println("Error restaurando " + (state.getFilePath() != null ? state.getFilePath() : state.getInside()) + ": " + cannot);
      }
    }
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
