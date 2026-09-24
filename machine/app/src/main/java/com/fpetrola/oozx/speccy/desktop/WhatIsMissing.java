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

package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.TellsThePerson;
import com.fpetrola.oozx.plugins.PluginReleases;
import com.fpetrola.oozx.plugins.Plugins;
import com.fpetrola.oozx.plugins.PluginReleases.Board;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What to do when this build cannot do something and a jar somewhere could: ask, bring it, and
 * then do the thing that was asked.
 * <p>
 * All of it in one go and with nothing written down. What came before kept the file that could
 * not be opened on one side and what to pick out on another, and put them together through the
 * event that says a plugin arrived - so installing anything, at any later moment, opened a file
 * somebody had asked for long before, and a window meant to offer one board let you press the
 * arrow on a different one.
 * <p>
 * There is no list to mis-click here and nothing waiting to happen later: one question, one
 * answer, and it is over either way.
 */
final class WhatIsMissing {

  /** Lo que se le puede pedir a un plugin que falta: abrir un archivo, o ser la maquina de un snapshot. */
  private static final List<String> ANSWERING = java.util.stream.Stream.of(
      com.fpetrola.oozx.speccy.machine.StartsAMachineOn.class,
      com.fpetrola.emulation.helpers.snapshots.SnapshotFile.class,
      com.fpetrola.oozx.speccy.devices.Equipment.class,
      com.fpetrola.oozx.Extension.class,
      com.fpetrola.oozx.plugins.BesideTheGame.class).map(Class::getName).toList();

  /** Asked once each, since a recording asks for its machine at the start of every segment. */
  private static final java.util.Set<String> asked = java.util.concurrent.ConcurrentHashMap.newKeySet();

  /**
   * Nothing here opens this file. Brings what does and opens it, or does neither.
   *
   * @param what what the missing thing is called where a release says what it brings: the end of
   *             a file's name, or a machine as a snapshot names it
   * @param then what to do once it is in, which is the thing that was asked for in the first place
   */
  static void bringWhatIsNeeded(Component over, String what, String said, Runnable then) {
    bringWhatIsNeeded(over, List.of(what), said, then);
  }

  /** Lo mismo para varias cosas a la vez, para que una sola pregunta traiga todo lo que falta. */
  static void bringWhatIsNeeded(Component over, List<String> wanted, String said, Runnable then) {
    new SwingWorker<List<Board>, Void>() {
      protected List<Board> doInBackground() throws Exception {
        java.util.Set<Board> brings = new java.util.LinkedHashSet<>();
        for (String what : wanted) brings.addAll(PluginReleases.bringing(what, ANSWERING));
        return List.copyOf(brings);
      }

      protected void done() {
        List<Board> brings;
        try {
          brings = get();
        } catch (Exception couldNotAsk) {
          say(over, said + "\n\nWhat is published cannot be asked for just now, so there is "
              + "nothing to offer. A jar dropped in the plugins folder works all the same.");
          return;
        }
        if (brings.isEmpty()) {
          say(over, said + "\n\nNothing published brings it either.");
          return;
        }
        if (agreed(over, said, brings)) {
          bring(over, brings, then);
        }
      }
    }.execute();
  }

  /**
   * Lo que un juego trae al lado y nada de lo que esta puesto responde, con quien lo traeria.
   * Vacio cuando nadie publicado lo trae tampoco: un archivo que nadie usa no es una pregunta.
   */
  static List<Board> bringingBeside(List<String> kinds) {
    java.util.Set<Board> brings = new java.util.LinkedHashSet<>();
    for (String kind : nobodyHereAnswers(kinds)) {
      try {
        brings.addAll(PluginReleases.bringing(kind, ANSWERING));
      } catch (Exception cannotAsk) {
        return List.of();
      }
    }
    return List.copyOf(brings);
  }

  /** Las que nada de lo que esta puesto responde. */
  static List<String> nobodyHereAnswers(List<String> kinds) {
    return kinds.stream().filter(kind ->
        ANSWERING.stream().allMatch(role -> Plugins.managing().answering(role, kind).isEmpty())).toList();
  }

  /** Ofrece traerlos: traidos, lo que se queria; dicho que no, lo mismo sin ellos. */
  static void offer(Component over, List<Board> brings, String said, Runnable then, Runnable otherwise) {
    if (agreed(over, said, brings)) bring(over, brings, then);
    else otherwise.run();
  }

  /** The same, for what is asked for by something running rather than by somebody clicking. */
  static void bringWhatIsNeeded(Component over, String what, String said) {
    if (asked.add(what)) {
      bringWhatIsNeeded(over, what, said, () -> {
      });
    }
  }

  private static boolean agreed(Component over, String said, List<Board> brings) {
    String names = brings.stream().map(Board::name).reduce((a, b) -> a + ", " + b).orElse("");
    return JOptionPane.showConfirmDialog(over, said + "\n\n" + names + (brings.size() == 1
            ? " brings it. Bring it in?" : " bring it. Bring them in?"),
        "This build cannot do that", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  /** Brings them, and whatever they were built against, and then does what was waiting on it. */
  private static void bring(Component over, List<Board> brings, Runnable then) {
    new SwingWorker<List<String>, Void>() {
      protected List<String> doInBackground() {
        List<String> didNotArrive = new ArrayList<>();
        for (Board board : brings) {
          try {
            // Una llamada por cada uno: quien los carga baja lo que haga falta, verificado, y
            // resuelve de que dependen. Seguirlas a mano era instalar sin la dependencia.
            PluginReleases.bring(board);
          } catch (Exception wouldNot) {
            didNotArrive.add(board.name() + ": " + wouldNot.getMessage());
          }
        }
        return didNotArrive;
      }


      protected void done() {
        List<String> didNotArrive;
        try {
          didNotArrive = get();
        } catch (Exception broken) {
          didNotArrive = List.of(String.valueOf(broken.getMessage()));
        }
        if (!didNotArrive.isEmpty()) {
          say(over, "That did not arrive:\n\n" + String.join("\n", didNotArrive));
          return;
        }
        TellsThePerson.thatMayHaveChanged();
        then.run();
      }
    }.execute();
  }

  private static void say(Component over, String what) {
    JOptionPane.showMessageDialog(over, what, "This build cannot do that",
        JOptionPane.WARNING_MESSAGE);
  }

  private WhatIsMissing() {
  }
}
