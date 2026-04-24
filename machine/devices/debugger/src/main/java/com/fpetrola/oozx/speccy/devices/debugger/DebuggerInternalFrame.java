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

package com.fpetrola.oozx.speccy.devices.debugger;

import com.fpetrola.oozx.Speccy;
import com.fpetrola.oozx.speccy.devices.MachineFrame;
import com.fpetrola.oozx.speccy.windows.Widgets;
import com.fpetrola.z80.registers.RegisterName;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.JTree;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Debugger UI for whatever machine this window is clipped to: position, memory, breakpoints,
 * plus step/run/stop controls. The layout was ported from an earlier version built against a
 * mock CPU; all data now comes from the real machine via {@link MachineDebugger}.
 */
public class DebuggerInternalFrame extends MachineFrame {

  private static final int REFRESH_MILLIS = 100;
  /** Number of instructions shown ahead of the current position. */
  private static final int LISTING = 64;
  private static final String[] REGISTERS = {"AF", "BC", "DE", "HL", "IX", "IY", "SP", "PC"};
  /** F-register flag labels, ordered from bit 7 down to bit 0. */
  private static final String[] FLAGS = {"S", "Z", "H", "P", "N", "C"};
  private static final int[] FLAG_BITS = {7, 6, 4, 2, 1, 0};

  private MachineDebugger debugger;

  private final HighlightChangedTextField[] registerFields = new HighlightChangedTextField[REGISTERS.length];
  private final JCheckBox[] flagBoxes = new JCheckBox[FLAGS.length];
  private final JButton stepOver = Widgets.iconButton("step-over.png", "Over", "One instruction, over what it calls");
  private final JButton stepInto = Widgets.iconButton("step-into.png", "Into", "One instruction, into what it calls");
  private final JButton stepOut = Widgets.iconButton("step-out.png", "Out", "Until the routine it is in gives control back");
  private final JButton run = Widgets.iconButton("continue.png", "Run", "Let it go");
  private final JButton pause = Widgets.iconButton("pause.png", "Pause", "Stop it where it is");
  private final JButton stop = Widgets.iconButton("stop.png", "Stop", "Stop it and forget every breakpoint");
  private final JLabel where = new JLabel();

  private final List<MachineDebugger.Line> lines = new ArrayList<>();
  /** Manually chosen listing start address; -1 means follow the machine's PC instead. */
  private int showing = -1;
  private final DefaultMutableTreeNode program = new DefaultMutableTreeNode("Program");
  private final DefaultTreeModel routineTree = new DefaultTreeModel(program);
  private final JTree routineList = new JTree(routineTree);
  private final Instructions instructions = new Instructions();
  private final Bytes bytes = new Bytes();
  private final Breakpoints breakpointRows = new Breakpoints();
  private final JTable instructionTable = new JTable(instructions);
  private final JTable memoryTable = new JTable(bytes);
  private final JTable breakpointTable = new JTable(breakpointRows);

  public DebuggerInternalFrame() {
    super("Debugger");

    on(stepInto, MachineDebugger::step);
    on(stepOver, MachineDebugger::stepOver);
    on(stepOut, MachineDebugger::stepOut);
    on(run, MachineDebugger::run);
    on(pause, MachineDebugger::pause);
    on(stop, it -> {
      it.pause();
      it.breakpoints().forEach(it::clearBreak);
    });
    controls.add(stepOver);
    controls.add(stepInto);
    controls.add(stepOut);
    controls.add(run);
    controls.add(pause);
    controls.add(stop);
    controls.add(where);

    routineList.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    routineList.addTreeSelectionListener(e -> {
      Object picked = routineList.getLastSelectedPathComponent();
      if (picked instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Routine routine) {
        showing = routine.address();
        refresh(false);
      }
    });

    // Left column acts as a breakpoint gutter: click toggles a breakpoint on that row's address.
    instructionTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        int row = instructionTable.rowAtPoint(e.getPoint());
        if (debugger == null || row < 0 || instructionTable.columnAtPoint(e.getPoint()) != 0) {
          return;
        }
        int address = lines.get(row).address();
        if (debugger.isBreakpoint(address)) {
          debugger.clearBreak(address);
        } else {
          debugger.breakAt(address);
        }
        refresh(false);
      }
    });

    assemble(body());
    setCompact(false);
    setSize(900, 520);

    Timer refresh = new Timer(REFRESH_MILLIS, e -> refresh(false));
    refresh.start();
    addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameClosed(InternalFrameEvent e) {
        refresh.stop();
      }
    });
    say();
  }

  /** Wires a button to a debugger action, then refreshes the view to show its effect. */
  private void on(JButton button, java.util.function.Consumer<MachineDebugger> what) {
    button.addActionListener(e -> {
      if (debugger != null) {
        showing = -1;
        what.accept(debugger);
        refresh(true);
      }
    });
  }

  /** A call target and its hit count, formatted for the routine tree. */
  private record Routine(int address, int times) {
    public String toString() {
      return "%04X \u00d7%d".formatted(address, times);
    }
  }

  private JPanel body() {
    instructionTable.getColumnModel().getColumn(0).setMaxWidth(24);
    instructionTable.getColumnModel().getColumn(1).setMaxWidth(70);
    instructionTable.getColumnModel().getColumn(2).setMaxWidth(110);
    instructionTable.getColumnModel().getColumn(3).setCellRenderer(new Z80InstructionRenderer());
    memoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    memoryTable.getColumnModel().getColumn(0).setPreferredWidth(60);
    for (int column = 1; column <= 16; column++) {
      memoryTable.getColumnModel().getColumn(column).setPreferredWidth(26);
    }

    JScrollPane instructionPane = new JScrollPane(instructionTable);
    instructionPane.setBorder(BorderFactory.createTitledBorder("Where it is"));
    JScrollPane memoryPane = new JScrollPane(memoryTable);
    memoryPane.setBorder(BorderFactory.createTitledBorder("Memory"));
    JScrollPane breakpointPane = new JScrollPane(breakpointTable);
    breakpointPane.setBorder(BorderFactory.createTitledBorder("Breakpoints"));

    JPanel memoryPanel = new JPanel(new BorderLayout());
    memoryPanel.add(memoryPane, BorderLayout.CENTER);
    memoryPanel.add(jumpTo(), BorderLayout.SOUTH);

    JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, instructionPane, registers());
    top.setResizeWeight(0.8);
    JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, memoryPanel);
    left.setResizeWeight(0.5);
    JSplitPane all = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, breakpointPane);
    all.setResizeWeight(0.8);

    JScrollPane routinePane = new JScrollPane(routineList);
    routinePane.setBorder(BorderFactory.createTitledBorder("Routines"));
    routinePane.setPreferredSize(new Dimension(150, 200));
    JSplitPane withRoutines = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, routinePane, all);
    withRoutines.setResizeWeight(0.2);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(withRoutines, BorderLayout.CENTER);
    return panel;
  }

  private JPanel registers() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createTitledBorder("Registers"));
    for (int i = 0; i < REGISTERS.length; i++) {
      JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
      row.setMaximumSize(new Dimension(200, 28));
      row.add(new JLabel(REGISTERS[i] + ":"));
      registerFields[i] = new HighlightChangedTextField("0000", 5);
      registerFields[i].setEditable(false);
      row.add(registerFields[i]);
      panel.add(row);
    }
    JPanel flags = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
    for (int i = 0; i < FLAGS.length; i++) {
      flagBoxes[i] = new JCheckBox(FLAGS[i]);
      flagBoxes[i].setEnabled(false);
      flags.add(flagBoxes[i]);
    }
    panel.add(flags);
    panel.add(Box.createVerticalGlue());
    return panel;
  }

  /** Address field and Go button that scroll the memory view to the typed address. */
  private JPanel jumpTo() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
    JTextField address = new JTextField(6);
    JButton go = new JButton("Go");
    Runnable jump = () -> {
      try {
        int at = Integer.parseInt(address.getText().trim(), 16) & 0xffff;
        int row = at / 16;
        memoryTable.scrollRectToVisible(memoryTable.getCellRect(row, 0, true));
        memoryTable.setRowSelectionInterval(row, row);
      } catch (NumberFormatException notAnAddress) {
        address.selectAll();
      }
    };
    go.addActionListener(e -> jump.run());
    address.addActionListener(e -> jump.run());
    panel.add(new JLabel("Go to:"));
    panel.add(address);
    panel.add(go);
    return panel;
  }

  @Override
  protected void machineChanged(Speccy was, Speccy now) {
    if (debugger != null) {
      debugger.close();
      debugger = null;
    }
    lines.clear();
    if (now != null) {
      debugger = new MachineDebugger(now);
      debugger.onStop(() -> SwingUtilities.invokeLater(() -> refresh(true)));
    }
    refresh(true);
  }

  private void say() {
    where.setText(debugger == null ? "clip me to a machine"
        : debugger.paused() ? "stopped" : "running");
  }

  private void refresh(boolean moved) {
    boolean has = debugger != null;
    for (JButton button : new JButton[]{stepOver, stepInto, stepOut, run, pause, stop}) {
      button.setEnabled(has);
    }
    say();
    if (!has) {
      return;
    }
    for (int i = 0; i < REGISTERS.length; i++) {
      registerFields[i].setText(hex4(debugger.register(RegisterName.valueOf(REGISTERS[i]))));
    }
    for (int i = 0; i < FLAGS.length; i++) {
      flagBoxes[i].setSelected(debugger.flag(FLAG_BITS[i]));
    }
    showRoutines();
    int from = showing < 0 ? debugger.register(RegisterName.PC) : showing;
    if (lines.isEmpty() || lines.get(0).address() != from) {
      lines.clear();
      lines.addAll(debugger.listingFrom(from, LISTING));
      instructions.fireTableDataChanged();
      breakpointRows.fireTableDataChanged();
      instructionTable.setRowSelectionInterval(0, 0);
      instructionTable.scrollRectToVisible(instructionTable.getCellRect(0, 0, true));
    }
    if (moved) {
      // Highlighting is only meaningful after a single step; free-running moves every register.
      for (HighlightChangedTextField field : registerFields) {
        field.settle();
      }
    }
    bytes.fireTableRowsUpdated(0, bytes.getRowCount() - 1);
  }

  /** Rebuilds the routine tree from newly discovered call targets. */
  private void showRoutines() {
    Map<Integer, Integer> found = debugger.routines();
    if (found.size() == program.getChildCount()) {
      return;
    }
    program.removeAllChildren();
    found.forEach((address, times) -> program.add(new DefaultMutableTreeNode(new Routine(address, times))));
    routineTree.reload();
    routineList.expandRow(0);
  }

  private static String hex4(int value) {
    return String.format("%04X", value & 0xffff);
  }

  private static String hex2(int value) {
    return String.format("%02X", value & 0xff);
  }

  /** Table model for the disassembly listing, including its breakpoint marker column. */
  private class Instructions extends AbstractTableModel {
    private final String[] columns = {"", "Address", "Bytes", "Instruction"};

    public int getRowCount() {
      return lines.size();
    }

    public int getColumnCount() {
      return columns.length;
    }

    public String getColumnName(int column) {
      return columns[column];
    }

    public Object getValueAt(int row, int column) {
      MachineDebugger.Line at = lines.get(row);
      return switch (column) {
        case 0 -> debugger != null && debugger.isBreakpoint(at.address()) ? "●" : "";
        case 1 -> hex4(at.address());
        case 2 -> at.bytes();
        default -> at.instruction();
      };
    }
  }

  /** Table model presenting all 64K of memory, 16 bytes per row, read on demand. */
  private class Bytes extends AbstractTableModel {
    public int getRowCount() {
      return 0x10000 / 16;
    }

    public int getColumnCount() {
      return 17;
    }

    public String getColumnName(int column) {
      return column == 0 ? "Address" : hex2(column - 1);
    }

    public Object getValueAt(int row, int column) {
      if (column == 0) {
        return hex4(row * 16);
      }
      return debugger == null ? "" : hex2(debugger.memory(row * 16 + column - 1));
    }
  }

  private class Breakpoints extends AbstractTableModel {
    private final String[] columns = {"Address"};

    public int getRowCount() {
      return debugger == null ? 0 : debugger.breakpoints().size();
    }

    public int getColumnCount() {
      return columns.length;
    }

    public String getColumnName(int column) {
      return columns[column];
    }

    public Object getValueAt(int row, int column) {
      return hex4(new ArrayList<>(debugger.breakpoints()).get(row));
    }
  }

  @Override
  protected String expandTip() {
    return "Show the memory and the breakpoints as well as the registers";
  }

  @Override
  protected String attachTip() {
    return "Clip this onto a machine's window, which is the machine it debugs";
  }
}
