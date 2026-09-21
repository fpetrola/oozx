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


package com.fpetrola.oozx.speccy.tools.calls;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;
import com.fpetrola.oozx.speccy.modules.z80.Disassembly;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** The routines a run found: where each starts, how far it goes, and how often it was called. */
public class FunctionsFrame extends MachineFrame {

  private static final int REFRESH_MILLIS = 1000;
  private static final String[] COLUMNS =
      {"Address", "Bytes", "Instructions", "Called", "Callers", "Ends with"};

  private final List<Routines.Routine> rows = new ArrayList<>();
  private final Table table = new Table();
  private final JTable routines = new JTable(table);
  private final JLabel said = new JLabel(" ");
  private CallTree watching;
  private Disassembly reading;
  private long drawn = -1;

  public FunctionsFrame() {
    super("Functions");
    setSize(620, 400);

    JButton forget = new JButton("Forget");
    forget.setToolTipText("Throw away the routines found so far and watch the next run");
    forget.addActionListener(e -> {
      if (watching != null) {
        watching.forget();
      }
    });
    controls.add(forget);
    controls.add(javax.swing.Box.createHorizontalStrut(10));
    controls.add(said);

    routines.getColumnModel().getColumn(0).setMaxWidth(70);
    routines.getColumnModel().getColumn(1).setMaxWidth(60);
    routines.getColumnModel().getColumn(2).setMaxWidth(90);
    routines.getColumnModel().getColumn(4).setMaxWidth(70);
    JScrollPane pane = new JScrollPane(routines);
    pane.setPreferredSize(new Dimension(600, 320));
    assemble(pane);
    setCompact(false);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> draw());
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
  }

  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    if (watching != null) {
      watching.close();
      watching = null;
    }
    reading = now == null ? null : new Disassembly(now);
    drawn = -1;
    if (now != null) {
      watching = new CallTree(now);
    }
    draw();
  }

  @Override
  protected String expandTip() {
    return "Show the routines, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  /**
   * Worked out again only when a call has been made since the last time: reading every routine
   * forward is the expensive half, and while nothing is being called it says the same thing.
   */
  private void draw() {
    long counted = watching == null ? -1 : watching.counted();
    if (counted == drawn) {
      return;
    }
    drawn = counted;
    rows.clear();
    if (watching != null) {
      rows.addAll(Routines.found(watching.program(), reading));
      said.setText("%d routines".formatted(rows.size()));
    } else {
      said.setText("not clipped onto a machine");
    }
    table.fireTableDataChanged();
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
      Routines.Routine routine = rows.get(row);
      return switch (column) {
        case 0 -> "%04X".formatted(routine.address());
        case 1 -> routine.bytes();
        case 2 -> routine.instructions();
        case 3 -> routine.times();
        case 4 -> routine.callers();
        default -> routine.ends();
      };
    }
  }
}
