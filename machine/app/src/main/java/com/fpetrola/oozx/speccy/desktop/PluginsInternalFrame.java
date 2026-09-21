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
import com.fpetrola.oozx.plugins.PluginReleases.Board;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * What the environment is made of, as two lists: what is published on the left, what is in it on
 * the right, and the boards moved from one side to the other.
 * <p>
 * Moving one to the right brings the jar in and plugs it in - it is in the Equipment menu before
 * the window closes, and in every machine opened from then on. Moving one to the left takes the
 * jar away, which the next run will not have: a class cannot be unloaded from a machine that is
 * already using it, and the window says so rather than pretending otherwise.
 */
public class PluginsInternalFrame extends JInternalFrame {

  private final DefaultListModel<Board> outside = new DefaultListModel<>();
  private final DefaultListModel<Board> inside = new DefaultListModel<>();
  private final JList<Board> published = new JList<>(outside);
  private final JList<Board> included = new JList<>(inside);
  private final JButton include = new JButton("→");
  private final JButton leaveOut = new JButton("←");
  private final JButton again = new JButton("Look again");
  private final JLabel saying = new JLabel(" ");
  private final JProgressBar bar = new JProgressBar();
  private final Consumer<Void> arrived;

  /** @param arrived told once something new is in, so the menus can say so */
  public PluginsInternalFrame(Consumer<Void> arrived) {
    super("Plugins - " + PluginReleases.publishedAt(), true, true, true, true);
    this.arrived = arrived;

    for (JList<Board> side : List.of(published, included)) {
      side.setCellRenderer(new AsABoard());
      side.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      side.setVisibleRowCount(12);
    }
    published.addMouseListener(doubleClick(this::includeTheChosen));
    included.addMouseListener(doubleClick(this::leaveOutTheChosen));

    // The two lists take the width; the middle is only as wide as its two buttons.
    JPanel sides = new JPanel(new BorderLayout(6, 0));
    sides.add(titled("Published", published), BorderLayout.WEST);
    sides.add(inTheMiddle(), BorderLayout.CENTER);
    sides.add(titled("In this emulator", included), BorderLayout.EAST);
    sides.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));

    JPanel bottom = new JPanel(new BorderLayout(6, 4));
    bottom.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    buttons.add(again);
    bottom.add(buttons, BorderLayout.WEST);
    bottom.add(saying, BorderLayout.CENTER);
    bar.setVisible(false);
    bottom.add(bar, BorderLayout.SOUTH);

    add(sides, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);
    setBounds(60, 60, 700, 420);

    include.addActionListener(pressed -> includeTheChosen());
    leaveOut.addActionListener(pressed -> leaveOutTheChosen());
    again.addActionListener(pressed -> look());
    look();
  }

  private static JComponent titled(String title, JList<Board> side) {
    JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.add(new JLabel(title), BorderLayout.NORTH);
    JScrollPane scroll = new JScrollPane(side);
    scroll.setPreferredSize(new Dimension(280, 260));
    panel.add(scroll, BorderLayout.CENTER);
    return panel;
  }

  private JComponent inTheMiddle() {
    JPanel middle = new JPanel();
    middle.setLayout(new BoxLayout(middle, BoxLayout.Y_AXIS));
    middle.add(Box.createVerticalGlue());
    for (JButton button : List.of(include, leaveOut)) {
      button.setAlignmentX(CENTER_ALIGNMENT);
      button.setMaximumSize(new Dimension(64, 28));
      middle.add(button);
      middle.add(Box.createVerticalStrut(6));
    }
    middle.add(Box.createVerticalGlue());
    return middle;
  }

  private static MouseAdapter doubleClick(Runnable what) {
    return new MouseAdapter() {
      public void mouseClicked(MouseEvent clicked) {
        if (clicked.getClickCount() == 2) what.run();
      }
    };
  }

  private void look() {
    busy("Looking at what is published…", true);
    new SwingWorker<List<Board>, Void>() {
      protected List<Board> doInBackground() throws Exception {
        return PluginReleases.published();
      }

      protected void done() {
        try {
          outside.clear();
          inside.clear();
          for (Board board : get()) {
            (PluginReleases.isHere(board) ? inside : outside).addElement(board);
          }
          busy(inside.size() + " in, " + outside.size() + " to be had", false);
        } catch (Exception noAnswer) {
          busy("They could not be asked for: " + reason(noAnswer), false);
        }
      }
    }.execute();
  }

  private void includeTheChosen() {
    List<Board> chosen = published.getSelectedValuesList();
    if (chosen.isEmpty()) {
      busy("Choose one on the left first", false);
      return;
    }
    busy("Bringing " + chosen.size() + "…", true);
    bar.setVisible(true);
    bar.setIndeterminate(false);
    bar.setMinimum(0);
    bar.setMaximum(chosen.size());
    bar.setValue(0);

    new SwingWorker<Void, Board>() {
      protected Void doInBackground() {
        for (Board board : chosen) {
          try {
            PluginReleases.bring(board);
            publish(board);
          } catch (Exception didNotArrive) {
            System.err.println("oozx: " + board.name() + " did not arrive: " + didNotArrive);
          }
        }
        return null;
      }

      protected void process(List<Board> arrived) {
        for (Board board : arrived) {
          outside.removeElement(board);
          inside.addElement(board);
          bar.setValue(bar.getValue() + 1);
          saying.setText(board.name() + " is in");
        }
        sort(inside);
      }

      protected void done() {
        bar.setVisible(false);
        if (arrived != null) arrived.accept(null);
        busy(inside.size() + " in, " + outside.size() + " to be had  -  what you just added is in the"
            + " Equipment menu and in the machines you open from now on", false);
      }
    }.execute();
  }

  private void leaveOutTheChosen() {
    List<Board> chosen = included.getSelectedValuesList();
    if (chosen.isEmpty()) {
      busy("Choose one on the right first", false);
      return;
    }
    int gone = 0;
    for (Board board : chosen) {
      try {
        PluginReleases.takeOut(board);
        inside.removeElement(board);
        outside.addElement(board);
        gone++;
      } catch (Exception wouldNotGo) {
        System.err.println("oozx: " + board.name() + " could not be taken out: " + wouldNotGo);
      }
    }
    sort(outside);
    busy(inside.size() + " in, " + outside.size() + " to be had  -  the " + gone + " taken out will be"
        + " gone the next time the emulator starts", false);
  }

  private static void sort(DefaultListModel<Board> side) {
    List<Board> boards = new ArrayList<>();
    for (int row = 0; row < side.size(); row++) boards.add(side.get(row));
    boards.sort(java.util.Comparator.comparing(Board::name));
    side.clear();
    boards.forEach(side::addElement);
  }

  private void busy(String what, boolean waiting) {
    saying.setText(what);
    include.setEnabled(!waiting);
    leaveOut.setEnabled(!waiting);
    again.setEnabled(!waiting);
    if (waiting) {
      bar.setIndeterminate(true);
      bar.setVisible(true);
    }
  }

  private static String reason(Exception broken) {
    Throwable deepest = broken;
    while (deepest.getCause() != null) deepest = deepest.getCause();
    return deepest.getMessage() == null ? deepest.getClass().getSimpleName() : deepest.getMessage();
  }

  /** A board says its name and what it weighs, since that is all there is to know before taking it. */
  private static class AsABoard extends javax.swing.DefaultListCellRenderer {
    public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
        boolean chosen, boolean focused) {
      super.getListCellRendererComponent(list, value, index, chosen, focused);
      if (value instanceof Board board) {
        setText("<html>" + board.name() + "  <font color='gray'>"
            + Math.max(1, board.size() / 1024) + " KB</font></html>");
      }
      return this;
    }
  }
}
