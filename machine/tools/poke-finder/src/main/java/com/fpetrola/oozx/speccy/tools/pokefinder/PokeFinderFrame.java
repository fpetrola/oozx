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


package com.fpetrola.oozx.speccy.tools.pokefinder;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * Finding the byte that holds the lives, by playing.
 * <p>
 * The buttons are the whole interface and they are questions to the player, not to the machine:
 * play until you lose a life, then say it went down. What is left after a few rounds is the
 * poke nobody has published yet.
 */
public class PokeFinderFrame extends MachineFrame {

  /** Often enough that a held address wins against a game writing to it every frame. */
  private static final int REFRESH_MILLIS = 20;
  private static final int DRAWN_EVERY = 10;
  private static final int SHOWN = 200;
  private static final String[] COLUMNS = {"Address", "Was", "Now", "Held at"};

  private final List<Hunt.Candidate> rows = new ArrayList<>();
  private final Table table = new Table();
  private final JTable candidates = new JTable(table);
  private final JTextField value = new JTextField("0", 4);
  private final JLabel said = new JLabel(" ");
  private Hunt hunt;
  private int ticks;

  public PokeFinderFrame() {
    super("Poke finder");
    setSize(560, 420);

    JButton over = new JButton("Start over");
    over.setToolTipText("Every address is a candidate again");
    over.addActionListener(e -> {
      if (hunt != null) {
        hunt.startOver();
      }
      draw();
    });
    controls.add(over);
    for (Hunt.Change change : Hunt.Change.values()) {
      controls.add(asking(change));
    }
    value.setToolTipText("The number you can see on the screen");
    value.setMaximumSize(new Dimension(50, 26));
    JButton is = new JButton("Is");
    is.setToolTipText("Keep only the addresses holding exactly that");
    is.addActionListener(e -> {
      if (hunt != null) {
        hunt.narrowTo(read(value));
      }
      draw();
    });
    controls.add(value);
    controls.add(is);
    controls.add(Box.createHorizontalStrut(10));
    controls.add(hold());
    controls.add(pokeLine());
    controls.add(Box.createHorizontalStrut(10));
    controls.add(said);

    candidates.getColumnModel().getColumn(0).setMaxWidth(80);
    JScrollPane pane = new JScrollPane(candidates);
    pane.setPreferredSize(new Dimension(540, 300));
    assemble(pane);
    setCompact(false);

    Timer ticking = new Timer(REFRESH_MILLIS, e -> {
      if (hunt != null) {
        hunt.keepHeld();
      }
      if (ticks++ % DRAWN_EVERY == 0) {
        draw();
      }
    });
    ticking.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        ticking.stop();
      }
    });
  }

  /** One button per thing the player can say happened to the number they are looking for. */
  private JButton asking(Hunt.Change change) {
    JButton button = new JButton(change.said());
    button.setToolTipText("Keep the addresses that " + change.said());
    button.addActionListener(e -> {
      if (hunt != null) {
        hunt.narrow(change);
      }
      draw();
    });
    return button;
  }

  /** Holds the chosen address at the value in the box, which is the poke tried out before it is written down. */
  private JButton hold() {
    JButton button = new JButton("Hold");
    button.setToolTipText("Keep the chosen address at that value, the way the poke would");
    button.addActionListener(e -> {
      Hunt.Candidate chosen = chosen();
      if (chosen == null) {
        return;
      }
      if (hunt.held().containsKey(chosen.address())) {
        hunt.release(chosen.address());
      } else {
        hunt.hold(chosen.address(), read(value));
      }
      draw();
    });
    return button;
  }

  private JButton pokeLine() {
    JButton button = new JButton("As a poke");
    button.setToolTipText("The line of a .pok file that does this");
    button.addActionListener(e -> {
      Hunt.Candidate chosen = chosen();
      if (chosen != null) {
        JOptionPane.showInputDialog(this, "A .pok file line for this address:",
            hunt.asPokeLine(chosen.address(), read(value)));
      }
    });
    return button;
  }

  private Hunt.Candidate chosen() {
    int row = candidates.getSelectedRow();
    return row < 0 || row >= rows.size() ? null : rows.get(row);
  }

  private static int read(JTextField field) {
    try {
      return Integer.parseInt(field.getText().trim());
    } catch (NumberFormatException notANumber) {
      return 0;
    }
  }

  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    hunt = now == null ? null : new Hunt(now);
    if (hunt != null) {
      hunt.startOver();
    }
    draw();
  }

  @Override
  protected String expandTip() {
    return "Show what is left, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  private void draw() {
    int chosen = candidates.getSelectedRow();
    rows.clear();
    if (hunt == null) {
      said.setText("not clipped onto a machine");
    } else {
      rows.addAll(hunt.shortlist(SHOWN));
      said.setText("%d left after %d rounds%s".formatted(hunt.left(), hunt.rounds(),
          hunt.left() > SHOWN ? ", showing the first " + SHOWN : ""));
    }
    table.fireTableDataChanged();
    if (chosen >= 0 && chosen < rows.size()) {
      candidates.setRowSelectionInterval(chosen, chosen);
    }
  }

  private class Table extends AbstractTableModel {
    public int getRowCount() {
      return rows.size();
    }

    public int getColumnCount() {
      return COLUMNS.length;
    }

    public String getColumnName(int column) {
      return COLUMNS[column];
    }

    public Object getValueAt(int row, int column) {
      Hunt.Candidate candidate = rows.get(row);
      return switch (column) {
        case 0 -> "%04X".formatted(candidate.address());
        case 1 -> candidate.before();
        case 2 -> candidate.now();
        default -> {
          Integer at = hunt.held().get(candidate.address());
          yield at == null ? "" : at;
        }
      };
    }
  }
}
