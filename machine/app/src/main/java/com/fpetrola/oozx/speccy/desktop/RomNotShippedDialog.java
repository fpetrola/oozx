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
    boolean yes = JOptionPane.showConfirmDialog(null,
        "<html>This machine needs <b>" + rom + "</b>, which is not part of this emulator."
            + "<br><br>It is published at:<br>" + from
            + "<br><br>Fetch it from there and keep a copy?</html>",
        "A ROM that is not shipped", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    // Opened on the yes and not on the first byte: what there is to wait for is mostly getting
    // through to the other end, and a ROM is small enough to arrive in a read or two after that.
    if (yes) onTheEventThread(() -> openFor(rom));
    return yes;
  }

  public void arriving(String rom, long soFar, long length) {
    if (window == null) onTheEventThread(() -> openFor(rom));
    if (window == null) return;
    if (length > 0) {
      bar.setIndeterminate(false);
      bar.setValue((int) (soFar * 100 / length));
    }
    howMuch.setText(kb(soFar) + (length > 0 ? " of " + kb(length) : ""));
    showItNow();
  }

  public void arrived(String rom) {
    if (window == null) return;
    JDialog closing = window;
    window = null;
    onTheEventThread(closing::dispose);
  }

  /**
   * Windows are built and closed where Swing says they are, whichever thread asked. Waited for
   * rather than queued: what comes next is a download that will not give this thread back.
   */
  private static void onTheEventThread(Runnable what) {
    if (SwingUtilities.isEventDispatchThread()) {
      what.run();
      return;
    }
    try {
      SwingUtilities.invokeAndWait(what);
    } catch (InterruptedException stopped) {
      Thread.currentThread().interrupt();
    } catch (java.lang.reflect.InvocationTargetException itThrew) {
      throw new IllegalStateException(itThrew.getCause());
    }
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
    window.setAlwaysOnTop(true);
    window.setVisible(true);
    showItNow();
  }

  /**
   * The machine is being built on the event thread and will not give it back until the ROM is
   * here, so nothing is going to repaint this window on its own: it is painted where it stands.
   */
  private void showItNow() {
    if (window == null || !SwingUtilities.isEventDispatchThread()) return;
    // The whole of it, not just the bar: with nothing pumping events the window has never had a
    // first paint either, and a bar painted onto a window that was never drawn is nothing at all.
    JPanel inside = (JPanel) window.getContentPane();
    inside.paintImmediately(0, 0, inside.getWidth(), inside.getHeight());
    java.awt.Toolkit.getDefaultToolkit().sync();
  }
}
