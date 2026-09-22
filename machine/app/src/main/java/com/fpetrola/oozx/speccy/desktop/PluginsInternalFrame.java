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
import javax.swing.tree.DefaultMutableTreeNode;

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

  /** The same list of what is plugged in, grouped two ways. */
  private final javax.swing.JTree byJar = new javax.swing.JTree(new DefaultMutableTreeNode());
  private final javax.swing.JTree byKind = new javax.swing.JTree(new DefaultMutableTreeNode());

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

    // Three ways of looking at the same thing: what can be had, what each jar put in, and what
    // there is of each kind. The second and the third are read from what the jars say they
    // bring, so they answer for what shipped as well as for what was brought.
    javax.swing.JTabbedPane ways = new javax.swing.JTabbedPane();
    ways.addTab("Published", sides);
    ways.addTab("What each jar brought", new JScrollPane(byJar));
    ways.addTab("What there is of each kind", new JScrollPane(byKind));
    ways.addChangeListener(changed -> tellWhatIsIn());

    add(ways, BorderLayout.CENTER);
    add(bottom, BorderLayout.SOUTH);
    setBounds(60, 60, 700, 460);

    include.addActionListener(pressed -> includeTheChosen());
    leaveOut.addActionListener(pressed -> leaveOutTheChosen());
    again.addActionListener(pressed -> look());
    tellWhatIsIn();
    look();
  }

  /**
   * What is plugged in, as two trees: one under the jar it came in, one under the kind of thing
   * it is. Read whenever this is looked at, because a board brought a moment ago is in it.
   */
  private void tellWhatIsIn() {
    java.util.List<com.fpetrola.oozx.plugins.Plugins.WhatIsIn> everything =
        com.fpetrola.oozx.plugins.Plugins.everythingPluggedIn();
    fill(byJar, everything, com.fpetrola.oozx.plugins.Plugins.WhatIsIn::from,
        one -> one.kind() + ": " + shortly(one.implementation()));
    fill(byKind, everything, one -> one.kind() + "  (" + shortly(one.wayIn()) + ")",
        one -> shortly(one.implementation()) + "  -  " + one.from());
  }

  private static void fill(javax.swing.JTree tree,
      java.util.List<com.fpetrola.oozx.plugins.Plugins.WhatIsIn> everything,
      java.util.function.Function<com.fpetrola.oozx.plugins.Plugins.WhatIsIn, String> branch,
      java.util.function.Function<com.fpetrola.oozx.plugins.Plugins.WhatIsIn, String> leaf) {
    java.util.Map<String, java.util.List<String>> grouped = new java.util.TreeMap<>();
    everything.forEach(one -> grouped.computeIfAbsent(branch.apply(one),
        first -> new ArrayList<>()).add(leaf.apply(one)));
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(
        grouped.size() + (grouped.size() == 1 ? " of them" : " of them"));
    grouped.forEach((name, leaves) -> {
      DefaultMutableTreeNode node = new DefaultMutableTreeNode(name + "  (" + leaves.size() + ")");
      leaves.stream().sorted().forEach(one -> node.add(new DefaultMutableTreeNode(one)));
      root.add(node);
    });
    tree.setModel(new javax.swing.tree.DefaultTreeModel(root));
    for (int row = 0; row < tree.getRowCount(); row++) {
      tree.expandRow(row);
    }
  }

  /** A class by its own name, with the package as far as it is worth reading. */
  private static String shortly(String className) {
    int lastDot = className.lastIndexOf('.');
    return lastDot < 0 ? className : className.substring(lastDot + 1);
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
          List<Board> published = get();
          // What is in is what is in the folder, not what was downloaded through this window:
          // one built here or copied in by hand is just as plugged in as one that arrived.
          PluginReleases.here(published).forEach(inside::addElement);
          for (Board board : published) {
            if (!PluginReleases.isHere(board)) outside.addElement(board);
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
    bar.setIndeterminate(true);

    new SwingWorker<Void, Board>() {
      protected Void doInBackground() {
        java.util.Deque<Board> taking = new java.util.ArrayDeque<>(chosen);
        java.util.Set<String> asked = new java.util.HashSet<>();
        while (!taking.isEmpty()) {
          Board board = taking.poll();
          if (!asked.add(board.jar())) continue;
          try {
            java.nio.file.Path jar = PluginReleases.bring(board);
            publish(board);
            // What it was built against and is not here: a board that uses another's code is no
            // use without it, so it arrives too rather than failing at the first missing class.
            for (String needed : PluginReleases.needs(jar)) {
              for (int row = 0; row < outside.size(); row++) {
                Board other = outside.get(row);
                if (other.jar().equals(needed) && !asked.contains(needed)) taking.add(other);
              }
            }
          } catch (Exception didNotArrive) {
            TellsThePerson.that(board.name() + " did not arrive: " + didNotArrive);
          }
        }
        return null;
      }

      protected void process(List<Board> arrived) {
        for (Board board : arrived) {
          outside.removeElement(board);
          inside.addElement(board);
          saying.setText(board.name() + " is in");
        }
        sort(inside);
      }

      protected void done() {
        bar.setVisible(false);
        tellWhatIsIn();
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
        TellsThePerson.that(board.name() + " could not be taken out: " + wouldNotGo);
      }
    }
    sort(outside);
    tellWhatIsIn();
    if (arrived != null) arrived.accept(null);
    busy(inside.size() + " in, " + outside.size() + " to be had  -  the " + gone + " taken out is gone"
        + " from the menu; a machine that already has it keeps it until the emulator starts again", false);
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
            + Math.max(1, board.size() / 1024) + " KB</font>"
            + (PluginReleases.isNewerThanHere(board)
            ? "  <font color='#3070c0'>a newer one is published</font>" : "") + "</html>");
      }
      return this;
    }
  }
}
