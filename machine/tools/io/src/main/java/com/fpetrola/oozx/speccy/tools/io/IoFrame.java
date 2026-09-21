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


package com.fpetrola.oozx.speccy.tools.io;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Every port the machine touched: how often, the last byte written, and the code that did it. */
public class IoFrame extends MachineFrame {

  private static final int REFRESH_MILLIS = 400;
  private static final String[] COLUMNS = {"Port", "Reads", "Writes", "Last written", "From"};

  private final List<Ports.Port> rows = new ArrayList<>();
  private final Table table = new Table();
  private final JTable ports = new JTable(table);
  private Ports watching;

  public IoFrame() {
    super("Ports");
    setSize(560, 400);

    JButton forget = new JButton("Forget");
    forget.setToolTipText("Throw away what has been touched so far");
    forget.addActionListener(e -> {
      if (watching != null) {
        watching.forget();
      }
    });
    controls.add(forget);

    ports.getColumnModel().getColumn(0).setMaxWidth(70);
    ports.getColumnModel().getColumn(3).setMaxWidth(110);
    JScrollPane pane = new JScrollPane(ports);
    pane.setPreferredSize(new Dimension(540, 320));
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
      watching = new Ports(now);
    }
    draw();
  }

  @Override
  protected String expandTip() {
    return "Show the ports, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  private void draw() {
    rows.clear();
    if (watching != null) {
      rows.addAll(watching.touched());
    }
    table.fireTableDataChanged();
  }

  /** The places that touched a port, the one that did it most first. */
  private static String who(Map<Integer, Integer> from) {
    List<String> places = from.entrySet().stream()
        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
        .map(place -> "%04X".formatted(place.getKey())).toList();
    return places.size() <= 3 ? String.join(", ", places)
        : String.join(", ", places.subList(0, 3)) + " +" + (places.size() - 3);
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
      Ports.Port port = rows.get(row);
      return switch (column) {
        case 0 -> "%04X".formatted(port.port());
        case 1 -> port.reads();
        case 2 -> port.writes();
        case 3 -> port.lastWritten() < 0 ? "" : "%02X".formatted(port.lastWritten());
        default -> who(port.from());
      };
    }
  }
}
