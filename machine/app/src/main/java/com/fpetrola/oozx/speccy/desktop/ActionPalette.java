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

import com.fpetrola.oozx.plugins.Plugins;
import dev.crystal.plugins.api.Offer;
import dev.crystal.plugins.api.PluginArtifact;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lo que se puede hacer, dicho por los plugins: lo de los que estan puestos se hace, y lo de los que
 * no, trae el plugin y despues lo hace. Es como la persona descubre que hay mas de lo que ve.
 */
final class ActionPalette extends JDialog {

  /** Una accion, y el plugin que hay que traer para hacerla: null si ya esta puesto. */
  private record Row(Offer offer, PluginArtifact toBring) {
    @Override
    public String toString() {
      return toBring == null ? offer.text() : offer.text() + "   —   needs " + toBring.id();
    }
  }

  private final ZXSpectrumDesktopApp desk;
  private final List<Row> all = new ArrayList<>();
  private final DefaultListModel<Row> shown = new DefaultListModel<>();
  private final JList<Row> list = new JList<>(shown);
  private final JTextField asked = new JTextField();
  private final JLabel status = new JLabel(" ");

  ActionPalette(ZXSpectrumDesktopApp desk) {
    super((Frame) desk, "What do you want to do?", false);
    this.desk = desk;
    setLayout(new BorderLayout(4, 4));
    add(asked, BorderLayout.NORTH);
    add(new JScrollPane(list), BorderLayout.CENTER);
    status.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
    add(status, BorderLayout.SOUTH);
    asked.getDocument().addDocumentListener(new DocumentListener() {
      public void insertUpdate(DocumentEvent e) { filter(); }
      public void removeUpdate(DocumentEvent e) { filter(); }
      public void changedUpdate(DocumentEvent e) { filter(); }
    });
    asked.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent key) {
        if (key.getKeyCode() == KeyEvent.VK_DOWN) list.requestFocusInWindow();
        if (key.getKeyCode() == KeyEvent.VK_ENTER) chosen(list.getSelectedValue() != null ? list.getSelectedValue()
            : shown.isEmpty() ? null : shown.get(0));
        if (key.getKeyCode() == KeyEvent.VK_ESCAPE) dispose();
      }
    });
    list.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent key) {
        if (key.getKeyCode() == KeyEvent.VK_ENTER) chosen(list.getSelectedValue());
        if (key.getKeyCode() == KeyEvent.VK_ESCAPE) dispose();
      }
    });
    list.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent click) {
        if (click.getClickCount() == 2) chosen(list.getSelectedValue());
      }
    });
    setSize(560, 380);
    setLocationRelativeTo(desk);
    Plugins.managing().offering().forEach(offer -> all.add(new Row(offer, null)));
    filter();
    lookForWhatCouldBeBrought();
  }

  /** Lo que ofrecen los que no estan puestos: pregunta al archivo, asi que no en este hilo. */
  private void lookForWhatCouldBeBrought() {
    status.setText("Looking for what else can be had...");
    new SwingWorker<Map<PluginArtifact, List<Offer>>, Void>() {
      protected Map<PluginArtifact, List<Offer>> doInBackground() {
        return Plugins.managing().availableOffering();
      }

      protected void done() {
        try {
          get().forEach((artifact, offers) -> offers.forEach(offer -> all.add(new Row(offer, artifact))));
          status.setText(" ");
        } catch (Exception cannotAsk) {
          status.setText("What else can be had cannot be asked for just now.");
        }
        filter();
      }
    }.execute();
  }

  private void filter() {
    String words = asked.getText().trim().toLowerCase();
    shown.clear();
    all.stream().filter(row -> row.offer().text().toLowerCase().contains(words)).forEach(shown::addElement);
    if (!shown.isEmpty()) list.setSelectedIndex(0);
  }

  private void chosen(Row row) {
    if (row == null) return;
    dispose();
    if (row.toBring() == null) {
      desk.doWhatIsOffered(row.offer());
      return;
    }
    new SwingWorker<Void, Void>() {
      protected Void doInBackground() {
        Plugins.add(row.toBring().id());
        return null;
      }

      protected void done() {
        desk.somethingWasPluggedIn();
        desk.doWhatIsOffered(row.offer());
      }
    }.execute();
  }
}
