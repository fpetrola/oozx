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

import com.fpetrola.oozx.plugins.PluginReleases;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;

/**
 * Brings the boards before the desktop exists, and shows what is arriving while it arrives.
 * <p>
 * Before, and never after: a peripheral that lands once a machine has been built is a peripheral
 * that machine will not have. The fetching happens on a thread of its own and this window waits
 * on it, which is the only way to wait on the event thread without stopping it.
 */
public class PluginsDialog implements PluginReleases.Consent, PluginReleases.Watching {

  private static final PluginsDialog ASKING = new PluginsDialog();

  private JDialog waiting;
  private final JLabel what = new JLabel(" ");
  private final JProgressBar bar = new JProgressBar();

  public static PluginsDialog asking() {
    return ASKING;
  }

  /** Called from the event thread while the desktop is being put together. */
  public static void bringThem() {
    ASKING.bring();
  }

  private void bring() {
    waiting = new JDialog((Frame) null, "Peripherals", true);
    JPanel body = new JPanel(new BorderLayout(8, 8));
    body.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    what.setText("Looking for the boards that are published…");
    bar.setIndeterminate(true);
    body.add(what, BorderLayout.NORTH);
    body.add(bar, BorderLayout.SOUTH);
    waiting.add(body);
    waiting.pack();
    waiting.setSize(Math.max(420, waiting.getWidth()), waiting.getHeight());
    waiting.setLocationRelativeTo(null);

    SwingWorker<Void, Void> fetching = new SwingWorker<>() {
      protected Void doInBackground() {
        PluginReleases.bringWhatIsPublished(PluginsDialog.this);
        return null;
      }

      protected void done() {
        waiting.dispose();
      }
    };
    fetching.execute();
    waiting.setVisible(true);
  }

  /**
   * Asked once, and from the event thread even though the asking comes from the other one: a
   * question is a window, and windows belong there.
   */
  @Override
  public boolean toBring(String repository, List<String> boards) {
    boolean[] yes = new boolean[1];
    Runnable question = () -> {
      String some = String.join(", ", boards.size() > 8 ? boards.subList(0, 8) : boards);
      if (boards.size() > 8) some += ", and " + (boards.size() - 8) + " more";
      yes[0] = JOptionPane.showConfirmDialog(waiting,
          "<html><b>" + boards.size() + " peripherals are published for this emulator</b><br><br>"
              + some + "<br><br>They are jars of their own, kept under your home directory, and each "
              + "appears in the Equipment menu once it is here.<br><br>Bring them from <b>"
              + repository + "</b>?<br><br><i>Saying no turns this off; the plugins section of the "
              + "configuration turns it back on.</i></html>",
          "Peripherals", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    };
    try {
      if (SwingUtilities.isEventDispatchThread()) question.run();
      else SwingUtilities.invokeAndWait(question);
    } catch (Exception notAsked) {
      return false;
    }
    return yes[0];
  }

  @Override
  public void bringing(String board, int which, int of) {
    SwingUtilities.invokeLater(() -> {
      what.setText(board + "…");
      bar.setIndeterminate(false);
      bar.setMinimum(0);
      bar.setMaximum(of);
      bar.setValue(which);
      bar.setStringPainted(true);
      bar.setString(which + " of " + of);
    });
  }
}
