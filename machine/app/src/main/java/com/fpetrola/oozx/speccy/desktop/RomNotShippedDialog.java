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

package com.fpetrola.oozx.speccy.desktop;

import com.fpetrola.oozx.config.RomFiles;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;

/**
 * Asks before a ROM this build cannot carry is fetched, and then shows how much of it has arrived.
 * <p>
 * Not a lambda in the launcher, which is where the asking began: a window and a bar are state that
 * lives across three answers, and something has to own it.
 */
public class RomNotShippedDialog implements RomFiles.Consent {
  private JDialog window;
  private JProgressBar bar;
  private JLabel howMuch;

  public boolean toDownload(String rom, String from) {
    return JOptionPane.showConfirmDialog(null,
        "<html>This machine needs <b>" + rom + "</b>, which is not part of this emulator."
            + "<br><br>It is published at:<br>" + from
            + "<br><br>Fetch it from there and keep a copy?</html>",
        "A ROM that is not shipped", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  public void arriving(String rom, long soFar, long length) {
    if (window == null) openFor(rom);
    if (length > 0) {
      bar.setIndeterminate(false);
      bar.setValue((int) (soFar * 100 / length));
    }
    howMuch.setText(kb(soFar) + (length > 0 ? " of " + kb(length) : ""));
    showItNow();
  }

  public void arrived(String rom) {
    if (window == null) return;
    window.dispose();
    window = null;
  }

  private static String kb(long bytes) {
    return (bytes + 1023) / 1024 + " KB";
  }

  private void openFor(String rom) {
    window = new JDialog((java.awt.Frame) null, "Fetching " + rom, false);
    bar = new JProgressBar(0, 100);
    bar.setIndeterminate(true);
    howMuch = new JLabel(" ", JLabel.CENTER);
    JPanel inside = new JPanel(new BorderLayout(0, 6));
    inside.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    inside.add(new JLabel(rom, JLabel.CENTER), BorderLayout.NORTH);
    inside.add(bar, BorderLayout.CENTER);
    inside.add(howMuch, BorderLayout.SOUTH);
    window.setContentPane(inside);
    window.setSize(320, 120);
    window.setLocationRelativeTo(null);
    window.setVisible(true);
  }

  /**
   * The machine is being built on the event thread and will not give it back until the ROM is
   * here, so nothing is going to repaint this window on its own: it is painted where it stands.
   */
  private void showItNow() {
    if (!SwingUtilities.isEventDispatchThread()) return;
    bar.paintImmediately(0, 0, bar.getWidth(), bar.getHeight());
    howMuch.paintImmediately(0, 0, howMuch.getWidth(), howMuch.getHeight());
  }
}
