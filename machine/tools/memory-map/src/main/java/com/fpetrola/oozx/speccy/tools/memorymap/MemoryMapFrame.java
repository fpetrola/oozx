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


package com.fpetrola.oozx.speccy.tools.memorymap;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** What is mapped into each part of the 64K right now, and how it behaves there. */
public class MemoryMapFrame extends MachineFrame {

  private static final int REFRESH_MILLIS = 500;
  private static final String[] COLUMNS = {"From", "To", "Size", "What", "Page", "Access", "Contended"};

  private final List<MemoryMap.Region> rows = new ArrayList<>();
  private final Table table = new Table();
  private final JTable map = new JTable(table);

  public MemoryMapFrame() {
    super("Memory map");
    setSize(560, 300);

    for (int column : new int[]{0, 1, 2, 4, 5, 6}) {
      map.getColumnModel().getColumn(column).setMaxWidth(90);
    }
    JScrollPane pane = new JScrollPane(map);
    pane.setPreferredSize(new Dimension(540, 220));
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
    draw();
  }

  @Override
  protected String expandTip() {
    return "Show the map, or just the buttons";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto the machine's window";
  }

  private void draw() {
    rows.clear();
    if (machine() != null) {
      rows.addAll(MemoryMap.of(machine()));
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
      MemoryMap.Region region = rows.get(row);
      return switch (column) {
        case 0 -> "%04X".formatted(region.from());
        case 1 -> "%04X".formatted(region.to());
        case 2 -> region.size() % 1024 == 0 ? region.size() / 1024 + "K" : region.size() + "b";
        case 3 -> region.what();
        case 4 -> region.page() < 0 ? "" : region.page();
        case 5 -> (region.readable() ? "r" : "-") + (region.writable() ? "w" : "-");
        default -> region.contended() ? "yes" : "";
      };
    }
  }
}
