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


package com.fpetrola.oozx.speccy.tools.trace;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;
import com.fpetrola.oozx.speccy.modules.z80.Disassembly;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** The last instructions the machine ran, disassembled only as far as the window shows. */
public class TraceFrame extends MachineFrame {

  private static final int REFRESH_MILLIS = 300;
  private static final int SHOWN = 300;
  private static final String[] COLUMNS = {"Address", "Bytes", "Instruction"};

  private final List<Disassembly.Line> rows = new ArrayList<>();
  private final Table table = new Table();
  private final JTable steps = new JTable(table);
  private final JToggleButton following = new JToggleButton("Following", true);
  private final JLabel said = new JLabel(" ");
  private Trace trace;
  private Disassembly reading;

  public TraceFrame() {
    super("Trace");
    setSize(520, 420);

    following.setToolTipText("Keep taking down what runs, or hold what is there to read it");
    following.addActionListener(e -> {
      following.setText(following.isSelected() ? "Following" : "Held");
      if (trace != null) {
        trace.follow(following.isSelected());
      }
    });
    JButton forget = new JButton("Forget");
    forget.addActionListener(e -> {
      if (trace != null) {
        trace.forget();
      }
    });
    controls.add(following);
    controls.add(forget);
    controls.add(javax.swing.Box.createHorizontalStrut(10));
    controls.add(said);

    steps.getColumnModel().getColumn(0).setMaxWidth(70);
    steps.getColumnModel().getColumn(1).setMaxWidth(110);
    JScrollPane pane = new JScrollPane(steps);
    pane.setPreferredSize(new Dimension(500, 340));
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
    if (trace != null) {
      trace.close();
      trace = null;
    }
    reading = now == null ? null : new Disassembly(now);
    if (now != null) {
      trace = new Trace(now);
      trace.follow(following.isSelected());
    }
    draw();
  }

  @Override
  protected String expandTip() {
    return "Show what ran, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  /**
   * Disassembles only the few hundred addresses on show. The ring holds many more, and decoding
   * all of them to draw a screenful is the one way a trace window slows the machine it watches.
   */
  private void draw() {
    rows.clear();
    if (trace != null) {
      for (int address : trace.last(SHOWN)) {
        rows.add(reading.lineAt(address));
      }
      said.setText("%d instructions run".formatted(trace.steps()));
    } else {
      said.setText("not clipped onto a machine");
    }
    table.fireTableDataChanged();
    if (following.isSelected() && !rows.isEmpty()) {
      steps.scrollRectToVisible(steps.getCellRect(rows.size() - 1, 0, true));
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
      Disassembly.Line line = rows.get(row);
      return switch (column) {
        case 0 -> "%04X".formatted(line.address());
        case 1 -> line.bytes();
        default -> line.instruction();
      };
    }
  }
}
