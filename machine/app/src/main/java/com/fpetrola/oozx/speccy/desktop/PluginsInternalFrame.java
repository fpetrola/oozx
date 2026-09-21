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
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The boards that are published, and which of them this emulator has.
 * <p>
 * Ticking one and pressing the button brings it in and plugs it in: it is in the Equipment menu
 * before the window closes. A machine that was already open was built without it, so it is the
 * machines opened from here on that have it - which the window says rather than leaves to be
 * discovered.
 */
public class PluginsInternalFrame extends JInternalFrame {

  private final List<Board> boards = new ArrayList<>();
  private final List<Boolean> wanted = new ArrayList<>();
  private final Offer offer = new Offer();
  private final JTable list = new JTable(offer);
  private final JLabel saying = new JLabel(" ");
  private final JProgressBar bar = new JProgressBar();
  private final JButton add = new JButton("Add the ticked ones");
  private final JButton again = new JButton("Look again");
  private final Consumer<Void> arrived;

  /** @param arrived told once something new is here, so the menus can say so */
  public PluginsInternalFrame(Consumer<Void> arrived) {
    super("Plugins - " + PluginReleases.publishedAt(), true, true, true, true);
    this.arrived = arrived;

    list.setRowHeight(22);
    list.getColumnModel().getColumn(0).setMaxWidth(30);
    list.getColumnModel().getColumn(2).setMaxWidth(90);
    list.getColumnModel().getColumn(3).setMaxWidth(110);

    JPanel top = new JPanel(new BorderLayout(6, 6));
    top.setBorder(BorderFactory.createEmptyBorder(6, 8, 0, 8));
    top.add(new JLabel("<html>Each board is a jar of its own, published where this build says. "
        + "What you add is kept under your home directory and is in the Equipment menu at once.</html>"),
        BorderLayout.CENTER);

    JPanel bottom = new JPanel(new BorderLayout(6, 6));
    bottom.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    buttons.add(add);
    buttons.add(again);
    bottom.add(buttons, BorderLayout.WEST);
    bottom.add(saying, BorderLayout.CENTER);
    bar.setVisible(false);
    bottom.add(bar, BorderLayout.SOUTH);

    add(top, BorderLayout.NORTH);
    add(new JScrollPane(list), BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);
    setBounds(60, 60, 680, 440);

    add.addActionListener(pressed -> bringTheTickedOnes());
    again.addActionListener(pressed -> look());
    look();
  }

  private void look() {
    busy("Looking at what is published…", true);
    new SwingWorker<List<Board>, Void>() {
      protected List<Board> doInBackground() throws Exception {
        return PluginReleases.published();
      }

      protected void done() {
        try {
          boards.clear();
          wanted.clear();
          for (Board board : get()) {
            boards.add(board);
            wanted.add(false);
          }
          offer.fireTableDataChanged();
          busy(boards.size() + " published, " + here() + " here", false);
        } catch (Exception noAnswer) {
          busy("They could not be asked for: " + reason(noAnswer), false);
        }
      }
    }.execute();
  }

  private void bringTheTickedOnes() {
    List<Board> taking = new ArrayList<>();
    for (int row = 0; row < boards.size(); row++) {
      if (wanted.get(row)) taking.add(boards.get(row));
    }
    if (taking.isEmpty()) {
      busy("Tick the ones to add first", false);
      return;
    }
    busy("Bringing " + taking.size() + "…", true);
    bar.setVisible(true);
    bar.setIndeterminate(false);
    bar.setMinimum(0);
    bar.setMaximum(taking.size());
    bar.setValue(0);

    new SwingWorker<Integer, Board>() {
      protected Integer doInBackground() {
        int brought = 0;
        for (Board board : taking) {
          try {
            PluginReleases.bring(board);
            brought++;
          } catch (Exception didNotArrive) {
            System.err.println("oozx: " + board.name() + " did not arrive: " + didNotArrive);
          }
          publish(board);
        }
        return brought;
      }

      protected void process(List<Board> done) {
        bar.setValue(bar.getValue() + done.size());
        saying.setText(done.get(done.size() - 1).name() + "…");
      }

      protected void done() {
        bar.setVisible(false);
        int brought;
        try {
          brought = get();
        } catch (Exception broken) {
          brought = 0;
        }
        for (int row = 0; row < wanted.size(); row++) wanted.set(row, false);
        offer.fireTableDataChanged();
        if (arrived != null) arrived.accept(null);
        busy(brought + " plugged in, and in the Equipment menu; a machine that was already open was built without them",
            false);
      }
    }.execute();
  }

  private int here() {
    int here = 0;
    for (Board board : boards) {
      if (PluginReleases.isHere(board)) here++;
    }
    return here;
  }

  private void busy(String what, boolean waiting) {
    saying.setText(what);
    add.setEnabled(!waiting);
    again.setEnabled(!waiting);
    bar.setVisible(waiting && bar.isIndeterminate());
    if (waiting) bar.setIndeterminate(true);
  }

  private static String reason(Exception broken) {
    Throwable deepest = broken;
    while (deepest.getCause() != null) deepest = deepest.getCause();
    return deepest.getMessage() == null ? deepest.getClass().getSimpleName() : deepest.getMessage();
  }

  /** What is published, whether it is here, and what was ticked. */
  private class Offer extends AbstractTableModel {
    private final String[] columns = {"", "Board", "Size", ""};

    public int getRowCount() {
      return boards.size();
    }

    public int getColumnCount() {
      return columns.length;
    }

    public String getColumnName(int column) {
      return columns[column];
    }

    public Class<?> getColumnClass(int column) {
      return column == 0 ? Boolean.class : String.class;
    }

    public boolean isCellEditable(int row, int column) {
      return column == 0;
    }

    public Object getValueAt(int row, int column) {
      Board board = boards.get(row);
      return switch (column) {
        case 0 -> wanted.get(row);
        case 1 -> board.name();
        case 2 -> Math.max(1, board.size() / 1024) + " KB";
        default -> PluginReleases.isHere(board) ? "here" : "";
      };
    }

    public void setValueAt(Object value, int row, int column) {
      if (column == 0) wanted.set(row, Boolean.TRUE.equals(value));
    }
  }
}
