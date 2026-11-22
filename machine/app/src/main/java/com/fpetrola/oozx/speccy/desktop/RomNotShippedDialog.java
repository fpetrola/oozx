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

import dev.plugdesk.app.Configuration;
import com.fpetrola.oozx.config.RomFiles;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.util.List;

/**
 * Brings the ROMs a machine or board needs and this build cannot carry, before anybody starts or plugs it in.
 * <p>
 * Before, and never during: a machine is built on the emulator's own thread, and asking a question
 * there means the window that asks is waiting on a thread that is waiting on the window. The
 * emulator hung exactly there. So the asking and the fetching happen while nothing is being built,
 * the fetching on a thread of its own, and the machine is started only once everything is here.
 */
public class RomNotShippedDialog implements RomFiles.Consent {
  private static final RomNotShippedDialog ASKING = new RomNotShippedDialog();

  private boolean agreed;
  private JDialog window;

  public static RomNotShippedDialog asking() {
    return ASKING;
  }

  @Override
  public boolean bring(RomFiles roms, List<String> missing) {
    if (!SwingUtilities.isEventDispatchThread()) return false;
    if (!agreesTo(missing)) return false;

    open(missing);
    agreed = true;
    SwingWorker<Boolean, Void> fetching = new SwingWorker<>() {
      @Override
      protected Boolean doInBackground() {
        for (String rom : missing) {
          if (!roms.bring(rom)) return false;
        }
        return true;
      }

      protected void done() {
        close();
      }
    };
    fetching.execute();
    // The window is modal, so this pumps the events that paint it while the fetching goes on.
    window.setVisible(true);
    agreed = false;
    try {
      return fetching.get();
    } catch (Exception itDidNotArrive) {
      JOptionPane.showMessageDialog(null, String.valueOf(itDidNotArrive.getCause() == null
          ? itDidNotArrive.getMessage() : itDidNotArrive.getCause().getMessage()), "That ROM did not arrive", JOptionPane.WARNING_MESSAGE);
      return false;
    }
  }

  private static boolean agreesTo(List<String> missing) {
    RomFiles roms = Configuration.shared().of(RomFiles.class);
    StringBuilder said = new StringBuilder("<html>This needs "
        + (missing.size() == 1 ? "a ROM" : missing.size() + " ROMs") + " that this emulator does not carry:<br><br>");
    for (String rom : missing) {
      RomFiles.Source source = roms.sourceFor(rom);
      said.append("<b>").append(rom).append("</b> - ").append(source == null ? "nowhere published; choose the file yourself" : source.url).append("<br>");
    }
    said.append("<br>Fetch them from there and keep a copy?</html>");
    return JOptionPane.showConfirmDialog(null, said.toString(), "ROMs that are not shipped",
        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  public boolean toDownload(String rom, String from) {
    return agreed;
  }

  private void open(List<String> missing) {
    window = new JDialog((java.awt.Frame) null, missing.size() == 1 ? "Fetching a ROM" : "Fetching " + missing.size() + " ROMs", true);
    JProgressBar bar = new JProgressBar(0, 100);
    bar.setIndeterminate(true);
    JLabel which = new JLabel(String.join(", ", missing), JLabel.CENTER);
    JPanel inside = new JPanel(new BorderLayout(0, 8));
    inside.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    inside.add(bar, BorderLayout.CENTER);
    inside.add(which, BorderLayout.SOUTH);
    window.setContentPane(inside);
    window.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    window.setSize(380, 120);
    window.setLocationRelativeTo(null);
  }

  private void close() {
    if (window == null) return;
    window.dispose();
    window = null;
  }
}
