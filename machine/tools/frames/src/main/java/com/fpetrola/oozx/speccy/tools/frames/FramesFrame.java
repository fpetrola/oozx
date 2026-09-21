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


package com.fpetrola.oozx.speccy.tools.frames;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

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

/** What the machine did in each of the last frames, the most recent last. */
public class FramesFrame extends MachineFrame {

  private static final int REFRESH_MILLIS = 250;
  private static final String[] COLUMNS =
      {"Frame", "Instructions", "T-states", "Lowest", "Highest"};

  private final List<Frames.Frame> rows = new ArrayList<>();
  private final Table table = new Table();
  private final JTable frames = new JTable(table);
  private final JLabel said = new JLabel(" ");
  private Frames watching;

  public FramesFrame() {
    super("Frames");
    setSize(540, 360);

    JButton forget = new JButton("Forget");
    forget.addActionListener(e -> {
      if (watching != null) {
        watching.forget();
      }
    });
    controls.add(forget);
    controls.add(javax.swing.Box.createHorizontalStrut(10));
    controls.add(said);

    for (int column = 0; column < COLUMNS.length; column++) {
      frames.getColumnModel().getColumn(column).setMaxWidth(120);
    }
    JScrollPane pane = new JScrollPane(frames);
    pane.setPreferredSize(new Dimension(520, 280));
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
    if (now != null) {
      watching = new Frames(now);
    }
    draw();
  }

  @Override
  protected String expandTip() {
    return "Show the frames, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  private void draw() {
    rows.clear();
    if (watching == null) {
      said.setText("not clipped onto a machine");
    } else {
      rows.addAll(watching.ended());
      said.setText(busiest());
    }
    table.fireTableDataChanged();
    if (!rows.isEmpty()) {
      frames.scrollRectToVisible(frames.getCellRect(rows.size() - 1, 0, true));
    }
  }

  /** The frame that ran the most, which is the one worth going and looking at. */
  private String busiest() {
    return rows.stream().mapToInt(Frames.Frame::instructions).max()
        .stream().mapToObj("busiest frame ran %d instructions"::formatted)
        .findFirst().orElse("nothing has run yet");
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
      Frames.Frame frame = rows.get(row);
      return switch (column) {
        case 0 -> frame.number();
        case 1 -> frame.instructions();
        case 2 -> frame.tStates();
        case 3 -> "%04X".formatted(frame.lowest());
        default -> "%04X".formatted(frame.highest());
      };
    }
  }
}
